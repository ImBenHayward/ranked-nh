package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReportSeenStoreTest {
    private final String[] saved={null};
    private ReportSeenStore store(){return new ReportSeenStore(()->saved[0],value->saved[0]=value);}
    private MatchReportVisibility visibility(){ReportSeenStore store=store();return new MatchReportVisibility(store::contains,store::remember);}
    private Models.Status result(){Models.Status s=MatchupTest.status();s.match.state="complete";return s;}
    @Test public void seenAndClosedReportsStayClosedAcrossRestartReconnectAndExplicitSessionReset(){
        Models.Status status=result();MatchReportVisibility first=visibility();
        assertTrue(first.visible(status,"service-a",0,60));assertNotNull(saved[0]);first.dismiss();
        assertFalse(first.visible(status,"service-a",100,60));
        MatchReportVisibility restarted=visibility();assertFalse(restarted.visible(status,"service-a",200,60));
        restarted.reset();assertFalse(restarted.visible(status,"service-a",300,60));
        restarted.reopen();assertTrue(restarted.visible(status,"service-a",400,60));
        assertFalse(visibility().visible(status,"service-a",500,60));
    }
    @Test public void newAccountServiceMatchAndResolvedReviewCanStillOpen(){
        Models.Status status=result();status.match.state="review";MatchReportVisibility review=visibility();
        assertFalse(review.visible(status,"a",0,60));review.reopen();assertTrue(review.visible(status,"a",0,60));
        status.match.state="complete";assertTrue(visibility().visible(status,"a",0,60));
        assertTrue(visibility().visible(status,"b",0,60));
        status.player.id="another-account";assertTrue(visibility().visible(status,"a",0,60));
        status.match.id="another-match";assertTrue(visibility().visible(status,"a",0,60));
    }
    @Test public void connectionDoesNotReplayAnOldResultButNewLiveResultsStillOpen(){
        Models.Status status=result();MatchReportVisibility reports=visibility();
        reports.connected(status,"service");assertFalse(reports.visible(status,"service",0,60));
        assertFalse(visibility().visible(status,"service",100,60));
        reports.reopen();assertTrue(reports.visible(status,"service",200,60));reports.dismiss();
        status.match.id="new-match";status.match.state="active";reports.connected(status,"service");
        assertFalse(reports.visible(status,"service",300,60));
        status.match.state="complete";assertTrue(reports.visible(status,"service",400,60));
    }
    @Test public void openingAPreviewDoesNotUseTheRealResultHistoryAndStorageIsBounded(){
        Models.Status status=result();status.queued=true;assertFalse(visibility().visible(status,"a",0,60));assertNull(saved[0]);
        for(RankPreview scenario:RankPreview.values())assertTrue(MatchReportView.preview(scenario).preview);
        assertNull(saved[0]);ReportSeenStore store=store();for(int i=0;i<150;i++)store.remember("report"+i);
        assertEquals(128,saved[0].split(",").length);assertTrue(store.contains("report149"));assertFalse(store.contains("report0"));
    }
}
