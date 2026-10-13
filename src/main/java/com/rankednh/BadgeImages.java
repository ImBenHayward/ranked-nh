package com.rankednh;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ImageIcon;
import net.runelite.client.util.ImageUtil;

final class BadgeImages
{
    private static final Map<String,BufferedImage> CACHE=new HashMap<>();
    static synchronized BufferedImage image(String name) {
        String safe=name!=null && name.matches("[a-z_]+") ? name : "unranked";
        if(!CACHE.containsKey(safe)) {
            BufferedImage image=ImageUtil.loadImageResource(BadgeImages.class,"badges/"+safe+".png");
            CACHE.put(safe,image);
        }
        return CACHE.get(safe);
    }
    static ImageIcon icon(String name) { return new ImageIcon(image(name)); }
    static BufferedImage menuImage(String name){
        BufferedImage source=image(name);int left=source.getWidth(),top=source.getHeight(),right=-1,bottom=-1;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++)if((source.getRGB(x,y)>>>24)>32){
            left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);
        }
        if(right<left)return new BufferedImage(13,13,BufferedImage.TYPE_INT_ARGB);
        BufferedImage cropped=source.getSubimage(left,top,right-left+1,bottom-top+1);
        double scale=13.0/Math.max(cropped.getWidth(),cropped.getHeight());
        BufferedImage resized=ImageUtil.resizeImage(cropped,Math.max(1,(int)Math.round(cropped.getWidth()*scale)),Math.max(1,(int)Math.round(cropped.getHeight()*scale)));
        // Native indexed sprites only keep alpha 255. Resampling the Unranked
        // artwork produces alpha <=254 everywhere, making the whole icon vanish.
        BufferedImage menu=new BufferedImage(resized.getWidth(),resized.getHeight(),BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<resized.getHeight();y++)for(int x=0;x<resized.getWidth();x++){
            int argb=resized.getRGB(x,y);
            if((argb>>>24)<128)continue;
            int rgb=argb&0xffffff;
            // Palette entry zero is transparent in RuneLite's sprite converter.
            menu.setRGB(x,y,0xff000000|(rgb==0?1:rgb));
        }
        return menu;
    }
    private BadgeImages() {}
}
