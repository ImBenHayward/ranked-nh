package com.rankednh;

import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** A saved-kit check belongs to one enrolled character, service and reference version. */
@Singleton
final class ArenaOnboarding {
    enum Step { HOP, CRATE, MAX_MED, FIX, CHECKING, READY }
    private final DefaultArenaKit reference;
    private final ReportSeenStore verified;
    private String owner;
    private long correctSince=-1;
    private boolean ready;
    private Step step=Step.HOP;

    @Inject ArenaOnboarding(DefaultArenaKit reference,ConfigManager config){
        this(reference,new ReportSeenStore(()->config.getConfiguration("rankednh","verifiedArenaKits"),
            value->config.setConfiguration("rankednh","verifiedArenaKits",value)));
    }
    ArenaOnboarding(DefaultArenaKit reference,ReportSeenStore verified){this.reference=reference;this.verified=verified;}

    void observe(String service,Models.Status status,boolean accountMatches,boolean arena,boolean fighting,
        boolean supplies,boolean maxMed,ArenaSetup.View setup,long now){
        String key=key(service,status);
        if(!Objects.equals(owner,key)){
            owner=key;ready=key!=null && verified.contains(key);correctSince=-1;
        }
        if(!accountMatches || key==null || fighting){correctSince=-1;return;}
        // A duel editor can also change the saved loadout. Invalidate future queue entry,
        // while the active match keeps its normal cancel, review and forfeit controls.
        if(arena && setup!=null && setup.kit!=null && setup.kit.complete() && !reference.matches(setup.kit)){
            ready=false;correctSince=-1;verified.forget(key);
        }
        if(supplies && maxMed && arena){
            boolean correct=setup!=null && !setup.warning && reference.matches(setup.kit);
            if(!correct){
                ready=false;correctSince=-1;verified.forget(key);step=Step.FIX;return;
            }
            if(!ready){
                if(correctSince<0)correctSince=now;
                // Two game ticks give the native editor time to populate all saved selectors.
                if(now-correctSince>=1200){verified.remember(key);ready=true;}
            }
            step=ready?Step.READY:Step.CHECKING;return;
        }
        correctSince=-1;
        step=ready?Step.READY:!arena?Step.HOP:supplies?Step.MAX_MED:Step.CRATE;
    }
    boolean canQueue(String service,Models.Status status,boolean accountMatches){
        if(!accountMatches || status==null || status.player==null)return false;
        if("demo".equals(status.mode))return true;
        return ready && Objects.equals(owner,key(service,status));
    }
    boolean restart(String service,Models.Status status,boolean accountMatches,boolean fighting){
        String key=key(service,status);
        if(key==null || !accountMatches || fighting || !idle(status) || "demo".equals(status.mode))return false;
        verified.forget(key);owner=key;ready=false;correctSince=-1;step=Step.HOP;
        return true;
    }
    void interrupt(){correctSince=-1;}
    Step step(){return step;}
    private String key(String service,Models.Status status){
        return service==null || status==null || status.player==null || status.player.id==null?null:
            service+"\n"+status.player.id+"\n"+reference.id();
    }
    static boolean idle(Models.Status s){return s!=null && !s.queued && (s.match==null || !s.match.isOpen());}
}
