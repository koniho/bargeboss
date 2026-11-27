package com.bargeboss.game;

/** Gavin, Montrose dockmaster: welcome and contiguous job dispatch. */
public final class MontroseDockmaster {
    private MontroseDockmaster(){}
    public static void dispatch(Game g){
        if(g.rescue.touring){g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"GAVIN / MONTROSE DOCKMASTER",
            "NICE WORK! GET TO KNOW THE AREA.","HEAD OUT AND CHECK OUT THE BEACH. KEEP YOUR RADIO ON.","COPY. A LITTLE SIGHTSEEING.");return;}
        if(g.challengeId==Challenges.RAFT_RESCUE){g.rescue.distress(g);return;}
        String title,detail,reply;
        switch(g.challengeId){
            case Challenges.HAULOUT:
                title="AIN'T BROKE HAS BROKEN DOWN";
                detail="RED RACER AT STAR DOCK S. RADIO THE N-M LIFT FIRST.";
                reply="COPY. TAKING THE TOW.";break;
            case Challenges.MONTROSE:
                title="NEXT CALL: YACHT AT THE INLET";
                detail="THE CREW IS SAFE. BRING THE YACHT TO DOCK N.";
                reply="COPY. HEADING TO THE INLET.";break;
            case Challenges.RAFT_RESCUE:
                title="TWO CRUISERS NEED A HAND";
                detail="SEPARATE THE RAFT. RETURN THEM TO C-38 AND C-22.";
                reply="COPY. TWO TOWS TO DOCK C.";break;
            case Challenges.SWIMMER_RESCUE:
                title="PEOPLE IN THE WATER";
                detail="STORM RESCUE. TOW THE RESCUE BARGE. SEARCH OFF THE BEACH.";
                reply="COPY. SEARCHLIGHT ON. BRINGING RINGS AND THE RESCUE BARGE.";break;
            case Challenges.WAKE_PATROL:
                title="BACK IN THE MARINA. WE'VE GOT HOOLIGANS";
                detail="SIX SPEEDERS IN THE NO-WAKE ZONE. BUMP 'EM, THEN TOW 'EM TO THE POLICE BOAT BY THE CANS.";
                reply="COPY. TIME TO WRITE SOME TICKETS.";break;
            case Challenges.LOOSE_BOATS:
                title="SQUALL JUST BLEW THROUGH";
                detail="FOUR BOATS SNAPPED THEIR LINES AND THEY'RE DRIFTING. GET THEM HOME.";
                reply="COPY. ROUNDING UP STRAYS.";break;
            case Challenges.WATER_TAXI:
                title="MORNING, CAPTAIN. THE BEACH IS PACKED";
                detail="BEACHGOERS ARE WAVING FROM THE SAND. FERRY THEM OUT TO THE PARTY RAFT. THREE SEATS, THREE MINUTES.";
                reply="COPY. BEACH SHUTTLE'S OPEN.";break;
            case Challenges.SANDBAR:
                title="YACHT ON THE BAR";
                detail="SOMEBODY CUT THE CORNER AND PUT HER ON THE SANDBAR OFF THE BEACH. HAUL HER FREE, TOW HER OUT TO THE DEEP ANCHORAGE.";
                reply="COPY. BRINGING THE BIG LINE.";break;
            case Challenges.REGATTA:
                title="REGATTA COMMITTEE CALLING";
                detail="PROVE THE COURSE: DOWN THE BEACH, ROUND THE BREAKWATER, HOME TO THE HARBOR MOUTH. CLOCK STARTS AT GATE ONE.";
                reply="COPY. FULL THROTTLE.";break;
            case Challenges.FIREWORKS_NIGHT:
                title="LAST JOB OF THE DAY. THE BIG ONE";
                detail="FIREWORKS BARGE TO THE SHOW MARK BEFORE SHOWTIME. THEN LIGHT IT UP.";
                reply="COPY. LET'S GIVE THEM A SHOW.";break;
            case Challenges.DEEP_FREEZE:
                title="BLIZZARD WARNING. THE HARBOR'S GOING TO FREEZE";
                detail="TEN BOATS STILL OUT. GET THEM TO THE LIFT BEFORE THE ICE LOCKS THEM IN.";
                reply="COPY. HAULING AS MANY AS I CAN.";break;
            default:return;
        }
        g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"GAVIN / MONTROSE DOCKMASTER",title,detail,reply);
    }

    /** Gavin's word on the job just finished, ahead of the next dispatch. Three tiers by stars
     * (three, two, one or none), with a remark on the hull and the clock. */
    public static void debrief(Game g,int job,int stars,int seconds){
        String[] lines=lines(job,Math.max(0,Math.min(3,stars)));
        if(lines==null)return;
        String detail=lines[1]+" "+hullRemark(g.hull,stars)+" "+clock(seconds);
        g.dialogue.interject(g,RadioAudio.DOCKMASTER,"GAVIN / MONTROSE DOCKMASTER",lines[0],detail.trim(),lines[2]);
    }
    private static String hullRemark(int hull,int stars){
        if(hull>=100)return stars>=3?"AND NOT A SCRATCH ON THE TUG.":"TUG'S STILL SPOTLESS, AT LEAST.";
        if(hull>=70)return "";
        if(hull>=40)return "YOUR HULL'S TAKING A BEATING. EASY ON THE BUMPS.";
        return "AND THAT TUG IS HELD TOGETHER WITH PRAYER. ONE MORE HARD HIT AND SHE'S GONE.";
    }
    private static String clock(int seconds){
        if(seconds<=0)return "";
        return "("+seconds/60+":"+(seconds%60<10?"0":"")+seconds%60+" ON THE CLOCK.)";
    }
    /** {title, detail, reply} for a job at a star tier; null when the job has no debrief. */
    private static String[] lines(int job,int stars){
        int tier=stars>=3?0:stars==2?1:2;
        switch(job){
            case Challenges.HAULOUT:return pick(tier,
                new String[]{"TEXTBOOK HAULOUT!","THE YARD CREW SAYS THAT WAS THE CLEANEST LIFT THEY'VE SEEN ALL SEASON.","JUST DOING MY JOB."},
                new String[]{"SHE'S UP ON THE LIFT","GOOD WORK. A FEW SCRAPES GETTING HER IN, BUT THE RACER'S DRY.","COPY. I'LL TIGHTEN IT UP."},
                new String[]{"WELL... SHE'S OUT OF THE WATER","THE OWNER IS COUNTING THE NEW DENTS. WE'LL CALL IT A LEARNING EXPERIENCE.","ROGER. ROUGH START."});
            case Challenges.RAFT_RESCUE:return pick(tier,
                new String[]{"BOTH CRUISERS HOME, NOT A MARK ON THEM","THE OWNERS WANT TO BUY YOU DINNER. I TOLD THEM YOU ONLY EAT DIESEL.","HA. TELL THEM I SAID THANKS."},
                new String[]{"CRUISERS ARE BACK IN THEIR SLIPS","NICE SEPARATION ON THAT RAFT. THE DOCKING COULD'VE BEEN SMOOTHER.","COPY. NOTED."},
                new String[]{"THEY'RE TIED UP. MOSTLY.","C DOCK LOOKS LIKE A BUMPER-CAR RINK. BUT EVERYONE'S HOME.","SORRY ABOUT THE PILINGS."});
            case Challenges.MONTROSE:return pick(tier,
                new String[]{"YACHT'S SAFE AT DOCK N","SMOOTH TOW FROM THE INLET. THE CREW IS ALREADY TELLING STORIES ABOUT YOU.","GLAD THEY'RE SAFE."},
                new String[]{"YACHT'S AT DOCK N","SHE MADE IT. A LITTLE BUMPY COMING IN, BUT SHE MADE IT.","COPY. NEXT ONE'LL BE CLEANER."},
                new String[]{"THE YACHT IS... AT THE DOCK","THE INSURANCE GUY IS GOING TO HAVE QUESTIONS. LET'S MOVE ON QUICKLY.","UNDERSTOOD."});
            case Challenges.SWIMMER_RESCUE:return pick(tier,
                new String[]{"EVERYONE'S OUT OF THE WATER!","COAST GUARD JUST CALLED ASKING WHO RAN THAT SEARCH. YOU'RE A HERO, CAPTAIN. GET SOME SLEEP.","LONG NIGHT. GLAD WE GOT THEM ALL."},
                new String[]{"RESCUE COMPLETE","WE GOT THEM OUT. IT WASN'T PRETTY, BUT EVERYBODY'S DRYING OFF ON THE BEACH.","COPY. GOOD TEAM EFFORT."},
                new String[]{"THEY'RE OUT. BARELY.","PARAMEDICS ARE BUSY TONIGHT. WE'LL TALK ABOUT SEARCHLIGHT DISCIPLINE TOMORROW.","ROGER. I'LL DO BETTER."});
            case Challenges.WATER_TAXI:return pick(tier,
                new String[]{"BEST SHUTTLE SERVICE ON THE LAKE!","THE PARTY RAFT IS CHANTING YOUR NAME. I CAN HEAR IT FROM THE OFFICE.","TELL THEM TO SAVE ME A SODA."},
                new String[]{"PARTY'S IN FULL SWING","GOOD HUSTLE. A FEW GUESTS GOT A LITTLE SPLASHED, BUT NOBODY'S COMPLAINING.","COPY. SHUTTLE CLOSED."},
                new String[]{"EVERYBODY MADE IT... EVENTUALLY","HALF THE GUESTS ARE SEASICK AND THE DJ ASKED IF THE TUG HAS BRAKES.","IT DOES. MOSTLY."});
            case Challenges.SANDBAR:return pick(tier,
                new String[]{"SHE CAME OFF THAT BAR LIKE A CORK!","OWNER SAYS THE HULL'S NOT EVEN SCUFFED. HE'S BUYING THE WHOLE CREW LUNCH.","I'LL TAKE A SANDWICH."},
                new String[]{"YACHT'S SAFE AT ANCHOR","GOOD HAUL. TOOK A FEW TRIES, BUT SHE'S FLOATING AGAIN.","COPY. NO HARM DONE."},
                new String[]{"SHE'S OFF THE BAR. THAT'S SOMETHING.","THE OWNER WATCHED THE WHOLE THING FROM THE BEACH. WITH HIS HANDS OVER HIS EYES.","AT LEAST SHE FLOATS."});
            case Challenges.REGATTA:return pick(tier,
                new String[]{"COURSE RECORD!","THE COMMITTEE WANTS TO KNOW IF YOU'LL RACE NEXT YEAR. IN A TUG. THEY'RE SERIOUS.","SIGN ME UP."},
                new String[]{"COURSE PROVEN","SOLID RUN. THE RACERS WILL HAVE NO EXCUSES.","COPY. GOOD COURSE."},
                new String[]{"YOU MADE IT ROUND","THE COMMITTEE TIMED YOU WITH A CALENDAR. BUT THE COURSE IS PROVEN.","HEY, A FINISH IS A FINISH."});
            case Challenges.WAKE_PATROL:return pick(tier,
                new String[]{"NO-WAKE ZONE IS QUIET AS A CHURCH","EVERY LAST HOOLIGAN IS IN THE IMPOUND ROW. THE POLICE WANT TO DEPUTIZE YOU.","JUST KEEPING THE PEACE."},
                new String[]{"SPEEDERS ARE IMPOUNDED","NICE WORK, SHERIFF. A FEW OF THEM GAVE YOU A RUN FOR IT.","COPY. MARINA'S QUIET."},
                new String[]{"WELL, THEY'RE IMPOUNDED","THERE MAY HAVE BEEN MORE WAKE FROM YOU THAN FROM THEM. BUT THE JOB'S DONE.","ROGER. POINT TAKEN."});
            case Challenges.LOOSE_BOATS:return pick(tier,
                new String[]{"ALL STRAYS HOME, CLEAN AS A WHISTLE","THE OWNERS WON'T EVEN KNOW THEIR BOATS WENT FOR A SWIM.","THAT'S THE IDEA."},
                new String[]{"STRAYS ARE BACK IN THEIR SLIPS","GOOD ROUNDUP. COUPLE OF BUMPS, NOTHING THE FENDERS CAN'T HANDLE.","COPY. LINES ARE DOUBLED."},
                new String[]{"THEY'RE HOME. IN PIECES.","I'LL BE WRITING LETTERS TO OWNERS ALL WEEK. BUT THEY'RE TIED UP.","SORRY, GAVIN."});
            case Challenges.FIREWORKS_NIGHT:return pick(tier,
                new String[]{"BEST SHOW MONTROSE HAS EVER SEEN!","THE WHOLE LAKEFRONT IS ON ITS FEET. TAKE A BOW, CAPTAIN.","WHAT A NIGHT."},
                new String[]{"NICE SHOW!","THE CROWD LOVED IT. TIMING WAS A LITTLE TIGHT, BUT THAT ADDS DRAMA.","COPY. LIGHTS OUT."},
                new String[]{"THE SHOW... HAPPENED","THREE FIREWORKS AND A LOT OF WAITING. THE KIDS STILL CLAPPED.","EVERYONE'S A CRITIC."});
            default:return null;
        }
    }
    private static String[] pick(int tier,String[] great,String[] good,String[] rough){return tier==0?great:tier==1?good:rough;}
}
