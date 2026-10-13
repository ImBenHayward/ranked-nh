package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class MatchReportTest
{
    private Models.Status result() {
        Models.Status s=new Models.Status();s.mode="pilot";s.player=new Models.Player();s.player.id="me";
        s.player.games=20;s.player.rating=1902.8;s.player.rank=new Models.Rank();
        s.player.rank.name="Dragonstone III";s.player.rank.badge="dragonstone_iii";s.player.rank.nextAt=1920;
        s.player.rank.nextRank="Dragonstone II";s.player.rank.pointsToNext=18;s.player.rank.progress=0.713;
        s.player.rank.divisionStart=1860;
        s.match=new Models.Match();s.match.id="match-1";s.match.state="complete";s.match.winner="me";s.match.rated=true;
        s.match.score=new int[]{2,1};s.match.opponent=new Models.Opponent();s.match.opponent.rsn="Opponent";
        s.match.ratingBefore=1864;s.match.ratingAfter=1902;s.match.ratingChange=38;return s;
    }
    @Test public void ratedReportUsesServerResultAndNextDivision(){
        MatchReportView v=MatchReportView.from(result());assertEquals("Victory",v.title);assertEquals("2 – 1",v.score);
        assertEquals("1864 → 1902 rating",v.rating);assertEquals("+38",v.delta);assertEquals("18 rating to Dragonstone II",v.progressLabel);
    }
    @Test public void firstRankNeedsAFifthRatedConfirmedResultAndAlsoCelebratesALoss(){
        Models.Status s=result();s.player.games=5;
        assertTrue(MatchReportView.from(s).placementReveal);
        s.match.winner="other";assertTrue(MatchReportView.from(s).placementReveal);
        s.match.rated=false;assertFalse(MatchReportView.from(s).placementReveal);
        s.match.rated=true;s.match.state="review";assertFalse(MatchReportView.from(s).placementReveal);
        s.match.state="void";assertFalse(MatchReportView.from(s).placementReveal);
        s.match.state="complete";s.match.ratingAfter=null;assertFalse(MatchReportView.from(s).placementReveal);
        s.match.ratingAfter=1902;s.match.ratingChange=null;assertFalse(MatchReportView.from(s).placementReveal);
        s.match.ratingChange=38;s.player.rank.badge="unranked";assertFalse(MatchReportView.from(s).placementReveal);
        s.player.rank.badge="dragonstone_iii";s.player.games=4;assertFalse(MatchReportView.from(s).placementReveal);
        s.player.games=6;assertFalse(MatchReportView.from(s).placementReveal);
    }
    @Test public void defeatKeepsTheNegativeRatingChange(){
        Models.Status s=result();s.match.winner="other";s.match.ratingChange=-27;s.match.ratingBefore=1929;
        MatchReportView v=MatchReportView.from(s);assertEquals("Defeat",v.title);assertEquals("-27",v.delta);
    }
    @Test public void unrankedAndUnratedNeverInventRatingGains(){
        Models.Status s=result();s.match.rated=false;
        assertEquals("",MatchReportView.from(s).delta);assertEquals("Unrated match",MatchReportView.from(s).rating);
        s.match.rated=true;s.player.games=4;s.player.rank.name="Unranked";
        MatchReportView v=MatchReportView.from(s);assertEquals("",v.delta);assertEquals("4 / 5 placements",v.progressLabel);
        assertEquals("1 rated match to reveal your rank",v.progressDetail);
    }
    @Test public void pendingAndVoidedOutcomesDoNotClaimVictory(){
        Models.Status s=result();s.match.state="review";
        assertEquals("Result pending",MatchReportView.from(s).title);assertEquals("",MatchReportView.from(s).delta);
        s.match.state="void";assertEquals("Match voided",MatchReportView.from(s).title);
        s.match.state="active";assertNull(MatchReportView.from(s));
    }
    @Test public void highestRankHasNoInventedNextBracket(){
        Models.Status s=result();s.player.rank.nextAt=null;s.player.rank.nextRank=null;
        MatchReportView v=MatchReportView.from(s);assertEquals("Highest division reached",v.progressLabel);assertEquals(1,v.progress,0);
    }
    private Models.Rank rank(String name,String badge,int start,Integer next,String nextRank){
        Models.Rank rank=new Models.Rank();rank.name=name;rank.badge=badge;rank.divisionStart=start;
        rank.nextAt=next;rank.nextRank=nextRank;return rank;
    }
    @Test public void realPromotionUsesTheMatchSnapshotsAndIncludesTheHighestDivision(){
        Models.Status s=result();s.match.ratingBefore=1799;s.match.ratingAfter=1826;s.match.ratingChange=27;
        s.match.rankBefore=rank("Diamond II","diamond_ii",1740,1800,"Diamond I");
        s.match.rankAfter=rank("Diamond I","diamond_i",1800,1860,"Dragonstone III");
        MatchReportView view=MatchReportView.from(s);assertTrue(view.promotion());
        assertEquals("diamond_ii",view.badgeBefore);assertEquals("diamond_i",view.badge);assertEquals(26/60.0,view.progress,.0001);
        // The current player rank is deliberately different; the awarded result wins.
        s.match.rankBefore=rank("Zenyte II","zenyte_ii",2280,2340,"Zenyte I");
        s.match.rankAfter=rank("Zenyte I","zenyte_i",2340,null,null);
        s.match.ratingBefore=2339;s.match.ratingAfter=2366;
        view=MatchReportView.from(s);assertTrue(view.promotion());
        assertEquals("Highest division reached",view.progressLabel);
        assertEquals(1,MatchReportAnimation.sample(view,5000,false).progress,0);
        s.match.rated=false;assertFalse(MatchReportView.from(s).promotion());
        s.match.rated=true;s.match.state="review";assertFalse(MatchReportView.from(s).promotion());
    }
    @Test public void storedFirstRankStaysAPlacementRevealAndDemotionsDoNotCelebrate(){
        Models.Status s=result();s.match.rankBefore=rank("Unranked","unranked",0,null,null);
        s.match.rankBefore.divisionStart=null;s.match.rankAfter=s.player.rank;
        assertTrue(MatchReportView.from(s).placementReveal);assertFalse(MatchReportView.from(s).promotion());
        s.match.rankBefore=rank("Dragonstone II","dragonstone_ii",1920,1980,"Dragonstone I");
        s.match.ratingBefore=1921;s.match.ratingAfter=1902;s.match.ratingChange=-19;
        assertFalse(MatchReportView.from(s).placementReveal);assertFalse(MatchReportView.from(s).promotion());
        s.match.rankBefore=null;s.match.rankAfter=null;
        assertFalse(MatchReportView.from(s).promotion());
    }
    @Test public void pollingDoesNotResetDismissalOrExpiry(){
        Models.Status s=result();MatchReportVisibility visibility=new MatchReportVisibility();
        assertTrue(visibility.visible(s,0,60));assertTrue(visibility.visible(s,59_000,60));assertFalse(visibility.visible(s,60_000,60));
        visibility.reopen();assertTrue(visibility.visible(s,61_000,60));visibility.dismiss();assertFalse(visibility.visible(s,62_000,60));
        s.match.id="match-2";assertTrue(visibility.visible(s,63_000,60));
    }
    @Test public void resolvedReviewShowsNewResultAndQueueHidesOldReport(){
        Models.Status s=result();s.match.state="review";MatchReportVisibility visibility=new MatchReportVisibility();
        assertFalse(visibility.visible(s,0,0));visibility.reopen();assertTrue(visibility.visible(s,0,0));visibility.dismiss();s.match.state="complete";assertTrue(visibility.visible(s,500,0));
        s.queued=true;assertFalse(visibility.visible(s,600,0));
    }
    @Test public void previewIsExplicitAndNeverConsumesARealResult(){
        MatchReportView v=MatchReportView.preview();assertTrue(v.preview);assertTrue(v.heading.contains("Preview"));
        assertTrue(v.note.contains("unchanged"));
    }
    @Test public void countAndBarStartAtPreviousRatingAndFinishAtConfirmedRating(){
        MatchReportView v=MatchReportView.from(result());
        MatchReportAnimation.Frame start=MatchReportAnimation.sample(v,0,false);
        assertEquals(Integer.valueOf(1864),start.rating);assertEquals(4/60.0,start.progress,0.00001);assertEquals(0,start.entrance,0);
        MatchReportAnimation.Frame middle=MatchReportAnimation.sample(v,900,false);
        assertTrue(middle.rating>1864 && middle.rating<1902);assertTrue(middle.progress>start.progress && middle.progress<0.7);
        MatchReportAnimation.Frame end=MatchReportAnimation.sample(v,1800,false);
        assertEquals(Integer.valueOf(1902),end.rating);assertEquals(0.7,end.progress,0.00001);
        assertEquals("18 rating to Dragonstone II",end.progressLabel);
    }
    @Test public void lossCountsDownAndClampsProgressToTheConfirmedDivision(){
        Models.Status s=result();s.match.ratingBefore=1929;s.match.ratingChange=-27;s.match.winner="other";
        MatchReportView v=MatchReportView.from(s);
        assertEquals(1,MatchReportAnimation.sample(v,0,false).progress,0);
        MatchReportAnimation.Frame middle=MatchReportAnimation.sample(v,900,false);
        assertTrue(middle.rating<1929 && middle.rating>1902);
        assertEquals(0.7,MatchReportAnimation.sample(v,1800,false).progress,0.00001);
        s.match.ratingBefore=1800;s.match.ratingChange=102;v=MatchReportView.from(s);
        assertEquals(0,MatchReportAnimation.sample(v,0,false).progress,0);
        assertEquals(0.7,MatchReportAnimation.sample(v,1800,false).progress,0.00001);
    }
    @Test public void animationSurvivesPollingAndReplaysOnlyForANewResultOrReopen(){
        MatchReportAnimation animation=new MatchReportAnimation();MatchReportView v=MatchReportView.preview();
        animation.frame("one",v,100,false);
        assertEquals(Integer.valueOf(1902),animation.frame("one",MatchReportView.preview(),2000,false).rating);
        assertEquals(Integer.valueOf(1902),animation.frame("one",v,6000,false).rating);
        assertEquals(Integer.valueOf(1864),animation.frame("two",v,6000,false).rating);
        animation.reset();assertEquals(Integer.valueOf(1864),animation.frame("two",v,7000,false).rating);
    }
    @Test public void reducedMotionImmediatelyShowsTheFinalValues(){
        MatchReportAnimation.Frame frame=MatchReportAnimation.sample(MatchReportView.preview(),0,true);
        assertEquals(Integer.valueOf(1902),frame.rating);assertEquals(0.7,frame.progress,0.00001);assertEquals(1,frame.entrance,0);
    }
    @Test public void hiddenOrUnconfirmedRatingsNeverCount(){
        Models.Status s=result();s.match.rated=false;
        assertNull(MatchReportAnimation.sample(MatchReportView.from(s),900,false).rating);
        s.match.rated=true;s.player.games=4;
        assertNull(MatchReportAnimation.sample(MatchReportView.from(s),900,false).rating);
        s.player.games=20;s.match.state="review";
        assertNull(MatchReportAnimation.sample(MatchReportView.from(s),900,false).rating);
        s.match.state="complete";s.match.ratingChange=null;
        assertNull(MatchReportAnimation.sample(MatchReportView.from(s),900,false).rating);
    }
}
