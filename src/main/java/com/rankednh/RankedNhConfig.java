package com.rankednh;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.config.Notification;

@ConfigGroup("rankednh")
public interface RankedNhConfig extends Config
{
    @ConfigItem(keyName = "serviceUrl", name = "Service URL", description = "Your operator's HTTPS service, or localhost for development. Changing it disconnects your session.")
    default String serviceUrl() { return "https://ranked-nh.vercel.app"; }

    @ConfigItem(keyName="showMatchReport", name="Show match report", description="Show your series result, rating change and progress after a match.", position=1)
    default boolean showMatchReport() { return true; }

    @Range(min=0,max=300)
    @ConfigItem(keyName="reportSeconds", name="Report duration", description="Seconds to keep the report visible. Set 0 to keep it until dismissed or you queue again.", position=2)
    default int reportSeconds() { return 60; }

    @ConfigItem(keyName="previewMatchReport", name="Preview match report", description="Show a labelled sample result in-game. No match is played and no rating changes.", position=3)
    default boolean previewMatchReport() { return false; }

    @ConfigItem(keyName="reduceReportMotion", name="Reduce report motion", description="Show the final rating and progress immediately, without the entrance or counting animation.", position=4)
    default boolean reduceReportMotion() { return false; }

    @ConfigItem(keyName="placementRevealSound", name="Rank reveal sound", description="Play a hum and chime with promotions and your first rank reveal, including previews. Independent of game sound volume.", position=10)
    default boolean placementRevealSound() { return true; }

    @Range(min=0,max=100)
    @ConfigItem(keyName="placementRevealVolume", name="Reveal volume", description="Volume of the rank reveal sound. Set 0 to mute.", position=11)
    default int placementRevealVolume() { return 65; }

    @ConfigItem(keyName="matchFoundNotification", name="Match found notification", description="Notify when a new matchup needs your acceptance. Uses RuneLite notification preferences.", position=5)
    default Notification matchFoundNotification() { return Notification.ON; }

    @ConfigItem(keyName="matchFoundPing", name="Match found ping", description="Play a short in-game sound once when a matchup is found.", position=6)
    default boolean matchFoundPing() { return true; }

    @ConfigItem(keyName="highlightOpponent", name="Highlight opponent", description="Mark your assigned opponent with a tile, overhead label and minimap marker in the assigned Arena world.", position=7)
    default boolean highlightOpponent() { return true; }

    @ConfigItem(keyName="preferredRegion", name="Preferred region", description="Preferred competition region. Also selectable before queueing in the sidebar.", hidden=true)
    default String preferredRegion() { return "EU"; }

    @ConfigItem(keyName="previewMatchup", name="Preview matchup in sidebar", description="Open a labelled sample of acceptance, travel and automatic arrival checks. No match or game actions are performed.", position=8)
    default boolean previewMatchup() { return false; }

    @ConfigItem(keyName="kitDiagnostics", name="Kit diagnostics", description="Show the developer control for saving an item list. Saving never approves or restores a default kit.", position=9)
    default boolean kitDiagnostics() { return false; }
}
