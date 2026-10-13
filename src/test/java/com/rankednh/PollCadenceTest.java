package com.rankednh;
import org.junit.Test;
import static org.junit.Assert.*;
public class PollCadenceTest {
    @Test public void idleBackoffDoesNotDelayQueueOrReportHeartbeats(){
        PollCadence cadence=new PollCadence();Models.Status s=new Models.Status();
        assertTrue(cadence.due(s,true,false,0));assertFalse(cadence.due(s,true,false,3000));
        s.queued=true;assertTrue(cadence.due(s,true,false,3000));assertFalse(cadence.due(s,true,false,5999));
        assertTrue(cadence.due(s,true,false,6000));s.queued=false;
        assertFalse(cadence.due(s,true,false,9000));assertTrue(cadence.due(s,true,true,9000));
        assertFalse(cadence.due(s,false,false,24000));assertTrue(cadence.due(s,false,false,39000));
        cadence.reset();assertTrue(cadence.due(s,false,false,39001));
    }
}
