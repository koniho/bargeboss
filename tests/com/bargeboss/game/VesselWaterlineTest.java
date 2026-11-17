package com.bargeboss.game;

/** Sprite contact, physically distinct foam sources and detached history remain deterministic. */
public final class VesselWaterlineTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Trace { int hash=1,points; }
    private static Trace draw(Game g,Body b) {
        Trace out=new Trace();
        float[] edge=VesselWaterline.boundary(g,null,b,b.berth<0,2*(b.half+b.radius)/.78f);
        for(float value:edge){check(Float.isFinite(value),"finite water boundary");out.hash=out.hash*31+Float.floatToIntBits(value);}
        out.points=edge.length/2;return out;
    }
    private static final class FoamTrace implements Painter {
        int patches,rings,foam,clips,depth;float alpha,maxWidth,minX=Float.MAX_VALUE,maxLength;
        public void pushClipOutside(float[] xy){clips++;depth++;}
        public void popClip(){depth--;check(depth>=0,"ripple masks balance");}
        public void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float opacity){
            check((atlas==SpriteAtlas.RIPPLES || atlas==SpriteAtlas.FOAM) && frame>=0 && frame<4,"uses registered water stamps");
            if(atlas==SpriteAtlas.RIPPLES)rings++;else foam++;
            minX=Math.min(minX,x);maxLength=Math.max(maxLength,Physics.length(ax,ay));
            patches++;alpha=Math.max(alpha,opacity);maxWidth=Math.max(maxWidth,Physics.length(bx,by));
        }
        public void rect(float x,float y,float w,float h,int c){} public void circle(float x,float y,float r,int c){}
        public void poly(float[] p,int c){} public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run() {
        Game g=new Game();g.mode=Game.TITLE;
        for(int kind=0;kind<2;kind++)for(int heading=0;heading<64;heading++) {
            Body b=new Body(kind,kind-1,300,400,heading*(float)StrictMath.PI/32);b.floatingInitialized=true;
            g.visualTicks=0;Trace idle=draw(g,b),again=draw(g,b);
            check(idle.hash==again.hash && idle.points==25,"repeatable bounded invisible water boundary");
            g.visualTicks=17;check(draw(g,b).hash!=idle.hash,"idle contact follows surface waves");
            g.visualTicks=0;b.vx=65;Trace moving=draw(g,b);
            check(moving.hash==idle.hash,"contact boundary does not emit speed-dependent random foam");
            b.heave=1.5f;check(draw(g,b).hash==moving.hash,"water contact stays on the surface while the hull heaves independently");
            check(b.x==300 && b.y==400 && g.visualTicks==0,"rendering does not advance vessels");
        }
        Body tug=new Body(0,-1,300,400,0),barge=new Body(1,0,300,400,0);
        g.visualTicks=45;
        FoamTrace idle=new FoamTrace();VesselRipples.draw(g,idle,0,300,400,0,80,30,0,false);
        FoamTrace fast=new FoamTrace();VesselRipples.draw(g,fast,0,300,400,0,80,30,48,false);
        check(idle.rings==2 && idle.foam==0 && fast.foam==2 && fast.rings==0 && fast.alpha>idle.alpha && fast.maxWidth>idle.maxWidth,"two fluffy foam cycles replace rings above hull speed");
        FoamTrace crossing=new FoamTrace();VesselRipples.draw(g,crossing,0,300,400,0,80,30,WaterFoam.SHED_SPEED+5,false);
        check(crossing.rings==2 && crossing.foam==2,"speed crossover blends rather than popping between textures");
        FoamTrace slow=new FoamTrace(),cruise=new FoamTrace();
        VesselRipples.draw(g,slow,0,300,400,0,80,30,10,false);
        VesselRipples.draw(g,cruise,0,300,400,0,80,30,28,false);
        check(idle.clips==0 && slow.clips==2 && cruise.clips==2 && slow.depth==0 && cruise.depth==0,"moving ripples use balanced stern arc masks, idle keeps circles");
        check(cruise.minX<slow.minX && cruise.maxLength>slow.maxLength,"higher displacement speed elongates arcs and shifts them aft");
        FoamTrace later=new FoamTrace();g.visualTicks=60;VesselRipples.draw(g,later,0,300,400,0,80,30,0,false);
        check(later.maxWidth>idle.maxWidth,"rings expand over lifetime");
        FoamTrace berth=new FoamTrace();VesselRipples.draw(g,berth,0,300,400,0,40,16,0,true);
        check(berth.alpha<idle.alpha,"moored vessels retain subdued ripples");

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
