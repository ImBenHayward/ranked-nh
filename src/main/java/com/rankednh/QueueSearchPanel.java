package com.rankednh;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Search feedback only. Queue requests and cancellation remain owned by the plugin. */
final class QueueSearchPanel extends JPanel {
    private final QueueSearchClock clock=new QueueSearchClock();
    private final JLabel elapsed=label("",20),scope=label("",11);
    private final JLabel spinner=new JLabel(new Spinner());
    private final Timer animation=new Timer(40,e->{updateText(System.nanoTime());spinner.repaint();});
    private String region;
    private int expandAfter;
    private boolean queued,disposed;
    QueueSearchPanel(){
        setOpaque(false);setAlignmentX(LEFT_ALIGNMENT);setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        JLabel title=label("Searching",14);add(title);add(Box.createVerticalStrut(16));
        JPanel time=new JPanel(new FlowLayout(FlowLayout.CENTER,8,0));time.setOpaque(false);
        time.setAlignmentX(CENTER_ALIGNMENT);time.add(spinner);time.add(elapsed);
        elapsed.setFont(new Font(Font.MONOSPACED,Font.PLAIN,20));
        time.setMaximumSize(new Dimension(Integer.MAX_VALUE,28));add(time);
        add(Box.createVerticalStrut(16));scope.setIconTextGap(6);add(scope);
        addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0)syncAnimation();});
        setVisible(false);
    }
    private static JLabel label(String text,int size){
        JLabel label=new JLabel(text);label.setAlignmentX(CENTER_ALIGNMENT);
        label.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,size));label.setForeground(RankedNhPanel.TEXT);return label;
    }
    void update(Models.Status status,long serverNow){
        queued=status!=null && status.queued;
        if(queued){
            region=status.queueRegion;expandAfter=Math.max(0,status.crossRegionAfterSeconds);
            clock.update(status.player==null?null:status.player.id,status.joinedAt,serverNow,System.nanoTime());
            scope.setToolTipText("DEMO".equals(region)?"Local simulation only":(region==null?"Your region":region)+" remains preferred.");
            updateText(System.nanoTime());
        }else clock.clear();
        setVisible(queued);syncAnimation();
    }
    private void updateText(long nowNanos){
        Long seconds=clock.seconds(nowNanos);
        elapsed.setVisible(seconds!=null);
        String value=seconds==null?"":QueueSearchClock.format(seconds);
        if(!value.equals(elapsed.getText())){
            elapsed.setText(value);elapsed.getAccessibleContext().setAccessibleName(seconds==null?"Search time unavailable":"Time in queue: "+seconds+" seconds");
        }
        boolean allRegions=!"DEMO".equals(region) && seconds!=null && seconds>=expandAfter;
        String message="DEMO".equals(region)?"Local simulation":allRegions?"Searching all regions":(region==null?"Your region":region)+" preferred";
        if(!message.equals(scope.getText())){scope.setText(message);scope.setIcon(new RegionFlag(allRegions?null:region));}
    }
    private void syncAnimation(){if(queued && !disposed && isShowing())animation.start();else animation.stop();}
    @Override public void removeNotify(){animation.stop();super.removeNotify();}
    void dispose(){disposed=true;animation.stop();clock.clear();}
    boolean animating(){return animation.isRunning();}
    private static final class Spinner implements Icon {
        public int getIconWidth(){return 20;}public int getIconHeight(){return 20;}
        public void paintIcon(Component component,Graphics graphics,int x,int y){
            Graphics2D g=(Graphics2D)graphics.create();
            try{
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                g.setColor(RankedNhPanel.LINE);g.drawOval(x+3,y+3,14,14);
                int angle=(int)(System.nanoTime()/1_000_000%1200)*360/1200;
                g.setColor(RankedNhPanel.ACCENT);g.drawArc(x+3,y+3,14,14,90-angle,-100);
            }finally{g.dispose();}
        }
    }
}
