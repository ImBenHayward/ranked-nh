package com.rankednh;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArenaOnboardingTest {
    private static final String SERVICE="https://example.test";
    private final AtomicReference<String> saved=new AtomicReference<>("");
    private final ReportSeenStore store=new ReportSeenStore(saved::get,saved::set);
    private ArenaOnboarding onboarding=new ArenaOnboarding(TestArenaKit.DEFAULT,store);
    private final Models.Status status=status("a");
    private final ArenaKit kit=TestArenaKit.DEFAULT.kit();
    private static Models.Status status(String id){
        Models.Status s=new Models.Status();s.mode="pilot";s.player=new Models.Player();s.player.id=id;return s;
    }
    private ArenaSetup.View view(ArenaKit kit){return new ArenaSetup.View("","","",false,kit);}
    private void observe(boolean arena,boolean supplies,boolean maxMed,ArenaKit kit,long now){
        onboarding.observe(SERVICE,status,true,arena,false,supplies,maxMed,view(kit),now);
    }
    private void verify(){observe(true,true,true,kit,0);observe(true,true,true,kit,1200);}
    @Test public void requiresAnActualStableMaxMedSuppliesRead(){
        observe(false,false,false,kit,0);assertEquals(ArenaOnboarding.Step.HOP,onboarding.step());
        observe(true,false,false,kit,1200);assertEquals(ArenaOnboarding.Step.CRATE,onboarding.step());
        observe(true,true,false,kit,2400);assertEquals(ArenaOnboarding.Step.MAX_MED,onboarding.step());
        assertFalse(onboarding.canQueue(SERVICE,status,true));
        observe(true,true,true,kit,3000);assertEquals(ArenaOnboarding.Step.CHECKING,onboarding.step());
        observe(true,true,true,kit,3600);assertFalse(onboarding.canQueue(SERVICE,status,true));
        observe(true,true,false,kit,4200);observe(true,true,true,kit,4800);assertFalse(onboarding.canQueue(SERVICE,status,true));
        observe(true,true,true,kit,6000);assertTrue(onboarding.canQueue(SERVICE,status,true));
    }
    @Test public void persistsAcrossReconnectButNeverTransfersToAnotherAccountOrService(){
        verify();assertTrue(onboarding.canQueue(SERVICE,status,true));
        onboarding=new ArenaOnboarding(TestArenaKit.DEFAULT,store);
        observe(false,false,false,null,2000);assertTrue(onboarding.canQueue(SERVICE,status,true));
        assertFalse(onboarding.canQueue(SERVICE,status,false));
        assertFalse(onboarding.canQueue(SERVICE,status("b"),true));
        assertFalse(onboarding.canQueue("https://another.test",status,true));
        assertFalse(saved.get().contains(SERVICE));
    }
    @Test public void aChangedSpellbookOrRuneSelectionRevokesTheCheckUntilFixed(){
        verify();
        ArenaKit wrong=new ArenaKit(kit.revision,kit.build,kit.spellbook+1,kit.runePouch,kit.equipment,kit.inventory);
        observe(true,true,true,wrong,2000);assertFalse(onboarding.canQueue(SERVICE,status,true));assertEquals("",saved.get());
        verify();assertTrue(onboarding.canQueue(SERVICE,status,true));
        wrong=new ArenaKit(kit.revision,kit.build,kit.spellbook,kit.runePouch+1,kit.equipment,kit.inventory);
        observe(true,true,true,wrong,3000);assertFalse(onboarding.canQueue(SERVICE,status,true));
    }
    @Test public void missingAndUnsupportedDataCannotUnlockQueue(){
        observe(true,true,true,null,0);observe(true,true,true,null,1200);assertFalse(onboarding.canQueue(SERVICE,status,true));
        ArenaKit future=new ArenaKit(kit.revision+1,kit.build,kit.spellbook,kit.runePouch,kit.equipment,kit.inventory);
        observe(true,true,true,future,2000);observe(true,true,true,future,3400);assertFalse(onboarding.canQueue(SERVICE,status,true));
        onboarding.observe(SERVICE,status,false,true,false,true,true,view(kit),4000);
        onboarding.observe(SERVICE,status,false,true,false,true,true,view(kit),6000);assertFalse(onboarding.canQueue(SERVICE,status,true));
        onboarding.observe(SERVICE,status,true,true,true,true,true,view(kit),7000);assertFalse(onboarding.canQueue(SERVICE,status,true));
    }
    @Test public void disconnectResetsTheStableReadAndDuelEditsRequireAnotherSuppliesCheck(){
        observe(true,true,true,kit,0);onboarding.interrupt();observe(true,true,true,kit,3000);
        assertFalse(onboarding.canQueue(SERVICE,status,true));observe(true,true,true,kit,4200);assertTrue(onboarding.canQueue(SERVICE,status,true));
        ArenaKit wrong=new ArenaKit(kit.revision,kit.build,kit.spellbook+1,kit.runePouch,kit.equipment,kit.inventory);
        observe(true,false,false,wrong,5000);assertFalse(onboarding.canQueue(SERVICE,status,true));
        observe(true,false,false,kit,5600);observe(true,false,false,kit,6800);assertFalse(onboarding.canQueue(SERVICE,status,true));
    }
    @Test public void demoIsStillUsableWithoutTheRealArenaAndAMatchDoesNotShowOnboarding(){
        status.mode="demo";assertTrue(onboarding.canQueue(SERVICE,status,true));assertFalse(onboarding.canQueue(SERVICE,status,false));
        status.match=new Models.Match();status.match.state="active";assertFalse(ArenaOnboarding.idle(status));
        status.match.state="complete";assertTrue(ArenaOnboarding.idle(status));status.queued=true;assertFalse(ArenaOnboarding.idle(status));
    }
}
