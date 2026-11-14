package com.bargeboss.game;

/** Measures visible new activity submissions over a minute, using warmed native-cache accounting. */
public final class FactoryActivityCost {
    private static final class Capture implements Painter {
        final ScenePerformanceBenchmark.Counter target;int extra;
        Capture(float width){target=new ScenePerformanceBenchmark.Counter(width,600,true);}
        public void beginFrame(){target.beginFrame();}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){if(layer.key()==0x42000010 || layer.key()==0x42000011)extra++;target.layer(layer,x,y,scale,alpha);}
        public void line(float x,float y,float xx,float yy,float w,int color){if((color&0xffffff)==0xffd88d)extra++;target.line(x,y,xx,yy,w,color);}
        public void rect(float x,float y,float w,float h,int color){target.rect(x,y,w,h,color);}
        public void circle(float x,float y,float radius,int color){target.circle(x,y,radius,color);}
        public void poly(float[] xy,int color){target.poly(xy,color);}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){target.sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){target.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);}
    }
    public static void main(String[] args) {
        System.out.println("width,scene,averageActivityCalls,maxActivityCalls,averageTotalCalls,maxTotalCalls,maxActivityPercent");
        for(int width:new int[]{960,1333,1920})for(String scene:ScenePerformanceBenchmark.SCENES) {
            Game g=ScenePerformanceBenchmark.fixture(width,scene);Capture p=new Capture(width);int base=g.visualTicks;
            for(int warm=0;warm<70;warm++){p.target.clear();p.extra=0;Renderer.draw(g,p);}
            long extras=0,calls=0;int maxExtra=0,maxCalls=0;float maxRatio=0;
            for(int sample=0;sample<120;sample++) {
                g.visualTicks=base+sample*30;p.target.clear();p.extra=0;Renderer.draw(g,p);
                int count=p.target.calls();extras+=p.extra;calls+=count;maxExtra=Math.max(maxExtra,p.extra);maxCalls=Math.max(maxCalls,count);
                maxRatio=Math.max(maxRatio,p.extra*100f/count);
            }
            System.out.printf(java.util.Locale.ROOT,"%d,%s,%.2f,%d,%.2f,%d,%.3f%n",width,scene,extras/120.0,maxExtra,calls/120.0,maxCalls,maxRatio);
            if(maxRatio>3)throw new AssertionError("Factory activity exceeds3% submission budget");
        }
    }
}
