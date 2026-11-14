package com.bargeboss.game;

import java.util.Arrays;

/** Host-side timing and complete 120-step pose sweep; timing never enters gameplay. */
public final class BridgeRigBenchmark implements Painter {
    private int polygons;
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {}
    public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha) {}
    public void rect(float x,float y,float w,float h,int color) {}
    public void circle(float x,float y,float r,int color) {}
    public void line(float x,float y,float a,float b,float w,int color) {}
    public void poly(float[] xy,int color) {
        if(xy.length<6)throw new AssertionError("Degenerate BSP fragment");
        for(float v:xy)if(!Float.isFinite(v))throw new AssertionError("Non-finite bridge projection");polygons++;
    }
    public static void main(String[] args) {
        Game g=new Game();g.challengeId=1;g.reset();g.layout=Layout.focused(960,600,300,Bridge.Y,1.5f,480,360);
        g.tug.x=300;g.tug.y=Bridge.Y+500;g.trafficBoat.x=8000;g.trafficBoat.y=8000;
        for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        for(Body b:g.cargo)if(b!=null)b.active=false;
        BridgeRigBenchmark p=new BridgeRigBenchmark();
        for(int i=0;i<40;i++)BridgeModel.MODEL.draw(g,p,BridgeModel.MODEL.pose(i/39f));
        long[] nanos=new long[121];int maxFragments=0,maxFaces=0,peakTick=0;long total=0;
        for(int tick=0;tick<=120;tick++) {
            p.polygons=0;long start=System.nanoTime();
            ArticulatedModel.Stats stats=BridgeModel.MODEL.draw(g,p,BridgeModel.MODEL.pose(tick/120f));
            nanos[tick]=System.nanoTime()-start;total+=nanos[tick];
            if(stats.fragments>maxFragments){maxFragments=stats.fragments;peakTick=tick;}
            maxFaces=Math.max(maxFaces,stats.drawnFaces);
            if(stats.drawnFaces!=p.polygons || stats.fragments>=3000)throw new AssertionError("Unbounded pose "+tick);
        }
        Arrays.sort(nanos);
        System.out.printf(java.util.Locale.ROOT,"121 poses: sourceFaces=%d maxFragments=%d (tick %d) maxDrawnFaces=%d; warmed geometry CPU mean=%.3fms p50=%.3fms p95=%.3fms max=%.3fms%n",
                BridgeModel.MODEL.sourceFaceCount(),maxFragments,peakTick,maxFaces,total/121e6,nanos[60]/1e6,nanos[114]/1e6,nanos[120]/1e6);
        System.out.println("Timing includes pose construction, BSP and projection, excludes native painter/raster cost. Host timing is not a device frame-rate guarantee.");
    }
}
