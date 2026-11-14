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
        Capture full=new Capture(),compact=new Capture(),changed=new Capture();
        SceneryEffects.glow(full,50,70,3,0xffffbd50,1,90,.8f);
        SceneryEffects.compactGlow(compact,50,70,3,0xffffbd50,1,90,.8f);
        SceneryEffects.glow(changed,80,95,7,0xffffbd50,.28f,173,2.3f);
        check(full.layers==1 && full.nativeCalls==5,"full glow submits one cached shell and four live accents");
        check(compact.layers==1 && compact.nativeCalls==2,"distant glow submits one cached shell and its live fixture core");
        check(full.layer==changed.layer && full.layer.key()==changed.layer.key(),"position, radius, phase and opacity reuse immutable kernel");
        check(full.layer.key()!=compact.layer.key(),"full and compact kernels have distinct cache identities");
        check((full.layer.key()&0xff000000)==0x40000000 && (compact.layer.key()&0xff000000)==0x41000000,"kernel keys are separate from terrain and building namespaces");
        check(changed.layerAlpha<full.layerAlpha && changed.layerScale>full.layerScale,"live fade and size remain outside cached pixels");
        check(near(full.lastX,50) && near(full.lastY,70),"bright core stays at its authored fixture while halo drifts");
        check(near(changed.lastX,80) && near(changed.lastY,95),"animated halo never drags the fixture lens");
        check(full.layer.width()==44 && full.layer.height()==44 && compact.layer.width()==44,"small glow kernels retain a fixed bounded raster footprint");
        for(StaticLayer layer:new StaticLayer[]{full.layer,compact.layer}) layer.paint(new KernelBounds(layer.width(),layer.height()));
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
    private static final class KernelBounds implements Painter {
        final float width,height;KernelBounds(float w,float h){width=w;height=h;}
        public void circle(float x,float y,float radius,int color) {
            check(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(radius),"kernel geometry is finite");
            check(x-radius>=1 && y-radius>=1 && x+radius<=width-1 && y+radius<=height-1,"kernel shells retain antialias gutters inside clipped layer bounds");
        }
        public void rect(float x,float y,float w,float h,int color){throw new AssertionError("unexpected kernel primitive");}
        public void line(float x,float y,float xx,float yy,float w,int color){throw new AssertionError("unexpected kernel primitive");}
        public void poly(float[] xy,int color){throw new AssertionError("unexpected kernel primitive");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("unexpected kernel asset");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new AssertionError("unexpected kernel asset");}
    }
    private static final class Capture implements Painter {
        int calls,atlas=-1,frame,layers,nativeCalls;long hash=17;float lastX,lastY,layerAlpha,layerScale;StaticLayer layer;boolean inLayer;
        public void layer(StaticLayer value,float x,float y,float scale,float alpha) {
            layers++;nativeCalls++;layer=value;layerAlpha=alpha;layerScale=scale;
            inLayer=true;value.draw(this,x,y,scale,alpha);inLayer=false;
        }
        private void add(float x) {hash=hash*31+Float.floatToIntBits(x);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {atlas=a;frame=f;calls++;add(x);add(y);add(alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha) {sprite(a,f,x,y,w,h,r,alpha);}
        public void rect(float x,float y,float w,float h,int color) {calls++;add(x);add(y);hash=hash*31+color;}
        public void circle(float x,float y,float r,int color) {calls++;if(!inLayer)nativeCalls++;lastX=x;lastY=y;add(x);add(y);add(r);hash=hash*31+color;}
        public void poly(float[] xy,int color) {calls++;for(float v:xy)add(v);hash=hash*31+color;}
        public void line(float x,float y,float xx,float yy,float w,int color) {calls++;add(x);add(y);add(xx);add(yy);hash=hash*31+color;}
    }
}
