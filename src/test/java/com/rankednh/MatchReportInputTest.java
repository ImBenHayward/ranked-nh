package com.rankednh;

import java.awt.Canvas;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class MatchReportInputTest
{
    private final Canvas canvas=new Canvas();
    private MouseEvent mouse(int type,Point p){return new MouseEvent(canvas,type,0,0,p.x,p.y,1,false,MouseEvent.BUTTON1);}
    private KeyEvent key(int type,int code,char value){return new KeyEvent(canvas,type,0,0,code,value);}
    private Point at(MatchReportLayout layout,int x,int y){return new Point((int)(layout.x+x*layout.scale),(int)(layout.y+y*layout.scale));}

    @Test public void closeAndContinueWorkAtFixedAndResizableSizesWithoutClickThrough(){
        for(int[] size:new int[][]{{765,503},{1024,768},{1920,1080}}){
            for(int[] control:new int[][]{{414,24},{344,293}}){
                AtomicInteger closes=new AtomicInteger();MatchReportInput input=new MatchReportInput(closes::incrementAndGet);
                MatchReportLayout layout=new MatchReportLayout(size[0],size[1],1);input.show(layout);
                assertTrue(layout.x>=0 && layout.y>=0);
                assertTrue(layout.x+MatchReportLayout.WIDTH*layout.scale<=size[0]);
                assertTrue(layout.y+MatchReportLayout.HEIGHT*layout.scale<=size[1]);
                Point p=at(layout,control[0],control[1]);
                assertTrue(input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,p)).isConsumed());assertEquals(0,closes.get());
                assertTrue(input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,p)).isConsumed());assertEquals(1,closes.get());
                assertFalse(input.visible());assertTrue(input.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED,p)).isConsumed());
                assertFalse(input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,new Point(2,2))).isConsumed());
            }
        }
    }
    @Test public void draggingOutDoesNotActivateAControlAndOutsideClickDismissesWithoutClickThrough(){
        AtomicInteger closes=new AtomicInteger();MatchReportInput input=new MatchReportInput(closes::incrementAndGet);
        MatchReportLayout layout=new MatchReportLayout(1024,768,1);input.show(layout);
        Point outside=new Point(2,2),close=at(layout,414,24);
        input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,close));
        assertTrue(input.mouseDragged(mouse(MouseEvent.MOUSE_DRAGGED,outside)).isConsumed());
        assertTrue(input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,outside)).isConsumed());
        assertTrue(input.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED,outside)).isConsumed());
        assertEquals(0,closes.get());assertTrue(input.visible());
        assertTrue(input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,outside)).isConsumed());
        assertTrue(input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,outside)).isConsumed());
        assertTrue(input.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED,outside)).isConsumed());
        assertEquals(1,closes.get());assertFalse(input.visible());
    }
    @Test public void escapeConsumesPressRepeatTypedAndReleaseThenRestoresInput(){
        AtomicInteger closes=new AtomicInteger();MatchReportInput input=new MatchReportInput(closes::incrementAndGet);
        input.show(new MatchReportLayout(765,503,1));
        KeyEvent press=key(KeyEvent.KEY_PRESSED,KeyEvent.VK_ESCAPE,(char)27);input.keyPressed(press);
        assertTrue(press.isConsumed());assertEquals(1,closes.get());assertFalse(input.visible());
        KeyEvent repeat=key(KeyEvent.KEY_PRESSED,KeyEvent.VK_ESCAPE,(char)27);input.keyPressed(repeat);
        assertTrue(repeat.isConsumed());assertEquals(1,closes.get());
        KeyEvent typed=key(KeyEvent.KEY_TYPED,KeyEvent.VK_UNDEFINED,(char)27);input.keyTyped(typed);assertTrue(typed.isConsumed());
        KeyEvent release=key(KeyEvent.KEY_RELEASED,KeyEvent.VK_ESCAPE,(char)27);input.keyReleased(release);assertTrue(release.isConsumed());
        KeyEvent next=key(KeyEvent.KEY_PRESSED,KeyEvent.VK_ESCAPE,(char)27);input.keyPressed(next);assertFalse(next.isConsumed());
    }
    @Test public void hidingMidGestureStillConsumesItsReleaseAndClick(){
        MatchReportInput input=new MatchReportInput(()->{});MatchReportLayout layout=new MatchReportLayout(765,503,1);input.show(layout);Point p=at(layout,100,100);
        input.mousePressed(mouse(MouseEvent.MOUSE_PRESSED,p));input.hide();
        assertTrue(input.mouseReleased(mouse(MouseEvent.MOUSE_RELEASED,p)).isConsumed());
        assertTrue(input.mouseClicked(mouse(MouseEvent.MOUSE_CLICKED,p)).isConsumed());
    }
    @Test public void keyboardFocusAndEnterCanDismissWithoutAMouse(){
        AtomicInteger closes=new AtomicInteger();MatchReportInput input=new MatchReportInput(closes::incrementAndGet);
        input.show(new MatchReportLayout(765,503,1));
        input.keyPressed(key(KeyEvent.KEY_PRESSED,KeyEvent.VK_TAB,'\t'));assertEquals(2,input.focused());
        input.keyReleased(key(KeyEvent.KEY_RELEASED,KeyEvent.VK_TAB,'\t'));
        input.keyPressed(key(KeyEvent.KEY_PRESSED,KeyEvent.VK_TAB,'\t'));assertEquals(1,input.focused());
        input.keyPressed(key(KeyEvent.KEY_PRESSED,KeyEvent.VK_ENTER,'\n'));assertEquals(1,closes.get());
    }
    @Test public void wheelOnlyBlocksInsideTheVisibleCardAndNormalTypingPassesThrough(){
        MatchReportInput input=new MatchReportInput(()->{});MatchReportLayout layout=new MatchReportLayout(765,503,1);input.show(layout);
        Point p=at(layout,100,100);
        MouseWheelEvent wheel=new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,0,0,p.x,p.y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,1);
        assertTrue(input.mouseWheelMoved(wheel).isConsumed());
        wheel=new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,0,0,10,10,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,1);
        assertFalse(input.mouseWheelMoved(wheel).isConsumed());
        KeyEvent letter=key(KeyEvent.KEY_PRESSED,KeyEvent.VK_A,'a');input.keyPressed(letter);assertFalse(letter.isConsumed());
        KeyEvent typed=key(KeyEvent.KEY_TYPED,KeyEvent.VK_UNDEFINED,'a');input.keyTyped(typed);assertFalse(typed.isConsumed());
        wheel=new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,0,0,p.x,p.y,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,1);
        assertTrue(input.mouseWheelMoved(wheel).isConsumed());input.hide();
        wheel=new MouseWheelEvent(canvas,MouseEvent.MOUSE_WHEEL,0,0,10,10,0,false,MouseWheelEvent.WHEEL_UNIT_SCROLL,1,1);
        assertFalse(input.mouseWheelMoved(wheel).isConsumed());
    }
    @Test public void cardCentersWithinTheViewportInsteadOfTheChatAndInventoryCanvas(){
        java.awt.Rectangle viewport=new java.awt.Rectangle(4,4,512,334);
        MatchReportLayout layout=new MatchReportLayout(viewport,1);
        assertEquals(viewport.getCenterX(),layout.x+MatchReportLayout.WIDTH*layout.scale/2,0.01);
        assertEquals(viewport.getCenterY(),layout.y+MatchReportLayout.HEIGHT*layout.scale/2,0.01);
        assertTrue(layout.y>=viewport.y+20);
        assertTrue(layout.y+MatchReportLayout.HEIGHT*layout.scale<=viewport.y+viewport.height-20);
        assertFalse(layout.contains(new Point(600,200)));assertFalse(layout.contains(new Point(200,400)));
        assertEquals(1,new MatchReportLayout(1920,1080,1).scale,0);
    }
}
