package com.rankednh;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.runelite.client.ui.laf.RuneLiteLAF;

/** The actual queue cards in RuneLite's theme, with sample search timestamps. */
public class MatchmakingPreview {
    public static void main(String[] args)throws Exception{
        UIManager.setLookAndFeel(new RuneLiteLAF());
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage image=new BufferedImage(930,320,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(RankedNhPanel.BG);g.fillRect(0,0,930,320);g.setColor(RankedNhPanel.MUTED);
                g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.drawString("Matchmaking preview · Sample search times",16,22);
                for(int i=0;i<4;i++){
                    Models.Status status=new Models.Status();status.mode="pilot";status.regions=java.util.List.of("EU","US");
                    status.player=new Models.Player();status.player.id="preview";status.player.rank=new Models.Rank();
                    status.player.rank.name="Unranked";status.player.rank.badge="unranked";status.crossRegionAfterSeconds=30;
                    status.queued=i==1 || i==2;status.queueRegion="EU";status.joinedAt="1970-01-01T00:00:00Z";
                    if(i==3){
                        status.match=MatchupTest.status().match;status.match.state="preparing";status.match.rated=true;
                        status.match.deadline="1970-01-01T01:00:00Z";
                    }
                    RankedNhPanel.Actions actions=(RankedNhPanel.Actions)Proxy.newProxyInstance(RankedNhPanel.Actions.class.getClassLoader(),new Class<?>[]{RankedNhPanel.Actions.class},(p,m,a)->null);
                    RankedNhPanel panel=new RankedNhPanel(actions);
                    panel.render(status,"",false,true,"",i==3?MatchupView.from(status,true,false,301,false,false,false,false,0):null,i==1?12_000:72_000);
                    panel.setSize(241,900);layout(panel);
                    JPanel card=(JPanel)find(panel,"Find a match").getParent().getParent();
                    g.translate(16+i*229,42);card.printAll(g);g.translate(-16-i*229,-42);panel.dispose();
                }
                g.dispose();ImageIO.write(image,"png",new File(args[0]));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static JButton find(Container root,String text){
        for(Component child:root.getComponents()){
            if(child instanceof JButton && text.equals(((JButton)child).getText()))return (JButton)child;
            if(child instanceof Container){JButton found=find((Container)child,text);if(found!=null)return found;}
        }return null;
    }
    private static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container)layout((Container)child);}
}
