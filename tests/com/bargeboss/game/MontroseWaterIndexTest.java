package com.bargeboss.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Spatial candidates must preserve the original ordered geometry, including exact edge cases. */
public final class MontroseWaterIndexTest {
    private static int checks,pointQueries,paintQueries,maxPieces;
    private static final float[][] WATER=MontroseHarbor.WATER_TRIANGLES,DOCKS=MontroseHarbor.DOCKS;
    private static final float[][] WATER_BOXES=boxes(WATER),DOCK_BOXES=boxes(DOCKS);
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static float[][] boxes(float[][] shapes){
        float[][] out=new float[shapes.length][];
        for(int i=0;i<out.length;i++)out[i]=PolygonWater.bounds(shapes[i]);
        return out;
    }
    /** Independent all-edge and all-dock scan; never uses the new candidate helpers. */
    private static boolean contains(float x,float y){
        if(!PolygonWater.contains(MontroseHarbor.WATER_POLYGON,x,y))return false;
        for(int i=0;i<DOCKS.length;i++){
            float[] b=DOCK_BOXES[i];
            if(x>=b[0] && x<=b[2] && y>=b[1] && y<=b[3] && PolygonWater.contains(DOCKS[i],x,y))return false;
        }
        return true;
    }
    private static void point(float x,float y){
        pointQueries++;
        check(MontroseHarbor.openWater(x,y)==PolygonWater.contains(MontroseHarbor.WATER_POLYGON,x,y),"Indexed coast ray matches all edges at "+x+","+y);
        check(MontroseHarbor.contains(x,y)==contains(x,y),"Indexed dock exclusion matches all footprints at "+x+","+y);
    }
    private static final class Capture implements Painter {
        final ArrayList<float[]> polygons=new ArrayList<float[]>();
        final ArrayList<Integer> colors=new ArrayList<Integer>();
        public void poly(float[] p,int c){polygons.add(p.clone());colors.add(c);}
        public void rect(float x,float y,float w,float h,int c){throw new AssertionError("Unexpected rectangle");}
        public void circle(float x,float y,float r,int c){throw new AssertionError("Unexpected circle");}
        public void line(float x,float y,float xx,float yy,float w,int c){throw new AssertionError("Unexpected line");}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("Unexpected sprite");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){throw new AssertionError("Unexpected sprite blend");}
    }
    /** The pre-index paint algorithm, scanning all IDs in source order. */
    private static void paint(Capture p,float[] points,int color){
        float[] box=PolygonWater.bounds(points);
        for(int i=0;i<WATER.length;i++)if(PolygonWater.overlaps(box,WATER_BOXES[i])){
            float[] q=PolygonWater.clip(points,WATER[i]);if(q.length<6)continue;
            ArrayList<float[]> pieces=new ArrayList<float[]>();pieces.add(q);
            for(int h=0;h<DOCKS.length;h++)if(PolygonWater.overlaps(box,DOCK_BOXES[h])){
                ArrayList<float[]> next=new ArrayList<float[]>();
                for(float[] piece:pieces)PolygonWater.subtract(piece,DOCKS[h],next);
                pieces=next;
            }
            for(float[] piece:pieces)p.poly(piece,color);
        }
    }
    private static void patch(float[] points,int color){
        paintQueries++;float[] before=points.clone();Capture expected=new Capture(),actual=new Capture();
        paint(expected,points,color);MontroseHarbor.paintWater(actual,points,color);
        check(Arrays.equals(points,before),"Water painting leaves borrowed geometry unchanged");
        check(actual.polygons.size()==expected.polygons.size(),"Indexed clip preserves bounded piece count for query "+paintQueries);
        maxPieces=Math.max(maxPieces,actual.polygons.size());
        for(int i=0;i<expected.polygons.size();i++){
            float[] a=actual.polygons.get(i),b=expected.polygons.get(i);
            check(actual.colors.get(i).intValue()==color,"Every ordered clipped piece retains caller alpha/color");
            check(a.length==b.length,"Clipped polygon topology/order unchanged for query "+paintQueries+" piece "+i);
            for(int j=0;j<b.length;j++)check(Float.floatToRawIntBits(a[j])==Float.floatToRawIntBits(b[j]),
                    "Exact ordered clipping float changed for query "+paintQueries+" piece "+i+" coordinate "+j);
        }
    }
    private static float[] rect(float x,float y,float w,float h){return new float[]{x,y,x+w,y,x+w,y+h,x,y+h};}
    private static void edgeSamples(float[] p,boolean paintEdges){
        for(int i=0;i<p.length;i+=2){
            int j=(i+2)%p.length;float x=p[i],y=p[i+1],mx=(x+p[j])*.5f,my=(y+p[j+1])*.5f;
            point(x,y);point(Math.nextDown(x),y);point(Math.nextUp(x),y);point(x,Math.nextDown(y));point(x,Math.nextUp(y));
            point(mx,my);point(Math.nextDown(mx),Math.nextUp(my));point(Math.nextUp(mx),Math.nextDown(my));
            if(paintEdges && i%4==0)patch(rect(mx-8,my-8,16,16),0x71429fa3);
        }
    }
    public static void run(){
        checks=pointQueries=paintQueries=maxPieces=0;
        // Play bounds, partial final grid cells and queries immediately outside the index.
        for(float y=-1728;y<=1270;y+=63.25f)for(float x=-1028;x<=2940;x+=63.25f)point(x,y);
        for(int row=-1;row<=23;row++)for(int col=-1;col<=30;col++){
            float x=-900+128*col,y=-1600+128*row;
            point(x,y);point(Math.nextDown(x),Math.nextDown(y));point(Math.nextUp(x),Math.nextUp(y));
            if(row%4==0 && col%4==0)for(float size:new float[]{127,128,129})patch(rect(x-size*.5f,y-size*.5f,size,size),0x804499aa);
        }
        // Every dock endpoint/midpoint and coast vertex, including exact ray Y bands.
        edgeSamples(MontroseHarbor.WATER_POLYGON,true);
        for(float[] dock:DOCKS)edgeSamples(dock,true);
        float[] coastBox=PolygonWater.bounds(MontroseHarbor.WATER_POLYGON);
        for(float y=coastBox[1];y<=coastBox[3]+128;y+=128)for(float x:new float[]{-16000,-900,0,599,1500,2812,16000}){
            point(x,y);point(x,Math.nextDown(y));point(x,Math.nextUp(y));
        }
        Random random=new Random(0x4d6f6e74726f7365L);
        for(int i=0;i<8000;i++){
            float x=-19000+random.nextFloat()*38000,y=-18000+random.nextFloat()*35000;point(x,y);
        }
        for(int i=0;i<400;i++){
            float x=-1100+random.nextFloat()*4100,y=-1800+random.nextFloat()*3200;
            float w=1+random.nextFloat()*160,h=1+random.nextFloat()*160;
            // Convex slanted patches exercise non-axis-aligned foam and arbitrary winding.
            float[] q={x-w*.5f,y-h*.3f,x+w*.3f,y-h*.5f,x+w*.5f,y+h*.3f,x-w*.3f,y+h*.5f};
            if((i&1)!=0){float[] forward=q.clone();for(int j=0;j<4;j++){q[j*2]=forward[(3-j)*2];q[j*2+1]=forward[(3-j)*2+1];}}
            patch(q,i%3==0?0:0xb03a9cdf);
        }
        // Broad fallback queries straddle many cells or lie wholly outside the play index.
        for(float[] q:new float[][]{
                rect(-900,-1600,3600,2700),rect(-1800,-2500,5400,4500),
                rect(-17000,10000,4000,3000),rect(6000,6000,3000,3000),
                rect(-900,-1600,128,129),rect(2812,1216,128,128),
                rect(300,-700,0,20),new float[0]})patch(q,0xff337799);
        check(pointQueries<30000 && paintQueries<1500,"Validation queries stay explicitly bounded");
        System.out.println(checks+" exact Montrose water-index assertions; "+pointQueries+" points, "+paintQueries+" patches, maximum "+maxPieces+" pieces");
    }
    public static void main(String[] args){run();}
}
