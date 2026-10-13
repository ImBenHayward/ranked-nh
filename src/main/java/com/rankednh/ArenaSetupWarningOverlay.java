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
    static final Color WARNING=new Color(237,137,129);
    private final Client client;
    private final RankedNhPlugin plugin;
    private final TooltipManager tooltips;
    ArenaSetupWarningOverlay(Client client,RankedNhPlugin plugin){this(client,plugin,null);}
    @Inject ArenaSetupWarningOverlay(Client client,RankedNhPlugin plugin,TooltipManager tooltips){
        this.client=client;this.plugin=plugin;this.tooltips=tooltips;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);setMovable(false);
        drawAfterInterface(ArenaSetup.DUEL);drawAfterInterface(ArenaSetup.SUPPLIES);
    }
    @Override public Dimension render(Graphics2D graphics){
        ArenaSetup.View view=plugin.setupView();
        if(view==null || !view.warning || !plugin.accountMatches() || client.getGameState()!=GameState.LOGGED_IN)return null;
        boolean duel=visible(ArenaSetup.DUEL,0)!=null;
        int group=duel?ArenaSetup.DUEL:ArenaSetup.SUPPLIES;
        if(!duel && visible(group,9)==null)return null;
        if(duel && "Choose Max/Med".equals(view.heading)){
            Widget choices=visible(group,10),target=findText(choices,"Max/Med",0);
            if(target!=null)drawBuildWarning(graphics,target.getBounds());
            return null;
        }
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
            g.setColor(WARNING);g.setStroke(new BasicStroke(1.5f));
            for(Rectangle r:slots){
                int x=r.x-1,y=r.y-1,right=r.x+r.width+1,bottom=r.y+r.height+1,n=Math.min(7,Math.min(r.width,r.height)/3);
                // Corners keep the item itself readable, including a heavily edited 28-slot inventory.
                g.drawLine(x,y,x+n,y);g.drawLine(x,y,x,y+n);g.drawLine(right-n,y,right,y);g.drawLine(right,y,right,y+n);
                g.drawLine(x,bottom-n,x,bottom);g.drawLine(x,bottom,x+n,bottom);g.drawLine(right,bottom-n,right,bottom);g.drawLine(right-n,bottom,right,bottom);
            }
        }finally{g.dispose();}
    }
    private Widget visible(int group,int child){Widget w=client.getWidget(group,child);return w==null || w.isHidden()?null:w;}
}
