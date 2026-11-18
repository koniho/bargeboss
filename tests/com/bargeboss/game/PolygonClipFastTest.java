package com.bargeboss.game;
import java.util.Arrays;
public final class PolygonClipFastTest {
    private static float[] original(float[] p,float ax,float ay,float bx,float by,boolean positive){
        if(p.length<6)return new float[0];float[] out=new float[p.length*2+4];int n=0;
        float x=p[p.length-2],y=p[p.length-1],d=PolygonWater.cross(ax,ay,bx,by,x,y)*(positive?1:-1);boolean inside=d>=0;
        for(int i=0;i<p.length;i+=2){float xx=p[i],yy=p[i+1],dd=PolygonWater.cross(ax,ay,bx,by,xx,yy)*(positive?1:-1);boolean next=dd>=0;
            if(inside!=next){float t=d/(d-dd);out[n++]=x+(xx-x)*t;out[n++]=y+(yy-y)*t;}
            if(next){out[n++]=xx;out[n++]=yy;}x=xx;y=yy;d=dd;inside=next;}
        return n<6?new float[0]:Arrays.copyOf(out,n);
    }
    public static void run(){int checks=0;
        for(int x=-10;x<=10;x++)for(int y=-10;y<=10;y++)for(int side=0;side<2;side++){
            float[] p={x,y,x+4,y+1,x+3,y+5,x-1,y+3};
            float[] a=original(p,0,-12,3,12,side==0),b=PolygonWater.edge(p,0,-12,3,12,side==0);
            if(!Arrays.equals(a,b))throw new AssertionError("clip vertex mismatch");
            if(b==p)throw new AssertionError("public edge aliases source geometry");checks++;
        }
        float[] source={0,0,2,0,2,2,0,2};float[] clipped=PolygonWater.rectangle(source,-10,-10,10,10);
        clipped[0]=99;if(source[0]!=0)throw new AssertionError("rectangle projection would mutate shared geometry");
        System.out.println(checks+" exact clipping comparisons and source ownership checks passed");
    }
}
