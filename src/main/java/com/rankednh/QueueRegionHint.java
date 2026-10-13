package com.rankednh;

import java.time.Instant;

final class QueueRegionHint {
    static String preference(int seconds){
        return "Your region first. Other regions after "+Math.max(0,seconds)+" seconds.";
    }
    static String searching(Models.Status status,long now){
        if("DEMO".equals(status.queueRegion))return "Local simulation only.";
        try{
            long remaining=status.crossRegionAfterSeconds-Math.max(0,(now-Instant.parse(status.joinedAt).toEpochMilli())/1000);
            if(remaining<=0)return "Searching all regions. Your region still comes first.";
            return "Other regions in up to "+remaining+(remaining==1?" second.":" seconds.");
        }catch(RuntimeException unavailable){return preference(status.crossRegionAfterSeconds);}
    }
    private QueueRegionHint(){}
}
