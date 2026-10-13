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
        BufferedImage sheet=new BufferedImage(1536,1152,BufferedImage.TYPE_INT_ARGB);
        sheet.setRGB(0,0,0xff123456);sheet.setRGB(1344,960,0xffabcdef);
        RankSpinImages spins=new RankSpinImages(work::add,badge->{loads.incrementAndGet();return sheet;});
        spins.prepare("diamond_i");spins.prepare("diamond_i");assertEquals(1,work.size());
        assertNull(spins.frame("diamond_i",.5));assertEquals(0,loads.get());
        spins.prepare("onyx_iii");work.get(0).run();assertEquals(0,loads.get());
        work.get(1).run();assertEquals(1,loads.get());assertNull(spins.frame("diamond_i",.5));
        assertEquals(0xff123456,spins.frame("onyx_iii",0).getRGB(0,0));
        assertEquals(0xffabcdef,spins.frame("onyx_iii",1).getRGB(0,0));
        spins.clear();assertNull(spins.frame("onyx_iii",.5));
    }
    @Test public void missingOrMalformedSheetsFallBackWithoutRetryingEveryPaint(){
        AtomicInteger loads=new AtomicInteger();
        RankSpinImages spins=new RankSpinImages(Runnable::run,badge->{loads.incrementAndGet();return new BufferedImage(1,1,2);});
        for(int i=0;i<100;i++){spins.prepare("diamond_i");assertNull(spins.frame("diamond_i",.4));}
        assertEquals(1,loads.get());spins.prepare("../../other");assertEquals(1,loads.get());
        assertFalse(RankSpinImages.supported("unranked"));
    }
    @Test public void everyRankHasATransparentBoundedSheetAndAFiniteTurn()throws Exception{
        String[] gems={"opal","jade","red_topaz","sapphire","emerald","ruby","diamond","dragonstone","onyx","zenyte"};
        for(String gem:gems)for(String tier:new String[]{"iii","ii","i"}){
            String badge=gem+"_"+tier;BufferedImage sheet=RankSpinImages.load(badge);
            assertNotNull(badge,sheet);assertEquals(1536,sheet.getWidth());assertEquals(1152,sheet.getHeight());
            assertTrue(sheet.getColorModel().hasAlpha());assertEquals(0,sheet.getRGB(0,0)>>>24);
            sheet.flush();
        }
    }
}
