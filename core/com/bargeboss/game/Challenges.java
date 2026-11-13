package com.bargeboss.game;

/** Shared challenge rules: one river, increasingly demanding shifts. */
public final class Challenges {
    public static final int COUNT=3;
    private static final String[] TITLES={"FIRST WATCH","BRIDGE RUN","HEAVY SHIFT"};
    private static final String[] DESCRIPTIONS={"ONE FEEDER. ONE BERTH. LEARN THE ROPES.",
            "FOLLOW THE RIVER. WAIT FOR THE BRIDGE.","TWO FEEDERS. THREE BERTHS. KEEP THEM MOVING."};
    private static final int[] QUOTAS={6,4,8},SECONDS={180,240,180};
    private Challenges() {}
    public static boolean valid(int id) { return id>=0 && id<COUNT; }
    private static int safe(int id) { return valid(id)?id:0; }
    public static String title(int id) { return TITLES[safe(id)]; }
    public static String description(int id) { return DESCRIPTIONS[safe(id)]; }
    public static int quota(int id) { return QUOTAS[safe(id)]; }
    public static int shiftTicks(int id) { return SECONDS[safe(id)]*60; }
    public static int stars(int id,int delivered,int lost,int hull) {
        if(delivered<quota(id) || hull<=0) return 0;
        return 1+(lost==0?1:0)+(hull>=80?1:0);
    }
}
