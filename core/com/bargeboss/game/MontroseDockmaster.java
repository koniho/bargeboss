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
                title="A BOAT HAS LOST POWER";
                detail="TAP THE N-M LIFT TO RADIO. CLEAR THE WELL FIRST.";
                reply="COPY. TAKING THE TOW.";break;
            case Challenges.MONTROSE:
                title="NEXT CALL: YACHT AT THE INLET";
                detail="CREW HAS THE HAULOUT. BRING THE YACHT TO DOCK N.";
                reply="COPY. HEADING TO THE INLET.";break;
            case Challenges.RAFT_RESCUE:
                title="TWO CRUISERS NEED A HAND";
                detail="SEPARATE THE RAFT. RETURN THEM TO C-38 AND C-22.";
                reply="COPY. TWO TOWS TO DOCK C.";break;
            case Challenges.SWIMMER_RESCUE:
                title="PEOPLE IN THE WATER";
                detail="20 OFF THE SOUTH SEAWALL. SLOW DOWN AND THROW BUOYS.";
                reply="COPY. KEEPING THE HULL CLEAR.";break;
            default:return;
        }
        g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"GAVIN / MONTROSE DOCKMASTER",title,detail,reply);
    }
}
