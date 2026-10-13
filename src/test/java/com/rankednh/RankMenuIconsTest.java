package com.rankednh;

import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class RankMenuIconsTest {
    @Test public void replacesArenaSkillRatingAndSpacesTheShield(){
        for(int sword=44;sword<=50;sword++)assertEquals("<img=101> <col=ffffff>Other Main<col=ff0000> (NH: Ruby II)",
            RankMenuIcons.replace("<img="+sword+"><col=ffffff>Other Main<col=ff0000> (skill-2500)",101,Set.of(101),"ruby_ii"));
        assertEquals("<img=1><img=101> <img=77>Other Main",RankMenuIcons.replace("<img=1><img=46><img=77>Other Main",101,Set.of(101),"ruby_ii"));
    }
    @Test public void repeatedDecorationAndRankChangesPreserveItemAndSpellTargets(){
        String target="<img=46>Item (skill-2500) -> <col=fff><img=46>Other Main (skill-2500)";
        String first=RankMenuIcons.replace(target,101,Set.of(101),"ruby_ii");
        assertEquals("<img=46>Item (skill-2500) -> <col=fff><img=101> Other Main (NH: Ruby II)",first);
        for(int i=0;i<20;i++)assertEquals(first,RankMenuIcons.replace(first,101,Set.of(101),"ruby_ii"));
        assertEquals("<img=46>Item (skill-2500) -> <col=fff><img=102> Other Main (NH: Ruby I)",
            RankMenuIcons.replace(first,102,Set.of(101,102),"ruby_i"));
        assertEquals("<img=101> Other Main",RankMenuIcons.replace("Other Main",101,Set.of(101),"unranked"));
    }
    @Test public void unrankedAndCombatLevelSuffixesAreUnambiguous(){
        assertEquals("<img=101> Other Main (NH: Unranked)",RankMenuIcons.replace("<img=44> Other Main (skill-2500)",101,Set.of(101),"unranked"));
        assertEquals("<img=101> Other Main (level-126)",RankMenuIcons.replace("Other Main (level-126)",101,Set.of(101),"red_topaz_iii"));
        assertEquals("Red Topaz III",RankMenuIcons.rankLabel("red_topaz_iii"));
        assertEquals("Other Main (skill-2500)",RankMenuIcons.replace("Other Main (skill-2500)",101,Set.of(101),"<img=1>"));
    }
    @Test public void unknownRanksStayUnknownAndCacheIsNormalizedAndExpires(){
        PlayerRankCache cache=new PlayerRankCache();cache.members(List.of("Other Main"),0);assertNull(cache.get("Other_Main",0));assertEquals(List.of("other main"),cache.take(0));
        Models.Opponent player=new Models.Opponent();player.rsn="Other Main";player.rank=new Models.Rank();player.rank.badge="unranked";
        cache.put(List.of("other main"),List.of(player),0);assertSame(player.rank,cache.get("OTHER\u00a0MAIN",1));assertTrue(cache.take(0).isEmpty());
        assertNull(cache.get("Other Main",60_001));assertEquals(List.of("other main"),cache.take(0));
        cache.put(List.of("other main"),List.of(),60_001);assertNull(cache.get("Other Main",60_002));assertTrue(cache.take(0).isEmpty());
        cache.clear();assertNull(cache.get("Other Main",60_003));assertTrue(cache.take(0).isEmpty());
    }
    @Test public void lookupBatchesAndAssetsAreBounded(){
        PlayerRankCache cache=new PlayerRankCache();cache.members(java.util.stream.IntStream.range(0,200).mapToObj(i->"player"+i).collect(java.util.stream.Collectors.toList()),0);for(int i=0;i<200;i++)cache.get("player"+i,0);
        for(int i=0;i<4;i++)assertEquals(25,cache.take(0).size());assertTrue(cache.take(0).isEmpty());
        assertTrue(RankMenuIcons.validBadge("unranked"));assertTrue(RankMenuIcons.validBadge("dragonstone_i"));
        assertFalse(RankMenuIcons.validBadge("gold"));assertFalse(RankMenuIcons.validBadge("../../secret"));assertFalse(RankMenuIcons.validBadge(null));
    }
    @Test public void neverQueuesNamesWithoutCurrentMembershipAndRemovesRevokedMembers(){
        PlayerRankCache cache=new PlayerRankCache();
        cache.get("Unknown",0);assertTrue(cache.take(0).isEmpty());
        cache.members(List.of("Other Main"),1);
        cache.get("Unknown",2);cache.get("Other Main",2);assertEquals(List.of("other main"),cache.take(2));
        cache.get("Other Main",600_001);assertTrue(cache.take(600_001).isEmpty());
        cache.members(List.of("Other Main"),700_000);cache.get("Other Main",700_001);
        cache.members(List.of(),700_002);assertTrue(cache.take(700_002).isEmpty());
    }
}
