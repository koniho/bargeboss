package com.bargeboss.game;

/** Lightweight instrument-glass UI; shared geometry, no native blur or new textures. */
public final class PilotGlass {
    public static final int TEXT=0xffd9e7e2,MUTED=0xff9bb6b8,AMBER=0xffe7bc73,
            FILL=0xcf091b25,LINE=0x7291b9bc,EDGE=0xa8bdd8d4,TRACK=0x485d7d86;
    private PilotGlass(){}
    public static void panel(Painter p,float x,float y,float w,float h,int fill){
        float cut=Math.min(7,Math.min(w,h)*.12f);
        float[] q={x+cut,y,x+w-cut,y,x+w,y+cut,x+w,y+h-cut,x+w-cut,y+h,x+cut,y+h,x,y+h-cut,x,y+cut};
        p.poly(q,fill);
        for(int i=0;i<q.length;i+=2){int j=(i+2)%q.length;p.line(q[i],q[i+1],q[j],q[j+1],.8f,LINE);}
        float mark=Math.min(18,w*.16f);
        p.line(x+cut+3,y+3,x+cut+3+mark,y+3,.8f,EDGE);
        p.line(x+w-cut-3-mark,y+h-3,x+w-cut-3,y+h-3,.8f,EDGE);
    }
    public static void button(Painter p,float x,float y,float w,float h,boolean primary){
        panel(p,x,y,w,h,primary?0xc1183038:0xa30b202a);
        if(primary){
            p.line(x+7,y+10,x+7,y+h-10,1.5f,AMBER);
            p.line(x+17,y+h*.5f-4,x+22,y+h*.5f,1,AMBER);
            p.line(x+22,y+h*.5f,x+17,y+h*.5f+4,1,AMBER);
        }
    }
}
