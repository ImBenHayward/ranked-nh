package com.rankednh;

import com.google.inject.Provides;
import java.util.Arrays;
import java.util.Map;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.SoundEffectID;
import net.runelite.api.Skill;
import net.runelite.client.Notifier;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;

@PluginDescriptor(name="Ranked NH", description="Max-main NH matchmaking and best-of-three series", tags={"pvp","nh","ranked","arena"})
public class RankedNhPlugin extends Plugin implements RankedNhPanel.Actions
{
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private OverlayManager overlays;
    @Inject private RankedNhReportOverlay reportOverlay;
    @Inject private RankedNhOpponentOverlay opponentOverlay;
    @Inject private RankedNhOpponentMinimapOverlay opponentMinimapOverlay;
    @Inject private ArenaSetupWarningOverlay setupWarningOverlay;
    @Inject private ArenaSuppliesOverlay suppliesOverlay;
    @Inject private ArenaOnboarding onboarding;
    private volatile ArenaOnboarding.Step onboardingStep;
    private volatile boolean kitRequired;
    private volatile Boolean serviceRated;
    private final AtomicBoolean checkingServiceMode=new AtomicBoolean();
    @Inject private RoundNoticeOverlay roundNoticeOverlay;
    @Inject private ArenaWorldHopper hopper;
    @Inject private Notifier notifier;
    @Inject private MouseManager mouseManager;
    @Inject private KeyManager keyManager;
    @Inject private RankedNhConfig config;
    @Inject private ConfigManager configManager;
    @Inject private RankedNhApi api;
    @Inject private RankMenuIcons rankMenuIcons;
    @Inject private ScheduledExecutorService executor;
    private ServiceRequests requests;
    private final AtomicLong generation=new AtomicLong();
    private final PollCadence pollCadence=new PollCadence();
    private final FightRecorder recorder=new FightRecorder();
    private final MatchFoundAlert matchAlert=new MatchFoundAlert();
    private final AutoReady autoReady=new AutoReady();
    @Inject private ArenaSetup arenaSetup;
    @Inject private ArenaRoundGuard roundGuard;
    private volatile boolean battleArea;
    private volatile Map<String,Object> pendingSetupIssue;
    private volatile String pendingSetupMatch;
    private String submittedSetup;
    private volatile ArenaSetup.View setupView;
    private volatile String snapshotMessage="";
    private final AtomicBoolean savingKit=new AtomicBoolean();
    private final AtomicBoolean fetchingRanks=new AtomicBoolean();
    private volatile long rankRetryAfter;
    private volatile Models.Status status;
    private volatile String token,baseUrl,problem="",recording="";
    private volatile boolean running,accountMatches;
    private volatile int currentWorld;
    private volatile boolean arenaWorld,opponentNearby,fighting;
    private volatile int hitpoints;
    private volatile long serverOffset;
    private volatile String travelProblem="",travelMatch;
    private volatile int previewStage;
    private volatile Models.Report pendingReport;
    private volatile String pendingMatch;
    private volatile boolean reportBlocked;
    private RankedNhPanel panel;
    private NavigationButton navigation;

    @Override protected void startUp() {
        running=true;pollCadence.reset();baseUrl=RankedNhApi.validateUrl(config.serviceUrl());
        requests=new ServiceRequests(executor,this::refresh,()->System.nanoTime()/1_000_000);
        SwingUtilities.invokeLater(()->{
            if(!running)return;
            panel=new RankedNhPanel(this,config.preferredRegion());
            navigation=NavigationButton.builder().tooltip("Ranked NH").icon(net.runelite.client.util.ImageUtil.resizeImage(BadgeImages.image("zenyte_i"),16,16)).priority(6).panel(panel).build();
            toolbar.addNavigation(navigation);
        });
        overlays.add(opponentOverlay);overlays.add(opponentMinimapOverlay);overlays.add(setupWarningOverlay);overlays.add(suppliesOverlay);previewStage=0;matchAlert.reset();autoReady.reset();
        reportOverlay.reset();overlays.add(reportOverlay);overlays.add(roundNoticeOverlay);roundGuard.reset();
        // Stretched Mode's listeners at index zero must translate coordinates first.
        mouseManager.registerMouseListener(reportOverlay.input);
        mouseManager.registerMouseWheelListener(reportOverlay.input);
        keyManager.registerKeyListener(reportOverlay.input);
    }
    @Override protected void shutDown() {
        generation.incrementAndGet();
        requests.discardQueued();
        running=false;token=null;status=null;serviceRated=null;pendingReport=null;pendingMatch=null;
        rankMenuIcons.cache.clear();
        clientThread.invoke(recorder::clear);overlays.remove(reportOverlay);
        overlays.remove(roundNoticeOverlay);pendingSetupIssue=null;pendingSetupMatch=null;submittedSetup=null;battleArea=false;
        overlays.remove(opponentOverlay);overlays.remove(opponentMinimapOverlay);overlays.remove(setupWarningOverlay);overlays.remove(suppliesOverlay);clientThread.invoke(hopper::cancel);
        reportOverlay.reset();
        mouseManager.unregisterMouseListener(reportOverlay.input);
        mouseManager.unregisterMouseWheelListener(reportOverlay.input);
        keyManager.unregisterKeyListener(reportOverlay.input);
        reportOverlay.input.focusLost();
        SwingUtilities.invokeLater(()->{if(navigation!=null)toolbar.removeNavigation(navigation);panel=null;});
    }
    @Provides RankedNhConfig provideConfig(ConfigManager manager){return manager.getConfig(RankedNhConfig.class);}
    Models.Status status(){return status;}
    ArenaSetup.View setupView(){return setupView;}
    ArenaOnboarding.Step onboardingStep(){return accountMatches && kitRequired && ArenaOnboarding.idle(status) && !battleArea?onboardingStep:null;}
    boolean inArenaBattle(){return battleArea;}
    String roundNotice(){
        Models.Status s=status;if(!accountMatches || !battleArea || s==null || s.match==null)return "";
        if(roundGuard.blocked())return "NH paused · Setup needs fixing";
        if("review".equals(s.match.state))return "NH paused · See the sidebar";
        if("reporting".equals(s.match.state))return "NH is waiting for both round reports";
        return "";
    }
    boolean accountMatches(){return accountMatches;}
    long serverNow(){return System.currentTimeMillis()+serverOffset;}
    MatchupView handoffView(){
        MatchupView view=MatchupView.from(status,accountMatches,requests!=null && requests.actionPending(),currentWorld,arenaWorld,opponentNearby,hopper.busy(),fighting,serverNow());
        if(view!=null && "hop".equals(view.action) && !travelProblem.isEmpty()){view.message=travelProblem;view.footer="Try again, or open the panel for details.";}
        Models.Status current=status;
        if(view!=null && current!=null && "preparing".equals(current.match.state) && "panel".equals(view.action) &&
            !current.match.ready && MatchFoundAlert.secondsLeft(current,serverNow())>0){
            String blocker=AutoReady.blocker(opponentNearby,hitpoints);
            if(!blocker.isEmpty())view.message=blocker;
            else if(battleArea){view.message="Finish the current duel before checking in.";view.footer="Return to the lobby after the fight.";}
            else if(autoReady.awaitingAcknowledgement(current)){
                view.message="Waiting for the server to confirm your arrival.";view.footer="Your client retries automatically.";
            }
        }
        return view;
    }
    void handoffProblem(String message){travelProblem=message;refresh();}
    @Override public void handoffAction(String key,String action){
        clientThread.invoke(()->{
            if(key.startsWith("preview:")){
                if(!config.previewMatchup() || !key.equals("preview:"+previewStage))return;
                if("close-preview".equals(action))configManager.setConfiguration("rankednh","previewMatchup",false);
                else if("preview".equals(action))previewStage=(previewStage+1)%4;
                refresh();return;
            }
            Models.Status s=status;
            if(!running || s==null || s.match==null || !MatchupView.key(s).equals(key))return;
            MatchupView view=handoffView();if(view==null)return;
            if("panel".equals(action)){SwingUtilities.invokeLater(()->{if(navigation!=null)toolbar.openPanel(navigation);});return;}
            if(!action.equals(view.action) && !action.equals(view.secondaryAction))return;
            if(action.equals(view.action) && !view.enabled || action.equals(view.secondaryAction) && !view.secondaryEnabled)return;
            switch(action){case "accept":accept(true);break;case "decline":accept(false);break;case "hop":hop();break;default:break;}
        });
    }
    private void updateHandoff(){
        currentWorld=client.getWorld();arenaWorld=client.getWorldType().contains(WorldType.PVP_ARENA);
        hitpoints=client.getBoostedSkillLevel(Skill.HITPOINTS);
        Models.Match match=status==null?null:status.match;
        if(match==null && (travelMatch!=null || arenaWorld) || match!=null && (!match.id.equals(travelMatch) || currentWorld==match.world || !"preparing".equals(match.state)))travelProblem="";
        travelMatch=match==null?null:match.id;
        battleArea=ArenaCombat.inBattle(client);
        opponentNearby=OpponentLocator.find(client,status,accountMatches)!=null;fighting=battleArea;
        setupView=arenaSetup.read(client,status,accountMatches,fighting);
        onboarding.observe(baseUrl,status,accountMatches,arenaWorld,battleArea,
            ArenaSetup.visible(client,ArenaSetup.SUPPLIES,0) && !ArenaSetup.visible(client,ArenaSetup.DUEL,0),
            ArenaSetup.visible(client,ArenaSetup.SUPPLIES,9),setupView,System.nanoTime()/1_000_000);
        kitRequired=!onboarding.canQueue(baseUrl,status,accountMatches);
        onboardingStep=onboarding.step();
        suppliesOverlay.update();
        if(accountMatches){
            roundGuard.observe(status,setupView,battleArea,client.getLocalPlayer()!=null && client.getLocalPlayer().getCombatLevel()==126 && FightRecorder.maxStats(client));
            if(battleArea){
                try{roundGuard.checkConfigured(ArenaSetup.ownKit(client));}
                catch(RuntimeException unavailable){roundGuard.checkConfigured(null);}
            }
            if(roundGuard.blocked()){
                recorder.stopForSetup(roundGuard.reason());
                if(match!=null && match.nonce!=null && ("active".equals(match.state) || "reporting".equals(match.state))){
                    String key=match.id+":"+match.round;
                    if(!key.equals(submittedSetup)){
                        submittedSetup=key;pendingSetupMatch=match.id;
                        pendingSetupIssue=Map.of("round",match.round,"nonce",match.nonce,"code",roundGuard.code());
                        if(match.id.equals(pendingMatch)){pendingReport=null;pendingMatch=null;}
                        poll();
                    }
                }
            }
        }
        if(matchAlert.take(status,accountMatches,serverNow())){
            notifier.notify(config.matchFoundNotification(),"Ranked NH: match found vs "+status.match.opponent.rsn+" · "+MatchFoundAlert.region(status.match.region)+". Accept in RuneLite.");
            if(config.matchFoundPing())client.playSoundEffect(SoundEffectID.GE_ADD_OFFER_DINGALING,80);
            openPanel();
        }
    }

    private interface Work { Models.Status run() throws Exception; }
    private void work(boolean action,String label,Work operation) {
        if(!running)return;
        final long session=generation.get();
        requests.submit(action,label,()->{
            if(!running || generation.get()!=session)return;
            try {Models.Status next=operation.run();if(running && generation.get()==session && next!=null){status=next;problem="";clientThread.invoke(()->apply(next));}}
            catch(Exception error){if(running && generation.get()==session)problem=error instanceof java.io.InterruptedIOException?
                "Server response timed out. Checking your match status again…":error.getMessage()==null?"Connection unavailable. Retry shortly.":error.getMessage();}
        });
    }
    private void apply(Models.Status next){
        if(!running || status!=next)return;
        try{serverOffset=Instant.parse(next.serverTime).toEpochMilli()-System.currentTimeMillis();}catch(Exception ignored){}
        accountMatches=client.getGameState()==GameState.LOGGED_IN && client.getLocalPlayer()!=null && FightRecorder.normalized(client.getLocalPlayer().getName()).equals(FightRecorder.normalized(next.player.rsn));
        recorder.arm(next);recording=reportBlocked?"Report needs attention — request a review":recorder.message();updateHandoff();refresh();
    }
    private void openPanel(){SwingUtilities.invokeLater(()->{if(running && navigation!=null)toolbar.openPanel(navigation);});}
    private void refresh(){SwingUtilities.invokeLater(()->{
        if(panel==null || !running)return;
        MatchupView view=handoffView();
        Models.Status current=status;
        if(config.previewMatchup() && (current==null || (!current.queued && (current.match==null || !current.match.isOpen()))))view=MatchupView.preview(previewStage);
        panel.onboarding(onboardingStep,kitRequired && accountMatches && !config.previewMatchup(),hopper.busy());
        panel.render(current,problem.isEmpty()?travelProblem:problem,requests.actionPending(),accountMatches,recording,view,serverNow());
        if(!config.previewMatchup()){
            panel.renderSetup(setupView,snapshotMessage,savingKit.get(),config.kitDiagnostics());
            if(accountMatches && ArenaOnboarding.idle(current))panel.renderOnboarding(setupView);
        }
        if(roundGuard.blocked() && battleArea)panel.renderRoundIssue(roundGuard.reason());
        panel.renderRatingAvailability(current==null?null:current.rated==null?serviceRated:current.rated);
        panel.renderRequest(requests.message());
    });}
    @Override public void connect(char[] key){
        String access=new String(key).trim();Arrays.fill(key,'\0');
        if(access.isEmpty()){problem="Enter your pilot access key.";refresh();return;}
        String service;
        try{service=RankedNhApi.validateUrl(config.serviceUrl());}catch(IllegalArgumentException e){problem=e.getMessage();refresh();return;}
        final long session=generation.get();
        work(true,"Connecting…",()->{Models.Status next=api.call(service,access,"/me",null);if(running && generation.get()==session){token=access;baseUrl=service;serviceRated=null;executor.execute(this::updateServiceMode);reportOverlay.connected(next);openPanel();}return next;});
    }
    @Schedule(period=60,unit=ChronoUnit.SECONDS,asynchronous=true)
    public void updateServiceMode(){
        if(!running || token==null || !checkingServiceMode.compareAndSet(false,true))return;
        final long session=generation.get();final String service=baseUrl,access=token;
        Boolean rated=null;
        try{rated=api.rated(service,access);}catch(java.io.IOException | RuntimeException unavailable){/* Show unknown, never invent a rating policy. */}
        finally{checkingServiceMode.set(false);}
        if(running && generation.get()==session && service.equals(baseUrl) && access.equals(token)){serviceRated=rated;refresh();}
    }
    @Schedule(period=3,unit=ChronoUnit.SECONDS,asynchronous=true)
    public void poll(){
        if(token==null || !pollCadence.due(status,accountMatches,pendingReport!=null || pendingSetupIssue!=null,System.nanoTime()/1_000_000))return;
        final String service=baseUrl,access=token;
        work(false,"",()->{
            Map<String,Object> issue=pendingSetupIssue;String issueMatch=pendingSetupMatch;
            if(issue!=null && issueMatch!=null){
                try{
                    Models.Status next=api.call(service,access,"/match/"+issueMatch+"/setup-issue",issue);
                    if(pendingSetupIssue==issue){pendingSetupIssue=null;pendingSetupMatch=null;}return next;
                }catch(RankedNhApi.ApiException error){
                    if(error.code==400 || error.code==404 || error.code==409){if(pendingSetupIssue==issue){pendingSetupIssue=null;pendingSetupMatch=null;}}
                    else throw error;
                }
            }
            Models.Report report=pendingReport;String match=pendingMatch;
            if(report!=null && match!=null && !reportBlocked){
                try{
                    Models.Status next=api.call(service,access,"/match/"+match+"/report",report);
                    if(pendingReport==report){pendingReport=null;pendingMatch=null;}
                    return next;
                }catch(RankedNhApi.ApiException error){
                    if(error.code==400 || error.code==404 || error.code==409){reportBlocked=true;}
                    else throw error;
                }
            }
            return api.call(service,access,"/queue/status",null);
        });
    }
    @Schedule(period=1,unit=ChronoUnit.SECONDS,asynchronous=true)
    public void lookupMenuRanks(){
        if(!running || token==null || !accountMatches || !arenaWorld || System.currentTimeMillis()<rankRetryAfter || !fetchingRanks.compareAndSet(false,true))return;
        final long session=generation.get();final String service=baseUrl,access=token;
        try{
            if(rankMenuIcons.cache.membershipDue(System.currentTimeMillis())){
                String membership=api.request(service,access,"/players/members",null);
                com.google.gson.JsonArray values=gson.fromJson(membership,com.google.gson.JsonObject.class).getAsJsonArray("names");
                if(values.size()>5000)throw new java.io.IOException("Membership directory is too large");
                java.util.List<String> members=new java.util.ArrayList<>();
                for(com.google.gson.JsonElement value:values)members.add(value.getAsString());
                if(!running || generation.get()!=session)return;
                clientThread.invoke(()->{if(running && generation.get()==session)rankMenuIcons.cache.members(members,System.currentTimeMillis());});
            }
            java.util.List<String> names=rankMenuIcons.cache.take(System.currentTimeMillis());if(names.isEmpty())return;
            String json=api.request(service,access,"/players/ranks",Map.of("names",names));
            com.google.gson.JsonArray players=gson.fromJson(json,com.google.gson.JsonObject.class).getAsJsonArray("players");
            java.util.List<Models.Opponent> ranks=new java.util.ArrayList<>();
            for(com.google.gson.JsonElement value:players)ranks.add(gson.fromJson(value,Models.Opponent.class));
            clientThread.invoke(()->{if(running && generation.get()==session)rankMenuIcons.cache.put(names,ranks,System.currentTimeMillis());});
        }catch(java.io.IOException | RuntimeException unavailable){if(generation.get()==session)rankRetryAfter=System.currentTimeMillis()+15_000;}
        finally{fetchingRanks.set(false);}
    }
    @Subscribe(priority=-1) public void onClientTick(ClientTick event){
        if(!client.isMenuOpen())rankMenuIcons.decorate(client.getMenuEntries(),status);
    }
    @Subscribe(priority=-1) public void onMenuOpened(MenuOpened event){rankMenuIcons.decorate(event.getMenuEntries(),status);}
    private void command(String path,Object body){
        if(token==null)return;
        // Explicit actions bypass the poll cadence; ServiceRequests serializes them safely.
        final String service=baseUrl,access=token;
        String label=path.equals("/queue/join")?"Joining the queue…":path.equals("/queue/leave")?"Leaving the queue…":"Updating your match…";
        work(true,label,()->api.call(service,access,path,body));
    }
    private void matchCommand(String suffix,Object body){Models.Status current=status;if(current!=null && current.match!=null)command("/match/"+current.match.id+suffix,body);}
    @Override public void queue(String region){
        clientThread.invoke(()->{
            if(!accountMatches || region==null || !ArenaOnboarding.idle(status) || client.getGameState()!=GameState.LOGGED_IN ||
                client.getLocalPlayer()==null || !FightRecorder.normalized(client.getLocalPlayer().getName()).equals(FightRecorder.normalized(status.player.rsn)))return;
            updateHandoff();
            if(!"demo".equals(status.mode) && client.getRevision()!=ArenaSetup.REVISION){
                problem="The Arena interface has changed. Update Ranked NH before queueing.";refresh();return;
            }
            if(!onboarding.canQueue(baseUrl,status,accountMatches)){
                problem="Visit the supplies crate and check your Max/Med kit before joining the queue.";refresh();return;
            }
            configManager.setConfiguration("rankednh","preferredRegion",region);
            command("/queue/join",Map.of("rulesetId","nh-max-126-v1","consent",true,"region",region));
        });
    }
    @Override public void setupHop(String region){clientThread.invoke(()->hopper.requestSetup(region));}
    @Override public void hop(){clientThread.invoke(hopper::request);}
    @Override public void leave(){command("/queue/leave",Map.of());}
    @Override public void accept(boolean accepted){matchCommand("/accept",Map.of("accept",accepted));}
    @Override public void ready(){
        clientThread.invoke(()->{
            Models.Status current=status;if(current==null || current.match==null || client.getGameState()!=GameState.LOGGED_IN || client.getLocalPlayer()==null)return;
            if(!"preparing".equals(current.match.state) || current.match.ready || current.match.world!=client.getWorld() ||
                !client.getWorldType().contains(WorldType.PVP_ARENA) ||
                ArenaCombat.inBattle(client) ||
                client.getBoostedSkillLevel(Skill.HITPOINTS)<=0 || OpponentLocator.find(client,current,accountMatches)==null)return;
            if(!FightRecorder.normalized(client.getLocalPlayer().getName()).equals(FightRecorder.normalized(current.player.rsn))){problem="Log into the enrolled RuneScape account.";refresh();return;}
            matchCommand("/ready",Map.of("round",current.match.round,"world",client.getWorld(),
                "rulesetId",current.match.rulesetId,
                "arenaWorld",client.getWorldType().contains(WorldType.PVP_ARENA)));
        });
    }
    @Override public void forfeit(){matchCommand("/forfeit",Map.of("confirm",true));}
    @Override public void review(){matchCommand("/review",Map.of());}
    @Override public void endTest(){matchCommand("/end-test",Map.of());}
    @Override public void showReport(){configManager.setConfiguration("rankednh","showMatchReport",true);reportOverlay.reopen();}
    @Override public void previewRank(RankPreview scenario){
        clientThread.invoke(()->{
            Models.Status current=status;
            if(current!=null && (current.queued || current.match!=null && current.match.isOpen()))return;
            if(client.getGameState()!=GameState.LOGGED_IN){problem="Log into RuneScape to view the preview.";refresh();return;}
            reportOverlay.preview(scenario);
        });
    }
    @Override public void saveKitSnapshot(){
        clientThread.invoke(()->{
            ArenaSetup.View view=arenaSetup.read(client,status,accountMatches,ArenaCombat.inBattle(client));
            if(view==null || view.kit==null || !view.kit.complete() || !savingKit.compareAndSet(false,true))return;
            // This is an observation for review, never a newly approved default or a gameplay action.
            String now=Instant.now().toString();
            String json=gson.toJson(Map.of("kind","unverified-arena-kit-snapshot","capturedAt",now,
                "equipmentSlots",ArenaKit.EQUIPMENT_SLOTS,"kit",view.kit));
            snapshotMessage="Saving kit snapshot…";refresh();
            executor.execute(()->{
                try{
                    net.runelite.client.util.Filepath folder=getPluginDirectory().join("kit-snapshots");
                    folder.createDirectories();
                    folder.join("max-med-"+now.replace(':','-')+".json").write(json,java.nio.file.StandardOpenOption.CREATE_NEW);
                    snapshotMessage="Diagnostic snapshot saved.";
                }catch(java.io.IOException error){snapshotMessage="Couldn't save the kit snapshot. Try again.";}
                finally{savingKit.set(false);refresh();}
            });
        });
    }
    @Override public void leaderboard(){
        try{LinkBrowser.browse(RankedNhApi.validateUrl(config.serviceUrl())+"/");}
        catch(IllegalArgumentException e){problem=e.getMessage();refresh();}
    }
    @Override public void joinDiscord(){LinkBrowser.browse("https://discord.gg/3fsrUg6VR");}
    @Override public void history(){
        final RankedNhPanel target=panel;final Models.Status current=status;
        if(!running || token==null || target==null || current==null)return;
        final long session=generation.get(),request=target.historyRequest();
        final String service=baseUrl,access=token,playerId=current.player.id;
        // History is read-only and has its own loading state; queue actions stay available.
        executor.execute(()->{
            try{
                String json=api.request(service,access,"/history",null);
                com.google.gson.JsonArray matches=gson.fromJson(json,com.google.gson.JsonObject.class).getAsJsonArray("matches");
                java.util.List<Models.Match> rows=new java.util.ArrayList<>();
                for(com.google.gson.JsonElement value:matches)rows.add(gson.fromJson(value,Models.Match.class));
                SwingUtilities.invokeLater(()->{if(running && generation.get()==session && panel==target)target.showHistory(request,playerId,rows);});
            }catch(java.io.IOException | RuntimeException unavailable){
                SwingUtilities.invokeLater(()->{if(running && generation.get()==session && panel==target)target.historyFailed(request);});
            }
        });
    }
    @Inject private com.google.gson.Gson gson;
    @Subscribe public void onGameTick(GameTick event){
        Models.Status current=status;
        accountMatches=current!=null && client.getGameState()==GameState.LOGGED_IN && client.getLocalPlayer()!=null && FightRecorder.normalized(client.getLocalPlayer().getName()).equals(FightRecorder.normalized(current.player.rsn));
        updateHandoff();
        Models.Report report=recorder.tick(client);
        if(report!=null && current!=null && current.match!=null){report.setup=roundGuard.evidence();pendingMatch=current.match.id;pendingReport=report;reportBlocked=false;poll();}
        hopper.tick();
        if(token!=null && !battleArea && autoReady.take(status,accountMatches,requests.actionPending(),currentWorld,arenaWorld,
            opponentNearby,hitpoints,System.nanoTime()/1_000_000,serverNow()))ready();
        recording=reportBlocked?"Report needs attention — request a review":recorder.message();refresh();
    }
    @Subscribe public void onHitsplatApplied(HitsplatApplied event){if(!roundGuard.blocked())recorder.hit(client,event.getActor(),event.getHitsplat().getAmount());}
    @Subscribe public void onActorDeath(ActorDeath event){recorder.death(client,event.getActor());}
    @Subscribe public void onGameStateChanged(GameStateChanged event){recorder.gameState(event.getGameState());roundGuard.gameState(event.getGameState());if(event.getGameState()!=GameState.LOGGED_IN){accountMatches=false;opponentNearby=false;setupView=null;arenaSetup.reset();onboarding.interrupt();reportOverlay.hide();}if(event.getGameState()==GameState.LOGIN_SCREEN)hopper.cancel();refresh();}
    @Subscribe public void onConfigChanged(ConfigChanged event){
        if("rankednh".equals(event.getGroup()) && ("placementRevealSound".equals(event.getKey()) || "placementRevealVolume".equals(event.getKey()) ||
            "reduceReportMotion".equals(event.getKey()) || "showMatchReport".equals(event.getKey())))reportOverlay.stopSound();
        if("rankednh".equals(event.getGroup()) && "previewMatchup".equals(event.getKey())){
            previewStage=0;refresh();if(config.previewMatchup())openPanel();
        }
        if("rankednh".equals(event.getGroup()) && "previewMatchReport".equals(event.getKey()))reportOverlay.resetPreview();
        if("rankednh".equals(event.getGroup()) && "showMatchReport".equals(event.getKey()) && config.showMatchReport())reportOverlay.reopen();
        if("rankednh".equals(event.getGroup()) && "serviceUrl".equals(event.getKey())){
            rankMenuIcons.cache.clear();rankRetryAfter=0;
            requests.discardQueued();
            pendingSetupIssue=null;pendingSetupMatch=null;submittedSetup=null;battleArea=false;
            clientThread.invoke(roundGuard::reset);
            generation.incrementAndGet();token=null;status=null;serviceRated=null;pendingReport=null;pendingMatch=null;reportBlocked=false;clientThread.invoke(()->{recorder.clear();hopper.cancel();matchAlert.reset();autoReady.reset();});
            problem="Service changed. Connect again with the appropriate access key.";refresh();
            reportOverlay.reset();
        }
    }
}
