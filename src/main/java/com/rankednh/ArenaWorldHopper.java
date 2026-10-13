package com.rankednh;

import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/** A hop is armed only by the user's click; RuneLite's normal world-switcher checks still apply. */
final class ArenaWorldHopper
{
    private final Client client;
    private final ClientThread clientThread;
    private final WorldService worlds;
    private final ScheduledExecutorService executor;
    private final RankedNhPlugin plugin;
    private volatile boolean busy;
    private long generation,started;
    private String matchId;
    private int round,world,attempts;
    private net.runelite.api.World target;
    private boolean submitted;

    @Inject ArenaWorldHopper(Client client,ClientThread clientThread,WorldService worlds,ScheduledExecutorService executor,RankedNhPlugin plugin){
        this.client=client;this.clientThread=clientThread;this.worlds=worlds;this.executor=executor;this.plugin=plugin;
    }
    boolean busy(){return busy;}
    static boolean eligible(Models.Status s){return s!=null && s.match!=null && "preparing".equals(s.match.state) && !"demo".equals(s.mode);}
    static String validate(World destination,int assigned,String region){
        if(destination==null || destination.getId()!=assigned)return "Assigned world is unavailable. Refresh the world list and try again.";
        if(!destination.getTypes().contains(WorldType.PVP_ARENA) || destination.getTypes().contains(WorldType.PVP) ||
            destination.getTypes().contains(WorldType.HIGH_RISK) || destination.getTypes().contains(WorldType.DEADMAN))
            return "Assigned world is not a safe Arena destination. Ask the operator to correct it.";
        String actual=destination.getLocation()==0?"US":destination.getLocation()==1 || destination.getLocation()==7?"EU":destination.getLocation()==3?"AU":"";
        if(!actual.equals(region))return "Assigned world does not match the competition region. Ask the operator to correct it.";
        if(destination.getPlayers()<0)return "Arena world is offline. Try again shortly.";
        if(destination.getPlayers()>=2000)return "Arena world is full. Try again shortly.";
        return null;
    }
    void request(){
        Models.Status s=plugin.status();
        if(busy)return;
        if(!plugin.accountMatches() || client.getGameState()!=GameState.LOGGED_IN){plugin.handoffProblem("Log into your enrolled account before hopping.");return;}
        if(!eligible(s)){plugin.handoffProblem(s!=null && "demo".equals(s.mode)?"Demo matches have no real Arena world to hop to.":"Both players must accept before hopping.");return;}
        if(client.getWorld()==s.match.world)return;
        busy=true;submitted=false;target=null;attempts=0;matchId=s.match.id;round=s.match.round;world=s.match.world;
        started=System.nanoTime();final long request=++generation;final String region=s.match.region;final int assigned=world;
        plugin.handoffProblem("");
        executor.execute(()->{
            World destination=null;
            try{WorldResult result=worlds.getWorlds();if(result!=null)destination=result.findWorld(assigned);}
            catch(RuntimeException ignored){/* Surface a retryable error without blocking the game thread. */}
            final World resolved=destination;
            clientThread.invoke(()->{
                if(request!=generation || !busy)return;
                String error=validate(resolved,assigned,region);
                if(error!=null){worlds.refresh();fail(error);return;}
                if(!current()){cancel();return;}
                target=client.createWorld();target.setId(resolved.getId());target.setAddress(resolved.getAddress());
                target.setActivity(resolved.getActivity());target.setLocation(resolved.getLocation());
                target.setPlayerCount(resolved.getPlayers());target.setTypes(WorldUtil.toWorldTypes(resolved.getTypes()));
            });
        });
    }
    private boolean current(){
        Models.Status s=plugin.status();
        return eligible(s) && plugin.accountMatches() && matchId.equals(s.match.id) && round==s.match.round && world==s.match.world && MatchFoundAlert.secondsLeft(s,plugin.serverNow())>0;
    }
    void tick(){
        if(!busy)return;
        if(System.nanoTime()-started>15_000_000_000L){fail("World hop did not complete. Finish any game dialog, then try again.");return;}
        if(client.getGameState()==GameState.HOPPING || client.getGameState()==GameState.LOADING)return;
        if(!current() || client.getGameState()!=GameState.LOGGED_IN){cancel();return;}
        if(client.getWorld()==world){cancel();return;}
        if(target==null || submitted)return;
        if(client.getWidget(InterfaceID.Worldswitcher.BUTTONS)==null){
            if(attempts++>=3){fail("Cannot open the world switcher. Finish what you're doing, then try again.");return;}
            client.openWorldHopper();
        }else{client.hopToWorld(target);submitted=true;}
    }
    private void fail(String message){cancel();plugin.handoffProblem(message);}
    void cancel(){generation++;busy=false;target=null;matchId=null;}
}
