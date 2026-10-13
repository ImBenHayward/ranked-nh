package com.rankednh;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaSetupWarningOverlayTest {
    private boolean matches=true,open=true;
    private GameState state=GameState.LOGGED_IN;
    private ArenaSetup.View view=new ArenaSetup.View("Fix your kit","","",true,null);
    private final Map<Integer,Widget> widgets=new HashMap<>();
    private final RankedNhPlugin plugin=new RankedNhPlugin(){
        @Override boolean accountMatches(){return matches;}
        @Override ArenaSetup.View setupView(){return view;}
    };
    private Widget widget(Rectangle bounds,String text,Widget... children){
        return (Widget)Proxy.newProxyInstance(Widget.class.getClassLoader(),new Class<?>[]{Widget.class},(p,m,a)->{
            switch(m.getName()){
                case "isHidden":return !open;
                case "getBounds":return bounds;
                case "getText":return text;
                case "getDynamicChildren":return children;
                case "getStaticChildren":return null;
                default:throw new AssertionError(m.getName());
            }
        });
    }
    private void put(int group,int child,Rectangle bounds,Widget... children){widgets.put(group<<16|child,widget(bounds,"",children));}
    private final Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(proxy,method,args)->{
        if(method.getName().equals("getGameState"))return state;
        if(method.getName().equals("getWidget"))return widgets.get((int)args[0]<<16|(int)args[1]);
        throw new AssertionError(method.getName());
    });
    private BufferedImage render(){
        BufferedImage image=new BufferedImage(500,330,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
        try{assertNull(new ArenaSetupWarningOverlay(client,plugin).render(g));}finally{g.dispose();}return image;
    }
    private int painted(BufferedImage image){int count=0;for(int x=0;x<image.getWidth();x++)for(int y=0;y<image.getHeight();y++)if(image.getRGB(x,y)!=0)count++;return count;}
    private ArenaKit.Change body(boolean opponent){return new ArenaKit.Change("Body","Empty","Virtus robe top",4,-1,opponent?"Opponent":"Yours","Default",opponent);}
    @Test public void ownKitTabShowsItsIncorrectWornSlotEvenWhenConfirmIsAbsent(){
        put(757,0,new Rectangle(0,0,500,330));put(757,37,new Rectangle(80,120,32,32));put(757,133,new Rectangle(350,120,32,32));view.changes=List.of(body(false));
        BufferedImage image=render();assertNotEquals(0,image.getRGB(79,119));assertEquals(0,image.getRGB(95,135));
        assertEquals(0,image.getRGB(349,119));assertTrue(view.changes.get(0).explanation().contains("Default: Virtus robe top"));
    }
    @Test public void nextStepHighlightsTheFinalTabThenConfirmWithoutPaintingOverItems(){
        put(757,0,new Rectangle(0,0,500,330));
        widgets.put(757<<16|5,widget(new Rectangle(180,40,100,20),"Opponent's kit"));
        widgets.put(757<<16|143,widget(new Rectangle(330,240,100,32),"<col=0dc10d>Confirm</col>"));
        view=new ArenaSetup.View("Your kit is ready","","",false,null).guide(ArenaSetup.Target.OPPONENT_TAB);
        BufferedImage image=render();assertTrue(painted(image)>0);assertEquals(0,image.getRGB(350,237));
        view=new ArenaSetup.View("Default kits ready","","",false,null).guide(ArenaSetup.Target.KIT_CONFIRM);
        image=render();assertEquals(ArenaSetupWarningOverlay.NEXT.getRGB(),image.getRGB(350,237));assertEquals(0,image.getRGB(200,37));
        view=new ArenaSetup.View("Warning","","",true,null).guide(ArenaSetup.Target.KIT_CONFIRM);
        assertEquals(0,painted(render()));
        view=new ArenaSetup.View("Waiting","","",false,null);assertEquals(0,painted(render()));
    }
    @Test public void suppliesTabIsHighlightedBeforeTheItemPanelIsOpen(){
        put(758,0,new Rectangle(0,0,500,330));
        widgets.put(758<<16|5,widget(new Rectangle(180,40,100,20),"Max/Med kit"));
        view=new ArenaSetup.View("Open Max/Med kit","","",false,null).guide(ArenaSetup.Target.MAX_MED_TAB);
        assertTrue(painted(render())>0);
    }
    @Test public void wrongBuildHighlightsMaxMedAndNeverConfirm(){
        put(757,0,new Rectangle(0,0,500,330));put(757,22,new Rectangle(330,240,100,32));
        Widget maxMed=widget(new Rectangle(80,90,76,18),"<col=ff9040>Max/Med</col>");
        put(757,10,new Rectangle(20,70,460,45),maxMed,widget(new Rectangle(210,90,76,18),"Zerker"));
        view=new ArenaSetup.View("Choose Max/Med","Select Max/Med in Stats.","",true,null);
        BufferedImage image=render();assertEquals(ArenaSetupWarningOverlay.WARNING.getRGB(),image.getRGB(77,87));
        assertEquals(0,image.getRGB(327,237));assertEquals(0,image.getRGB(350,252));
    }
    @Test public void inventoryOpponentAndSpellbookUseTheVisibleTabsActualSlots(){
        put(757,0,new Rectangle(0,0,500,330));put(757,133,new Rectangle(350,120,32,32));
        Widget slot=widget(new Rectangle(200,120,32,32),"");put(757,140,new Rectangle(200,120,150,224),slot);
        put(757,121,new Rectangle(280,80,140,20));
        view.changes=List.of(body(true),new ArenaKit.Change("Inventory slot 1","Empty","Anglerfish",-1,0,"Opponent","Default",true),new ArenaKit.Change("Spellbook","Standard","Ancient Magicks",-1,-1,"Opponent","Default",true));
        BufferedImage image=render();assertNotEquals(0,image.getRGB(349,119));assertNotEquals(0,image.getRGB(199,119));assertNotEquals(0,image.getRGB(279,79));
        widgets.clear();put(757,0,new Rectangle(0,0,500,330));put(757,25,new Rectangle(280,80,140,20));
        view.changes=List.of(new ArenaKit.Change("Spellbook","Standard","Ancient Magicks",-1,-1,"Yours","Default",false));
        assertNotEquals(0,render().getRGB(279,79));
    }
    @Test public void suppliesScreenDoesNotDependOnAnyDuelControls(){
        put(758,9,new Rectangle(0,0,500,330));put(758,22,new Rectangle(80,120,32,32));view.changes=List.of(body(false));
        assertNotEquals(0,render().getRGB(79,119));
    }
    @Test public void denseWarningsLeaveTheItemsVisibleAndVanishWithTheScreen(){
        put(757,0,new Rectangle(0,0,500,330));Widget[] inventory=new Widget[28];java.util.ArrayList<ArenaKit.Change> changes=new java.util.ArrayList<>();
        for(int i=0;i<28;i++){inventory[i]=widget(new Rectangle(30+i%4*38,35+i/4*36,32,32),"");changes.add(new ArenaKit.Change("Slot "+i,"Empty","Anglerfish",-1,i,"Yours","Default",false));}
        put(757,44,new Rectangle(30,35,160,260),inventory);view.changes=changes;
        BufferedImage image=render();assertTrue(painted(image)>0);assertTrue(painted(image)<28*32*32/3);
        for(int i=0;i<28;i++)assertEquals(0,image.getRGB(46+i%4*38,51+i/4*36));
        open=false;assertEquals(0,painted(render()));open=true;matches=false;assertEquals(0,painted(render()));matches=true;
        state=GameState.LOGIN_SCREEN;assertEquals(0,painted(render()));state=GameState.LOGGED_IN;
        view=null;assertEquals(0,painted(render()));
    }
}
