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
                title="GOOD MORNING. WE'VE GOT HOOLIGANS";
                detail="SIX SPEEDERS IN THE NO-WAKE ZONE. BUMP 'EM, THEN TOW 'EM TO THE POLICE BOAT BY THE CANS.";
                reply="COPY. TIME TO WRITE SOME TICKETS.";break;
            case Challenges.LOOSE_BOATS:
                title="SQUALL JUST BLEW THROUGH";
                detail="FOUR BOATS SNAPPED THEIR LINES AND THEY'RE DRIFTING. GET THEM HOME.";
                reply="COPY. ROUNDING UP STRAYS.";break;
            case Challenges.WATER_TAXI:
                title="PARTY AT THE INLET PIER";
                detail="GUESTS ARE WAVING FROM THE DOCKS. THREE SEATS. THREE MINUTES. MIND THE NO-WAKE TRAFFIC.";
                reply="COPY. TAXI'S OPEN.";break;
            case Challenges.SANDBAR:
                title="YACHT ON THE BAR";
                detail="SHE'S HARD AGROUND OFF THE SOUTH SHORE. HAUL HER FREE, BRING HER TO SERVICE.";
                reply="COPY. BRINGING THE BIG LINE.";break;
            case Challenges.REGATTA:
                title="REGATTA COMMITTEE CALLING";
                detail="PROVE THE COURSE BEFORE THE RACE. TEN GATES. CLOCK STARTS NOW.";
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
}
