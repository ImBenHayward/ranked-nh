package com.rankednh;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/** Persistent, single-click choices that remain usable while status updates arrive. */
final class RegionPicker extends JPanel {
    private List<String> available=List.of();
    private ButtonGroup group=new ButtonGroup();
    private String preferred;
    RegionPicker(String preferred){
        this.preferred=preferred;setLayout(new GridLayout(1,0,6,0));setOpaque(false);setAlignmentX(LEFT_ALIGNMENT);
        setPreferredSize(new Dimension(1,34));setMaximumSize(new Dimension(Integer.MAX_VALUE,34));
        getAccessibleContext().setAccessibleName("Preferred competition region");
    }
    String selected(){return group.getSelection()==null?null:group.getSelection().getActionCommand();}
    void update(List<String> regions,boolean enabled){
        if(!available.equals(regions)){
            available=List.copyOf(regions);removeAll();group=new ButtonGroup();
            String chosen=regions.contains(preferred)?preferred:regions.isEmpty()?null:regions.get(0);
            for(String region:regions){
                JToggleButton button=new JToggleButton(region,new RegionFlag(region));
                button.setActionCommand(region);button.setIconTextGap(6);button.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));
                button.setContentAreaFilled(false);button.setOpaque(true);button.setFocusPainted(false);
                button.setToolTipText(MatchFoundAlert.region(region));
                button.getAccessibleContext().setAccessibleName(MatchFoundAlert.region(region)+" preferred region");
                Runnable style=()->style(button);
                button.getModel().addChangeListener(e->style.run());
                button.addFocusListener(new FocusAdapter(){
                    @Override public void focusGained(FocusEvent e){style.run();}
                    @Override public void focusLost(FocusEvent e){style.run();}
                });
                button.addActionListener(e->preferred=region);
                group.add(button);add(button);button.setSelected(region.equals(chosen));style.run();
            }
            revalidate();repaint();
        }
        for(java.awt.Component button:getComponents())button.setEnabled(enabled);
    }
    private static void style(JToggleButton button){
        boolean selected=button.isSelected(),enabled=button.isEnabled();
        Color fill=selected?new Color(49,69,58):RankedNhPanel.SURFACE;
        if(enabled && button.getModel().isRollover())fill=selected?new Color(61,84,71):new Color(44,50,53);
        if(enabled && button.getModel().isPressed())fill=RankedNhPanel.BG;
        button.setBackground(fill);button.setForeground(enabled?(selected?RankedNhPanel.ACCENT:RankedNhPanel.TEXT):RankedNhPanel.MUTED);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(
            button.hasFocus()?RankedNhPanel.ACCENT:selected?new Color(65,89,74):RankedNhPanel.LINE),BorderFactory.createEmptyBorder(6,8,6,8)));
    }
}
