package com.rankednh;

import java.util.ArrayList;
import java.util.Locale;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.WorldType;

/** Client-thread only. Observes only the two enrolled participants during an armed Arena round. */
final class FightRecorder
{
    private Models.Status status;
    private String nonce;
    private final ArrayList<Models.Hit> hits=new ArrayList<>();
    private int startTick=-1, firstDeathTick=-1;
    private boolean selfDead,opponentDead,finished,interrupted;
    private String message="Waiting for a round";
    static String normalized(String name) {
        return name==null?"":name.replace('\u00a0',' ').replace('_',' ').trim().toLowerCase(Locale.ROOT);
    }
    void arm(Models.Status next) {
        Models.Match match=next.match;
        if(match==null || !("active".equals(match.state) || "reporting".equals(match.state))) { clear(); return; }
        if(match.nonce==null || match.nonce.equals(nonce)) return;
        clear();status=next;nonce=match.nonce;message="Ready to observe your manual Arena fight";
    }
    void clear() {
        status=null;nonce=null;hits.clear();startTick=-1;firstDeathTick=-1;
        selfDead=false;opponentDead=false;finished=false;interrupted=false;message="Waiting for a round";
    }
    private boolean context(Client client) {
        Player local=client.getLocalPlayer();
        return status!=null && !finished && !interrupted && client.getGameState()==GameState.LOGGED_IN &&
            local!=null && normalized(local.getName()).equals(normalized(status.player.rsn)) &&
            client.getWorld()==status.match.world && ArenaCombat.inBattle(client);
    }
    private String target(Actor actor) {
        if(!(actor instanceof Player) || status==null) return null;
        String name=normalized(actor.getName());
        if(name.equals(normalized(status.player.rsn))) return "self";
        if(name.equals(normalized(status.match.opponent.rsn))) return "opponent";
        return null;
    }
    void hit(Client client,Actor actor,int amount) {
        if(!context(client)) return;
        String target=target(actor);if(target==null) return;
        // Only begin while the assigned players are interacting. This is a pilot heuristic,
        // not a verified Arena result signal or protection against forged clients.
        if(startTick<0) {
            Actor interacting=client.getLocalPlayer().getInteracting();
            boolean opponentTargeted="opponent".equals(target(interacting));
            boolean opponentTargetsSelf="opponent".equals(target) && "self".equals(target(actor.getInteracting()));
            if(!opponentTargeted && !opponentTargetsSelf) return;
            // Arena applies build stats after the challenge, not on lobby arrival.
            if(client.getLocalPlayer().getCombatLevel()!=126 || !maxStats(client)){
                interrupted=true;message="Build mismatch — request a review";return;
            }
            startTick=client.getTickCount();
        }
        recordHit(target,amount,client.getTickCount());
    }
    static boolean maxStats(Client client){
        for(Skill skill:new Skill[]{Skill.ATTACK,Skill.STRENGTH,Skill.DEFENCE,Skill.HITPOINTS,Skill.RANGED,Skill.MAGIC,Skill.PRAYER})
            if(client.getRealSkillLevel(skill)!=99)return false;
        return true;
    }
    void recordHit(String target,int amount,int gameTick) {
        if(status==null || finished || interrupted || !("self".equals(target) || "opponent".equals(target)))return;
        if(startTick<0)startTick=gameTick;
        int tick=gameTick-startTick;
        if(tick>1500 || hits.size()>=3000) {interrupted=true;message="Recording limit reached — request a review";return;}
        if(amount<0 || amount>200) return;
        hits.add(new Models.Hit(target,amount,tick));message="Recording this round · "+hits.size()+" observations";
    }
    void death(Client client,Actor actor) {
        if(!context(client) || startTick<0) return;
        String target=target(actor);if(target==null) return;
        recordDeath(target,client.getTickCount());
    }
    void recordDeath(String target,int gameTick) {
        if(status==null || startTick<0 || finished || interrupted || !("self".equals(target) || "opponent".equals(target)))return;
        if("self".equals(target)) selfDead=true;else opponentDead=true;
        if(firstDeathTick<0) firstDeathTick=gameTick;
        message="Confirming observed round outcome";
    }
    void gameState(GameState state) {
        if(status!=null && !finished && (state==GameState.LOGIN_SCREEN || state==GameState.CONNECTION_LOST || state==GameState.HOPPING)) {
            interrupted=true;message="Recording interrupted — request a review";
        }
    }
    Models.Report tick(Client client) {
        if(!context(client))return null;
        return finishAt(client.getTickCount());
    }
    Models.Report finishAt(int gameTick) {
        if(status==null || finished || interrupted || firstDeathTick<0 || gameTick-firstDeathTick<3)return null;
        Models.Report result=new Models.Report();
        result.round=status.match.round;result.nonce=nonce;result.world=status.match.world;
        result.winner=selfDead?(opponentDead?null:status.match.opponent.id):status.player.id;
        result.endReason=selfDead && opponentDead?"double-death":"death";
        result.durationTicks=Math.max(1,gameTick-startTick);
        if(result.durationTicks>1500) {interrupted=true;message="Recording limit reached — request a review";return null;}
        result.hits=new ArrayList<>(hits);finished=true;message="Round report queued for submission";
        return result;
    }
    String message() { return message; }
    void stopForSetup(String reason){interrupted=true;message="Round not recorded: "+reason;}
    boolean started(){return startTick>=0;}
}
