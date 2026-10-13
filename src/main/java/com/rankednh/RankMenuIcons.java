package com.rankednh;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.WorldType;
import net.runelite.client.game.ChatIconManager;

/** Replaces only Arena rank swords in player targets; never changes game sprites globally. */
@Singleton
final class RankMenuIcons {
    private static final Pattern IMAGE=Pattern.compile("<img=(\\d+)> *");
    private final Client client;
    private final ChatIconManager icons;
    private final Map<String,Integer> registered=new HashMap<>();
    final PlayerRankCache cache=new PlayerRankCache();
    @Inject RankMenuIcons(Client client,ChatIconManager icons){this.client=client;this.icons=icons;}
    void decorate(MenuEntry[] entries,Models.Status status){
        if(status==null || client.getRevision()!=ArenaSetup.REVISION || client.getGameState()!=GameState.LOGGED_IN || !client.getWorldType().contains(WorldType.PVP_ARENA))return;
        Set<Integer> ownIcons=new HashSet<>();
        for(int id:registered.values()){int index=icons.chatIconIndex(id);if(index>=0)ownIcons.add(index);}
        for(MenuEntry entry:entries){
            Player player=entry.getPlayer();if(player==null || player.getName()==null)continue;
            Models.Rank rank=knownRank(player.getName(),status);
            if(rank==null || !validBadge(rank.badge))continue;
            int id=registered.computeIfAbsent(rank.badge,badge->icons.registerChatIcon(BadgeImages.menuImage(badge)));
            int index=icons.chatIconIndex(id);if(index<0)continue;
            ownIcons.add(index);
            entry.setTarget(replace(entry.getTarget(),index,ownIcons,rank.badge));
        }
    }
    private Models.Rank knownRank(String name,Models.Status status){
        String normalized=FightRecorder.normalized(name);
        if(status.player!=null && normalized.equals(FightRecorder.normalized(status.player.rsn)))return status.player.rank;
        if(status.match!=null && status.match.opponent!=null && normalized.equals(FightRecorder.normalized(status.match.opponent.rsn)))return status.match.opponent.rank;
        return cache.get(name,System.currentTimeMillis());
    }
    static boolean validBadge(String badge){return badge!=null && (badge.equals("unranked") || badge.matches("(opal|jade|red_topaz|sapphire|emerald|ruby|diamond|dragonstone|onyx|zenyte)_(i|ii|iii)"));}
    static String replace(String target,int index,Set<Integer> ownIcons,String badge){
        if(target==null || target.isEmpty() || index<0 || !validBadge(badge))return target;
        // Item/spell target icons before the arrow belong to the item, not the player.
        int arrow=target.lastIndexOf("->");int start=arrow<0?0:arrow+2;
        String prefix=target.substring(0,start),player=target.substring(start);
        Matcher matcher=IMAGE.matcher(player);StringBuffer out=new StringBuffer();boolean inserted=false;
        while(matcher.find()){
            int icon;
            try{icon=Integer.parseInt(matcher.group(1));}catch(NumberFormatException invalid){continue;}
            // Verified revision-241 mod_icons atlas: Arena swords run from bronze (44) to dragon (50).
            if(icon>=44 && icon<=50 || ownIcons.contains(icon)){
                matcher.appendReplacement(out,inserted?"":"<img="+index+"> ");inserted=true;
            }
        }
        matcher.appendTail(out);
        String decorated=inserted?out.toString():"<img="+index+"> "+player;
        // Only the Arena skill suffix (or our own prior suffix). Combat levels stay intact.
        return prefix+decorated.replaceAll("\\((?:skill-\\d+|NH: [A-Za-z ]+)\\)",
            "(NH: "+rankLabel(badge)+")");
    }
    static String rankLabel(String badge){
        if("unranked".equals(badge))return "Unranked";
        String[] words=badge.split("_");StringBuilder label=new StringBuilder();
        for(int i=0;i<words.length;i++){
            if(i>0)label.append(' ');
            String word=words[i];
            label.append(i==words.length-1?word.toUpperCase(java.util.Locale.ROOT):Character.toUpperCase(word.charAt(0))+word.substring(1));
        }
        return label.toString();
    }
}
