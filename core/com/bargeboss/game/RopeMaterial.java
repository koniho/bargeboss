package com.bargeboss.game;

/** Weathered hemp: shared geometry and continuous strand coordinates, native shading only. */
public final class RopeMaterial {
    public static final int COLOR=0xffaa9069;
    private RopeMaterial(){}
    public static void fallback(Painter p,float x,float y,float xx,float yy,float width,float from,float to,int color) {
        float dx=xx-x,dy=yy-y,len=Physics.length(dx,dy);if(len<.001f || width<=0)return;
        float nx=-dy/len,ny=dx/len;int alpha=color&0xff000000;
        p.line(x,y,xx,yy,width,alpha|0x574a38);
        p.line(x-nx*width*.12f,y-ny*width*.12f,xx-nx*width*.12f,yy-ny*width*.12f,width*.68f,color);
        p.line(x-nx*width*.26f,y-ny*width*.26f,xx-nx*width*.26f,yy-ny*width*.26f,width*.18f,alpha|0xd7bf92);
        int count=Math.min(8,Math.max(1,(int)StrictMath.ceil(Math.abs(to-from)*2)));
        for(int i=0;i<count;i++) {
            float t=(i+.5f)/count,wave=(float)StrictMath.sin((from+(to-from)*t)*Math.PI*2);
            float cx=x+dx*t,cy=y+dy*t;
            p.line(cx-nx*width*.23f,cy-ny*width*.23f,cx+nx*width*.23f+dx/len*width*.24f,cy+ny*width*.23f+dy/len*width*.24f,width*.16f,alpha|(wave>0?0x796448:0xbca47c));
        }
    }
}
