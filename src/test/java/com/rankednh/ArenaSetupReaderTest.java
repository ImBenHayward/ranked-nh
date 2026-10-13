package com.rankednh;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.function.BiFunction;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaSetupReaderTest {
    @SuppressWarnings("unchecked") private static <T> T proxy(Class<T> type,BiFunction<String,Object[],Object> call){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,args)->call.apply(m.getName(),args));
    }
    private int revision=241,world=558,pouch=0;
    private boolean duel=true,arena=true;
    private final Map<Integer,Integer> vars=new HashMap<>();
    private final Map<Integer,String> texts=new HashMap<>();
    private final Map<Integer,Widget> widgets=new HashMap<>();
    private ItemContainer transmitted;
    public ArenaSetupReaderTest(){
        ArenaKit kit=TestArenaKit.DEFAULT.kit();
        int[] inventoryBits={14032,14033,14034,14036,14037,14038,14040,14041,14042,14044,14045,14046,14047,14048,14049,14050,14051,14052,14053,14054,14055,14056,14057,14058,14059,14060,14061,14062};
        for(int i=0;i<11;i++)vars.put(14063+i,kit.equipment.get(i).itemId);
        for(int i=0;i<28;i++)vars.put(inventoryBits[i],kit.inventory.get(i).itemId);
        vars.put(14035,1);vars.put(14031,1);vars.put(14028,2);
    }
    private final Client client=proxy(Client.class,(method,args)->{
        switch(method){
            case "getRevision":return revision;
            case "getGameState":return GameState.LOGGED_IN;
            case "getWorld":return world;
            case "getWorldType":return arena?EnumSet.of(WorldType.PVP_ARENA):EnumSet.noneOf(WorldType.class);
            case "getWidget":if(widgets.containsKey((int)args[1]))return widgets.get((int)args[1]);
                return duel && (int)args[0]==757 && ((int)args[1]==0 || texts.containsKey((int)args[1]))?
                proxy(Widget.class,(m,a)->m.equals("isHidden")?false:texts.get((int)args[1])):null;
            case "getVarbitValue":return vars.getOrDefault((int)args[0],0);
            case "getVarpValue":return pouch;
            case "getEnum":return proxy(EnumComposition.class,(m,a)->(int)args[0]==982?554+(int)a[0]:(int)a[0]);
            case "getItemDefinition":return proxy(ItemComposition.class,(m,a)->m.equals("getName")?"Item "+args[0]:m.equals("isStackable") && ((int)args[0]==21932 || (int)args[0]==1002));
            case "getItemContainer":return transmitted;
            default:throw new AssertionError("Unexpected client read: "+method);
        }
    });
    private Models.Status status(){Models.Status s=MatchupTest.status();s.match.state="active";return s;}
    @Test public void readsTheSelectedSuppliesRatherThanThePlayersCurrentInventory(){
        vars.put(14032,1002);vars.put(14063,1002);pouch=55;
        ArenaSetup.View view=new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false);
        assertEquals("Fix your kit",view.heading);
        assertEquals(241,view.kit.revision);assertEquals(11,view.kit.equipment.size());assertEquals(28,view.kit.inventory.size());
        assertEquals(5000,view.kit.inventory.get(0).quantity);assertEquals(1002,view.kit.equipment.get(0).itemId);
        assertEquals(1,view.kit.spellbook);assertEquals(55,view.kit.runePouch);
    }
    @Test public void closesChecksForWrongAccountWorldClosedScreensAndUnsupportedRevision(){
        ArenaSetup setup=new ArenaSetup(TestArenaKit.DEFAULT);Models.Status s=status();
        assertNull(setup.read(client,s,false,false));world=301;assertNull(setup.read(client,s,true,false));world=558;
        arena=false;assertNull(setup.read(client,s,true,false));arena=true;
        duel=false;assertNull(setup.read(client,s,true,false));duel=true;
        revision=242;ArenaSetup.View view=setup.read(client,s,true,false);assertEquals("Setup check unavailable",view.heading);assertNull(view.kit);
        revision=241;s.match.state="offered";assertNull(setup.read(client,s,true,false));
    }
    @Test public void cancellationOrNewRoundClearsOldConfirmation(){
        ArenaSetup setup=new ArenaSetup(TestArenaKit.DEFAULT);Models.Status s=status();vars.put(14029,1);vars.put(14063,1002);
        setup.read(client,s,true,false);vars.put(14063,26382);
        assertEquals("Kit changed",setup.read(client,s,true,false).heading);
        s.match.round=2;assertEquals("Your kit is ready",setup.read(client,s,true,false).heading);
        vars.put(14029,0);vars.put(14063,1002);setup.read(client,s,true,false);vars.put(14063,26382);
        assertEquals("Your kit is ready",setup.read(client,s,true,false).heading);
        duel=false;assertNull(setup.read(client,s,true,false));duel=true;
        assertEquals("Your kit is ready",setup.read(client,s,true,false).heading);
    }
    @Test public void readsNativeMismatchAndAssignedOpponentFromVisibleDuelWidgets(){
        texts.put(145,"Your opponent's load-out <col=ff0000>does not match</col> yours.");
        ArenaSetup.View view=new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false);
        assertTrue(view.warning);assertEquals("Check opponent's kit",view.heading);
        texts.put(2,"Unranked Duel: Someone Else");
        view=new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false);assertEquals("Wrong opponent",view.heading);
        texts.put(2,"Unranked Duel: Other Main");
        assertEquals("Check opponent's kit",new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false).heading);
    }
    private Widget item(int id,int quantity){return proxy(Widget.class,(m,a)->m.equals("getItemId")?id:m.equals("getItemQuantity")?quantity:false);}
    @Test public void displayedOpponentItemsAreComparedToDefaultWithCorrectAttribution(){
        texts.put(120,"");texts.put(145,"Your opponent's load-out does not match yours.");ArenaKit kit=TestArenaKit.DEFAULT.kit();
        for(int i=0;i<11;i++){
            ArenaKit.Slot slot=kit.equipment.get(i);final int id=i==4?4712:slot.itemId;
            widgets.put(129+i,proxy(Widget.class,(m,a)->m.equals("getDynamicChildren")?new Widget[]{item(-1,0),item(id,slot.quantity)}:false));
        }
        Widget[] bag=new Widget[28];for(int i=0;i<28;i++)bag[i]=item(kit.inventory.get(i).itemId,kit.inventory.get(i).quantity);
        widgets.put(140,proxy(Widget.class,(m,a)->m.equals("getDynamicChildren")?bag:false));
        ArenaSetup.View view=new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false);
        assertEquals("Opponent needs to fix their kit",view.heading);assertEquals(2,view.changes.size());
        ArenaKit.Change change=view.changes.get(0);assertTrue(change.opponentSide);
        assertEquals("Virtus robe top",change.location);assertEquals("Missing",change.yours);assertEquals("Virtus robe top",change.theirs);
        ArenaKit.Change extra=view.changes.get(1);assertTrue(extra.opponentSide);assertEquals(4,extra.equipment);
        assertEquals("Item 4712",extra.yours);assertEquals("Not required",extra.theirs);
    }
    @Test public void runeMaskListsDisabledRunesAndZeroMeansAllEnabled(){
        assertEquals("All runes enabled",ArenaSetup.runes(client,0));
        assertEquals("Disabled: Item 554, Item 556",ArenaSetup.runes(client,5));
    }
    @Test public void nativeTransmittedKitTakesPrecedenceOverIdenticalButStaleDisplayedItems(){
        texts.put(120,"");texts.put(145,"Your opponent's load-out does not match yours.");ArenaKit kit=TestArenaKit.DEFAULT.kit();
        Item[] items=new Item[42];java.util.Arrays.fill(items,new Item(-1,0));
        for(int i=0;i<11;i++){
            ArenaKit.Slot slot=kit.equipment.get(i);final int id=slot.itemId;
            items[ArenaKit.EQUIPMENT_SLOTS[i]]=new Item(id,slot.quantity);
            widgets.put(129+i,proxy(Widget.class,(m,a)->m.equals("getDynamicChildren")?new Widget[]{item(id,slot.quantity)}:false));
        }
        Widget[] bag=new Widget[28];for(int i=0;i<28;i++){
            ArenaKit.Slot slot=kit.inventory.get(i);bag[i]=item(slot.itemId,slot.quantity);items[14+i]=new Item(slot.itemId,slot.quantity);
        }
        widgets.put(140,proxy(Widget.class,(m,a)->m.equals("getDynamicChildren")?bag:false));
        items[ArenaKit.EQUIPMENT_SLOTS[4]]=new Item(-1,0);
        transmitted=proxy(ItemContainer.class,(m,a)->m.equals("getItems")?items:items[(int)a[0]]);
        ArenaSetup.View view=new ArenaSetup(TestArenaKit.DEFAULT).read(client,status(),true,false);
        assertTrue(view.warning);assertEquals("Opponent needs to fix their kit",view.heading);assertEquals(1,view.changes.size());
        assertEquals("Virtus robe top",view.changes.get(0).theirs);assertTrue(view.changes.get(0).opponentSide);
        assertEquals(4,view.changes.get(0).equipment);
    }
}
