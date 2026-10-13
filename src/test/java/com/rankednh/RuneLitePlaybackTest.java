package com.rankednh;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;
import static org.junit.Assert.*;

public class RuneLitePlaybackTest {
    private final Queue<Runnable> work=new ArrayDeque<>();
    private final Queue<Task> timers=new ArrayDeque<>();
    private final AtomicLong now=new AtomicLong();
    private final List<byte[]> played=new ArrayList<>();
    private final ScheduledExecutorService executor=(ScheduledExecutorService)Proxy.newProxyInstance(
        getClass().getClassLoader(),new Class[]{ScheduledExecutorService.class},(p,m,a)->{
            if(m.getName().equals("execute")){work.add((Runnable)a[0]);return null;}
            if(m.getName().equals("schedule")){
                Task task=new Task((Runnable)a[0],((TimeUnit)a[2]).toMillis((long)a[1]));timers.add(task);return task;
            }
            throw new UnsupportedOperationException(m.getName());
        });
    private PlacementRevealAudio.RuneLitePlayback playback(){
        return new PlacementRevealAudio.RuneLitePlayback(executor,(stream,gain)->{
            assertEquals(0,gain,0);played.add(stream.readAllBytes());
        },now::get);
    }
    private static final class Task extends FutureTask<Void> implements ScheduledFuture<Void> {
        final long delay;
        Task(Runnable work,long delay){super(work,null);this.delay=delay;}
        @Override public long getDelay(TimeUnit unit){return unit.convert(delay,TimeUnit.MILLISECONDS);}
        @Override public int compareTo(Delayed other){return Long.compare(getDelay(TimeUnit.MILLISECONDS),other.getDelay(TimeUnit.MILLISECONDS));}
    }
    @Test public void closingBeforeWorkStaysSilentAndClosingDuringHumCancelsTheChime(){
        PlacementRevealAudio.RuneLitePlayback playback=playback();
        playback.play(0,65);playback.stop();work.remove().run();assertTrue(played.isEmpty());
        playback.play(0,65);work.remove().run();assertEquals(1,played.size());
        Task chime=timers.remove();assertEquals(1800,chime.delay);
        playback.stop();assertTrue(chime.isCancelled());now.set(1_800_000_000L);chime.run();assertEquals(1,played.size());
    }
    @Test public void humAndChimeUseValidWavesAndFollowTheRevealClock()throws Exception{
        PlacementRevealAudio.RuneLitePlayback playback=playback();
        playback.play(0,65);now.set(900_000_000L);work.remove().run();
        assertEquals(900,timers.peek().delay);assertWave(played.get(0),900);
        now.set(1_850_000_000L);timers.remove().run();
        assertEquals(2,played.size());assertWave(played.get(1),1550);assertTrue(timers.isEmpty());
        byte[] actual;
        try(AudioInputStream stream=AudioSystem.getAudioInputStream(new ByteArrayInputStream(played.get(1)))){actual=stream.readAllBytes();}
        byte[] expected=PlacementRevealAudio.pcm(65);
        int start=1850*44100/1000*2;
        assertEquals(expected[start+2000],actual[2000]);assertEquals(expected[start+2001],actual[2001]);
    }
    @Test public void reducedMotionSkipsHumAndStaleOrMutedWorkStaysSilent()throws Exception{
        PlacementRevealAudio.RuneLitePlayback playback=playback();
        playback.play(1800,65);work.remove().run();assertEquals(1,played.size());assertWave(played.get(0),1600);assertTrue(timers.isEmpty());
        playback.play(0,65);now.set(4_000_000_000L);work.remove().run();assertEquals(1,played.size());
        playback.play(0,0);work.remove().run();assertEquals(1,played.size());
    }
    @Test public void aNewReportCancelsThePreviousReportsPendingChime(){
        PlacementRevealAudio.RuneLitePlayback playback=playback();
        playback.play(0,65);work.remove().run();Task previous=timers.remove();
        playback.play(1800,65);work.remove().run();assertTrue(previous.isCancelled());previous.run();assertEquals(2,played.size());
    }
    @Test public void unavailableAudioDoesNotScheduleMoreWork(){
        PlacementRevealAudio.RuneLitePlayback playback=new PlacementRevealAudio.RuneLitePlayback(executor,
            (stream,gain)->{throw new java.io.IOException("no device");},now::get);
        playback.play(0,65);work.remove().run();assertTrue(timers.isEmpty());playback.stop();
    }
    private static void assertWave(byte[] wav,long durationMillis)throws Exception{
        try(InputStream bytes=new ByteArrayInputStream(wav);AudioInputStream audio=AudioSystem.getAudioInputStream(bytes)){
            assertEquals(44100,audio.getFormat().getSampleRate(),0);assertEquals(1,audio.getFormat().getChannels());
            assertEquals(16,audio.getFormat().getSampleSizeInBits());assertFalse(audio.getFormat().isBigEndian());
            byte[] pcm=audio.readAllBytes();assertEquals(durationMillis*44100/1000*2,pcm.length);
            assertEquals(0,pcm[0]);assertEquals(0,pcm[1]);assertEquals(0,pcm[pcm.length-1]);assertEquals(0,pcm[pcm.length-2]);
        }
    }
}
