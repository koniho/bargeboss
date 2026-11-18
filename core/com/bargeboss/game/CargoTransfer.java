package com.bargeboss.game;

/** Stateless, fixed-tick three-second lift/land/return choreography. */
final class CargoTransfer {
    static final int PICK=24,LIFT=54,SWING=108,LAND=132,CLEAR=150;
    static final float PIVOT=110,LUFF=1.0f,HOVER=48,SLING=24;
    private CargoTransfer(){}
    static float ease(float tick,float from,float to){float t=Physics.clamp((tick-from)/(to-from),0,1);return t*t*(3-2*t);}
    static float phase(int tick){return ease(tick,LIFT,SWING)*(1-ease(tick,CLEAR,Game.UNLOAD_TICKS));}
    static float landZ(){return GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE;}
    static boolean attached(int tick){return tick>=PICK && tick<LAND;}
    static int dock(Game g,Body b){return g.calumetStage()?b.destination:b.berth;}
    static Body load(Game g,int dock){
        Body previous=null;
        for(Body b:g.cargo)if(b!=null && !b.yacht && !b.cruiser && dock(g,b)==dock){
            if(b.active && b.lineAttached && !b.deliveryRecorded && b.unloadingTicks>0)return b;
            if(b.deliveryRecorded && (previous==null || b.id>previous.id))previous=b;
        }
        return previous;
    }
    static final class Pose {
        final Body load;final int tick;final float phase,tipX,tipY,tipZ,x,y,z,hookZ,angle,minDepth,maxDepth;
        Pose(Game g,DockView.Crane crane,int dock){
            load=load(g,dock);tick=load==null?0:load.deliveryRecorded?Game.UNLOAD_TICKS:Math.min(Game.UNLOAD_TICKS,load.unloadingTicks);
            phase=phase(tick);float pitch=phase*LUFF,c=(float)StrictMath.cos(crane.heading),s=(float)StrictMath.sin(crane.heading);
            tipX=crane.x+c*(crane.reach*(float)StrictMath.cos(pitch)-4*(float)StrictMath.sin(pitch));tipY=crane.y+s*(crane.reach*(float)StrictMath.cos(pitch)-4*(float)StrictMath.sin(pitch));
            tipZ=landZ()+PIVOT+crane.reach*(float)StrictMath.sin(pitch)+4*(float)StrictMath.cos(pitch);
            angle=load==null?crane.heading+(float)Math.PI/2:load.deliveryRecorded?load.tiedAngle:load.angle;
            if(load!=null && tick>=LAND){x=crane.landX();y=crane.landY();z=landZ();}
            else if(load!=null){float shift=ease(tick,LIFT,SWING);
                // Follow the exact rigid jib's horizontal motion during transfer.
                if(tick>=LIFT){float reach=(crane.reach*(float)StrictMath.cos(pitch)-4*(float)StrictMath.sin(pitch));x=crane.x+c*reach+(load.x-crane.pickX)*(1-shift);y=crane.y+s*reach+(load.y-crane.pickY)*(1-shift);}else{x=load.x;y=load.y;}
                float bob=WaterSurface.lift(g,load)/ArticulatedModel.Z_SCALE;
                z=bob+(HOVER-bob)*ease(tick,PICK,LIFT)+(landZ()-HOVER)*ease(tick,SWING,LAND);
            }else{x=tipX;y=tipY;z=0;}
            ArticulatedModel.Pose mesh=crane.motion.pose(phase);float lo=Float.POSITIVE_INFINITY,hi=Float.NEGATIVE_INFINITY;
            for(int node=0;node<crane.model.nodeCount();node++){float[] bounds=mesh.nodeWorldBounds(node);if(bounds!=null){lo=Math.min(lo,bounds[1]-bounds[3]);hi=Math.max(hi,bounds[4]-bounds[0]);}}
            minDepth=lo;maxDepth=hi;
            float resting=tipZ-22;
            if(tick<PICK)hookZ=resting+(z+SLING-resting)*ease(tick,0,PICK);
            else if(tick<LAND)hookZ=z+SLING;
            else hookZ=landZ()+SLING+(resting-landZ()-SLING)*ease(tick,LAND,CLEAR);
        }
        boolean cargo(){return load!=null && tick>=PICK;}
        float hookX(){return tick<LAND && load!=null?x:tipX;}
        float hookY(){return tick<LAND && load!=null?y:tipY;}
    }
}
