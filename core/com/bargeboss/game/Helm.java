package com.bargeboss.game;

/** Shared two-finger ownership and thrust. Throttle latches; wheel springs home. */
public final class Helm {
    public boolean enabled=true;
    public float throttle, rudder;
    private int throttlePointer=-1, wheelPointer=-1;
    public void reset() { enabled=true; cancel(); }
    public void cancel() { throttle=0; rudder=0; throttlePointer=wheelPointer=-1; }
    public void neutral() { throttle=0; rudder=0; }
    public boolean down(Game g,int id,float x,float y) {
        if(!enabled) return false;
        if(x>=48 && x<=190 && y>=785 && y<=943 && throttlePointer==-1) {
            throttlePointer=id; setThrottle(g,y); return true;
        }
        if(Physics.length(x-455,y-865)<=85 && wheelPointer==-1) {
            wheelPointer=id; setWheel(g,x); return true;
        }
        return false;
    }
    public boolean move(Game g,int id,float x,float y) {
        if(id==throttlePointer && id!=-1) { setThrottle(g,y); return true; }
        if(id==wheelPointer && id!=-1) { setWheel(g,x); return true; }
        return false;
    }
    public boolean up(Game g,int id,boolean cancelled) {
        if(id==throttlePointer && id!=-1) {
            throttlePointer=-1; if(cancelled) { throttle=0; g.braking=true; } return true;
        }
        if(id==wheelPointer && id!=-1) { wheelPointer=-1; if(rudder!=0) g.haptic(Game.HAPTIC_TICK); rudder=0; return true; }
        return false;
    }
    private void setThrottle(Game g,float y) {
        float value=Physics.clamp((864-y)/66,-1,1);
        if(Math.abs(value)<.12f) value=0;
        if(Math.round(throttle*2)!=Math.round(value*2)) g.haptic(Game.HAPTIC_TICK);
        throttle=value; g.navigating=false; g.braking=false;
    }
    private void setWheel(Game g,float x) {
        float value=Physics.clamp((x-455)/65,-1,1);
        if(Math.abs(value)<.12f) value=0;
        if(Math.signum(value)!=Math.signum(rudder)) g.haptic(Game.HAPTIC_TICK);
        rudder=value;
    }
    public void drive(Game g) {
        Body tug=g.tug;
        if(g.braking) { tug.vx*=.90f; tug.vy*=.90f; tug.spin*=.90f; }
        else {
            float c=(float)StrictMath.cos(tug.angle),s=(float)StrictMath.sin(tug.angle);
            tug.vx+=c*throttle*76*Physics.DT; tug.vy+=s*throttle*76*Physics.DT;
            float drag=tug.speed()*.007f*Physics.DT;
            tug.vx*=1-drag; tug.vy*=1-drag;
            // Vectored thrust gives low-speed authority; backing reverses the rudder.
            float direction=throttle<-.05f?-1:1;
            float turn=rudder*(.5f+Math.min(1.4f,tug.speed()/45))*direction;
            tug.spin+=(turn-tug.spin)*3.2f*Physics.DT;
        }
        g.control=g.braking && tug.speed()>3?Game.BRAKING:Math.abs(throttle)>.05f?Game.MOVING:tug.speed()>3?Game.COASTING:Game.IDLE;
    }
}
