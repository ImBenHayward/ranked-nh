package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Uses the production renderers with labelled samples; never connects or changes account data. */
public class RankExperiencePreview {
    private static final RankSpinImages SPINS=new RankSpinImages(Runnable::run);
    private static final RankedNhPanel.Actions NOOP=new RankedNhPanel.Actions(){
        public void connect(char[] k){}public void queue(String r){}public void leave(){}public void accept(boolean b){}public void hop(){}
        public void ready(){}public void forfeit(){}public void review(){}public void leaderboard(){}public void history(){}public void showReport(){}
    };
    public static void main(String[] args)throws Exception{
        File folder=new File(args[0]);folder.mkdirs();
        SwingUtilities.invokeAndWait(()->{
            try{
                BufferedImage image=new BufferedImage(498,664,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
                g.setColor(RankedNhPanel.BG);g.fillRect(0,0,image.getWidth(),image.getHeight());
                g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.drawString("Sidebar preview · Sample match",24,24);
                for(int i=0;i<2;i++){
                    Models.Status s=MatchupTest.status();s.match.state="active";s.match.score=new int[]{1,0};s.regions=List.of("EU","US");
                    s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
                    s.match.opponent.rsn="Demo Opponent";s.match.opponent.rank=new Models.Rank();
                    s.match.opponent.rank.name="Ruby II";s.match.opponent.rank.badge="ruby_ii";
                    RankedNhPanel panel=new RankedNhPanel(NOOP);
                    panel.render(s,"",false,true,"",MatchupView.from(s,true,false,558,true,true,false,false,0));
                    if(i==1){
                        ArenaKit base=TestArenaKit.DEFAULT.kit();java.util.ArrayList<ArenaKit.Slot> mine=new java.util.ArrayList<>(base.equipment),theirs=new java.util.ArrayList<>(base.equipment);
                        mine.set(4,new ArenaKit.Slot(26243,1,"Virtus robe top"));theirs.set(4,new ArenaKit.Slot(4712,1,"Ahrim's robetop"));
                        ArenaSetup.View view=TestArenaKit.SETUP.describe(true,2,0,true,false,new ArenaKit(241,0,1,0,mine,base.inventory),new ArenaKit(241,0,1,0,theirs,base.inventory),null);
                        panel.renderSetup(view,"",false);
                    }
                    panel.setSize(225,612);layout(panel);g.translate(12+i*249,40);panel.printAll(g);g.translate(-12-i*249,-40);
                }
                g.dispose();ImageIO.write(image,"png",new File(folder,"match-cleanup-preview.png"));
            }catch(Exception e){throw new RuntimeException(e);}
        });
        File frames=new File(folder,"rank-up-frames");frames.mkdirs();
        for(int i=0;i<240;i++)ImageIO.write(report(RankPreview.RANK_UP,Math.round(i*1000/60.0)),"png",new File(frames,String.format("%03d.png",i)));
        File placementFrames=new File(folder,"placement-reveal-frames");placementFrames.mkdirs();
        for(int i=0;i<240;i++)ImageIO.write(report(RankPreview.PLACEMENTS,Math.round(i*1000/60.0)),"png",new File(placementFrames,String.format("%03d.png",i)));
        byte[] audio=PlacementRevealAudio.pcm(new RankedNhConfig(){}.placementRevealVolume());
        try(javax.sound.sampled.AudioInputStream stream=new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(audio),
            new javax.sound.sampled.AudioFormat(44100,16,1,true,false),audio.length/2)){
            javax.sound.sampled.AudioSystem.write(stream,javax.sound.sampled.AudioFileFormat.Type.WAVE,new File(folder,"placement-reveal.wav"));
        }
        for(RankPreview scenario:RankPreview.values())ImageIO.write(report(scenario,2800),"png",new File(folder,"rank-preview-"+scenario.name().toLowerCase(java.util.Locale.ROOT)+".png"));
        // Render timing includes Java2D image scaling/compositing at the actual card size.
        long[] times=new long[240];
        for(int i=0;i<times.length;i++){long before=System.nanoTime();report(RankPreview.PLACEMENTS,Math.round(700+i*1100/240.0));times[i]=System.nanoTime()-before;}
        java.util.Arrays.sort(times);
        System.out.printf(java.util.Locale.ROOT,"Rank spin paint: median %.2f ms, p95 %.2f ms (headless Java2D, 520x400)\n",times[120]/1e6,times[228]/1e6);
        if(args.length>1)warning(new File(args[1]),new File(folder,"arena-warning-preview.png"));
    }
    private static BufferedImage report(RankPreview scenario,long elapsed){
        BufferedImage image=new BufferedImage(520,400,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();
        g.setColor(new Color(48,51,54));g.fillRect(0,0,520,400);
        MatchReportView view=MatchReportView.preview(scenario);MatchReportAnimation.Frame frame=MatchReportAnimation.sample(view,elapsed,false);
        SPINS.prepare(view.badge);
        MatchReportRenderer.render(g,view,frame,new MatchReportLayout(new Rectangle(0,0,520,400),frame.entrance),0,0,0,SPINS.frame(frame.badge,frame.spin));
        g.dispose();return image;
    }
    private static void warning(File screenshot,File output)throws Exception{
        BufferedImage source=ImageIO.read(screenshot),image=new BufferedImage(508,375,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();g.setColor(RankedNhPanel.BG);g.fillRect(0,0,508,375);
        g.setColor(RankedNhPanel.TEXT);g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,12));g.drawString("Warning preview over supplied Arena screenshot",12,22);
        g.translate(0,38);g.drawImage(source,0,0,508,337,0,0,1016,674,null);
        ArenaSetupWarningOverlay.drawSlots(g,List.of(new Rectangle(70,152,32,32)));
        g.dispose();ImageIO.write(image,"png",output);
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container)layout((Container)child);}
}
