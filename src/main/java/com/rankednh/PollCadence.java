package com.rankednh;

/** Idle clients back off; joining a queue immediately restores the normal heartbeat cadence. */
final class PollCadence {
    private boolean polled;
    private long last;
    synchronized boolean due(Models.Status status,boolean loggedIn,boolean pending,long now){
        boolean active=status==null || status.queued || status.match!=null && status.match.isOpen();
        long interval=pending || active?3000:loggedIn?15000:30000;
        if(polled && now-last<interval)return false;
        last=now;polled=true;return true;
    }
    synchronized void reset(){polled=false;}
}
