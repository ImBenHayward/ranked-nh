package com.rankednh;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

final class RankedNhOpponentOverlay extends Overlay
{
    private final RankedNhPlugin plugin;
    private final RankedNhConfig config;
    private final Client client;
    private String name,badge;
    private BufferedImage nameplate;
    @Inject RankedNhOpponentOverlay(RankedNhPlugin plugin,RankedNhConfig config,Client client){
        this.plugin=plugin;this.config=config;this.client=client;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_SCENE);setMovable(false);
    }
    @Override public Dimension render(Graphics2D graphics){
        if(!config.highlightOpponent())return null;
        Models.Status status=plugin.status();
        Player opponent=OpponentLocator.find(client,status,plugin.accountMatches());
        if(opponent==null)return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.clipRect(client.getViewportXOffset(),client.getViewportYOffset(),client.getViewportWidth(),client.getViewportHeight());
            Polygon tile=opponent.getCanvasTilePoly();
            if(tile!=null)OverlayUtil.renderPolygon(g,tile,RankedNhPanel.ACCENT,new Color(236,4,4,28),new BasicStroke(2));
            String current=Text.removeTags(opponent.getName());
            String currentBadge=status.match.opponent.rank==null?"unranked":status.match.opponent.rank.badge;
            if(!current.equals(name) || !java.util.Objects.equals(currentBadge,badge)){
                name=current;badge=currentBadge;nameplate=OpponentNameplate.image(name,badge);
            }
            Point feet=Perspective.localToCanvas(client,opponent.getLocalLocation(),client.getPlane(),0);
            Point head=Perspective.localToCanvas(client,opponent.getLocalLocation(),client.getPlane(),opponent.getLogicalHeight());
            Point text=Perspective.localToCanvas(client,opponent.getLocalLocation(),client.getPlane(),opponent.getLogicalHeight()+16);
            if(text!=null){
                double scale=feet==null || head==null?0.65:OpponentNameplate.scale(Math.hypot(feet.getX()-head.getX(),feet.getY()-head.getY()));
                int width=(int)Math.round(nameplate.getWidth()*scale),height=(int)Math.round(nameplate.getHeight()*scale);
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(nameplate,text.getX()-width/2,text.getY()-height,width,height,null);
            }
        }finally{g.dispose();}
        return null;
    }
}
