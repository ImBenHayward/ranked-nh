package com.rankednh;

/** Presentation of server-confirmed results. Never infers a rating award from local fight events. */
final class MatchReportView
{
    String heading,title,opponent,score,badge,rank,rating,delta,progressLabel,progressDetail,note;
    double progress;
    Integer ratingBefore,ratingAfter,divisionStart,nextAt;
    String nextRank;
    String badgeBefore,rankBefore;
    Integer previousDivisionStart;
    boolean won,decided,preview,placementReveal;
    boolean promotion(){return badgeBefore!=null && previousDivisionStart!=null && divisionStart!=null &&
        ratingBefore!=null && ratingAfter!=null && ratingBefore<divisionStart && ratingAfter>=divisionStart;}

    static boolean reportable(Models.Status s) {
        return s!=null && s.player!=null && s.match!=null &&
            ("complete".equals(s.match.state) || "review".equals(s.match.state) || "void".equals(s.match.state));
    }
    static MatchReportView from(Models.Status s) {
        if(!reportable(s))return null;
        MatchReportView v=new MatchReportView();Models.Match m=s.match;Models.Player p=s.player;
        Models.Rank rank=m.rankAfter!=null?m.rankAfter:p.rank;
        v.heading="demo".equals(s.mode)?"Local demo · Match report":s.season!=null && s.season.beta?"Beta season · Match report":"Ranked NH · Match report";
        v.decided="complete".equals(m.state);v.won=v.decided && p.id.equals(m.winner);
        v.title=v.decided?(v.won?"Victory":"Defeat"):("review".equals(m.state)?"Result pending":"Match voided");
        v.opponent="vs "+m.opponent.rsn+" · Best of three";
        v.score=m.score[0]+" – "+m.score[1];
        v.badge=rank.badge;v.rank=rank.name;
        v.delta="";
        boolean placed=m.rankAfter!=null?!"unranked".equals(rank.badge):p.games>=5;
        if(!v.decided) {
            v.rating="Rating unchanged";
            v.note="review".equals(m.state)?"Awaiting review. No result has been awarded.":"This match did not affect your rating.";
        } else if(!m.rated) {
            v.rating="Unrated match";v.note="Your rating and placements are unchanged.";
        } else if(m.ratingChange==null) {
            v.rating="Rating update pending";v.note="Waiting for the confirmed rating change.";
        } else if(!placed) {
            v.rating="Placement recorded";v.note="Your rank is revealed after five rated matches.";
        } else {
            v.delta=(m.ratingChange>=0?"+":"")+m.ratingChange;
            v.ratingBefore=m.ratingBefore;v.ratingAfter=m.ratingAfter;
            v.rating=m.ratingBefore!=null && m.ratingAfter!=null?m.ratingBefore+" → "+m.ratingAfter+" rating":"Rating updated";
            v.note=p.games==5?"Placements complete. Your rank is unlocked.":"One rating update for the whole series.";
            // A first rank needs a confirmed, rated fifth result, even if that result was a loss.
            v.placementReveal=(m.rankBefore!=null?"unranked".equals(m.rankBefore.badge):p.games==5)
                && m.ratingAfter!=null && rank.badge!=null && !"unranked".equals(rank.badge);
            if(!v.placementReveal && m.rankBefore!=null && m.rankBefore.divisionStart!=null && rank.divisionStart!=null
                && m.rankBefore.divisionStart<rank.divisionStart){
                v.badgeBefore=m.rankBefore.badge;v.rankBefore=m.rankBefore.name;
                v.previousDivisionStart=m.rankBefore.divisionStart;
            }
        }
        v.divisionStart=rank.divisionStart;v.nextAt=rank.nextAt;v.nextRank=rank.nextRank;
        if(!placed) {
            v.progress=Math.max(0,Math.min(1,p.games/5.0));
            v.progressLabel=p.games+" / 5 placements";
            int left=Math.max(0,5-p.games);
            v.progressDetail=left+" rated match"+(left==1?"":"es")+" to reveal your rank";
        } else if(rank.nextAt==null) {
            v.progress=1;v.progressLabel="Highest division reached";v.progressDetail="Keep climbing the leaderboard.";
        } else {
            v.progress=rank.progress==null?0:rank.progress;
            // Use the same integer snapshot as the counter so its endpoint agrees with the bar.
            if(v.ratingAfter!=null && v.divisionStart!=null && v.nextAt>v.divisionStart)
                v.progress=Math.max(0,Math.min(1,(v.ratingAfter-v.divisionStart)/(double)(v.nextAt-v.divisionStart)));
            int left=rank.pointsToNext==null?Math.max(0,rank.nextAt-(int)Math.floor(p.rating)):rank.pointsToNext;
            v.progressLabel=left+" rating to "+(rank.nextRank==null?"next division":rank.nextRank);
            v.progressDetail=(v.ratingAfter!=null?v.ratingAfter:(int)Math.floor(p.rating))+" / "+rank.nextAt+" rating";
        }
        return v;
    }
    static MatchReportView preview() {
        MatchReportView v=new MatchReportView();
        v.heading="Preview · Sample result";v.title="Victory";v.opponent="vs Demo Opponent · Best of three";v.score="2 – 1";
        v.badge="dragonstone_iii";v.rank="Dragonstone III";v.rating="2164 → 2202 rating";v.delta="+38";
        v.progressLabel="18 rating to Dragonstone II";v.progressDetail="2202 / 2220 rating";v.progress=0.7;
        v.ratingBefore=2164;v.ratingAfter=2202;v.divisionStart=2160;v.nextAt=2220;v.nextRank="Dragonstone II";
        v.note="Sample only. Your real rating is unchanged.";v.won=true;v.decided=true;v.preview=true;
        return v;
    }
    static MatchReportView preview(RankPreview scenario){
        MatchReportView v=preview();
        if(scenario==RankPreview.RANK_UP){
            v.badgeBefore="diamond_ii";v.rankBefore="Diamond II";v.previousDivisionStart=2040;
            v.badge="diamond_i";v.rank="Diamond I";v.ratingBefore=2088;v.ratingAfter=2126;
            v.rating="2088 → 2126 rating";v.divisionStart=2100;v.nextAt=2160;v.nextRank="Dragonstone III";
            v.progress=26/60.0;v.progressLabel="34 rating to Dragonstone III";v.progressDetail="2126 / 2160 rating";
        }else if(scenario==RankPreview.RATING_LOSS){
            v.title="Defeat";v.score="1 – 2";v.won=false;v.ratingBefore=2210;v.ratingAfter=2183;v.delta="-27";
            v.rating="2210 → 2183 rating";v.progress=23/60.0;v.progressLabel="37 rating to Dragonstone II";v.progressDetail="2183 / 2220 rating";
        }else if(scenario==RankPreview.PLACEMENTS){
            v.title="Rank revealed";v.badgeBefore="unranked";v.rankBefore="Unranked";
            v.badge="diamond_i";v.rank="Diamond I";v.ratingAfter=2126;
            v.placementReveal=true;
            v.note="Sample placement reveal. Your real rating is unchanged.";
        }
        return v;
    }
}
