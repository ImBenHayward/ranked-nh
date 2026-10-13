package com.rankednh;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.imageio.ImageIO;

/** One rank's bounded frames at a time. Decode off-thread; missing frames use the still badge. */
final class RankSpinImages {
    static final int SIZE=192,FRAMES=48;
    interface Loader { BufferedImage[] load(String badge) throws IOException; }
    private final Executor executor;
    private final Loader loader;
    private volatile Request requested;
    private static final class Request {
        final String badge;
        volatile BufferedImage[] frames;
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
                BufferedImage[] frames=loader.load(badge);
                if(requested==request && valid(frames))request.frames=frames;
            }catch(IOException | RuntimeException unavailable){/* Keep the ordinary static reveal. */}
        });
    }
    BufferedImage frame(String badge,double progress){
        Request request=requested;
        if(request==null || !request.badge.equals(badge) || request.frames==null || progress<0)return null;
        int index=(int)Math.round(Math.max(0,Math.min(1,progress))*(FRAMES-1));
        return request.frames[index];
    }
    synchronized void clear(){requested=null;}
    static boolean supported(String badge){
        return badge!=null && badge.matches("(?:opal|jade|red_topaz|sapphire|emerald|ruby|diamond|dragonstone|onyx|zenyte)_(?:i|ii|iii)");
    }
    private static boolean valid(BufferedImage[] frames){
        if(frames==null || frames.length!=FRAMES)return false;
        for(BufferedImage frame:frames)if(frame==null || frame.getWidth()!=SIZE || frame.getHeight()!=SIZE)return false;
        return true;
    }
    static BufferedImage[] load(String badge)throws IOException{
        if(!supported(badge))return null;
        BufferedImage[] frames=new BufferedImage[FRAMES];
        for(int index=0;index<FRAMES;index++){
            String path=String.format(java.util.Locale.ROOT,"spins/%s/%03d.png",badge,index);
            try(InputStream source=RankSpinImages.class.getResourceAsStream(path)){
                BufferedImage decoded=source==null?null:ImageIO.read(source);
                if(decoded==null)return null;
                if(decoded.getWidth()!=SIZE || decoded.getHeight()!=SIZE){decoded.flush();return null;}
                BufferedImage frame=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB_PRE);
                java.awt.Graphics2D graphics=frame.createGraphics();
                try{graphics.drawImage(decoded,0,0,null);}finally{graphics.dispose();decoded.flush();}
                frames[index]=frame;
            }
        }
        return frames;
    }
}
