package com.rankednh;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class DefaultArenaKitTest {
    private ArenaKit changedBody(int id){ArenaKit kit=TestArenaKit.DEFAULT.kit();ArrayList<ArenaKit.Slot> worn=new ArrayList<>(kit.equipment);worn.set(4,new ArenaKit.Slot(id,id>0?1:0,id>0?"Edited body":"Empty"));return new ArenaKit(241,0,1,0,worn,kit.inventory);}
    @Test public void bothPlayersMustMatchTheFixedReference(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();assertEquals(26243,kit.equipment.get(4).itemId);assertEquals(5000,kit.equipment.get(10).quantity);
        ArenaSetup.View view=ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null),"You have the same options");
        assertTrue(view.comparisonKnown);assertFalse(view.warning);assertSame(kit,view.opponentKit);
        for(int body:new int[]{-1,4712}){
            ArenaKit edited=changedBody(body);view=TestArenaKit.SETUP.describe(true,2,0,true,false,edited,edited,null);
            assertTrue(view.warning);assertFalse(view.comparisonKnown);assertEquals(body<0?2:4,view.changes.size());
            assertFalse(view.detail.contains("Copy Load-out"));assertTrue(view.changes.stream().anyMatch(c->c.opponentSide && c.explanation().contains("Default: Virtus robe top")));
        }
    }
    @Test public void copyingIsSuggestedOnlyWhenTheSourceMatchesDefault(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit(),edited=changedBody(-1);
        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,edited,kit,null);
        assertTrue(view.detail.contains("Copy Load-out"));assertFalse(view.changes.get(0).opponentSide);
        view=TestArenaKit.SETUP.describe(true,2,0,true,false,kit,edited,null);
        assertTrue(view.detail.contains("Your kit is correct"));assertTrue(view.changes.get(0).opponentSide);
        assertFalse(TestArenaKit.SETUP.describe(true,2,0,true,false,edited,null,null).detail.contains("Copy Load-out"));
    }
    @Test public void inventoryOrderIsFreeButQuantitySpellbookAndRuneExclusionsMustMatch(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();ArrayList<ArenaKit.Slot> inventory=new ArrayList<>(kit.inventory);
        java.util.Collections.swap(inventory,0,1);
        assertTrue(TestArenaKit.DEFAULT.matches(new ArenaKit(241,0,1,0,kit.equipment,inventory)));
        inventory=new ArrayList<>(kit.inventory);inventory.set(2,new ArenaKit.Slot(inventory.get(2).itemId,2,"More doses"));
        assertFalse(TestArenaKit.DEFAULT.matches(new ArenaKit(241,0,1,0,kit.equipment,inventory)));
        assertFalse(TestArenaKit.DEFAULT.matches(new ArenaKit(241,0,0,0,kit.equipment,kit.inventory)));
        assertFalse(TestArenaKit.DEFAULT.matches(new ArenaKit(241,0,1,1,kit.equipment,kit.inventory)));
        assertFalse(TestArenaKit.DEFAULT.matches(new ArenaKit(242,0,1,0,kit.equipment,kit.inventory)));
        assertFalse(TestArenaKit.DEFAULT.matches(new ArenaKit(241,1,1,0,kit.equipment,kit.inventory)));
    }
    @Test public void roundEvidenceIsOnlyAvailableAfterCheckedBattleEntryAndCannotSurviveANewNonce(){
        Models.Status status=MatchupTest.status();status.match.state="active";ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);
        ArenaKit kit=TestArenaKit.DEFAULT.kit();ArenaSetup.View view=ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null),"same options");
        guard.observe(status,view,false,true);assertNull(guard.evidence());guard.observe(status,null,true,true);
        assertNotNull(guard.evidence());assertEquals(TestArenaKit.DEFAULT.id(),guard.evidence().referenceId);
        status.match.nonce="changed";guard.observe(status,null,true,true);assertTrue(guard.blocked());assertNull(guard.evidence());
        guard.reset();guard.observe(status,view,false,true);guard.invalidateSetup();guard.observe(status,null,true,true);assertTrue(guard.blocked());
    }
    @Test public void laterStagingEditsInvalidateTheRoundWithoutCheckingLiveWornItems(){
        Models.Status status=MatchupTest.status();status.match.state="active";ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);ArenaKit kit=TestArenaKit.DEFAULT.kit();
        ArenaSetup.View view=ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null),"same options");
        guard.observe(status,view,false,true);guard.gameState(net.runelite.api.GameState.LOADING);guard.observe(status,null,true,true);guard.checkConfigured(kit);assertFalse(guard.blocked());
        guard.checkConfigured(changedBody(-1));assertEquals("kit",guard.code());assertNull(guard.evidence());
    }
}
