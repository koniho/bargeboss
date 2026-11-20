package com.bargeboss.game;

/** Dockside teacher arrival and radio handoff, in the same continuous practice scene. */
final class TutorialArrival {
    static final float DOCK_X=-1600,DOCK_Y=810,TEACHER_Y=752;
    static final int APPROACH=240;
    Body teacher;
    private int replyTicks,warningCooldown,warningSerial;
    int warningTicks;
    private final TeacherFollow follow=new TeacherFollow();
    private String reply="";
    void start(Game g){
        teacher=new Body(0,-1,-1820,TEACHER_Y,0);replyTicks=0;reply="";
        g.tug.x=DOCK_X;g.tug.y=DOCK_Y;g.tug.angle=0;g.tug.vx=g.tug.vy=g.tug.spin=0;
    }
    void step(Game g){
        if(teacher==null)return;
        if(warningTicks>0)warningTicks--;if(warningCooldown>0)warningCooldown--;
        Tutorial t=g.tutorial;
        if(t.entering){
            t.entranceTicks++;
            if(t.entranceTicks<=APPROACH){
                float u=t.entranceTicks/(float)APPROACH;
                teacher.x=-1820+220*u*u*(3-2*u);teacher.vx=220*6*u*(1-u)*60/APPROACH;
                teacher.engineLoad=Physics.clamp(teacher.vx/85,0,.85f);
                if(t.entranceTicks==APPROACH)g.dialogue.show(g,"INSTRUCTOR / CHANNEL 08","CAPTAIN - DO YOU KNOW HOW TO DRIVE?","I'M ALONGSIDE IN THE GREEN TUG.",
                    "FIRST DAY AT THE HELM.","I COULD USE A REFRESHER.","YES. LET'S GET UNDER WAY.");
            } else if(g.dialogue.result()>=0){
                replyTicks++;
                if(replyTicks==24){
                    int choice=g.dialogue.result();
                    reply=choice==0?"WELCOME ABOARD. WE'LL TAKE IT SLOW.":choice==1?"COPY THAT. LET'S SHAKE OFF THE RUST.":"ROGER, CAPTAIN. SHOW ME YOUR HELM WORK.";
                    g.dialogue.close(g);g.radio.transmit(RadioAudio.TEACHER);
                }
                if(replyTicks>=180){t.entering=false;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);}
            }
        } else follow.step(g,teacher);
    }
    void warn(Game g){
        if(g.tutorial.entering || g.dialogue.active || warningCooldown>0)return;
        warningTicks=180;warningCooldown=420;warningSerial++;g.radio.transmit(RadioAudio.TEACHER);
    }
    InstructionPanel.Content warning(){
        return new InstructionPanel.Content("teacher-warning-"+warningSerial,"EASY, CAPTAIN!","GIVE MY TUG A LITTLE MORE ROOM.",
                "I'M FOLLOWING YOU. KEEP SOME CLEARANCE.",-1,0,"INSTRUCTOR / GREEN TUG","RADIO / RX");
    }

    InstructionPanel.Content content(){
        return new InstructionPanel.Content("teacher-"+reply,"INCOMING / CHANNEL 08",reply.isEmpty()?"INSTRUCTOR COMING ALONGSIDE.":reply,
                reply.isEmpty()?"STAND BY AT THE SEAWALL DOCK.":"CAST OFF AND CIRCLE THE TWO LIT BUOYS.",-1,0,"INSTRUCTOR / GREEN TUG","RADIO");
    }
}
