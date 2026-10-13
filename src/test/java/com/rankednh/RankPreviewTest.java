package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class RankPreviewTest {
    @Test public void bothRevealsTurnOnceAndLetTheBadgeSettleBeforeShowingTheRating(){
        for(RankPreview scenario:new RankPreview[]{RankPreview.RANK_UP,RankPreview.PLACEMENTS}){
            MatchReportView view=MatchReportView.preview(scenario);
            long start=scenario==RankPreview.RANK_UP?MatchReportAnimation.PROMOTION_REVEAL_END:MatchReportAnimation.PLACEMENT_REVEAL;
            MatchReportAnimation.Frame middle=MatchReportAnimation.sample(view,start+500,false);
            assertTrue(middle.spin>0 && middle.spin<1);assertEquals(0,middle.rankAlpha,0);assertEquals(0,middle.ratingAlpha,0);
            MatchReportAnimation.Frame settled=MatchReportAnimation.sample(view,start+2000,false);
            assertEquals(1,settled.spin,0);assertEquals(1,settled.stillMix,0);assertEquals(1,settled.rankAlpha,0);
            assertEquals(1,settled.ratingAlpha,0);
            MatchReportAnimation.Frame reduced=MatchReportAnimation.sample(view,0,true);
            assertEquals(-1,reduced.spin,0);assertEquals(1,reduced.stillMix,0);
        }
    }
    @Test public void promotionFillsOldDivisionBeforeRevealingNewBadgeAndContinuingTheBar(){
        MatchReportView v=MatchReportView.preview(RankPreview.RANK_UP);
        MatchReportAnimation.Frame start=MatchReportAnimation.sample(v,300,false),before=MatchReportAnimation.sample(v,450,false),after=MatchReportAnimation.sample(v,1800,false),end=MatchReportAnimation.sample(v,MatchReportAnimation.PROMOTION_END,false);
        assertEquals("diamond_ii",start.badge);assertEquals(Integer.valueOf(2088),start.rating);assertEquals(.8,start.progress,.0001);
        assertEquals("diamond_ii",before.badge);assertTrue(before.progress>.8);
        assertEquals("diamond_i",after.badge);assertEquals("Rank up",after.title);assertTrue(after.progress<.8);
        MatchReportAnimation.Frame threshold=MatchReportAnimation.sample(v,1100,false);
        assertEquals(Integer.valueOf(2100),threshold.rating);assertEquals(1,threshold.progress,0);assertEquals("diamond_ii",threshold.badge);
        assertEquals(Integer.valueOf(2126),end.rating);assertEquals(26/60.0,end.progress,.0001);assertEquals("34 rating to Dragonstone III",end.progressLabel);
    }
    @Test public void promotionMotionIsMonotonicAndReducedMotionImmediatelyRevealsTheFinalRank(){
        MatchReportView v=MatchReportView.preview(RankPreview.RANK_UP);int last=2088;
        for(int ms=0;ms<=4000;ms+=16){MatchReportAnimation.Frame frame=MatchReportAnimation.sample(v,ms,false);assertTrue(frame.rating>=last);assertTrue(frame.rating<=2126);assertTrue(frame.progress>=0 && frame.progress<=1);last=frame.rating;}
        MatchReportAnimation.Frame reduced=MatchReportAnimation.sample(v,0,true);
        assertEquals(Integer.valueOf(2126),reduced.rating);assertEquals("diamond_i",reduced.badge);assertEquals(1,reduced.reveal,0);assertTrue(reduced.promoted);
        MatchReportAnimation a=new MatchReportAnimation();a.frame("promotion",v,0,false);
        assertEquals(Integer.valueOf(2126),a.frame("promotion",MatchReportView.preview(RankPreview.RANK_UP),4000,false).rating);
    }
    @Test public void previewsCoverGainLossAndPlacementRevealWithoutChangingAnyAccount(){
        assertEquals("+38",MatchReportView.preview(RankPreview.RATING_GAIN).delta);
        MatchReportView loss=MatchReportView.preview(RankPreview.RATING_LOSS);assertFalse(loss.won);
        assertTrue(MatchReportAnimation.sample(loss,300,false).rating>MatchReportAnimation.sample(loss,1800,false).rating);
        MatchReportView placement=MatchReportView.preview(RankPreview.PLACEMENTS);
        assertEquals("unranked",MatchReportAnimation.sample(placement,300,false).badge);
        assertEquals("diamond_i",MatchReportAnimation.sample(placement,1800,false).badge);
        for(RankPreview scenario:RankPreview.values()){
            MatchReportView view=MatchReportView.preview(scenario);assertTrue(view.preview);assertTrue(view.heading.contains("Preview"));assertTrue(view.note.contains("unchanged"));
        }
    }
    @Test public void placementHidesTheRankUntilRevealAndNeverCountsUpFromAHiddenRating(){
        MatchReportView view=MatchReportView.preview(RankPreview.PLACEMENTS);
        assertTrue(view.placementReveal);assertFalse(view.promotion());
        MatchReportAnimation.Frame waiting=MatchReportAnimation.sample(view,500,false);
        assertEquals("unranked",waiting.badge);assertEquals("",waiting.rank);assertNull(waiting.rating);
        assertEquals(0,waiting.rankAlpha,0);assertEquals(0,waiting.ratingAlpha,0);
        MatchReportAnimation.Frame revealed=MatchReportAnimation.sample(view,700,false);
        assertEquals("diamond_i",revealed.badge);assertEquals(Integer.valueOf(2126),revealed.rating);
        assertEquals(0,revealed.rankAlpha,0);
        MatchReportAnimation.Frame name=MatchReportAnimation.sample(view,1880,false);
        assertTrue(name.rankAlpha>0);assertEquals(0,name.ratingAlpha,0);
        MatchReportAnimation.Frame end=MatchReportAnimation.sample(view,MatchReportAnimation.PLACEMENT_END,false);
        assertEquals(1,end.badgeAlpha,0);assertEquals(1,end.rankAlpha,0);assertEquals(1,end.ratingAlpha,0);
        for(int ms=0;ms<=2600;ms+=16){
            MatchReportAnimation.Frame frame=MatchReportAnimation.sample(view,ms,false);
            assertTrue(frame.rating==null || frame.rating.equals(view.ratingAfter));
            assertEquals("",frame.progressLabel);assertEquals("",frame.progressDetail);
        }
    }
    @Test public void placementPollingKeepsItsClockAndReducedMotionShowsTheWholeResultImmediately(){
        MatchReportView view=MatchReportView.preview(RankPreview.PLACEMENTS);MatchReportAnimation animation=new MatchReportAnimation();
        animation.frame("first",view,1000,false);
        MatchReportAnimation.Frame end=animation.frame("first",MatchReportView.preview(RankPreview.PLACEMENTS),4000,false);
        assertEquals(3000,end.elapsed);assertEquals("diamond_i",end.badge);assertEquals(1,end.ratingAlpha,0);
        MatchReportAnimation.Frame reduced=MatchReportAnimation.sample(view,0,true);
        assertEquals("diamond_i",reduced.badge);assertEquals(Integer.valueOf(2126),reduced.rating);
        assertEquals(1,reduced.entrance,0);assertEquals(1,reduced.badgeAlpha,0);assertEquals(1,reduced.rankAlpha,0);assertEquals(1,reduced.ratingAlpha,0);
    }
}
