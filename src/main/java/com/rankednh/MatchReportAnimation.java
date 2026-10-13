package com.rankednh;

/** A monotonic clock: network polling never restarts an already visible result. */
final class MatchReportAnimation
{
    static final long ENTRANCE_MS=220,DELAY_MS=300,RATING_MS=1500;
    static final long SPIN_MS=1100;
    static final long PROMOTION_FILL_END=1100,PROMOTION_REVEAL_START=1100,PROMOTION_REVEAL_END=1260,
        PROMOTION_SPIN_END=PROMOTION_REVEAL_END+SPIN_MS,PROMOTION_END=PROMOTION_SPIN_END+800;
    static final long PLACEMENT_FADE_START=480,PLACEMENT_REVEAL=700,PLACEMENT_BADGE_END=PLACEMENT_REVEAL+SPIN_MS,
        PLACEMENT_NAME_START=PLACEMENT_BADGE_END-40,PLACEMENT_RATING_START=PLACEMENT_BADGE_END+160,PLACEMENT_END=PLACEMENT_BADGE_END+380;
    private String key;
    private long started;

    Frame frame(String reportKey,MatchReportView view,long now,boolean reducedMotion) {
        if(!reportKey.equals(key)){key=reportKey;started=now;}
        return sample(view,Math.max(0,now-started),reducedMotion);
    }
    void reset(){key=null;}

    static Frame sample(MatchReportView view,long elapsed,boolean reducedMotion) {
        Frame frame=sampleFrame(view,elapsed,reducedMotion);frame.elapsed=elapsed;return frame;
    }
    private static Frame sampleFrame(MatchReportView view,long elapsed,boolean reducedMotion) {
        if(view.placementReveal)return placement(view,elapsed,reducedMotion);
        if(view.promotion())return promotion(view,elapsed,reducedMotion);
        double entrance=reducedMotion?1:ease(elapsed/(double)ENTRANCE_MS);
        double t=reducedMotion?1:ease((elapsed-DELAY_MS)/(double)RATING_MS);
        Integer rating=null;
        double progress=view.progress;
        String label=view.progressLabel,detail=view.progressDetail;
        String badge=view.badge,rank=view.rank,title=view.title;
        if(view.ratingBefore!=null && view.ratingAfter!=null) {
            double interpolated=view.ratingBefore+(view.ratingAfter-view.ratingBefore)*t;
            rating=(int)Math.round(interpolated);
            boolean beforePromotion=view.badgeBefore!=null && view.previousDivisionStart!=null && view.divisionStart!=null && interpolated<view.divisionStart;
            // The badge and track describe the confirmed final division. Crossing into it
            // starts at zero; a demotion enters it from a full bar. Never infer a rank locally.
            if(view.divisionStart!=null && view.nextAt!=null && view.nextAt>view.divisionStart) {
                progress=clamp((interpolated-view.divisionStart)/(double)(view.nextAt-view.divisionStart));
                label=Math.max(0,view.nextAt-rating)+" rating to "+(view.nextRank==null?"next division":view.nextRank);
                detail=rating+" / "+view.nextAt+" rating";
            }
            if(beforePromotion){
                badge=view.badgeBefore;rank=view.rankBefore;
                progress=clamp((interpolated-view.previousDivisionStart)/(view.divisionStart-(double)view.previousDivisionStart));
                label=Math.max(0,view.divisionStart-rating)+" rating to "+view.rank;detail=rating+" / "+view.divisionStart+" rating";
            }else if(view.previousDivisionStart!=null)title="Rank up";
            if(view.badgeBefore!=null && view.previousDivisionStart==null && t<0.5){badge=view.badgeBefore;rank=view.rankBefore;}
        }
        return new Frame(entrance,rating,progress,label,detail,badge,rank,title);
    }
    private static Frame placement(MatchReportView view,long elapsed,boolean reducedMotion){
        boolean revealed=reducedMotion || elapsed>=PLACEMENT_REVEAL;
        Frame frame=new Frame(reducedMotion?1:ease(elapsed/(double)ENTRANCE_MS),
            revealed?view.ratingAfter:null,0,"","",revealed?view.badge:"unranked",revealed?view.rank:"","Placements complete");
        frame.reveal=reducedMotion?1:clamp((elapsed-PLACEMENT_REVEAL)/(double)(PLACEMENT_BADGE_END-PLACEMENT_REVEAL));
        frame.spin=revealed && !reducedMotion?frame.reveal:-1;
        frame.badgeAlpha=reducedMotion?1:revealed?ease((elapsed-PLACEMENT_REVEAL)/100.0):1-ease((elapsed-PLACEMENT_FADE_START)/(double)(PLACEMENT_REVEAL-PLACEMENT_FADE_START));
        frame.badgeScale=revealed?.96+.04*ease(frame.reveal):1;
        frame.stillMix=reducedMotion?1:ease((elapsed-PLACEMENT_BADGE_END)/160.0);
        frame.rankAlpha=reducedMotion?1:ease((elapsed-PLACEMENT_NAME_START)/200.0);
        frame.ratingAlpha=reducedMotion?1:ease((elapsed-PLACEMENT_RATING_START)/(double)(PLACEMENT_END-PLACEMENT_RATING_START));
        return frame;
    }
    private static Frame promotion(MatchReportView view,long elapsed,boolean reducedMotion){
        double reveal=reducedMotion?1:clamp((elapsed-PROMOTION_REVEAL_START)/(double)(PROMOTION_REVEAL_END-PROMOTION_REVEAL_START));
        boolean promoted=reducedMotion || elapsed>=PROMOTION_REVEAL_END;
        double rating;
        if(reducedMotion)rating=view.ratingAfter;
        else if(elapsed<PROMOTION_FILL_END)rating=view.ratingBefore+(view.divisionStart-view.ratingBefore)*ease((elapsed-DELAY_MS)/(double)(PROMOTION_FILL_END-DELAY_MS));
        else rating=view.divisionStart+(view.ratingAfter-view.divisionStart)*ease((elapsed-PROMOTION_SPIN_END)/(double)(PROMOTION_END-PROMOTION_SPIN_END));
        int displayed=(int)Math.floor(rating+1e-7);
        double progress=promoted?(view.nextAt==null?1:clamp((rating-view.divisionStart)/(view.nextAt-(double)view.divisionStart))):
            clamp((rating-view.previousDivisionStart)/(view.divisionStart-(double)view.previousDivisionStart));
        String label=promoted?(view.nextAt==null?"Highest division reached":Math.max(0,view.nextAt-displayed)+" rating to "+view.nextRank):
            displayed>=view.divisionStart?"New rank unlocked":(view.divisionStart-displayed)+" rating to "+view.rank;
        Frame frame=new Frame(reducedMotion?1:ease(elapsed/(double)ENTRANCE_MS),displayed,progress,label,
            displayed+" / "+(promoted?view.nextAt:view.divisionStart)+" rating",promoted?view.badge:view.badgeBefore,
            promoted?view.rank:view.rankBefore,promoted?"Rank up":view.title);
        frame.reveal=reveal;frame.promoted=promoted;
        frame.spin=promoted && !reducedMotion?clamp((elapsed-PROMOTION_REVEAL_END)/(double)SPIN_MS):-1;
        frame.badgeAlpha=reducedMotion?1:promoted?ease((elapsed-PROMOTION_REVEAL_END)/100.0):1-ease(reveal);
        frame.badgeScale=promoted?.96+.04*ease(frame.spin<0?1:frame.spin):1;
        frame.stillMix=reducedMotion?1:ease((elapsed-PROMOTION_SPIN_END)/160.0);
        frame.rankAlpha=reducedMotion?1:promoted?ease((elapsed-PROMOTION_SPIN_END+40)/200.0):1-ease(reveal);
        frame.ratingAlpha=reducedMotion?1:promoted?ease((elapsed-PROMOTION_SPIN_END)/200.0):1-ease(reveal);
        return frame;
    }
    private static double clamp(double n){return Math.max(0,Math.min(1,n));}
    private static double ease(double n){double t=clamp(n);return 1-Math.pow(1-t,3);}

    static final class Frame {
        final double entrance,progress;
        final Integer rating;
        final String progressLabel,progressDetail;
        final String badge,rank,title;
        double reveal=1;
        double badgeAlpha=1,badgeScale=1,rankAlpha=1,ratingAlpha=1;
        double spin=-1,stillMix=1;
        long elapsed;
        boolean promoted;
        Frame(double entrance,Integer rating,double progress,String label,String detail,String badge,String rank,String title){
            this.entrance=entrance;this.rating=rating;this.progress=progress;progressLabel=label;progressDetail=detail;
            this.badge=badge;this.rank=rank;this.title=title;
        }
    }
}
