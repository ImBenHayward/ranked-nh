package com.rankednh;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class ServiceRequestsTest {
    @Test public void pollingLeavesControlsEnabledAndPreservesAClickUntilItCanRun(){
        ArrayDeque<Runnable> tasks=new ArrayDeque<>();List<String> events=new ArrayList<>();AtomicLong now=new AtomicLong();
        ServiceRequests requests=new ServiceRequests(tasks::add,()->{},now::get);
        assertTrue(requests.submit(false,"",()->events.add("poll")));
        assertTrue(requests.busy());assertFalse(requests.actionPending());assertEquals("",requests.message());
        now.set(1500);assertEquals("Updating match status…",requests.message());
        assertTrue(requests.submit(true,"Joining the queue…",()->events.add("join")));
        assertTrue(requests.actionPending());assertEquals("Joining the queue…",requests.message());
        assertFalse(requests.submit(false,"",()->fail("Repeated poll must be coalesced")));
        assertFalse(requests.submit(true,"",()->fail("Duplicate click must not execute")));
        assertEquals(1,tasks.size());tasks.remove().run();assertEquals(List.of("poll"),events);
        assertTrue(requests.actionPending());tasks.remove().run();assertEquals(List.of("poll","join"),events);
        assertFalse(requests.busy());assertFalse(requests.actionPending());assertEquals("",requests.message());
    }
    @Test public void failedRequestReleasesControlsAndDoesNotLoseTheQueuedAction(){
        ArrayDeque<Runnable> tasks=new ArrayDeque<>();List<String> events=new ArrayList<>();
        ServiceRequests requests=new ServiceRequests(tasks::add,()->{},()->0L);
        requests.submit(false,"",()->{throw new RuntimeException("timeout");});requests.submit(true,"Join",()->events.add("join"));
        try{tasks.remove().run();fail();}catch(RuntimeException expected){}
        assertTrue(requests.actionPending());tasks.remove().run();assertEquals(List.of("join"),events);assertFalse(requests.busy());
        requests.submit(true,"Leave",()->{throw new RuntimeException("offline");});
        try{tasks.remove().run();fail();}catch(RuntimeException expected){}
        assertFalse(requests.actionPending());assertTrue(requests.submit(false,"",()->{}));
    }
    @Test public void changingServiceDropsQueuedActionsWithoutStartingParallelRequests(){
        ArrayDeque<Runnable> tasks=new ArrayDeque<>();ServiceRequests requests=new ServiceRequests(tasks::add,()->{},()->0L);
        requests.submit(false,"",()->{});requests.submit(true,"Old server action",()->fail());requests.discardQueued();
        assertFalse(requests.actionPending());assertTrue(requests.busy());tasks.remove().run();assertTrue(tasks.isEmpty());assertFalse(requests.busy());
    }
}
