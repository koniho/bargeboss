package com.bargeboss.game;

/** Append-only level catalog. Existing IDs stay stable in saved careers. */
public final class Challenges {
    public static final int TWO_BANKS=0,SHARED_SLIP=1,LOCK=2,DEEP_CHANNEL=3,PAGE_SIZE=3;
    private static final class Definition {
        final int kind,quota,replaySeconds;
        final String title,description,summary,cardGoal,cardHint;
        Definition(int kind,int quota,int replaySeconds,String title,String description,String summary,String cardGoal,String cardHint){
            this.kind=kind;this.quota=quota;this.replaySeconds=replaySeconds;this.title=title;this.description=description;
            this.summary=summary;this.cardGoal=cardGoal;this.cardHint=cardHint;
        }
    }
    // Add future levels at the end; menu pages, save slots and star totals derive from this list.
    private static final Definition[] LEVELS={
        new Definition(TWO_BANKS,2,180,"TWO BANKS","TWO BARGES. BERTHS ON OPPOSITE BANKS.",
                "2 BARGES / OPPOSITE BANKS","2 BARGES / 2 BANKS","UNLOADED BARGES STAY AFLOAT"),
        new Definition(SHARED_SLIP,3,300,"SHARED SLIP","ONE SLIP. TOW EMPTY BARGES CLEAR TO SWAP.",
                "3 BARGES / SWAP THE BERTH","ONE SLIP / THREE LOADS","PULL EACH EMPTY OUT FIRST"),
        new Definition(LOCK,2,180,"LOCK RUN","ENTER THE LOCK. RISE. CLEAR THE EXIT.",
                "2 BARGES / THROUGH THE LOCK","TIMED LOCK GATES","NO OVERALL TIME LIMIT"),
        new Definition(DEEP_CHANNEL,3,360,"DEEP FIRST","NARROW CHANNEL. WORK THE DEEPEST BERTH FIRST.",
                "3 BARGES / DEEPEST FIRST","DEEP FIRST / NARROW REACH","PLAN SPACE FOR THE EMPTIES")
    };
    public static final int COUNT=LEVELS.length;
    private Challenges() {}
    public static boolean valid(int id) { return id>=0 && id<COUNT; }
    private static Definition level(int id) { return LEVELS[valid(id)?id:0]; }
    public static int kind(int id) { return level(id).kind; }
    public static String title(int id) { return level(id).title; }
    public static String description(int id) { return level(id).description; }
    public static String summary(int id) { return level(id).summary; }
    public static String cardGoal(int id) { return level(id).cardGoal; }
    public static String cardHint(int id) { return level(id).cardHint; }
    /** Delivery order also supplies the asset manifest, including loads not yet spawned. */
    public static int feedBerth(int id,int index) {
        int kind=kind(id);
        return kind==SHARED_SLIP || kind==DEEP_CHANNEL?new int[]{2,0,1}[index]:index==0?2:1;
    }
    public static int quota(int id) { return level(id).quota; }
    public static int pageCount() { return (COUNT+PAGE_SIZE-1)/PAGE_SIZE; }
    public static int pageFor(int id) { return valid(id)?id/PAGE_SIZE:0; }
    public static int maxStars() { return COUNT*3; }
    /** Bounded replay budget retained for tooling, not a gameplay deadline. */
    public static int shiftTicks(int id) { return level(id).replaySeconds*60; }
    public static int stars(int id,int delivered,int lost,int hull) {
        if(delivered<quota(id) || hull<=0) return 0;
        return 1+(lost==0?1:0)+(hull>=80?1:0);
    }
}
