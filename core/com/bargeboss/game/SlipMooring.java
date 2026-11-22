package com.bargeboss.game;

/** Optional tug tether. The engine stays live; only outward motion is constrained. */
final class SlipMooring {
    static final float X=-1930,Y=TrainingChannel.SLIP_BOTTOM+24;
    private final float[] fitting=new float[2];
    float length;
    static boolean available(Game g){return g.tutorial!=null && !g.tutorial.entering && g.tutorial.lesson>=Tutorial.SLIP && g.tutorial.lesson<Tutorial.DONE;}
    static boolean eligible(Game g){return available(g) && !g.tug.lineAttached && g.tug.x<TrainingChannel.BASIN_LEFT && g.tug.x>=TrainingChannel.SLIP_LEFT && g.tug.y>=TrainingChannel.SLIP_TOP && g.tug.y<=TrainingChannel.SLIP_BOTTOM && !g.tutorial.navigation.retry;}
    static float x(){return X+RiverCamera.riseX(GroundArt.LAND_RISE);}
    static float y(){return Y+RiverCamera.riseY(GroundArt.LAND_RISE);}
    void attach(Game g){
        Body b=g.tug;
        b.lineAttached=true;b.lineSnapTicks=18;b.tieBurstTicks=30;b.dockTime=1.2f;
        b.mooringX=b.x;b.mooringY=b.y;b.mooringAngle=b.angle;b.mooringTicks=Mooring.ALIGN_TICKS;
        RopeAttachment.mooring(b,x(),y(),fitting);
        length=Math.max(12,Physics.length(fitting[0]-X,fitting[1]-Y));
    }
    void constrain(Game g){
        Body b=g.tug;if(!b.lineAttached)return;
        RopeAttachment.mooring(b,x(),y(),fitting);
        float dx=fitting[0]-X,dy=fitting[1]-Y,d=Physics.length(dx,dy);
        if(d<=length || d<.001f)return; // Slack never pushes a hull away from shore.
        dx/=d;dy/=d;
        float rx=fitting[0]-b.x,ry=fitting[1]-b.y,cross=rx*dy-ry*dx;
        float inverse=1/b.mass+cross*cross/b.inertia;
        float outward=(b.vx-b.spin*ry)*dx+(b.vy+b.spin*rx)*dy;
        if(outward>0){float impulse=outward/inverse;b.vx-=dx*impulse/b.mass;b.vy-=dy*impulse/b.mass;b.spin-=cross*impulse/b.inertia;}
        // A hard length limit bounds engine-driven stretch; tangential motion,
        // inward travel and rotation remain free. Collision resolution precedes this.
        b.x-=dx*(d-length);b.y-=dy*(d-length);
    }
    float slack(Game g){
        if(!g.tug.lineAttached)return 0;
        RopeAttachment.mooring(g.tug,x(),y(),fitting);
        return Math.min(9,Math.max(0,length-Physics.length(fitting[0]-X,fitting[1]-Y))*.5f);
    }
    static void release(Game g){
        g.tug.lineAttached=false;g.tug.lineSnapTicks=g.tug.tieBurstTicks=0;g.tug.mooringTicks=0;
        if(g.ropeBody==g.tug){g.ropeBody=null;g.ropePhase=g.ropeTicks=0;}
        g.haptic(Game.HAPTIC_TICK);
    }
}
