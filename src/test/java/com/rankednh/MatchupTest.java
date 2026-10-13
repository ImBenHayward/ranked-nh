package com.rankednh;

import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldType;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatchupTest
{
    static Models.Status status(){
        Models.Status s=new Models.Status();s.mode="pilot";s.player=new Models.Player();s.player.id="me";s.player.rsn="My Main";
        s.match=new Models.Match();s.match.id="one";s.match.state="offered";s.match.region="EU";s.match.world=558;s.match.round=1;s.match.score=new int[]{0,0};
        s.match.deadline=Instant.ofEpochMilli(60_000).toString();s.match.opponent=new Models.Opponent();s.match.opponent.rsn="Other Main";return s;
    }
    @Test public void eachOfferNotifiesOnceDespitePollingAndWorldHops(){
        Models.Status s=status();MatchFoundAlert alert=new MatchFoundAlert();
        assertFalse(alert.take(s,false,0));assertTrue(alert.take(s,true,0));assertFalse(alert.take(s,true,3_000));
        assertFalse(alert.take(s,false,5_000));assertFalse(alert.take(s,true,6_000));
        s.match.id="two";assertTrue(alert.take(s,true,8_000));
        s.match.id="three";s.match.accepted=true;assertFalse(alert.take(s,true,10_000));
        s.match.accepted=false;assertFalse(alert.take(s,true,60_000));
    }
    @Test public void offerUsesItsActualDeadlineAndShowsRegionBeforeAcceptance(){
        Models.Status s=status();MatchupView v=MatchupView.from(s,true,false,301,false,false,false,false,57_000);
        assertEquals("3 seconds to accept",v.message);assertTrue(v.destination.contains("EU · World 558"));assertTrue(v.enabled);
        v=MatchupView.from(s,true,false,301,false,false,false,false,60_000);assertFalse(v.enabled);assertFalse(v.secondaryEnabled);
    }
    @Test public void handoffMovesFromHopToReadyToCompactFightWithoutAutomatingChallenge(){
        Models.Status s=status();s.match.state="preparing";
        MatchupView v=MatchupView.from(s,true,false,301,false,false,false,false,0);assertEquals("hop",v.action);assertTrue(v.enabled);
        v=MatchupView.from(s,true,false,301,false,false,true,false,0);assertFalse(v.enabled);
        v=MatchupView.from(s,true,false,558,true,true,false,false,0);assertEquals("panel",v.action);assertEquals("Opponent found. Checking you in…",v.message);assertFalse(v.enabled);
        s.match.ready=true;assertFalse(MatchupView.from(s,true,false,558,true,true,false,false,0).enabled);
        s.match.state="active";v=MatchupView.from(s,true,false,558,true,true,false,true,0);assertTrue(v.compact);assertEquals("panel",v.action);
    }
    @Test public void demoCannotHopAndClosedOrWrongAccountMatchesHaveNoPrompt(){
        Models.Status s=status();s.mode="demo";s.match.state="preparing";
        assertFalse(MatchupView.from(s,true,false,301,false,false,false,false,0).enabled);
        assertFalse(ArenaWorldHopper.eligible(s));
        assertNull(MatchupView.from(s,false,false,301,false,false,false,false,0));
        s.match.state="complete";assertNull(MatchupView.from(s,true,false,301,false,false,false,false,0));
    }
    private World world(int id,int location,int players,WorldType... types){
        EnumSet<WorldType> set=EnumSet.noneOf(WorldType.class);for(WorldType type:types)set.add(type);
        return World.builder().id(id).location(location).players(players).types(set).address("oldschool258a.runescape.com").activity("PvP Arena").build();
    }
    @Test public void hopChecksLiveArenaTypeRegionPopulationAndExactWorld(){
        assertNull(ArenaWorldHopper.validate(world(558,1,45,WorldType.PVP_ARENA),558,"EU"));
        assertNull(ArenaWorldHopper.validate(world(578,0,45,WorldType.PVP_ARENA),578,"US"));
        assertNotNull(ArenaWorldHopper.validate(null,558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(301,1,45,WorldType.MEMBERS),558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(558,1,45,WorldType.PVP),558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(558,0,45,WorldType.PVP_ARENA),558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(558,1,-1,WorldType.PVP_ARENA),558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(558,1,2000,WorldType.PVP_ARENA),558,"EU"));
        assertNotNull(ArenaWorldHopper.validate(world(558,1,10,WorldType.PVP_ARENA,WorldType.HIGH_RISK),558,"EU"));
    }
    @Test public void highlightsAreScopedToTheAssignedArenaAndDisappearWhenResolved(){
        Models.Status s=status();assertFalse(OpponentLocator.eligible(s,true,558,true));
        s.match.state="preparing";assertTrue(OpponentLocator.eligible(s,true,558,true));
        assertFalse(OpponentLocator.eligible(s,true,578,true));assertFalse(OpponentLocator.eligible(s,true,558,false));assertFalse(OpponentLocator.eligible(s,false,558,true));
        for(String state:new String[]{"complete","void","review"}){s.match.state=state;assertFalse(OpponentLocator.eligible(s,true,558,true));}
    }
    private RankedNhPanel panel(List<String> calls){
        return new RankedNhPanel(new RankedNhPanel.Actions(){
            public void connect(char[] k){fail();}public void queue(String r){fail();}public void leave(){fail();}public void accept(boolean b){fail();}public void hop(){fail();}
            public void ready(){fail();}public void forfeit(){fail();}public void review(){fail();}public void leaderboard(){}public void history(){calls.add("history");}public void showReport(){}
            public void handoffAction(String key,String action){calls.add(key+"/"+action);}
            public void previewRank(RankPreview scenario){calls.add("preview/"+scenario.name());}
        });
    }
    private JButton button(Container container,String text){
        for(Component c:container.getComponents()){
            if(c instanceof JButton && text.equals(((JButton)c).getText()))return (JButton)c;
            if(c instanceof Container){JButton b=button((Container)c,text);if(b!=null)return b;}
        }
        return null;
    }
    @Test public void sidebarPreviewWorksWithoutAnAccountAndOnlyDispatchesSampleActions()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> calls=new ArrayList<>();RankedNhPanel panel=panel(calls);
            panel.render(null,"",false,false,"",MatchupView.preview(0));
            JButton accept=button(panel,"Accept match");assertTrue(accept.isVisible());assertTrue(accept.isEnabled());accept.doClick();
            panel.render(null,"",false,false,"",MatchupView.preview(1));button(panel,"Hop to World 558").doClick();
            button(panel,"Exit preview").doClick();
            assertEquals(List.of("preview:0/preview","preview:1/preview","preview:1/close-preview"),calls);
        });
    }
    @Test public void sidebarDisablesExpiredOffersAndSendsTheDisplayedMatchKey()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> calls=new ArrayList<>();RankedNhPanel panel=panel(calls);Models.Status s=status();
            s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
            panel.render(s,"",false,true,"",MatchupView.from(s,true,false,301,false,false,false,false,60_000));
            assertFalse(button(panel,"Accept match").isEnabled());button(panel,"Accept match").doClick();assertTrue(calls.isEmpty());
            panel.render(s,"",false,true,"",MatchupView.from(s,true,false,301,false,false,false,false,0));
            button(panel,"Accept match").doClick();assertEquals(List.of("one:offered:1/accept"),calls);
        });
    }
    @Test public void rankPreviewIsExplicitAndDiagnosticsStayOutOfTheMainFlow()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> calls=new ArrayList<>();RankedNhPanel p=panel(calls);
            button(p,"Preview ranks").doClick();button(p,"Play preview").doClick();assertEquals(List.of("preview/RANK_UP"),calls);
            ArenaSetup.View setup=TestArenaKit.SETUP.describe(true,2,0,true,false,ArenaSetupTest.kit(0,1,55),null,null);
            p.renderSetup(setup,"Snapshot saved",false);assertFalse(button(p,"Save kit snapshot").isVisible());
            p.renderSetup(setup,"Snapshot saved",false,true);assertTrue(button(p,"Save kit snapshot").isVisible());
            Models.Status s=status();s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
            s.match.state="active";p.render(s,"",false,true,"",MatchupView.from(s,true,false,558,true,true,false,false,0));
            assertFalse(button(p,"Preview ranks").isEnabled());
        });
    }
    @Test public void historyToggleAndCloseWorkDuringLoadingAndPollingDoesNotReopenIt()throws Exception{
        SwingUtilities.invokeAndWait(()->{
            List<String> calls=new ArrayList<>();RankedNhPanel panel=panel(calls);Models.Status s=status();
            s.player.rank=new Models.Rank();s.player.rank.name="Unranked";s.player.rank.badge="unranked";
            panel.render(s,"",false,true,"",null);
            button(panel,"Match history").doClick();long first=panel.historyRequest();assertEquals(List.of("history"),calls);
            button(panel,"Hide match history").doClick();panel.showHistory(first,"me",List.of(MatchHistoryTest.match("complete","me")));
            panel.render(s,"",false,true,"",null);assertNotNull(button(panel,"Match history"));
            button(panel,"Match history").doClick();button(panel,"Close").doClick();assertNotNull(button(panel,"Match history"));
            button(panel,"Match history").doClick();long previousAccount=panel.historyRequest();
            s.player.id="another";panel.render(s,"",false,true,"",null);panel.showHistory(previousAccount,"me",List.of(MatchHistoryTest.match("complete","me")));
            assertNotNull(button(panel,"Match history"));assertNull(button(panel,"Hide match history"));
        });
    }

}
