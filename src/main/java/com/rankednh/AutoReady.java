package com.rankednh;

/** Checks arrival only. It never chooses gear, issues a challenge or starts combat. */
final class AutoReady
{
    private String key;
    private long arrivedAt=-1,nextAttempt;
    static String blocker(boolean nearby,int hitpoints){
        if(!nearby)return "Move into view of your opponent.";
        if(hitpoints<=0)return "Waiting for you to respawn.";
        return "";
    }
    boolean take(Models.Status s,boolean accountMatches,boolean busy,int world,boolean arena,
        boolean nearby,int hitpoints,long now,long serverNow){
        String current=s==null || s.match==null?null:s.player.id+":"+s.match.id+":"+s.match.round;
        if(current==null || !current.equals(key)){reset();key=current;}
        if(!accountMatches || s==null || s.match==null || !"pilot".equals(s.mode) ||
            !"preparing".equals(s.match.state) || s.match.ready || world!=s.match.world || !arena ||
            !blocker(nearby,hitpoints).isEmpty() || MatchFoundAlert.secondsLeft(s,serverNow)==0){
            arrivedAt=-1;return false;
        }
        if(arrivedAt<0)arrivedAt=now;
        // Let scene loading settle. A lost HTTP response can retry the idempotent endpoint.
        if(busy || now-arrivedAt<1200 || now<nextAttempt)return false;
        nextAttempt=now+5000;return true;
    }
    void reset(){key=null;arrivedAt=-1;nextAttempt=0;}
    boolean awaitingAcknowledgement(Models.Status s){return s!=null && s.player!=null && s.match!=null &&
        (s.player.id+":"+s.match.id+":"+s.match.round).equals(key) && nextAttempt>0 && !s.match.ready;}
}
