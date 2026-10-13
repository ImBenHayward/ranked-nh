package com.rankednh;

import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.WorldService;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

/** Validates the suggested Arena world and opens the picker. The player selects the world. */
final class ArenaWorldGuide
{
    private final Client client;
    private final ClientThread clientThread;
    private final WorldService worlds;
    private final ScheduledExecutorService executor;
    private final RankedNhPlugin plugin;
    private volatile boolean busy;
    private long generation,started;
    private String matchId;
    private int round,world;
    private boolean readyToOpen;
    private volatile String instruction="",region="";
    private boolean setupHop;
    private String playerId;

    @Inject ArenaWorldGuide(Client client,ClientThread clientThread,WorldService worlds,ScheduledExecutorService executor,RankedNhPlugin plugin){
        this.client=client;this.clientThread=clientThread;this.worlds=worlds;this.executor=executor;this.plugin=plugin;
    }
    boolean busy(){return busy;}
    String instruction(){return instruction;}
    String region(){return region;}
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
        if(!plugin.accountMatches() || client.getGameState()!=GameState.LOGGED_IN){plugin.handoffProblem("Log into your enrolled account before opening the world switcher.");return;}
        if(!eligible(s)){plugin.handoffProblem(s!=null && "demo".equals(s.mode)?"Demo matches have no real Arena destination.":"Both players must accept before opening the world switcher.");return;}
        if(client.getWorld()==s.match.world)return;
        setupHop=false;matchId=s.match.id;round=s.match.round;
        start(s.match.world,s.match.region);
    }
    void requestSetup(String region){
        Models.Status s=plugin.status();
        if(busy)return;
        if(!plugin.accountMatches() || client.getGameState()!=GameState.LOGGED_IN || !ArenaOnboarding.idle(s) ||
            "demo".equals(s.mode) || plugin.inArenaBattle() || !("EU".equals(region) || "US".equals(region)))return;
        if(client.getWorldType().contains(net.runelite.api.WorldType.PVP_ARENA))return;
        setupHop=true;playerId=s.player.id;
        start(0,region);
    }
    private void start(int assigned,String region){
        busy=true;readyToOpen=false;world=assigned;instruction="";this.region=region;
        started=System.nanoTime();final long request=++generation;
        plugin.handoffProblem("");
        executor.execute(()->{
            World destination=null;
            try{
                WorldResult result=worlds.getWorlds();
                if(result!=null){
                    if(assigned>0)destination=result.findWorld(assigned);
                    else for(World candidate:result.getWorlds())if(validate(candidate,candidate.getId(),region)==null){destination=candidate;break;}
                }
            }
            catch(RuntimeException ignored){/* Surface a retryable error without blocking the game thread. */}
            final World resolved=destination;
            clientThread.invoke(()->{
                if(request!=generation || !busy)return;
                String error=validate(resolved,assigned>0?assigned:resolved==null?0:resolved.getId(),region);
                if(error!=null){worlds.refresh();fail(error);return;}
                if(!current()){cancel();return;}
                world=resolved.getId();
                instruction="Choose World "+world+" ("+region+") in the game’s world switcher.";
                readyToOpen=true;
            });
        });
    }
    private boolean current(){
        Models.Status s=plugin.status();
        if(setupHop)return ArenaOnboarding.idle(s) && plugin.accountMatches() && playerId.equals(s.player.id) && !plugin.inArenaBattle();
        return eligible(s) && plugin.accountMatches() && matchId.equals(s.match.id) && round==s.match.round && world==s.match.world && MatchFoundAlert.secondsLeft(s,plugin.serverNow())>0;
    }
    void tick(){
        if(!busy)return;
        if(System.nanoTime()-started>15_000_000_000L){fail("Could not load Arena worlds. Try again shortly.");return;}
        if(client.getGameState()==GameState.HOPPING || client.getGameState()==GameState.LOADING)return;
        if(!current() || client.getGameState()!=GameState.LOGGED_IN){cancel();return;}
        if(client.getWorld()==world){cancel();return;}
        if(!readyToOpen)return;
        // Opening the native picker does not select a world or perform a hop.
        client.openWorldHopper();
        busy=false;readyToOpen=false;
        plugin.handoffProblem("");
    }
    private void fail(String message){cancel();plugin.handoffProblem(message);}
    void cancel(){generation++;busy=false;readyToOpen=false;matchId=null;playerId=null;setupHop=false;instruction="";region="";}
}
