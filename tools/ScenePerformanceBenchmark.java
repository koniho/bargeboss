package com.bargeboss.game;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;

/** Reproducible real tutorial snapshots. Host timings never enter gameplay. */
public final class ScenePerformanceBenchmark {
    public static final String[] SCENES={"basin-entry","canal-entry","canal-middle","harbor-close"};
    public static final String[] PARTS={"full","factories","ground","roads","walls"};
    public static Game fixture(int width,String scene) {
        Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();
        if(scene.equals("basin-entry"))return g;
        int channelTicks=0;
        for(int tick=0;tick<16000;tick++) {
            if(g.tutorial.lesson==Tutorial.CHANNEL)channelTicks++;
            if(scene.equals("canal-entry") && channelTicks==1)return g;
            if(scene.equals("canal-middle") && channelTicks==700)return g;
            if(scene.equals("harbor-close") && g.tutorial.lesson==Tutorial.STEER)return g;
            ReplayInput.tutorial(root);root.step();
        }
        throw new AssertionError("Fixture not reached: "+scene);
    }
    public static void draw(Game g,Painter p,String part) {
        if(part.equals("full"))Renderer.draw(g,p);
        else if(part.equals("factories"))for(int i=0;i<TrainingChannelView.FACTORY_PLACEMENTS;i++)
            FactoryArt.draw(g,p,TrainingChannelView.factoryCell(i),TrainingChannelView.factoryX(i),TrainingChannelView.factoryY(i));
        else if(part.equals("ground"))TrainingGroundView.ground(g,p);
        else if(part.equals("roads"))IndustrialRoadView.draw(g,p);
        else if(part.equals("walls")){TrainingGroundView.walls(g,p);TrainingGroundView.caps(g,p);TrainingGroundView.foreground(g,p);}
        else throw new IllegalArgumentException(part);
    }
    /** Counts submission work and destination rectangle coverage, not actual GPU pixels. */
    public static final class Counter implements Painter {
        public int sprites,blends,rects,circles,lines,polys,offscreenCircles;
        public int factorySprites,groundSprites;public double factoryArea,circleArea;
        final float width,height;
        public int layerCalls,layerBlits,cacheHits,cacheBuilds,cacheFallbacks,buildPrimitives;
        public long cacheBytes;
        private final boolean nativeLayers;
        private int buildsThisFrame;
        private final LinkedHashMap<Long,Long> cached=new LinkedHashMap<Long,Long>(16,.75f,true);
        public Counter(float w,float h){this(w,h,false);}
        public Counter(float w,float h,boolean nativeLayers){width=w;height=h;this.nativeLayers=nativeLayers;}
        public void beginFrame(){buildsThisFrame=0;}
        public void clear(){sprites=blends=rects=circles=lines=polys=offscreenCircles=factorySprites=groundSprites=0;factoryArea=circleArea=0;layerCalls=layerBlits=cacheHits=cacheBuilds=cacheFallbacks=buildPrimitives=0;}
        public int calls(){return sprites+blends+rects+circles+lines+polys+layerBlits;}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
            if(scale<=0 || alpha<=0 || area(x,y,x+layer.width()*scale,y+layer.height()*scale)==0)return;
            layerCalls++;
            if(!nativeLayers){layer.draw(this,x,y,scale,alpha);return;}
            float density=StaticLayer.rasterScale(scale);
            long key=((long)layer.key()<<32)^(Float.floatToIntBits(density)&0xffffffffL);
            if(cached.get(key)!=null){cacheHits++;layerBlits++;return;}
            long bytes=4L*(long)StrictMath.ceil(layer.width()*density)*(long)StrictMath.ceil(layer.height()*density);
            if(bytes<=StaticLayer.cacheBudgetBytes() && buildsThisFrame<StaticLayer.maxBuildsPerFrame()) {
                Iterator<Map.Entry<Long,Long>> it=cached.entrySet().iterator();
                while(cacheBytes+bytes>StaticLayer.cacheBudgetBytes() && it.hasNext()){cacheBytes-=it.next().getValue();it.remove();}
                cached.put(key,bytes);cacheBytes+=bytes;buildsThisFrame++;cacheBuilds++;layerBlits++;
                Counter paint=new Counter(layer.width(),layer.height());layer.paint(paint);buildPrimitives+=paint.calls();
            } else {
                cacheFallbacks++;layer.draw(this,x,y,scale,alpha);
                // Current hosts use an offscreen group for translucent cold layers.
                // Count one composite here; oversized multi-tile fallbacks need native telemetry.
                if(alpha<1)layerBlits++;
            }
        }
        private double area(float l,float t,float r,float b){return Math.max(0,Math.min(width,r)-Math.max(0,l))*Math.max(0,Math.min(height,b)-Math.max(0,t));}
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha) {
            sprites++;
            if(atlas==SpriteAtlas.GROUND)groundSprites++;
            if(atlas==SpriteAtlas.FACTORY) {
                factorySprites++;w*=SpriteAtlas.widthScale(atlas,frame);h*=SpriteAtlas.heightScale(atlas,frame);
                float left=x-SpriteAtlas.pivotX(atlas,frame)*w,top=y-SpriteAtlas.pivotY(atlas,frame)*h;
                factoryArea+=area(left,top,left+w,top+h);
            }
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){blends++;}
        public void rect(float x,float y,float w,float h,int color){rects++;}
        public void line(float x,float y,float xx,float yy,float w,int color){lines++;}
        public void poly(float[] xy,int color){polys++;}
        public void polygons(float[][] xy,int count,int color){polys+=nativeLayers?1:count;}
        public void circle(float x,float y,float r,int color){circles++;double a=area(x-r,y-r,x+r,y+r);circleArea+=a;if(a==0)offscreenCircles++;}
    }
    private static double percentile(long[] samples,int percent){Arrays.sort(samples);return samples[Math.min(samples.length-1,samples.length*percent/100)]/1e6;}
    private static void countProfile(Game g,int width,String scene,String part,int samples,boolean nativeLayers) {
        Counter p=new Counter(width,600,nativeLayers);
        if(nativeLayers) {
            p.beginFrame();long start=System.nanoTime();draw(g,p,part);
            printCount("CACHE_COLD",width,scene,part,(System.nanoTime()-start)/1e6,0,p,0);
        }
        int warmFrames=0;
        for(int i=0;i<(nativeLayers?240:20);i++){p.clear();p.beginFrame();draw(g,p,part);warmFrames++;if(nativeLayers && p.cacheFallbacks==0 && p.cacheBuilds==0)break;}
        long[] times=new long[samples];
        for(int i=0;i<samples;i++){p.clear();p.beginFrame();long start=System.nanoTime();draw(g,p,part);times[i]=System.nanoTime()-start;}
        printCount(nativeLayers?"CACHE_WARM":"EXPANDED",width,scene,part,percentile(times,50),percentile(times,95),p,warmFrames);
    }
    private static void printCount(String mode,int width,String scene,String part,double p50,double p95,Counter p,int warmFrames) {
        System.out.printf(java.util.Locale.ROOT,"%s,%d,%s,%s,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%d,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%.3f,%d%n",
                mode,width,scene,part,p50,p95,p.calls(),p.sprites,p.factorySprites,p.groundSprites,p.circles,p.offscreenCircles,p.polys,p.lines,p.rects,
                p.factoryArea/(width*600.0),p.circleArea/(width*600.0),p.layerCalls,p.layerBlits,p.cacheHits,p.cacheBuilds,p.cacheFallbacks,p.buildPrimitives,p.cacheBytes/1048576.0,warmFrames);
    }
    private static void rasterProfile(Game g,int width,String scene,int samples) {
        BufferedImage image=new BufferedImage(width,600,BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Painter p=new DesktopPainter(graphics);
        long cold=System.nanoTime();Renderer.draw(g,p);cold=System.nanoTime()-cold;
        // The native cache admits one raster/frame; three warmups would still
        // measure cold terrain replay. Stable fixtures need up to25 new keys.
        for(int i=0;i<80;i++)Renderer.draw(g,p);
        long[] times=new long[samples];
        for(int i=0;i<samples;i++){long start=System.nanoTime();Renderer.draw(g,p);times[i]=System.nanoTime()-start;}
        graphics.dispose();System.out.printf(java.util.Locale.ROOT,"RASTER,%d,%s,first=%.3fms,p50=%.3fms,p95=%.3fms%n",width,scene,cold/1e6,percentile(times,50),percentile(times,95));
    }
    public static void main(String[] args) {
        boolean raster=args.length>0 && args[0].equals("raster");
        System.out.println("mode,width,scene,part,cpu_p50_ms,cpu_p95_ms,calls,sprites,factorySprites,groundSprites,circles,offscreenCircles,polys,lines,rects,factoryRectScreenCoverage,circleBoxScreenCoverage,layerCalls,layerBlits,cacheHits,cacheBuilds,cacheFallbacks,buildPrimitives,cacheMiB,warmFrames");
        for(int width:new int[]{960,1333,1920})for(String scene:SCENES) {
            Game g=fixture(width,scene);
            if(raster)rasterProfile(g,width,scene,8);
            else for(String part:PARTS){countProfile(g,width,scene,part,50,false);countProfile(g,width,scene,part,50,true);}
        }
        System.out.println("Timings are Termux JVM/Java2D only, not Android Canvas/GPU timings. CACHE modes simulate the native byte cap, LRU and one build/frame; buildPrimitives are separate offscreen work, not main-target calls. Large translucent fallback tiling is approximated as one composite; inspect cacheFallbacks before claiming a warm hit. EXPANDED replays immutable commands. Destination-area ratios are upper-bound submission coverage, not visible alpha pixels.");
    }
}
