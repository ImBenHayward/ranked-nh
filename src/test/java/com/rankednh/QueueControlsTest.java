package com.rankednh;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractButton;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueueControlsTest {
    @Test public void refreshKeepsRegionChoiceAndQueueControlsStable()throws Exception{
        AtomicReference<RankedNhPanel> current=new AtomicReference<>();
        List<String> actions=new ArrayList<>();Models.Status status=status();AtomicInteger hidden=new AtomicInteger();
        SwingUtilities.invokeAndWait(()->{
            RankedNhPanel panel=new RankedNhPanel(actions(actions),"EU");current.set(panel);panel.render(status,"",false,true,"");
        });
        SwingUtilities.invokeAndWait(()->{
            RankedNhPanel panel=current.get();AbstractButton us=button(panel,"US"),queue=button(panel,"Find a match");
            ComponentAdapter detectHide=new ComponentAdapter(){@Override public void componentHidden(ComponentEvent e){hidden.incrementAndGet();}};
            us.getParent().getParent().addComponentListener(detectHide);queue.addComponentListener(detectHide);
            assertTrue(button(panel,"EU").isSelected());us.doClick();
            // A status refresh between mouse-down and mouse-up must preserve the choice and controls.
            queue.getModel().setArmed(true);queue.getModel().setPressed(true);
            for(int i=0;i<5;i++)panel.render(status,"",false,true,"");
            assertSame(us,button(panel,"US"));assertTrue(us.isSelected());assertFalse(button(panel,"EU").isSelected());
            queue.getModel().setPressed(false);queue.getModel().setArmed(false);
            assertEquals(List.of("queue US"),actions);
        });
        SwingUtilities.invokeAndWait(()->assertEquals("Refresh must not hide and restore the queue controls",0,hidden.get()));
        SwingUtilities.invokeAndWait(()->{
            RankedNhPanel panel=current.get();status.queued=true;status.queueRegion="US";panel.render(status,"",false,true,"");
        });
        SwingUtilities.invokeAndWait(()->{
            RankedNhPanel panel=current.get();AbstractButton cancel=button(panel,"Cancel search");hidden.set(0);
            cancel.addComponentListener(new ComponentAdapter(){@Override public void componentHidden(ComponentEvent e){hidden.incrementAndGet();}});
            cancel.getModel().setArmed(true);cancel.getModel().setPressed(true);
            panel.render(status,"",false,true,"");cancel.getModel().setPressed(false);cancel.getModel().setArmed(false);
            assertEquals(List.of("queue US","leave"),actions);
        });
        SwingUtilities.invokeAndWait(()->assertEquals("Refresh must not hide and restore Cancel search",0,hidden.get()));
    }

    @Test public void busyAndUnavailableRegionsCannotChangeTheSubmittedChoice()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> actions=new ArrayList<>();Models.Status status=status();RankedNhPanel panel=new RankedNhPanel(actions(actions),"US");
            panel.render(status,"",false,true,"");assertTrue(button(panel,"US").isSelected());
            panel.render(status,"",true,true,"");button(panel,"EU").doClick();button(panel,"Find a match").doClick();
            assertTrue(button(panel,"US").isSelected());assertTrue(actions.isEmpty());
            panel.render(status,"",false,true,"");button(panel,"Find a match").doClick();assertEquals(List.of("queue US"),actions);
            status.regions=List.of("DEMO");panel.render(status,"",false,true,"");
            assertNull(button(panel,"US"));assertTrue(button(panel,"DEMO").isSelected());
            button(panel,"Find a match").doClick();assertEquals(List.of("queue US","queue DEMO"),actions);
            status.regions=List.of();panel.render(status,"",false,true,"");assertFalse(button(panel,"Find a match").isEnabled());
        });
    }
    @Test public void onboardingBlocksJoiningButLeavesCancelAvailableAndUnlocksWithoutAutoQueueing()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> events=new ArrayList<>();Models.Status status=status();RankedNhPanel panel=new RankedNhPanel(actions(events),"EU");
            panel.onboarding(ArenaOnboarding.Step.HOP,true,false);panel.render(status,"",false,true,"");panel.renderOnboarding(null);
            assertFalse(button(panel,"Find a match").isEnabled());assertTrue(button(panel,"Open world switcher").isVisible());assertTrue(button(panel,"Open world switcher").isEnabled());
            button(panel,"Find a match").doClick();assertTrue(events.isEmpty());
            status.queued=true;panel.render(status,"",false,true,"");button(panel,"Cancel search").doClick();assertEquals(List.of("leave"),events);
            status.queued=false;panel.onboarding(ArenaOnboarding.Step.READY,false,false);panel.render(status,"",false,true,"");
            assertTrue(button(panel,"Find a match").isEnabled());assertFalse(button(panel,"Open world switcher").isVisible());assertEquals(List.of("leave"),events);
            button(panel,"Find a match").doClick();assertEquals(List.of("leave","queue EU"),events);
        });
    }
    @Test public void oldResultsReturnToQueueWhileReportsAndCooldownsRemainAvailable()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> events=new ArrayList<>();Models.Status s=status();
            RankedNhPanel panel=new RankedNhPanel(actions(events));
            s.match=new Models.Match();s.match.state="complete";s.match.winner="other";
            s.match.opponent=new Models.Opponent();s.match.opponent.rsn="Previous opponent";s.match.score=new int[]{0,2};
            for(int i=0;i<3;i++){
                panel.render(s,"",false,true,"");List<String> labels=new ArrayList<>();collectLabels(panel,labels);
                assertTrue(labels.contains("Ranked 1v1"));assertFalse(labels.contains("Match lost"));assertFalse(labels.contains("Previous opponent"));
                assertTrue(button(panel,"Show match report").isVisible());assertTrue(button(panel,"Find a match").isEnabled());
            }
            button(panel,"Find a match").doClick();assertEquals(List.of("queue EU"),events);
            s.cooldownUntil="2026-10-14T12:01:00Z";
            panel.render(s,"",false,true,"",null,java.time.Instant.parse("2026-10-14T12:00:00Z").toEpochMilli());
            List<String> labels=new ArrayList<>();collectLabels(panel,labels);
            assertTrue(labels.contains("Queue cooldown"));assertFalse(button(panel,"Find a match").isEnabled());
        });
    }
    @Test public void localQueueWarningFollowsTheConnectedService()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            Models.Status s=status();RankedNhPanel panel=new RankedNhPanel(actions(new ArrayList<>()));
            for(String url:List.of("http://127.0.0.1:8787","http://localhost:8787","http://[::1]:8787")){
                panel.service(url);panel.render(s,"",false,true,"");
                List<String> labels=new ArrayList<>();collectLabels(panel,labels);
                assertTrue(labels.contains("Local test"));assertTrue(labels.stream().anyMatch(t->t.contains("hosted beta cannot match")));
            }
            panel.service("https://ranked-nh.vercel.app");panel.render(s,"",false,true,"");
            List<String> labels=new ArrayList<>();collectLabels(panel,labels);
            assertFalse(labels.contains("Local test"));assertFalse(labels.stream().anyMatch(t->t.contains("hosted beta cannot match")));
        });
    }
    @Test public void setupReplayIsAvailableOnlyForAnIdleVerifiedCharacter()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> events=new ArrayList<>();Models.Status s=status();RankedNhPanel panel=new RankedNhPanel(actions(events));
            panel.onboarding(ArenaOnboarding.Step.READY,false,false);panel.render(s,"",false,true,"");
            AbstractButton redo=button(panel,"Redo kit setup");assertTrue(redo.isVisible());redo.doClick();
            assertEquals(List.of("redoKitSetup"),events);
            panel.render(s,"",true,true,"");assertFalse(redo.isEnabled());
            s.queued=true;panel.render(s,"",false,true,"");assertFalse(redo.isVisible());
            s.queued=false;s.match=new Models.Match();s.match.state="active";s.match.opponent=new Models.Opponent();s.match.opponent.rsn="Other";
            panel.render(s,"",false,true,"");assertFalse(redo.isVisible());
            s.match=null;panel.render(s,"",false,false,"");assertFalse(redo.isVisible());
            panel.onboarding(ArenaOnboarding.Step.CRATE,true,false);panel.render(s,"",false,true,"");assertFalse(redo.isVisible());
        });
    }
    private static Models.Status status(){
        Models.Status s=new Models.Status();s.mode="pilot";s.regions=List.of("EU","US");s.crossRegionAfterSeconds=30;
        s.player=new Models.Player();s.player.id="test-player";s.player.rsn="Test Player";s.player.rank=new Models.Rank();
        s.player.rank.name="Unranked";s.player.rank.badge="unranked";return s;
    }
    @Test public void betaSeasonExplainsTheResetAndUsesServerDivisionBounds()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            Models.Status status=status();status.rated=true;status.season=new Models.Season();status.season.id="beta-1";
            status.season.name="Beta season";status.season.beta=true;status.season.resetNotice="Beta ratings reset before Season 1.";
            status.player.games=5;status.player.rating=1520;status.player.rank.name="Sapphire II";status.player.rank.badge="sapphire_ii";
            status.player.rank.divisionStart=1500;status.player.rank.nextAt=1580;
            RankedNhPanel panel=new RankedNhPanel(actions(new ArrayList<>()));panel.render(status,"",false,true,"");
            List<String> labels=new ArrayList<>();collectLabels(panel,labels);
            assertTrue(labels.contains("Beta season"));assertTrue(labels.stream().anyMatch(s->s.contains("reset before Season 1")));
            assertFalse(labels.stream().anyMatch(s->s.contains("Placements paused")));
            javax.swing.JProgressBar bar=progress(panel);assertEquals(80,bar.getMaximum());assertEquals(20,bar.getValue());
        });
    }
    private static void collectLabels(Container parent,List<String> labels){
        for(Component child:parent.getComponents()){
            if(child instanceof javax.swing.JLabel && child.isVisible())labels.add(((javax.swing.JLabel)child).getText().replace('\u00a0',' '));
            if(child instanceof Container && child.isVisible())collectLabels((Container)child,labels);
        }
    }
    private static javax.swing.JProgressBar progress(Container parent){
        for(Component child:parent.getComponents()){
            if(child instanceof javax.swing.JProgressBar)return (javax.swing.JProgressBar)child;
            if(child instanceof Container){javax.swing.JProgressBar result=progress((Container)child);if(result!=null)return result;}
        }return null;
    }
    private static RankedNhPanel.Actions actions(List<String> events){
        return (RankedNhPanel.Actions)Proxy.newProxyInstance(RankedNhPanel.Actions.class.getClassLoader(),new Class<?>[]{RankedNhPanel.Actions.class},(proxy,method,args)->{
            if(method.getName().equals("queue"))events.add("queue "+args[0]);
            if(method.getName().equals("redoKitSetup"))events.add("redoKitSetup");
            if(method.getName().equals("leave"))events.add("leave");return null;
        });
    }
    private static AbstractButton button(Container parent,String text){
        for(Component child:parent.getComponents()){
            if(child instanceof AbstractButton && text.equals(((AbstractButton)child).getText()))return (AbstractButton)child;
            if(child instanceof Container){AbstractButton found=button((Container)child,text);if(found!=null)return found;}
        }return null;
    }
}
