package com.bargeboss.game;

/** Real-time dock winch: tighten, draw alongside, then hand over to the unloading crew. */
public final class Mooring {
    public static final int TIGHTEN_TICKS=0,ALIGN_TICKS=72;
    private Mooring() {}
    public static void attach(Body b) {
        b.lineAttached=true;b.lineSnapTicks=18;b.tieBurstTicks=30;b.dockTime=1.2f;b.mooringTicks=0;b.unloadingTicks=0;
        b.mooringX=b.tiedX=b.x;b.mooringY=b.tiedY=b.y;b.mooringAngle=b.tiedAngle=b.angle;
        b.vx=b.vy=b.spin=0;
    }
    public static boolean aligning(Body b) {
        return b.lineAttached && b.mooringTicks>=TIGHTEN_TICKS && b.mooringTicks<TIGHTEN_TICKS+ALIGN_TICKS;
    }
    public static void step(Game g,Body b) {
        if(g.berthBlocker(b.berth,b)!=null)return;
        if(b.mooringTicks<TIGHTEN_TICKS+ALIGN_TICKS) b.mooringTicks++;
        b.dockTime=1.2f;
        if(b.mooringTicks<=TIGHTEN_TICKS) return;
        float t=(b.mooringTicks-TIGHTEN_TICKS)/(float)ALIGN_TICKS;
        float eased=t*t*(3-2*t);
        float heading=(float)(StrictMath.sin(b.mooringAngle)>=0?Math.PI/2:-Math.PI/2);
        b.angle=Physics.angle(b.mooringAngle+Physics.angle(heading-b.mooringAngle)*eased);
        float targetX=g.indianaStage()?g.dockX(b.berth):g.orderStage()?g.dockX(b.berth):g.berthRight(b.berth)?Physics.RIGHT-b.radius-10:Physics.LEFT+b.radius+10;
        b.x=b.mooringX+(targetX-b.mooringX)*eased;
        b.y=b.mooringY+(g.dockY(b.berth)-b.mooringY)*eased;
        // Keep the complete rotating hull in the berth, even when caught near its edge.
        float ex=b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(b.angle));
        float ey=b.radius+b.half*(float)StrictMath.abs(StrictMath.sin(b.angle));
        float minX=Math.max((g.indianaStage()?250:Physics.LEFT)+ex,g.dockX(b.berth)-g.dockHalfWidth()+ex),maxX=Math.min((g.indianaStage()?500:g.orderStage()?PhysicalBerths.RIGHT:Physics.RIGHT)-ex,g.dockX(b.berth)+g.dockHalfWidth()-ex);
        float minY=g.dockY(b.berth)-53+ey,maxY=g.dockY(b.berth)+53-ey;
        // A moving throw may catch just outside the berth. Reduce its initial
        // overshoot smoothly instead of teleporting the hull back inside.
        b.x=Physics.clamp(b.x,minX+Math.min(0,b.mooringX-minX)*(1-eased),maxX+Math.max(0,b.mooringX-maxX)*(1-eased));
        b.y=Physics.clamp(b.y,minY+Math.min(0,b.mooringY-minY)*(1-eased),maxY+Math.max(0,b.mooringY-maxY)*(1-eased));
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;
        if(b.mooringTicks>=TIGHTEN_TICKS+ALIGN_TICKS && g.inDock(b)) b.unloadingTicks++;
    }
}
