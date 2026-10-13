package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class QueueRegionHintTest {
    @Test public void secondsDoNotRoundToZeroMinutesAndCountdownEndsInAllRegions(){
        Models.Status status=new Models.Status();status.queueRegion="EU";status.crossRegionAfterSeconds=30;status.joinedAt="1970-01-01T00:00:00Z";
        assertEquals("Your region first. Other regions after 30 seconds.",QueueRegionHint.preference(30));
        assertEquals("Other regions in up to 30 seconds.",QueueRegionHint.searching(status,0));
        assertEquals("Other regions in up to 1 second.",QueueRegionHint.searching(status,29_000));
        assertEquals("Searching all regions. Your region still comes first.",QueueRegionHint.searching(status,30_000));
        status.joinedAt=null;assertEquals(QueueRegionHint.preference(30),QueueRegionHint.searching(status,0));
    }
}
