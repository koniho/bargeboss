package com.bargeboss.game;

/** The practice mooring ball in the middle of the woodpile basin. In the last lesson both fireworks
 * barges (cargo 1 and 2) are towed to it and tied on with the normal lasso; the ball is a floating
 * tutorial buoy (NavigationLights / Buoys), so it bobs, takes contacts and depth-sorts like the others. */
public final class PracticeMooring {
    /** Ball anchor: open water between the three fire-barge stations. */
    public static final float X=180,Y=-1370;
    /** Throw reach from a barge, the ball's float size, and the line's fitting height on the ball. */
    static final float REACH=165,SIZE=16,TOP=7;
    static final int HULLS=2;
    /** Tied barges this lesson (for the panel). */
    int count;

    /** The fireworks barges (found by ability: practice reorders cargo slots) belong to the ball from the mooring lesson on. */
    boolean owns(Game g,Body b){
        return g.tutorial!=null && g.tutorial.lesson>=Tutorial.MOOR && b!=null && b.ability==BargeAbilities.FIREWORKS && hullIndex(g,b)>=0;
    }
    /** The k-th fireworks barge in cargo order, or null. */
    static Body hull(Game g,int k){for(Body b:g.cargo)if(b!=null && b.active && b.ability==BargeAbilities.FIREWORKS && k--==0)return b;return null;}
    private static int hullIndex(Game g,Body b){for(int k=0;k<HULLS;k++)if(hull(g,k)==b)return k;return -1;}
    boolean tieable(Game g,Body b){
        return g.tutorial.lesson==Tutorial.MOOR && b.active && !b.lineAttached && Physics.length(b.x-x(g),b.y-y(g))<=REACH;
    }
    int tied(Game g){int n=0;for(int k=0;k<HULLS;k++){Body b=hull(g,k);if(b!=null && b.lineAttached)n++;}count=n;return n;}
    /** Every tied barge has been winched in to its riding line. */
    boolean snug(Game g){float bx=x(g),by=y(g);for(int k=0;k<HULLS;k++){Body b=hull(g,k);if(b==null || !b.lineAttached || Physics.length(b.x-bx,b.y-by)>LINE+2)return false;}return true;}
    /** Live ball position (it floats on its chain). */
    float x(Game g){int i=index(g);return i<0?X:NavigationLights.x(g,i);}
    float y(Game g){int i=index(g);return i<0?Y:NavigationLights.y(g,i);}
    static int index(Game g){return g.tutorial==null?-1:NavigationLights.practiceMooring(g);}
    /** Short riding line once the winch has drawn a caught barge in, and the winch speed (units/tick). */
    static final float LINE=72,WINCH=.55f;
    /** A tied barge is winched in to a short riding line, turning to lie to the ball, and kept clear of
     * the other moored barge. Tied hulls are outside the motion solver, so this moves them directly. */
    void moor(Game g,Body b){
        b.vx*=.8f;b.vy*=.8f;b.spin*=.8f;
        float bx=x(g),by=y(g),dx=bx-b.x,dy=by-b.y,d=Physics.length(dx,dy);
        if(d>LINE+.5f){float step=Math.min(WINCH,d-LINE);b.x+=dx/d*step;b.y+=dy/d*step;}
        Body other=null;for(int k=0;k<HULLS;k++){Body h=hull(g,k);if(h!=null && h!=b && h.lineAttached)other=h;}
        if(other!=null){
            float ox=b.x-other.x,oy=b.y-other.y,od=Physics.length(ox,oy),clear=b.radius+other.radius+b.half+8;
            if(od<clear && od>.001f){float push=Math.min(.6f,clear-od);b.x+=ox/od*push;b.y+=oy/od*push;}
        }
        // Lie bow-to-ball, easing the heading round.
        float want=(float)StrictMath.atan2(by-b.y,bx-b.x),turn=(float)StrictMath.atan2(StrictMath.sin(want-b.angle),StrictMath.cos(want-b.angle));
        b.angle+=Physics.clamp(turn,-.012f,.012f);
        b.tiedX=b.mooringX=b.x;b.tiedY=b.mooringY=b.y;b.tiedAngle=b.mooringAngle=b.angle;
        if(b.dockTime<1.2f)b.dockTime=Math.min(1.2f,b.dockTime+1/60f);
        if(b.lineSnapTicks>0)b.lineSnapTicks--;if(b.tieBurstTicks>0)b.tieBurstTicks--;
    }
}
