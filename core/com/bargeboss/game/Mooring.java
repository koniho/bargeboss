package com.bargeboss.game;

/** Real-time dock winch: tighten, draw alongside, then hand over to the unloading crew. */
public final class Mooring {
    public static final int TIGHTEN_TICKS=72,ALIGN_TICKS=72;
    private Mooring() {}
    public static void attach(Body b) {
        b.lineAttached=true;b.dockTime=0;b.mooringTicks=0;b.unloadingTicks=0;
        b.mooringX=b.tiedX=b.x;b.mooringY=b.tiedY=b.y;b.mooringAngle=b.tiedAngle=b.angle;
        b.vx=b.vy=b.spin=0;
    }
    public static boolean aligning(Body b) {
        return b.lineAttached && b.mooringTicks>=TIGHTEN_TICKS && b.mooringTicks<TIGHTEN_TICKS+ALIGN_TICKS;
    }
    public static void step(Game g,Body b) {
        if(b.mooringTicks<TIGHTEN_TICKS+ALIGN_TICKS) b.mooringTicks++;
        b.dockTime=Math.min(b.mooringTicks,TIGHTEN_TICKS)*Physics.DT;
        if(b.mooringTicks==TIGHTEN_TICKS) { b.lineSnapTicks=18;g.haptic(Game.HAPTIC_CONTACT); }
        if(b.mooringTicks<=TIGHTEN_TICKS) return;
        float t=(b.mooringTicks-TIGHTEN_TICKS)/(float)ALIGN_TICKS;
        float eased=t*t*(3-2*t);
        float heading=(float)(StrictMath.sin(b.mooringAngle)>=0?Math.PI/2:-Math.PI/2);
        b.angle=Physics.angle(b.mooringAngle+Physics.angle(heading-b.mooringAngle)*eased);
        float targetX=b.berth==1?Physics.RIGHT-b.radius-10:Physics.LEFT+b.radius+10;
        b.x=b.mooringX+(targetX-b.mooringX)*eased;
        b.y=b.mooringY+(Game.DOCK_Y[b.berth]-b.mooringY)*eased;
        // Keep the complete rotating hull in the berth, even when caught near its edge.
        float ex=b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(b.angle));
        float ey=b.radius+b.half*(float)StrictMath.abs(StrictMath.sin(b.angle));
        b.x=Physics.clamp(b.x,Math.max(Physics.LEFT+ex,Game.DOCK_X[b.berth]-55+ex),Math.min(Physics.RIGHT-ex,Game.DOCK_X[b.berth]+55-ex));
        b.y=Physics.clamp(b.y,Game.DOCK_Y[b.berth]-53+ey,Game.DOCK_Y[b.berth]+53-ey);
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;
        if(b.mooringTicks>=TIGHTEN_TICKS+ALIGN_TICKS) b.unloadingTicks++;
    }
}
