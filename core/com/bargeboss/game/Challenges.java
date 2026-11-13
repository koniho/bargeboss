package com.bargeboss.game;

/** Shared challenge rules: one river, increasingly demanding shifts. */
public final class Challenges {
    public static final int COUNT=3;
    private static final String[] TITLES={"FIRST WATCH","QUICK TURN","HEAVY SHIFT"};
    private static final String[] DESCRIPTIONS={"LEARN THE RIVER. EARN YOUR FIRST STAR.",
            "SAME CARGO TARGET. THIRTY SECONDS LESS.","TWO EXTRA LOADS. KEEP THE CHANNEL CLEAR."};
    private static final int[] QUOTAS={6,6,8},SECONDS={180,150,180};
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
