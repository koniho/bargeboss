package com.bargeboss.game;

/** Real helm practice, with no clock, damage or traffic. Each lesson needs a real action. */
public final class Tutorial {
    public static final int THROTTLE=0, WHEEL=1, STOP=2, REVERSE=3, DOCK=4, DONE=5;
    public int lesson=THROTTLE, actionTicks;
    public boolean reversed, exitRequested, resumeShift;
    private float heading;
    public void setup(Game g) {
        g.cancelInputs(); g.helm.enabled=true; g.braking=false;
        g.tug=new Body(0,-1,350,650,-(float)Math.PI/2);
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0; g.nextId=1; g.selected=-1; actionTicks=0; reversed=false;
        if(lesson==STOP) { g.helm.throttle=.4f; g.tug.vy=-25; }
        if(lesson==DOCK) {
            g.tug=new Body(0,-1,376,700,(float)Math.PI);
            g.addCargo(2,292,700); g.selected=0;
        }
        heading=g.tug.angle;
    }
    public void update(Game g) {
        if(lesson==THROTTLE && g.helm.throttle>.15f && g.tug.speed()>20) {
            lesson=WHEEL; heading=g.tug.angle; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==WHEEL && Math.abs(g.helm.rudder)>.35f && Math.abs(Physics.angle(g.tug.angle-heading))>.28f) {
            lesson=STOP; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==STOP && g.helm.throttle==0 && g.tug.speed()<10) {
            lesson=REVERSE; g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==REVERSE) {
            float forward=g.tug.vx*(float)StrictMath.cos(g.tug.angle)+g.tug.vy*(float)StrictMath.sin(g.tug.angle);
            if(g.helm.throttle<-.15f && forward< -12) reversed=true;
            if(reversed && g.helm.throttle==0 && g.tug.speed()<10) { lesson=DOCK; setup(g); g.haptic(Game.HAPTIC_TICK); }
        } else if(lesson==DOCK && g.delivered>0) { lesson=DONE; g.cancelInputs(); g.haptic(Game.HAPTIC_SUCCESS); }
    }
    public String title() {
        return new String[]{"SET YOUR THROTTLE","TURN THE WHEEL","COME TO A STOP","TRY REVERSE","DOCK YOUR FIRST LOAD","READY FOR YOUR SHIFT"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case THROTTLE: return "SLIDE THE LEVER UP FOR FORWARD.";
            case WHEEL: return "HOLD EITHER SIDE OF THE WHEEL.";
            case STOP: return "CENTER THE LEVER, OR TAP BRAKE.";
            case REVERSE: return reversed?"NOW TAP BRAKE TO STOP AGAIN.":"SLIDE BELOW N TO BACK UP.";
            case DOCK: return "EASE FORWARD TO PUSH CARGO TO C.";
            default: return "YOU DROVE, TURNED AND DELIVERED.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case THROTTLE: return "SPEED STAYS SET WHEN YOU LET GO.";
            case WHEEL: return "RELEASE TO CENTER THE RUDDER.";
            case STOP: return "NEUTRAL COASTS. BRAKE STOPS FASTER.";
            case REVERSE: return "REVERSE THRUST SLOWS, THEN BACKS UP.";
            case DOCK: return "BERTH FENDERS HELP YOU SETTLE IN.";
            default: return "THE SAME HELM WORKS IN YOUR SHIFT.";
        }
    }
}
