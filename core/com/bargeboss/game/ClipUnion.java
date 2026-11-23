package com.bargeboss.game;

/** Exact fallback for hosts that need disjoint clip triangles. No gameplay state. */
public final class ClipUnion {
    private ClipUnion(){}
    public static float[] disjoint(float[] triangles){
        java.util.ArrayList<float[]> pieces=new java.util.ArrayList<float[]>(),blockers=new java.util.ArrayList<float[]>(),boxes=new java.util.ArrayList<float[]>();
        for(int at=0;at<triangles.length;at+=6){
            float[] source={triangles[at],triangles[at+1],triangles[at+2],triangles[at+3],triangles[at+4],triangles[at+5]};
            if(!solid(source))continue;float[] box=PolygonWater.bounds(source);
            java.util.ArrayList<float[]> pending=new java.util.ArrayList<float[]>();pending.add(source);
            for(int i=0;i<blockers.size() && !pending.isEmpty();i++){
                if(!PolygonWater.overlaps(box,boxes.get(i)))continue;
                java.util.ArrayList<float[]> next=new java.util.ArrayList<float[]>();
                for(float[] candidate:pending){
                    if(!PolygonWater.overlaps(PolygonWater.bounds(candidate),boxes.get(i))){next.add(candidate);continue;}
                    PolygonWater.subtract(candidate,blockers.get(i),next);
                }
                for(int j=next.size()-1;j>=0;j--)if(!solid(next.get(j)))next.remove(j);
                pending=next;
            }
            for(float[] candidate:pending)if(solid(candidate))pieces.add(candidate);
            blockers.add(source);boxes.add(box);
        }
        int count=0;for(float[] piece:pieces)count+=(piece.length/2-2)*6;float[] result=new float[count];int at=0;
        for(float[] p:pieces)for(int i=2;i<p.length-2;i+=2){result[at++]=p[0];result[at++]=p[1];result[at++]=p[i];result[at++]=p[i+1];result[at++]=p[i+2];result[at++]=p[i+3];}
        return result;
    }
    private static boolean solid(float[] p){
        if(p.length<6)return false;double area=0;
        for(int i=2;i<p.length-2;i+=2)area+=(double)(p[i]-p[0])*(p[i+3]-p[1])-(double)(p[i+2]-p[0])*(p[i+1]-p[1]);
        return Math.abs(area)>.0001;
    }
}
