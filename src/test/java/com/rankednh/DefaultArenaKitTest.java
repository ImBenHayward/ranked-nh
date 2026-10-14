package com.rankednh;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class DefaultArenaKitTest {
    private ArenaKit changedBody(int id){ArenaKit kit=TestArenaKit.DEFAULT.kit();ArrayList<ArenaKit.Slot> worn=new ArrayList<>(kit.equipment);worn.set(4,new ArenaKit.Slot(id,id>0?1:0,id>0?"Edited body":"Empty"));return new ArenaKit(241,0,1,0,worn,kit.inventory);}
    @Test public void eachItemCanBeOmittedAndPlayersCanUseDifferentSubsets(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();
        for(int i=0;i<39;i++){
            ArrayList<ArenaKit.Slot> all=new ArrayList<>(kit.equipment);all.addAll(kit.inventory);
            all.set(i,new ArenaKit.Slot(-1,0,"Empty"));
            ArenaKit subset=new ArenaKit(241,0,1,0,all.subList(0,11),all.subList(11,39));
            assertTrue(TestArenaKit.DEFAULT.matches(subset));assertTrue(subset.defaultChanges(kit,false).isEmpty());
            ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,subset,kit,null);
            assertFalse(view.warning);assertTrue(view.comparisonKnown);
            assertTrue(TestArenaKit.SETUP.describe(true,2,0,true,false,kit,subset,null).comparisonKnown);
        }
    }
    @Test public void matchingForbiddenKitsStillFailAndOnlyActualItemsAreMarked(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit(),edited=changedBody(4712);
        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,edited,edited,null);
        assertTrue(view.warning);assertFalse(view.comparisonKnown);assertEquals(2,view.changes.size());
        assertFalse(view.detail.contains("Copy Load-out"));
        assertTrue(view.changes.stream().allMatch(c->c.equipment==4 && c.explanation().contains("Not allowed")));
        view=TestArenaKit.SETUP.describe(true,2,0,true,false,kit,edited,null);
        assertEquals("Opponent needs to fix their kit",view.heading);assertTrue(view.changes.get(0).opponentSide);
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
        guard.checkConfigured(changedBody(-1));assertFalse(guard.blocked());
        guard.checkConfigured(changedBody(4712));assertEquals("kit",guard.code());assertNull(guard.evidence());
    }
}
