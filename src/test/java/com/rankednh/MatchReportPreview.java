package com.rankednh;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renders the same component as the in-game overlay; all values are explicit preview data. */
public class MatchReportPreview
{
    public static void main(String[] args) throws Exception {
        File output=new File(args[0]);File folder=output.getParentFile();
        MatchReportView win=MatchReportView.preview();
        ImageIO.write(render(win,1024,768,2000),"png",output);
        ImageIO.write(render(win,765,503,2000),"png",new File(folder,"match-report-fixed.png"));
        MatchReportView loss=MatchReportView.preview();
        loss.title="Defeat";loss.won=false;loss.score="1 – 2";loss.delta="-27";
        loss.ratingBefore=2202;loss.ratingAfter=2175;loss.rating="2202 → 2175 rating";
        ImageIO.write(render(loss,1024,768,2000),"png",new File(folder,"match-report-defeat.png"));
        MatchReportView unrated=MatchReportView.preview();
        unrated.rank="Unranked";unrated.badge="unranked";unrated.delta="";unrated.rating="Unrated match";
        unrated.ratingBefore=null;unrated.ratingAfter=null;unrated.divisionStart=null;unrated.nextAt=null;
        unrated.progressLabel="0 / 5 placements";unrated.progressDetail="5 rated matches to reveal your rank";unrated.progress=0;
        ImageIO.write(render(unrated,765,503,2000),"png",new File(folder,"match-report-unrated.png"));
        ImageIO.write(OpponentNameplate.image("Demo Opponent"),"png",new File(folder,"opponent-nameplate-preview.png"));
        File frames=new File(folder,"match-report-frames");frames.mkdirs();
        for(int i=0;i<=60;i++)ImageIO.write(render(win,900,700,i*50L),"png",new File(frames,String.format("%03d.png",i)));
    }
    private static BufferedImage render(MatchReportView view,int width,int height,long elapsed){
        BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(new Color(48,51,54));g.fillRect(0,0,width,height);
        MatchReportAnimation.Frame frame=MatchReportAnimation.sample(view,elapsed,false);
        Rectangle viewport=width==765?new Rectangle(4,4,512,334):new Rectangle(0,0,width,height);
        if(width==765){g.setColor(new Color(27,30,34));g.fillRect(0,342,width,height-342);g.fillRect(522,0,width-522,height);
            g.setColor(new Color(159,167,164));g.drawString("Chat area · Preview",24,380);g.drawString("Inventory · Preview",548,180);}
        MatchReportRenderer.render(g,view,frame,new MatchReportLayout(viewport,frame.entrance),0,0,0);
        g.dispose();return image;
    }
}
