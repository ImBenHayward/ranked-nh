package com.rankednh;

import java.awt.*;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.*;

/** A non-interactive line in the scene; ordinary interfaces paint over it. */
final class RoundNoticeOverlay extends Overlay {
    private final Client client;private final RankedNhPlugin plugin;
    @Inject RoundNoticeOverlay(Client client,RankedNhPlugin plugin){
        this.client=client;this.plugin=plugin;setLayer(OverlayLayer.ABOVE_SCENE);setPosition(OverlayPosition.DYNAMIC);setMovable(false);
    }
    @Override public Dimension render(Graphics2D graphics){
        String text=plugin.roundNotice();if(text.isEmpty())return null;
        Graphics2D g=(Graphics2D)graphics.create();
        try{
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,11));int width=g.getFontMetrics().stringWidth(text)+20;
            int x=client.getViewportXOffset()+(client.getViewportWidth()-width)/2,y=client.getViewportYOffset()+10;
            g.setColor(new Color(35,29,24));g.fillRect(x,y,width,24);
            g.setColor(new Color(234,173,115));g.drawString(text,x+10,y+16);
        }finally{g.dispose();}return null;
    }
}
