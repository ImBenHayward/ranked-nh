package com.rankednh;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaSetupTest
{
    // Synthetic fixtures exercise validation; these are deliberately not a default kit.
    static ArenaKit kit(int build,int spellbook,int pouch){
        List<ArenaKit.Slot> worn=new ArrayList<>(),inventory=new ArrayList<>();
        for(int i=0;i<11;i++)worn.add(new ArenaKit.Slot(100+i,1,"Worn "+i));
        for(int i=0;i<28;i++)inventory.add(new ArenaKit.Slot(200+i,1,"Item "+i));
        return new ArenaKit(241,build,spellbook,pouch,worn,inventory);
    }
    @Test public void suppliesDoNotPretendTheDuelBuildHasAlreadyBeenSelected(){
        ArenaSetup.View view=TestArenaKit.SETUP.describe(false,0,2,false,false,TestArenaKit.DEFAULT.kit(),null,null);
        assertTrue(view.message.contains("after challenging"));assertFalse(view.comparisonKnown);
    }
    @Test public void challengeSelectsBuildBeforeCheckingKits(){
        ArenaSetup.View wrong=TestArenaKit.SETUP.describe(true,0,2,false,false,kit(0,1,55),null,null);
        assertEquals("Choose Max/Med",wrong.heading);assertTrue(wrong.warning);
        ArenaSetup.View selected=TestArenaKit.SETUP.describe(true,1,0,false,false,kit(0,1,55),null,null);
        assertEquals("Confirm build",selected.heading);assertTrue(selected.message.contains("Confirm in-game"));
        ArenaSetup.View waiting=TestArenaKit.SETUP.describe(true,1,0,true,false,kit(0,1,55),null,null);
        assertTrue(waiting.message.contains("Waiting for your opponent"));
        assertEquals("Your kit is ready",TestArenaKit.SETUP.describe(true,2,0,true,false,TestArenaKit.DEFAULT.kit(),null,null).heading);
    }
    @Test public void matchingEditedKitsNeverBecomeADefaultVerification(){
        ArenaKit arbitrary=kit(0,1,55);
        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,arbitrary,arbitrary,null);
        assertEquals("Fix both kits",view.heading);assertTrue(view.warning);assertFalse(view.comparisonKnown);
        assertFalse(view.detail.contains("Copy Load-out"));
    }
    @Test public void checksSpellbookRunePouchAndQuantitiesRegardlessOfSlotPlacement(){
        ArenaKit reference=kit(0,1,55);
        assertEquals(List.of("Spellbook"),reference.differences(kit(0,0,55)));
        assertEquals(List.of("Rune pouch"),reference.differences(kit(0,1,56)));
        assertEquals(List.of("Build"),reference.differences(kit(1,1,55)));
        List<ArenaKit.Slot> changed=new ArrayList<>(reference.inventory);
        changed.set(2,new ArenaKit.Slot(202,2,"Same item; more of it"));
        ArenaKit larger=new ArenaKit(241,0,1,55,reference.equipment,changed);
        assertEquals(List.of("Items or quantities"),reference.differences(larger));
        java.util.Collections.swap(changed,4,5);
        assertEquals(List.of("Items or quantities"),reference.differences(new ArenaKit(241,0,1,55,reference.equipment,changed)));
        List<ArenaKit.Slot> worn=new ArrayList<>(reference.equipment);worn.set(3,new ArenaKit.Slot(900,1,"Different weapon"));
        assertEquals(List.of("Items or quantities"),reference.differences(new ArenaKit(241,0,1,55,worn,reference.inventory)));
    }
    @Test public void changesAfterConfirmationAreFlaggedBeforeAnOpponentComparison(){
        ArenaKit before=TestArenaKit.DEFAULT.kit(),after=new ArenaKit(241,0,1,1,before.equipment,before.inventory);
        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,true,after,null,before);
        assertEquals("Fix your kit",view.heading);assertEquals("Rune pouch",view.changes.get(0).location);assertTrue(view.warning);
        assertTrue(TestArenaKit.SETUP.describe(true,2,0,true,false,before,after,null).warning);
    }
    @Test public void missingIncompleteOrDifferentRevisionDataDoesNotMatch(){
        ArenaKit reference=kit(0,1,55);
        assertFalse(reference.differences(null).isEmpty());
        ArenaKit empty=new ArenaKit(241,0,1,55,List.of(),List.of());
        assertFalse(empty.complete());assertEquals(List.of("Kit data unavailable"),reference.differences(empty));
        assertEquals(List.of("Game version changed"),reference.differences(new ArenaKit(242,0,1,55,reference.equipment,reference.inventory)));
        assertEquals("Reading kit",TestArenaKit.SETUP.describe(true,2,0,true,false,null,reference,null).heading);
        assertEquals("Your kit is ready",TestArenaKit.SETUP.describe(true,2,0,true,false,TestArenaKit.DEFAULT.kit(),empty,null).heading);
        ArenaKit wrongRevision=new ArenaKit(242,0,1,0,TestArenaKit.DEFAULT.kit().equipment,TestArenaKit.DEFAULT.kit().inventory);
        assertFalse(TestArenaKit.SETUP.describe(true,2,0,true,false,wrongRevision,wrongRevision,null).comparisonKnown);
    }
}
