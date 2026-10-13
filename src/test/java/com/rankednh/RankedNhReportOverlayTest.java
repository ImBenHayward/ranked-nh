package com.rankednh;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.junit.Test;
import static org.junit.Assert.*;

public class RankedNhReportOverlayTest
{
    private GameState state=GameState.LOGGED_IN;
    private boolean battle;
    private final PlacementRevealAudioTest.Player sound=new PlacementRevealAudioTest.Player();
    private final RankedNhConfig config=new RankedNhConfig(){
        @Override public boolean previewMatchReport(){return true;}
        @Override public boolean reduceReportMotion(){return true;}
    };
    private RankedNhReportOverlay overlay(){
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args)->{
            switch(method.getName()){
                case "getGameState":return state;
                case "getCanvasWidth":return 765;
                case "getCanvasHeight":return 503;
                case "getViewportXOffset":return 4;
                case "getViewportYOffset":return 4;
                case "getViewportWidth":return 512;
                case "getViewportHeight":return 334;
                default:throw new UnsupportedOperationException(method.getName());
            }
        });
        return new RankedNhReportOverlay(new RankedNhPlugin(){@Override boolean inArenaBattle(){return battle;}},config,client,null,new PlacementRevealAudio(sound),new RankSpinImages(Runnable::run));
    }
    private Dimension render(RankedNhReportOverlay overlay){
        Graphics2D g=new BufferedImage(765,503,BufferedImage.TYPE_INT_RGB).createGraphics();
        try{return overlay.render(g);}finally{g.dispose();}
    }
    @Test public void savedDragPositionsCannotMoveTheViewportCenteredReport(){
        RankedNhReportOverlay overlay=overlay();
        overlay.setPreferredLocation(new Point(400,400));overlay.setPreferredPosition(OverlayPosition.BOTTOM_RIGHT);
        overlay.setPreferredSize(new Dimension(292,332));overlay.setBounds(new Rectangle(400,400,292,332));
        assertNull(overlay.getPreferredLocation());assertNull(overlay.getPreferredPosition());assertNull(overlay.getPreferredSize());
        assertFalse(overlay.isMovable());assertFalse(overlay.isSnappable());assertFalse(overlay.isResizable());
        assertEquals(OverlayLayer.ALWAYS_ON_TOP,overlay.getLayer());
        assertEquals(new Dimension(765,503),render(overlay));assertEquals(new Point(0,0),overlay.getBounds().getLocation());
    }
    @Test public void escapeStaysDismissedAcrossRendersAndReopenRestoresTheReport(){
        RankedNhReportOverlay overlay=overlay();assertNotNull(render(overlay));assertTrue(overlay.input.visible());
        KeyEvent escape=new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27);
        overlay.input.keyPressed(escape);assertTrue(escape.isConsumed());
        assertNull(render(overlay));assertNull(render(overlay));assertFalse(overlay.input.visible());
        overlay.reopen();assertNotNull(render(overlay));
        state=GameState.LOGIN_SCREEN;assertNull(render(overlay));assertFalse(overlay.input.visible());
    }
    @Test public void nativeBattleSuppressesEvenManuallyOpenedReportsAndReleasesInput(){
        RankedNhReportOverlay overlay=overlay();assertNotNull(render(overlay));
        battle=true;overlay.reopen();assertNull(render(overlay));assertFalse(overlay.input.visible());
        KeyEvent escape=new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27);
        overlay.input.keyPressed(escape);assertFalse(escape.isConsumed());
        battle=false;assertNotNull(render(overlay));
    }
    @Test public void placementSoundStartsOnlyWithVisibleReportAndStopsOnDismissalOrLogout(){
        RankedNhReportOverlay overlay=overlay();overlay.preview(RankPreview.PLACEMENTS);
        battle=true;assertNull(render(overlay));assertEquals(0,sound.plays);
        battle=false;assertNotNull(render(overlay));assertEquals(1,sound.plays);assertEquals(1800,sound.offset);
        render(overlay);assertEquals(1,sound.plays);
        overlay.input.keyPressed(new KeyEvent(new Canvas(),KeyEvent.KEY_PRESSED,0,0,KeyEvent.VK_ESCAPE,(char)27));
        assertEquals(1,sound.stops);assertNull(render(overlay));
        overlay.preview(RankPreview.PLACEMENTS);render(overlay);assertEquals(2,sound.plays);
        state=GameState.LOGIN_SCREEN;assertNull(render(overlay));assertEquals(2,sound.stops);
        state=GameState.LOGGED_IN;render(overlay);assertEquals(2,sound.plays);
        overlay.preview(RankPreview.PLACEMENTS);render(overlay);overlay.reset();assertEquals(3,sound.stops);
    }
}
