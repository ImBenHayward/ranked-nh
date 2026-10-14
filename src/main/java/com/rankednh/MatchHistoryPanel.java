package com.rankednh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import net.runelite.client.util.ImageUtil;

/** A bounded history drawer. Delayed responses cannot reopen a dismissed drawer. */
final class MatchHistoryPanel extends JPanel {
    private final JPanel content=new JPanel();
    private final Runnable retry;
    private long request;
    MatchHistoryPanel(Runnable closed,Runnable retry){
        this.retry=retry;setLayout(new BorderLayout(0,8));setBackground(RankedNhPanel.BG);
        setAlignmentX(LEFT_ALIGNMENT);setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);
        heading.add(label("Recent matches",11,RankedNhPanel.MUTED),BorderLayout.WEST);
        JButton close=control("Close",()->{close();closed.run();});
        close.getAccessibleContext().setAccessibleName("Close match history");heading.add(close,BorderLayout.EAST);add(heading,BorderLayout.NORTH);
        content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));content.setOpaque(false);add(content,BorderLayout.CENTER);
        setVisible(false);
    }
    @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
    long request(){return request;}
    void open(){request++;setVisible(true);message("Loading matches…",false);}
    void close(){request++;setVisible(false);content.removeAll();revalidate();repaint();}
    void failed(long id){if(id==request && isVisible())message("Couldn't load matches.",true);}
    void show(long id,String playerId,List<Models.Match> matches){
        if(id!=request || !isVisible())return;
        if(matches.isEmpty()){message("No matches yet.",false);return;}
        content.removeAll();
        JPanel rows=new JPanel();rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));rows.setBackground(RankedNhPanel.SURFACE);
        for(Models.Match match:matches)rows.add(row(match,playerId));
        JScrollPane scroll=new JScrollPane(rows,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(RankedNhPanel.LINE));scroll.getViewport().setBackground(RankedNhPanel.SURFACE);
        scroll.setPreferredSize(new Dimension(1,Math.min(4,matches.size())*64+2));scroll.getVerticalScrollBar().setUnitIncrement(32);
        scroll.getAccessibleContext().setAccessibleName("Recent matches, newest first");content.add(scroll);revalidate();repaint();
    }
    private void message(String text,boolean error){
        content.removeAll();content.add(label(text,11,error?new Color(231,156,147):RankedNhPanel.MUTED));
        if(error){content.add(Box.createVerticalStrut(8));content.add(control("Try again",()->{open();retry.run();}));}
        revalidate();repaint();
    }
    private JPanel row(Models.Match match,String playerId){
        JPanel row=new JPanel(new BorderLayout(0,6));row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,RankedNhPanel.LINE),BorderFactory.createEmptyBorder(8,8,8,8)));
        row.setPreferredSize(new Dimension(1,64));row.setMaximumSize(new Dimension(Integer.MAX_VALUE,64));
        Color resultColor="Won".equals(outcome(match,playerId))?RankedNhPanel.SUCCESS:"Lost".equals(outcome(match,playerId))?new Color(231,156,147):RankedNhPanel.MUTED;
        JPanel top=new JPanel(new BorderLayout(4,0));top.setOpaque(false);
        JLabel name=label(match.opponent==null?"Opponent":match.opponent.rsn,12,RankedNhPanel.TEXT);
        if(match.opponent!=null && match.opponent.rank!=null){
            name.setIcon(new ImageIcon(ImageUtil.resizeImage(BadgeImages.image(match.opponent.rank.badge),18,18)));name.setIconTextGap(4);
            name.setToolTipText("Current rank: "+match.opponent.rank.name);
        }
        top.add(name,BorderLayout.CENTER);
        String score="void".equals(match.state) || match.score==null || match.score.length<2?"—":match.score[0]+"–"+match.score[1];
        JLabel scoreLabel=label(score,13,resultColor);scoreLabel.getAccessibleContext().setAccessibleName("Your score first: "+score);
        top.add(scoreLabel,BorderLayout.EAST);row.add(top,BorderLayout.NORTH);
        JPanel bottom=new JPanel(new BorderLayout(4,0));bottom.setOpaque(false);
        String result=outcome(match,playerId);
        if("complete".equals(match.state))result+=!match.rated?" · Unrated":match.ratingChange==null?"":" · "+(match.ratingChange>=0?"+":"")+match.ratingChange;
        bottom.add(label(result,10,resultColor),BorderLayout.WEST);
        bottom.add(label(date(match,ZoneId.systemDefault()),10,RankedNhPanel.MUTED),BorderLayout.EAST);
        row.add(bottom,BorderLayout.SOUTH);
        if(match.reason!=null)row.setToolTipText(match.reason);
        return row;
    }
    static String outcome(Models.Match match,String playerId){
        if("complete".equals(match.state))return match.winner==null || playerId==null?"Complete":playerId.equals(match.winner)?"Won":"Lost";
        if("void".equals(match.state))return "Cancelled";
        if("review".equals(match.state))return "Under review";
        return "In progress";
    }
    static String date(Models.Match match,ZoneId zone){
        try{return Instant.parse(match.endedAt==null?match.createdAt:match.endedAt).atZone(zone).format(DateTimeFormatter.ofPattern("d MMM · HH:mm",Locale.UK));}
        catch(RuntimeException invalid){return "";}
    }
    private static JLabel label(String text,int size,Color color){
        JLabel label=new JLabel(text);label.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,size));label.setForeground(color);label.setAlignmentX(LEFT_ALIGNMENT);return label;
    }
    private static JButton control(String text,Runnable action){
        JButton button=new JButton(text);button.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));button.setFocusPainted(false);
        button.setAlignmentX(LEFT_ALIGNMENT);button.addActionListener(e->action.run());
        Runnable style=()->{
            button.setBackground(button.getModel().isPressed()?RankedNhPanel.BG:button.getModel().isRollover()?RankedNhPanel.LINE:RankedNhPanel.SURFACE);
            button.setForeground(RankedNhPanel.TEXT);button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(button.hasFocus()?RankedNhPanel.ACCENT:RankedNhPanel.LINE),BorderFactory.createEmptyBorder(4,8,4,8)));
        };
        button.getModel().addChangeListener(e->style.run());button.addFocusListener(new java.awt.event.FocusAdapter(){
            @Override public void focusGained(java.awt.event.FocusEvent e){style.run();}
            @Override public void focusLost(java.awt.event.FocusEvent e){style.run();}
        });style.run();return button;
    }
}
