package com.bargeboss.game;

/** Dock departure is earned by powered travel and actual turns, followed by a rendezvous. */
public final class HelmPractice {
    public static final int FORWARD=0,LEFT=1,RIGHT=2,REVERSE=3,RENDEZVOUS=4;
    public static final float MEET_X=-1510,MEET_Y=545,MEET_RADIUS=42;
    public int phase;
    public int pace; // Forward substeps: gentle power, then stronger power.
    public float progress,targetX,targetY;
    private float previousX,previousY,previousAngle,phaseHeading;
    public void reset(Game g){phase=pace=0;progress=0;previousX=g.tug.x;previousY=g.tug.y;previousAngle=phaseHeading=g.tug.angle;target(g);}
    void skip(Game g){if(phase==FORWARD && pace==0)pace=1;else phase=Math.min(RENDEZVOUS,phase+1);progress=0;previousX=g.tug.x;previousY=g.tug.y;
        previousAngle=phaseHeading=g.tug.angle;target(g);}
    public boolean step(Game g){
        Body b=g.tug;float dx=b.x-previousX,dy=b.y-previousY,turn=Physics.angle(b.angle-previousAngle);
        previousX=b.x;previousY=b.y;previousAngle=b.angle;
        float along=dx*(float)StrictMath.cos(b.angle)+dy*(float)StrictMath.sin(b.angle);
        boolean physical=Physics.length(dx,dy)<12 && Math.abs(turn)<.3f;
        if(physical && g.navigating){
            float throttle=Joystick.appliedThrottle(g);
            if(phase==FORWARD && !g.reversing && (pace==0?throttle>0 && throttle<=.45f:throttle>=.75f))progress+=Math.max(0,along);
            if(phase==LEFT && !g.reversing)progress+=Math.max(0,-turn);
            if(phase==RIGHT && !g.reversing)progress+=Math.max(0,turn);
            if(phase==REVERSE && g.reversing)progress+=Math.max(0,-along);
        }
        float needed=phase==FORWARD?(pace==0?25:40):phase==REVERSE?35:.65f;
        if(phase<RENDEZVOUS && progress>=needed){
            if(phase==FORWARD && pace==0)pace=1;else phase++;progress=0;phaseHeading=b.angle;
            if(phase>=REVERSE)g.cancelInputs();
            g.sounds.complete();g.haptic(Game.HAPTIC_TICK);
        }
        target(g);
        return phase==RENDEZVOUS && Physics.length(b.x-MEET_X,b.y-MEET_Y)<=MEET_RADIUS;
    }
    private void target(Game g){
        float a=phaseHeading+(phase==LEFT?-.95f:phase==RIGHT?.95f:phase==REVERSE?(float)Math.PI:0);
        targetX=phase==RENDEZVOUS?MEET_X:g.tug.x+150*(float)StrictMath.cos(a);
        targetY=phase==RENDEZVOUS?MEET_Y:g.tug.y+150*(float)StrictMath.sin(a);
    }
    public String title(){if(phase==FORWARD)return pace==0?"SLOW AHEAD":"NOW TRY MORE POWER";return new String[]{"LEAVE THE DOCK","TURN TO PORT","TURN TO STARBOARD","TRY REVERSE","MEET AT THE START MARKER"}[phase];}
    public String first(){if(phase==FORWARD)return pace==0?"DRAG A LITTLE INTO GREEN. MOVE SLOWLY.":"DRAG FARTHER OUT TO PICK UP SPEED.";return new String[]{"DRAG THE THROTTLE INTO GREEN TO GO.","SWEEP THE THROTTLE LEFT TO TURN.","NOW SWEEP RIGHT. KEEP GENTLE POWER.","LIFT. DRAG INTO AMBER TO BACK UP.","DRIVE TO THE GLOWING MARKER."}[phase];}
    public String second(){if(phase==FORWARD)return pace==0?"SMALL DRAG = GENTLE THROTTLE.":"LONGER DRAG = MORE POWER AND SPEED.";return new String[]{"FARTHER FROM CENTER MEANS MORE POWER.","THE TUG NEEDS ROOM TO SWING.","GREEN IS FORWARD. AMBER IS REVERSE.","RELEASE BEFORE CHANGING DIRECTION.","WE'LL PRACTICE FIGURE EIGHTS THERE."}[phase];}
}
