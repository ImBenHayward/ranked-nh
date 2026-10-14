package com.rankednh;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/** Filters only the local Max/Med catalogue. Never changes a kit or dispatches a game action. */
@Singleton
final class ArenaItemCatalog {
    private static final int ROW_HEIGHT=32;
    private final Client client;
    private final DefaultArenaKit reference;
    private volatile boolean enabled=true;
    private volatile String notice="";
    private Widget list;
    private Widget[] children;
    private Set<Integer> shownItems=Set.of();
    private int originalHeight,originalScroll,filteredHeight;
    private final List<Cell> cells=new ArrayList<>();
    private static final class Cell {
        final Widget widget;
        final int y,itemId;
        final boolean hidden;
        int shownY;
        boolean shownHidden;
        Cell(Widget widget){this.widget=widget;y=widget.getOriginalY();itemId=widget.getItemId();hidden=widget.isSelfHidden();}
        boolean unchanged(){return widget.getOriginalY()==shownY && widget.isSelfHidden()==shownHidden && widget.getItemId()==itemId;}
    }
    @Inject ArenaItemCatalog(Client client,DefaultArenaKit reference){this.client=client;this.reference=reference;}
    void enabled(boolean value){enabled=value;if(!value)clear();}
    boolean enabled(){return enabled;}
    String notice(){return notice;}
    Widget visibleList(){
        if(client.getRevision()!=ArenaSetup.REVISION || client.getGameState()!=GameState.LOGGED_IN ||
            !client.getWorldType().contains(WorldType.PVP_ARENA))return null;
        // Never touch the opponent's catalogue or a different build's editor.
        int id=ArenaSetup.visible(client,ArenaSetup.DUEL,0)?InterfaceID.PvpArenaUnrankedduel._0ITEMS_LIST:
            InterfaceID.PvpArenaStagingareaSupplies._0ITEMS_LIST;
        Widget widget=client.getWidget(id);
        return widget==null || widget.isHidden()?null:widget;
    }
    void maintain(boolean active,ArenaKit kit){
        Widget current=active && enabled?visibleList():null;
        if(current==null){clear();return;}
        if(kit==null || !kit.complete()){clear();return;}
        Set<Integer> available=optional(kit).keySet();
        apply(current,available);
        if(available.isEmpty() && list!=null)notice="All default items are already included.";
    }
    void apply(Widget current,Set<Integer> allowed){
        if(list!=null){
            if(current==list && allowed.equals(shownItems) && intact())return;
            clear();
        }
        Widget[] items=current.getDynamicChildren();if(items==null || items.length==0)return;
        Map<Integer,List<Widget>> rows=new TreeMap<>();
        for(Widget child:items)if(child!=null){
            if(child.getOriginalY()<0)return;
            rows.computeIfAbsent(child.getOriginalY()/ROW_HEIGHT,k->new ArrayList<>()).add(child);
        }
        // Empty search results are valid; a layout with no item-bearing rows is not ours to reshape.
        if(rows.values().stream().noneMatch(row->row.stream().anyMatch(w->w.getItemId()>0)))return;
        list=current;children=items.clone();shownItems=Set.copyOf(allowed);originalHeight=current.getScrollHeight();originalScroll=current.getScrollY();
        int shown=0;
        for(Map.Entry<Integer,List<Widget>> row:rows.entrySet()){
            boolean keep=row.getValue().stream().anyMatch(w->allowed.contains(w.getItemId()));
            // Preserve the game's existing search results; filtering only narrows that list.
            boolean visible=keep && row.getValue().stream().anyMatch(w->!w.isSelfHidden());
            for(Widget widget:row.getValue()){
                Cell cell=new Cell(widget);cells.add(cell);
                cell.shownY=visible?shown*ROW_HEIGHT+cell.y-row.getKey()*ROW_HEIGHT:cell.y;
                cell.shownHidden=cell.hidden || !keep;
                widget.setHidden(cell.shownHidden);widget.setOriginalY(cell.shownY);widget.revalidate();
            }
            if(visible)shown++;
        }
        filteredHeight=shown*ROW_HEIGHT;
        notice=shown==0?"No missing approved items match this search.":"";
        current.setScrollHeight(filteredHeight);current.setScrollY(Math.min(originalScroll,Math.max(0,filteredHeight-current.getHeight())));
        current.revalidateScroll();
    }
    private boolean intact(){
        Widget[] live=list.getDynamicChildren();
        if(live==null || live.length!=children.length || list.getScrollHeight()!=filteredHeight)return false;
        for(int i=0;i<live.length;i++)if(live[i]!=children[i])return false;
        return cells.stream().allMatch(Cell::unchanged);
    }
    void clear(){
        if(list==null)return;
        // A native rebuild owns the new widgets. Do not overwrite it with an old snapshot.
        if(intact()){
            for(Cell cell:cells){cell.widget.setHidden(cell.hidden);cell.widget.setOriginalY(cell.y);cell.widget.revalidate();}
            list.setScrollHeight(originalHeight);list.setScrollY(originalScroll);list.revalidateScroll();
        }
        list=null;children=null;shownItems=Set.of();cells.clear();notice="";
    }
    Map<Integer,Integer> optional(ArenaKit kit){return kit==null || !kit.complete()?Map.of():kit.availableFrom(reference.kit());}
}
