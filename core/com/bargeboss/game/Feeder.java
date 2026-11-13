package com.bargeboss.game;

/** One deterministic edge delivery lane. Incoming hulls belong to the NPC until handoff. */
public final class Feeder {
    public static final int IDLE=0, INBOUND=1, RETURNING=2;
    public static final float DROP_Y=730, START_Y=900, SPEED=62, GAP=67;
    public final float x;
    public final Body boat;
    public Body load;
    public int phase,shoutTicks,blockedTicks,drops;
    public boolean blocked;
    public String shout="";
    public Feeder(float x) { this.x=x; boat=new Body(0,-1,x,START_Y+GAP,-(float)Math.PI/2); }
    public void request(int berth) {
        if(phase!=IDLE) return;
        load=new Body(-2,berth,x,START_Y,-(float)Math.PI/2);
        boat.x=x; boat.y=START_Y+GAP; boat.angle=-(float)Math.PI/2;
        phase=INBOUND; blocked=false; blockedTicks=0;
    }
    private boolean clear(Game g,Body candidate,float padding) {
        if(Physics.touching(candidate,g.tug,padding)) return false;
        for(Body b:g.cargo) if(b!=null && b.active && Physics.touching(candidate,b,padding)) return false;
        return true;
    }
    private void call(Game g,String words) { shout=words; shoutTicks=150; g.tell(words); }
    public void step(Game g) {
        if(shoutTicks>0) shoutTicks--;
        if(phase==IDLE) return;
        float old=boat.y;
        if(phase==INBOUND) {
            float previous=load.y;
            load.y=Math.max(DROP_Y,load.y-SPEED*Physics.DT); boat.y=load.y+GAP;
            boolean room=false; for(Body b:g.cargo) if(b==null || !b.active) room=true;
            blocked=!room || !clear(g,load,5) || !clear(g,boat,5);
            if(blocked) {
                load.y=previous; boat.y=old;
                if(blockedTicks%240==0) call(g,"HURRY UP!");
                blockedTicks++; boat.vy=load.vy=0; return;
            }
            blockedTicks=0; boat.vy=load.vy=-SPEED;
            if(load.y<=DROP_Y) {
                for(int i=0;i<g.cargo.length;i++) if(g.cargo[i]==null || !g.cargo[i].active) {
                    Body delivered=new Body(g.nextId++,load.berth,load.x,load.y,load.angle);
                    delivered.awaitingPickup=true; delivered.tiedX=delivered.x; delivered.tiedY=delivered.y; delivered.tiedAngle=delivered.angle;
                    g.cargo[i]=delivered; drops++; phase=RETURNING;
                    call(g,"ZONE "+(char)('A'+load.berth)+"!"); g.haptic(Game.HAPTIC_TICK); break;
                }
                load=null;
            }
        } else {
            boat.y+=SPEED*Physics.DT;
            blocked=!clear(g,boat,0);
            if(blocked) {
                boat.y=old; boat.vy=0;
                if(blockedTicks%240==0) call(g,"HURRY UP!");
                blockedTicks++;
            } else { blockedTicks=0; boat.vy=SPEED; }
            if(boat.y>START_Y+GAP) { phase=IDLE; blocked=false; boat.vy=0; }
        }
    }
}
