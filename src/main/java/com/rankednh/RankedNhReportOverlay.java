package com.rankednh;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.config.ConfigManager;

final class RankedNhReportOverlay extends Overlay
{
    private final RankedNhPlugin plugin;
    private final RankedNhConfig config;
    private final Client client;
    private final MatchReportVisibility visibility;
    private final MatchReportAnimation animation=new MatchReportAnimation();
    private final PlacementRevealAudio revealAudio;
    private final RankSpinImages spins;
    final MatchReportInput input=new MatchReportInput(this::dismiss);
    private boolean previewDismissed;
    private MatchReportView sample;
    private int previewSequence;
    @Inject RankedNhReportOverlay(RankedNhPlugin plugin,RankedNhConfig config,Client client,ConfigManager manager,java.util.concurrent.ScheduledExecutorService executor,net.runelite.client.audio.AudioPlayer audioPlayer) {
        this(plugin,config,client,manager,new PlacementRevealAudio(executor,audioPlayer),new RankSpinImages(executor));
    }
    RankedNhReportOverlay(RankedNhPlugin plugin,RankedNhConfig config,Client client,ConfigManager manager,PlacementRevealAudio revealAudio,RankSpinImages spins) {
        super(plugin);this.plugin=plugin;this.config=config;this.client=client;
        this.revealAudio=revealAudio;
        this.spins=spins;
        ReportSeenStore store=manager==null?null:new ReportSeenStore(
            ()->manager.getConfiguration("rankednh","seenMatchReports"),value->manager.setConfiguration("rankednh","seenMatchReports",value));
        visibility=store==null?new MatchReportVisibility():new MatchReportVisibility(store::contains,store::remember);
        setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ALWAYS_ON_TOP);setPriority(PRIORITY_HIGHEST);
        setResizable(false);setMovable(false);setSnappable(false);setDragTargetable(false);setResettable(false);
    }
    synchronized void reopen(){visibility.reopen();sample=null;previewDismissed=false;animation.reset();revealAudio.reset();spins.clear();input.hide();}
    synchronized void reset(){visibility.reset();sample=null;previewDismissed=false;animation.reset();revealAudio.reset();spins.clear();input.hide();}
    synchronized void resetPreview(){sample=null;previewDismissed=false;animation.reset();revealAudio.reset();spins.clear();input.hide();}
    synchronized void connected(Models.Status status){visibility.connected(status,config.serviceUrl());}
    synchronized void preview(RankPreview scenario){sample=MatchReportView.preview(scenario);previewSequence++;previewDismissed=false;animation.reset();revealAudio.reset();spins.clear();input.hide();}
    synchronized void hide(){input.hide();revealAudio.hide();spins.clear();}
    synchronized void stopSound(){revealAudio.hide();}
    private synchronized void dismiss(){if(sample==null && !config.previewMatchReport())visibility.dismiss();sample=null;previewDismissed=true;hide();}
    // Ignore saved positions from the old movable report, including positions restored at startup.
    @Override public Point getPreferredLocation(){return null;}
    @Override public OverlayPosition getPreferredPosition(){return null;}
    @Override public Dimension getPreferredSize(){return null;}

    @Override public synchronized Dimension render(Graphics2D graphics) {
        // Server review can arrive while the native duel is still running.
        // Do not start the visibility timer or intercept any combat input in the battle area.
        if(plugin.inArenaBattle()){hide();return null;}
        MatchReportView view=null;String key="preview";
        long now=System.nanoTime()/1_000_000;
        if(client.getGameState()==GameState.LOGGED_IN){
            if(sample!=null || config.previewMatchReport()){
                if(!previewDismissed){view=sample==null?MatchReportView.preview():sample;key="preview:"+previewSequence;}
            } else {
                previewDismissed=false;
                Models.Status status=plugin.status();
                MatchReportView candidate=MatchReportView.from(status);
                if(config.showMatchReport() && plugin.accountMatches() && candidate!=null &&
                    visibility.visible(status,config.serviceUrl(),now,candidate.placementReveal?0:config.reportSeconds())){
                    view=candidate;
                    key=status.player.id+":"+status.match.id+":"+status.match.state+":"+status.match.ratingAfter;
                }
            }
        }
        if(view==null){hide();return null;}
        int width=client.getCanvasWidth(),height=client.getCanvasHeight();
        if(width<=24 || height<=24){hide();return null;}
        Rectangle viewport=new Rectangle(client.getViewportXOffset(),client.getViewportYOffset(),client.getViewportWidth(),client.getViewportHeight());
        viewport=viewport.intersection(new Rectangle(0,0,width,height));
        if(viewport.width<=40 || viewport.height<=40){hide();return null;}
        boolean reduced=config.reduceReportMotion();
        MatchReportAnimation.Frame frame=animation.frame(key,view,now,reduced);
        if(!reduced && frame.stillMix<1 && (view.placementReveal || view.promotion()))spins.prepare(view.badge);else spins.clear();
        MatchReportLayout layout=new MatchReportLayout(viewport,frame.entrance);
        Graphics2D g=(Graphics2D)graphics.create();
        try {
            g.translate(-getBounds().x,-getBounds().y);getBounds().setLocation(0,0);
            MatchReportRenderer.render(g,view,frame,layout,input.hovered(),input.focused(),input.pressed(),spins.frame(frame.badge,frame.spin));
        } finally {g.dispose();}
        input.show(layout);
        revealAudio.update(key,view,frame,config.placementRevealSound(),config.placementRevealVolume(),config.reduceReportMotion());
        return new Dimension(width,height);
    }
}
