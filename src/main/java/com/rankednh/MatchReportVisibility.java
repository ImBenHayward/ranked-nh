package com.rankednh;

/** Polling the same result cannot restart its timer or undo dismissal. */
final class MatchReportVisibility
{
    private final java.util.function.Predicate<String> seen;
    private final java.util.function.Consumer<String> remember;
    MatchReportVisibility(){this(key->false,key->{});}
    MatchReportVisibility(java.util.function.Predicate<String> seen,java.util.function.Consumer<String> remember){this.seen=seen;this.remember=remember;}
    private String key;
    private long firstSeen;
    private boolean dismissed,manual;
    synchronized boolean visible(Models.Status status,long now,int seconds) {
        return visible(status,"",now,seconds);
    }
    synchronized boolean visible(Models.Status status,String service,long now,int seconds) {
        if(!MatchReportView.reportable(status) || status.queued)return false;
        String next=service+":"+status.player.id+":"+status.match.id+":"+status.match.state;
        // Review and void are status updates, not end-of-match celebrations. Open only on request.
        if(!"complete".equals(status.match.state) && !manual && !next.equals(key))return false;
        if(!next.equals(key)){
            key=next;firstSeen=now;dismissed=!manual && seen.test(key);manual=false;
            if(!dismissed)remember.accept(key);
        }
        return !dismissed && (seconds==0 || now-firstSeen<seconds*1000L);
    }
    synchronized void dismiss(){dismissed=true;}
    synchronized void reopen(){key=null;dismissed=false;manual=true;}
    synchronized void reset(){key=null;dismissed=false;manual=false;}
    synchronized void connected(Models.Status status,String service){
        // Reconnecting is not a new match event. Older results remain available through Show match report.
        if(MatchReportView.reportable(status)){
            String result=service+":"+status.player.id+":"+status.match.id+":"+status.match.state;
            remember.accept(result);key=result;dismissed=true;manual=false;
        }
    }
}
