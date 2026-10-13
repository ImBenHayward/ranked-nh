package com.rankednh;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaWorldGuideTest {
    private final Models.Status status=MatchupTest.status();
    private final List<String> calls=new ArrayList<>();
    private boolean accountMatches=true;
    private long now;
    private final Client client=(Client)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Client.class},(p,m,a)->{
        switch(m.getName()){
            case "getGameState":return GameState.LOGGED_IN;
            case "getWorld":return 301;
            case "openWorldHopper":calls.add("open picker");return null;
            default:throw new AssertionError("Unexpected client action: "+m.getName());
        }
    });
    private ArenaWorldGuide resolved()throws Exception{
        status.match.state="preparing";
        RankedNhPlugin plugin=new RankedNhPlugin(){
            @Override Models.Status status(){return status;}
            @Override boolean accountMatches(){return accountMatches;}
            @Override long serverNow(){return now;}
            @Override void handoffProblem(String message){}
        };
        ArenaWorldGuide guide=new ArenaWorldGuide(client,null,null,null,plugin);
        set(guide,"busy",true);set(guide,"started",System.nanoTime());set(guide,"matchId",status.match.id);
        set(guide,"round",1);set(guide,"world",558);set(guide,"readyToOpen",true);
        return guide;
    }
    @Test public void resolvedWorldOnlyOpensThePickerOnce()throws Exception{
        ArenaWorldGuide guide=resolved();guide.tick();guide.tick();
        assertEquals(List.of("open picker"),calls);assertFalse(guide.busy());assertEquals(301,client.getWorld());
    }
    @Test public void staleMatchCannotOpenThePicker()throws Exception{
        ArenaWorldGuide guide=resolved();status.match.id="different";guide.tick();
        assertTrue(calls.isEmpty());assertFalse(guide.busy());
    }
    @Test public void expiredOfferOrChangedAccountCancelsThePendingPicker()throws Exception{
        ArenaWorldGuide guide=resolved();now=60_000;guide.tick();assertTrue(calls.isEmpty());assertFalse(guide.busy());
        now=0;guide=resolved();accountMatches=false;guide.tick();assertTrue(calls.isEmpty());assertFalse(guide.busy());
    }
    private static void set(ArenaWorldGuide guide,String name,Object value)throws Exception{
        Field field=ArenaWorldGuide.class.getDeclaredField(name);field.setAccessible(true);field.set(guide,value);
    }
}
