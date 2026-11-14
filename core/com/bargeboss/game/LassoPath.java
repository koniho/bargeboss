package com.bargeboss.game;

/** One shared trajectory for aiming, flight and recoil, with a hull-edge origin. */
public final class LassoPath {
    public static final float ARC_HEIGHT=30,DECK_HEIGHT=4;
    private LassoPath() {}
    /** Ray intersection with the rounded hull envelope enclosing the collision discs. */
    public static void edge(Body b,float targetX,float targetY,float[] out) {
        float dx=targetX-b.x,dy=targetY-b.y,length=Physics.length(dx,dy);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        if(length<.001f) {dx=c;dy=s;length=1;}
        dx/=length;dy/=length;
        float along=Math.abs(dx*c+dy*s),across=Math.abs(-dx*s+dy*c);
        float sideDistance=across>.0001f?b.radius/across:Float.MAX_VALUE;
        float distance=sideDistance*along<=b.half?sideDistance:
                b.half*along+(float)StrictMath.sqrt(Math.max(0,b.radius*b.radius-b.half*b.half*across*across));
        out[0]=b.x+dx*distance;out[1]=b.y+dy*distance;
    }
    public static float liftX(boolean river,float height) {return river?RiverCamera.riseX(height):0;}
    public static float liftY(boolean river,float height) {return river?RiverCamera.riseY(height):-height;}
    public static void sample(float sx,float sy,float ex,float ey,float deckHeight,float fraction,boolean river,float[] out) {
        float t=Physics.clamp(fraction,0,1);
        float height=deckHeight*(1-t)+ARC_HEIGHT*(float)StrictMath.sin(t*Math.PI);
        out[0]=sx+(ex-sx)*t+liftX(river,height);
        out[1]=sy+(ey-sy)*t+liftY(river,height);
    }
}
