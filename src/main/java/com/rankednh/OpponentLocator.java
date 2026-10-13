package com.rankednh;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldType;

final class OpponentLocator
{
    static boolean eligible(Models.Status s,boolean accountMatches,int world,boolean arena){
        if(!accountMatches || s==null || s.match==null || !arena || world!=s.match.world)return false;
        String state=s.match.state;
        return "preparing".equals(state) || "active".equals(state) || "reporting".equals(state);
    }
    static Player find(Client client,Models.Status s,boolean accountMatches){
        if(client.getGameState()!=GameState.LOGGED_IN || !eligible(s,accountMatches,client.getWorld(),client.getWorldType().contains(WorldType.PVP_ARENA)))return null;
        String name=FightRecorder.normalized(s.match.opponent.rsn);
        if(client.getTopLevelWorldView()==null)return null;
        for(Player player:client.getTopLevelWorldView().players())
            if(player!=client.getLocalPlayer() && name.equals(FightRecorder.normalized(player.getName())))return player;
        return null;
    }
    private OpponentLocator(){}
}
