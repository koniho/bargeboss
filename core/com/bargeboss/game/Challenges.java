package com.bargeboss.game;

/** Append-only level catalog. Existing IDs stay stable in saved careers. */
public final class Challenges {
    public static final int TWO_BANKS=0,SHARED_SLIP=1,LOCK=2,DEEP_CHANNEL=3,MONTROSE=4,INDIANA=5,RAFT_RESCUE=6,SWIMMER_RESCUE=7,HAULOUT=8,CHICAGO=9,
            WAKE_PATROL=10,LOOSE_BOATS=11,WATER_TAXI=12,SANDBAR=13,REGATTA=14,FIREWORKS_NIGHT=15,DEEP_FREEZE=16,PAGE_SIZE=3;
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
                "DISABLED BOAT / HAULOUT","TOW / RELEASE / CREW HANDOFF","BETWEEN DOCKS N AND M"),
        new Definition(CHICAGO,1,1200,"NAVY PIER FIRE","LOCK IN, FETCH THE ENGINE 2 FIRE BARGE, LOCK OUT AND SPRAY THE WHEEL.",
                "FIRE BARGE / HARBOR LOCK","NAVY PIER TO ENGINE 2","LOCK IN / TOW OUT / SPRAY"),
        new Definition(MONTROSE,6,600,"WAKE PATROL","SIX SPEEDERS ARE TEARING UP THE NO-WAKE MARINA. BUMP TO TICKET, THEN TOW EACH TO THE POLICE BOAT.",
                "6 SPEEDERS / CHASE","BUMP TO TICKET / TOW TO POLICE","THEY JUKE WHEN YOU GET CLOSE"),
        new Definition(MONTROSE,4,900,"LOOSE BOATS","A SQUALL SNAPPED FOUR BOATS LOOSE. TOW EACH DRIFTER BACK TO ITS EMPTY SLIP.",
                "4 DRIFTERS / SQUALL","LASSO / TOW / RELEASE AT THE SLIP","THE WIND KEEPS PUSHING"),
        new Definition(MONTROSE,10,600,"WATER TAXI","FERRY TEN PARTY GUESTS FROM THE DOCKS TO THE PARTY PIER BEFORE THE CLOCK RUNS OUT.",
                "10 GUESTS / 3:00","SLOW TO BOARD / 3 SEATS","FULL LOADS PAY A BONUS"),
        new Definition(MONTROSE,1,900,"SANDBAR","A YACHT IS HARD AGROUND OFF THE SOUTH SHORE. HAUL IT FREE AND TOW IT HOME.",
                "YACHT AGROUND / HAUL","LASSO / HAUL FREE / TOW HOME","KEEP THE LINE LOADED"),
        new Definition(MONTROSE,10,600,"REGATTA RUN","PROVE THE REGATTA COURSE: THREAD TEN GATES AGAINST THE CLOCK.",
                "10 GATES / TIME TRIAL","EACH GATE ADDS TIME","LEFTOVER SECONDS PAY OUT"),
        new Definition(MONTROSE,3,1200,"FIREWORKS NIGHT","TOW THE FIREWORKS BARGE TO THE SHOW MARK AND FIRE THREE VOLLEYS FOR THE BEACH.",
                "FIREWORKS / FINALE","TOW OUT / ANCHOR / LAUNCH","BEAT SHOWTIME"),
        new Definition(MONTROSE,10,1200,"DEEP FREEZE","A BLIZZARD IS ROLLING IN. HAUL AS MANY BOATS TO THE TRAVEL LIFT AS YOU CAN BEFORE THE HARBOR FREEZES.",
                "BLIZZARD / HAUL OUT","TOW TO THE LIFT / BEAT THE ICE","4 SAVES IT / 8 FOR THREE STARS")
    };
    public static final int COUNT=LEVELS.length;
    // Hot scene queries need only primitive metadata. Object-array reads retain /
    // autorelease Definitions after J2ObjC translation, even though they are fixed.
    private static final int[] KINDS=kinds();
    private static int[] kinds(){int[] out=new int[COUNT];for(int i=0;i<COUNT;i++)out[i]=LEVELS[i].kind;return out;}
    private Challenges() {}
    public static boolean standalone(int id) {return id==HAULOUT || id==MONTROSE || id==INDIANA || id==RAFT_RESCUE || id==SWIMMER_RESCUE || id==CHICAGO || id>=WAKE_PATROL && id<=DEEP_FREEZE;}
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
        if(kind==CHICAGO)return 2;
        return kind==SHARED_SLIP || kind==DEEP_CHANNEL?new int[]{2,0,1}[index]:index==0?2:1;
    }
    public static int quota(int id) { return level(id).quota; }
    public static final int LOCATION_COUNT=4;
    public static boolean calumet(int id){return id>=0 && id<=DEEP_CHANNEL;}
    public static int stageEntry(int id){return calumet(id)?TWO_BANKS:id==HAULOUT || id==RAFT_RESCUE || id==SWIMMER_RESCUE || id>=WAKE_PATROL && id<=DEEP_FREEZE?MONTROSE:id;}
    public static int menuId(int slot){return slot==0?TWO_BANKS:slot==1?MONTROSE:slot==2?INDIANA:slot==3?CHICAGO:-1;}
    /** Selector index of a location entry; -1 for a job that is not an entry. */
    public static int locationIndex(int id){int entry=stageEntry(id);for(int i=0;i<LOCATION_COUNT;i++)if(menuId(i)==entry)return i;return -1;}
    public static int[] locationJobs(int id){return calumet(id)?new int[]{0,1,2,3}:stageEntry(id)==MONTROSE?new int[]{8,4,6,7,10,11,12,13,14,15,16}:stageEntry(id)==CHICAGO?new int[]{CHICAGO}:new int[]{5};}
    public static int nextStage(int id){int entry=stageEntry(id);return entry==TWO_BANKS?MONTROSE:entry==MONTROSE?INDIANA:entry==INDIANA?CHICAGO:-1;}
    public static String locationTitle(int id){return calumet(id)?"CALUMET RIVER":stageEntry(id)==MONTROSE?"MONTROSE HARBOR":stageEntry(id)==CHICAGO?"CHICAGO RIVER":"INDIANA HARBOR";}
    public static String locationDescription(int id){return calumet(id)?"LAKE MICHIGAN TO TURNING BASIN FIVE / FOUR CONTIGUOUS JOBS":stageEntry(id)==MONTROSE?"ELEVEN JOBS / RESCUES / CHASES / FIREWORKS / BLIZZARD":stageEntry(id)==CHICAGO?"NAVY PIER TO WOLF POINT / HARBOR LOCK FIRE CALL":"OUTER HARBOR TO SOUTH MILL / STEEL TOW";}
    public static int pageCount() { return (LOCATION_COUNT+PAGE_SIZE-1)/PAGE_SIZE; }
    public static int pageFor(int id) { return Math.max(0,locationIndex(id))/PAGE_SIZE; }
    public static int maxStars() { return COUNT*3; }
    /** Bounded replay budget retained for tooling, not a gameplay deadline. */
    public static int shiftTicks(int id) { return level(id).replaySeconds*60; }
    public static int stars(int id,int delivered,int lost,int hull) {
        if(delivered<quota(id) || hull<=0) return 0;
        return 1+(lost==0?1:0)+(hull>=80?1:0);
    }
}
