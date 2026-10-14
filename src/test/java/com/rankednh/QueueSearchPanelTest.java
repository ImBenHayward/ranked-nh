package com.rankednh;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueueSearchPanelTest {
    private static Models.Status queued(){
        Models.Status status=new Models.Status();status.queued=true;status.queueRegion="EU";status.crossRegionAfterSeconds=30;
        status.joinedAt="1970-01-01T00:00:00Z";return status;
    }
    @Test public void showsElapsedAndRegionExpansionWithoutLosingPreferredRegion()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            QueueSearchPanel panel=new QueueSearchPanel();Models.Status status=queued();panel.update(status,12_000);
            assertTrue(labels(panel).contains("00:12"));assertTrue(labels(panel).contains("EU preferred"));
            panel.update(status,45_000);
            assertTrue(labels(panel).contains("00:45"));assertTrue(labels(panel).contains("Searching all regions"));
            assertTrue(tooltips(panel).contains("EU remains preferred."));
            status.joinedAt=null;panel.update(status,50_000);assertFalse(labels(panel).contains("00:00"));
            assertFalse(labels(panel).contains("Time in queue"));panel.dispose();
        });
    }
    @Test public void animationStopsWhenHiddenCancelledRemovedOrDisposed()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            QueueSearchPanel panel=new QueueSearchPanel();JPanel host=new JPanel();host.add(panel);
            try{
                panel.update(queued(),0);assertFalse(panel.animating());
                host.addNotify();assertTrue(panel.isShowing());assertTrue(panel.animating());
                panel.setVisible(false);assertFalse(panel.animating());
                panel.setVisible(true);assertTrue(panel.animating());
                panel.update(null,0);assertFalse(panel.animating());assertFalse(panel.isVisible());
                panel.update(queued(),0);assertTrue(panel.animating());
                host.removeNotify();assertFalse(panel.animating());
                host.addNotify();assertTrue(panel.animating());
                panel.dispose();assertFalse(panel.animating());
                panel.update(queued(),0);assertFalse(panel.animating());
            }finally{panel.dispose();host.removeNotify();}
        });
    }
    private static List<String> tooltips(Container root){
        List<String> text=new ArrayList<>();
        for(Component child:root.getComponents())if(child.isVisible()){
            if(child instanceof JLabel)text.add(((JLabel)child).getToolTipText());
            if(child instanceof Container)text.addAll(tooltips((Container)child));
        }return text;
    }
    private static List<String> labels(Container root){
        List<String> text=new ArrayList<>();
        for(Component child:root.getComponents())if(child.isVisible()){
            if(child instanceof JLabel)text.add(((JLabel)child).getText());
            if(child instanceof Container)text.addAll(labels((Container)child));
        }return text;
    }
}
