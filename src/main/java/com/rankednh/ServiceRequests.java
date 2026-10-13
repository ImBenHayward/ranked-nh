package com.rankednh;

import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

/** Serialize requests without disabling controls for background status polling. */
final class ServiceRequests {
    private static final class Job {
        final boolean action; final String label; final Runnable work;
        long started;
        Job(boolean action,String label,Runnable work){this.action=action;this.label=label;this.work=work;}
    }
    private final Executor executor;
    private final Runnable changed;
    private final LongSupplier clock;
    private Job running,queued;
    ServiceRequests(Executor executor,Runnable changed,LongSupplier clock){this.executor=executor;this.changed=changed;this.clock=clock;}
    synchronized boolean submit(boolean action,String label,Runnable work){
        Job job=new Job(action,label,work);
        if(running!=null){
            if(!action || running.action || queued!=null)return false;
            queued=job;
        }else{running=job;start(job);}
        changed.run();return true;
    }
    private void start(Job job){
        job.started=clock.getAsLong();
        executor.execute(()->{
            try{job.work.run();}
            finally{
                synchronized(this){
                    running=queued;queued=null;
                    if(running!=null)start(running);
                }
                changed.run();
            }
        });
    }
    synchronized boolean busy(){return running!=null;}
    synchronized boolean actionPending(){return queued!=null || running!=null && running.action;}
    synchronized String message(){
        if(queued!=null)return queued.label;
        if(running==null)return "";
        if(running.action)return running.label;
        return clock.getAsLong()-running.started>=1500?"Updating match status…":"";
    }
    synchronized void discardQueued(){queued=null;}
}
