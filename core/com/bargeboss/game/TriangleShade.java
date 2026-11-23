package com.bargeboss.game;

/** Bounded reference approximation for hosts without a native vertex-color primitive. */
public final class TriangleShade {
    private TriangleShade(){}
    public static int mix(int a,int b,float t){return sample(a,b,b,t,0);}
    private static int sample(int a,int b,int c,float u,float v){
        float aa=(a>>>24)*(1-u-v),ba=(b>>>24)*u,ca=(c>>>24)*v,total=aa+ba+ca;
        if(total<=0)return 0;int out=Math.round(total)<<24;
        for(int shift=0;shift<=16;shift+=8)out|=Math.round(((a>>shift&255)*aa+(b>>shift&255)*ba+(c>>shift&255)*ca)/total)<<shift;return out;
    }
    public static void reference(Painter p,float ax,float ay,int ac,float bx,float by,int bc,float cx,float cy,int cc){
        int n=4;float dx=bx-ax,dy=by-ay,ex=cx-ax,ey=cy-ay;
        for(int row=0;row<n;row++)for(int col=0;col<n-row;col++){
            float u=col/(float)n,v=row/(float)n,s=1f/n;
            p.poly(new float[]{ax+dx*u+ex*v,ay+dy*u+ey*v,ax+dx*(u+s)+ex*v,ay+dy*(u+s)+ey*v,ax+dx*u+ex*(v+s),ay+dy*u+ey*(v+s)},sample(ac,bc,cc,u+s/3,v+s/3));
            if(col+row<n-1)p.poly(new float[]{ax+dx*(u+s)+ex*v,ay+dy*(u+s)+ey*v,ax+dx*(u+s)+ex*(v+s),ay+dy*(u+s)+ey*(v+s),ax+dx*u+ex*(v+s),ay+dy*u+ey*(v+s)},sample(ac,bc,cc,u+2*s/3,v+2*s/3));
        }
    }
}
