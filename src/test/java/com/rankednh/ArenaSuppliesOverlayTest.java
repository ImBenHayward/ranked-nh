package com.rankednh;

import net.runelite.api.coords.LocalPoint;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaSuppliesOverlayTest {
    @Test public void groundArrowPointsTowardTheCrateAndStopsBeforeIt(){
        LocalPoint from=new LocalPoint(2000,2000,-1);
        for(int[] offset:new int[][]{{512,0},{0,-512},{512,512},{-256,128}}){
            LocalPoint to=from.plus(offset[0],offset[1]);LocalPoint[] arrow=ArenaSuppliesOverlay.arrow(from,to);
            assertEquals(7,arrow.length);
            double dx=to.getX()-from.getX(),dy=to.getY()-from.getY(),distance=Math.hypot(dx,dy);
            LocalPoint tip=arrow[3];double tx=tip.getX()-from.getX(),ty=tip.getY()-from.getY();
            assertEquals("Tip follows the target direction",0,(tx*dy-ty*dx)/distance,1);
            double along=(tx*dx+ty*dy)/distance;
            assertTrue(along>80);assertTrue(along<=273);assertTrue(distance-along>=95);
            for(LocalPoint point:arrow)assertEquals(from.getWorldView(),point.getWorldView());
        }
    }
    @Test public void noArrowWhenAlreadyAtTheCrateOrCoordinatesAreUnavailable(){
        LocalPoint from=new LocalPoint(2000,2000,-1);
        assertEquals(0,ArenaSuppliesOverlay.arrow(from,from).length);
        assertEquals(0,ArenaSuppliesOverlay.arrow(from,from.plus(128,0)).length);
        assertEquals(0,ArenaSuppliesOverlay.arrow(from,new LocalPoint(2500,2000,1)).length);
        assertEquals(0,ArenaSuppliesOverlay.arrow(null,from).length);
        assertEquals(0,ArenaSuppliesOverlay.arrow(from,null).length);
    }
}
