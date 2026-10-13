package com.rankednh;

import java.awt.Component;
import java.awt.Container;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatchHistoryTest {
    static Models.Match match(String state,String winner){
        Models.Match match=new Models.Match();match.state=state;match.winner=winner;match.score=new int[]{2,0};
        match.createdAt="2026-10-12T23:50:00Z";match.endedAt="2026-10-13T00:12:00Z";
        match.opponent=new Models.Opponent();match.opponent.rsn="Other Main";
        match.opponent.rank=new Models.Rank();match.opponent.rank.name="Ruby II";match.opponent.rank.badge="ruby_ii";
        return match;
    }
    @Test public void resultsUseYourPerspectiveAndDatesUseLocalTime(){
        assertEquals("Won",MatchHistoryPanel.outcome(match("complete","me"),"me"));
        assertEquals("Lost",MatchHistoryPanel.outcome(match("complete","them"),"me"));
        assertEquals("Cancelled",MatchHistoryPanel.outcome(match("void",null),"me"));
        assertEquals("Under review",MatchHistoryPanel.outcome(match("review",null),"me"));
        Models.Match match=match("complete","me");
        assertEquals("13 Oct · 01:12",MatchHistoryPanel.date(match,ZoneId.of("Europe/London")));
        assertEquals("12 Oct · 17:12",MatchHistoryPanel.date(match,ZoneId.of("America/Los_Angeles")));
        match.endedAt=null;assertEquals("13 Oct · 00:50",MatchHistoryPanel.date(match,ZoneId.of("Europe/London")));
        match.createdAt=null;assertEquals("",MatchHistoryPanel.date(match,ZoneId.of("Europe/London")));
    }
    @Test public void closingDuringLoadingRejectsOldSuccessAndFailureEvenAfterReopening()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            AtomicInteger closed=new AtomicInteger();MatchHistoryPanel history=new MatchHistoryPanel(closed::incrementAndGet,()->{});
            history.open();long first=history.request();button(history,"Close").doClick();assertFalse(history.isVisible());assertEquals(1,closed.get());
            history.show(first,"me",List.of(match("complete","me")));assertFalse(history.isVisible());
            history.open();history.show(first,"me",List.of(match("complete","me")));history.failed(first);
            assertTrue(hasLabel(history,"Loading matches…"));
            history.show(history.request(),"me",List.of(match("complete","me")));assertTrue(hasLabel(history,"Won · Unrated"));
        });
    }
    @Test public void emptyErrorRetryAndLongHistoryKeepACloseControl()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            AtomicInteger retried=new AtomicInteger();MatchHistoryPanel history=new MatchHistoryPanel(()->{},retried::incrementAndGet);
            history.open();history.show(history.request(),"me",List.of());assertTrue(hasLabel(history,"No matches yet."));
            history.failed(history.request());assertTrue(hasLabel(history,"Couldn't load matches."));
            long failed=history.request();button(history,"Try again").doClick();assertEquals(1,retried.get());assertTrue(history.request()>failed);
            history.show(history.request(),"me",java.util.Collections.nCopies(20,match("complete","me")));
            JScrollPane scroll=find(history,JScrollPane.class);assertNotNull(scroll);assertTrue(scroll.getPreferredSize().height<=260);
            assertNotNull(button(history,"Close"));
        });
    }
    private static JButton button(Container c,String text){
        for(Component child:c.getComponents()){
            if(child instanceof JButton && text.equals(((JButton)child).getText()))return (JButton)child;
            if(child instanceof Container){JButton found=button((Container)child,text);if(found!=null)return found;}
        }return null;
    }
    private static boolean hasLabel(Container c,String text){
        for(Component child:c.getComponents()){
            if(child instanceof JLabel && text.equals(((JLabel)child).getText()))return true;
            if(child instanceof Container && hasLabel((Container)child,text))return true;
        }return false;
    }
    private static <T> T find(Container c,Class<T> type){
        for(Component child:c.getComponents()){
            if(type.isInstance(child))return type.cast(child);
            if(child instanceof Container){T found=find((Container)child,type);if(found!=null)return found;}
        }return null;
    }
}
