package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class AutoReadyTest
{
    private Models.Status preparing(){Models.Status s=MatchupTest.status();s.match.state="preparing";return s;}
    private boolean take(AutoReady ready,Models.Status s,long now){return ready.take(s,true,false,558,true,true,99,now,0);}
    @Test public void waitsForStableArrivalAndRetriesOnlyUntilServerAcknowledges(){
        AutoReady ready=new AutoReady();Models.Status s=preparing();
        assertFalse(take(ready,s,0));assertFalse(take(ready,s,600));assertTrue(take(ready,s,1200));
        assertFalse(take(ready,s,1800));assertFalse(take(ready,s,6199));assertTrue(take(ready,s,6200));
        s.match.ready=true;assertFalse(take(ready,s,12000));
        s.match.ready=false;s.match.round=2;assertFalse(take(ready,s,12600));assertTrue(take(ready,s,13800));
    }
    @Test public void wrongAccountWorldDeadMissingOpponentAndExpiredOffersNeverCheckIn(){
        Models.Status s=preparing();
        for(int failure=0;failure<6;failure++){
            AutoReady ready=new AutoReady();
            for(long now:new long[]{0,1800,8000})assertFalse(ready.take(s,failure!=0,false,failure==1?301:558,
                failure!=2,failure!=3,failure==4?0:99,now,failure==5?60000:0));
        }
        s.mode="demo";AutoReady ready=new AutoReady();assertFalse(take(ready,s,0));assertFalse(take(ready,s,1800));
    }
    @Test public void sceneLossRestartsArrivalCheckAndBusyRequestsDoNotConsumeAttempt(){
        AutoReady ready=new AutoReady();Models.Status s=preparing();assertFalse(take(ready,s,0));
        assertFalse(ready.take(s,true,false,558,true,false,99,600,0));
        assertFalse(take(ready,s,1200));
        assertFalse(ready.take(s,true,true,558,true,true,99,2400,0));
        assertTrue(take(ready,s,3000));
        s.match.state="active";assertFalse(take(ready,s,10000));
    }
    @Test public void arrivalCanQueueBehindABackgroundPollAndExplainsWaitingForAcknowledgement(){
        java.util.List<Runnable> pending=new java.util.ArrayList<>();ServiceRequests requests=new ServiceRequests(pending::add,()->{},()->0);
        requests.submit(false,"",()->{});assertTrue(requests.busy());assertFalse(requests.actionPending());
        AutoReady ready=new AutoReady();Models.Status s=preparing();
        assertFalse(ready.take(s,true,requests.actionPending(),558,true,true,99,0,0));
        assertTrue(ready.take(s,true,requests.actionPending(),558,true,true,99,1200,0));assertTrue(ready.awaitingAcknowledgement(s));
        assertTrue(requests.submit(true,"Checking arrival…",()->{}));assertTrue(requests.actionPending());
        pending.remove(0).run();pending.remove(0).run();assertFalse(requests.busy());
        s.match.ready=true;assertFalse(ready.awaitingAcknowledgement(s));s.match.ready=false;s.match.round=2;assertFalse(ready.awaitingAcknowledgement(s));
    }
}
