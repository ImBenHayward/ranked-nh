package com.rankednh;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaWorldGuideTest {
    private final Models.Status status=MatchupTest.status();
    private final List<String> calls=new ArrayList<>();
    private boolean accountMatches=true;
    private long now;
    private int currentWorld=301;
    private boolean pickerOpen,canOpen=true;
    private final World target=(World)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{World.class},(p,m,a)->null);
    private final Widget picker=(Widget)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Widget.class},(p,m,a)->null);
    private final Client client=(Client)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Client.class},(p,m,a)->{
        switch(m.getName()){
            case "getGameState":return GameState.LOGGED_IN;
            case "getWorld":return currentWorld;
            case "getWidget":return pickerOpen?picker:null;
            case "openWorldHopper":calls.add("open picker");pickerOpen=canOpen;return null;
            case "hopToWorld":assertSame(target,a[0]);calls.add("hop");return null;
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
        set(guide,"round",1);set(guide,"world",558);set(guide,"target",target);
        return guide;
    }
    @Test public void clickedHopOpensThePickerAndSubmitsOnlyOnce()throws Exception{
        ArenaWorldGuide guide=resolved();guide.tick();guide.tick();guide.tick();
        assertEquals(List.of("open picker","hop"),calls);assertTrue(guide.busy());
        currentWorld=558;guide.tick();assertFalse(guide.busy());assertEquals(2,calls.size());
    }
    @Test public void noClickMeansNoHop()throws Exception{
        ArenaWorldGuide guide=resolved();guide.cancel();guide.tick();assertTrue(calls.isEmpty());
    }
    @Test public void failedPickerStopsAfterThreeAttempts()throws Exception{
        ArenaWorldGuide guide=resolved();canOpen=false;
        for(int i=0;i<6;i++)guide.tick();
        assertEquals(List.of("open picker","open picker","open picker"),calls);assertFalse(guide.busy());
    }
    @Test public void changedRoundCannotHop()throws Exception{
        ArenaWorldGuide guide=resolved();status.match.round=2;guide.tick();assertTrue(calls.isEmpty());assertFalse(guide.busy());
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
