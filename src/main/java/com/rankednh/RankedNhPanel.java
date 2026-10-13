package com.rankednh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

final class RankedNhPanel extends PluginPanel
{
    interface Actions {
        void connect(char[] key); void queue(String region); void leave(); void accept(boolean accept); void hop();
        void ready(); void forfeit(); void review(); void leaderboard(); void history(); void showReport();
        default void handoffAction(String key,String action){}
        default void saveKitSnapshot(){}
        default void previewRank(RankPreview scenario){}
        default void endTest(){}
        default void joinDiscord(){}
        default void setupHop(String region){}
    }
    static final Color BG=new Color(27,30,34),SURFACE=new Color(35,39,43),TEXT=new Color(231,235,232),
        MUTED=new Color(159,167,164),ACCENT=new Color(155,198,173),LINE=new Color(54,60,60);
    private final JLabel mode=label("Local",10,MUTED),badge=new JLabel(),rank=label("Unranked",17,TEXT),
        rating=label("0 / 5 placements",12,MUTED),record=label("126 NH · Best of three",10,MUTED);
    private final JLabel eyebrow=label("Matchmaking",10,MUTED),state=label("Find a match",16,TEXT),
        opponent=label("",16,TEXT),destination=label("",11,MUTED),score=label("",18,TEXT),
        detail=label("",11,TEXT),hint=label("",10,MUTED),error=label("",11,new Color(231,156,147)),
        observation=label("",11,MUTED);
    private final JProgressBar progress=new JProgressBar(0,5);
    private final JPanel access=column(),regionChoice=column(),match=column(),summary=column();
    private final RegionPicker region;
    private final JLabel regionHint=label("",11,MUTED);
    private final JLabel ratingNotice=label("",10,MUTED);
    private final JLabel seasonNotice=label("",10,MUTED);
    private final JPasswordField key=new JPasswordField();
    private final JCheckBox consent=new JCheckBox("Share match observations");
    private final JButton connect,queue,leave,primary,secondary,forfeit,review,showReport,saveKit,endTest,setupHop;
    private ArenaOnboarding.Step onboardingStep;
    private boolean kitRequired,setupHopBusy;
    private String matchRatingNotice="";
    private String worldGuide="",worldGuideRegion="";
    private final JPanel setupDetails=column();
    private final JLabel kitSaved=label("",11,MUTED);
    private final JLabel requestStatus=label("",11,MUTED);
    private final JPanel rankPreviews=column();
    private final JButton previewToggle;
    private final JComboBox<RankPreview> previewChoice=new JComboBox<>(RankPreview.values());
    private final Actions actions;
    private final JButton historyToggle;
    private final MatchHistoryPanel historyPanel;
    private String historyOwner;
    private String historySeason;
    private MatchupView displayedHandoff;
    private boolean confirmForfeit;

    RankedNhPanel(Actions actions){this(actions,"EU");}
    RankedNhPanel(Actions actions,String preferredRegion){
        this.actions=actions;region=new RegionPicker(preferredRegion);
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBackground(BG);setBorder(BorderFactory.createEmptyBorder(16,12,16,12));
        JPanel heading=new JPanel(new BorderLayout());heading.setOpaque(false);heading.setAlignmentX(LEFT_ALIGNMENT);
        heading.add(label("Ranked NH",14,TEXT),BorderLayout.WEST);heading.add(mode,BorderLayout.EAST);
        heading.setMaximumSize(new Dimension(Integer.MAX_VALUE,24));add(heading);gap(16);
        JPanel rankRow=new JPanel(new BorderLayout(8,0));rankRow.setOpaque(false);rankRow.setAlignmentX(LEFT_ALIGNMENT);
        badge.setIcon(badge("unranked"));rankRow.add(badge,BorderLayout.WEST);
        JPanel numbers=column();numbers.add(rank);numbers.add(Box.createVerticalStrut(4));numbers.add(rating);numbers.add(Box.createVerticalStrut(4));numbers.add(record);
        rankRow.add(numbers,BorderLayout.CENTER);rankRow.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));summary.add(rankRow);
        summary.add(Box.createVerticalStrut(12));progress.setForeground(ACCENT);progress.setBackground(LINE);progress.setBorderPainted(false);
        progress.setMinimumSize(new Dimension(1,3));progress.setPreferredSize(new Dimension(1,3));
        progress.setAlignmentX(LEFT_ALIGNMENT);progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,3));summary.add(progress);
        summary.add(Box.createVerticalStrut(6));summary.add(ratingNotice);summary.add(seasonNotice);add(summary);gap(24);

        match.setBackground(SURFACE);match.setOpaque(true);
        match.setBorder(BorderFactory.createEmptyBorder(16,12,12,12));
        match.add(eyebrow);match.add(Box.createVerticalStrut(8));match.add(state);match.add(Box.createVerticalStrut(12));
        opponent.setIconTextGap(6);match.add(opponent);match.add(Box.createVerticalStrut(8));
        destination.setOpaque(true);destination.setBackground(BG);destination.setIconTextGap(5);
        destination.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(4,6,4,6)));
        JPanel meta=column();meta.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
        meta.add(destination);meta.add(Box.createHorizontalStrut(10));meta.add(score);match.add(meta);
        match.add(Box.createVerticalStrut(12));match.add(detail);match.add(Box.createVerticalStrut(4));match.add(hint);
        match.add(setupDetails);
        match.add(Box.createVerticalStrut(8));match.add(requestStatus);match.add(error);
        primary=button("Accept match",true,()->handoff(false));secondary=button("Decline",false,()->handoff(true));
        match.add(primary);match.add(secondary);
        setupHop=button("Hop to Arena",true,()->actions.setupHop(region.selected()));match.add(setupHop);
        add(match);gap(16);

        access.add(label("Pilot access key",12,TEXT));access.add(Box.createVerticalStrut(8));
        key.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));key.setBackground(SURFACE);key.setForeground(TEXT);key.setCaretColor(TEXT);
        key.getAccessibleContext().setAccessibleName("Pilot access key");access.add(key);access.add(Box.createVerticalStrut(8));
        consent.setOpaque(false);consent.setForeground(MUTED);consent.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));consent.setAlignmentX(LEFT_ALIGNMENT);
        consent.setToolTipText("Only your enrolled duel: participants, result, world, timing and damage. No chat or credentials.");
        access.add(consent);connect=button("Connect",true,()->{if(consent.isSelected()){actions.connect(key.getPassword());key.setText("");}});
        connect.setEnabled(false);consent.addActionListener(e->connect.setEnabled(consent.isSelected()));access.add(connect);add(access);

        regionChoice.add(label("Preferred region",12,TEXT));regionChoice.add(Box.createVerticalStrut(8));
        regionChoice.add(region);
        regionChoice.add(Box.createVerticalStrut(8));regionChoice.add(regionHint);add(regionChoice);
        queue=button("Find a match",true,()->actions.queue(region.selected()));add(queue);
        leave=button("Cancel search",false,actions::leave);add(leave);
        showReport=button("Show match report",false,actions::showReport);add(showReport);
        saveKit=button("Save kit snapshot",false,actions::saveKitSnapshot);add(saveKit);add(kitSaved);
        forfeit=button("Forfeit match",false,()->{if(confirmForfeit){confirmForfeit=false;actions.forfeit();}else{confirmForfeit=true;forfeitText();}});add(forfeit);
        review=button("Request a review",false,actions::review);add(review);
        endTest=button("End test match",false,actions::endTest);add(endTest);
        gap(12);add(observation);gap(16);
        add(button("Leaderboard ↗",false,actions::leaderboard));
        historyToggle=button("Match history",false,this::toggleHistory);add(historyToggle);
        historyPanel=new MatchHistoryPanel(this::historyClosed,actions::history);add(historyPanel);
        JButton discord=button("Join Discord",false,actions::joinDiscord);
        discord.setIcon(new ImageIcon(ImageUtil.resizeImage(ImageUtil.loadImageResource(RankedNhPanel.class,"discord.png"),16,16)));
        discord.setIconTextGap(8);discord.setToolTipText("Open the Ranked NH Discord in your browser");add(discord);
        previewToggle=button("Preview ranks",false,()->{rankPreviews.setVisible(!rankPreviews.isVisible());finish();});add(previewToggle);
        previewChoice.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));previewChoice.setAlignmentX(LEFT_ALIGNMENT);
        previewChoice.getAccessibleContext().setAccessibleName("Rank preview scenario");rankPreviews.add(previewChoice);
        rankPreviews.add(button("Play preview",false,()->actions.previewRank((RankPreview)previewChoice.getSelectedItem())));
        rankPreviews.setVisible(false);add(rankPreviews);add(Box.createVerticalGlue());
        render(null,"",false,false,"",null);
    }
    private void handoff(boolean other){MatchupView view=displayedHandoff;if(view!=null)actions.handoffAction(view.key,other?view.secondaryAction:view.action);}
    private void forfeitText(){forfeit.setText(confirmForfeit?"Confirm forfeit":"Forfeit match");}
    private static ImageIcon badge(String name){return new ImageIcon(ImageUtil.resizeImage(BadgeImages.image(name),56,56));}
    private static JPanel column(){
        JPanel p=new JPanel(){@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}};
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setOpaque(false);p.setAlignmentX(LEFT_ALIGNMENT);return p;
    }
    private void gap(int height){add(Box.createVerticalStrut(height));}
    private static JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,size));l.setForeground(color);l.setAlignmentX(LEFT_ALIGNMENT);return l;}
    private JButton button(String title,boolean isPrimary,Runnable action){
        JButton b=new JButton(title);b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));b.setOpaque(false);b.setContentAreaFilled(false);b.setFocusPainted(false);
        b.setAlignmentX(LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.addActionListener(e->action.run());
        Runnable style=()->{
            boolean enabled=b.isEnabled(),hover=b.getModel().isRollover(),pressed=b.getModel().isPressed();
            Color fill=isPrimary?new Color(49,69,58):SURFACE;
            if(enabled && hover)fill=isPrimary?new Color(61,84,71):new Color(44,50,53);
            if(enabled && pressed)fill=isPrimary?new Color(39,57,47):BG;
            b.setBackground(fill);b.setOpaque(true);b.setForeground(enabled?(isPrimary?ACCENT:TEXT):MUTED);
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(8,0,0,0,b.getParent()==match?SURFACE:BG),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(b.hasFocus()?ACCENT:isPrimary?new Color(65,89,74):LINE),BorderFactory.createEmptyBorder(8,6,8,6))));
        };
        b.getModel().addChangeListener(e->style.run());
        b.addFocusListener(new java.awt.event.FocusAdapter(){@Override public void focusGained(java.awt.event.FocusEvent e){style.run();}@Override public void focusLost(java.awt.event.FocusEvent e){style.run();}});
        style.run();return b;
    }
    static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static String html(String value){return "<html><div style='width:126px'>"+value+"</div></html>";}
    private static void copy(JLabel label,String text){label.setText(text==null || text.isEmpty()?"":html(escape(text).replace("\n","<br>")));label.setVisible(text!=null && !text.isEmpty());}
    private void toggleHistory(){
        if(historyPanel.isVisible()){historyPanel.close();historyClosed();}
        else{historyPanel.open();historyToggle.setText("Hide match history");actions.history();finish();}
    }
    private void historyClosed(){historyToggle.setText("Match history");finish();}
    long historyRequest(){return historyPanel.request();}
    void showHistory(long request,String playerId,java.util.List<Models.Match> matches){historyPanel.show(request,playerId,matches);finish();}
    void historyFailed(long request){historyPanel.failed(request);finish();}
    void render(Models.Status s,String problem,boolean busy,boolean accountMatches,String recording){render(s,problem,busy,accountMatches,recording,null);}
    void render(Models.Status s,String problem,boolean busy,boolean accountMatches,String recording,MatchupView handoff){
        render(s,problem,busy,accountMatches,recording,handoff,System.currentTimeMillis());
    }
    void render(Models.Status s,String problem,boolean busy,boolean accountMatches,String recording,MatchupView handoff,long serverNow){
        setupDetails.setVisible(false);endTest.setVisible(false);endTest.setEnabled(!busy && accountMatches);
        saveKit.setVisible(false);kitSaved.setVisible(false);requestStatus.setVisible(false);detail.setForeground(TEXT);
        connect.setEnabled(!busy && consent.isSelected());
        displayedHandoff=handoff;boolean preview=handoff!=null && handoff.preview,connected=s!=null;
        String owner=s==null || s.player==null?null:s.player.id;
        String season=s==null || s.season==null?null:s.season.id;
        if(!java.util.Objects.equals(owner,historyOwner) || !java.util.Objects.equals(season,historySeason)){
            historyOwner=owner;historySeason=season;historyPanel.close();historyClosed();
        }
        historyToggle.setEnabled(connected);historyToggle.setToolTipText(connected?"Your 20 most recent matches":"Connect to view your match history");
        boolean canPreview=s==null || !s.queued && (s.match==null || !s.match.isOpen());
        previewToggle.setEnabled(canPreview);previewToggle.setToolTipText(canPreview?"Sample animations; your rating stays unchanged.":"Available between matches. Finish or leave your match first.");
        if(!canPreview)rankPreviews.setVisible(false);
        mode.setText(preview?"Preview":!connected || "demo".equals(s.mode)?"Local":s.season!=null && s.season.beta?"Beta season":"Pilot");
        copy(seasonNotice,connected && !preview && s.season!=null && s.season.resetNotice!=null?s.season.resetNotice.replace("Season 1","Season\u00a01"):"");
        matchRatingNotice=connected && !preview && s.match!=null && s.match.isOpen() && !s.match.rated?
            (s.match.reason!=null && s.match.reason.contains("Daily rated limit")?
                "Daily limit for this opponent. This match is unrated.":"Unrated match. Placement progress stays unchanged."):"";
        renderRatingAvailability(connected && !preview?s.rated:null);
        access.setVisible(!connected && !preview);
        boolean choosingRegion=connected && !preview && !s.queued && (s.match==null || !s.match.isOpen());
        regionChoice.setVisible(choosingRegion);queue.setVisible(choosingRegion);leave.setVisible(connected && !preview && s.queued);
        setupHop.setVisible(choosingRegion && kitRequired && onboardingStep==ArenaOnboarding.Step.HOP);
        setupHop.setText(setupHopBusy?"Hopping…":"Hop to Arena");
        queue.setToolTipText(kitRequired?"Check your saved Max/Med kit at the supplies crate first.":null);
        queue.setEnabled(!busy && accountMatches && !kitRequired);leave.setEnabled(!busy && accountMatches);
        for(JButton b:new JButton[]{primary,secondary,forfeit,review,showReport}){b.setVisible(false);b.setEnabled(!busy && accountMatches);}
        for(JLabel l:new JLabel[]{opponent,destination,score,detail,hint,error,observation})l.setVisible(false);
        eyebrow.setText(preview?"Sample match":"Matchmaking");state.setText("Find a match");state.setForeground(TEXT);
        if(connected){
            Models.Player p=s.player;rank.setText(p.rank.name);badge.setIcon(badge(p.rank.badge));
            boolean placed=p.games>=5;rating.setText(placed?(int)Math.floor(p.rating)+" rating":Math.min(5,p.games)+" / 5 placements");
            record.setText(p.wins+" W · "+(p.games-p.wins)+" L");
            int width=placed && p.rank.nextAt!=null && p.rank.divisionStart!=null?Math.max(1,p.rank.nextAt-p.rank.divisionStart):60;
            progress.setMaximum(placed?width:5);
            progress.setValue(placed?(p.rank.nextAt==null?width:Math.max(0,width-(p.rank.nextAt-(int)p.rating))):p.games);
        }
        if(preview){renderHandoff(handoff);copy(hint,"Sample match");finish();return;}
        copy(error,problem);
        if(!connected){copy(detail,"Connect to find a 126 NH opponent.");copy(hint,"Best of three · Max gear");finish();return;}
        region.update(s.regions,!busy);
        setupHop.setEnabled(!busy && accountMatches && !setupHopBusy && region.selected()!=null);
        copy(regionHint,"DEMO".equals(region.selected())?"Local simulation only.":QueueRegionHint.preference(s.crossRegionAfterSeconds));
        Models.Match m=s.match;
        endTest.setVisible(m!=null && !m.rated && "review".equals(m.state));
        endTest.setToolTipText("End this unrated test and return to the queue. Reports are retained; ratings stay unchanged.");
        showReport.setVisible(!s.queued && MatchReportView.reportable(s));
        showReport.setEnabled(true);
        if(s.queued){
            state.setText("Finding a match");destination(s.queueRegion,0);copy(detail,"We'll notify you when a match is ready.");
            copy(hint,QueueRegionHint.searching(s,serverNow));
        }else if(m==null || !m.isOpen()){
            copy(detail,"126 combat · Max gear");copy(hint,"First to two fight wins.");
            if(m!=null && "complete".equals(m.state)){
                state.setText(s.player.id.equals(m.winner)?"Match won":"Match lost");opponent(m.opponent.rsn,m.opponent.rank);score(m.score);destination(m.region,m.world);
                copy(detail,m.ratingChange==null?"Unrated match":(m.ratingChange>=0?"+":"")+m.ratingChange+" rating");
            }else if(m!=null && m.reason!=null)copy(hint,m.reason);
            queue.setEnabled(!busy && accountMatches && !kitRequired && s.cooldownUntil==null && region.selected()!=null);
            QueueCooldown cooldown=QueueCooldown.from(s,serverNow,java.time.ZoneId.systemDefault());
            if(cooldown!=null){state.setText("Queue cooldown");copy(detail,cooldown.remaining);copy(hint,cooldown.detail);}
        }else if(handoff!=null){
            renderHandoff(handoff);
            if(!"offered".equals(m.state)){score(m.score);forfeit.setVisible(true);}
            review.setVisible("active".equals(m.state));
        }else{
            state.setText("review".equals(m.state)?"Under review":"reporting".equals(m.state)?"Checking result":"Match ready");
            opponent(m.opponent.rsn,m.opponent.rank);destination(m.region,m.world);
            copy(detail,"reporting".equals(m.state)?"Waiting for both round reports.":m.reason);
            if("reporting".equals(m.state)){
                copy(detail,m.reported?"Your round report was received.":"Your opponent reported the round ending.");
                copy(hint,m.reported?"Waiting for your opponent's client. Don't start the next duel yet.":"Waiting for this client's report. Don't start the next duel yet.");
            }
            if("review".equals(m.state) && !m.rated)copy(hint,"Return to the lobby, then End test match to try again.");
            forfeit.setVisible("reporting".equals(m.state));review.setVisible("reporting".equals(m.state));
        }
        if(!accountMatches)copy(observation,"Log into "+s.player.rsn+" to play.");
        else if(m!=null && (recording.contains("review") || recording.contains("interrupted")))copy(observation,recording);
        if(m==null || !m.isOpen())confirmForfeit=false;forfeitText();finish();
    }
    private void renderHandoff(MatchupView view){
        String action=view.action;
        boolean offer="accept".equals(action) || view.preview && view.key.endsWith(":0");
        state.setText(html(escape(view.heading)));
        state.setForeground(offer?ACCENT:TEXT);opponent(view.opponent,view.opponentRank);destination(view.region,view.world);
        copy(detail,view.message);copy(hint,view.footer);
        if(!"panel".equals(action)){primary.setText(view.primary);primary.setVisible(true);primary.setEnabled(view.enabled);}
        if(!"panel".equals(view.secondaryAction) && !view.secondary.isEmpty()){secondary.setText(view.secondary);secondary.setVisible(true);secondary.setEnabled(view.secondaryEnabled);}
    }
    void renderSetup(ArenaSetup.View view,String saved,boolean saving){
        renderSetup(view,saved,saving,false);
    }
    void renderSetup(ArenaSetup.View view,String saved,boolean saving,boolean diagnostics){
        if(view==null)return;
        eyebrow.setText("Arena setup");state.setText(html(escape(view.heading)));state.setForeground(view.warning?ArenaSetupWarningOverlay.WARNING:TEXT);
        copy(detail,view.message);detail.setForeground(view.warning?ArenaSetupWarningOverlay.WARNING:TEXT);copy(hint,view.detail);
        primary.setVisible(false);secondary.setVisible(false);
        setupDetails.removeAll();setupDetails.setVisible(!view.changes.isEmpty());
        if(!view.changes.isEmpty()){
            state.setText(html(escape(view.changes.size()==1?view.changes.get(0).location+" differs":view.changes.size()+" kit differences")));
            copy(detail,view.changes.size()>1?"Start here. The next fix appears once this is corrected.":"");
            ArenaKit.Change first=view.changes.get(0);
            copy(hint,"Spellbook".equals(first.location)?"Use the spellbook selector above the kit.":
                "Rune pouch".equals(first.location)?"Open the rune pouch to check enabled runes.":
                "Counts include worn and inventory items. Hover red marks for details.");
            for(ArenaKit.Change change:view.changes.subList(0,1)){
                setupDetails.add(Box.createVerticalStrut(8));
                JLabel location=label(change.title(),11,TEXT);setupDetails.add(location);
                JLabel items=label("",11,MUTED);copy(items,change.actualLabel+": "+change.yours+"\n"+change.expectedLabel+": "+change.theirs);setupDetails.add(items);
            }
            if(view.changes.size()>1)setupDetails.add(label("+ "+(view.changes.size()-1)+" other fixes",10,MUTED));
            JLabel recovery=label("",10,MUTED);copy(recovery,view.detail);
            setupDetails.add(Box.createVerticalStrut(8));setupDetails.add(recovery);
        }
        saveKit.setVisible(diagnostics && view.kit!=null && view.kit.complete());saveKit.setEnabled(!saving);
        copy(kitSaved,diagnostics?saved:"");finish();
    }
    void onboarding(ArenaOnboarding.Step step,boolean required,boolean hopping){
        onboardingStep=step;kitRequired=required;setupHopBusy=hopping;
    }
    void onboardingGuide(String instruction,String region){worldGuide=instruction;worldGuideRegion=region;}
    void renderOnboarding(ArenaSetup.View setup){
        if(!kitRequired)return;
        eyebrow.setText("First-time setup");
        if(onboardingStep==ArenaOnboarding.Step.FIX || onboardingStep==ArenaOnboarding.Step.MAX_MED)return;
        state.setForeground(TEXT);
        if(onboardingStep==ArenaOnboarding.Step.HOP){
            state.setText("Visit the supplies crate");
            copy(detail,!worldGuide.isEmpty() && java.util.Objects.equals(worldGuideRegion,region.selected())?worldGuide:
                "Hop to a PvP Arena world in your selected region to check your Max/Med kit.");
            copy(hint,"A one-time check for this character before matchmaking.");
        }else if(onboardingStep==ArenaOnboarding.Step.CRATE){
            state.setText("Open the supplies crate");
            copy(detail,"Open Supplies in the lobby, then choose Max/Med kit.");
            copy(hint,"We'll check your items, spellbook and runes. Your item order is up to you.");
        }else if(onboardingStep==ArenaOnboarding.Step.CHECKING){
            state.setText("Checking your saved kit");
            copy(detail,"Keep Max/Med kit open for a moment.");copy(hint,"Matchmaking unlocks automatically when the check passes.");
        }
        finish();
    }
    void renderRatingAvailability(Boolean rated){
        copy(ratingNotice,Boolean.FALSE.equals(rated)?"Placements paused. Matches are unrated.":matchRatingNotice);
    }
    void renderRoundIssue(String reason){
        state.setText("Round not recorded");copy(detail,reason);detail.setForeground(new Color(231,174,136));
        copy(hint,"Return to the lobby and fix the setup before trying again.");finish();
    }
    private void opponent(String name,Models.Rank rank){
        opponent.setText(name);opponent.setVisible(true);
        opponent.setIcon(new ImageIcon(ImageUtil.resizeImage(BadgeImages.image(rank==null?"unranked":rank.badge),20,20)));
        opponent.setToolTipText(rank==null?"Unranked":rank.name);
    }
    private void destination(String region,int world){
        String label=region==null?"Arena":region;
        destination.setText(label+(world>0?" · "+world:""));destination.setIcon(new RegionFlag(label));destination.setVisible(true);
        destination.setToolTipText(MatchFoundAlert.region(label)+(world>0?" · World "+world:" preferred"));
    }
    private void score(int[] values){
        if(values==null || values.length<2)return;
        String yours=values[0]>values[1]?"#9bc6ad":values[0]<values[1]?"#e79b88":"#e7ebe8";
        String theirs=values[1]>values[0]?"#9bc6ad":values[1]<values[0]?"#e79b88":"#e7ebe8";
        score.setText("<html><span style='color:"+yours+"'>"+values[0]+"</span><span style='color:#9fa7a4'> – </span><span style='color:"+theirs+"'>"+values[1]+"</span></html>");
        score.setToolTipText("You "+values[0]+" · Opponent "+values[1]);score.getAccessibleContext().setAccessibleName(score.getToolTipText());score.setVisible(true);
    }
    void renderRequest(String message){copy(requestStatus,message);finish();}
    private void finish(){revalidate();repaint();}
}
