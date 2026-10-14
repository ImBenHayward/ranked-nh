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
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/** Points to the nearest supplies crate only while the first-time check needs it. */
final class ArenaSuppliesOverlay extends Overlay {
    static final Color GUIDE=RankedNhPanel.ACCENT;
    private final Client client;
    private final RankedNhPlugin plugin;
    private final ModelOutlineRenderer outlines;
    private final Map<Integer,Boolean> supplies=new HashMap<>();
    private GameObject crate;
    @Inject ArenaSuppliesOverlay(Client client,RankedNhPlugin plugin,ModelOutlineRenderer outlines){
        this.client=client;this.plugin=plugin;this.outlines=outlines;
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
        if(crate==null || plugin.onboardingStep()!=ArenaOnboarding.Step.CRATE || client.getGameState()!=GameState.LOGGED_IN ||
            client.getLocalPlayer()==null)return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.clipRect(client.getViewportXOffset(),client.getViewportYOffset(),client.getViewportWidth(),client.getViewportHeight());
            outlines.drawOutline(crate,3,GUIDE,0);
            Polygon tile=crate.getCanvasTilePoly();
            if(tile!=null)OverlayUtil.renderPolygon(g,tile,GUIDE,new Color(236,4,4,28),new BasicStroke(2));
            LocalPoint[] ground=arrow(client.getLocalPlayer().getLocalLocation(),crate.getLocalLocation());
            Polygon arrow=new Polygon();
            for(LocalPoint vertex:ground){
                Point screen=Perspective.localToCanvas(client,vertex,client.getPlane(),0);
                if(screen==null){arrow=null;break;}
                arrow.addPoint(screen.getX(),screen.getY());
            }
            if(arrow!=null && arrow.npoints>=3)drawArrow(g,arrow);
            Point text=crate.getCanvasTextLocation(g,"Set up your kit",70);
            if(text!=null)OverlayUtil.renderTextLocation(g,text,"Set up your kit",RankedNhPanel.TEXT);
        }finally{g.dispose();}
        return null;
    }
    /** A short directional cue on the ground, not a walking route through scene obstacles. */
    static LocalPoint[] arrow(LocalPoint from,LocalPoint to){
        if(from==null || to==null || from.getWorldView()!=to.getWorldView())return new LocalPoint[0];
        double dx=to.getX()-from.getX(),dy=to.getY()-from.getY(),distance=Math.hypot(dx,dy);
        double start=80,length=Math.min(192,distance-start-96);
        if(length<64)return new LocalPoint[0];
        double ux=dx/distance,uy=dy/distance,neck=length-Math.min(64,length*.45);
        double[][] points={{0,-15},{neck,-15},{neck,-43},{length,0},{neck,43},{neck,15},{0,15}};
        LocalPoint[] polygon=new LocalPoint[points.length];
        for(int i=0;i<points.length;i++){
            double along=start+points[i][0],side=points[i][1];
            polygon[i]=from.plus((int)Math.round(ux*along-uy*side),(int)Math.round(uy*along+ux*side));
        }
        return polygon;
    }
    static void drawArrow(Graphics2D g,Polygon arrow){
        OverlayUtil.renderPolygon(g,arrow,new Color(65,20,23),new Color(236,4,4,220),new BasicStroke(2));
    }
}
