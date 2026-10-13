package com.rankednh;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Points to the nearest supplies crate only while the first-time check needs it. */
final class ArenaSuppliesOverlay extends Overlay {
    private final Client client;
    private final RankedNhPlugin plugin;
    private final Map<Integer,Boolean> supplies=new HashMap<>();
    private GameObject crate;
    @Inject ArenaSuppliesOverlay(Client client,RankedNhPlugin plugin){
        this.client=client;this.plugin=plugin;
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_SCENE);setMovable(false);
    }
    void update(){
        crate=null;
        if(plugin.onboardingStep()!=ArenaOnboarding.Step.CRATE || client.getRevision()!=ArenaSetup.REVISION ||
            client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null)return;
        Tile[][] plane=client.getScene().getTiles()[client.getPlane()];int nearest=Integer.MAX_VALUE;
        for(Tile[] row:plane)for(Tile tile:row){
            if(tile==null)continue;
            for(GameObject object:tile.getGameObjects()){
                if(object==null || !supplies.computeIfAbsent(object.getId(),id->{
                    String name=client.getObjectDefinition(id).getName();
                    return "Supplies".equalsIgnoreCase(name) || "Supplies crate".equalsIgnoreCase(name);
                }))continue;
                int distance=object.getWorldLocation().distanceTo(client.getLocalPlayer().getWorldLocation());
                if(distance<nearest){nearest=distance;crate=object;}
            }
        }
    }
    @Override public Dimension render(Graphics2D graphics){
        if(crate==null || plugin.onboardingStep()!=ArenaOnboarding.Step.CRATE || client.getGameState()!=GameState.LOGGED_IN)return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.clipRect(client.getViewportXOffset(),client.getViewportYOffset(),client.getViewportWidth(),client.getViewportHeight());
            Polygon tile=crate.getCanvasTilePoly();
            if(tile!=null)OverlayUtil.renderPolygon(g,tile,RankedNhPanel.ACCENT,new Color(155,198,173,24),new BasicStroke(2));
            Point text=crate.getCanvasTextLocation(g,"Set up your kit",70);
            if(text!=null)OverlayUtil.renderTextLocation(g,text,"Set up your kit",RankedNhPanel.ACCENT);
        }finally{g.dispose();}
        return null;
    }
}
