package com.rankednh;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Render the real sidebar at each first-time step with synthetic data. */
public class OnboardingPreview {
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage image=new BufferedImage(1020,1000,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();g.setColor(new Color(20,23,26));g.fillRect(0,0,1020,1000);
                g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));
                g.drawString("First-time setup · Sidebar preview with sample data",16,24);
                ArenaOnboarding.Step[] steps={ArenaOnboarding.Step.HOP,ArenaOnboarding.Step.CRATE,ArenaOnboarding.Step.FIX,ArenaOnboarding.Step.READY};
                for(int i=0;i<steps.length;i++){
                    Models.Status s=new Models.Status();s.mode="pilot";s.rated=true;s.regions=List.of("EU","US");s.crossRegionAfterSeconds=30;
                    s.season=new Models.Season();s.season.id="beta-1";s.season.name="Beta season";s.season.beta=true;s.season.resetNotice="Beta ratings reset before Season 1.";
                    s.player=new Models.Player();s.player.id="preview";s.player.rsn="Preview";s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
                    RankedNhPanel.Actions actions=(RankedNhPanel.Actions)Proxy.newProxyInstance(RankedNhPanel.Actions.class.getClassLoader(),new Class<?>[]{RankedNhPanel.Actions.class},(p,m,a)->null);
                    RankedNhPanel panel=new RankedNhPanel(actions,"EU");panel.onboarding(steps[i],i!=3,false);panel.render(s,"",false,true,"");
                    ArenaSetup.View setup=null;ArenaKit kit=TestArenaKit.DEFAULT.kit();
                    if(i==2){ArenaKit wrong=new ArenaKit(241,0,0,kit.runePouch,kit.equipment,kit.inventory);setup=TestArenaKit.SETUP.describe(false,0,0,false,false,wrong,null,null);}
                    if(i==3)setup=TestArenaKit.SETUP.describe(false,0,0,false,false,kit,null,null);
                    panel.renderSetup(setup,"",false);panel.renderOnboarding(setup);panel.setSize(240,940);layout(panel);
                    g.translate(8+i*255,40);panel.printAll(g);g.translate(-8-i*255,-40);
                }
                g.dispose();ImageIO.write(image,"png",new File(args[0]));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container parent){parent.doLayout();for(Component child:parent.getComponents())if(child instanceof Container)layout((Container)child);}
}
