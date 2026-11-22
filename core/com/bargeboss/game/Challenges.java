package com.bargeboss.game;

/** Append-only level catalog. Existing IDs stay stable in saved careers. */
public final class Challenges {
    public static final int TWO_BANKS=0,SHARED_SLIP=1,LOCK=2,DEEP_CHANNEL=3,MONTROSE=4,INDIANA=5,RAFT_RESCUE=6,SWIMMER_RESCUE=7,HAULOUT=8,PAGE_SIZE=3;
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
                "3 BARGES / DEEPEST FIRST","DEEP FIRST / NARROW REACH","PLAN SPACE FOR THE EMPTIES"),
        new Definition(MONTROSE,1,600,"MONTROSE HARBOR","TOW AN 80 FT YACHT FROM THE INLET INTO ITS SLIP.",
                "80 FT YACHT / MARINA TOW","INLET TO WOODEN SLIP","ATTACH / TOW / SETTLE"),
        new Definition(INDIANA,1,900,"INDIANA HARBOR","TOW STEEL THROUGH THE SHIP CANAL TO THE SOUTH MILL.",
                "STEEL TOW / THE FORKS","OUTER HARBOR TO SOUTH MILL","PASS HICK TOWER / TAKE SOUTH FORK"),
        new Definition(MONTROSE,2,1200,"RAFTED CRUISERS","SEPARATE TWO RAFTED CRUISERS. TOW BOTH TO DOCK C.",
                "2 CRUISERS / DOCK C","SEPARATE / TOW / DOCK","RETURN TO C-38 AND C-22"),
        new Definition(MONTROSE,20,900,"SWIMMER RESCUE","THROW BUOYS TO 20 SWIMMERS OFF THE SEAWALL.",
                "20 SWIMMERS / KEEP CLEAR","SLOW DOWN / THROW BUOYS","AVOID HULL AND PROPELLER CONTACT"),
        new Definition(MONTROSE,1,600,"HAULOUT CALL","TOW A DISABLED BOAT TO THE NORTH SHORE TRAVEL LIFT.",
                "DISABLED BOAT / HAULOUT","TOW / RELEASE / CREW HANDOFF","BETWEEN DOCKS N AND M")
    };
    public static final int COUNT=LEVELS.length;
    // Hot scene queries need only primitive metadata. Object-array reads retain /
    // autorelease Definitions after J2ObjC translation, even though they are fixed.
    private static final int[] KINDS=kinds();
    private static int[] kinds(){int[] out=new int[COUNT];for(int i=0;i<COUNT;i++)out[i]=LEVELS[i].kind;return out;}
    private Challenges() {}
    public static boolean standalone(int id) {return id==HAULOUT || id==MONTROSE || id==INDIANA || id==RAFT_RESCUE || id==SWIMMER_RESCUE;}
    public static boolean valid(int id) { return id>=0 && id<COUNT; }
    private static Definition level(int id) { return LEVELS[valid(id)?id:0]; }
    public static int kind(int id) { return KINDS[valid(id)?id:0]; }
    public static String title(int id) { return level(id).title; }
    public static String description(int id) { return level(id).description; }
    public static String summary(int id) { return level(id).summary; }
    public static String cardGoal(int id) { return level(id).cardGoal; }
    public static String cardHint(int id) { return level(id).cardHint; }
    /** Delivery order also supplies the asset manifest, including loads not yet spawned. */
    public static int feedBerth(int id,int index) {
        int kind=kind(id);
        if(kind==INDIANA)return 0;
        return kind==SHARED_SLIP || kind==DEEP_CHANNEL?new int[]{2,0,1}[index]:index==0?2:1;
    }
    public static int quota(int id) { return level(id).quota; }
    public static final int LOCATION_COUNT=3;
    public static boolean calumet(int id){return id>=0 && id<=DEEP_CHANNEL;}
    public static int stageEntry(int id){return calumet(id)?TWO_BANKS:id==HAULOUT || id==RAFT_RESCUE || id==SWIMMER_RESCUE?MONTROSE:id;}
    public static int menuId(int slot){return slot==0?TWO_BANKS:slot==1?MONTROSE:slot==2?INDIANA:-1;}
    public static int[] locationJobs(int id){return calumet(id)?new int[]{0,1,2,3}:stageEntry(id)==MONTROSE?new int[]{8,4,6,7}:new int[]{5};}
    public static int nextStage(int id){int entry=stageEntry(id);return entry==TWO_BANKS?MONTROSE:entry==MONTROSE?INDIANA:-1;}
    public static String locationTitle(int id){return calumet(id)?"CALUMET RIVER":stageEntry(id)==MONTROSE?"MONTROSE HARBOR":"INDIANA HARBOR";}
    public static String locationDescription(int id){return calumet(id)?"LAKE MICHIGAN TO TURNING BASIN FIVE / FOUR CONTIGUOUS JOBS":stageEntry(id)==MONTROSE?"HAULOUT / YACHT TOW / CRUISERS / SWIMMERS":"OUTER HARBOR TO SOUTH MILL / STEEL TOW";}
    public static int pageCount() { return 1; }
    public static int pageFor(int id) { return 0; }
    public static int maxStars() { return COUNT*3; }
    /** Bounded replay budget retained for tooling, not a gameplay deadline. */
    public static int shiftTicks(int id) { return level(id).replaySeconds*60; }
    public static int stars(int id,int delivered,int lost,int hull) {
        if(delivered<quota(id) || hull<=0) return 0;
        return 1+(lost==0?1:0)+(hull>=80?1:0);
    }
}
