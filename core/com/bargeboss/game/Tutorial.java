package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, STOP=1, HOLD=2, STEER=3, DOCK=4, TIE=5, CONNECT=6, PULL=7, RELEASE=8, DONE=9;
    public int lesson=MOVE, actionTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY;
    public void setup(Game g) {
        g.cancelInputs(); g.braking=false;
        // A lesson reset owns a fresh fixture; no line may keep targeting a removed hull.
        g.ropePhase=g.ropeTicks=0; g.ropeBody=null;g.ropeToTug=false;g.tow=new Towline();
        g.tug=new Body(0,-1,350,700,-(float)Math.PI/2);
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0; g.nextId=1; g.selected=-1; actionTicks=0;
        if(lesson==STOP) g.tug.vy=-40;
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
        if(lesson==MOVE && g.navigating && Physics.length(g.tug.x-startX,g.tug.y-startY)>30) {
            lesson=STOP; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==STOP && g.braking && !g.navigating && g.tug.speed()<10) {
            lesson=HOLD; setup(g); g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==HOLD && g.pushing && g.heldCargo()==g.cargo[0]
                && Physics.touching(g.tug,g.cargo[0],5)
                && Physics.length(g.cargo[0].x-startX,g.cargo[0].y-startY)>3) {
            lesson=STEER; startX=g.cargo[0].x; startY=g.cargo[0].y; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==STEER) {
            Body b=g.cargo[0];
            if(g.pushing && g.manualSteering) actionTicks++; else actionTicks=0;
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
        return new String[]{"DRAG TO DRIVE","COME TO A STOP","HOLD TO PUSH","DRAG TO STEER","DOCK YOUR FIRST LOAD","THROW YOUR LINE","CONNECT A TOWLINE","PULL YOUR BARGE","LET GO OF THE LINE","READY FOR YOUR SHIFT"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case MOVE: return "PRESS THE TUG, THEN DRAG LEFT.";
            case STOP: return "LIFT YOUR FINGER. TAP BRAKE.";
            case HOLD: return "HOLD THE CARGO INSIDE THE GOLD RING.";
            case STEER: return "KEEP HOLDING. DRAG DOWN AND LEFT.";
            case DOCK: return "LIFT, THEN HOLD THE BARGE AGAIN.";
            case TIE: return "DRAG TO AIM AT THE GLOWING BOLLARD.";
            case CONNECT: return "DRAG FROM THE BARGE TO THE TUG.";
            case PULL: return "DRAG THE TUG LEFT TO PULL THE LOAD.";
            case RELEASE: return "TAP RELEASE LINE AT THE TOP RIGHT.";
            default: return "YOU PUSHED, TIED UP AND TOWED.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case MOVE: return "PULL FARTHER FOR MORE THRUST.";
            case STOP: return "RELEASING COASTS. BRAKE STOPS.";
            case HOLD: return "THE TUG LINES UP AUTOMATICALLY.";
            case STEER: return "DRAG OUTSIDE THE RING TO TURN.";
            case DOCK: return "HOLD STILL TO ENTER THE BERTH.";
            case TIE: return "RELEASE TO THROW. MISSES REEL BACK.";
            case CONNECT: return "RELEASE OVER THE TUG TO THROW.";
            case PULL: return "TAKE UP THE SLACK. THE BARGE FOLLOWS.";
            case RELEASE: return "THE BARGE KEEPS ITS MOMENTUM.";
            default: return "THE SAME CONTROLS WORK IN A SHIFT.";
        }
    }
}
