package com.rankednh;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.Icon;

/** Tiny vector region flags remain sharp under RuneLite UI scaling. */
final class RegionFlag implements Icon {
    private final String region;
    RegionFlag(String region){this.region=region;}
    public int getIconWidth(){return 16;}
    public int getIconHeight(){return 12;}
    public void paintIcon(Component c,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
        try{
            g.setColor(new Color(32,62,130));g.fillRect(0,0,16,12);
            if("EU".equals(region)){
                g.setColor(new Color(247,206,76));
                for(int i=0;i<12;i++){double a=i*Math.PI/6;g.fillRect((int)Math.round(7.5+4*Math.cos(a)),(int)Math.round(5.5+4*Math.sin(a)),1,1);}
            }else if("US".equals(region)){
                for(int i=0;i<12;i++){g.setColor(i%2==0?new Color(188,59,67):new Color(234,233,225));g.fillRect(0,i,16,1);}
                g.setColor(new Color(38,59,105));g.fillRect(0,0,7,7);g.setColor(new Color(234,233,225));
                for(int row=0;row<3;row++)for(int col=0;col<3;col++)g.fillRect(1+col*2,1+row*2,1,1);
            }else{
                // Globe for regions without a supplied flag; the adjacent text always names the region.
                g.setColor(RankedNhPanel.MUTED);g.drawOval(2,1,11,10);g.drawOval(5,1,5,10);g.drawLine(2,6,13,6);
            }
        }finally{g.dispose();}
    }
}
