package com.rankednh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A snapshot of configured supplies, not the player's lobby inventory or worn gear. */
final class ArenaKit
{
    static final int[] EQUIPMENT_SLOTS={0,1,2,3,4,5,7,9,10,12,13};
    static final String[] EQUIPMENT_NAMES={"Head","Cape","Neck","Weapon","Body","Shield","Legs","Hands","Feet","Ring","Ammo"};
    static final class Slot {
        final int itemId,quantity;
        final String name;
        Slot(int itemId,int quantity,String name){this.itemId=itemId;this.quantity=quantity;this.name=name;}
        boolean same(Slot other){return other!=null && itemId==other.itemId && quantity==other.quantity;}
    }
    final int revision,build,spellbook,runePouch;
    final List<Slot> equipment,inventory;
    String runeSelection;
    ArenaKit(int revision,int build,int spellbook,int runePouch,List<Slot> equipment,List<Slot> inventory){
        this.revision=revision;this.build=build;this.spellbook=spellbook;this.runePouch=runePouch;
        this.equipment=List.copyOf(equipment);this.inventory=List.copyOf(inventory);
    }
    boolean complete(){
        return equipment.size()==11 && inventory.size()==28 && spellbook>=0 && runePouch>=0 &&
            slots().stream().allMatch(s->s!=null && s.quantity>=0 && s.quantity<=5000 && (s.itemId>0?s.quantity>0:s.quantity==0));
    }
    private List<Slot> slots(){List<Slot> all=new ArrayList<>(equipment);all.addAll(inventory);return all;}
    /** Compare possession across both containers, including duplicate items and stack quantities. */
    Map<Integer,Integer> itemTotals(){
        Map<Integer,Integer> totals=new LinkedHashMap<>();
        for(Slot slot:slots())if(slot.itemId>0)totals.merge(slot.itemId,slot.quantity,Integer::sum);
        return totals;
    }
    /** Every default item is optional; the reference sets allowed IDs and maximum totals. */
    boolean allowedBy(ArenaKit reference){
        if(reference==null || !complete() || !reference.complete() || revision!=reference.revision ||
            build!=reference.build || spellbook!=reference.spellbook || runePouch!=reference.runePouch)return false;
        Map<Integer,Integer> limits=reference.itemTotals();
        return itemTotals().entrySet().stream().allMatch(e->e.getValue()<=limits.getOrDefault(e.getKey(),0));
    }
    Map<Integer,Integer> availableFrom(ArenaKit reference){
        Map<Integer,Integer> remaining=new LinkedHashMap<>(),actual=itemTotals();
        reference.itemTotals().forEach((id,max)->{int count=max-actual.getOrDefault(id,0);if(count>0)remaining.put(id,count);});
        return remaining;
    }
    List<String> differences(ArenaKit other){
        List<String> out=new ArrayList<>();
        if(other==null || !complete() || !other.complete()){out.add("Kit data unavailable");return out;}
        if(revision!=other.revision)out.add("Game version changed");
        if(build!=other.build)out.add("Build");
        if(!itemTotals().equals(other.itemTotals()))out.add("Items or quantities");
        if(spellbook!=other.spellbook)out.add("Spellbook");
        if(runePouch!=other.runePouch)out.add("Rune pouch");
        return out;
    }
    String spellbookName(){return spellbook==0?"Standard":spellbook==1?"Ancient Magicks":spellbook==2?"Lunar":spellbook==3?"Arceuus":"Unknown";}
    static final class Change {
        final String location,yours,theirs;final int equipment,inventory;
        final String actualLabel,expectedLabel;final boolean opponentSide;
        Change(String location,String yours,String theirs,int equipment,int inventory){this(location,yours,theirs,equipment,inventory,"Yours","Theirs",false);}
        Change(String location,String yours,String theirs,int equipment,int inventory,String actualLabel,String expectedLabel,boolean opponentSide){
            this.location=location;this.yours=yours;this.theirs=theirs;this.equipment=equipment;this.inventory=inventory;
            this.actualLabel=actualLabel;this.expectedLabel=expectedLabel;this.opponentSide=opponentSide;
        }
        String title(){return "Default".equals(expectedLabel) || "Allowed".equals(expectedLabel)?(opponentSide?"Opponent · ":"You · ")+location:location;}
        String summary(){return "Allowed".equals(expectedLabel) && theirs.startsWith("Not allowed")?theirs:
            actualLabel+": "+yours+"\n"+expectedLabel+": "+theirs;}
        String explanation(){return title()+"\n"+summary();}
    }
    static String item(Slot slot){return slot.itemId<=0?"Empty":slot.name+(slot.quantity>1?" × "+slot.quantity:"");}
    List<Change> changes(ArenaKit other){
        List<Change> changes=new ArrayList<>();if(other==null || !complete() || !other.complete())return changes;
        Map<Integer,Integer> actual=itemTotals(),expected=other.itemTotals();
        // One row per item type. Layout changes create no rows or red slot marks.
        for(Map.Entry<Integer,Integer> entry:expected.entrySet()){
            int id=entry.getKey(),have=actual.getOrDefault(id,0),need=entry.getValue();
            if(have<need){
                Slot named=other.first(id);int index=anchor(id,have,other);
                changes.add(itemChange(named,have,need,index));
            }
        }
        for(Map.Entry<Integer,Integer> entry:actual.entrySet()){
            int id=entry.getKey(),have=entry.getValue(),need=expected.getOrDefault(id,0);
            if(have>need)changes.add(itemChange(first(id),have,need,anchor(id,need,null)));
        }
        if(spellbook!=other.spellbook)changes.add(new Change("Spellbook",spellbookName(),other.spellbookName(),-1,-1));
        if(runePouch!=other.runePouch)changes.add(new Change("Rune pouch",runeSelection==null?"Check your rune selection":runeSelection,other.runeSelection==null?"Different rune selection":other.runeSelection,-1,pouchSlot()));
        return changes;
    }
    private Slot first(int id){for(Slot slot:slots())if(slot.itemId==id)return slot;throw new IllegalArgumentException("Missing item");}
    private Change itemChange(Slot named,int have,int need,int index){
        return new Change(named.name,have==0?"Missing":item(new Slot(named.itemId,have,named.name)),
            need==0?"Not required":item(new Slot(named.itemId,need,named.name)),index>=0 && index<11?index:-1,index>=11?index-11:-1);
    }
    private int anchor(int id,int allowed,ArenaKit expected){
        List<Slot> all=slots();int count=0,first=-1;
        for(int i=0;i<all.size();i++)if(all.get(i).itemId==id){
            if(first<0)first=i;
            count+=all.get(i).quantity;if(count>allowed)return i;
        }
        if(first>=0)return first; // Quantity shortage: point at an existing stack/item of this type.
        if(expected!=null){
            // A missing item has no actual position. Use an empty original worn slot or any empty
            // inventory slot as a hint; never mark a correctly owned item in its former position.
            for(int i=0;i<11;i++)if(expected.equipment.get(i).itemId==id && equipment.get(i).itemId<=0)return i;
            for(int i=0;i<28;i++)if(inventory.get(i).itemId<=0)return 11+i;
        }
        return -1;
    }
    private int pouchSlot(){for(int i=0;i<inventory.size();i++)if(inventory.get(i).itemId==27086)return i;return -1;}
    List<Change> defaultChanges(ArenaKit reference,boolean opponent){
        List<Change> out=new ArrayList<>();
        if(reference==null || !complete() || !reference.complete())return out;
        Map<Integer,Integer> limits=reference.itemTotals(),seen=new LinkedHashMap<>(),totals=itemTotals();
        List<Slot> all=slots();
        for(int i=0;i<all.size();i++){
            Slot slot=all.get(i);if(slot.itemId<=0)continue;
            int max=limits.getOrDefault(slot.itemId,0),count=seen.merge(slot.itemId,slot.quantity,Integer::sum);
            if(count>max)out.add(new Change(slot.name,item(new Slot(slot.itemId,totals.get(slot.itemId),slot.name)),
                max==0?"Not allowed. Remove this item.":"Up to "+max+" in total. Remove the extra.",
                i<11?i:-1,i>=11?i-11:-1,opponent?"Opponent":"Yours","Allowed",opponent));
        }
        if(spellbook!=reference.spellbook)out.add(new Change("Spellbook",spellbookName(),reference.spellbookName(),-1,-1,
            opponent?"Opponent":"Yours","Allowed",opponent));
        if(runePouch!=reference.runePouch)out.add(new Change("Rune pouch",runeSelection==null?"Check your rune selection":runeSelection,
            reference.runeSelection==null?"Different rune selection":reference.runeSelection,-1,pouchSlot(),opponent?"Opponent":"Yours","Allowed",opponent));
        return out;
    }
}
