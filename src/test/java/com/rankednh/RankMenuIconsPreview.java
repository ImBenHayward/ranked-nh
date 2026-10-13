package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.ImageUtil;

/** Labelled visual preview; no live menu or rating is changed. */
public class RankMenuIconsPreview {
    public static void main(String[] args)throws Exception{
        BufferedImage image=new BufferedImage(660,284,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(27,30,34));g.fillRect(0,0,660,284);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,13));g.setColor(new Color(215,216,210));
        g.drawString("Menu badge preview · Unranked and Sapphire I samples",16,24);
        for(int col=0;col<2;col++){
            BufferedImage menu=new BufferedImage(310,224,BufferedImage.TYPE_INT_RGB);Graphics2D m=menu.createGraphics();
            m.setColor(new Color(93,85,66));m.fillRect(0,0,310,224);m.setColor(Color.BLACK);m.fillRect(1,1,308,21);m.drawRect(1,1,308,222);
            m.setFont(FontManager.getRunescapeBoldFont());m.setColor(new Color(93,85,66));m.drawString("Choose Option",4,17);
            String[] options={"Walk here","Challenge","Follow","Load-outs","Lookup","Report"};
            BufferedImage badge=BadgeImages.menuImage(col==0?"unranked":"sapphire_i");
            for(int row=0;row<options.length;row++){
                int y=43+row*25;String option=options[row]+" ";int x=4+m.getFontMetrics().stringWidth(option);
                m.setColor(Color.BLACK);m.drawString(option,5,y+1);m.drawString("Other Main (skill-2500)",x+16,y+1);
                m.setColor(Color.WHITE);m.drawString(option,4,y);m.drawImage(badge,x,y-12,null);m.drawString("Other Main (skill-2500)",x+15,y);
            }
            m.setColor(Color.WHITE);m.drawString("Cancel",4,211);m.dispose();g.drawImage(menu,16+col*326,44,null);
        }
        g.dispose();ImageIO.write(image,"png",new File(args[0]));
    }
}
