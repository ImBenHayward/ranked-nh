package com.rankednh;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Bounded, short-lived directory cache. An absent player is unknown, not Unranked. */
final class PlayerRankCache {
    private static final class Entry { Models.Rank rank;long until;Entry(Models.Rank rank,long until){this.rank=rank;this.until=until;} }
    private final Map<String,Entry> entries=new LinkedHashMap<>();
    private final LinkedHashSet<String> pending=new LinkedHashSet<>();
    private final java.util.Set<String> members=new java.util.HashSet<>();
    private long membershipRefresh, membershipExpires;
    synchronized boolean membershipDue(long now){return now>=membershipRefresh;}
    synchronized void members(List<String> names,long now){
        members.clear();for(String name:names)members.add(FightRecorder.normalized(name));
        entries.keySet().retainAll(members);pending.retainAll(members);
        membershipRefresh=now+300_000;membershipExpires=now+600_000;
    }
    synchronized Models.Rank get(String name,long now){
        String key=FightRecorder.normalized(name);
        if(now>=membershipExpires || !members.contains(key) || !key.matches("[a-z0-9 -]{1,12}"))return null;
        Entry entry=entries.get(key);
        if(entry!=null && now<entry.until)return entry.rank;
        if(pending.size()<100)pending.add(key);
        return null;
    }
    synchronized List<String> take(long now){
        if(now>=membershipExpires){pending.clear();return List.of();}
        List<String> out=new ArrayList<>();
        java.util.Iterator<String> it=pending.iterator();
        while(it.hasNext() && out.size()<25){out.add(it.next());it.remove();}
        return out;
    }
    synchronized void put(List<String> requested,List<Models.Opponent> players,long now){
        for(String name:requested)entries.put(FightRecorder.normalized(name),new Entry(null,now+60_000));
        for(Models.Opponent p:players){String key=FightRecorder.normalized(p.rsn);
            if(requested.contains(key))entries.put(key,new Entry(p.rank,now+60_000));
        }
        while(entries.size()>256)entries.remove(entries.keySet().iterator().next());
    }
    synchronized void clear(){entries.clear();pending.clear();members.clear();membershipRefresh=0;membershipExpires=0;}
}
