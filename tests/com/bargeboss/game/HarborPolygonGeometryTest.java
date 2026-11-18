package com.bargeboss.game;

/** Geography-independent coverage, winding and local-query regression for long polygon harbors. */
public final class HarborPolygonGeometryTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean inside(float[][] pieces,float x,float y){for(float[] p:pieces)if(PolygonWater.contains(p,x,y))return true;return false;}
    private static float[] triangle(float[] a,int at){return new float[]{a[at],a[at+1],a[at+2],a[at+3],a[at+4],a[at+5]};}
    public static void run(){
        // A long dogleg with two opposite-side slips and an interior quay corner.
        float[] coast={0,100,1800,100,1800,380,1300,380,1300,850,900,850,900,600,600,600,600,380,0,380};
        HarborPolygonGeometry geometry=new HarborPolygonGeometry(coast,PolygonWater.triangles(coast),2000,1000);
        for(int x=-60;x<1860;x+=137)for(int y=-50;y<930;y+=113){
            float[][] dry=geometry.dry(x,y,x+180,y+150);
            for(int a=0;a<11;a++)for(int b=0;b<9;b++){
                float px=x+3.37f+a*15.8f,py=y+2.71f+b*16.1f;
                check(geometry.contains(px,py)==PolygonWater.contains(coast,px,py),"bucket lookup equals exact polygon interior");
                check(inside(dry,px,py)!=PolygonWater.contains(coast,px,py),"local dry complement equals polygon, including concave banks");
            }
        }
        final java.util.ArrayList<float[]> painted=new java.util.ArrayList<float[]>();
        Painter ink=new Painter(){
            public void poly(float[] p,int color){painted.add(p);}
            public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
            public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
            public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){}
        };
        geometry.paintWater(ink,new float[]{500,300,1400,300,1400,700,500,700},0x80ffffff);
        for(float x=503.5f;x<1400;x+=17.3f)for(float y=302.7f;y<700;y+=18.1f)
            check(inside(painted.toArray(new float[painted.size()][]),x,y)==PolygonWater.contains(coast,x,y),"water polygons never paint across dogleg land");
        RiverCamera camera=new RiverCamera(200,100,1.7f);
        for(int i=0;i<4;i++){
            float depth=i==0?Float.NEGATIVE_INFINITY:camera.screenY(950,500)+(i-2)*150;
            float[] mask=geometry.shore(camera,750,350,1400,920,depth,false),union=geometry.shore(camera,750,350,1400,920,depth,true);
            for(int a=0;a<mask.length;a+=6){float[] ta=triangle(mask,a);check(PolygonWater.area(ta)>0,"inverse clip has consistent positive winding");
                for(int b=a+6;b<mask.length;b+=6){float[] intersection=PolygonWater.clip(ta,triangle(mask,b));check(Math.abs(PolygonWater.area(intersection))<.03f,"even-odd shore pieces do not overlap");}}
            for(float x=760.7f;x<1395;x+=19.3f)for(float y=360.2f;y<915;y+=21.1f){float sx=camera.screenX(x,y),sy=camera.screenY(x,y)-GroundArt.LAND_RISE*camera.scale;
                check(ShoreOcclusion.contains(mask,sx,sy)==ShoreOcclusion.contains(union,sx,sy),"union and disjoint native clip paths cover the same shore");}
        }
        for(int i=0;i<150;i++)geometry.dry(i*300,-700,i*300+100,-600);
        check(geometry.dryCacheSize()<=HarborPolygonGeometry.DRY_CACHE_LIMIT,"long-river travel cannot grow geometry cache without bound");
        check(geometry.waterCandidates(10,120,20,130)<PolygonWater.triangles(coast).length,"local water query excludes distant river reaches");
        System.out.println(checks+" polygon harbor clipping, winding, bucket and bounded-cache assertions passed");
    }
    public static void main(String[] args){run();}
}
