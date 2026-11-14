package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, COAST=1, CHANNEL=2, HOLD=3, STEER=4, DOCK=5, TIE=6, CONNECT=7, PULL=8, RELEASE=9, DONE=10;
    public static final float PUSH_X=240,PUSH_Y=600,PUSH_RADIUS=24;
    public int lesson=MOVE, actionTicks,channelGate=1;
    public int clearedGate,gateClearTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY,channelPreviousX,channelPreviousY;
    public void setup(Game g) {
        clearedGate=gateClearTicks=0;
        g.cancelInputs(); g.reversing=false;
        // A lesson reset owns a fresh fixture; no line may keep targeting a removed hull.
        g.ropePhase=g.ropeTicks=0; g.ropeBody=null;g.ropeToTug=false;g.tow=new Towline();
        g.tug=new Body(0,-1,350,700,-(float)Math.PI/2);
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0; g.nextId=1; g.selected=-1; actionTicks=0;
        if(lesson==CHANNEL) {
            channelGate=1;g.tug=new Body(0,-1,TrainingChannel.X[0],TrainingChannel.Y[0],0);
            channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
            g.fitTutorialCamera();return;
        }
        g.fitTutorialCamera();
        if(lesson==COAST) g.tug.vy=-40;
        if(lesson>=HOLD) {
            g.tug=new Body(0,-1,376,700,(float)Math.PI);
            g.addCargo(2,lesson==TIE?Game.DOCK_X[2]:292,700);
        }
        if(lesson>=CONNECT) {
            g.tug=new Body(0,-1,300,570,-(float)Math.PI/2);
            g.cargo[0]=new Body(1,2,300,690,-(float)Math.PI/2);
            if(lesson>=PULL) g.tow.attach(g,g.cargo[0]);
            actionTicks=g.towReleases;
            startX=g.cargo[0].x;startY=g.cargo[0].y;
            return;
        }
        if(lesson==TIE || lesson==HOLD) park(g.cargo[0]);
        startX=lesson==HOLD?g.cargo[0].x:g.tug.x;
        startY=lesson==HOLD?g.cargo[0].y:g.tug.y;
    }
    private static void park(Body b) {
        b.vx=b.vy=b.spin=0;b.awaitingPickup=true;
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
    }
    public void update(Game g) {
        if(gateClearTicks>0) gateClearTicks--;
        if(lesson==MOVE && g.navigating && Physics.length(g.tug.x-startX,g.tug.y-startY)>30) {
            lesson=COAST; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==COAST) {
            actionTicks=!g.pointer && !g.navigating?actionTicks+1:0;
            if(actionTicks>=60) { lesson=CHANNEL;setup(g);g.haptic(Game.HAPTIC_TICK); }
        } else if(lesson==CHANNEL) {
            boolean reached=TrainingChannel.reachedGate(channelGate,channelPreviousX,channelPreviousY,g.tug.x,g.tug.y);
            channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
            if(reached) {
                int cleared=channelGate;
                channelGate++;g.haptic(Game.HAPTIC_CONTACT);
                if(channelGate==TrainingChannel.X.length) { lesson=HOLD;setup(g);g.haptic(Game.HAPTIC_SUCCESS); }
                clearedGate=cleared;gateClearTicks=90;
            }
        } else if(lesson==HOLD) {
            Body b=g.cargo[0];
            if(g.pushing && g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++;
            if(actionTicks>=6 && Physics.length(b.x-PUSH_X,b.y-PUSH_Y)<=PUSH_RADIUS) {
                lesson=STEER;actionTicks=0;startX=b.x;startY=b.y;
                g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
            }
        } else if(lesson==STEER) {
            Body b=g.cargo[0];
            if(g.pushing && !g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++; else actionTicks=0;
            if(actionTicks>=12 && Physics.length(b.x-startX,b.y-startY)>3) {
                lesson=DOCK; setup(g); g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==DOCK && g.inTieArea(g.cargo[0])) {
            lesson=TIE; park(g.cargo[0]); g.cancelInputs(); g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==TIE && g.delivered>0) { lesson=CONNECT; setup(g);g.haptic(Game.HAPTIC_SUCCESS); }
        else if(lesson==CONNECT && g.tow.cargo==g.cargo[0]) {
            lesson=PULL;startX=g.cargo[0].x;startY=g.cargo[0].y;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==PULL && g.navigating && g.tow.cargo==g.cargo[0]
                && Physics.length(g.cargo[0].x-startX,g.cargo[0].y-startY)>45) {
            lesson=RELEASE;actionTicks=g.towReleases;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==RELEASE && g.tow.cargo==null && g.towReleases>actionTicks) { lesson=DONE;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS); }
        else if((lesson==PULL || lesson==RELEASE) && g.tow.cargo==null && g.ropePhase==0) { lesson=CONNECT;setup(g); }
    }
    public String title() {
        return new String[]{"DRAG TO DRIVE","RELEASE TO COAST","NAVIGATE THE CANAL","DRAG TO PUSH","HOLD FOR ASSIST","DOCK YOUR FIRST LOAD","THROW YOUR LINE","CONNECT A TOWLINE","PULL YOUR BARGE","LET GO OF THE LINE","READY FOR YOUR SHIFT"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case MOVE: return "PRESS THE TUG. DRAG ALONG THE ARROW.";
            case COAST: return "LIFT YOUR FINGER TO CUT THRUST.";
            case CHANNEL: return "STEER UPSTREAM THROUGH EVERY GATE.";
            case HOLD: return "PRESS AND DRAG THE BARGE TO PUSH HERE.";
            case STEER: return "LIFT, THEN HOLD THE BARGE STILL.";
            case DOCK: return "LIFT, THEN HOLD THE BARGE AGAIN.";
            case TIE: return "DRAG TO AIM AT THE GLOWING BOLLARD.";
            case CONNECT: return "DRAG FROM THE BARGE TO THE TUG.";
            case PULL: return "DRAG ALONG THE ARROW TO PULL.";
            case RELEASE: return "TAP THE CONNECTED BARGE TO LET GO.";
            default: return "YOU PUSHED, TIED UP AND TOWED.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case MOVE: return "PULL FARTHER FOR MORE THRUST.";
            case COAST: return "BOATS COAST. DRAG BACK TO SLOW DOWN.";
            case CHANNEL: return "SLOW FOR SQUARE TURNS. CAMERA FOLLOWS.";
            case HOLD: return "REACH THE MARKED ZONE TO CONTINUE.";
            case STEER: return "HOLD STILL FOR AN ASSISTED PUSH.";
            case DOCK: return "HOLD STILL TO ENTER THE BERTH.";
            case TIE: return "RELEASE TO THROW. MISSES REEL BACK.";
            case CONNECT: return "RELEASE OVER THE TUG TO THROW.";
            case PULL: return "TAKE UP THE SLACK. THE BARGE FOLLOWS.";
            case RELEASE: return "THE BARGE KEEPS ITS MOMENTUM.";
            default: return "THE SAME CONTROLS WORK IN A SHIFT.";
        }
    }
}
