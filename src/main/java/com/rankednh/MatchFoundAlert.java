package com.rankednh;

import java.time.Instant;

/** A repeated poll, reconnect or world hop must not repeat the same matchup ping. */
final class MatchFoundAlert
{
    private String key;
    boolean take(Models.Status s,boolean accountMatches,long now){
        if(!accountMatches || s==null || s.player==null || s.match==null ||
            !"offered".equals(s.match.state) || s.match.accepted || secondsLeft(s,now)==0)return false;
        String next=s.player.id+":"+s.match.id;
        if(next.equals(key))return false;
        key=next;return true;
    }
    void reset(){key=null;}
    static int secondsLeft(Models.Status s,long serverNow){
        try{return (int)Math.max(0,Math.ceil((Instant.parse(s.match.deadline).toEpochMilli()-serverNow)/1000.0));}
        catch(Exception ignored){return 0;}
    }
    static String region(String region){
        return region==null?"Arena":"DEMO".equals(region)?"Local demo":region;
    }
}
