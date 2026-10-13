package com.rankednh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.function.LongSupplier;
import net.runelite.client.audio.AudioPlayer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** One finite cue per reveal. Hiding or muting consumes it; polling cannot start it again. */
final class PlacementRevealAudio
{
    static final String RESOURCE="sounds/placement-reveal.wav";
    static final long CHIME_MS=1800;
    interface Playback { void play(long offsetMillis,int volume); void stop(); }
    private final Playback playback;
    private String key;
    private boolean handled,playing;

    PlacementRevealAudio(ScheduledExecutorService executor,AudioPlayer audioPlayer){
        this(new RuneLitePlayback(executor,audioPlayer::play,System::nanoTime));
    }
    PlacementRevealAudio(Playback playback){this.playback=playback;}

    synchronized void update(String reportKey,MatchReportView view,MatchReportAnimation.Frame frame,boolean enabled,int volume,boolean reducedMotion){
        if(!view.placementReveal && !view.promotion()){hide();return;}
        if(!reportKey.equals(key)){hide();key=reportKey;handled=false;}
        if(!enabled || volume<=0){handled=true;hide();return;}
        if(handled)return;
        long landing=view.placementReveal?MatchReportAnimation.PLACEMENT_BADGE_END:MatchReportAnimation.PROMOTION_SPIN_END;
        long start=landing-CHIME_MS;
        if(!reducedMotion && frame.elapsed<start)return;
        handled=true;
        // Don't play an anticipation cue after a report has already settled off-screen.
        if(frame.elapsed>=(reducedMotion?1:landing+380))return;
        playback.play(reducedMotion?CHIME_MS:frame.elapsed-start,Math.min(100,volume));
        playing=true;
    }
    synchronized void hide(){if(playing)playback.stop();playing=false;}
    synchronized void reset(){hide();key=null;handled=false;}

    /** This bundled resource is canonical 44.1 kHz, mono, 16-bit PCM with a 44-byte WAV header. */
    static byte[] pcm(int volume)throws IOException{
        try(InputStream resource=PlacementRevealAudio.class.getResourceAsStream(RESOURCE)){
            if(resource==null)throw new IOException("Missing placement reveal sound");
            byte[] wav=resource.readAllBytes();
            if(wav.length<44)throw new IOException("Truncated placement reveal sound");
            ByteBuffer header=ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
            if(header.getInt(0)!=0x46464952 || header.getInt(4)!=wav.length-8 || header.getInt(8)!=0x45564157 ||
                header.getInt(12)!=0x20746d66 || header.getInt(16)!=16 || header.getShort(20)!=1 ||
                header.getShort(22)!=1 || header.getInt(24)!=44100 || header.getInt(28)!=88200 ||
                header.getShort(32)!=2 || header.getShort(34)!=16 || header.getInt(36)!=0x61746164 ||
                header.getInt(40)!=wav.length-44 || (wav.length-44)%2!=0)
                throw new IOException("Unsupported placement sound format");
            byte[] data=Arrays.copyOfRange(wav,44,wav.length);
            double gain=Math.max(0,Math.min(100,volume))/100.0;
            for(int i=0;i<data.length;i+=2){
                int sample=(short)((data[i]&255)|(data[i+1]<<8));int scaled=(int)Math.round(sample*gain);
                data[i]=(byte)scaled;data[i+1]=(byte)(scaled>>8);
            }
            return data;
        }
    }

    /** Trim to the animation clock and soften cut edges without using Java's audio devices. */
    static byte[] wave(byte[] pcm,long startMillis,long endMillis){
        int samples=pcm.length/2;
        int first=(int)Math.min(samples,Math.max(0,startMillis)*44100/1000);
        int last=(int)Math.min(samples,Math.max(0,endMillis)*44100/1000);
        if(last<=first)return null;
        int length=(last-first)*2;
        ByteBuffer wave=ByteBuffer.allocate(44+length).order(ByteOrder.LITTLE_ENDIAN);
        wave.putInt(0x46464952).putInt(36+length).putInt(0x45564157).putInt(0x20746d66).putInt(16);
        wave.putShort((short)1).putShort((short)1).putInt(44100).putInt(88200).putShort((short)2).putShort((short)16);
        wave.putInt(0x61746164).putInt(length);
        int fade=Math.min(220,(last-first)/2);
        for(int i=first;i<last;i++){
            int sample=(short)((pcm[i*2]&255)|(pcm[i*2+1]<<8));
            double edge=fade==0?1:Math.min(1,Math.min(i-first,last-i-1)/(double)fade);
            wave.putShort((short)Math.round(sample*edge));
        }
        return wave.array();
    }

    /**
     * RuneLite owns the audio device. Its API has no stop handle, so use a finite hum and
     * a separate chime: dismissing cancels queued work, while a cue already started finishes.
     */
    static final class RuneLitePlayback implements Playback {
        interface Output { void play(InputStream wave,float gain)throws Exception; }
        private final ScheduledExecutorService executor;
        private final Output output;
        private final LongSupplier clock;
        private Session current;
        RuneLitePlayback(ScheduledExecutorService executor,Output output,LongSupplier clock){
            this.executor=executor;this.output=output;this.clock=clock;
        }
        @Override public synchronized void play(long offsetMillis,int volume){
            stop();Session session=new Session(Math.max(0,offsetMillis),volume,clock.getAsLong());current=session;
            executor.execute(session::open);
        }
        @Override public synchronized void stop(){
            Session session=current;current=null;
            if(session!=null)session.cancel();
        }
        private final class Session {
            final AtomicBoolean cancelled=new AtomicBoolean();
            final long offset,requested;
            final int volume;
            volatile ScheduledFuture<?> next;
            byte[] data;
            Session(long offset,int volume,long requested){this.offset=offset;this.volume=volume;this.requested=requested;}
            long elapsed(){return offset+Math.max(0,(clock.getAsLong()-requested)/1_000_000);}
            void open(){
                if(cancelled.get() || volume<=0)return;
                try{data=pcm(volume);playPart(elapsed()>=CHIME_MS);}
                catch(Exception unavailable){cancel();/* Visual reveal remains usable without an audio device. */}
            }
            void playPart(boolean chime){
                if(cancelled.get())return;
                long elapsed=elapsed();
                if(!chime && elapsed>=CHIME_MS){playPart(true);return;}
                byte[] wav=wave(data,chime?Math.max(CHIME_MS,elapsed):elapsed,chime?data.length*1000L/88200:CHIME_MS);
                if(wav==null || cancelled.get())return;
                try(InputStream source=new ByteArrayInputStream(wav)){
                    output.play(source,0); // PCM gain also works on devices without a volume control.
                    if(!chime && !cancelled.get()){
                        next=executor.schedule(()->playPart(true),Math.max(0,CHIME_MS-elapsed()),TimeUnit.MILLISECONDS);
                        if(cancelled.get())next.cancel(false);
                    }
                }catch(Exception unavailable){cancel();}
            }
            void cancel(){
                cancelled.set(true);
                ScheduledFuture<?> pending=next;
                if(pending!=null)pending.cancel(false);
            }
        }
    }
}
