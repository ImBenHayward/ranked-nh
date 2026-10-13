package com.rankednh;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Draw within the minimap's widget pass, so later interfaces and menus cover the marker. */
final class RankedNhOpponentMinimapOverlay extends Overlay {
    private final RankedNhPlugin plugin;
    private final RankedNhConfig config;
    private final Client client;
    @Inject RankedNhOpponentMinimapOverlay(RankedNhPlugin plugin,RankedNhConfig config,Client client){
        this.plugin=plugin;this.config=config;this.client=client;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.MANUAL);setMovable(false);
        drawAfterLayer(InterfaceID.Toplevel.MINIMAP);
        drawAfterLayer(InterfaceID.ToplevelOsrsStretch.MINIMAP);
        drawAfterLayer(InterfaceID.ToplevelPreEoc.MINIMAP);
    }
    @Override public Dimension render(Graphics2D graphics){
        if(!config.highlightOpponent())return null;
        Player opponent=OpponentLocator.find(client,plugin.status(),plugin.accountMatches());
        if(opponent==null)return null;
        Point point=opponent.getMinimapLocation();
        if(point!=null)OverlayUtil.renderMinimapLocation(graphics,point,RankedNhPanel.ACCENT);
        return null;
    }
}
