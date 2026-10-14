package com.rankednh;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Renders the actual Swing panel with clearly synthetic preview data, without logging into OSRS. */
public class PanelPreview
{
    private static final RankedNhPanel.Actions NOOP=new RankedNhPanel.Actions(){
        public void connect(char[] k){} public void queue(String region){} public void leave(){} public void accept(boolean b){} public void hop(){}
        public void ready(){} public void forfeit(){} public void review(){} public void leaderboard(){} public void history(){} public void showReport(){}
    };
    public static void main(String[] args) throws Exception {
        javax.swing.UIManager.setLookAndFeel(new net.runelite.client.ui.laf.RuneLiteLAF());
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage image=new BufferedImage(996,920,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();g.setColor(new Color(20,23,26));g.fillRect(0,0,996,920);
                for(int i=0;i<4;i++){
                    RankedNhPanel panel=new RankedNhPanel(NOOP);
                    if(i>0){
                        Models.Status status=new Models.Status();status.mode="pilot";status.player=new Models.Player();
                        status.player.id="preview-player";status.player.rsn="Preview Main";status.player.games=32;status.player.wins=21;status.player.rating=1864;
                        status.player.rank=new Models.Rank();status.player.rank.name="Dragonstone III";status.player.rank.badge="dragonstone_iii";status.player.rank.nextAt=1920;
                        status.regions=java.util.List.of("EU","US");status.queueRegion="EU";status.crossRegionAfterSeconds=30;
                        if(i==2){status.queued=true;status.joinedAt=java.time.Instant.now().minusSeconds(12).toString();}
                        if(i==3){Models.Match m=new Models.Match();m.id="preview-match";m.state="preparing";m.world=558;m.rated=true;m.score=new int[]{1,1};m.round=3;
                            m.opponent=new Models.Opponent();m.opponent.rsn="Demo Opponent";m.region="EU";m.deadline=java.time.Instant.ofEpochMilli(60_000).toString();status.match=m;}
                        panel.render(status,"",false,true,i==3?"Deciding fight · restore supplies":"",MatchupView.from(status,true,false,301,false,false,false,false,0));
                    }
                    panel.setSize(225,888);layout(panel);g.translate(12+i*249,16);panel.printAll(g);g.translate(-12-i*249,-16);
                }
                g.dispose();ImageIO.write(image,"png",new File(args[0]));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
