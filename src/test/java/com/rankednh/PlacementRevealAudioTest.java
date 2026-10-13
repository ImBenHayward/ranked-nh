package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlacementRevealAudioTest
{
    static final class Player implements PlacementRevealAudio.Playback {
        int plays,stops,volume;long offset;
        public void play(long offsetMillis,int volume){plays++;offset=offsetMillis;this.volume=volume;}
        public void stop(){stops++;}
    }
    private final Player player=new Player();
    private final PlacementRevealAudio audio=new PlacementRevealAudio(player);
    private final MatchReportView view=MatchReportView.preview(RankPreview.PLACEMENTS);
    private void update(String key,long elapsed,boolean enabled,int volume,boolean reduced){
        audio.update(key,view,MatchReportAnimation.sample(view,elapsed,reduced),enabled,volume,reduced);
    }
    @Test public void playsOnceAcrossPollingAndHidingUntilAnExplicitReplay(){
        update("one",0,true,65,false);update("one",20,true,65,false);update("one",2000,true,65,false);
        assertEquals(1,player.plays);assertEquals(0,player.offset);assertEquals(65,player.volume);
        audio.hide();assertEquals(1,player.stops);update("one",2100,true,65,false);assertEquals(1,player.plays);
        audio.reset();update("one",0,true,65,false);assertEquals(2,player.plays);
        update("two",0,true,65,false);assertEquals(3,player.plays);assertEquals(2,player.stops);
    }
    @Test public void mutingStopsImmediatelyAndEnablingMidRevealDoesNotStartALateHum(){
        update("one",0,false,65,false);update("one",100,true,65,false);assertEquals(0,player.plays);
        update("two",0,true,0,false);update("two",100,true,65,false);assertEquals(0,player.plays);
        update("three",0,true,65,false);update("three",100,false,65,false);assertEquals(1,player.plays);assertEquals(1,player.stops);
        update("three",200,true,65,false);assertEquals(1,player.plays);
    }
    @Test public void reducedMotionPlaysOnlyTheRevealChordAndSettledReportsStaySilent(){
        update("one",0,true,65,true);assertEquals(1800,player.offset);
        update("two",2600,true,65,false);assertEquals(1,player.plays);
        MatchReportView ordinary=MatchReportView.preview();
        audio.update("three",ordinary,MatchReportAnimation.sample(ordinary,0,false),true,65,false);
        assertEquals(1,player.plays);
    }
    @Test public void packagedCueIsFiniteClickFreeAndVolumeScalingLeavesHeadroom()throws Exception{
        byte[] pcm=PlacementRevealAudio.pcm(100),quiet=PlacementRevealAudio.pcm(65),muted=PlacementRevealAudio.pcm(0);
        assertEquals(44100*2*3.4,pcm.length,1);
        int peak=0;long humEnergy=0,chimeEnergy=0;
        for(int i=0;i<pcm.length;i+=2){
            int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));int low=(short)((quiet[i]&255)|(quiet[i+1]<<8));
            peak=Math.max(peak,Math.abs(sample));assertTrue(Math.abs(low)<=Math.abs(sample));
            assertEquals(0,muted[i]);assertEquals(0,muted[i+1]);
            double time=i/88200.0;if(time>.3 && time<.8)humEnergy+=(long)sample*sample;
            if(time>1.9 && time<2.4)chimeEnergy+=(long)sample*sample;
        }
        assertTrue(peak>4000 && peak<25000);assertTrue(humEnergy>0);assertTrue(chimeEnergy>humEnergy);
        assertEquals(0,pcm[0]);assertEquals(0,pcm[1]);assertEquals(0,pcm[pcm.length-1]);assertEquals(0,pcm[pcm.length-2]);
    }
    @Test public void promotionCueWaitsForTheBarAndItsChimeLandsWithTheShield(){
        MatchReportView promotion=MatchReportView.preview(RankPreview.RANK_UP);
        long start=MatchReportAnimation.PROMOTION_SPIN_END-PlacementRevealAudio.CHIME_MS;
        audio.update("promotion",promotion,MatchReportAnimation.sample(promotion,0,false),true,65,false);
        assertEquals(0,player.plays);
        audio.update("promotion",promotion,MatchReportAnimation.sample(promotion,start,false),true,65,false);
        assertEquals(1,player.plays);assertEquals(0,player.offset);
        audio.update("promotion",promotion,MatchReportAnimation.sample(promotion,start+100,false),true,65,false);
        assertEquals(1,player.plays);audio.hide();assertEquals(1,player.stops);
        audio.reset();
        audio.update("muted",promotion,MatchReportAnimation.sample(promotion,0,false),false,65,false);
        audio.update("muted",promotion,MatchReportAnimation.sample(promotion,start,false),true,65,false);
        assertEquals(1,player.plays);
    }
}
