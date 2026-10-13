package com.rankednh;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
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

    PlacementRevealAudio(ScheduledExecutorService executor){this(new ClipPlayback(executor,AudioSystem::getClip));}
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

    static byte[] pcm(int volume)throws Exception{
        try(InputStream resource=PlacementRevealAudio.class.getResourceAsStream(RESOURCE)){
            if(resource==null)throw new IOException("Missing placement reveal sound");
            // The supplied stream need not support mark/reset, which the WAV reader requires.
            try(AudioInputStream input=AudioSystem.getAudioInputStream(new ByteArrayInputStream(resource.readAllBytes()))){
                AudioFormat format=input.getFormat();
                if(format.getSampleSizeInBits()!=16 || format.isBigEndian() || format.getChannels()!=1 || format.getSampleRate()!=44100)
                    throw new IOException("Unsupported placement sound format");
                byte[] data=input.readAllBytes();double gain=Math.max(0,Math.min(100,volume))/100.0;
                for(int i=0;i<data.length;i+=2){
                    int sample=(short)((data[i]&255)|(data[i+1]<<8));int scaled=(int)Math.round(sample*gain);
                    data[i]=(byte)scaled;data[i+1]=(byte)(scaled>>8);
                }
                return data;
            }
        }
    }

    /** All device operations use RuneLite's executor; cancellation never interrupts a thread. */
    static final class ClipPlayback implements Playback {
        interface Factory { Clip create() throws Exception; }
        private final ScheduledExecutorService executor;
        private final Factory factory;
        private Session current;
        ClipPlayback(ScheduledExecutorService executor,Factory factory){this.executor=executor;this.factory=factory;}
        @Override public synchronized void play(long offsetMillis,int volume){
            stop();Session session=new Session();current=session;long requested=System.nanoTime();
            executor.execute(()->session.open(offsetMillis,volume,requested));
        }
        @Override public synchronized void stop(){
            Session session=current;current=null;
            if(session!=null){session.cancelled.set(true);executor.execute(session::close);}
        }
        private final class Session {
            final AtomicBoolean cancelled=new AtomicBoolean();
            private Clip clip;
            private ScheduledFuture<?> finish;
            synchronized void open(long offsetMillis,int volume,long requested){
                if(cancelled.get())return;
                try{
                    byte[] data=pcm(volume);
                    if(cancelled.get())return;
                    clip=factory.create();
                    clip.open(new AudioFormat(44100,16,1,true,false),data,0,data.length);
                    long elapsed=offsetMillis+(System.nanoTime()-requested)/1_000_000;
                    long remaining=clip.getMicrosecondLength()/1000-elapsed;
                    if(cancelled.get() || remaining<=0){close();return;}
                    clip.setMicrosecondPosition(elapsed*1000);clip.start();
                    finish=executor.schedule(this::close,remaining,TimeUnit.MILLISECONDS);
                }catch(Exception unavailable){close();/* Visual reveal remains usable without a sound device. */}
            }
            synchronized void close(){
                if(finish!=null){finish.cancel(false);finish=null;}
                if(clip!=null){Clip opened=clip;clip=null;try{opened.stop();}finally{opened.close();}}
            }
        }
    }
}
