package com.bargeboss.game;

/** Live dock fixtures use bounded geometry and nearby-light bins without changing illumination. */
public final class MarinaPedestalsTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int polygons,layers,hash=1;
        public void poly(float[] q,int color){polygons++;hash=31*hash+color;for(float v:q){check(Float.isFinite(v),"Rigid pedestal projection is finite");hash=31*hash+Float.floatToIntBits(v);}}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){layers++;check(alpha>=0 && alpha<=1,"Cached lamp kernel energy remains bounded");hash=31*hash+layer.key();hash=31*hash+Float.floatToIntBits(x+y+scale+alpha);}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run(){
        Game g=MontroseTest.game();g.layout=MontroseHarbor.overview(1600,1000);int count=MontroseHarbor.PEDESTALS.length;
        check(MarinaPedestals.count(g)==count,"Every authored utility fixture is rendered");
        check(Math.abs(MarinaPedestals.DECK_Z*ArticulatedModel.Z_SCALE-MontroseHarbor.DOCK_RISE)<.00001f,"Rigid pedestal base matches dock receiving elevation");
        for(int i=1;i<count;i++)check(MarinaPedestals.depth(g,i)>=MarinaPedestals.depth(g,i-1),"Pedestals are merged in unraised ground-depth order");
        for(int tick:new int[]{0,157,471}){g.visualTicks=tick;
            // A bounded grid spans every populated spatial bin and extends beyond the harbor.
            for(float y=-1400;y<=500;y+=113)for(float x=-400;x<=2100;x+=127){float[] fast=new float[4],brute=new float[4];MarinaPedestals.sample(g,fast,x,y);
                for(int id=0;id<count;id++){float[] f=MontroseHarbor.PEDESTALS[id];SurfaceLighting.add(brute,x,y,f[0],f[1],MarinaPedestals.LAMP_Z*ArticulatedModel.Z_SCALE,MarinaPedestals.LIGHT_RADIUS,.34f*SceneryEffects.intensity(tick,id*1.719f));}
                for(int d=0;d<4;d++)check(Math.abs(fast[d]-brute[d])<.000001f,"Spatial-bin sampling matches exhaustive lights at bin edges and outside marina");
            }
        }
        int ticks=g.ticks,visual=g.visualTicks;float x=g.tug.x,y=g.tug.y;Capture a=new Capture(),b=new Capture();
        MarinaPedestals.drawPools(g,a);for(int i=0;i<count;i++)MarinaPedestals.draw(g,a,i);
        MarinaPedestals.drawPools(g,b);for(int i=0;i<count;i++)MarinaPedestals.draw(g,b,i);
        check(a.layers==count*2 && a.polygons==count*7,"Overview submits one pool, one halo and seven faces per pedestal");
        check(a.hash==b.hash && a.layers==b.layers && a.polygons==b.polygons,"Repeated fixture drawing is deterministic");
        check(g.ticks==ticks && g.visualTicks==visual && g.tug.x==x && g.tug.y==y,"Lighting draws do not advance gameplay");
        g.layout=Layout.focused(960,600,10000,10000,2,480,300);Capture offscreen=new Capture();MarinaPedestals.drawPools(g,offscreen);for(int i=0;i<count;i++)MarinaPedestals.draw(g,offscreen,i);
        check(offscreen.layers==0 && offscreen.polygons==0,"Offscreen fixtures are culled before drawing geometry or kernels");
        check(MarinaPedestals.count(new Game())==0,"Other stages receive no marina fixtures");
        System.out.println(checks+" marina pedestal registration, depth, light sampling, culling and render-purity assertions passed");
    }
    public static void main(String[] args){run();}
}
