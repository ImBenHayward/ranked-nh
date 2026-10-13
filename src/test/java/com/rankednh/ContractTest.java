package com.rankednh;

import org.junit.Test;
import static org.junit.Assert.*;

public class ContractTest
{
    @Test public void serviceRequiresHttpsExceptLoopback(){
        assertEquals("https://ranked.example",RankedNhApi.validateUrl("https://ranked.example/"));
        assertEquals("http://127.0.0.1:8787",RankedNhApi.validateUrl("http://127.0.0.1:8787"));
        for(String url:new String[]{"http://example.com","https://key@example.com","file:///tmp/key","https://example.com?token=key"}){
            try{RankedNhApi.validateUrl(url);fail("Accepted unsafe service URL");}catch(IllegalArgumentException expected){}
        }
    }
    @Test public void runescapeNamesNormalizeSpacesAndCase(){assertEquals("test main",FightRecorder.normalized(" Test\u00a0Main "));assertEquals("test main",FightRecorder.normalized("Test_Main"));}
    @Test public void externalTextCannotInjectSwingHtml(){assertEquals("&lt;b&gt;&amp;",RankedNhPanel.escape("<b>&"));}
    @Test public void allThirtyBadgesAndPlacementAssetLoad(){
        for(String gem:new String[]{"opal","jade","red_topaz","sapphire","emerald","ruby","diamond","dragonstone","onyx","zenyte"}){
            for(String division:new String[]{"iii","ii","i"}){
                assertEquals(256,BadgeImages.image(gem+"_"+division).getWidth());
                assertEquals(256,BadgeImages.image(gem+"_"+division).getHeight());
            }
        }
        assertEquals(256,BadgeImages.image("unranked").getWidth());
    }
}
