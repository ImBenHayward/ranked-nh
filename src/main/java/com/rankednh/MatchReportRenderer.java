package com.rankednh;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Used unchanged in the game and in off-screen preview frames. */
final class MatchReportRenderer
{
    private static final int WIDTH=MatchReportLayout.WIDTH,HEIGHT=MatchReportLayout.HEIGHT;
    private static final Color BG=new Color(27,30,34),LINE=new Color(62,66,73),TEXT=new Color(235,230,220),
        MUTED=new Color(174,178,181),ACCENT=RankedNhPanel.ACCENT,WIN=new Color(151,204,157),LOSS=new Color(231,155,136);

    static void render(Graphics2D graphics,MatchReportView view,MatchReportAnimation.Frame frame,
        MatchReportLayout layout,int hovered,int focused,int pressed) {
        render(graphics,view,frame,layout,hovered,focused,pressed,null);
    }
    static void render(Graphics2D graphics,MatchReportView view,MatchReportAnimation.Frame frame,
        MatchReportLayout layout,int hovered,int focused,int pressed,java.awt.image.BufferedImage spinImage) {
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setComposite(AlphaComposite.SrcOver.derive((float)frame.entrance));
            g.translate(layout.x,layout.y);g.scale(layout.scale,layout.scale);
            if(view.placementReveal){
                placement(g,view,frame,hovered,focused,pressed,spinImage);return;
            }
            if(view.promotion()){
                promotion(g,view,frame,hovered,focused,pressed,spinImage);return;
            }
            g.setColor(new Color(10,12,15,70));g.fillRoundRect(-4,4,WIDTH+8,HEIGHT+4,8,8);
            g.setColor(BG);g.fillRect(0,0,WIDTH,HEIGHT);g.setColor(LINE);g.drawRect(0,0,WIDTH-1,HEIGHT-1);
            Color accent=view.decided?(view.won?WIN:LOSS):ACCENT;
            g.setColor(accent);g.fillRect(0,0,WIDTH,3);
            text(g,view.heading,24,26,11,MUTED,false);
            control(g,MatchReportLayout.CLOSE,1,hovered,focused,pressed,false);
            g.setColor(TEXT);g.setStroke(new BasicStroke(2));
            g.drawLine(410,20,418,28);g.drawLine(418,20,410,28);
            text(g,frame.title,24,60,28,accent,true);
            right(g,view.score,WIDTH-24,60,26,TEXT,true);
            fitted(g,view.opponent,24,82,WIDTH-48,12,MUTED,false);
            divider(g,96);

            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(BadgeImages.image(frame.badge),24,110,80,80,null);
            fitted(g,frame.rank,120,119,WIDTH-144,18,TEXT,true);
            text(g,frame.rating==null?"Match status":"Series rating",120,136,10,MUTED,false);
            if(frame.rating!=null) {
                text(g,Integer.toString(frame.rating),120,170,36,TEXT,true);
                right(g,view.delta,WIDTH-24,166,22,view.delta.startsWith("-")?LOSS:WIN,true);
                text(g,view.ratingBefore+" → "+view.ratingAfter,120,189,11,MUTED,false);
            } else {
                fitted(g,view.rating,120,169,WIDTH-144,19,TEXT,true);
            }

            fitted(g,frame.progressLabel,24,213,WIDTH-48,13,TEXT,true);
            progressBar(g,225,6,frame.progress);
            fitted(g,frame.progressDetail,24,248,WIDTH-48,11,MUTED,false);
            fitted(g,view.note,24,267,WIDTH-48,10,MUTED,false);

            control(g,MatchReportLayout.CONTINUE,2,hovered,focused,pressed,true);
            centered(g,"Continue",344,298,12,ACCENT,true);
            text(g,"Esc to close",24,298,11,MUTED,false);
        } finally {g.dispose();}
    }
    private static void placement(Graphics2D g,MatchReportView view,MatchReportAnimation.Frame frame,int hovered,int focused,int pressed,java.awt.image.BufferedImage spinImage){
        g.setColor(BG);g.fillRect(0,0,WIDTH,HEIGHT);g.setColor(LINE);g.drawRect(0,0,WIDTH-1,HEIGHT-1);
        centered(g,"Placements complete",WIDTH/2,35,12,MUTED,false);
        control(g,MatchReportLayout.CLOSE,1,hovered,focused,pressed,false);
        g.setColor(TEXT);g.setStroke(new BasicStroke(2));g.drawLine(410,20,418,28);g.drawLine(418,20,410,28);
        badge(g,frame,spinImage,184,128);
        Graphics2D caption=(Graphics2D)g.create();
        try{
            caption.setComposite(AlphaComposite.SrcOver.derive((float)(frame.entrance*frame.rankAlpha)));
            centered(caption,frame.rank,WIDTH/2,234,28,TEXT,true);
            caption.setComposite(AlphaComposite.SrcOver.derive((float)(frame.entrance*frame.ratingAlpha)));
            if(frame.rating!=null)centered(caption,String.format(java.util.Locale.ROOT,"%,d rating",frame.rating),WIDTH/2,257,14,MUTED,false);
        }finally{caption.dispose();}
        neutralContinue(g,hovered,focused,pressed);
        if(view.preview)text(g,"Preview · Rating unchanged",24,298,10,MUTED,false);
    }
    private static void promotion(Graphics2D g,MatchReportView view,MatchReportAnimation.Frame frame,int hovered,int focused,int pressed,java.awt.image.BufferedImage spinImage){
        Color gain=WIN;
        g.setColor(BG);g.fillRect(0,0,WIDTH,HEIGHT);g.setColor(LINE);g.drawRect(0,0,WIDTH-1,HEIGHT-1);
        text(g,view.preview?"Preview · Rank promotion":"Ranked NH · Rank promotion",24,26,10,MUTED,false);
        fitted(g,(view.won?"Victory":"Series complete")+" · "+view.score,24,48,128,12,TEXT,true);
        control(g,MatchReportLayout.CLOSE,1,hovered,focused,pressed,false);
        g.setColor(TEXT);g.setStroke(new BasicStroke(2));g.drawLine(410,20,418,28);g.drawLine(418,20,410,28);

        badge(g,frame,spinImage,160,106);
        Graphics2D caption=(Graphics2D)g.create();
        try{
            caption.setComposite(AlphaComposite.SrcOver.derive((float)(frame.entrance*frame.rankAlpha)));
            centered(caption,frame.promoted?"Promoted to":"Next rank · "+view.rank,WIDTH/2,185,11,MUTED,false);
            centered(caption,frame.rank,WIDTH/2,214,28,TEXT,true);
            caption.setComposite(AlphaComposite.SrcOver.derive((float)(frame.entrance*frame.ratingAlpha)));
            font(caption,22,true);String number=String.format(java.util.Locale.ROOT,"%,d",frame.rating);int numberWidth=caption.getFontMetrics().stringWidth(number);
            font(caption,13,true);int deltaWidth=caption.getFontMetrics().stringWidth(view.delta);int x=(WIDTH-numberWidth-deltaWidth-12)/2;
            text(caption,number,x,241,22,TEXT,true);text(caption,view.delta,x+numberWidth+12,239,13,gain,true);
        }finally{caption.dispose();}
        Graphics2D progress=(Graphics2D)g.create();
        try{
            progress.setComposite(AlphaComposite.SrcOver.derive((float)(frame.entrance*frame.ratingAlpha)));
            fitted(progress,frame.progressLabel,24,260,WIDTH-48,11,MUTED,false);
            progressBar(progress,268,4,frame.progress);
        }finally{progress.dispose();}
        neutralContinue(g,hovered,focused,pressed);
        text(g,view.preview?"Sample · Rating unchanged":"Esc to close",24,298,10,MUTED,false);
    }
    private static void badge(Graphics2D g,MatchReportAnimation.Frame frame,java.awt.image.BufferedImage spinImage,int baseSize,int centerY){
        Graphics2D hero=(Graphics2D)g.create();
        try{
            hero.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int size=(int)Math.round(baseSize*frame.badgeScale),x=(WIDTH-size)/2,y=centerY-size/2;
            float alpha=(float)(frame.entrance*frame.badgeAlpha);
            if(spinImage!=null && frame.stillMix<1){
                hero.setComposite(AlphaComposite.SrcOver.derive(alpha));
                hero.drawImage(spinImage,x,y,size,size,null);
                if(frame.stillMix<=0)return;
                alpha*=frame.stillMix;
            }
            hero.setComposite(AlphaComposite.SrcOver.derive(alpha));
            hero.drawImage(BadgeImages.image(frame.badge),x,y,size,size,null);
        }finally{hero.dispose();}
    }
    private static void neutralContinue(Graphics2D g,int hovered,int focused,int pressed){
        Rectangle button=MatchReportLayout.CONTINUE;
        g.setColor(pressed==2?new Color(37,41,47):hovered==2?new Color(59,64,72):new Color(43,47,54));g.fill(button);
        if(focused==2){g.setColor(TEXT);g.setStroke(new BasicStroke(2));g.drawRect(button.x-4,button.y-4,button.width+8,button.height+8);}
        centered(g,"Continue",344,298,12,TEXT,true);
    }
    private static void progressBar(Graphics2D g,int y,int height,double progress){
        int width=WIDTH-48,fill=(int)Math.round(width*Math.max(0,Math.min(1,progress)));
        g.setColor(LINE);g.fillRect(24,y,width,height);
        // Keep the advancing tip light green, including when the next bracket is only partly filled.
        g.setPaint(new java.awt.GradientPaint(24,y,new Color(71,151,94),24+Math.max(1,fill),y,new Color(164,221,174)));
        g.fillRect(24,y,fill,height);
    }
    private static void control(Graphics2D g,Rectangle r,int id,int hovered,int focused,int pressed,boolean primary) {
        Color fill=primary?new Color(49,69,58):BG;
        if(hovered==id)fill=primary?new Color(61,84,71):LINE;
        if(pressed==id)fill=primary?new Color(39,57,47):new Color(78,82,90);
        g.setColor(fill);g.fill(r);
        if(focused==id){g.setStroke(new BasicStroke(2));g.setColor(TEXT);g.drawRect(r.x-4,r.y-4,r.width+8,r.height+8);}
    }
    private static void divider(Graphics2D g,int y){g.setStroke(new BasicStroke(1));g.setColor(LINE);g.drawLine(24,y,WIDTH-24,y);}
    private static void font(Graphics2D g,int size,boolean bold){g.setFont(new Font(Font.SANS_SERIF,bold?Font.BOLD:Font.PLAIN,size));}
    private static void text(Graphics2D g,String value,int x,int y,int size,Color color,boolean bold){font(g,size,bold);g.setColor(color);g.drawString(value,x,y);}
    private static void right(Graphics2D g,String value,int x,int y,int size,Color color,boolean bold){font(g,size,bold);g.setColor(color);g.drawString(value,x-g.getFontMetrics().stringWidth(value),y);}
    private static void centered(Graphics2D g,String value,int x,int y,int size,Color color,boolean bold){font(g,size,bold);g.setColor(color);g.drawString(value,x-g.getFontMetrics().stringWidth(value)/2,y);}
    private static void fitted(Graphics2D g,String value,int x,int y,int width,int size,Color color,boolean bold){
        font(g,size,bold);g.setColor(color);FontMetrics fm=g.getFontMetrics();
        if(fm.stringWidth(value)>width){while(!value.isEmpty() && fm.stringWidth(value+"…")>width)value=value.substring(0,value.length()-1);value+="…";}
        g.drawString(value,x,y);
    }
    private MatchReportRenderer(){}
}
