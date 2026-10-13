package com.rankednh;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sound.sampled.Clip;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClipPlaybackTest {
    @Test public void cancelBeforeDeviceWorkCannotPlayAndStoppingClosesOnExecutor()throws Exception{
        Queue<Runnable> work=new ArrayDeque<>(),timers=new ArrayDeque<>();AtomicInteger opened=new AtomicInteger(),starts=new AtomicInteger(),closes=new AtomicInteger();
        ScheduledFuture<?> future=(ScheduledFuture<?>)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ScheduledFuture.class},(p,m,a)->{
            if(m.getName().equals("cancel")){assertEquals(false,a[0]);return true;}return null;
        });
        ScheduledExecutorService executor=(ScheduledExecutorService)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ScheduledExecutorService.class},(p,m,a)->{
            if(m.getName().equals("execute")){work.add((Runnable)a[0]);return null;}
            if(m.getName().equals("schedule")){timers.add((Runnable)a[0]);return future;}
            throw new UnsupportedOperationException(m.getName());
        });
        Clip clip=(Clip)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Clip.class},(p,m,a)->{
            switch(m.getName()){case "getMicrosecondLength":return 2_800_000L;case "start":starts.incrementAndGet();return null;case "close":closes.incrementAndGet();return null;default:return null;}
        });
        PlacementRevealAudio.ClipPlayback playback=new PlacementRevealAudio.ClipPlayback(executor,()->{opened.incrementAndGet();return clip;});
        playback.play(0,65);playback.stop();while(!work.isEmpty())work.remove().run();assertEquals(0,opened.get());
        playback.play(900,65);work.remove().run();assertEquals(1,starts.get());
        playback.stop();assertEquals(0,closes.get());work.remove().run();assertEquals(1,closes.get());
        timers.remove().run();assertEquals(1,closes.get());
    }
}
