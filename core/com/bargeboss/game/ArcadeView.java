package com.bargeboss.game;

/** Readable foam and short-lived payoff effects; rendering never changes physics. */
public final class ArcadeView {
    private ArcadeView() {}
    public static void water(Game g,Painter p) {
        for(int row=0;row<26;row++) {
            float y=218+row*23;
            for(int col=0;col<7;col++) {
                float x=116+col*54+(row%2)*6;
                float phase=g.ticks*.045f+row*.8f+col;
                float lastY=y+3*(float)StrictMath.sin(phase);
                for(int i=1;i<=4;i++) {
                    float yy=y+3*(float)StrictMath.sin(phase+i*.8);
                    p.line(x+(i-1)*7,lastY,x+i*7,yy,1.5f,0x5559d7e2); lastY=yy;
                }
            }
        }
    }
    public static void wake(Painter p,Body b,int ticks) {
        if(b.speed()<5 || b.lineAttached) return;
        float power=Math.min(1,b.speed()/65),c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float stern=-(b.half+b.radius);
        for(int side=-1;side<=1;side+=2) {
            float lastX=b.x+c*stern-s*side*9,lastY=b.y+s*stern+c*side*9;
            for(int i=1;i<=7;i++) {
                float back=stern-i*(5+power*3),width=side*(9+i*(1+power*1.4f));
                width+=2*(float)StrictMath.sin(ticks*.19+i);
                float x=b.x+c*back-s*width,y=b.y+s*back+c*width;
                p.line(lastX,lastY,x,y,Math.max(1,4-i*.4f)*power,0xaaaffff3);
                if(i%2==0) p.circle(x,y,1.5f+power,0xaaffffed);
                lastX=x; lastY=y;
            }
        }
        float bow=b.half+b.radius+3;
        p.line(b.x+c*bow-s*8,b.y+s*bow+c*8,b.x+c*(bow+4),b.y+s*(bow+4),2.5f,0xddedffef);
        p.line(b.x+c*bow+s*8,b.y+s*bow-c*8,b.x+c*(bow+4),b.y+s*(bow+4),2.5f,0xddedffef);
    }
    public static void effects(Game g,Painter p) {
        if(g.impactTicks>0) {
            float t=1-g.impactTicks/45f;
            ControlView.ring(p,g.impactX,g.impactY,12+48*t,1,0x99bcfff3,3*(1-t)+1);
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4; float r=15+30*t;
                p.circle(g.impactX+(float)StrictMath.cos(a)*r,g.impactY+(float)StrictMath.sin(a)*r,2.5f*(1-t)+1,0xccf6ffe7);
            }
        }
        if(g.celebrationTicks>0) {
            float t=1-g.celebrationTicks/90f;
            if(t<.6f) ControlView.ring(p,g.celebrationX,g.celebrationY,20+100*t,1,0x99a5ffb5,4*(1-t));
            for(int i=0;i<16;i++) {
                double a=i*Math.PI*2/16; float r=20+90*t;
                float x=g.celebrationX+(float)StrictMath.cos(a)*r,y=g.celebrationY+(float)StrictMath.sin(a)*r+30*t*t;
                int color=i%2==0?0xffffd259:0xff9affd2;
                p.line(x-3,y-3,x+3,y+3,3,color); p.line(x-3,y+3,x+3,y-3,3,color);
            }
            float y=g.celebrationY-20-35*t;
            Lettering.center(p,"+"+g.celebrationPoints,g.celebrationX+2,y+2,24,0xff102b40);
            Lettering.center(p,"+"+g.celebrationPoints,g.celebrationX,y,24,0xffffe58c);
            Lettering.center(p,"UNLOADED!",g.celebrationX,y+33,12,0xffd7ffe7);
        }
    }
}
