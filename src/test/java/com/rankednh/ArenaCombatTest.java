package com.rankednh;

import java.lang.reflect.Proxy;
import java.util.EnumSet;
import net.runelite.api.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaCombatTest {
    private int area=0,state=0,timer=0;
    private boolean arena=true,loggedIn=true,challenge=false;
    private final Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(p,m,a)->{
        switch(m.getName()){
            case "getGameState":return loggedIn?GameState.LOGGED_IN:GameState.LOGIN_SCREEN;
            case "getWorldType":return arena?EnumSet.of(WorldType.PVP_ARENA):EnumSet.noneOf(WorldType.class);
            case "getRevision":return 241;
            case "getVarbitValue":return (int)a[0]==14022?area:(int)a[0]==14017?state:timer;
            case "getWidget":return challenge?(net.runelite.api.widgets.Widget)Proxy.newProxyInstance(net.runelite.api.widgets.Widget.class.getClassLoader(),new Class<?>[]{net.runelite.api.widgets.Widget.class},(wp,wm,wa)->false):null;
            default:throw new AssertionError(m.getName());
        }
    });
    @Test public void nativeBattleIsIndependentOfServerStateAndExcludesLobbyAndChallenge(){
        assertFalse(ArenaCombat.inBattle(client));area=1;assertFalse(ArenaCombat.inBattle(client));
        timer=10;assertTrue(ArenaCombat.inBattle(client));timer=0;state=1;assertTrue(ArenaCombat.inBattle(client));
        arena=false;assertFalse(ArenaCombat.inBattle(client));arena=true;loggedIn=false;assertFalse(ArenaCombat.inBattle(client));
    }
    @Test public void challengeScreenOverridesStaleBattleFlagsSoArrivalAndKitChecksCanRun(){
        area=1;state=1;timer=900;challenge=true;assertFalse(ArenaCombat.inBattle(client));
        Models.Status status=MatchupTest.status();status.match.state="preparing";AutoReady ready=new AutoReady();
        assertFalse(ready.take(status,true,false,558,true,true,99,0,0));
        assertTrue(!ArenaCombat.inBattle(client) && ready.take(status,true,false,558,true,true,99,1200,0));
        challenge=false;assertTrue(ArenaCombat.inBattle(client));
    }
}
