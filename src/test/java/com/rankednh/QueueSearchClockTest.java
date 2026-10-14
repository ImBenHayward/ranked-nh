package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class QueueSearchClockTest {
    private final QueueSearchClock clock=new QueueSearchClock();
    private static final String START="2026-10-14T12:00:00Z";
    private static final long EPOCH=java.time.Instant.parse(START).toEpochMilli();
    @Test public void reconnectUsesServerElapsedAndRefreshCannotMoveTheTimerBackwards(){
        clock.update("a",START,EPOCH+42_000,0);assertEquals(Long.valueOf(42),clock.seconds(0));
        assertEquals(Long.valueOf(44),clock.seconds(2_000_000_000L));
        clock.update("a",START,EPOCH+43_000,2_000_000_000L);
        assertEquals(Long.valueOf(44),clock.seconds(2_000_000_000L));
        assertEquals(Long.valueOf(45),clock.seconds(3_000_000_000L));
    }
    @Test public void newEntryResetsTheClockAndUnknownTimestampsNeverInventATime(){
        clock.update("a",START,EPOCH+60_000,0);
        clock.update("a","2026-10-14T12:01:00Z",EPOCH+60_000,10);
        assertEquals(Long.valueOf(0),clock.seconds(10));
        clock.update("b",START,EPOCH+5_000,20);assertEquals(Long.valueOf(5),clock.seconds(20));
        clock.clear();assertNull(clock.seconds(30));
        clock.update("a",null,EPOCH,40);assertNull(clock.seconds(40));
        clock.update("a","not a timestamp",EPOCH,50);assertNull(clock.seconds(50));
        clock.update("a",START,EPOCH-5000,60);assertEquals(Long.valueOf(0),clock.seconds(60));
    }
    @Test public void timerFormatsMinutesAndHoursWithoutClaimingAnEstimatedWait(){
        assertEquals("00:00",QueueSearchClock.format(0));assertEquals("00:59",QueueSearchClock.format(59));
        assertEquals("01:00",QueueSearchClock.format(60));assertEquals("59:59",QueueSearchClock.format(3599));
        assertEquals("1:00:00",QueueSearchClock.format(3600));
    }
}
