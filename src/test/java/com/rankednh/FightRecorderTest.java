package com.rankednh;

import net.runelite.api.GameState;
import org.junit.Test;
import static org.junit.Assert.*;

public class FightRecorderTest
{
    private Models.Status active(String nonce) {
        Models.Status s=new Models.Status();s.player=new Models.Player();s.player.id="self-id";s.player.rsn="My Main";
        s.match=new Models.Match();s.match.state="active";s.match.round=1;s.match.world=999;s.match.nonce=nonce;
        s.match.opponent=new Models.Opponent();s.match.opponent.id="opponent-id";s.match.opponent.rsn="Other Main";return s;
    }
    @Test public void validatesAppliedBuildStatsRatherThanBoostedOrLobbyStats(){
        net.runelite.api.Client max=(net.runelite.api.Client)java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.Client.class.getClassLoader(),new Class<?>[]{net.runelite.api.Client.class},
            (proxy,method,args)->method.getName().equals("getRealSkillLevel")?99:null);
        assertTrue(FightRecorder.maxStats(max));
        net.runelite.api.Client med=(net.runelite.api.Client)java.lang.reflect.Proxy.newProxyInstance(
            net.runelite.api.Client.class.getClassLoader(),new Class<?>[]{net.runelite.api.Client.class},
            (proxy,method,args)->method.getName().equals("getRealSkillLevel")?(args[0]==net.runelite.api.Skill.DEFENCE?70:99):null);
        assertFalse(FightRecorder.maxStats(med));
    }
    @Test public void deathProducesOneReportAfterAnObservationWindow(){
        FightRecorder r=new FightRecorder();r.arm(active("round-1"));r.recordHit("opponent",40,100);r.recordDeath("opponent",105);
        assertNull(r.finishAt(107));Models.Report result=r.finishAt(108);
        assertEquals("self-id",result.winner);assertEquals(8,result.durationTicks);assertEquals(1,result.hits.size());assertNull(r.finishAt(109));
    }
    @Test public void simultaneousDeathsAreDraws(){
        FightRecorder r=new FightRecorder();r.arm(active("round-1"));r.recordHit("self",40,100);r.recordDeath("opponent",105);r.recordDeath("self",106);
        Models.Report result=r.finishAt(108);assertNull(result.winner);assertEquals("double-death",result.endReason);
    }
    @Test public void disconnectNeverInventsAnOutcome(){
        FightRecorder r=new FightRecorder();r.arm(active("round-1"));r.recordHit("self",40,100);r.recordDeath("opponent",105);
        r.gameState(GameState.CONNECTION_LOST);assertNull(r.finishAt(200));assertTrue(r.message().contains("interrupted"));
    }
    @Test public void statusPollingDoesNotResetEvidenceAndNewRoundDoes(){
        FightRecorder r=new FightRecorder();Models.Status s=active("round-1");r.arm(s);r.recordHit("self",40,100);r.arm(s);r.recordDeath("self",105);
        assertEquals("opponent-id",r.finishAt(108).winner);
        r.arm(active("round-2"));assertNull(r.finishAt(200));r.recordHit("opponent",20,201);r.recordDeath("opponent",205);
        Models.Report result=r.finishAt(208);assertEquals("round-2",result.nonce);assertEquals(1,result.hits.size());
    }
    @Test public void unrelatedActorsAndUnarmedRoundsDoNotProduceEvidence(){
        FightRecorder r=new FightRecorder();r.recordHit("self",20,1);r.recordDeath("self",2);assertNull(r.finishAt(9));
        r.arm(active("round-1"));r.recordHit("unrelated",40,100);r.recordDeath("unrelated",105);assertNull(r.finishAt(200));
    }
}
