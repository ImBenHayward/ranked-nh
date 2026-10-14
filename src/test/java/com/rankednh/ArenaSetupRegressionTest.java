package com.rankednh;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaSetupRegressionTest {
    @Test public void realBodyRemovalSnapshotIsAnAllowedOmission(){
        ArenaKit removed=new Gson().fromJson(new InputStreamReader(getClass().getResourceAsStream("body-removed-kit.json"),StandardCharsets.UTF_8),ArenaKit.class);
        assertEquals(-1,removed.equipment.get(4).itemId);
        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,removed,removed,null);
        assertFalse(view.warning);assertTrue(view.comparisonKnown);assertTrue(view.changes.isEmpty());
        assertFalse(ArenaSetup.withGameCheck(view,"Your load-outs do not match.").warning);
    }
    @Test public void nhMatchStaysReadyRegardlessOfNativeComparisonText(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();
        ArenaSetup.View local=TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null);
        for(String text:new String[]{"Your opponent's load-out <col=ff0000>does not match</col> yours.",
            "Your kits do not match.","","You and your opponent have the same options.",
            "This load-out contains too many of certain items.","The build will not permit these items.","Ammo cannot be fired."}){
            ArenaSetup.View result=ArenaSetup.withGameCheck(local,text);
            assertSame(local,result);assertFalse(result.warning);assertTrue(result.comparisonKnown);
            assertEquals("Both kits ready",result.heading);assertSame(kit,result.opponentKit);
        }
    }
    @Test public void nativeTextNeverApprovesUnknownDataOrOverridesSpecificNhDifferences(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit(),changed=ArenaSetupTest.kit(0,1,56);
        ArenaSetup.View local=TestArenaKit.SETUP.describe(true,2,0,true,false,kit,changed,null);
        for(String text:new String[]{"same options","does not match","too many items"}){
            assertSame(local,ArenaSetup.withGameCheck(local,text));assertTrue(local.warning);assertFalse(local.comparisonKnown);
            assertFalse(local.changes.isEmpty());
        }
        ArenaSetup.View pending=TestArenaKit.SETUP.describe(true,2,0,true,false,kit,null,null);
        assertFalse(ArenaSetup.withGameCheck(pending,"same options").comparisonKnown);
        ArenaSetup.View waiting=ArenaSetup.withGameCheck(pending,"does not match");
        assertEquals("Check opponent's kit",waiting.heading);assertFalse(waiting.comparisonKnown);
        assertTrue(ArenaSetup.withGameCheck(pending,"This load-out contains too many of certain items.").warning);
    }
    @Test public void nativeMismatchDoesNotStopRoundRecordingWhenNhHasVerifiedBothKits(){
        Models.Status status=MatchupTest.status();status.match.state="active";
        ArenaKit own=KitComparisonContractTest.rearranged(),opponent=TestArenaKit.DEFAULT.kit();
        ArenaSetup.View view=ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,true,own,opponent,null),"Your opponent's load-out does not match yours.");
        ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);guard.observe(status,view,false,true);guard.observe(status,null,true,true);guard.checkConfigured(own);
        assertFalse(guard.blocked());assertNotNull(guard.evidence());
        assertSame(own,guard.evidence().own);assertSame(opponent,guard.evidence().opponent);
    }
}
