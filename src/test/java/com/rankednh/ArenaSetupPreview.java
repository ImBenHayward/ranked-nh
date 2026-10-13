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

/** Actual sidebar UI with synthetic kits, clearly labelled as a preview. */
public class ArenaSetupPreview {
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage image=new BufferedImage(996,850,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();g.setColor(new Color(20,23,26));g.fillRect(0,0,996,850);
                g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));
                g.drawString("Arena setup preview · Select build → Check kits → Flag changes · Default-kit scenarios",12,24);
                for(int i=0;i<4;i++){
                    Models.Status s=MatchupTest.status();s.match.state="active";s.regions=List.of("EU","US");
                    s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
                    RankedNhPanel p=new RankedNhPanel(new RankedNhPanel.Actions(){
                        public void connect(char[] k){}public void queue(String r){}public void leave(){}public void accept(boolean b){}public void hop(){}
                        public void ready(){}public void forfeit(){}public void review(){}public void leaderboard(){}public void history(){}public void showReport(){}
                    });
                    p.render(s,"",false,true,"",MatchupView.from(s,true,false,558,true,true,false,false,0));
                    ArenaKit kit=TestArenaKit.DEFAULT.kit(),edited=new ArenaKit(241,0,1,1,kit.equipment,kit.inventory);edited.runeSelection="Disabled: Fire rune";
                    ArenaSetup.View v=i==0?TestArenaKit.SETUP.describe(true,1,0,false,false,kit,null,null):
                        i==1?TestArenaKit.SETUP.describe(true,2,0,true,false,kit,null,null):
                        i==2?TestArenaKit.SETUP.describe(true,2,0,true,false,kit,kit,null):
                        TestArenaKit.SETUP.describe(true,2,0,true,true,edited,kit,kit);
                    p.renderSetup(v,"",false);p.setSize(225,792);layout(p);
                    g.translate(12+i*249,40);p.printAll(g);g.translate(-12-i*249,-40);
                }
                g.dispose();ImageIO.write(image,"png",new File(args[0]));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
