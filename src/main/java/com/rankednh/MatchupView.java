package com.rankednh;

/** Matchup copy and actions for the Ranked NH sidebar. */
final class MatchupView
{
    String key,heading,opponent,destination,message,primary,action,secondary="Open panel",secondaryAction="panel",footer="";
    String region; int world; Models.Rank opponentRank;
    boolean enabled,secondaryEnabled=true,compact,preview;
    static String key(Models.Status s){return s.match.id+":"+s.match.state+":"+s.match.round;}
    static MatchupView from(Models.Status s,boolean accountMatches,boolean busy,int world,boolean arena,boolean nearby,boolean hopping,boolean fighting,long now){
        if(!accountMatches || s==null || s.match==null || s.queued)return null;
        Models.Match m=s.match;
        if(!("offered".equals(m.state) || "preparing".equals(m.state) || "active".equals(m.state)))return null;
        MatchupView v=new MatchupView();v.key=key(s);v.opponent=m.opponent.rsn;v.opponentRank=m.opponent.rank;v.region=m.region;v.world=m.world;
        v.destination=MatchFoundAlert.region(m.region)+" · World "+m.world;
        v.enabled=!busy;
        if("offered".equals(m.state)){
            int seconds=MatchFoundAlert.secondsLeft(s,now);
            v.heading="Match found";v.primary=m.accepted?"Accepted":"Accept match";v.action="accept";
            v.message=seconds==0?"Offer expired. Updating…":m.accepted?"Waiting for your opponent · "+seconds+"s":seconds+" seconds to accept";
            v.enabled=!busy && !m.accepted && seconds>0;
            v.secondary=m.accepted?"Open panel":"Decline";v.secondaryAction=m.accepted?"panel":"decline";
            v.secondaryEnabled=m.accepted || (!busy && seconds>0);
            v.footer=m.accepted?"Both players must accept before travelling.":"Declining starts a queue cooldown.";
        }else if("preparing".equals(m.state)){
            v.heading="Preparing fight "+m.round;
            if(world!=m.world || !arena){
                v.message=hopping?"Switching worlds. Complete any confirmation in game.":"Travel to World "+m.world+" to meet your opponent.";
                v.primary=hopping?"Hopping…":"Hop to World "+m.world;v.action="hop";
                v.heading="Head to the Arena";
                v.enabled=!busy && !hopping && !"demo".equals(s.mode);
                v.footer="demo".equals(s.mode)?"Demo world only. No real Arena destination.":"Your opponent will be marked when nearby.";
            }else{
                v.message=!nearby?"Waiting for "+v.opponent+" nearby…":m.ready?"You're checked in. Waiting for your opponent's client.":"Opponent found. Checking you in…";
                v.primary="";v.action="panel";v.enabled=false;
                v.footer="";
            }
            if(MatchFoundAlert.secondsLeft(s,now)==0){v.enabled=false;v.message="Preparation expired. Updating…";}
        }else{
            v.heading=fighting?"Fight in progress":"Challenge opponent";
            v.message=fighting?"Recording this fight.":"Right-click your opponent → Challenge.";
            v.primary="Open panel";v.action="panel";v.enabled=true;v.secondary="";v.secondaryAction="";
            v.footer=fighting?"":"Then choose Max/Med.";v.compact=fighting;
        }
        return v;
    }
    static MatchupView preview(int stage){
        MatchupView v=new MatchupView();v.key="preview:"+stage;v.preview=true;
        v.heading=stage==0?"Match found":stage==1?"Head to the Arena":stage==2?"Preparing fight 1":"Challenge opponent";
        v.opponent="Demo Opponent";v.destination="EU · World 558";v.region="EU";v.world=558;
        v.message=stage==0?"57 seconds to accept":stage==1?"Travel to World 558 to meet your opponent.":stage==2?"Opponent found. Checking you in automatically…":"Right-click your opponent → Challenge.";
        v.primary=stage==0?"Accept match":stage==1?"Hop to World 558":stage==2?"Next preview step":"Replay preview";v.action="preview";v.enabled=true;
        v.secondary="Exit preview";v.secondaryAction="close-preview";v.footer="Sample only. No game or match actions.";
        return v;
    }
}
