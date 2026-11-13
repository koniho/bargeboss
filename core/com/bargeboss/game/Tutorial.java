package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, STOP=1, HOLD=2, STEER=3, DOCK=4, TIE=5, DONE=6;
    public int lesson=MOVE, actionTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY;
    public void setup(Game g) {
        g.cancelInputs(); g.braking=false;
        g.tug=new Body(0,-1,350,700,-(float)Math.PI/2);
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0; g.nextId=1; g.selected=-1; actionTicks=0;
        if(lesson==STOP) g.tug.vy=-40;
        if(lesson>=HOLD) {
            g.tug=new Body(0,-1,376,700,(float)Math.PI);
            g.addCargo(2,lesson==TIE?Game.DOCK_X[2]:292,700);
        }
        startX=g.tug.x; startY=g.tug.y;
    }
    public void update(Game g) {
        if(lesson==MOVE && g.navigating && Physics.length(g.tug.x-startX,g.tug.y-startY)>30) {
            lesson=STOP; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==STOP && g.braking && !g.navigating && g.tug.speed()<10) {
            lesson=HOLD; setup(g); g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==HOLD && g.pushing && g.control==Game.PUSHING) {
            lesson=STEER; startX=g.cargo[0].x; startY=g.cargo[0].y; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==STEER) {
            Body b=g.cargo[0];
            if(g.pushing && g.manualSteering) actionTicks++; else actionTicks=0;
            if(actionTicks>=12 && Physics.length(b.x-startX,b.y-startY)>3) {
                lesson=DOCK; setup(g); g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==DOCK && g.inDock(g.cargo[0]) && g.cargo[0].speed()<24) {
            lesson=TIE; g.cancelInputs(); g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==TIE && g.delivered>0) { lesson=DONE; g.cancelInputs(); g.haptic(Game.HAPTIC_SUCCESS); }
    }
    public String title() {
        return new String[]{"DRAG TO DRIVE","COME TO A STOP","HOLD TO PUSH","DRAG TO STEER","DOCK YOUR FIRST LOAD","THROW YOUR LINE","READY FOR YOUR SHIFT"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case MOVE: return "PRESS THE TUG, THEN DRAG UP.";
            case STOP: return "LIFT YOUR FINGER. TAP BRAKE.";
            case HOLD: return "PRESS CARGO C AND KEEP HOLDING.";
            case STEER: return "KEEP HOLDING. DRAG UP AND LEFT.";
            case DOCK: return "LIFT, THEN HOLD CARGO C AGAIN.";
            case TIE: return "DRAG TO AIM AT THE GLOWING BOLLARD.";
            default: return "YOU MOVED, STEERED AND TIED UP.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case MOVE: return "PULL FARTHER FOR MORE THRUST.";
            case STOP: return "RELEASING COASTS. BRAKE STOPS.";
            case HOLD: return "THE TUG LINES UP AUTOMATICALLY.";
            case STEER: return "DRAG OUTSIDE THE RING TO TURN.";
            case DOCK: return "HOLD STILL TO ENTER THE C ZONE.";
            case TIE: return "RELEASE TO THROW. MISSES REEL BACK.";
            default: return "THE SAME CONTROLS WORK IN A SHIFT.";
        }
    }
}
