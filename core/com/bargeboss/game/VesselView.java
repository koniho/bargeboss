package com.bargeboss.game;

/** Directional painted hulls with deterministic, phase-driven water and exhaust. */
public final class VesselView {
    private VesselView() {}
    public static void draw(Game g,Painter p,Body b,boolean tug) { draw(g,p,b,tug,true); }
    public static void draw(Game g,Painter p,Body b,boolean tug,boolean wake) {
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        float[] clip=river==null || g.haulout.vesselRise(b)>=MontroseHarbor.LAND_RISE?ShoreOcclusion.EMPTY:ShoreOcclusion.vessel(g,river.camera,b,false,river.canvas().supportsUnionClips());
        if(clip.length>0)river.canvas().pushClipOutside(clip);
        try {if(b.cruiser)CruiserView.draw(g,p,b);else paint(g,p,b,tug,wake);if(tug)g.spotlight.lamp(g,p);if(tug || g.radio.transmitting(g,b))VesselRadioView.draw(g,p,b);} finally {if(clip.length>0)river.canvas().popClip();}
    }
    private static void paint(Game g,Painter p,Body b,boolean tug,boolean wake) {
        int tick=g.visualTicks;
        // A sinking hull settles under its fixed waterline clip.
        float bob=WaterSurface.lift(g,b)-HullDamage.sinkDepth(b);
        boolean river=p instanceof RiverPainter;
        float hullX=b.x+(river?RiverCamera.riseX(bob):0),hullY=b.y+(river?RiverCamera.riseY(bob):-bob);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float size=2*(b.half+b.radius)/(b.yacht?YachtArt.HULL_FRACTION:tug?TugArt.HULL_FRACTION:BargeArt.HULL_FRACTION);
        float[] surface=river?ShoreOcclusion.vessel(g,((RiverPainter)p).camera,b,true,((RiverPainter)p).canvas().supportsUnionClips()):ShoreOcclusion.EMPTY;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        if(surface.length>0)canvas.pushClipOutside(surface);
        try {
            if(tug)VesselLighting.reflection(g,p,b);
            VesselNavigationLights.reflections(g,p,b,tug);
            ShadowView.vessel(g,p,b,tug,size);
            VesselRipples.draw(g,p,b.id,b.x,b.y,b.angle,2*(b.half+b.radius),2*b.radius,b.speed(),false);
            if(g.mode!=Game.TITLE)WaterFoam.bow(g,p,b);
        } finally {if(surface.length>0)canvas.popClip();}
        float[] waterline=VesselWaterline.boundary(g,p,b,tug,size);
        float waterScale=river?((RiverPainter)p).camera.scale:1;
        canvas.pushClipOutside(VesselWaterline.submerged(waterline,size*waterScale));
        try {
            boolean flash=tug && b==g.tug && g.damageFlashTicks>0 && (40-g.damageFlashTicks)%20<7;
            if(flash) {
                // A bright hull-shaped pulse under the fading sprite flashes the
                // vessel itself; normal fender contacts never start this timer.
                p.line(hullX-c*b.half,hullY-s*b.half,hullX+c*b.half,hullY+s*b.half,
                        2*b.radius+7,0xccff534b);
                p.line(hullX-c*b.half,hullY-s*b.half,hullX+c*b.half,hullY+s*b.half,
                        2*b.radius+1,0xffffb69a);
            }
            SpriteAtlas.draw(p,b==g.teacherBoat()?SpriteAtlas.TEACHER_TUG:b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,hullX,hullY,size,size,flash?.28f:1);
            SurfaceLighting.directional(g,p,b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,hullX,hullY,size,flash?.28f:1,b);
            RopeAttachment.fittings(g,p,b);
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
                CargoView.draw(g,p,b,hullX,hullY,size);
                VesselHighlight.draw(g,p,b,false,hullX,hullY,size);
                VesselNavigationLights.draw(g,p,b,false,bob,true);
                if(b.lineAttached || (b.berth>=0 && g.canTie(b))) {
                    float pulse=(float)StrictMath.sin(tick*.12)*.5f+.5f;
                    p.circle(b.x-c*28+s*9,b.y-s*28-c*9,1.7f+pulse,b.lineAttached?0xffd3e8a1:0xffffd34e);
                }
            }
        } finally {canvas.popClip();}
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
