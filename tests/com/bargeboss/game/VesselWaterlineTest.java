package com.bargeboss.game;

/** Sprite contact, physically distinct foam sources and detached history remain deterministic. */
public final class VesselWaterlineTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Trace implements Painter {
        int hash=1,sprites;float energy;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void poly(float[] p,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int color){sprites++;energy+=((color>>>24)&255)/255f;hash=hash*31+Float.floatToIntBits(x);hash=hash*31+Float.floatToIntBits(y);}
        public void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
            check(atlas==SpriteAtlas.FOAM && frame>=0 && frame<4,"reviewed foam texture");
            check(Float.isFinite(x+ax+bx) && Float.isFinite(y+ay+by) && alpha>0 && alpha<=1,"finite sprite projection and opacity");
            sprites++;energy+=alpha;hash=hash*31+Float.floatToIntBits(x);hash=hash*31+Float.floatToIntBits(y);hash=hash*31+Float.floatToIntBits(alpha);
        }
    }
    private static Trace draw(Game g,Body b) {
        Trace p=new Trace();VesselWaterline.draw(g,new RiverPainter(p,new RiverCamera(0,0,2)),b,b.berth<0,2*(b.half+b.radius)/.78f,true);return p;
    }
    public static void run() {
        Game g=new Game();g.mode=Game.TITLE;
        for(int kind=0;kind<2;kind++)for(int heading=0;heading<64;heading++) {
            Body b=new Body(kind,kind-1,300,400,heading*(float)StrictMath.PI/32);b.floatingInitialized=true;
            g.visualTicks=0;Trace idle=draw(g,b),again=draw(g,b);
            check(idle.hash==again.hash && idle.sprites==24 && idle.energy>0,"repeatable bounded contact sprites");
            g.visualTicks=17;check(draw(g,b).hash!=idle.hash,"idle contact follows surface waves");
            g.visualTicks=0;b.vx=65;Trace moving=draw(g,b);
            check(moving.hash==idle.hash,"contact boundary does not emit speed-dependent random foam");
            b.heave=1.5f;check(draw(g,b).hash==moving.hash,"water contact stays on the surface while the hull heaves independently");
            check(b.x==300 && b.y==400 && g.visualTicks==0,"rendering does not advance vessels");
        }
        Body tug=new Body(0,-1,300,400,0),barge=new Body(1,0,300,400,0);
        VesselFoam foam=new VesselFoam();g.visualTicks=0;foam.emit(g,tug);
        check(foam.emitted==0,"idle hull creates no continuous whitewater");
        tug.engineLoad=1;foam.emit(g,tug);
        check(foam.propEmitted==2 && foam.emitted==2,"stationary loaded tug emits twin propeller wash");
        float oldX=foam.x[0],oldY=foam.y[0];tug.x+=100;tug.angle=2;foam.step();
        check(Math.abs(foam.x[0]-oldX)<1 && Math.abs(foam.y[0]-oldY)<1,"released foam drifts independently of source turns and translation");
        VesselFoam passive=new VesselFoam();barge.engineLoad=1;barge.vx=40;g.visualTicks=1;passive.emit(g,barge);
        check(passive.propEmitted==0 && passive.emitted==2,"moving barge generates displacement foam only");
        VesselFoam reverse=new VesselFoam();tug.x=300;tug.angle=0;g.tug=tug;g.reversing=true;g.visualTicks=0;reverse.emit(g,tug);
        check(reverse.vx[0]>0 && foam.vx[0]<0,"astern thrust reverses wash flow at the stern fitting");
        for(int tick=0;tick<160;tick++)foam.step();
        for(int i=0;i<VesselFoam.CAPACITY;i++)check(foam.age[i]>=foam.life[i],"unrefreshed foam dissipates");
        System.out.println(checks+" sprite foam direction, idle, prop load, drift and lifecycle assertions passed");
    }
}
