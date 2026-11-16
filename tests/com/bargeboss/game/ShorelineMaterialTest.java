package com.bargeboss.game;

/** Shoreline motion stays continuous, planar UVs survive wrappers, reference coverage stays clipped. */
public final class ShorelineMaterialTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static class Capture implements Painter {
        float[] xy,uv;float phase,energy;int kind,color,calls,polys;boolean ordered;long hash=1;
        public void shoreline(float[] p,float[] u,float t,float e,int k,int c){xy=p.clone();uv=u;phase=t;energy=e;kind=k;color=c;calls++;ordered=polys>0;}
        public void poly(float[] p,int c){polys++;for(float v:p){check(Float.isFinite(v),"finite fallback geometry");hash=31*hash+Float.floatToIntBits(v);}color=c;xy=p.clone();}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha){}
    }
    public static void run(){
        float[] xy={20,30,100,50,90,74,10,54},uv={400,0,440,0,440,12,400,12};
        Capture p=new Capture();Painter wrapped=new OpacityPainter(ScreenView.scaled(p,7,9,2),.5f);
        wrapped.shoreline(xy,uv,12.75f,.7f,0,0x80445566);
        check(p.calls==1 && p.uv==uv,"wrapper preserves stable world UV array");
        check(p.color==0x40445566 && p.phase==12.75f && p.energy==.7f && p.kind==0,"opacity changes only caller alpha");
        for(int i=0;i<xy.length;i+=2){check(p.xy[i]==7+xy[i]*2,"screen X transforms");check(p.xy[i+1]==9+xy[i+1]*2,"screen Y transforms");}
        RiverCamera camera=new RiverCamera(10,20,1.3f);new RiverPainter(p,camera).shoreline(xy,uv,3,.2f,1,0xff123456);
        for(int i=0;i<xy.length;i+=2){check(p.xy[i]==camera.screenX(xy[i],xy[i+1]),"river X projection");check(p.xy[i+1]==camera.screenY(xy[i],xy[i+1]),"river Y projection");}
        int before=p.calls;new ViewportPainter(p,5,5).shoreline(xy,uv,3,.2f,1,0xff123456);check(p.calls==before,"viewport rejects distant band");
        WaterBatchPainter batch=new WaterBatchPainter(p,200,200);batch.poly(xy,0xff222222);batch.shoreline(xy,uv,3,.2f,0,0xffeeeeee);check(p.ordered,"water batches flush before shoreline coverage");
        for(int u=-300;u<=300;u+=7)for(int t=0;t<1200;t+=3){float phase=t/60f,c=ShorelineMaterial.crest(u,phase,0),next=ShorelineMaterial.crest(u,phase+1/60f,0);
            check(Math.abs(next-c)<.018f,"crest advances smoothly each fixed tick");check(Math.abs(ShorelineMaterial.crest(u,phase,1)-c-1.1f)<.00001f,"wake energy raises crest continuously");}
        float[] triangle={0,0,48,0,0,12};
        for(int kind=0;kind<2;kind++){
            Capture a=new Capture(){public void poly(float[] q,int color){super.poly(q,color);for(int i=0;i<q.length;i+=2)check(q[i]>=-.001f && q[i+1]>=-.001f && q[i]/48+q[i+1]/12<=1.001f,"reference ribbon remains inside clipped water polygon");}};
            ShorelineMaterial.fallback(a,triangle,triangle,2.3f,.5f,kind,0x80445566);
            check(a.polys>0 && a.polys<=8,"reference path has bounded polygon count");check((a.color>>>24)<=128 && (a.color&0xffffff)==0x445566,"fallback preserves tint and opacity bound");
            Capture b=new Capture();ShorelineMaterial.fallback(b,triangle,triangle,2.3f,.5f,kind,0x80445566);check(a.hash==b.hash,"reference motion is deterministic");
        }
        System.out.println(checks+" shoreline continuity, clipped reference geometry and wrapper assertions passed");
    }
    public static void main(String[] args){run();}
}
