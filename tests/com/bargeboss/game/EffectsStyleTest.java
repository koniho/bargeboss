package com.bargeboss.game;

/** Ambient draw budgets and water/air separation protect readability and deterministic playback. */
final class EffectsStyleTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int stamps,ripples,spray,clips;long hash=1;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void poly(float[] q,int c){}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){}
        public void pushClipOutside(float[] q){clips++;}public void popClip(){clips--;check(clips>=0,"Balanced effect clip scopes");}
        public boolean supportsUnionClips(){return true;}
        public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){
            check(Float.isFinite(x+y+ax+ay+bx+by+alpha) && alpha>=0 && alpha<=1,"Finite registered effect");
            stamps++;if(a==SpriteAtlas.RIPPLES)ripples++;if(a==SpriteAtlas.SPLASH)spray++;
            hash=hash*31+a;hash=hash*31+f;hash=hash*31+Float.floatToIntBits(ax);hash=hash*31+Float.floatToIntBits(alpha);
        }
    }
    static void run(){
        Game g=new Game();g.selectChallenge(Challenges.ISLAND_TAG);g.reset();g.settings.waterDetail=2;
        float[] view={0,0,ArenaCourse.Islands.W,ArenaCourse.Islands.H};g.visualTicks=73;
        Capture a=new Capture(),b=new Capture();int tick=g.visualTicks;IslandArt.ripples(g,a,g.layout.camera,view);IslandArt.ripples(g,b,g.layout.camera,view);
        check(a.stamps>0 && a.stamps<=2*ArenaCourse.Islands.ISLES.length,"At most two stamps per island");
        check(a.hash==b.hash && g.visualTicks==tick,"Repeated draws preserve phase and simulation");
        g.visualTicks+=11;Capture moving=new Capture();IslandArt.ripples(g,moving,g.layout.camera,view);check(moving.hash!=a.hash,"Ripple expansion follows shared ticks");
        Capture off=new Capture();IslandArt.ripples(g,off,g.layout.camera,new float[]{-10000,-10000,-9000,-9000});check(off.stamps==0,"Offscreen islands submit no ripples");
        g.settings.waterDetail=0;Capture low=new Capture();IslandArt.ripples(g,low,g.layout.camera,view);check(low.stamps==0,"Water detail toggle removes ambient ripple cost");
        g.selectChallenge(Challenges.MONTROSE);g.reset();g.montroseArrival.clear();g.tug.x=1300;g.tug.y=1000;g.tug.vx=90;g.tug.angle=0;g.transit.boostTicks=80;
        Capture boost=new Capture();TransitEffects.bow(g,boost,true);check(boost.stamps<=8 && boost.spray==6 && boost.clips==0,"Boost has six spray packets, two prop lanes and restored clips");
        float x=g.tug.x,y=g.tug.y;g.layout=Layout.focused(960,600,x,y,1,480,300);
        for(int phase:new int[]{4,40,126,138,160,260}){
            Capture water=new Capture(),air=new Capture();HullDamage.wreckWater(g,water,g.layout.camera,x,y,24,12,7,phase);HullDamage.wreck(g,air,g.layout.camera,x,y,24,12,7,false,phase);
            check(water.spray==0 && water.ripples<=4 && water.clips==0,"Bounded water fronts stay in the clipped water pass");
            check(air.ripples==0,"Upright effect pass never paints water rings over hulls");
        }
        System.out.println(checks+" effect budget, phase, culling, quality toggle and water/air separation checks passed");
    }
    public static void main(String[] args){run();}
}
