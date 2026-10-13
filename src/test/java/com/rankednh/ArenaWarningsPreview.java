package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.components.TooltipComponent;

/** Explicit composites over the supplied screenshots, using production warning drawing and RuneLite tooltips. */
public class ArenaWarningsPreview {
    public static void main(String[] args)throws Exception{
        File folder=new File(args[0]);
        BufferedImage image=new BufferedImage(1080,576,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(RankedNhPanel.BG);g.fillRect(0,0,1080,576);g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));
        g.drawString("Warning previews · Composited over your screenshots",16,24);
        g.drawString("Wrong build: points to Max/Med",16,54);
        BufferedImage build=ImageIO.read(new File(folder,"build-warning-crop.png"));
        g.translate(16,66);g.drawImage(build,0,0,null);ArenaSetupWarningOverlay.drawBuildWarning(g,new Rectangle(82,99,78,18));g.translate(-16,-66);
        g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.drawString("One wrong item: hover for the replacement",16,264);
        BufferedImage kit=ImageIO.read(new File(folder,"kit-warning-crop.png"));
        // Display only the kit area here to keep the combined preview compact.
        g.translate(16,280);g.drawImage(kit,0,0,520,280,0,53,520,333,null);
        ArenaSetupWarningOverlay.drawSlots(g,List.of(new Rectangle(82,97,34,34)));
        tooltip(g,"You · Body<br>Yours: Empty<br>Default: Virtus robe top",126,102);g.translate(-16,-280);
        g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.drawString("Many wrong items: quiet marks, one tooltip",552,54);
        g.translate(552,66);g.drawImage(kit,0,0,null);
        // Simulate an emptied inventory so the highlighted replacements are visibly necessary.
        g.setColor(new Color(kit.getRGB(180,70)));g.fillRect(177,70,158,232);
        List<Rectangle> slots=new ArrayList<>();slots.add(new Rectangle(82,150,34,34));
        for(int row=0;row<7;row++)for(int col=0;col<4;col++)slots.add(new Rectangle(179+col*39,72+row*32,32,32));
        ArenaSetupWarningOverlay.drawSlots(g,slots);tooltip(g,"You · Inventory slot 1<br>Yours: Empty<br>Default: Super combat potion(4)",180,112);g.translate(-552,-66);
        g.setColor(RankedNhPanel.MUTED);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));
        g.drawString("The sidebar shows one fix, then moves to the next as you correct it.",552,430);
        g.drawString("Markers never cover the item artwork or point at Confirm.",552,450);
        g.dispose();ImageIO.write(image,"png",new File(folder,"arena-warnings-preview.png"));
    }
    private static void tooltip(Graphics2D g,String text,int x,int y){
        g.setFont(FontManager.getRunescapeSmallFont());TooltipComponent tooltip=new TooltipComponent();
        tooltip.setText(text);tooltip.setPosition(new Point(x,y));tooltip.render(g);
    }
}
