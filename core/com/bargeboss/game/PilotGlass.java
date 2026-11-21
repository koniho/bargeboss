package com.bargeboss.game;

/** Industrial nameplate UI, derived from the title palette; shared vector geometry. */
public final class PilotGlass {
    public static final int TEXT=0xfffff0ce,MUTED=0xffb9b5a8,AMBER=0xffffc52f,
            NAVY=0xff101c30,RED=0xffa92d37,FILL=0xe0101c30,
            LINE=0xbba92d37,EDGE=0x80fff0ce,TRACK=0x485f6774;
    private PilotGlass(){}
    /** Closed outline, also used by the radio's long directional chevrons. */
    public static void plate(Painter p,float[] q,int fill,int edge,float stroke){
        p.poly(q,fill);
        for(int i=0;i<q.length;i+=2){int j=(i+2)%q.length;p.line(q[i],q[i+1],q[j],q[j+1],stroke,edge);}
    }
    public static void panel(Painter p,float x,float y,float w,float h,int fill){
        frame(p,x,y,w,h,fill,LINE,1.4f);
    }
    private static void frame(Painter p,float x,float y,float w,float h,int fill,int edge,float stroke){
        float cut=Math.min(10,Math.min(w,h)*.18f);
        float[] q={x+cut,y,x+w-cut,y,x+w,y+cut,x+w,y+h-cut,x+w-cut,y+h,x+cut,y+h,x,y+h-cut,x,y+cut};
        plate(p,q,fill,edge,stroke);
        if(h>8){
            float mark=Math.min(32,w*.2f);
            p.line(x+cut+3,y+3,x+cut+3+mark,y+3,.8f,EDGE);
        }
    }
    public static int buttonText(boolean primary){return primary?NAVY:TEXT;}
    public static void button(Painter p,float x,float y,float w,float h,boolean primary){
        frame(p,x,y,w,h,primary?AMBER:0xe0101c30,primary?0xffad7730:LINE,primary?1.8f:1.4f);
    }
}
