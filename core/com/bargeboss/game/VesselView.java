package com.bargeboss.game;

/** Directional painted hulls with deterministic, phase-driven water and exhaust. */
public final class VesselView {
    private VesselView() {}
    public static void draw(Game g,Painter p,Body b,boolean tug) { draw(g,p,b,tug,true); }
    public static void draw(Game g,Painter p,Body b,boolean tug,boolean wake) {
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        float[] clip=river==null?ShoreOcclusion.EMPTY:ShoreOcclusion.vessel(g,river.camera,b,false);
        if(clip.length>0)river.canvas().pushClipOutside(clip);
        try {paint(g,p,b,tug,wake);} finally {if(clip.length>0)river.canvas().popClip();}
    }
    private static void paint(Game g,Painter p,Body b,boolean tug,boolean wake) {
        int tick=g.visualTicks;
        float bob=WaterSurface.lift(g,b);
        boolean river=p instanceof RiverPainter;
        float hullX=b.x+(river?RiverCamera.riseX(bob):0),hullY=b.y+(river?RiverCamera.riseY(bob):-bob);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float size=2*(b.half+b.radius)/(b.yacht?YachtArt.HULL_FRACTION:tug?TugArt.HULL_FRACTION:BargeArt.HULL_FRACTION);
        float[] surface=river?ShoreOcclusion.vessel(g,((RiverPainter)p).camera,b,true):ShoreOcclusion.EMPTY;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        if(surface.length>0)canvas.pushClipOutside(surface);
        try {
            if(tug)VesselLighting.reflection(g,p,b);
            VesselNavigationLights.reflections(g,p,b,tug);
            ShadowView.vessel(g,p,b,tug,size);
        } finally {if(surface.length>0)canvas.popClip();}
        boolean flash=tug && b==g.tug && g.damageFlashTicks>0 && (40-g.damageFlashTicks)%20<7;
        if(flash) {
            // A bright hull-shaped pulse under the fading sprite flashes the
            // vessel itself; normal fender contacts never start this timer.
            p.line(hullX-c*b.half,hullY-s*b.half,hullX+c*b.half,hullY+s*b.half,
                    2*b.radius+7,0x88ff935a);
            p.line(hullX-c*b.half,hullY-s*b.half,hullX+c*b.half,hullY+s*b.half,
                    2*b.radius+1,0xffffedc0);
        }
        SpriteAtlas.draw(p,b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,hullX,hullY,size,size,flash?.28f:1);
        SurfaceLighting.directional(g,p,b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,hullX,hullY,size,flash?.28f:1,b);
        if(tug) {
            VesselHighlight.draw(g,p,b,true,hullX,hullY,size);
            VesselLighting.tug(g,p,b,bob);
            VesselNavigationLights.draw(g,p,b,true,bob,true);
            exhaust(p,b,tick,b.engineLoad,bob);
        } else if(b.yacht) {
            YachtArt.mast(g,p,b,bob);
            VesselHighlight.draw(g,p,b,false,hullX,hullY,size);
            VesselNavigationLights.draw(g,p,b,false,bob,true);
        } else {
            // Coaming fittings sit behind cargo; the painted loads occlude them.
            VesselLighting.barge(g,p,b,bob);
            VesselNavigationLights.draw(g,p,b,false,bob,false);
            int remaining=b.unloadingTicks==0?3:Math.max(0,3-(b.unloadingTicks+50)/60);
            int atlas=b.berth==0?SpriteAtlas.STEEL:b.berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
            // Coal is registered to this hull and masked behind its coaming.
            // Fade quantity without moving the mask through the painted rim.
            if(b.berth==2 && b.unloadingTicks<Game.UNLOAD_TICKS) {
                float fill=1-b.unloadingTicks/(float)Game.UNLOAD_TICKS;
                SpriteAtlas.draw(p,atlas,b.angle,hullX,hullY,size,size,fill);
                SurfaceLighting.directional(g,p,atlas,b.angle,hullX,hullY,size,fill,b);
            }
            // Unloading removes the same indexed loads at every heading. Only
            // painter order changes: farther deck positions must draw first.
            if(b.berth!=2) {
                float[] cargoX=new float[remaining],cargoY=new float[remaining],depth=new float[remaining];
                int[] order=new int[remaining];
                for(int i=0;i<remaining;i++) {
                    float xx=b.berth==0?0:-17+i*17,yy=b.berth==0?-8+i*8:0;
                    cargoX[i]=hullX+c*xx-s*yy;cargoY[i]=hullY+s*xx+c*yy;
                    depth[i]=river?((RiverPainter)p).camera.screenY(cargoX[i],cargoY[i]):cargoY[i];
                    int j=i-1;
                    while(j>=0 && depth[order[j]]>depth[i]) {order[j+1]=order[j];j--;}
                    order[j+1]=i;
                }
                float cargoSize=b.berth==0?58:26;
                for(int n=0;n<remaining;n++) {
                    int i=order[n];
                    SpriteAtlas.draw(p,atlas,b.angle,cargoX[i],cargoY[i],cargoSize,cargoSize,1);
                    SurfaceLighting.directional(g,p,atlas,b.angle,cargoX[i],cargoY[i],cargoSize,1,b);
                }
            }
            VesselHighlight.draw(g,p,b,false,hullX,hullY,size);
            VesselNavigationLights.draw(g,p,b,false,bob,true);
            if(b.lineAttached || (b.berth>=0 && g.canTie(b))) {
                float pulse=(float)StrictMath.sin(tick*.12)*.5f+.5f;
                p.circle(b.x-c*28+s*9,b.y-s*28-c*9,1.7f+pulse,b.lineAttached?0xffd3e8a1:0xffffd34e);
            }
        }
        VesselWaterline.draw(g,p,b,tug,size,true);
    }
    private static void exhaust(Painter p,Body b,int tick,float power,float bob) {
        boolean river=p instanceof RiverPainter;
        float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
        float scale=river?((RiverPainter)p).camera.scale:1;
        float sx=river?((RiverPainter)p).camera.screenX(b.x,b.y):b.x;
        float sy=river?((RiverPainter)p).camera.screenY(b.x,b.y):b.y;
        sx+=SpriteAtlas.stackOffset(b.angle,river,false)*size*scale;
        sy+=(SpriteAtlas.stackOffset(b.angle,river,true)*size-bob)*scale;
        if(river) p=((RiverPainter)p).canvas();
        power=Physics.clamp(power,0,1);
        // Only the fresh throat stays attached; the plume lives in VesselSmoke.
        p.circle(sx,sy,(2+power)*scale,0xc02b3540);
    }
}
