package com.bargeboss.game;

/** One permanent Calumet geography; missions select objectives, never shoreline shapes.
 * TrainingChannel retains the historical canal authoring helpers used by tutorial gates. */
public final class CalumetMap {
    public static final float EAST=628,BERTH_X=599,BERTH_MOUTH=570;
    private CalumetMap(){}
    public static boolean contains(float x,float y){return TrainingChannel.contains(x,y);}
    public static float[][] water(){return TrainingChannel.WATER_RECTANGLES;}
    public static float collide(Body b,CollisionSplashes splashes){float vx=b.vx,vy=b.vy;TrainingChannel.collideWater(b,splashes);return Physics.length(vx-b.vx,vy-b.vy)/1.08f;}
    public static boolean clear(float x,float y,float xx,float yy,float pad){
        float distance=Physics.length(xx-x,yy-y);int steps=Math.max(1,(int)StrictMath.ceil(distance/3));
        for(int i=0;i<=steps;i++){float t=i/(float)steps,cx=x+(xx-x)*t,cy=y+(yy-y)*t;
            if(!contains(cx,cy)||!contains(cx-pad,cy)||!contains(cx+pad,cy)||!contains(cx,cy-pad)||!contains(cx,cy+pad))return false;}
        return true;
    }
}
