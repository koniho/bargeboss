package com.bargeboss.game;

/** Organic scenery effects remain smooth, registered, deterministic and fade with their prop. */
public final class SceneryEffectsTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static boolean near(float a,float b) {return Math.abs(a-b)<.002f;}
    public static void main(String[] args) {run();}
    public static void run() {
        float previous=SceneryEffects.intensity(0,.8f),min=previous,max=previous;
        for(int tick=1;tick<=2400;tick++) {
            float value=SceneryEffects.intensity(tick,.8f);
            check(value>.79f && value<1.03f,"industrial glow remains bounded without on/off flashes");
            check(Math.abs(value-previous)<.006f,"glow changes smoothly between simulation ticks");
            min=Math.min(min,value);max=Math.max(max,value);previous=value;
        }
        check(max-min>.12f,"glow visibly breathes over time");
        Capture clear=new Capture();SceneryEffects.glow(clear,50,70,3,0xffffbd50,0,90,.8f);
        SceneryEffects.smoke(clear,50,70,3,1,0,90,.8f);check(clear.calls==0,"invisible prop has no remaining glow or smoke");
        Capture a=new Capture(),b=new Capture();
        SceneryEffects.glow(a,50,70,3,0xffffbd50,1,90,.8f);SceneryEffects.glow(b,50,70,3,0xffffbd50,1,90,.8f);
        SceneryEffects.smoke(a,50,70,3,1,1,90,.8f);SceneryEffects.smoke(b,50,70,3,1,1,90,.8f);
        check(a.hash==b.hash && a.calls==b.calls,"same visual clock and emitter produce identical primitives");
        check(near(a.lastX,50) && near(a.lastY,70),"smoke throat remains at its authored outlet");
        for(int frame=0;frame<2;frame++) for(float width:new float[]{960,1333.3334f}) {
            Game g=new Game();g.configureCanvas(width,600);g.reset();g.visualTicks=90;
            Capture p=new Capture();ChimneyArt.draw(g,p,frame,508,620);
            check(p.atlas==SpriteAtlas.CHIMNEYS && p.frame==frame,"both chimney views use shared source metadata");
            float x=g.layout.camera.screenX(508,620),y=g.layout.camera.screenY(508,620),size=ChimneyArt.WORLD_SIZE*g.layout.camera.scale;
            check(near(p.lastX,x+(ChimneyArt.outletX(frame)-ChimneyArt.pivotX(frame))*size)
                    && near(p.lastY,y+(ChimneyArt.outletY(frame)-ChimneyArt.pivotY(frame))*size),
                    "particle origin follows the exact sprite pivot and scale on all canvas sizes");
        }
        System.out.println(checks+" organic scenery glow, smoke, opacity and attachment assertions passed");
    }
    private static final class Capture implements Painter {
        int calls,atlas=-1,frame;long hash=17;float lastX,lastY;
        private void add(float x) {hash=hash*31+Float.floatToIntBits(x);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {atlas=a;frame=f;calls++;add(x);add(y);add(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {sprite(a,f,x,y,w,h,r,alpha);}
        public void rect(float x,float y,float w,float h,int color) {calls++;add(x);add(y);hash=hash*31+color;}
        public void circle(float x,float y,float r,int color) {calls++;lastX=x;lastY=y;add(x);add(y);add(r);hash=hash*31+color;}
        public void poly(float[] xy,int color) {calls++;for(float v:xy)add(v);hash=hash*31+color;}
        public void line(float x,float y,float xx,float yy,float w,int color) {calls++;add(x);add(y);add(xx);add(yy);hash=hash*31+color;}
    }
}
