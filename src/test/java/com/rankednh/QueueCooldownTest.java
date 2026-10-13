package com.rankednh;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueueCooldownTest {
    private final Models.Status status=new Models.Status();
    private QueueCooldown at(String now,String zone){return QueueCooldown.from(status,Instant.parse(now).toEpochMilli(),ZoneId.of(zone));}
    @Test public void screenshotTimestampUsesLocalDayTimeAndExplainsTheActualReason(){
        status.cooldownUntil="2026-10-12T23:34:08.072Z";status.cooldownReason="cancelled";
        QueueCooldown cooldown=at("2026-10-12T23:31:22.337Z","Europe/London");
        assertEquals("Available in 2m 46s",cooldown.remaining);
        assertEquals("Today at 00:34 BST\nCancelled before the first fight.",cooldown.detail);
        assertFalse(cooldown.detail.contains("2026-10-12T"));
        cooldown=at("2026-10-12T23:31:22.337Z","America/New_York");assertTrue(cooldown.detail.startsWith("Today at 19:34 EDT"));
    }
    @Test public void midnightDstAndFutureDatesRemainUnambiguous(){
        status.cooldownUntil="2026-10-12T23:01:00Z";
        assertTrue(at("2026-10-12T22:59:00Z","Europe/London").detail.startsWith("Tomorrow at 00:01 BST"));
        status.cooldownUntil="2026-10-25T01:30:00Z";
        assertTrue(at("2026-10-25T00:59:00Z","Europe/London").detail.startsWith("Today at 01:30 GMT"));
        status.cooldownUntil="2027-01-01T00:00:00Z";
        assertTrue(at("2026-12-29T23:59:00Z","Europe/London").detail.startsWith("1 Jan 2027 at 00:00 GMT"));
    }
    @Test public void onlyTheServerClearsCooldownAndBadDatesNeverLeakRawText(){
        status.cooldownUntil="2026-10-12T23:34:08.072Z";
        assertEquals("Available in 1s",at("2026-10-12T23:34:08Z","UTC").remaining);
        assertEquals("Checking queue availability…",at("2026-10-12T23:34:09Z","UTC").remaining);
        status.cooldownUntil="bad timestamp";
        assertEquals("Checking queue availability…",at("2026-10-12T23:34:09Z","UTC").remaining);
        status.cooldownUntil=null;assertNull(at("2026-10-12T23:34:09Z","UTC"));
    }
    @Test public void knownReasonsAreDistinctAndUnknownReasonsAreNotGuessed(){
        status.cooldownUntil="2026-10-12T23:34:08Z";
        for(String code:new String[]{"declined","missed_accept","no_show",null,"future"}){
            status.cooldownReason=code;QueueCooldown cooldown=at("2026-10-12T23:33:08Z","UTC");
            assertEquals("Available in 1m 0s",cooldown.remaining);
            String expected="declined".equals(code)?"Match offer declined.":"missed_accept".equals(code)?"Match offer wasn't accepted in time.":
                "no_show".equals(code)?"Didn't arrive before the match deadline.":"Temporary matchmaking cooldown.";
            assertTrue(cooldown.detail.endsWith(expected));
        }
    }
}
