package com.bargeboss.game;
/** 80 ft yacht; one uniform scale and translated waterline pivots, no per-pose stretch.
 * Source model anchors remain estimates, explicitly recorded in the art study. */
public final class YachtArt {
    public static final float HULL_FRACTION=.78f,SCALE=0.9690734f;
    private static final float[] X={0.5080618f,0.5080618f,0.5080618f,0.5080618f,0.5080618f,0.5000000f,0.5000000f,0.5080618f,0.5080618f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.4919382f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5080618f,0.5000000f,0.5000000f,0.5000000f,0.5000000f,0.5080618f,0.5161236f,0.5161236f,0.5161236f,0.5161236f},Y={0.7280618f,0.7280618f,0.7280618f,0.7280618f,0.7280618f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7119382f,0.7119382f,0.7119382f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f,0.7200000f};
    private YachtArt(){}
    /** Estimated model mast; body-local projection remains continuous through pose changes. */
    public static void mast(Game g,Painter p,Body b,float bob){
        float x=b.x-13.2f*(float)StrictMath.cos(b.angle),y=b.y-13.2f*(float)StrictMath.sin(b.angle);
        float scale=1;
        if(p instanceof RiverPainter){RiverPainter r=(RiverPainter)p;float sx=r.camera.screenX(x,y);y=r.camera.screenY(x,y);x=sx;scale=r.camera.scale;p=r.canvas();}
        y-=(46.2f*.81649658f+bob)*scale;
        SceneryEffects.glow(p,x,y,1.8f*scale,0xffffd59a,.72f,g.visualTicks,.3f);
    }
    public static float pivotX(int frame){return X[frame];}
    public static float pivotY(int frame){return Y[frame];}
}
