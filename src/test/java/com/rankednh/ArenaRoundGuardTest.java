package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaRoundGuardTest {
    private Models.Status active(){Models.Status s=MatchupTest.status();s.match.state="active";return s;}
    private ArenaSetup.View matched(){ArenaKit kit=TestArenaKit.DEFAULT.kit();return ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null),"You have the same options");}
    @Test public void knownWarningSurvivesClosingTheDialogAndStopsRoundOnEntry(){
        Models.Status s=active();ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);
        ArenaSetup.View warning=new ArenaSetup.View("Kits differ","Body armour differs","",true,null);
        guard.observe(s,warning,false,true);assertFalse(guard.blocked());
        guard.observe(s,null,false,true);guard.observe(s,null,true,true);
        assertTrue(guard.blocked());assertEquals("kit",guard.code());assertEquals("Body armour differs",guard.reason());
        s.match.state="review";guard.observe(s,null,true,true);assertTrue(guard.blocked());
        s.match.id="new-match";s.match.state="active";guard.observe(s,matched(),false,true);assertFalse(guard.blocked());
        guard.observe(s,null,true,true);assertFalse(guard.blocked());
    }
    @Test public void correctingTheKitBeforeEntryClearsTheWarningAndAppliedStatsAreChecked(){
        ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);Models.Status s=active();
        guard.observe(s,new ArenaSetup.View("Kits differ","","",true,null),false,false);
        guard.observe(s,matched(),false,false);guard.observe(s,null,true,true);assertFalse(guard.blocked());
        s.match.round++;guard.observe(s,matched(),false,true);guard.observe(s,null,true,false);
        assertEquals("build",guard.code());
    }
    @Test public void missingOrStaleSetupCannotBeReusedForANewRound(){
        ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);Models.Status s=active();
        guard.observe(s,matched(),false,true);s.match.round++;guard.observe(s,null,true,true);
        assertTrue(guard.blocked());assertEquals("unchecked",guard.code());
        FightRecorder recorder=new FightRecorder();s.match.nonce="round";recorder.arm(s);
        recorder.recordHit("self",40,1);recorder.stopForSetup(guard.reason());recorder.recordDeath("self",3);
        assertNull(recorder.finishAt(8));
    }
}
