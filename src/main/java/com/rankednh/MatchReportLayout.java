package com.rankednh;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

/** The renderer and input handler share exactly the same canvas coordinates. */
final class MatchReportLayout
{
    static final int WIDTH=440,HEIGHT=324;
    static final Rectangle CLOSE=new Rectangle(396,6,36,36),CONTINUE=new Rectangle(272,278,144,30);
    final Rectangle canvas;
    final double scale,x,y;

    MatchReportLayout(int width,int height,double entrance) {
        this(new Rectangle(0,0,width,height),entrance);
    }
    MatchReportLayout(Rectangle viewport,double entrance){
        canvas=new Rectangle(viewport);
        scale=Math.max(0.01,Math.min(1,Math.min((viewport.width-40)/(double)WIDTH,(viewport.height-40)/(double)HEIGHT)));
        x=viewport.x+(viewport.width-WIDTH*scale)/2;
        y=viewport.y+(viewport.height-HEIGHT*scale)/2+8*scale*(1-entrance);
    }
    boolean contains(Point point){Point2D p=local(point);return p.getX()>=0 && p.getY()>=0 && p.getX()<WIDTH && p.getY()<HEIGHT;}
    Point2D local(Point point){return new Point2D.Double((point.x-x)/scale,(point.y-y)/scale);}
    int control(Point point){
        Point2D local=local(point);
        return CLOSE.contains(local)?1:CONTINUE.contains(local)?2:0;
    }
}
