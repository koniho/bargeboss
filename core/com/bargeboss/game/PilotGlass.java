package com.bargeboss.game;

/** Shared industrial UI components: containers, actions, choices and instrument controls. */
public final class PilotGlass {
    public static final int TEXT=0xfffff0ce,MUTED=0xffb9b5a8,AMBER=0xffffc52f,
            NAVY=0xff101c30,RED=0xffa92d37,FILL=0xe8101c30,
            LINE=0x805c6574,EDGE=0x80fff0ce,TRACK=0x485f6774;
    private PilotGlass(){}
    public static void plate(Painter p,float[] q,int fill,int edge,float stroke){
        p.poly(q,fill);
        for(int i=0;i<q.length;i+=2){int j=(i+2)%q.length;p.line(q[i],q[i+1],q[j],q[j+1],stroke,edge);}
    }
    static float[] panelShape(float x,float y,float w,float h){
        float cut=Math.min(18,Math.min(w,h)*.22f);
        return new float[]{x+cut,y,x+w,y,x+w,y+h-cut,x+w-cut,y+h,x,y+h,x,y+cut};
    }
    static float[] buttonShape(float x,float y,float w,float h,boolean primary){
        float cut=Math.min(14,Math.min(w,h)*.22f);
        return primary?new float[]{x+cut,y,x+w,y,x+w,y+h-cut-3,x+w-cut,y+h-3,x,y+h-3,x,y+cut}
                :new float[]{x,y,x+w-cut,y,x+w,y+cut,x+w,y+h,x,y+h};
    }
    static float[] replyShape(Layout.Box b,boolean left){
        float rear=left?b.x:b.x+b.w,tip=left?b.x+b.w:b.x,shoulder=tip+(left?-14:14);
        return new float[]{rear,b.y,shoulder,b.y,tip,b.y+b.h/2,shoulder,b.y+b.h,rear,b.y+b.h};
    }
    /** Quiet container with one structural red rail; it is not a button. */
    public static void panel(Painter p,float x,float y,float w,float h,int fill){
        float cut=Math.min(18,Math.min(w,h)*.22f);
        float[] q=panelShape(x,y,w,h);
        p.poly(q,fill);
        p.line(x+cut,y+.7f,x+w-1,y+.7f,.8f,LINE);
        if(h>12)p.line(x+1,y+cut+2,x+1,y+h-4,2,0xbba92d37);
        p.line(x+w-cut,y+h-.7f,x+w-.7f,y+h-cut,1.5f,0xbba92d37);
    }
    public static int buttonText(boolean primary){return primary?NAVY:TEXT;}
    /** Solid cut-corner action versus a quieter underline-style secondary action. */
    public static void button(Painter p,float x,float y,float w,float h,boolean primary){
        float cut=Math.min(14,Math.min(w,h)*.22f);
        if(primary){
            p.poly(new float[]{x+cut,y+3,x+w,y+3,x+w,y+h-cut,x+w-cut,y+h,x,y+h,x,y+cut},0xff775529);
            p.poly(buttonShape(x,y,w,h,true),AMBER);
            p.line(x+cut+1,y+1,x+w-2,y+1,1,0x99fff0ce);
        }else{
            p.poly(buttonShape(x,y,w,h,false),0xc8101c30);
            p.line(x+12,y+h-1,x+w-12,y+h-1,1,0x807e8590);
            p.line(x+1,y+8,x+1,y+h-8,2,0xbba92d37);
        }
    }
    /** Segmented selectors are states, not departure actions. */
    public static void segment(Painter p,float x,float y,float w,float h,boolean selected){
        p.rect(x,y,w,h,selected?0xff29364a:0xc00b1424);
        p.rect(x+1,y+h-3,w-2,3,selected?AMBER:0x404d5768);
        if(selected)tick(p,x+10,y+h*.5f,3,AMBER);
    }
    public static void toggle(Painter p,float x,float y,boolean on){
        float w=64,h=28;int color=on?AMBER:0xff475263;
        p.poly(new float[]{x+7,y,x+w-7,y,x+w,y+7,x+w,y+h-7,x+w-7,y+h,x+7,y+h,x,y+h-7,x,y+7},color);
        float thumb=on?x+w-15:x+15;p.circle(thumb,y+14,10,on?NAVY:TEXT);
    }
    public static void chevron(Painter p,float x,float y,int direction,int color){
        p.line(x-direction*4,y-5,x+direction*2,y,1.8f,color);
        p.line(x+direction*2,y,x-direction*4,y+5,1.8f,color);
    }
    public static void tick(Painter p,float x,float y,float size,int color){
        p.line(x-size,y,x,y+size,1.6f,color);p.line(x,y+size,x+size*2,y-size,1.6f,color);
    }
    /** Minimal mission/career ribbon leaves the scene visible around it. */
    public static void ribbon(Painter p,float x,float y,float w,float h){
        p.poly(new float[]{x,y,x+w,y,x+w-12,y+h,x,y+h},0xd00b1424);
        p.rect(x,y,3,h,AMBER);
    }
}
