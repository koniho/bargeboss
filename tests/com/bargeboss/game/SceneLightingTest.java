package com.bargeboss.game;

/** One dusk grade per scenery surface; preserve opacity, lamps and non-scenery paths. */
public final class SceneLightingTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int color,calls;float[] points;
        public void poly(float[] p,int c){color=c;calls++;points=p;}
        public void rect(float x,float y,float w,float h,int c){color=c;}
        public void circle(float x,float y,float r,int c){color=c;}
        public void line(float x,float y,float xx,float yy,float w,int c){color=c;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha){}
    }
    public static void run(){
        for(int alpha=0;alpha<256;alpha++)for(int channel=0;channel<256;channel+=7)for(int material=-2;material<=5;material++){
            int source=(alpha<<24)|(channel<<16)|((255-channel)<<8)|(channel/2),graded=SceneLighting.surfaceColor(source,material);
            check((graded>>>24)==alpha,"dusk preserves every alpha value");
            if(material==SurfaceMaterial.EMISSIVE || alpha==0)check(graded==source,"emissive and transparent source pixels are untouched");
            else {check(((graded>>>16)&255)<=((source>>>16)&255) && ((graded>>>8)&255)<=((source>>>8)&255) && (graded&255)<=(source&255),"ambient material grade never adds emissive energy");}
        }
        int white=SceneLighting.sceneryColor(0xffffffff);check((white&255)>((white>>>8)&255) && ((white>>>8)&255)>((white>>>16)&255),"ambient white becomes muted cool dusk");
        int dock=SceneLighting.surfaceColor(0xff8b8170,SurfaceMaterial.WOOD);check(((dock>>>16)&255)<30 && ((dock>>>8)&255)<30 && (dock&255)<30,"wood docks are nearly dark");
        float[] xy={10,10,40,10,40,30,10,30},uv={0,0,30,0,30,20,0,20};Capture p=new Capture();
        Painter wrapped=new WaterBatchPainter(new OpacityPainter(ScreenView.scaled(new ViewportPainter(p,400,400),5,8,2),.5f),400,400);
        wrapped.surfacePolygon(xy,uv,SurfaceMaterial.CONCRETE,0xc0a0b0c0,.3f);
        check(p.color==SceneLighting.surfaceColor(0x60a0b0c0,SurfaceMaterial.CONCRETE),"wrapper chain grades once at terminal fallback after opacity");
        check(p.color!=SceneLighting.surfaceColor(SceneLighting.surfaceColor(0x60a0b0c0,SurfaceMaterial.CONCRETE),SurfaceMaterial.CONCRETE),"surface is not double darkened");
        check(p.points[0]==25 && p.points[1]==28,"grading does not change surface coordinates");
        p.poly(xy,0xffffc070);check(p.color==0xffffc070,"generic emissive/UI polygons bypass ambient grading");
        p.circle(0,0,2,0xff20ff70);check(p.color==0xff20ff70,"navigation lamp primitives retain source intensity");
        p.surfacePolygon(xy,uv,SurfaceMaterial.EMISSIVE,0xffffc070,0);check(p.color==0xffffc070,"reference emissive face retains source light");
        System.out.println(checks+" dusk surface opacity, material distinction and single-grade assertions passed");
    }
    public static void main(String[] args){run();}
}
