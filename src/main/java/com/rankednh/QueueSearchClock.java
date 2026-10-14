package com.rankednh;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Anchors elapsed time to the server queue entry, then advances without network requests. */
final class QueueSearchClock {
    private String entry;
    private long elapsedMillis,sampledNanos;
    void update(String player,String joinedAt,long serverNow,long nowNanos){
        long joined;
        try{joined=Instant.parse(joinedAt).toEpochMilli();}catch(RuntimeException unavailable){clear();return;}
        String next=player+"\n"+joinedAt;
        long elapsed=Math.max(0,serverNow-joined);
        if(Objects.equals(entry,next))elapsed=Math.max(elapsed,millis(nowNanos));
        entry=next;elapsedMillis=elapsed;sampledNanos=nowNanos;
    }
    Long seconds(long nowNanos){return entry==null?null:millis(nowNanos)/1000;}
    private long millis(long nowNanos){return elapsedMillis+Math.max(0,(nowNanos-sampledNanos)/1_000_000);}
    void clear(){entry=null;elapsedMillis=0;sampledNanos=0;}
    static String format(long seconds){
        return seconds<3600?String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60):
            String.format(Locale.ROOT,"%d:%02d:%02d",seconds/3600,seconds/60%60,seconds%60);
    }
}
