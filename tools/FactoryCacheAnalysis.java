package com.bargeboss.game;

import java.util.Map;
import java.util.TreeMap;

/** Read-only audit of exact ground-depth bands and retained sprite-bound area. */
public final class FactoryCacheAnalysis {
    private static final class Bounds {
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;int sprites;
        void add(float left,float top,float right,float bottom){l=Math.min(l,left);t=Math.min(t,top);r=Math.max(r,right);b=Math.max(b,bottom);sprites++;}
        double area(float width,float height){return Math.max(0,Math.min(width,r)-Math.max(0,l))*Math.max(0,Math.min(height,b)-Math.max(0,t));}
    }
    private static final class Capture implements Painter {
        final Game g;int factories,faded,vessels,waterOverlap;
        final Map<Integer,Bounds> bands=new TreeMap<Integer,Bounds>();
        double fullRects,tightRects;
        Capture(Game game){g=game;}
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha){
            if(atlas<=SpriteAtlas.BULK){vessels++;return;}
            if(atlas!=SpriteAtlas.FACTORY)return;
            factories++;if(alpha<.999f)faded++;
            RiverCamera camera=g.layout.camera;float lift=GroundArt.LAND_RISE*camera.scale;
            float wx=camera.worldX(x,y+lift),wy=camera.worldY(x,y+lift);
            int depth=Math.round((wy-wx)/32);
            Bounds band=bands.get(depth);if(band==null){band=new Bounds();bands.put(depth,band);}
            float l=x-FactoryArt.pivotX(frame)*w,t=y-FactoryArt.pivotY(frame)*h;
            float[] tight=FactoryArtData.BOUNDS[frame];
            Bounds own=new Bounds();own.add(l,t,l+w,t+h);fullRects+=own.area(g.layout.width,g.layout.height);
            own=new Bounds();own.add(l+tight[0]*w,t+tight[1]*h,l+tight[2]*w,t+tight[3]*h);tightRects+=own.area(g.layout.width,g.layout.height);
            band.add(own.l,own.t,own.r,own.b);
            // Potential foreground overlap is camera-invariant in world units.
            // Sampling a conservative opaque interior gives a useful LOWER bound;
            // production eligibility must also account for silhouette edges.
            long[] rows=OcclusionMasks.rows(SpriteAtlas.FACTORY)[frame];boolean overlap=false;
            for(int yy=0;yy<64&&!overlap;yy++)for(int xx=0;xx<64;xx++)if((rows[yy]&(1L<<xx))!=0) {
                float sx=l+(xx+.5f)/64*w,sy=t+(yy+.5f)/64*h;
                if(TrainingChannel.contains(camera.worldX(sx,sy),camera.worldY(sx,sy))) {overlap=true;break;}
            }
            if(overlap)waterOverlap++;
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){if(a<=SpriteAtlas.BULK)vessels++;}
        public void rect(float x,float y,float w,float h,int color){}
        public void circle(float x,float y,float r,int color){}
        public void line(float x,float y,float xx,float yy,float w,int color){}
        public void poly(float[] xy,int color){}
    }
    public static void main(String[] args) {
        System.out.println("width,scene,factories,faded,potentialWaterOverlapLowerBound,visibleDepthBands,largestBandSprites,bandClippedMiB,individualTightMiB,individualFullMiB,vesselSprites");
        for(int width:new int[]{960,1333,1920})for(String scene:ScenePerformanceBenchmark.SCENES) {
            Game g=ScenePerformanceBenchmark.fixture(width,scene);Capture c=new Capture(g);Renderer.draw(g,c);
            double sum=0;int max=0;for(Bounds b:c.bands.values()){sum+=b.area(width,600);max=Math.max(max,b.sprites);}
            System.out.printf(java.util.Locale.ROOT,"%d,%s,%d,%d,%d,%d,%d,%.3f,%.3f,%.3f,%d%n",width,scene,c.factories,c.faded,c.waterOverlap,c.bands.size(),max,sum*4/1048576,c.tightRects*4/1048576,c.fullRects*4/1048576,c.vessels);
        }
    }
}
