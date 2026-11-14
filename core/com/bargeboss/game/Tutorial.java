package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, COAST=1, CHANNEL=2, HOLD=3, STEER=4, DOCK=5, TIE=6, CONNECT=7, PULL=8, RELEASE=9, DONE=10;
    public static final float PUSH_X=240,PUSH_Y=600,PUSH_RADIUS=24;
    public int lesson=MOVE, actionTicks,channelGate=1;
    public int clearedGate,gateClearTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY,channelPreviousX,channelPreviousY;
    /** Explicit start/reset only. Lesson progression never rebuilds the scene. */
    public void setup(Game g) {
        clearedGate=gateClearTicks=0;channelGate=1;
        g.cancelInputs();g.reversing=false;
        g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeToTug=false;g.tow=new Towline();
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0;g.nextId=1;g.selected=-1;actionTicks=0;
        g.addCargo(2,292,700);park(g.cargo[0]);
        g.addCargo(2,350,440);g.cargo[1].angle=-(float)Math.PI/2;park(g.cargo[1]);
        g.tug=new Body(0,-1,TrainingChannel.X[0],TrainingChannel.Y[0],0);
        if(lesson==COAST) g.tug.vx=40;
        // Reset can restart the current exercise; normal progression preserves
        // the original bodies, positions, momentum, water and camera instead.
        if(lesson>=HOLD) g.tug=new Body(0,-1,376,700,(float)Math.PI);
        if(lesson==TIE) {g.cargo[0].x=Game.DOCK_X[2];park(g.cargo[0]);}
        if(lesson>=CONNECT) {
            Body finished=g.cargo[0],towPractice=g.cargo[1];
            finished.x=Physics.LEFT+finished.radius+10;finished.y=g.dockY(2);finished.angle=(float)Math.PI/2;
            park(finished);finished.lineAttached=true;finished.deliveryRecorded=true;
            finished.unloadingTicks=Game.UNLOAD_TICKS;finished.mooringTicks=Mooring.ALIGN_TICKS;
            finished.dockTime=1.2f;
            g.cargo[0]=towPractice;g.cargo[1]=finished;g.delivered=1;
            towPractice.x=300;towPractice.y=690;park(towPractice);
            g.tug=new Body(0,-1,300,570,-(float)Math.PI/2);
            if(lesson>=PULL) g.tow.attach(g,towPractice);
            actionTicks=g.towReleases;
        }
        startX=lesson>=HOLD?g.cargo[0].x:g.tug.x;
        startY=lesson>=HOLD?g.cargo[0].y:g.tug.y;
        channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        g.fitTutorialCamera();
    }
    private static void park(Body b) {
        b.vx=b.vy=b.spin=0;b.awaitingPickup=true;
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
    }
    public void update(Game g) {
        if(gateClearTicks>0) gateClearTicks--;
        // A player may keep driving before following the coast instruction.
        // Remember genuine crossings so lifting late never strands progression.
        if(lesson<=CHANNEL) {
            if(channelGate<TrainingChannel.X.length && TrainingChannel.reachedGate(channelGate,
                    channelPreviousX,channelPreviousY,g.tug.x,g.tug.y)) {
                clearedGate=channelGate++;gateClearTicks=90;g.haptic(Game.HAPTIC_CONTACT);
            }
            channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        }
        if(lesson==MOVE && g.navigating && Physics.length(g.tug.x-startX,g.tug.y-startY)>30) {
            lesson=COAST; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==COAST) {
            actionTicks=!g.pointer && !g.navigating?actionTicks+1:0;
            if(actionTicks>=60) {
                lesson=CHANNEL;actionTicks=0;
                channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
                g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==CHANNEL) {
            if(channelGate==TrainingChannel.X.length) {
                lesson=HOLD;actionTicks=0;g.haptic(Game.HAPTIC_SUCCESS);
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
                lesson=DOCK;actionTicks=0;g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==DOCK && g.inTieArea(g.cargo[0])) {
            lesson=TIE;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==TIE && g.delivered>0) {
            // The unloaded barge stays alongside. The next job uses the second
            // hull that has been waiting in the same harbor since the start.
            Body finished=g.cargo[0];g.cargo[0]=g.cargo[1];g.cargo[1]=finished;
            lesson=CONNECT;actionTicks=0;g.selected=-1;g.cancelInputs();
            g.haptic(Game.HAPTIC_SUCCESS);
        }
        else if(lesson==CONNECT && g.tow.cargo==g.cargo[0]) {
            lesson=PULL;startX=g.cargo[0].x;startY=g.cargo[0].y;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==PULL && g.navigating && g.tow.cargo==g.cargo[0]
                && Physics.length(g.cargo[0].x-startX,g.cargo[0].y-startY)>45) {
            lesson=RELEASE;actionTicks=g.towReleases;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==RELEASE && g.tow.cargo==null && g.towReleases>actionTicks) { lesson=DONE;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS); }
        else if((lesson==PULL || lesson==RELEASE) && g.tow.cargo==null && g.ropePhase==0) { lesson=CONNECT;actionTicks=0;g.cancelInputs(); }
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
            case CONNECT: return "APPROACH THE SECOND BARGE. DRAG TO TUG.";
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
