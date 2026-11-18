package com.bargeboss.game;

/** Shared, deterministic vessel fixtures. The painted hull remains fully colored. */
public final class VesselLighting {
    private VesselLighting() {}
    private static int warm(float opacity,int alpha,int rgb) {
        return ((int)(Physics.clamp(opacity,0,1)*alpha)<<24)|rgb;
    }
    /** Four gentle halos and a small hot lens, independent of scenery grading. */
    private static void lamp(Painter p,float x,float y,float radius,float opacity,int tick,float phase) {
        float energy=SceneryEffects.workIntensity(tick,phase);
        p.circle(x,y,radius*5.2f,warm(opacity*energy,13,0xffad46));
        p.circle(x,y,radius*3.3f,warm(opacity*energy,22,0xffbd58));
        p.circle(x,y,radius*1.8f,warm(opacity*energy,55,0xffce72));
        p.circle(x,y,radius*.8f,warm(opacity*energy,160,0xffdc92));
        p.circle(x,y,radius*.38f,warm(opacity*energy,238,0xfff0bd));
    }
    public static void tug(Game g,Painter p,Body b,float bob) {
        boolean river=p instanceof RiverPainter;
        RiverCamera camera=river?((RiverPainter)p).camera:null;
        float scale=river?camera.scale:1,size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
        float sx=river?camera.screenX(b.x,b.y):b.x,sy=river?camera.screenY(b.x,b.y):b.y;
        Painter canvas=river?((RiverPainter)p).canvas():p;
        for(int i=0;i<TugLightArt.COUNT;i++) {
            float opacity=TugLightArt.opacity(i,b.angle,river);if(opacity<=0)continue;
            float x=sx+TugLightArt.offset(i,b.angle,river,false)*size*scale;
            float y=sy+(TugLightArt.offset(i,b.angle,river,true)*size-bob)*scale;
            lamp(canvas,x,y,(i==0?1.25f:1.05f)*size/70*scale,opacity,g.visualTicks,b.id*.71f);
        }
    }
    /** Broken warm water shimmer is drawn before the hull and clipped to real water.
     * It is an authored stylized reflection, not a ray-traced light projection. */
    public static void reflection(Game g,Painter p,Body b) {
        boolean river=p instanceof RiverPainter;RiverCamera camera=river?((RiverPainter)p).camera:null;
        float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
        float sx=river?camera.screenX(b.x,b.y):b.x,sy=river?camera.screenY(b.x,b.y):b.y,scale=river?camera.scale:1;
        sx+=TugLightArt.offset(0,b.angle,river,false)*size*scale;
        float xx=sx,yy=sy+b.radius*.65f*scale;
        int reflection=((int)(153*SceneryEffects.workIntensity(g.visualTicks,b.id*.71f))<<24)|0xffba5c;
        if(river)WaterReflection.draw(g,p,camera.worldX(xx,yy),camera.worldY(xx,yy),12,36,reflection,b.id);
        else p.waterReflection(xx,yy+15,18,0,0,30,WaterFoam.phase(g.visualTicks,b.id),reflection);
    }
    /** Two newly authored low coaming service lamps: bow/stern centerline, +Z normal.
     * These fittings are explicitly additional geometry, not painted-source anchors. */
    public static float bargeLocalX(Body b,int fixture) { return (fixture==0?-1:1)*(b.half+b.radius*.65f); }
    public static final float BARGE_LAMP_Z=5;
    public static void barge(Game g,Painter p,Body b,float bob) {
        boolean river=p instanceof RiverPainter;RiverCamera camera=river?((RiverPainter)p).camera:null;
        float scale=river?camera.scale:1,c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        Painter canvas=river?((RiverPainter)p).canvas():p;
        for(int i=0;i<2;i++) {
            float local=bargeLocalX(b,i),wx=b.x+c*local,wy=b.y+s*local;
            float sx=river?camera.screenX(wx,wy):wx,sy=(river?camera.screenY(wx,wy):wy)-(bob+BARGE_LAMP_Z*(river?.81649658f:1))*scale;
            canvas.rect(sx-1.1f*scale,sy-.9f*scale,2.2f*scale,3.1f*scale,0xff342c23);
            lamp(canvas,sx,sy,.7f*scale,1,g.visualTicks,b.id*.71f);
        }
    }
}
