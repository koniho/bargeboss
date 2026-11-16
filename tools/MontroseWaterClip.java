package com.bargeboss.game;

import java.util.ArrayList;

/** EXPERIMENT ONLY: slower/unproven in the recorded JVM comparison; not production code.
 * Bounded broad phase for the marina's exact water clipping. No geometry is simplified.
 * Original triangle/dock order is retained so alpha overlap and native edges do not change. */
final class MontroseWaterClip {
    private static final int CELL=128;
    private static final Index INDEX=new Index();
    private static final ThreadLocal<Scratch> SCRATCH=new ThreadLocal<Scratch>() {
        protected Scratch initialValue(){return new Scratch();}
    };
    private MontroseWaterClip(){}

    private static final class Scratch {
        final float[] box=new float[4];
        final int[] triangles=new int[MontroseHarbor.WATER_TRIANGLES.length],docks=new int[MontroseHarbor.DOCKS.length];
        final int[] triangleMarks=new int[triangles.length],dockMarks=new int[docks.length];
        int generation,triangleCount,dockCount;
    }
    private static int cell(float coordinate){return (int)StrictMath.floor(coordinate/CELL);}
    private static float[][] bounds(float[][] polygons){float[][] out=new float[polygons.length][];for(int i=0;i<out.length;i++)out[i]=PolygonWater.bounds(polygons[i]);return out;}
    private static final class Index {
        final int left,top,width,height;
        final float[][] triangleBounds=bounds(MontroseHarbor.WATER_TRIANGLES),dockBounds=bounds(MontroseHarbor.DOCKS);
        final int[][] triangles,docks;
        final int[] fullTriangle;
        Index(){
            float[] coast=PolygonWater.bounds(MontroseHarbor.COAST);
            left=cell(coast[0])-2;top=cell(coast[1])-2;width=cell(coast[2])+3-left;height=cell(coast[3])+3-top;
            triangles=build(triangleBounds);docks=build(dockBounds);fullTriangle=new int[width*height];
            for(int yy=0;yy<height;yy++)for(int xx=0;xx<width;xx++){
                int slot=yy*width+xx;fullTriangle[slot]=-1;
                float l=(xx+left)*CELL,t=(yy+top)*CELL,r=l+CELL,b=t+CELL;
                for(int id:triangles[slot])if(strictlyInsideTriangle(MontroseHarbor.WATER_TRIANGLES[id],l,t,r,b)){fullTriangle[slot]=id;break;}
            }
        }
        private int[][] build(float[][] bounds){
            int[][] bins=new int[width*height][];int[] sizes=new int[bins.length];
            for(float[] b:bounds)for(int y=Math.max(0,cell(b[1])-top);y<=Math.min(height-1,cell(b[3])-top);y++)
                for(int x=Math.max(0,cell(b[0])-left);x<=Math.min(width-1,cell(b[2])-left);x++)sizes[y*width+x]++;
            for(int i=0;i<bins.length;i++)bins[i]=new int[sizes[i]];
            java.util.Arrays.fill(sizes,0);
            for(int id=0;id<bounds.length;id++){float[] b=bounds[id];
                for(int y=Math.max(0,cell(b[1])-top);y<=Math.min(height-1,cell(b[3])-top);y++)
                    for(int x=Math.max(0,cell(b[0])-left);x<=Math.min(width-1,cell(b[2])-left);x++){int slot=y*width+x;bins[slot][sizes[slot]++]=id;}}
            return bins;
        }
    }
    /** A strict interior cell cannot touch a coast OR an internal triangle edge.
     * Thus its direct polygon has exactly the original clip's coordinates and alpha coverage. */
    private static boolean strictlyInsideTriangle(float[] p,float l,float t,float r,float b){
        float sign=PolygonWater.area(p)>0?1:-1;
        for(int i=0;i<6;i+=2){int j=(i+2)%6;float dx=p[j]-p[i],dy=p[j+1]-p[i+1];
            float margin=.02f*(Math.abs(dx)+Math.abs(dy));
            // The signed edge function is linear: this box corner has its minimum.
            float x=dy*sign>0?r:l,y=dx*sign>0?t:b;
            if(PolygonWater.cross(p[i],p[i+1],p[j],p[j+1],x,y)*sign<=margin)return false;
        }
        return true;
    }
    private static void add(int[] source,int[] target,int[] marks,Scratch s,boolean triangle){
        int count=triangle?s.triangleCount:s.dockCount;
        for(int id:source)if(marks[id]!=s.generation){marks[id]=s.generation;int j=count++;
            while(j>0 && target[j-1]>id){target[j]=target[j-1];j--;}target[j]=id;}
        if(triangle)s.triangleCount=count;else s.dockCount=count;
    }
    /** Returns one containing bin, or -1 for a multi-bin/outside query. */
    private static int query(Scratch s){
        if(++s.generation==0){java.util.Arrays.fill(s.triangleMarks,0);java.util.Arrays.fill(s.dockMarks,0);s.generation=1;}
        s.triangleCount=s.dockCount=0;
        int l=cell(s.box[0])-INDEX.left,t=cell(s.box[1])-INDEX.top,r=cell(s.box[2])-INDEX.left,b=cell(s.box[3])-INDEX.top;
        if(l<0 || t<0 || r>=INDEX.width || b>=INDEX.height || (long)(r-l+1)*(b-t+1)>64){
            for(int i=0;i<s.triangles.length;i++)s.triangles[s.triangleCount++]=i;
            for(int i=0;i<s.docks.length;i++)s.docks[s.dockCount++]=i;
            return -1;
        }
        if(l==r && t==b){int slot=t*INDEX.width+l;s.triangleCount=INDEX.triangles[slot].length;s.dockCount=INDEX.docks[slot].length;
            System.arraycopy(INDEX.triangles[slot],0,s.triangles,0,s.triangleCount);System.arraycopy(INDEX.docks[slot],0,s.docks,0,s.dockCount);return slot;}
        for(int y=t;y<=b;y++)for(int x=l;x<=r;x++){int slot=y*INDEX.width+x;
            add(INDEX.triangles[slot],s.triangles,s.triangleMarks,s,true);add(INDEX.docks[slot],s.docks,s.dockMarks,s,false);}
        return l==r && t==b?t*INDEX.width+l:-1;
    }
    static void paint(Painter painter,float[] points,int color){
        if(points.length<6 || (color>>>24)==0)return;
        Scratch s=SCRATCH.get();float[] box=s.box;box[0]=box[1]=Float.MAX_VALUE;box[2]=box[3]=-Float.MAX_VALUE;
        for(int i=0;i<points.length;i+=2){box[0]=Math.min(box[0],points[i]);box[1]=Math.min(box[1],points[i+1]);box[2]=Math.max(box[2],points[i]);box[3]=Math.max(box[3],points[i+1]);}
        int single=query(s),holes=0;
        // Keep only actual bounds overlaps once, not once for every candidate triangle.
        for(int n=0;n<s.dockCount;n++){int id=s.docks[n];if(PolygonWater.overlaps(box,INDEX.dockBounds[id]))s.docks[holes++]=id;}
        if(single>=0 && INDEX.fullTriangle[single]>=0 && holes==0){painter.poly(points,color);return;}
        for(int n=0;n<s.triangleCount;n++){int id=s.triangles[n];if(!PolygonWater.overlaps(box,INDEX.triangleBounds[id]))continue;
            if(holes==0 && strictlyInsideTriangle(MontroseHarbor.WATER_TRIANGLES[id],box[0],box[1],box[2],box[3])){painter.poly(points,color);return;}
            float[] q=PolygonWater.clip(points,MontroseHarbor.WATER_TRIANGLES[id]);if(q.length<6)continue;
            if(holes==0){painter.poly(q,color);continue;}
            ArrayList<float[]> pieces=new ArrayList<float[]>();pieces.add(q);
            for(int h=0;h<holes;h++){
                ArrayList<float[]> next=new ArrayList<float[]>();for(float[] piece:pieces)PolygonWater.subtract(piece,MontroseHarbor.DOCKS[s.docks[h]],next);pieces=next;
            }
            for(float[] piece:pieces)painter.poly(piece,color);
        }
    }
}
