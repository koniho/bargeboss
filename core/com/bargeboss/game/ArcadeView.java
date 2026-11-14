package com.bargeboss.game;

/** Water, vessel churn and arcade rewards. Deterministic drawing only; no wake forces. */
public final class ArcadeView {
    private ArcadeView() {}
    public static void water(Game g,Painter p) {
        HarborAtmosphere.reflections(g,p);
        g.water.draw(g,p);
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
