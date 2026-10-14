package com.rankednh;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Render the actual sidebar with labelled sample data; no game connection. */
public class MatchupPreview
{
    private static final RankedNhPanel.Actions NOOP=new RankedNhPanel.Actions(){
        public void connect(char[] k){} public void queue(String region){} public void leave(){} public void accept(boolean b){} public void hop(){}
        public void ready(){} public void forfeit(){} public void review(){} public void leaderboard(){} public void history(){} public void showReport(){}
    };
    public static void main(String[] args)throws Exception{
        javax.swing.UIManager.setLookAndFeel(new net.runelite.client.ui.laf.RuneLiteLAF());
        File output=new File(args[0]);
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage sidebar=new BufferedImage(747,724,BufferedImage.TYPE_INT_RGB);Graphics2D p=sidebar.createGraphics();
                p.setColor(new Color(16,19,22));p.fillRect(0,0,747,724);p.setColor(RankedNhPanel.TEXT);p.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));
                p.drawString("Sidebar preview · Accept → Choose world → Check-in",12,24);
                for(int i=0;i<3;i++){
                    Models.Status s=MatchupTest.status();s.regions=List.of("EU","US");s.crossRegionAfterSeconds=30;
                    s.player.games=32;s.player.wins=21;s.player.rating=2202;s.player.rank=new Models.Rank();
                    s.player.rank.name="Dragonstone III";s.player.rank.badge="dragonstone_iii";s.player.rank.nextAt=2220;
                    RankedNhPanel panel=new RankedNhPanel(NOOP);
                    MatchupView handoff=MatchupView.preview(i);
                    panel.render(s,"",false,true,"",handoff);
                    panel.setSize(225,668);layout(panel);p.translate(12+i*249,40);panel.printAll(p);p.translate(-12-i*249,-40);
                }
                p.dispose();ImageIO.write(sidebar,"png",output);ImageIO.write(sidebar,"png",new File(output.getParentFile(),"matchup-sidebar-preview.png"));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
