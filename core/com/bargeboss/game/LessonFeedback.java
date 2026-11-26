package com.bargeboss.game;

/** Bounded FIFO of earned coaching, independent of rapid lesson transitions. */
final class LessonFeedback {
    private final int[] lessons=new int[Tutorial.DONE];
    private final boolean[] rough=new boolean[Tutorial.DONE];
    private int head,count,seen,ticks,serial;
    private boolean bumped;
    void reset(){head=count=seen=ticks=serial=0;bumped=false;}
    void sample(Game g){if(g.splashes.impactOn(g.tug)>=25 || g.tutorial.navigation.retry)bumped=true;}
    void completed(Game g,int before,int after){
        if(after<=before || before>=Tutorial.DONE || (seen&(1<<before))!=0)return;
        seen|=1<<before;
        int slot=(head+count)%lessons.length;
        lessons[slot]=before;rough[slot]=bumped;count++;bumped=false;
        if(ticks==0 && !g.dialogue.active){ticks=240;serial++;g.radio.transmit(RadioAudio.TEACHER);}
    }
    void step(Game g){
        if(count==0 || g.dialogue.active || g.tutorial.instructorRadio.phase!=0)return;
        if(ticks==0){ticks=240;serial++;g.radio.transmit(RadioAudio.TEACHER);}
        else if(--ticks==0){head=(head+1)%lessons.length;count--;
            if(count>0){ticks=240;serial++;g.radio.transmit(RadioAudio.TEACHER);}}
    }
    float burstProgress(){return active()?(240-ticks)/45f:1;}
    boolean active(){return count>0 && ticks>0;}
    InstructionPanel.Content content(){
        if(!active())return null;
        int lesson=lessons[head];boolean improve=rough[head];
        String detail=improve?"YOU GOT IT DONE. KEEP BUILDING ON THAT.":PRAISE[lesson];
        String tip=improve?"TRY LESS THROTTLE AND MORE CLEARANCE.":"READY FOR THE NEXT CHALLENGE, CAPTAIN.";
        return new InstructionPanel.Content("lesson-feedback-"+serial,
                improve?"GOOD RECOVERY, CAPTAIN!":"NICELY DONE, CAPTAIN!",detail,tip,-1,0,"INSTRUCTOR / GREEN TUG",improve?"KEEP AT IT":"WELL DONE");
    }
    private static final String[] PRAISE={
        "YOU HAVE A FEEL FOR THE THROTTLE NOW.","A CLEAN FIGURE EIGHT. GOOD HELM WORK.",
        "NICE ASTERN CONTROL AROUND THOSE BUOYS.","A TIDY PARKING JOB. WELL HANDLED.",
        "GOOD CHART WORK. YOU FOUND YOUR WAY.","YOU HANDLED THOSE TIGHT TURNS WELL.",
        "A SOLID CONNECTION. READY TO TOW.","GOOD TOWING. ROOM TO WORK NOW.",
        "LINE CLEAR. NOW YOU CAN PUSH HER IN.",
        "THAT IS HOW TO MOVE A HEAVY LOAD.","STEADY PRESSURE. LET THE TUG DO THE WORK.",
        "GOOD APPROACH. YOUR BARGE IS IN POSITION.","LINE SECURED. A PROPER DELIVERY!",
        "TWO LOADS CONNECTED. NICE TEAMWORK.","BOTH LOADS THROUGH. WELL JUDGED!",
        "A NEAT TRAIN. SMART LINE HANDLING.","TIGHT CHANNEL, CLEAN PASSAGE. NICE WORK!",
        "ALL LINES CLEAR. YOU EARNED YOUR STRIPES!",
        "A VERY SMALL SUN. PERFECTLY NORMAL.","FIRE CREW ARRIVED. TRY THE WET BUTTON.","EXCELLENT. WE INVENTED WET FIREWORKS.",
        "TEN BARGES, ONE CLOCK. NICE HUSTLE."};
}
