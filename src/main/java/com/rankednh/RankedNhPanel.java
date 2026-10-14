package com.rankednh;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
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
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
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
        default void filterCatalog(boolean enabled){}
        default void redoKitSetup(){}
    }
    static final Color BG=new Color(27,30,34),SURFACE=new Color(35,39,43),TEXT=new Color(231,235,232),
        MUTED=new Color(159,167,164),ACCENT=new Color(0xEC0404),SUCCESS=new Color(155,198,173),LINE=new Color(54,60,60),
        ACCENT_SURFACE=ACCENT,ACCENT_HOVER=new Color(211,4,4),ACCENT_PRESSED=new Color(179,3,3);
    private final JLabel mode=label("Local",10,MUTED),badge=new JLabel(),rank=label("Unranked",17,TEXT),
        rating=label("0 / 5 placements",12,MUTED),record=label("126 NH · Best of three",10,MUTED);
    private final JLabel eyebrow=label("Matchmaking",10,MUTED),state=label("Find a match",16,TEXT),
        opponent=label("",16,TEXT),destination=label("",11,MUTED),score=label("",18,TEXT),
        detail=label("",11,TEXT),hint=label("",10,MUTED),error=label("",11,new Color(231,156,147)),
        observation=label("",11,MUTED);
    private final JProgressBar progress=new JProgressBar(0,5);
    private final JPanel access=column(),regionChoice=column(),match=column(),summary=column(),matchCard=matchmakingCard(),matchBody=column(),matchActions=column(),queueIntro=column(),utilities=column();
    private final JPanel opponentRow=spaced(opponent,12),detailRow=spaced(detail,12),hintRow=spaced(hint,4),metaRow=column();
    private final java.awt.Component eyebrowGap=Box.createVerticalStrut(8),scoreGap=Box.createHorizontalStrut(10),
        travelMetaGap=Box.createVerticalStrut(8);
    private boolean travelCard;
    private final QueueSearchPanel search=new QueueSearchPanel();
    private final java.awt.Component matchControlsGap=Box.createVerticalStrut(16);
    private boolean queueActive;
    private final RegionPicker region;
    private final JLabel ratingNotice=label("",10,MUTED);
    private final JLabel seasonNotice=label("",10,MUTED);
    private final JLabel serverNotice=label("",11,new Color(231,174,136));
    private boolean localServer;
    private final JPasswordField key=new JPasswordField();
    private final JCheckBox consent=new JCheckBox("Share match observations");
    private final JPanel catalogOptions=column();
    private final JCheckBox catalogFilter=new JCheckBox("Allowed & missing only",true){
        @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
    };
    private final JLabel catalogNotice=label("",10,new Color(139,148,146));
    private final JButton connect,queue,leave,primary,secondary,forfeit,review,showReport,saveKit,endTest,setupHop,redoKit;
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
        summary.add(Box.createVerticalStrut(12));progress.setForeground(SUCCESS);progress.setBackground(LINE);progress.setBorderPainted(false);
        progress.setMinimumSize(new Dimension(1,3));progress.setPreferredSize(new Dimension(1,3));
        progress.setAlignmentX(LEFT_ALIGNMENT);progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,3));summary.add(progress);
        summary.add(Box.createVerticalStrut(6));summary.add(ratingNotice);summary.add(seasonNotice);summary.add(serverNotice);add(summary);gap(24);

        matchCard.setBackground(SURFACE);matchCard.setOpaque(true);
        matchCard.setBorder(BorderFactory.createEmptyBorder(16,12,16,12));
        matchCard.add(matchBody,BorderLayout.CENTER);matchCard.add(matchActions,BorderLayout.SOUTH);
        queueIntro.add(center(label("Ranked 1v1",16,TEXT)));queueIntro.add(Box.createVerticalStrut(6));
        queueIntro.add(center(label("126 NH · Best of three",11,MUTED)));queueIntro.add(Box.createVerticalStrut(16));
        JPanel divider=new JPanel();divider.setBackground(LINE);divider.setPreferredSize(new Dimension(1,1));divider.setMaximumSize(new Dimension(Integer.MAX_VALUE,1));
        divider.setAlignmentX(LEFT_ALIGNMENT);queueIntro.add(divider);queueIntro.add(Box.createVerticalStrut(16));
        matchBody.add(queueIntro);matchBody.add(search);matchBody.add(match);
        for(JLabel text:new JLabel[]{eyebrow,state,opponent,detail,hint,error,requestStatus,catalogNotice})center(text);
        match.add(eyebrow);match.add(eyebrowGap);match.add(state);
        opponent.setIconTextGap(6);match.add(opponentRow);
        destination.setOpaque(true);destination.setBackground(BG);destination.setIconTextGap(5);
        destination.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),BorderFactory.createEmptyBorder(4,6,4,6)));
        JPanel meta=column();meta.setLayout(new FlowLayout(FlowLayout.CENTER,0,0));
        meta.add(destination);meta.add(scoreGap);meta.add(score);
        metaRow.add(travelMetaGap);metaRow.add(Box.createVerticalStrut(8));metaRow.add(meta);match.add(metaRow);
        match.add(detailRow);match.add(hintRow);
        match.add(setupDetails);
        catalogOptions.setBorder(BorderFactory.createEmptyBorder(12,0,0,0));
        catalogFilter.setOpaque(false);catalogFilter.setForeground(TEXT);catalogFilter.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,11));
        catalogFilter.setAlignmentX(LEFT_ALIGNMENT);catalogFilter.setHorizontalAlignment(JCheckBox.CENTER);
        catalogFilter.setBorder(BorderFactory.createEmptyBorder());catalogFilter.setIconTextGap(6);
        catalogFilter.setToolTipText("Show approved items below their quantity limit. Missing items are optional.");
        catalogFilter.addActionListener(e->actions.filterCatalog(catalogFilter.isSelected()));catalogOptions.add(catalogFilter);
        catalogNotice.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));catalogOptions.add(catalogNotice);match.add(catalogOptions);

        primary=button("Accept match",true,()->handoff(false));secondary=button("Decline",false,()->handoff(true));
        matchActions.add(primary);matchActions.add(secondary);
        setupHop=button("Open world switcher",true,()->actions.setupHop(region.selected()));matchActions.add(setupHop);
        add(matchCard);gap(16);

        access.add(label("Pilot access key",12,TEXT));access.add(Box.createVerticalStrut(8));
        key.setMaximumSize(new Dimension(Integer.MAX_VALUE,34));key.setBackground(SURFACE);key.setForeground(TEXT);key.setCaretColor(TEXT);
        key.getAccessibleContext().setAccessibleName("Pilot access key");access.add(key);access.add(Box.createVerticalStrut(8));
        consent.setOpaque(false);consent.setForeground(MUTED);consent.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,10));consent.setAlignmentX(LEFT_ALIGNMENT);
        consent.setToolTipText("Only your enrolled duel: participants, result, world, timing and damage. No chat or credentials.");
        access.add(consent);connect=button("Connect",true,()->{if(consent.isSelected()){actions.connect(key.getPassword());key.setText("");}});
        connect.setEnabled(false);consent.addActionListener(e->connect.setEnabled(consent.isSelected()));access.add(connect);add(access);

        matchBody.add(matchControlsGap);
        regionChoice.add(center(label("Preferred region",12,TEXT)));regionChoice.add(Box.createVerticalStrut(8));
        regionChoice.add(region);
        matchBody.add(regionChoice);
        queue=button("Find a match",true,()->actions.queue(region.selected()));matchActions.add(queue);
        leave=button("Cancel search",false,actions::leave);matchActions.add(leave);
        requestStatus.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));error.setBorder(BorderFactory.createEmptyBorder(8,0,0,0));
        matchActions.add(requestStatus);matchActions.add(error);
        showReport=button("Show match report",false,actions::showReport);add(showReport);
        saveKit=button("Save kit snapshot",false,actions::saveKitSnapshot);add(saveKit);add(kitSaved);
        forfeit=button("Forfeit match",false,()->{if(confirmForfeit){confirmForfeit=false;actions.forfeit();}else{confirmForfeit=true;forfeitText();}});add(forfeit);
        review=button("Request a review",false,actions::review);add(review);
        endTest=button("End test match",false,actions::endTest);add(endTest);
        gap(12);add(observation);gap(16);
        add(utilities);utilities.add(button("Leaderboard ↗",false,actions::leaderboard));
        historyToggle=button("Match history",false,this::toggleHistory);utilities.add(historyToggle);
        historyPanel=new MatchHistoryPanel(this::historyClosed,actions::history);utilities.add(historyPanel);
        JButton discord=button("Join Discord",false,actions::joinDiscord);
        discord.setIcon(new ImageIcon(ImageUtil.resizeImage(ImageUtil.loadImageResource(RankedNhPanel.class,"discord.png"),16,16)));
        discord.setIconTextGap(8);discord.setToolTipText("Open the Ranked NH Discord in your browser");utilities.add(discord);
        previewToggle=button("Preview ranks",false,()->{rankPreviews.setVisible(!rankPreviews.isVisible());finish();});utilities.add(previewToggle);
        previewChoice.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));previewChoice.setAlignmentX(LEFT_ALIGNMENT);
        previewChoice.getAccessibleContext().setAccessibleName("Rank preview scenario");rankPreviews.add(previewChoice);
        rankPreviews.add(button("Play preview",false,()->actions.previewRank((RankPreview)previewChoice.getSelectedItem())));
        rankPreviews.setVisible(false);utilities.add(rankPreviews);
        redoKit=button("Redo kit setup",false,actions::redoKitSetup);
        redoKit.setToolTipText("Replay the supplies-crate check for this character. Rating and match history stay unchanged.");
        utilities.add(redoKit);add(Box.createVerticalGlue());
        render(null,"",false,false,"",null);
    }
    private void handoff(boolean other){MatchupView view=displayedHandoff;if(view!=null)actions.handoffAction(view.key,other?view.secondaryAction:view.action);}
    private void forfeitText(){forfeit.setText(confirmForfeit?"Confirm forfeit":"Forfeit match");}
    private static ImageIcon badge(String name){return new ImageIcon(ImageUtil.resizeImage(BadgeImages.image(name),56,56));}
    private static JPanel matchmakingCard(){
        // Buttons include 8px above their face; the layout adds the other 8px.
        JPanel card=new JPanel(new BorderLayout(0,8)){
            @Override public Dimension getMinimumSize(){return new Dimension(0,getPreferredSize().height);}
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };
        card.setAlignmentX(LEFT_ALIGNMENT);return card;
    }
    private static JPanel column(){
        JPanel p=new JPanel(){@Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}};
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setOpaque(false);p.setAlignmentX(LEFT_ALIGNMENT);return p;
    }
    private static JPanel spaced(java.awt.Component child,int top){JPanel row=column();row.add(Box.createVerticalStrut(top));row.add(child);return row;}
    private void gap(int height){add(Box.createVerticalStrut(height));}
    private static JLabel label(String text,int size,Color color){
        JLabel l=new JLabel(text){
            @Override public Dimension getPreferredSize(){
                View view=(View)getClientProperty(BasicHTML.propertyKey);
                if(Boolean.TRUE.equals(getClientProperty("centeredCopy")) && view!=null){
                    int width=copyWidth(this);
                    view.setSize(Math.max(1,width-getInsets().left-getInsets().right),0);
                    return new Dimension(width,(int)Math.ceil(view.getPreferredSpan(View.Y_AXIS))+getInsets().top+getInsets().bottom);
                }
                return super.getPreferredSize();
            }
            @Override public Dimension getMinimumSize(){
                return Boolean.TRUE.equals(getClientProperty("centeredCopy"))?new Dimension(0,getPreferredSize().height):super.getMinimumSize();
            }
            @Override public Dimension getMaximumSize(){
                return Boolean.TRUE.equals(getClientProperty("centeredCopy"))?new Dimension(Integer.MAX_VALUE,getPreferredSize().height):super.getMaximumSize();
            }
            @Override public void setBounds(int x,int y,int width,int height){
                boolean resized=width!=getWidth();super.setBounds(x,y,width,height);
                if(resized && Boolean.TRUE.equals(getClientProperty("centeredCopy")))revalidate();
            }
        };
        l.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,size));l.setForeground(color);l.setAlignmentX(LEFT_ALIGNMENT);return l;
    }
    private static int copyWidth(JLabel label){
        // Measure from the sidebar so nested rows wrap before their first layout.
        int padding=0;
        for(Container parent=label.getParent();parent!=null;parent=parent.getParent()){
            padding+=parent.getInsets().left+parent.getInsets().right;
            if(parent instanceof RankedNhPanel && parent.getWidth()>0)return Math.max(1,parent.getWidth()-padding);
        }
        return 168;
    }
    private static JLabel center(JLabel label){label.putClientProperty("centeredCopy",true);label.setHorizontalAlignment(JLabel.CENTER);return label;}
    private JButton button(String title,boolean isPrimary,Runnable action){
        JButton b=new JButton(title);b.setFont(new Font(Font.SANS_SERIF,Font.BOLD,12));b.setOpaque(false);b.setContentAreaFilled(true);b.setFocusPainted(false);
        b.setAlignmentX(LEFT_ALIGNMENT);b.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));b.addActionListener(e->action.run());
        Runnable style=()->{
            boolean enabled=b.isEnabled(),hover=b.getModel().isRollover(),pressed=b.getModel().isPressed();
            Color fill=enabled && isPrimary?ACCENT_SURFACE:SURFACE;
            if(enabled && hover)fill=isPrimary?ACCENT_HOVER:new Color(44,50,53);
            if(enabled && pressed)fill=isPrimary?ACCENT_PRESSED:BG;
            b.setBackground(fill);b.setOpaque(true);b.setForeground(enabled?(isPrimary?Color.WHITE:TEXT):MUTED);
            b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(8,0,0,0,b.getParent()==matchActions?SURFACE:BG),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(enabled && b.hasFocus()?TEXT:enabled && isPrimary?fill:LINE),BorderFactory.createEmptyBorder(8,6,8,6))));
        };
        b.getModel().addChangeListener(e->style.run());
        b.addFocusListener(new java.awt.event.FocusAdapter(){@Override public void focusGained(java.awt.event.FocusEvent e){style.run();}@Override public void focusLost(java.awt.event.FocusEvent e){style.run();}});
        style.run();return b;
    }
    static String escape(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static String html(String value){return html(value,false);}
    private static String html(String value,boolean centered){return "<html><div style='"+(centered?"text-align:center":"width:126px")+"'>"+value+"</div></html>";}
    private static void copy(JLabel label,String text){label.setText(text==null || text.isEmpty()?"":html(escape(text).replace("\n","<br>"),Boolean.TRUE.equals(label.getClientProperty("centeredCopy"))));label.setVisible(text!=null && !text.isEmpty());}
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
        setupDetails.setVisible(false);catalogFilter.setVisible(false);catalogNotice.setVisible(false);endTest.setVisible(false);endTest.setEnabled(!busy && accountMatches);
        saveKit.setVisible(false);kitSaved.setVisible(false);requestStatus.setVisible(false);detail.setForeground(TEXT);
        connect.setEnabled(!busy && consent.isSelected());
        displayedHandoff=handoff;boolean preview=handoff!=null && handoff.preview,connected=s!=null;
        travelCard=false;queueActive=connected && !preview && s.queued;
        boolean idle=connected && !preview && !s.queued && (s.match==null || !s.match.isOpen()) && !kitRequired && s.cooldownUntil==null;
        queueIntro.setVisible(idle);match.setVisible(!idle && !queueActive);
        search.update(queueActive?s:null,serverNow);
        String owner=s==null || s.player==null?null:s.player.id;
        String season=s==null || s.season==null?null:s.season.id;
        if(!java.util.Objects.equals(owner,historyOwner) || !java.util.Objects.equals(season,historySeason)){
            historyOwner=owner;historySeason=season;historyPanel.close();historyClosed();
        }
        historyToggle.setEnabled(connected);historyToggle.setToolTipText(connected?"Your 20 most recent matches":"Connect to view your match history");
        boolean canPreview=s==null || !s.queued && (s.match==null || !s.match.isOpen());
        previewToggle.setEnabled(canPreview);previewToggle.setToolTipText(canPreview?"Sample animations; your rating stays unchanged.":"Available between matches. Finish or leave your match first.");
        if(!canPreview)rankPreviews.setVisible(false);
        utilities.setVisible(canPreview && !preview);
        redoKit.setVisible(connected && !preview && accountMatches && canPreview && !kitRequired && !"demo".equals(s.mode));
        redoKit.setEnabled(!busy);
        mode.setText(preview?"Preview":localServer?"Local test":!connected?"Offline":"demo".equals(s.mode)?"Demo":s.season!=null && s.season.beta?"Beta season":"Pilot");
        copy(serverNotice,localServer?"Local test server. Players on the hosted beta cannot match with you.":"");
        copy(seasonNotice,connected && !preview && s.season!=null && s.season.resetNotice!=null?s.season.resetNotice.replace("Season 1","Season\u00a01"):"");
        matchRatingNotice=connected && !preview && s.match!=null && s.match.isOpen() && !s.match.rated?
            (s.match.reason!=null && s.match.reason.contains("Daily rated limit")?
                "Daily limit for this opponent. This match is unrated.":"Unrated match. Placement progress stays unchanged."):"";
        renderRatingAvailability(connected && !preview?s.rated:null);
        access.setVisible(!connected && !preview);
        boolean choosingRegion=connected && !preview && !s.queued && (s.match==null || !s.match.isOpen());
        regionChoice.setVisible(choosingRegion);queue.setVisible(choosingRegion);leave.setVisible(connected && !preview && s.queued);
        setupHop.setVisible(choosingRegion && kitRequired && onboardingStep==ArenaOnboarding.Step.HOP);
        setupHop.setText(setupHopBusy?"Finding world…":"Open world switcher");
        queue.setToolTipText(kitRequired?"Check your saved Max/Med kit at the supplies crate first.":null);
        queue.setEnabled(!busy && accountMatches && !kitRequired);leave.setEnabled(!busy && accountMatches);
        for(JButton b:new JButton[]{primary,secondary,forfeit,review,showReport}){b.setVisible(false);b.setEnabled(!busy && accountMatches);}
        for(JLabel l:new JLabel[]{opponent,destination,score,detail,hint,error,observation})l.setVisible(false);
        eyebrow.setVisible(handoff==null || preview);eyebrow.setText(preview?"Sample match":"Matchmaking");state.setText("Find a match");state.setForeground(TEXT);
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
        Models.Match m=s.match;
        endTest.setVisible(m!=null && !m.rated && "review".equals(m.state));
        endTest.setToolTipText("End this unrated test and return to the queue. Reports are retained; ratings stay unchanged.");
        showReport.setVisible(!s.queued && MatchReportView.reportable(s));
        showReport.setEnabled(true);
        if(s.queued){
            // Search has its own clock and compact layout. The existing controls stay mounted.
            state.setText("Finding a match");
        }else if(m==null || !m.isOpen()){
            copy(detail,"126 combat · Max gear");copy(hint,"First to two fight wins.");
            // Finished results remain in Show match report and history, not the matchmaking card.
            queue.setEnabled(!busy && accountMatches && !kitRequired && s.cooldownUntil==null && region.selected()!=null);
            QueueCooldown cooldown=QueueCooldown.from(s,serverNow,java.time.ZoneId.systemDefault());
            if(cooldown!=null){state.setText("Queue cooldown");copy(detail,cooldown.remaining);copy(hint,cooldown.detail);}
        }else if(handoff!=null){
            renderHandoff(handoff);
            if(!"offered".equals(m.state)){
                if("active".equals(m.state) && m.round>1)score(m.score);
                forfeit.setVisible(true);
            }
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
        String action=view.action;travelCard=view.travel;
        copy(state,view.heading);
        state.setForeground(TEXT);
        if(view.travel)opponent.setVisible(false);else opponent(view.opponent,view.opponentRank);
        destination(view.region,view.world);
        copy(detail,view.message);copy(hint,view.footer);
        if(!"panel".equals(action)){primary.setText(view.primary);primary.setVisible(true);primary.setEnabled(view.enabled);}
        if(!"panel".equals(view.secondaryAction) && !view.secondary.isEmpty()){secondary.setText(view.secondary);secondary.setVisible(true);secondary.setEnabled(view.secondaryEnabled);}
    }
    void renderSetup(ArenaSetup.View view,String saved,boolean saving){
        renderSetup(view,saved,saving,false);
    }
    void renderSetup(ArenaSetup.View view,String saved,boolean saving,boolean diagnostics){
        if(view==null || queueActive && !view.warning)return;
        showMatchDetails();
        eyebrow.setVisible(true);eyebrow.setText("Arena setup");copy(state,view.heading);state.setForeground(view.warning?ArenaSetupWarningOverlay.WARNING:TEXT);
        copy(detail,view.message);detail.setForeground(view.warning?ArenaSetupWarningOverlay.WARNING:TEXT);copy(hint,view.detail);
        primary.setVisible(false);secondary.setVisible(false);
        setupDetails.removeAll();setupDetails.setVisible(!view.changes.isEmpty());
        if(!view.changes.isEmpty()){
            copy(state,view.changes.size()==1?"One kit issue":view.changes.size()+" kit issues");
            copy(detail,view.changes.size()>1?"Fix the red marks. Start with this one.":"");
            ArenaKit.Change first=view.changes.get(0);
            copy(hint,"Spellbook".equals(first.location)?"Use the spellbook selector above the kit.":
                "Rune pouch".equals(first.location)?"Open the rune pouch to check enabled runes.":
                "Counts include worn and inventory items. Hover red marks for details.");
            for(ArenaKit.Change change:view.changes.subList(0,1)){
                setupDetails.add(Box.createVerticalStrut(8));
                JLabel location=center(label(change.title(),11,TEXT));setupDetails.add(location);
                JLabel items=center(label("",11,MUTED));copy(items,change.summary());setupDetails.add(items);
            }
            if(view.changes.size()>1)setupDetails.add(center(label("+ "+(view.changes.size()-1)+" other fixes",10,MUTED)));
            JLabel recovery=center(label("",10,MUTED));copy(recovery,view.detail);
            setupDetails.add(Box.createVerticalStrut(8));setupDetails.add(recovery);
        }
        saveKit.setVisible(diagnostics && view.kit!=null && view.kit.complete());saveKit.setEnabled(!saving);
        copy(kitSaved,diagnostics?saved:"");finish();
    }
    void onboarding(ArenaOnboarding.Step step,boolean required,boolean hopping){
        onboardingStep=step;kitRequired=required;setupHopBusy=hopping;
    }
    void renderCatalog(ArenaSetup.View view,boolean enabled){
        renderCatalog(view,enabled,"");
    }
    void renderCatalog(ArenaSetup.View view,boolean enabled,String notice){
        catalogFilter.setSelected(enabled);
        catalogFilter.setVisible(view!=null && view.kit!=null && view.kit.complete() &&
            view.kit.revision==ArenaSetup.REVISION && view.target!=ArenaSetup.Target.STATS_TAB &&
            view.target!=ArenaSetup.Target.BUILD_CONFIRM && !"Wrong opponent".equals(view.heading));
        copy(catalogNotice,catalogFilter.isVisible() && enabled?notice:"");
        finish();
    }
    void onboardingGuide(String instruction,String region){worldGuide=instruction;worldGuideRegion=region;}
    void renderOnboarding(ArenaSetup.View setup){
        if(!kitRequired || queueActive)return;
        showMatchDetails();
        eyebrow.setVisible(true);eyebrow.setText("First-time setup");
        if(onboardingStep==ArenaOnboarding.Step.FIX || onboardingStep==ArenaOnboarding.Step.MAX_MED)return;
        state.setForeground(TEXT);
        if(onboardingStep==ArenaOnboarding.Step.HOP){
            state.setText("Visit the supplies crate");
            copy(detail,!worldGuide.isEmpty() && java.util.Objects.equals(worldGuideRegion,region.selected())?worldGuide:
                "Open the world switcher and choose a PvP Arena world to check your Max/Med kit.");
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
    void service(String url){
        String host=null;
        try{host=url==null?null:java.net.URI.create(url).getHost();}catch(IllegalArgumentException ignored){}
        localServer="127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "[::1]".equals(host);
    }
    void renderRoundIssue(String reason){
        showMatchDetails();
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
        destination.setText(label+(world>0?" · World "+world:""));destination.setIcon(new RegionFlag(label));destination.setVisible(true);
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
    private void showMatchDetails(){travelCard=false;queueIntro.setVisible(false);search.setVisible(false);match.setVisible(true);}
    void dispose(){search.dispose();}
    private void finish(){
        catalogOptions.setVisible(catalogFilter.isVisible());
        travelMetaGap.setVisible(travelCard);
        scoreGap.setVisible(destination.isVisible() && score.isVisible());
        eyebrowGap.setVisible(eyebrow.isVisible());opponentRow.setVisible(opponent.isVisible());
        metaRow.setVisible(destination.isVisible() || score.isVisible());detailRow.setVisible(detail.isVisible());hintRow.setVisible(hint.isVisible());
        matchControlsGap.setVisible(match.isVisible() && regionChoice.isVisible());revalidate();repaint();
    }
}
