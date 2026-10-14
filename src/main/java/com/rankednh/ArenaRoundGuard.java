package com.rankednh;

/** Keeps the last challenge check across the transition into battle and across server review updates. */
final class ArenaRoundGuard {
    private final DefaultArenaKit defaultKit;
    @javax.inject.Inject ArenaRoundGuard(DefaultArenaKit defaultKit){this.defaultKit=defaultKit;}
    private String key;
    private ArenaSetup.View setup;
    private boolean entered;
    private volatile String code="",reason="";
    void reset(){key=null;setup=null;entered=false;code="";reason="";}
    void observe(Models.Status status,ArenaSetup.View view,boolean battle,boolean maxBuild){
        Models.Match match=status==null?null:status.match;
        if(match==null){reset();return;}
        String next=match.id+":"+match.round+":"+match.nonce;
        if(!next.equals(key)){reset();key=next;}
        if(!battle && !entered && view!=null)setup=view;
        if(!battle || entered || !("active".equals(match.state) || "reporting".equals(match.state)))return;
        entered=true;
        if(!maxBuild){code="build";reason="This fight is not using 126 combat with all combat stats at 99.";}
        else if(setup!=null && setup.warning){code="kit";reason=setup.message;}
        else if(setup==null || !setup.comparisonKnown || !defaultKit.matches(setup.kit) || !defaultKit.matches(setup.opponentKit)){
            code="unchecked";reason="Both kits were not checked before the fight started.";
        }
    }
    Models.SetupEvidence evidence(){return entered && !blocked() && setup!=null && setup.comparisonKnown?new Models.SetupEvidence(defaultKit.id(),setup.kit,setup.opponentKit):null;}
    void invalidateSetup(){if(!entered)setup=null;}
    void gameState(net.runelite.api.GameState state){
        // LOADING is the normal teleport into the fight; keep its just-checked setup.
        if(state==net.runelite.api.GameState.LOGIN_SCREEN || state==net.runelite.api.GameState.HOPPING || state==net.runelite.api.GameState.CONNECTION_LOST)invalidateSetup();
    }
    void checkConfigured(ArenaKit own){
        // These are saved setup selectors, not the worn items/inventory that change during combat.
        if(!entered || blocked())return;
        if(own==null){code="unchecked";reason="The configured kit could not be checked in the battle area.";}
        else if(!defaultKit.matches(own)){code="kit";reason="Your configured kit has an unapproved item, excess quantity or incorrect settings.";}
    }
    boolean blocked(){return !code.isEmpty();}
    String code(){return code;}
    String reason(){return reason;}
}
