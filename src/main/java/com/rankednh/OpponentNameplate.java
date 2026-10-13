package com.rankednh;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import net.runelite.client.util.ImageUtil;

/** A small, opaque nameplate stays legible against terrain and other player text. */
final class OpponentNameplate
{
    static BufferedImage image(String name){return image(name,"unranked");}
    static BufferedImage image(String name,String badge){
        BufferedImage measure=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
        Graphics2D metrics=measure.createGraphics();
        Font font=new Font(Font.SANS_SERIF,Font.BOLD,13);metrics.setFont(font);
        int width=metrics.getFontMetrics().stringWidth(name)+42;metrics.dispose();
        BufferedImage image=new BufferedImage(width,30,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();
        try{
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(23,27,29,245));g.fillRoundRect(0,0,width,26,6,6);
            g.drawImage(ImageUtil.resizeImage(BadgeImages.image(badge),20,20),6,3,null);
            g.setFont(font);g.setColor(new Color(239,242,239));
            g.drawString(name,30,18);
            g.setColor(new Color(23,27,29,245));g.fillPolygon(new int[]{width/2-3,width/2+3,width/2},new int[]{26,26,30},3);
        }finally{g.dispose();}
        return image;
    }
    // Projected actor height follows camera distance, zoom and pitch. Never grow past native size.
    static double scale(double projectedHeight){return Math.max(0.65,Math.min(1,projectedHeight/48.0));}
    private OpponentNameplate(){}
}
