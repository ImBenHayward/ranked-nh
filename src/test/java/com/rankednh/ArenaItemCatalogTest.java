package com.rankednh;

import java.lang.reflect.Proxy;
import java.util.Set;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaItemCatalogTest {
    private static class Cell {
        int y,id,height=96,scroll,viewport=32;
        boolean hidden;
        Widget[] children;
        final Widget widget=(Widget)Proxy.newProxyInstance(Widget.class.getClassLoader(),new Class[]{Widget.class},(p,m,a)->{
            switch(m.getName()){
                case "getOriginalY":return y;
                case "setOriginalY":y=(int)a[0];return null;
                case "getItemId":return id;
                case "isSelfHidden":case "isHidden":return hidden;
                case "setHidden":hidden=(boolean)a[0];return null;
                case "getDynamicChildren":return children;
                case "getScrollHeight":return height;
                case "setScrollHeight":height=(int)a[0];return null;
                case "getScrollY":return scroll;
                case "setScrollY":scroll=(int)a[0];return null;
                case "getHeight":return viewport;
                case "revalidate":case "revalidateScroll":return null;
                default:throw new AssertionError("Unexpected widget operation: "+m.getName());
            }
        });
        Cell(int y,int id){this.y=y;this.id=id;}
    }
    private final ArenaItemCatalog catalog=new ArenaItemCatalog(null,TestArenaKit.DEFAULT);
    private final Cell first=new Cell(0,1),excluded=new Cell(32,2),last=new Cell(64,3),hotspot=new Cell(64,-1),list=new Cell(0,-1);
    private void apply(){list.children=new Widget[]{first.widget,excluded.widget,last.widget,hotspot.widget};catalog.apply(list.widget,Set.of(1,3));}
    @Test public void packsEntireAllowedRowsAndRestoresAllNativePositionsAndScroll(){
        list.scroll=40;apply();
        assertFalse(first.hidden);assertTrue(excluded.hidden);assertEquals(32,last.y);assertEquals(32,hotspot.y);
        assertFalse(hotspot.hidden);assertEquals(64,list.height);assertEquals(32,list.scroll);
        catalog.apply(list.widget,Set.of(1,3));assertEquals(32,last.y);
        catalog.clear();assertFalse(excluded.hidden);assertEquals(64,last.y);assertEquals(64,hotspot.y);
        assertEquals(96,list.height);assertEquals(40,list.scroll);
    }
    @Test public void disablingOrLeavingSetupRestoresTheCatalogue(){
        apply();catalog.enabled(false);assertFalse(excluded.hidden);assertEquals(64,last.y);
        catalog.enabled(true);apply();catalog.maintain(false,null);assertFalse(excluded.hidden);assertEquals(96,list.height);
    }
    @Test public void gameSearchResultsAreNeverUnhidden(){
        last.hidden=true;hotspot.hidden=true;apply();
        assertTrue(last.hidden);assertTrue(hotspot.hidden);assertEquals(32,list.height);
        catalog.clear();assertTrue(last.hidden);assertFalse(excluded.hidden);
    }
    @Test public void nativeRebuildWithSameHeightUsesNewChildrenAndDoesNotRestoreOldRowsOverIt(){
        apply();Cell newFirst=new Cell(0,2),newLast=new Cell(32,3);
        list.children=new Widget[]{newFirst.widget,newLast.widget};list.height=64;
        catalog.apply(list.widget,Set.of(1,3));assertTrue(newFirst.hidden);assertEquals(0,newLast.y);
        catalog.clear();assertFalse(newFirst.hidden);assertEquals(32,newLast.y);assertEquals(64,list.height);
    }
    @Test public void changedNativeItemOrPositionIsDetectedEvenWithoutAScrollHeightChange(){
        apply();first.id=2;first.y=0;first.hidden=false;excluded.y=32;excluded.hidden=false;last.y=64;hotspot.y=64;
        catalog.apply(list.widget,Set.of(1,3));assertTrue(first.hidden);assertTrue(excluded.hidden);assertEquals(0,last.y);
        catalog.clear();assertFalse(first.hidden);assertEquals(64,last.y);
    }
    @Test public void unknownCatalogueLayoutIsLeftAlone(){
        excluded.id=-1;list.children=new Widget[]{excluded.widget};catalog.apply(list.widget,Set.of(1,3));
        assertFalse(excluded.hidden);assertEquals(96,list.height);
        excluded.y=-1;list.children=new Widget[]{first.widget,excluded.widget};catalog.apply(list.widget,Set.of(1,3));assertFalse(excluded.hidden);
    }
    @Test public void searchForOnlyForbiddenItemsShowsAnEmptyResultAndCanBeRestored(){
        list.children=new Widget[]{excluded.widget};catalog.apply(list.widget,Set.of(1,3));
        assertTrue(excluded.hidden);assertEquals(0,list.height);assertFalse(catalog.notice().isEmpty());
        catalog.clear();assertFalse(excluded.hidden);assertEquals(96,list.height);assertTrue(catalog.notice().isEmpty());
    }
    @Test public void reachingTheLimitHidesARowAndRemovingTheItemBringsItBack(){
        apply();catalog.apply(list.widget,Set.of(3));
        assertTrue(first.hidden);assertTrue(excluded.hidden);assertEquals(0,last.y);assertEquals(32,list.height);
        catalog.apply(list.widget,Set.of(1,3));
        assertFalse(first.hidden);assertTrue(excluded.hidden);assertEquals(32,last.y);
        catalog.apply(list.widget,Set.of());assertTrue(first.hidden);assertTrue(last.hidden);assertEquals(0,list.height);
        catalog.clear();assertFalse(first.hidden);assertFalse(excluded.hidden);assertEquals(64,last.y);
    }
    @Test public void completeKitHasNoAdditionsAndOmissionsAreOptional(){
        ArenaKit base=TestArenaKit.DEFAULT.kit();assertTrue(catalog.optional(base).isEmpty());
        java.util.List<ArenaKit.Slot> bag=new java.util.ArrayList<>(base.inventory);bag.set(21,new ArenaKit.Slot(-1,0,"Empty"));
        ArenaKit less=new ArenaKit(241,0,1,0,base.equipment,bag);
        assertTrue(TestArenaKit.DEFAULT.matches(less));assertEquals(Integer.valueOf(1),catalog.optional(less).get(11283));
    }
}
