package com.bargeboss.game;

/** Water, vessel churn and arcade rewards. Deterministic drawing only; no wake forces. */
public final class ArcadeView {
    private ArcadeView() {}
    public static void water(Game g,Painter p) {
        HarborAtmosphere.reflections(g,p);
        g.water.draw(g,p);
    }
    public static void wake(Game g,Painter p,Body b,int ticks) {
        wake(p instanceof RiverPainter?new WaterEffectsPainter(g,p):p,b,ticks);
    }
    public static void wake(Painter p,Body b,int ticks) {
        if(b.lineAttached) return;
        float speed=b.speed();
        if(speed<4) {
            // Small curved hull ripples stay subtle against the darker open water.
            float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
            float pulse=.5f+.5f*(float)StrictMath.sin(ticks*.04+b.id);
            for(int side=-1;side<=1;side+=2) {
                float lastX=0,lastY=0;
                for(int i=0;i<=5;i++) {
                    float xx=(i/5f-.5f)*(b.half*2+8),yy=side*(b.radius+2+pulse*2+(float)StrictMath.sin(i*StrictMath.PI/5)*1.5f);
                    float x=b.x+c*xx-s*yy,y=b.y+s*xx+c*yy;
                    if(i>0) p.line(lastX,lastY,x,y,.8f,0x356eaaa9);
                    lastX=x;lastY=y;
                }
            }
            return;
        }
        float travel=(float)StrictMath.atan2(b.vy,b.vx);
        float power=Math.min(1,speed/65),c=(float)StrictMath.cos(travel),s=(float)StrictMath.sin(travel);
        float kick=b.floatingInitialized?b.wakeKickTicks/18f:(float)StrictMath.pow(Math.max(0,-StrictMath.sin(ticks*.048+b.x*.017+b.y*.009)),10);
        float stern=-(b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(travel-b.angle)));
        // Continuous wash stays visible between the stronger trough-triggered bursts.
        float wash=.42f+.58f*kick;
        for(int side=-1;side<=1;side+=2) {
            float lastX=b.x+c*stern-s*side*8,lastY=b.y+s*stern+c*side*8;
            for(int i=1;i<=12;i++) {
                float age=(i+(ticks%12)/12f)/12f;
                float back=stern-age*(15+power*24);
                float spread=side*(8+age*(5+power*11));
                spread+=(float)StrictMath.sin(ticks*.16+i*1.6)*1.2f;
                float x=b.x+c*back-s*spread,y=b.y+s*back+c*spread;
                int alpha=(int)((180*(1-age)+30)*wash);
                p.line(lastX,lastY,x,y,(3.5f+power*3)*(1-age*.5f),((int)(80*wash)<<24)|0x3c9caa);
                p.line(lastX,lastY,x,y,1.4f+power*1.3f,(alpha<<24)|0xc5f6ee);
                if(i%2==0) {
                    float r=(1.1f+power*.7f)*(1-age*.5f);
                    p.circle(x,y,r,((int)(alpha*.85f)<<24)|0xf0fff7);
                    p.line(x-s*side*2,y+c*side*2,x-c*2-s*side*4,y-s*2+c*side*4,.8f,(alpha/2<<24)|0xa9e5df);
                }
                lastX=x;lastY=y;
            }
        }
        if(b.berth<0) for(int i=0;i<9;i++) {
            float t=((ticks+i*7)%63)/63f;
            float back=stern-4-t*(32+power*26);
            float jitter=(float)StrictMath.sin(t*15+i)*(2+t*4);
            float x=b.x+c*back-s*jitter,y=b.y+s*back+c*jitter;
            float radius=(1.2f+power*1.4f)*(1-t*.55f);
            int alpha=(int)((155+power*70)*(1-t)*wash);
            p.circle(x,y,radius*1.8f,(alpha/3<<24)|0x58b8be);
            p.circle(x,y,radius,(alpha<<24)|0xdafbf0);
        }
        // Bow pressure highlights the front shoulder without a large opaque splash decal.
        float bow=b.half+b.radius+1;
        for(int side=-1;side<=1;side+=2) for(int i=0;i<4;i++) {
            float xx=bow-i*1.7f,yy=side*(3+i*3);
            float x=b.x+c*xx-s*yy,y=b.y+s*xx+c*yy;
            int alpha=(int)((55+150*kick)*power);
            p.line(x,y,x-c*2-s*side*2,y-s*2+c*side*2,1.3f,(alpha<<24)|0xe0fff4);
        }
    }
    public static void effects(Game g,Painter p) {
        if(g.impactTicks>0) {
            float t=1-g.impactTicks/45f;
            ring(p,g.impactX,g.impactY,10+40*t,0x99b8f9e8,1.4f);
            for(int i=0;i<10;i++) {
                double a=i*Math.PI/5;
                float r=12+37*t;
                float x=g.impactX+(float)StrictMath.cos(a)*r,y=g.impactY+(float)StrictMath.sin(a)*r-12*(float)StrictMath.sin(t*Math.PI);
                float size=3*(1-t)+1;
                p.rect(x-size,y,size*2,size,0xddfff5d0);
                p.rect(x,y-size,size,size*2,0xaad7fff2);
            }
        }
        if(g.celebrationTicks>0) {
            float t=1-g.celebrationTicks/90f;
            if(t<.6f) ring(p,g.celebrationX,g.celebrationY,15+95*t,0xbb8bffd0,3*(1-t));
            for(int i=0;i<12;i++) {
                double a=i*Math.PI*2/12;
                float r=12+79*t;
                float x=g.celebrationX+(float)StrictMath.cos(a)*r,y=g.celebrationY+(float)StrictMath.sin(a)*r+26*t*t;
                if(i%3==0) star(p,x,y,4.5f*(1-t*.4f),VisualStyle.GOLD);
                else {
                    p.rect(x,y,3+(i%2)*2,2,i%2==0?0xffffc65b:0xffa0ffd2);
                    if(t<.7f) p.rect(x+2,y-3,1,2,0xfffff3c9);
                }
            }
            float y=g.celebrationY-25-25*t;
            String points="+"+g.celebrationPoints;
            RiverPainter.center(p,points,g.celebrationX+2,y+3,25,VisualStyle.OUTLINE);
            RiverPainter.center(p,points,g.celebrationX,y,25,0xffffda64);
            RiverPainter.center(p,"LOAD CLEARED!",g.celebrationX+1,y+34,10,VisualStyle.OUTLINE);
            RiverPainter.center(p,"LOAD CLEARED!",g.celebrationX,y+33,10,VisualStyle.CREAM);
        }
    }
    public static void star(Painter p,float x,float y,float radius,int color) {
        float[] points=new float[20];
        for(int i=0;i<10;i++) {
            double a=-Math.PI/2+i*Math.PI/5;float r=i%2==0?radius:radius*.47f;
            points[i*2]=x+(float)StrictMath.cos(a)*r;points[i*2+1]=y+(float)StrictMath.sin(a)*r;
        }
        p.poly(points,color);
    }
    private static void ring(Painter p,float x,float y,float radius,int color,float size) {
        SoftCue.ring(p,x,y,radius,1,color,size);
    }
}
