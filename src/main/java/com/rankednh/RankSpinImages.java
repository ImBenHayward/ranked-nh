package com.rankednh;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.imageio.ImageIO;

/** One 6.75 MiB sheet at a time. Decode off the render thread; a missing sheet uses the still badge. */
final class RankSpinImages {
    static final int SIZE=192,FRAMES=48,COLUMNS=8;
    interface Loader { BufferedImage load(String badge) throws IOException; }
    private final Executor executor;
    private final Loader loader;
    private volatile Request requested;
    private static final class Request {
        final String badge;
        volatile BufferedImage sheet;
        Request(String badge){this.badge=badge;}
    }
    RankSpinImages(Executor executor){this(executor,RankSpinImages::load);}
    RankSpinImages(Executor executor,Loader loader){this.executor=executor;this.loader=loader;}
    synchronized void prepare(String badge){
        if(!supported(badge)){clear();return;}
        if(requested!=null && requested.badge.equals(badge))return;
        Request request=new Request(badge);requested=request;
        executor.execute(()->{
            if(requested!=request)return;
            try{
                BufferedImage sheet=loader.load(badge);
                if(requested==request && sheet!=null && sheet.getWidth()==SIZE*COLUMNS && sheet.getHeight()==SIZE*(FRAMES/COLUMNS))
                    request.sheet=sheet;
            }catch(IOException | RuntimeException unavailable){/* Keep the ordinary static reveal. */}
        });
    }
    BufferedImage frame(String badge,double progress){
        Request request=requested;
        if(request==null || !request.badge.equals(badge) || request.sheet==null || progress<0)return null;
        int index=(int)Math.round(Math.max(0,Math.min(1,progress))*(FRAMES-1));
        return request.sheet.getSubimage(index%COLUMNS*SIZE,index/COLUMNS*SIZE,SIZE,SIZE);
    }
    synchronized void clear(){requested=null;}
    static boolean supported(String badge){
        return badge!=null && badge.matches("(?:opal|jade|red_topaz|sapphire|emerald|ruby|diamond|dragonstone|onyx|zenyte)_(?:i|ii|iii)");
    }
    static BufferedImage load(String badge)throws IOException{
        if(!supported(badge))return null;
        try(InputStream source=RankSpinImages.class.getResourceAsStream("spins/"+badge+".png")){
            BufferedImage decoded=source==null?null:ImageIO.read(source);
            if(decoded==null)return null;
            if(decoded.getWidth()!=SIZE*COLUMNS || decoded.getHeight()!=SIZE*(FRAMES/COLUMNS))return null;
            BufferedImage sheet=new BufferedImage(decoded.getWidth(),decoded.getHeight(),BufferedImage.TYPE_INT_ARGB_PRE);
            java.awt.Graphics2D graphics=sheet.createGraphics();
            try{graphics.drawImage(decoded,0,0,null);}finally{graphics.dispose();decoded.flush();}
            return sheet;
        }
    }
}
