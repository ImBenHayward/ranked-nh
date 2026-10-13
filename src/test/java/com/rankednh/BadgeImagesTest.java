package com.rankednh;

import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.IndexedSprite;
import net.runelite.client.util.ImageUtil;
import org.junit.Test;
import static org.junit.Assert.*;

public class BadgeImagesTest {
    @Test public void everyMenuBadgeSurvivesRuneLiteIndexedSpriteConversion(){
        List<String> badges=new ArrayList<>();badges.add("unranked");
        for(String gem:new String[]{"opal","jade","red_topaz","sapphire","emerald","ruby","diamond","dragonstone","onyx","zenyte"})
            for(String tier:new String[]{"i","ii","iii"})badges.add(gem+"_"+tier);
        Client client=(Client)Proxy.newProxyInstance(Client.class.getClassLoader(),new Class<?>[]{Client.class},(p,m,a)->{
            if(!m.getName().equals("createIndexedSprite"))throw new AssertionError(m.getName());
            Map<String,Object> fields=new HashMap<>();
            return Proxy.newProxyInstance(IndexedSprite.class.getClassLoader(),new Class<?>[]{IndexedSprite.class},(sp,sm,sa)->{
                String key=sm.getName().substring(3);
                if(sm.getName().startsWith("set")){fields.put(key,sa[0]);return null;}
                return fields.get(key);
            });
        });
        for(String badge:badges){
            BufferedImage image=BadgeImages.menuImage(badge);
            IndexedSprite sprite=ImageUtil.getImageIndexedSprite(image,client);
            int visible=0;
            for(byte pixel:sprite.getPixels())if(pixel!=0)visible++;
            assertTrue(badge+" must remain visible in the native menu",visible>=image.getWidth()*image.getHeight()/3);
            assertTrue(badge+" must fit the menu line",sprite.getWidth()<=13 && sprite.getHeight()<=13);
            assertTrue(badge+" must retain a transparent background",visible<sprite.getPixels().length);
        }
    }
}
