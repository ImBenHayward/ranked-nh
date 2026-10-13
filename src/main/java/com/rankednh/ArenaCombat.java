package com.rankednh;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;

/** Revision 241 native battle context and timer. Independent of API match state. */
final class ArenaCombat {
    static boolean inBattle(Client client){
        return client.getGameState()==GameState.LOGGED_IN && client.getWorldType().contains(WorldType.PVP_ARENA)
            && client.getRevision()==ArenaSetup.REVISION && client.getVarbitValue(14022)==1
            && (client.getVarbitValue(14017)==1 || client.getVarbitValue(14016)>0)
            // The challenge dialog is never a fight. Its shared-area flag can coexist with
            // timer/status values left over from the previous duel.
            && !ArenaSetup.visible(client,ArenaSetup.DUEL,0);
    }
}
