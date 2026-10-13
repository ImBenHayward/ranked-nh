package com.rankednh;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class RankSpinImagesTest {
    @Test public void decodingIsDeferredAndStaleLoadsCannotReplaceTheCurrentRank(){
        List<Runnable> work=new ArrayList<>();AtomicInteger loads=new AtomicInteger();
        BufferedImage[] frames=frames();
        frames[0].setRGB(0,0,0xff123456);frames[47].setRGB(0,0,0xffabcdef);
        RankSpinImages spins=new RankSpinImages(work::add,badge->{loads.incrementAndGet();return frames;});
        spins.prepare("diamond_i");spins.prepare("diamond_i");assertEquals(1,work.size());
        assertNull(spins.frame("diamond_i",.5));assertEquals(0,loads.get());
        spins.prepare("onyx_iii");work.get(0).run();assertEquals(0,loads.get());
        work.get(1).run();assertEquals(1,loads.get());assertNull(spins.frame("diamond_i",.5));
        assertEquals(0xff123456,spins.frame("onyx_iii",0).getRGB(0,0));
        assertEquals(0xffabcdef,spins.frame("onyx_iii",1).getRGB(0,0));
        spins.clear();assertNull(spins.frame("onyx_iii",.5));
    }
    @Test public void missingOrMalformedFramesFallBackWithoutRetryingEveryPaint(){
        AtomicInteger loads=new AtomicInteger();
        RankSpinImages spins=new RankSpinImages(Runnable::run,badge->{loads.incrementAndGet();return new BufferedImage[]{new BufferedImage(1,1,2)};});
        for(int i=0;i<100;i++){spins.prepare("diamond_i");assertNull(spins.frame("diamond_i",.4));}
        assertEquals(1,loads.get());spins.prepare("../../other");assertEquals(1,loads.get());
        assertFalse(RankSpinImages.supported("unranked"));
    }
    @Test public void everyRankHas48SmallTransparentFramesAndNoOversizedSheet()throws Exception{
        String[] gems={"opal","jade","red_topaz","sapphire","emerald","ruby","diamond","dragonstone","onyx","zenyte"};
        for(String gem:gems)for(String tier:new String[]{"iii","ii","i"}){
            String badge=gem+"_"+tier;BufferedImage[] frames=RankSpinImages.load(badge);
            assertNotNull(badge,frames);assertEquals(48,frames.length);
            assertNull(RankSpinImages.class.getResource("spins/"+badge+".png"));
            for(BufferedImage frame:frames){
                assertNotNull(badge,frame);assertEquals(192,frame.getWidth());assertEquals(192,frame.getHeight());
                assertTrue(frame.getColorModel().hasAlpha());assertEquals(0,frame.getRGB(0,0)>>>24);
                assertTrue(frame.getWidth()*frame.getHeight()*4<=147456);
                frame.flush();
            }
        }
    }
    @Test public void unavailableRankFallsBackWithoutRetryingUntilCleared(){
        AtomicInteger loads=new AtomicInteger();
        RankSpinImages spins=new RankSpinImages(Runnable::run,badge->{loads.incrementAndGet();throw new java.io.IOException("missing");});
        spins.prepare("jade_i");spins.prepare("jade_i");assertNull(spins.frame("jade_i",0));assertEquals(1,loads.get());
        spins.clear();spins.prepare("jade_i");assertEquals(2,loads.get());
    }
    private static BufferedImage[] frames(){
        BufferedImage[] frames=new BufferedImage[48];
        for(int i=0;i<frames.length;i++)frames[i]=new BufferedImage(192,192,BufferedImage.TYPE_INT_ARGB);
        return frames;
    }
}
