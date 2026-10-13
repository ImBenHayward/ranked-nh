package com.rankednh;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class RankedNhPluginTest
{
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(RankedNhPlugin.class);
        RuneLite.main(args);
    }
}
