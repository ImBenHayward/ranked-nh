package com.rankednh;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.Set;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseWheelListener;

/** The card captures its controls. An outside click dismisses without clicking the game. */
final class MatchReportInput extends MouseAdapter implements KeyListener,MouseWheelListener
{
    private final Runnable dismiss;
    private volatile MatchReportLayout layout;
    private volatile Point pointer=new Point(-1,-1);
    private volatile int focused,armed;
    private final Set<Integer> mouseDown=new HashSet<>(),swallowClick=new HashSet<>(),keysDown=new HashSet<>();
    private boolean swallowTyped;

    MatchReportInput(Runnable dismiss){this.dismiss=dismiss;}
    void show(MatchReportLayout next){layout=next;}
    void hide(){layout=null;focused=0;}
    boolean visible(){return layout!=null;}
    int hovered(){MatchReportLayout current=layout;return current==null?0:current.control(pointer);}
    int focused(){return focused;}
    int pressed(){return armed;}
    private boolean inside(Point point){MatchReportLayout current=layout;return current!=null && current.contains(point);}
    private void close(){hide();dismiss.run();}

    @Override public synchronized MouseEvent mousePressed(MouseEvent e){
        pointer=e.getPoint();swallowClick.remove(e.getButton());
        if(inside(pointer)){
            mouseDown.add(e.getButton());e.consume();
            if(e.getButton()==MouseEvent.BUTTON1){armed=hovered();focused=0;}
        }else if(visible()){
            mouseDown.add(e.getButton());e.consume();close();
        }
        return e;
    }
    @Override public synchronized MouseEvent mouseReleased(MouseEvent e){
        pointer=e.getPoint();boolean captured=mouseDown.remove(e.getButton());
        if(captured || inside(pointer)){e.consume();swallowClick.add(e.getButton());}
        if(e.getButton()==MouseEvent.BUTTON1){
            int control=armed;armed=0;
            if(captured && control!=0 && control==hovered())close();
        }
        return e;
    }
    @Override public synchronized MouseEvent mouseClicked(MouseEvent e){
        if(swallowClick.remove(e.getButton()) || inside(e.getPoint()))e.consume();return e;
    }
    @Override public synchronized MouseEvent mouseDragged(MouseEvent e){
        pointer=e.getPoint();if(!mouseDown.isEmpty() || inside(pointer))e.consume();return e;
    }
    @Override public MouseEvent mouseMoved(MouseEvent e){pointer=e.getPoint();if(inside(pointer))e.consume();return e;}
    @Override public MouseEvent mouseExited(MouseEvent e){pointer=new Point(-1,-1);return e;}
    @Override public MouseWheelEvent mouseWheelMoved(MouseWheelEvent e){if(inside(e.getPoint()))e.consume();return e;}

    @Override public synchronized void keyPressed(KeyEvent e){
        int code=e.getKeyCode();
        boolean control=code==KeyEvent.VK_ESCAPE || code==KeyEvent.VK_ENTER || code==KeyEvent.VK_TAB || (code==KeyEvent.VK_SPACE && focused!=0);
        if(visible() && control || keysDown.contains(code)){
            boolean first=keysDown.add(code);swallowTyped=true;e.consume();
            if(first && visible()){
                if(code==KeyEvent.VK_ESCAPE || code==KeyEvent.VK_ENTER || (code==KeyEvent.VK_SPACE && focused!=0))close();
                else if(code==KeyEvent.VK_TAB)focused=focused==2?1:2;
            }
        }
    }
    @Override public synchronized void keyReleased(KeyEvent e){
        if(keysDown.remove(e.getKeyCode()))e.consume();
        if(keysDown.isEmpty())swallowTyped=false;
    }
    @Override public synchronized void keyTyped(KeyEvent e){if(swallowTyped){e.consume();swallowTyped=false;}}
    @Override public synchronized void focusLost(){keysDown.clear();mouseDown.clear();swallowClick.clear();swallowTyped=false;armed=0;}
}
