package com.rankednh;

import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.WorldType;
import net.runelite.api.widgets.Widget;

/** Read-only Arena setup guidance. All reads run on the client thread. */
final class ArenaSetup
{
    // Revision 241 scripts 6202/6335: build 0 = Max/Med; progress <=1 = build selection.
    // Scripts 6321/6322/6311: A-kit slot selectors -> enum 4390; stackables receive 5,000.
    // Script 6325 maps configured spellbooks through enum 4389. Opponent container = 207.
    static final int REVISION=241,DUEL=757,SUPPLIES=758;
    private static final int[] INVENTORY_BITS={14032,14033,14034,14036,14037,14038,14040,14041,14042,
        14044,14045,14046,14047,14048,14049,14050,14051,14052,14053,14054,14055,14056,14057,14058,14059,14060,14061,14062};
    enum Target { NONE, MAX_MED_TAB, STATS_TAB, BUILD_CONFIRM, OPPONENT_TAB, KIT_CONFIRM }
    static final class View {
        final String heading,message,detail;
        final boolean warning;
        final ArenaKit kit;
        List<ArenaKit.Change> changes=java.util.List.of();
        boolean comparisonKnown;
        ArenaKit opponentKit;
        Target target=Target.NONE;
        View guide(Target target){this.target=target;return this;}
        View(String heading,String message,String detail,boolean warning,ArenaKit kit){
            this.heading=heading;this.message=message;this.detail=detail;this.warning=warning;this.kit=kit;
        }
    }
    private final DefaultArenaKit defaultKit;
    @javax.inject.Inject ArenaSetup(DefaultArenaKit defaultKit){this.defaultKit=defaultKit;}
    private ArenaKit confirmed;
    private String roundKey;
    private boolean wasDuel;
    void reset(){confirmed=null;roundKey=null;wasDuel=false;}
    static boolean visible(Client client,int group,int child){Widget w=client.getWidget(group,child);return w!=null && !w.isHidden();}
    View read(Client client,Models.Status status,boolean accountMatches,boolean fighting){
        if(!accountMatches || client.getGameState()!=GameState.LOGGED_IN || !client.getWorldType().contains(WorldType.PVP_ARENA)){
            reset();return null;
        }
        Models.Match match=status==null?null:status.match;
        boolean scoped=match!=null && ("preparing".equals(match.state) || "active".equals(match.state)) && client.getWorld()==match.world;
        if(match!=null && match.isOpen() && !scoped){reset();return null;}
        String key=scoped?match.id+":"+match.round:null;
        if(!java.util.Objects.equals(key,roundKey)){confirmed=null;roundKey=key;}
        boolean duel=visible(client,DUEL,0),supplies=visible(client,SUPPLIES,0);
        if(!duel || !wasDuel)confirmed=null;
        wasDuel=duel;
        if(!duel && !supplies || fighting)return null;
        if(client.getRevision()!=REVISION)return new View("Setup check unavailable","The Arena interface has changed.","Check Max/Med and both kits manually. Default kit not verified.",true,null);
        try{
            int build=client.getVarbitValue(14024),progress=client.getVarbitValue(14028);
            ArenaKit own=ownKit(client);
            own.runeSelection=runes(client,own.runePouch);
            if(!duel && !visible(client,SUPPLIES,9))return new View("Open Max/Med kit","Select the Max/Med kit tab to check your items.","Choose the duel build after challenging.",false,own).guide(Target.MAX_MED_TAB);
            if(scoped && duel){
                String title=widgetText(client,DUEL,2);
                if(title.startsWith("Unranked Duel: ") && !FightRecorder.normalized(title.substring(15)).equals(FightRecorder.normalized(match.opponent.rsn)))
                    return new View("Wrong opponent","Challenge "+match.opponent.rsn+" instead.","This duel is with another player.",true,own);
            }
            ArenaKit opponent=duel && progress>=2 && visible(client,DUEL,120)?opponentKit(client,build):null;
            if(opponent!=null)opponent.runeSelection=runes(client,opponent.runePouch);
            boolean accepted=duel && client.getVarbitValue(14029)==1;
            if(!accepted)confirmed=null;
            View result=describe(duel,progress,build,client.getVarbitValue(14026)==1,accepted,own,opponent,confirmed);
            if(duel && progress>=2)result=withGameCheck(result,widgetText(client,DUEL,55)+" "+widgetText(client,DUEL,142)+" "+widgetText(client,DUEL,145));
            // The supplies editor has no opponent to compare. Our own-kit rules determine readiness.
            if(accepted && confirmed==null && own.complete())confirmed=own;
            return result;
        }catch(RuntimeException unavailable){
            confirmed=null;
            return new View("Reading Arena setup","Waiting for the kit data…","Default kit not verified. Check the game's setup before confirming.",true,null);
        }
    }
    View describe(boolean duel,int progress,int build,boolean acceptedBuild,boolean acceptedKit,ArenaKit own,ArenaKit opponent,ArenaKit confirmed){
        if(duel && build!=0)return new View("Choose Max/Med","Select Max/Med in Stats.","Confirm the build together.",true,own).guide(Target.STATS_TAB);
        if(duel && progress<=1)return new View("Confirm build",acceptedBuild?"Waiting for your opponent.":"Max/Med selected. Confirm in-game.","Next: check both kits against the default.",false,own).guide(acceptedBuild?Target.NONE:Target.BUILD_CONFIRM);
        if(own==null || !own.complete())return new View("Reading kit","Open your Max/Med kit.","Waiting for the configured supplies.",true,null).guide(Target.MAX_MED_TAB);
        if(own.revision!=REVISION || opponent!=null && opponent.revision!=REVISION)
            return new View("Setup check unavailable","The game version differs from the default reference.","Update the plugin before fighting.",true,own);
        boolean yours=defaultKit.matches(own),theirs=defaultKit.matches(opponent);
        if(!yours || opponent!=null && opponent.complete() && !theirs){
            String heading=!yours?(opponent!=null && opponent.complete() && !theirs?"Fix both kits":"Fix your kit"):"Opponent needs to fix their kit";
            String recovery=yours?"Your kit is ready. Your opponent needs to fix the red-marked items or settings.":
                "Fix the marked items or settings in your kit. Any default item can be left out.";
            View view=new View(heading,"Use only approved items, within their quantity limits.",recovery,true,own);
            List<ArenaKit.Change> changes=new ArrayList<>(own.defaultChanges(defaultKit.kit(),false));
            if(opponent!=null && opponent.complete())changes.addAll(opponent.defaultChanges(defaultKit.kit(),true));
            if(!duel && changes.size()==1 && "Spellbook".equals(changes.get(0).location))
                view=new View(heading,"Choose Ancient Magicks.","Set the spellbook to Ancient Magicks in the Arena editor.",true,own);
            view.changes=List.copyOf(changes);
            view.target=!yours?Target.MAX_MED_TAB:Target.OPPONENT_TAB;
            return view;
        }
        if(!duel)return new View("Kit ready","Find a match in the NH panel.","Highlighted additions are optional. Arrange and equip approved items however you like.",false,own);
        if(confirmed!=null && !confirmed.differences(own).isEmpty())
            return new View("Kit changed","Your kit changed after confirmation.","Check both kits and confirm again.",true,own);
        if(opponent==null || !opponent.complete())return new View("Your kit is ready","Open Opponent's kit to check theirs.","Your items are allowed. Highlighted additions are optional.",false,own).guide(Target.OPPONENT_TAB);
        View matched=new View("Both kits ready",acceptedKit?"Waiting for your opponent.":"Both kits meet the Max/Med rules.",acceptedKit?"The duel starts when you have both confirmed.":"Click Confirm on Opponent's kit to enter the duel.",false,own).guide(acceptedKit?Target.NONE:Target.KIT_CONFIRM);
        matched.comparisonKnown=true;matched.opponentKit=opponent;return matched;
    }
    static View withGameCheck(View view,String text){
        // NH's complete comparison owns readiness. Native text can be stale or disagree
        // despite identical kits; it must not erase validated evidence or specific NH fixes.
        if(view.warning || view.comparisonKnown)return view;
        String plain=net.runelite.client.util.Text.removeTags(text).replace(' ',' ').toLowerCase(java.util.Locale.ROOT);
        if(plain.contains("does not match") || plain.contains("do not match")){
            return new View("Check opponent's kit","The opponent's full kit hasn't loaded yet.",
                "Open Opponent's kit so NH can check the items and settings.",true,view.kit).guide(Target.OPPONENT_TAB);
        }
        if(plain.contains("will not permit") || plain.contains("too many") || plain.contains("cannot be fired"))
            return new View("Check kit","The Arena has flagged a loadout problem.","Review the warning below the kit.",true,view.kit);
        return view;
    }
    private static String widgetText(Client client,int group,int child){
        Widget w=client.getWidget(group,child);return w==null || w.isHidden() || w.getText()==null?"":net.runelite.client.util.Text.removeTags(w.getText());
    }
    static ArenaKit ownKit(Client client){
        EnumComposition items=client.getEnum(4390),spellbooks=client.getEnum(4389);
        List<ArenaKit.Slot> equipment=new ArrayList<>(),inventory=new ArrayList<>();
        for(int i=0;i<11;i++)equipment.add(configuredSlot(client,items,client.getVarbitValue(14063+i)));
        for(int bit:INVENTORY_BITS)inventory.add(configuredSlot(client,items,client.getVarbitValue(bit)));
        return new ArenaKit(client.getRevision(),0,spellbooks.getIntValue(client.getVarbitValue(14035)),client.getVarpValue(3534),equipment,inventory);
    }
    private static ArenaKit.Slot configuredSlot(Client client,EnumComposition items,int selector){
        int id=items.getIntValue(selector);
        if(id<=0)return new ArenaKit.Slot(-1,0,"Empty");
        ItemComposition item=client.getItemDefinition(id);
        return new ArenaKit.Slot(id,item.isStackable()?5000:1,item.getName());
    }
    private static ArenaKit opponentKit(Client client,int build){
        // Native comparison script 6208 uses container 207. Prefer that transmitted data;
        // cached widget sprites can still show the previous kit while the native check changes.
        ItemContainer container=client.getItemContainer(207);
        if(container!=null && container.getItems().length>=42){
            List<ArenaKit.Slot> equipment=new ArrayList<>(),inventory=new ArrayList<>();
            for(int slot:ArenaKit.EQUIPMENT_SLOTS)equipment.add(containerSlot(client,container,slot));
            for(int i=0;i<28;i++)inventory.add(containerSlot(client,container,14+i));
            return new ArenaKit(client.getRevision(),build,client.getVarbitValue(14031),client.getVarpValue(3533),equipment,inventory);
        }
        // Widget data is a fallback while the full transmitted container is unavailable.
        Widget inventoryWidget=client.getWidget(DUEL,140);
        Widget[] displayed=inventoryWidget==null?null:inventoryWidget.getDynamicChildren();
        if(displayed!=null && displayed.length>=28){
            List<ArenaKit.Slot> worn=new ArrayList<>(),bag=new ArrayList<>();boolean complete=true;
            for(int i=0;i<11;i++){
                Widget slot=client.getWidget(DUEL,129+i);if(slot==null){complete=false;break;}
                Widget[] children=slot.getDynamicChildren();if(children==null){complete=false;break;}
                ArenaKit.Slot item=new ArenaKit.Slot(-1,0,"Empty");
                for(Widget child:children)if(child!=null && child.getItemId()>0)item=widgetSlot(client,child);
                worn.add(item);
            }
            for(int i=0;i<28;i++){if(displayed[i]==null){complete=false;break;}bag.add(widgetSlot(client,displayed[i]));}
            // Empty widgets can be loading placeholders. A genuinely empty opponent kit is
            // allowed, but it needs the full transmitted container above as evidence.
            if(complete && (worn.stream().anyMatch(s->s.itemId>0) || bag.stream().anyMatch(s->s.itemId>0)))
                return new ArenaKit(client.getRevision(),build,client.getVarbitValue(14031),client.getVarpValue(3533),worn,bag);
        }
        return null;
    }
    private static ArenaKit.Slot widgetSlot(Client client,Widget widget){
        int id=widget.getItemId();return id<=0?new ArenaKit.Slot(-1,0,"Empty"):
            new ArenaKit.Slot(id,widget.getItemQuantity(),client.getItemDefinition(id).getName());
    }
    static String runes(Client client,int mask){
        // Native editor 6424 offers Enable for set bits: the mask records EXCLUDED runes.
        if(mask==0)return "All runes enabled";
        EnumComposition types=client.getEnum(982);List<String> names=new ArrayList<>();
        for(int bit=0;bit<=23;bit++)if((mask & (1<<bit))!=0){int item=types.getIntValue(bit);if(item>0)names.add(client.getItemDefinition(item).getName());}
        return names.isEmpty()?"Rune selection unavailable":"Disabled: "+String.join(", ",names);
    }
    private static ArenaKit.Slot containerSlot(Client client,ItemContainer container,int slot){
        Item item=container.getItem(slot);
        return item==null || item.getId()<=0?new ArenaKit.Slot(-1,0,"Empty"):
            new ArenaKit.Slot(item.getId(),item.getQuantity(),client.getItemDefinition(item.getId()).getName());
    }
}
