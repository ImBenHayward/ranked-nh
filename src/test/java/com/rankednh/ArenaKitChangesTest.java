package com.rankednh;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaKitChangesTest {
    @Test public void changesIdentifyTheActualItemsQuantitiesAndRuneSelection(){
        ArenaKit a=ArenaSetupTest.kit(0,1,1);ArrayList<ArenaKit.Slot> worn=new ArrayList<>(a.equipment),bag=new ArrayList<>(a.inventory);
        worn.set(4,new ArenaKit.Slot(4712,1,"Ahrim's robetop"));bag.set(7,new ArenaKit.Slot(11212,5000,"Dragon arrow"));
        ArenaKit b=new ArenaKit(241,0,0,2,worn,bag);a.runeSelection="Blood rune, Death rune";b.runeSelection="Air rune";
        java.util.List<ArenaKit.Change> changes=a.changes(b);
        assertEquals(6,changes.size());assertEquals("Ahrim's robetop",changes.get(0).theirs);assertEquals("Missing",changes.get(0).yours);
        assertEquals("Dragon arrow × 5000",changes.get(1).theirs);
        assertEquals("Not required",changes.get(2).theirs);assertEquals(4,changes.get(2).equipment);
        assertEquals("Not required",changes.get(3).theirs);assertEquals(7,changes.get(3).inventory);
        assertEquals("Ancient Magicks",changes.get(4).yours);assertEquals("Standard",changes.get(4).theirs);
        assertEquals("Blood rune, Death rune",changes.get(5).yours);assertEquals("Air rune",changes.get(5).theirs);
    }
    @Test public void missingMovedGearMarksTheEmptyBagSlotAndNotTheCorrectWornSwitch(){
        ArenaKit rearranged=KitComparisonContractTest.rearranged();ArrayList<ArenaKit.Slot> bag=new ArrayList<>(rearranged.inventory);
        assertEquals(26243,bag.get(16).itemId);bag.set(16,new ArenaKit.Slot(-1,0,"Empty"));
        ArenaKit missing=new ArenaKit(241,0,1,0,rearranged.equipment,bag);
        java.util.List<ArenaKit.Change> changes=missing.defaultChanges(TestArenaKit.DEFAULT.kit(),false);
        assertEquals(1,changes.size());ArenaKit.Change change=changes.get(0);
        assertEquals("Virtus robe top",change.theirs);assertEquals(-1,change.equipment);assertEquals(16,change.inventory);
    }
    @Test public void repeatedItemsProduceOneQuantityFixAndExtrasPointAtTheirActualPosition(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();ArrayList<ArenaKit.Slot> bag=new ArrayList<>(kit.inventory);
        bag.set(4,new ArenaKit.Slot(-1,0,"Empty"));bag.set(5,new ArenaKit.Slot(-1,0,"Empty"));
        ArenaKit missing=new ArenaKit(241,0,1,0,kit.equipment,bag);
        assertEquals(1,missing.defaultChanges(TestArenaKit.DEFAULT.kit(),false).size());assertEquals("Anglerfish × 8",missing.defaultChanges(TestArenaKit.DEFAULT.kit(),false).get(0).yours);
        assertEquals("Anglerfish × 10",missing.defaultChanges(TestArenaKit.DEFAULT.kit(),false).get(0).theirs);
        bag=new ArrayList<>(kit.inventory);bag.set(19,new ArenaKit.Slot(4712,1,"Ahrim's robetop"));
        ArenaKit edited=new ArenaKit(241,0,1,0,kit.equipment,bag);
        ArenaKit.Change extra=edited.defaultChanges(TestArenaKit.DEFAULT.kit(),false).stream().filter(c->c.yours.equals("Ahrim's robetop")).findFirst().get();
        assertEquals(19,extra.inventory);assertEquals(-1,extra.equipment);assertEquals("Not required",extra.theirs);
    }
}
