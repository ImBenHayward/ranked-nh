package com.rankednh;

import com.google.gson.*;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** The same rearrangements and tampering cases run against the backend. Slots 0-10 are worn. */
public class KitComparisonContractTest {
    static ArenaKit rearranged(){
        ArenaKit base=TestArenaKit.DEFAULT.kit();List<ArenaKit.Slot> all=new ArrayList<>(base.equipment);all.addAll(base.inventory);
        for(int[] pair:new int[][]{{3,23},{5,24},{4,27},{6,28},{11,38}})Collections.swap(all,pair[0],pair[1]);
        return new ArenaKit(base.revision,base.build,base.spellbook,base.runePouch,all.subList(0,11),all.subList(11,39));
    }
    @Test public void clientAndBackendSharePossessionCases()throws Exception{
        try(InputStreamReader reader=new InputStreamReader(getClass().getResourceAsStream("kit-comparison-cases.json"),StandardCharsets.UTF_8)){
            for(JsonElement element:new Gson().fromJson(reader,JsonArray.class)){
                JsonObject example=element.getAsJsonObject();ArenaKit base=TestArenaKit.DEFAULT.kit();
                List<ArenaKit.Slot> all=new ArrayList<>(base.equipment);all.addAll(base.inventory);
                if(example.has("swaps"))for(JsonElement swap:example.getAsJsonArray("swaps")){
                    JsonArray pair=swap.getAsJsonArray();Collections.swap(all,pair.get(0).getAsInt(),pair.get(1).getAsInt());
                }
                if(example.has("edits"))for(JsonElement edit:example.getAsJsonArray("edits")){
                    JsonObject e=edit.getAsJsonObject();int i=e.get("slot").getAsInt();ArenaKit.Slot old=all.get(i);
                    all.set(i,new ArenaKit.Slot(e.has("itemId")?e.get("itemId").getAsInt():old.itemId,
                        e.has("quantity")?e.get("quantity").getAsInt():old.quantity,e.has("name")?e.get("name").getAsString():old.name));
                }
                JsonObject fields=example.has("fields")?example.getAsJsonObject("fields"):new JsonObject();
                ArenaKit kit=new ArenaKit(field(fields,"revision",base.revision),field(fields,"build",base.build),
                    field(fields,"spellbook",base.spellbook),field(fields,"runePouch",base.runePouch),all.subList(0,11),all.subList(11,39));
                boolean matches=example.get("matches").getAsBoolean();
                assertEquals(example.get("name").getAsString(),matches,TestArenaKit.DEFAULT.matches(kit));
                if(matches)assertTrue(kit.defaultChanges(TestArenaKit.DEFAULT.kit(),false).isEmpty());
            }
        }
    }
    private int field(JsonObject fields,String name,int fallback){return fields.has(name)?fields.get(name).getAsInt():fallback;}
    @Test public void differentStartingSwitchesStayReadyAcrossConfirmationAndBattleEntry(){
        ArenaKit original=TestArenaKit.DEFAULT.kit(),rearranged=rearranged();
        ArenaSetup.View view=ArenaSetup.withGameCheck(TestArenaKit.SETUP.describe(true,2,0,true,true,rearranged,original,original),"same options");
        assertFalse(view.warning);assertTrue(view.comparisonKnown);assertTrue(view.changes.isEmpty());
        Models.Status status=MatchupTest.status();status.match.state="active";ArenaRoundGuard guard=new ArenaRoundGuard(TestArenaKit.DEFAULT);
        guard.observe(status,view,false,true);guard.observe(status,null,true,true);guard.checkConfigured(original);
        assertFalse(guard.blocked());assertNotNull(guard.evidence());assertTrue(TestArenaKit.DEFAULT.matches(guard.evidence().own));
        ArrayList<ArenaKit.Slot> bag=new ArrayList<>(rearranged.inventory);bag.set(16,new ArenaKit.Slot(-1,0,"Empty"));
        guard.checkConfigured(new ArenaKit(241,0,1,0,rearranged.equipment,bag));assertTrue(guard.blocked());assertNull(guard.evidence());
    }
    @Test public void quantitiesAreSummedAcrossContainersWithoutTreatingDuplicatesAsASet(){
        ArenaKit base=ArenaSetupTest.kit(0,1,0);List<ArenaKit.Slot> worn=new ArrayList<>(base.equipment),bag=new ArrayList<>(base.inventory);
        worn.set(10,new ArenaKit.Slot(21932,3000,"Bolts"));bag.set(0,new ArenaKit.Slot(21932,2000,"Bolts"));bag.set(1,new ArenaKit.Slot(-1,0,"Empty"));
        ArenaKit split=new ArenaKit(241,0,1,0,worn,bag);
        bag.set(1,new ArenaKit.Slot(21932,5000,"Bolts"));bag.set(0,new ArenaKit.Slot(0,0,"Empty"));worn.set(10,new ArenaKit.Slot(-1,0,"Empty"));
        ArenaKit combined=new ArenaKit(241,0,1,0,worn,bag);assertTrue(split.differences(combined).isEmpty());assertTrue(split.changes(combined).isEmpty());
        bag.set(1,new ArenaKit.Slot(21932,4999,"Bolts"));assertFalse(split.differences(new ArenaKit(241,0,1,0,worn,bag)).isEmpty());
    }
}
