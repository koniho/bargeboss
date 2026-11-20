package com.bargeboss.game;

/** Dockside teacher arrival and radio handoff, in the same continuous practice scene. */
final class TutorialArrival {
    static final float DOCK_X=-1600,DOCK_Y=810,TEACHER_Y=752;
    static final int APPROACH=240;
    Body teacher;
    private int replyTicks,warningCooldown,warningSerial,reactionTicks,returnCooldown;
    private boolean returnWarning;
    int warningTicks;
    private final TeacherFollow follow=new TeacherFollow();
    private String reply="";
    void observe(){teacher=new Body(0,-1,TrainingChannel.MIN-280,TrainingChannel.Y[0]-90,0);}
    void start(Game g){
        teacher=new Body(0,-1,-1820,TEACHER_Y,0);replyTicks=0;reply="";
        g.tug.x=DOCK_X;g.tug.y=DOCK_Y;g.tug.angle=0;g.tug.vx=g.tug.vy=g.tug.spin=0;
    }
    void step(Game g){
        if(teacher==null)return;
        if(warningTicks>0)warningTicks--;if(warningCooldown>0)warningCooldown--;
        if(returnCooldown>0)returnCooldown--;
        if(reactionTicks>0 && --reactionTicks==0){
            warningTicks=180;warningSerial++;returnWarning=false;g.radio.transmit(RadioAudio.TEACHER);
        }
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
        } else {
            follow.step(g,teacher);
            if(strayed(g)
                    && returnCooldown==0 && warningTicks==0 && reactionTicks==0){
                returnWarning=true;warningTicks=240;returnCooldown=900;warningSerial++;g.radio.transmit(RadioAudio.TEACHER);
            }
        }
    }
    private boolean strayed(Game g){
        Tutorial t=g.tutorial;float x=HelmPractice.MEET_X,y=HelmPractice.MEET_Y,limit=490;
        if(t.lesson==Tutorial.SLIP || t.lesson==Tutorial.MAP){x=t.navigation.targetX(g);y=t.navigation.targetY(g);limit=700;}
        else if(t.lesson==Tutorial.CHANNEL){int gate=Math.min(t.channelGate,TrainingChannel.X.length-1);x=TrainingChannel.X[gate];y=TrainingChannel.Y[gate];limit=700;}
        else if(t.lesson>=Tutorial.HOLD){
            if(t.lesson==Tutorial.DONE)return false;
            Body b=t.lesson==Tutorial.DOUBLE || t.lesson==Tutorial.LINK?g.cargo[2]:g.cargo[0];
            if(b==null)return false;x=b.x;y=b.y;limit=700;
        }
        return Physics.length(g.tug.x-x,g.tug.y-y)>limit;
    }
    void warn(Game g){
        if(g.tutorial.entering || g.dialogue.active || warningCooldown>0)return;
        reactionTicks=30;warningCooldown=420;
    }
    InstructionPanel.Content warning(){
        return new InstructionPanel.Content("teacher-warning-"+warningSerial,returnWarning?"COME BACK, CAPTAIN!":"EASY, CAPTAIN!",returnWarning?"RETURN TO THE MARKED PRACTICE AREA.":"GIVE MY TUG A LITTLE MORE ROOM.",
                "I'LL WATCH FROM THE SIDE.",-1,0,"INSTRUCTOR / GREEN TUG","RADIO / RX");
    }

    InstructionPanel.Content content(){
        return new InstructionPanel.Content("teacher-"+reply,"INCOMING / CHANNEL 08",reply.isEmpty()?"INSTRUCTOR COMING ALONGSIDE.":reply,
                reply.isEmpty()?"STAND BY AT THE SEAWALL DOCK.":"LET'S PRACTICE THE THROTTLE FIRST.",-1,0,"INSTRUCTOR / GREEN TUG","RADIO");
    }
}
