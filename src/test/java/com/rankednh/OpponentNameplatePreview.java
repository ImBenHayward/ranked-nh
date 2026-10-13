package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Real renderer, labelled synthetic rank examples at normal and distant camera sizes. */
public class OpponentNameplatePreview {
    public static void main(String[] args)throws Exception{
        BufferedImage canvas=new BufferedImage(800,244,BufferedImage.TYPE_INT_RGB);Graphics2D g=canvas.createGraphics();
        g.setColor(new Color(35,39,43));g.fillRect(0,0,800,244);g.setColor(new Color(231,235,232));
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.drawString("Opponent labels · Preview",20,26);
        for(int row=0;row<2;row++){
            double scale=row==0?1:OpponentNameplate.scale(20);
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.setColor(new Color(159,167,164));
            g.drawString(row==0?"Normal camera":"Zoomed out",20,65+row*92);
            String[] names={"73u","Demo Opponent","Preview Main"};String[] badges={"unranked","dragonstone_iii","zenyte_i"};
            for(int i=0;i<3;i++){
                BufferedImage label=OpponentNameplate.image(names[i],badges[i]);
                int width=(int)Math.round(label.getWidth()*scale*1.5),height=(int)Math.round(label.getHeight()*scale*1.5);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(label,20+i*260,76+row*92,width,height,null);
            }
        }
        g.dispose();ImageIO.write(canvas,"png",new File(args[0]));
    }
}
