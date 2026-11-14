package com.bargeboss.game;

/** Contact animation stays deterministic, bounded and alive at rest in all turn sectors. */
public final class VesselWaterlineTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Trace implements Painter {
        int hash=1,lines;float totalWidth;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void poly(float[] p,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int color) {
            check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(xx) && Float.isFinite(yy),"finite contact contour at every heading");
            check(w>0 && w<24 && (color>>>24)<250,"soft bounded contact effect");
            totalWidth+=w;lines++;hash=hash*31+Float.floatToIntBits(x);hash=hash*31+Float.floatToIntBits(y);hash=hash*31+color;
        }
    }
    private static Trace draw(Game g,Body b) {
        Trace p=new Trace();float size=2*(b.half+b.radius)/.78f;
        Painter river=new RiverPainter(p,new RiverCamera(0,0,2));
        VesselWaterline.draw(g,river,b,b.berth<0,size,false);VesselWaterline.draw(g,river,b,b.berth<0,size,true);return p;
    }
    public static void run() {
        Game g=new Game();g.mode=Game.TITLE;
        for(int kind=0;kind<2;kind++)for(int heading=0;heading<64;heading++) {
            Body b=new Body(kind,kind-1,300,400,heading*(float)StrictMath.PI/32);b.floatingInitialized=true;
            g.visualTicks=0;Trace idle=draw(g,b),again=draw(g,b);
            check(idle.hash==again.hash && idle.lines==72,"render repeats exactly without advancing time, with fixed submission cost");
            g.visualTicks=17;check(draw(g,b).hash!=idle.hash,"idle contact continues animating");
            g.visualTicks=0;b.vx=20;Trace slow=draw(g,b);
            b.vx=65;Trace fast=draw(g,b);
            check(slow.totalWidth>idle.totalWidth && fast.totalWidth>slow.totalWidth,"waterline thickness grows with actual speed");
            b.vx=-65;check(draw(g,b).totalWidth==fast.totalWidth,"reverse motion has the same thickness at the same speed");
            b.vx=0;b.vy=65;check(draw(g,b).totalWidth==fast.totalWidth,"sideways drift also builds a thick waterline");
            b.vy=0;
            b.vx=0;b.heave=1.5f;b.heaveVelocity=-4;check(draw(g,b).hash!=idle.hash,"heave moves contact and trough descent strengthens it");
            check(b.x==300 && b.y==400 && b.heave==1.5f && g.visualTicks==0,"effect does not mutate vessel or clock");
        }
        System.out.println(checks+" animated waterline direction, idle, heave and render-purity assertions passed");
    }
}
