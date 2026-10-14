package com.rankednh;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.Text;

/** Marks the setting to fix, independently of which tab contains Confirm. Never intercepts input. */
final class ArenaSetupWarningOverlay extends Overlay {
    static final Color WARNING=new Color(239,107,100);
    static final Color NEXT=new Color(155,211,173);
    private final Client client;
    private final RankedNhPlugin plugin;
    private final TooltipManager tooltips;
    private final ArenaItemCatalog catalog;
    ArenaSetupWarningOverlay(Client client,RankedNhPlugin plugin){this(client,plugin,null,null);}
    @Inject ArenaSetupWarningOverlay(Client client,RankedNhPlugin plugin,TooltipManager tooltips,ArenaItemCatalog catalog){
        this.client=client;this.plugin=plugin;this.tooltips=tooltips;this.catalog=catalog;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);setMovable(false);
        drawAfterInterface(ArenaSetup.DUEL);drawAfterInterface(ArenaSetup.SUPPLIES);
    }
    @Override public Dimension render(Graphics2D graphics){
        ArenaSetup.View view=plugin.setupView();
        if(view==null || !plugin.accountMatches() || client.getGameState()!=GameState.LOGGED_IN)return null;
        boolean duel=visible(ArenaSetup.DUEL,0)!=null;
        int group=duel?ArenaSetup.DUEL:ArenaSetup.SUPPLIES;
        if(!duel && visible(group,0)==null && visible(group,9)==null)return null;
        if(duel && "Choose Max/Med".equals(view.heading)){
            Widget choices=visible(group,10),target=findText(choices,"Max/Med",0);
            if(target!=null)drawBuildWarning(graphics,target.getBounds());
            else guide(graphics,group,"Stats",WARNING);
            return null;
        }
        switch(view.target){
            case MAX_MED_TAB:
                if(visible(group,duel?25:9)==null)guide(graphics,group,"Max/Med kit",view.warning?WARNING:NEXT);
                break;
            case OPPONENT_TAB:
                if(duel && visible(group,120)==null)guide(graphics,group,"Opponent's kit",view.warning?WARNING:NEXT);
                break;
            case BUILD_CONFIRM: case KIT_CONFIRM:
                if(duel && !view.warning)guide(graphics,group,"Confirm",NEXT);
                break;
            default:break;
        }
        drawOptional(graphics,view);
        if(!view.warning)return null;
        Map<Rectangle,ArenaKit.Change> marked=new LinkedHashMap<>();
        for(ArenaKit.Change change:view.changes){
            if(!duel && change.opponentSide)continue;
            if(change.equipment>=0)mark(visible(group,(!duel?18:change.opponentSide?129:33)+change.equipment),change,marked);
            if(change.inventory>=0){
                Widget inventory=visible(group,!duel?29:change.opponentSide?140:44);
                Widget[] children=inventory==null?null:inventory.getDynamicChildren();
                if(children!=null && children.length>change.inventory)mark(children[change.inventory],change,marked);
            }
            if("Spellbook".equals(change.location))mark(visible(group,!duel?10:change.opponentSide?121:25),change,marked);
        }
        drawSlots(graphics,marked.keySet());
        if(tooltips!=null){
            net.runelite.api.Point mouse=client.getMouseCanvasPosition();
            // One tooltip even when an item and its rune configuration share the same slot.
            for(Map.Entry<Rectangle,ArenaKit.Change> entry:marked.entrySet())if(entry.getKey().contains(mouse.getX(),mouse.getY())){
                tooltips.add(new Tooltip(RankedNhPanel.escape(entry.getValue().explanation()).replace("\n","<br>")));break;
            }
        }
        return null;
    }
    private void drawOptional(Graphics2D graphics,ArenaSetup.View view){
        if(catalog==null || view.kit==null || "Wrong opponent".equals(view.heading))return;
        Widget list=catalog.visibleList();if(list==null)return;
        Map<Integer,Integer> additions=catalog.optional(view.kit);
        Widget[] children=list.getDynamicChildren();if(children==null)return;
        Rectangle clip=list.getBounds();Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.clip(clip);
            for(Widget item:children)if(item!=null && !item.isHidden() && additions.containsKey(item.getItemId())){
                Rectangle bounds=item.getBounds();if(bounds.width<=0 || bounds.height<=0 || !bounds.intersects(clip))continue;
                int count=additions.get(item.getItemId());drawAddition(g,bounds,count);
                if(tooltips!=null){net.runelite.api.Point mouse=client.getMouseCanvasPosition();
                    if(bounds.intersection(clip).contains(mouse.getX(),mouse.getY()))
                        tooltips.add(new Tooltip("Optional: add up to "+count+" more.<br>You can leave out any approved item."));
                }
            }
        }finally{g.dispose();}
    }
    static void drawAddition(Graphics2D graphics,Rectangle bounds,int count){
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setColor(NEXT);g.setStroke(new BasicStroke(1.5f));g.drawRect(bounds.x,bounds.y,bounds.width-1,bounds.height-1);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));String text="+"+count;
            int width=g.getFontMetrics().stringWidth(text)+4,x=bounds.x+bounds.width-width,y=bounds.y+bounds.height-12;
            g.setColor(new Color(27,30,34));g.fillRect(x,y,width,12);g.setColor(NEXT);g.drawString(text,x+2,y+10);
        }finally{g.dispose();}
    }
    private void guide(Graphics2D graphics,int group,String text,Color color){
        // Some native tabs are siblings of the frame rather than children of widget zero.
        for(int child=0;child<150;child++){
            Widget target=findText(visible(group,child),text,0);
            if(target==null)continue;
            Rectangle bounds=target.getBounds();
            if(bounds.width<=0 || bounds.height<=0)continue;
            Graphics2D g=(Graphics2D)graphics.create();
            try{g.setColor(color);g.setStroke(new BasicStroke(2));g.drawRoundRect(bounds.x-3,bounds.y-3,bounds.width+6,bounds.height+6,5,5);}
            finally{g.dispose();}
            return;
        }
    }
    private static void mark(Widget widget,ArenaKit.Change change,Map<Rectangle,ArenaKit.Change> slots){
        if(widget==null || widget.isHidden())return;Rectangle bounds=widget.getBounds();
        if(bounds.width>0 && bounds.height>0)slots.putIfAbsent(bounds,change);
    }
    static Widget findText(Widget root,String text,int depth){
        if(root==null || root.isHidden() || depth>8)return null;
        if(root.getText()!=null && text.equalsIgnoreCase(Text.removeTags(root.getText()).trim()))return root;
        for(Widget[] children:new Widget[][]{root.getDynamicChildren(),root.getStaticChildren()})if(children!=null)
            for(Widget child:children){Widget found=findText(child,text,depth+1);if(found!=null)return found;}
        return null;
    }
    static void drawBuildWarning(Graphics2D graphics,Rectangle target){
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setColor(WARNING);g.setStroke(new BasicStroke(2));g.drawRect(target.x-3,target.y-3,target.width+6,target.height+6);
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));String text="Required: Max/Med";
            int width=g.getFontMetrics().stringWidth(text)+16,x=target.x+(target.width-width)/2,y=target.y+target.height+8;
            g.setColor(new Color(38,26,25));g.fillRect(x,y,width,21);g.setColor(WARNING);g.drawString(text,x+8,y+14);
        }finally{g.dispose();}
    }
    static void drawSlots(Graphics2D graphics,Iterable<Rectangle> slots){
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setColor(WARNING);g.setStroke(new BasicStroke(2));
            for(Rectangle r:slots){
                // A full red outline is easy to spot while leaving the item artwork unobscured.
                g.drawRect(r.x-1,r.y-1,r.width+2,r.height+2);
            }
        }finally{g.dispose();}
    }
    private Widget visible(int group,int child){Widget w=client.getWidget(group,child);return w==null || w.isHidden()?null:w;}
}
