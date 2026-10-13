package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Actual history component with labelled sample data, including empty/error states. */
public class MatchHistoryPreview {
    public static void main(String[] args)throws Exception{
        SwingUtilities.invokeAndWait(()->{
            try{
                net.runelite.client.ui.laf.RuneLiteLAF.setup();
                BufferedImage image=new BufferedImage(741,380,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(RankedNhPanel.BG);g.fillRect(0,0,image.getWidth(),image.getHeight());
                g.setColor(RankedNhPanel.MUTED);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.drawString("Match history · Sample data",12,22);
                List<Models.Match> rows=new ArrayList<>();
                for(int i=0;i<12;i++){
                    Models.Match match=MatchHistoryTest.match(i%3==2?"void":"complete",i%3==0?"me":"them");
                    match.opponent.rsn=i==1?"Twelve Chars":"Demo Main";match.rated=i%2==0;match.ratingChange=i%3==0?38:-27;
                    if(i%3==1)match.score=new int[]{1,2};rows.add(match);
                }
                for(int i=0;i<3;i++){
                    MatchHistoryPanel panel=new MatchHistoryPanel(()->{},()->{});panel.open();
                    if(i==2)panel.failed(panel.request());else panel.show(panel.request(),"me",i==0?rows:List.of());
                    panel.setSize(225,panel.getPreferredSize().height);layout(panel);
                    g.translate(12+i*247,40);panel.printAll(g);g.translate(-12-i*247,-40);
                }
                g.dispose();ImageIO.write(image,"png",new File(args[0]));
                RankedNhPanel panel=new RankedNhPanel(new RankedNhPanel.Actions(){
                    public void connect(char[] k){} public void queue(String r){} public void leave(){} public void accept(boolean a){} public void hop(){}
                    public void ready(){} public void forfeit(){} public void review(){} public void leaderboard(){} public void history(){} public void showReport(){}
                });
                Models.Status status=MatchupTest.status();status.match=null;status.regions=List.of("EU","US");status.crossRegionAfterSeconds=30;
                status.player.rank=new Models.Rank();status.player.rank.name="Unranked";status.player.rank.badge="unranked";
                panel.render(status,"",false,true,"",null);
                for(Component child:panel.getComponents())if(child instanceof javax.swing.JButton && "Match history".equals(((javax.swing.JButton)child).getText()))((javax.swing.JButton)child).doClick();
                panel.showHistory(panel.historyRequest(),"me",rows);panel.setSize(225,1040);layout(panel);
                BufferedImage sidebar=new BufferedImage(249,1080,BufferedImage.TYPE_INT_RGB);Graphics2D sg=sidebar.createGraphics();
                sg.setColor(RankedNhPanel.BG);sg.fillRect(0,0,249,1080);sg.setColor(RankedNhPanel.MUTED);sg.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));sg.drawString("Sidebar · Sample data",12,20);
                sg.translate(12,32);panel.printAll(sg);sg.dispose();ImageIO.write(sidebar,"png",new File(new File(args[0]).getParentFile(),"history-sidebar-preview.png"));
            }catch(Exception e){throw new RuntimeException(e);}
        });
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
