package com.bargeboss.game;

/** Navy Pier fire call: lock in from the lake, collect the Engine 2 fire barge,
 * lock out with it in tow and spray the burning wheel gondolas. */
public final class ChicagoMission {
    public static final int TO_LOCK=0,FETCH=1,TOW_OUT=2,FIGHT=3,DONE=4;
    final HarborLock lock=new HarborLock();
    final ChicagoFire fire=new ChicagoFire();
    final ChicagoView.Fades fades=new ChicagoView.Fades();
    public int phase;
    Body barge;
    /** Open water south of the pier, inside monitor range of every burning gondola. */
    static final float[] SPRAY=sprayPoint();
    void setup(Game g){phase=TO_LOCK;lock.reset();fire.reset();fire.ignite();
        g.addCargo(2,ChicagoRiver.FIRE_DOCK_X,ChicagoRiver.FIRE_DOCK_Y);
        barge=g.cargo[0];barge.ability=BargeAbilities.FIRE;barge.angle=ChicagoRiver.FIRE_DOCK_ANGLE;
        barge.vx=barge.vy=barge.spin=0;barge.awaitingPickup=true;barge.tiedX=barge.x;barge.tiedY=barge.y;barge.tiedAngle=barge.angle;
    }
    public boolean complete(){return phase==DONE;}
    boolean towing(Game g){return barge!=null && barge.active && g.tow.connected(barge,g.tug);}
    /** Upstream (river) side of the chamber, not merely west of its centre. */
    private boolean river(Body b){return lock.along(b.x,b.y)<-b.half-b.radius;}
    private boolean lake(Body b){return lock.along(b.x,b.y)>lock.length+b.half+b.radius;}
    void step(Game g){
        lock.step(g);fire.step(g);fades.step(g);
        if(phase==TO_LOCK && river(g.tug)){phase=FETCH;g.tell("RIVER SIDE / FIRE BARGE AT ENGINE 2");g.haptic(Game.HAPTIC_SUCCESS);}
        if(phase==FETCH && towing(g)){phase=TOW_OUT;g.tell("FIRE BARGE IN TOW / BACK THROUGH THE LOCK");g.haptic(Game.HAPTIC_SUCCESS);}
        if(phase==TOW_OUT && lake(barge) && towing(g)){phase=FIGHT;g.tell("CLEAR OF THE LOCK / TAP THE BARGE / SPRAY THE WHEEL");g.haptic(Game.HAPTIC_SUCCESS);}
        if(phase==FIGHT && fire.out()){phase=DONE;g.delivered=1;g.score+=1500;g.tell("FIRE OUT / NAVY PIER IS SAFE");g.haptic(Game.HAPTIC_SUCCESS);}
    }
    float targetX(Game g){return target(g,true);}
    float targetY(Game g){return target(g,false);}
    private float target(Game g,boolean x){
        if(phase==FETCH)return x?barge.x:barge.y;
        if(phase==FIGHT || phase==DONE)return x?SPRAY[0]:SPRAY[1];
        // Through the chamber toward the destination side: centre first, then the far gate.
        int destination=phase==TO_LOCK?HarborLock.RIVER:HarborLock.LAKE;
        float s=lock.inside(g.tug)?(destination==HarborLock.RIVER?-150:lock.length+150):lock.length*.5f;
        return x?lock.originX+lock.axisX*s:lock.originY+lock.axisY*s;
    }
    /** Short world-space guidance label for the current destination. */
    String label(Game g){
        if(phase==FETCH)return "ENGINE 2 FIRE BARGE";
        if(phase==FIGHT)return "SPRAY STATION";
        if(phase==TOW_OUT && !towing(g))return "RECONNECT FIRE BARGE";
        return lock.inside(g.tug)?(phase==TO_LOCK?"RIVER GATES":"LAKE GATES"):"HARBOR LOCK";
    }
    String status(Game g){
        if(phase==TO_LOCK)return "FIRE AT NAVY PIER / LOCK IN TO ENGINE 2 / "+lock.signal();
        if(phase==FETCH)return "THROW A LINE TO THE FIRE BARGE";
        if(phase==TOW_OUT)return towing(g)?"TOW THE FIRE BARGE OUT / "+lock.signal():"RECONNECT THE FIRE BARGE";
        if(phase==FIGHT)return "SPRAY THE WHEEL / "+fire.remaining()+" FIRES LEFT";
        return "FIRE OUT / NAVY PIER IS SAFE";
    }
    private static float[] sprayPoint(){
        float x=ChicagoRiver.WHEEL_X,y=ChicagoRiver.WHEEL_Y;
        // March south from the wheel to open water, then stand off from the pier wall.
        float step=4;int guard=0;
        while(!ChicagoRiver.contains(x,y) && guard++<200)x-=step;
        return new float[]{x-32,y};
    }
}
