package com.bargeboss.game;

/** Retained static shore geometry must agree with direct masks after camera changes. */
public final class WaterClipTest {
    private static final class QuadCapture implements Painter {
        int quads,polygons;float[] points;int color;
        public void convexQuad(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy,int c){quads++;points=new float[]{ax,ay,bx,by,cx,cy,dx,dy};color=c;}
        public void poly(float[] p,int c){polygons++;points=p;color=c;}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){}
    }
    private static void directPath(Game g){
        QuadCapture direct=new QuadCapture(),reference=new QuadCapture();
        RiverCamera c=Layout.focused(960,600,100,100,1,480,300).camera;
        Painter fast=new RiverPainter(new SceneryOcclusion(new ViewportPainter(new OpacityPainter(direct,.5f),960,600)),c);
        Painter slow=new RiverPainter(new SceneryOcclusion(new ViewportPainter(new OpacityPainter(reference,.5f),960,600)),c);
        g.settings.directWaterQuads=true;new WaterEffectsPainter(g,fast).preclipped().line(100,100,110,110,2,0x8083b3b4);
        g.settings.directWaterQuads=false;new WaterEffectsPainter(g,slow).preclipped().line(100,100,110,110,2,0x8083b3b4);
        g.settings.directWaterQuads=true;
        if(direct.quads!=1 || direct.polygons!=0 || reference.polygons!=1)throw new AssertionError("Scene wrappers must preserve the native quad fast path");
        if(direct.color!=reference.color || !java.util.Arrays.equals(direct.points,reference.points))throw new AssertionError("Direct path changes projected vertices or opacity");
    }
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        directPath(g);
        WaterClip cache=new WaterClip();int checks=0;
        float x=MontroseHarbor.mapX(350),y=MontroseHarbor.mapY(1250);
        for(boolean union:new boolean[]{false,true})for(float zoom:new float[]{.65f,1.5f,2.5f}){
            RiverCamera c=Layout.focused(960,600,x+zoom*11,y-zoom*9,zoom,480,300).camera;
            float l=x-140,t=y-140,r=x+140,b=y+140;
            float[] saved=cache.get(g,c,l,t,r,b,union);
            float[] direct=ShoreOcclusion.region(g,c,l-96,t-96,r+96,b+96,Float.NEGATIVE_INFINITY,union);
            for(int sy=80;sy<520;sy+=13)for(int sx=80;sx<880;sx+=13){
                if(ShoreOcclusion.contains(saved,sx+.37f,sy+.23f)!=ShoreOcclusion.contains(direct,sx+.37f,sy+.23f))throw new AssertionError("retained shoreline changes coverage after camera transform");checks++;
            }
            if(cache.get(g,c,l,t,r,b,union)!=saved)throw new AssertionError("stationary water mask should reuse its projection");
        }
        System.out.println(checks+" retained water-mask coverage assertions passed");
    }
}
