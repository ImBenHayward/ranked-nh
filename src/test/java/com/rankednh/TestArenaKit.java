package com.rankednh;

/** Explicit test fixture; production receives RuneLite's Gson through dependency injection. */
final class TestArenaKit {
    static final DefaultArenaKit DEFAULT=new DefaultArenaKit(new com.google.gson.Gson());
    static final ArenaSetup SETUP=new ArenaSetup(DEFAULT);
    private TestArenaKit(){}
}
