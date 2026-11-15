package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayList;

public final class CanvasPainter implements Painter {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final Paint spritePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint affinePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Matrix affineMatrix=new Matrix();
    private final float[] affineValues=new float[9];
    private final RectF affineUnit=new RectF(0,0,1,1);
    private static final PorterDuffXfermode ADD=new PorterDuffXfermode(PorterDuff.Mode.ADD);
    private final AssetManager assets;
    private final Map<Integer,Atlas> atlases=new HashMap<Integer,Atlas>();
    private final Map<String,Bitmap> sourceImages=new HashMap<String,Bitmap>();
    // Only cropped cells allocate extra CPU storage. Full-image cells reuse the source.
    // Bound this independently of terrain/UI rasters; never recycle a bitmap that a
    // hardware display list may still reference.
    private static final long FRAME_TEXTURE_BUDGET=24L*1024*1024;
    private final LinkedHashMap<Long,Bitmap> frameImages=new LinkedHashMap<Long,Bitmap>(64,.75f,true);
    private long frameBytes;
    private final LinkedHashMap<Long,Bitmap> layers=new LinkedHashMap<Long,Bitmap>(16,.75f,true);
    private final Paint layerPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private long layerBytes;
    private int layerBuilds;
    private boolean paintingLayer;
    private Canvas canvas;
    private TerrainRasterizer terrain;
    private boolean asyncLayers=true;
    public void profileAsyncLayers(boolean enabled){asyncLayers=enabled;}
    public long profilePendingBytes(){return terrain==null?0:terrain.reservedBytes();}
    public long profileWorkerSourceBytes(){return terrain==null?0:terrain.sourceBytes();}
    public void close(){if(terrain!=null){terrain.close();terrain=null;}}
    public boolean preparesLayers(){return asyncLayers && !paintingLayer;}
    public void prepareLayer(StaticLayer layer,float scale) {
        if(!preparesLayers() || !layer.backgroundSafe() || !(scale>0) || !Float.isFinite(scale))return;
        float density=StaticLayer.rasterScale(scale);long key=layerKey(layer,density);
        if(!layers.containsKey(key))requestLayer(layer,density,key,false);
    }
    private static long layerKey(StaticLayer layer,float density){return ((long)layer.key()<<32)^(Float.floatToIntBits(density)&0xffffffffL);}
    private boolean requestLayer(StaticLayer layer,float density,long key,boolean visible) {
        double size=StrictMath.ceil(layer.width()*density)*StrictMath.ceil(layer.height()*density)*4;
        if(!(size>0) || size>StaticLayer.cacheBudgetBytes() || !Double.isFinite(size))return false;
        long bytes=(long)size;
        if(terrain==null)terrain=new TerrainRasterizer(assets);
        if(terrain.request(key,layer,density,bytes,StaticLayer.cacheBudgetBytes()-layerBytes,visible))return true;
        // Visible misses must still progress when the retained cache is full.
        // Speculation never evicts visible cache entries to make room for itself.
        return visible && roomForLayer(bytes) && terrain.request(key,layer,density,bytes,StaticLayer.cacheBudgetBytes()-layerBytes,true);
    }
    /** Opt-in host telemetry; no counters or clocks are read by gameplay. */
    public static final class Profile {
        public long buildNs,decodeNs,frameBuildNs,textureBytes;
        public int frameBuilds,frameEvictions,backgroundBuilds,asyncMisses;
        public long backgroundNs;
        public int layerKey;public float layerWidth,layerHeight;
        private final ArrayList<Bitmap> textures=new ArrayList<Bitmap>();
        void texture(Bitmap image){if(!textures.contains(image)){textures.add(image);textureBytes+=image.getAllocationByteCount();}}
        public int hits,builds,fallbacks,evictions,sprites,blends,affines,circles,polys,lines,clips;
        void clear(){backgroundNs=0;backgroundBuilds=asyncMisses=0;layerKey=0;layerWidth=layerHeight=0;textures.clear();textureBytes=0;frameBuildNs=0;frameBuilds=frameEvictions=0;buildNs=decodeNs=0;hits=builds=fallbacks=evictions=sprites=blends=affines=circles=polys=lines=clips=0;}
    }
    private Profile profile;
    // This device's native compound water paths cost more than their separate
    // contours. Keep the shared batch order, but submit individual native paths.
    private boolean compoundPaths=false,frameTextures=true;
    /** Diagnostic host strategies; only the debuggable session calls this. */
    public void profileStrategies(boolean compoundPaths,boolean frameTextures){this.compoundPaths=compoundPaths;this.frameTextures=frameTextures;}
    public long profileSourceBytes(){long bytes=0;for(Bitmap b:sourceImages.values())bytes+=b.getAllocationByteCount();return bytes;}
    public long profileFrameBytes(){return frameBytes;}
    void profileResetTextures(){atlases.clear();sourceImages.clear();frameImages.clear();frameBytes=0;}
    public Profile enableProfile(){if(profile==null)profile=new Profile();return profile;}
    public long profileCacheBytes(){return layerBytes;}

    public void pushClipOutside(float[] triangles) {
        if(profile!=null)profile.clips++;
        canvas.save();path.reset();path.setFillType(Path.FillType.WINDING);
        for(int i=0;i<triangles.length;i+=6){path.moveTo(triangles[i],triangles[i+1]);path.lineTo(triangles[i+2],triangles[i+3]);path.lineTo(triangles[i+4],triangles[i+5]);path.close();}
        canvas.clipPath(path,android.graphics.Region.Op.DIFFERENCE);
    }
    public void popClip(){canvas.restore();}
    public void beginFrame() {if(!paintingLayer) {layerBuilds=0;if(profile!=null)profile.clear();if(terrain!=null){terrain.beginFrame();terrain.telemetry(profile);}}}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
        float width=layer.width()*scale,height=layer.height()*scale;
        if(!(width>0 && height>0 && alpha>0) || !Float.isFinite(width) || !Float.isFinite(height)) return;
        if(canvas.quickReject(x,y,x+width,y+height,Canvas.EdgeType.AA)) return;
        if(paintingLayer) {drawLayerDirect(layer,x,y,scale,alpha);return;}
        float density=StaticLayer.rasterScale(scale);
        double pw=StrictMath.ceil(layer.width()*density),ph=StrictMath.ceil(layer.height()*density);
        long key=layerKey(layer,density);
        Bitmap image=layers.get(key);
        if(profile!=null && image!=null)profile.hits++;
        if(image==null && asyncLayers && layer.backgroundSafe()) {
            if(terrain!=null)image=terrain.take(key);
            if(image!=null){roomForLayer(image.getAllocationByteCount());layers.put(key,image);layerBytes+=image.getAllocationByteCount();}
            else {
                requestLayer(layer,density,key,true);
                if(profile!=null){profile.fallbacks++;profile.asyncMisses++;}
                drawLayerDirect(layer,x,y,scale,alpha);return;
            }
        }
        if(image==null) {
            if(pw<=0 || ph<=0 || pw*ph*4>StaticLayer.cacheBudgetBytes()
                    || layerBuilds>=StaticLayer.maxBuildsPerFrame()) {if(profile!=null)profile.fallbacks++;drawLayerDirect(layer,x,y,scale,alpha);return;}
            long buildStart=profile==null?0:System.nanoTime();
            if(profile!=null)profile.builds++;
            long bytes=4L*(int)pw*(int)ph;layerBuilds++;
            if(!roomForLayer(bytes)){if(profile!=null)profile.fallbacks++;drawLayerDirect(layer,x,y,scale,alpha);return;}
            if(profile!=null){profile.layerKey=layer.key();profile.layerWidth=layer.width();profile.layerHeight=layer.height();}
            image=rasterLayer(layer,density);
            layers.put(key,image);layerBytes+=image.getAllocationByteCount();
            if(profile!=null)profile.buildNs+=System.nanoTime()-buildStart;
        }
        int saved=canvas.save();
        try {
            canvas.clipRect(x,y,x+width,y+height);canvas.translate(x,y);canvas.scale(scale/density,scale/density);
            layerPaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
            if(profile!=null)profile.texture(image);
            canvas.drawBitmap(image,0,0,layerPaint);
        } finally {canvas.restoreToCount(saved);}
    }
    private boolean roomForLayer(long bytes) {
        if(terrain!=null)terrain.trimTo(Math.max(0,StaticLayer.cacheBudgetBytes()-layerBytes-bytes));
        long reserved=profilePendingBytes();
        Iterator<Map.Entry<Long,Bitmap>> entries=layers.entrySet().iterator();
        while(layerBytes+reserved+bytes>StaticLayer.cacheBudgetBytes() && entries.hasNext()) {
            Bitmap old=entries.next().getValue();layerBytes-=old.getAllocationByteCount();entries.remove();
            if(profile!=null)profile.evictions++;
        }
        return layerBytes+reserved+bytes<=StaticLayer.cacheBudgetBytes();
    }
    /** Used by a private worker painter too. No bitmap is published until painting finishes. */
    Bitmap rasterLayer(StaticLayer layer,float density) {
        Bitmap image=Bitmap.createBitmap((int)StrictMath.ceil(layer.width()*density),(int)StrictMath.ceil(layer.height()*density),Bitmap.Config.ARGB_8888);
        image.setDensity(Bitmap.DENSITY_NONE);
        Canvas previous=canvas;boolean wasPainting=paintingLayer;
        Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);
        paintingLayer=true;canvas=new Canvas(image);canvas.scale(density,density);canvas.clipRect(0,0,layer.width(),layer.height());
        try {layer.paint(this);} finally {canvas=previous;paintingLayer=wasPainting;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
        return image;
    }
    private void drawLayerDirect(StaticLayer layer,float x,float y,float scale,float alpha) {
        int saved=canvas.save();boolean previousLayer=paintingLayer;
        Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);paintingLayer=true;
        try {
            canvas.clipRect(x,y,x+layer.width()*scale,y+layer.height()*scale);
            if(alpha<1) canvas.saveLayerAlpha(x,y,x+layer.width()*scale,y+layer.height()*scale,Math.round(Math.max(0,alpha)*255));
            canvas.translate(x,y);canvas.scale(scale,scale);layer.paint(this);
        } finally {canvas.restoreToCount(saved);paintingLayer=previousLayer;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
    }
    public CanvasPainter(AssetManager assets) {this(assets,true);}
    CanvasPainter(AssetManager assets,boolean preloadVessels) {
        this.assets=assets;spritePaint.setFilterBitmap(false);
        if(preloadVessels){atlas(0);atlas(1);}
    }
    private static final class Atlas {
        final Bitmap[] images;
        final Rect[] frames;
        final int id;
        final Rect[] cellFrames;
        // The title sky uses very thin adjacent fade strips. Keep their shared
        // texture so filtered sampling at the strip edges remains unchanged.
        boolean cropped(boolean enabled){return enabled && id!=SpriteAtlas.TITLE_BACKDROP;}
        Rect source(int frame,boolean cropped){return cropped(cropped)?cellFrames[frame]:frames[frame];}
        Atlas(int atlas,AssetManager assets,Map<String,Bitmap> sources) throws IOException {
            int count=SpriteAtlas.count(atlas);
            id=atlas;images=new Bitmap[count];frames=new Rect[count];cellFrames=new Rect[count];
            for(int f=0;f<count;f++) {
                String path="sprites/"+SpriteAtlas.file(atlas,f);
                String key=SpriteGrade.cacheKey(atlas,path);
                Bitmap image=sources.get(key);
                if(image==null) {
                    try(InputStream input=assets.open(path)) {
                        BitmapFactory.Options options=new BitmapFactory.Options(); options.inScaled=false;
                        options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inMutable=SpriteGrade.applies(atlas);
                        image=BitmapFactory.decodeStream(input,null,options);
                    }
                    if(image==null) throw new IllegalStateException("Unreadable sprite atlas: "+path);
                    if(SpriteGrade.applies(atlas)) {
                        int w=image.getWidth(),h=image.getHeight();int[] row=new int[w];
                        for(int y=0;y<h;y++) {
                            image.getPixels(row,0,w,0,y,w,1);SpriteGrade.applyArgb(atlas,row);
                            image.setPixels(row,0,w,0,y,w,1);
                        }
                    }
                    sources.put(key,image);
                }
                images[f]=image;
                frames[f]=new Rect(Math.round(image.getWidth()*SpriteAtlas.sourceLeft(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceTop(atlas,f)),
                        Math.round(image.getWidth()*SpriteAtlas.sourceRight(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceBottom(atlas,f)));
                cellFrames[f]=new Rect(0,0,frames[f].width(),frames[f].height());
            }
        }
    }
    private Atlas atlas(int id) {
        Atlas cached=atlases.get(id); if(cached!=null) return cached;
        try {
            long decodeStart=profile==null?0:System.nanoTime();
            cached=new Atlas(id,assets,sourceImages); atlases.put(id,cached);
            if(profile!=null)profile.decodeNs+=System.nanoTime()-decodeStart;
            return cached;
        } catch(IOException e) { throw new IllegalStateException("Missing sprite atlas for asset "+id,e); }
    }
    private Bitmap frameImage(Atlas atlas,int frame) {
        Bitmap original=atlas.images[frame],image=original;
        Rect r=atlas.frames[frame];
        if(atlas.cropped(frameTextures) && (r.left!=0 || r.top!=0 || r.right!=original.getWidth() || r.bottom!=original.getHeight())) {
            long key=((long)atlas.id<<32)|(frame&0xffffffffL);
            image=frameImages.get(key);
            if(image==null) {
                long start=profile==null?0:System.nanoTime();
                // Pixel-identical cell copy. Preserve the complete authored crop,
                // including transparent margins, so pivots and effect anchors do not move.
                image=Bitmap.createBitmap(original,r.left,r.top,r.width(),r.height());
                image.setDensity(Bitmap.DENSITY_NONE);
                long bytes=image.getAllocationByteCount();
                Iterator<Map.Entry<Long,Bitmap>> entries=frameImages.entrySet().iterator();
                while(frameBytes+bytes>FRAME_TEXTURE_BUDGET && entries.hasNext()) {
                    frameBytes-=entries.next().getValue().getAllocationByteCount();entries.remove();
                    if(profile!=null)profile.frameEvictions++;
                }
                if(bytes<=FRAME_TEXTURE_BUDGET){frameImages.put(key,image);frameBytes+=bytes;}
                if(profile!=null){profile.frameBuildNs+=System.nanoTime()-start;profile.frameBuilds++;}
            }
        }
        if(profile!=null)profile.texture(image);
        return image;
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        if(profile!=null)profile.sprites++;
        width*=SpriteAtlas.widthScale(id,frame); height*=SpriteAtlas.heightScale(id,frame);
        Atlas atlas=atlas(id);
        float left=-SpriteAtlas.pivotX(id,frame)*width,top=-SpriteAtlas.pivotY(id,frame)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        spritePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        canvas.drawBitmap(frameImage(atlas,frame),atlas.source(frame,frameTextures),new RectF(left,top,left+width,top+height),spritePaint);
        canvas.restore();
    }
    /** Map the complete cropped source cell onto supplied axes without another size correction. */
    public void spriteAffine(int id,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid affine sprite frame: "+frame);
        if(!(alpha>0) || ax*by-ay*bx==0) return;
        if(profile!=null)profile.affines++;
        Atlas atlas=atlas(id);float px=SpriteAtlas.pivotX(id,frame),py=SpriteAtlas.pivotY(id,frame);
        affineValues[0]=ax;affineValues[1]=bx;affineValues[2]=x-ax*px-bx*py;
        affineValues[3]=ay;affineValues[4]=by;affineValues[5]=y-ay*px-by*py;
        affineValues[6]=affineValues[7]=0;affineValues[8]=1;affineMatrix.setValues(affineValues);
        affinePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        int saved=canvas.save();
        try {canvas.concat(affineMatrix);canvas.drawBitmap(frameImage(atlas,frame),atlas.source(frame,frameTextures),affineUnit,affinePaint);}
        finally {canvas.restoreToCount(saved);}
    }
    public void spriteBlend(int id,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) {
        int count=SpriteAtlas.count(id);
        if(frameA<0 || frameA>=count || frameB<0 || frameB>=count)
            throw new IllegalArgumentException("Invalid sprite blend frames");
        int weight=Math.round(Math.max(0,Math.min(1,blend))*255);
        if(weight==0 || frameA==frameB) { sprite(id,frameA,cx,cy,width,height,rotation,alpha); return; }
        if(weight==255) { sprite(id,frameB,cx,cy,width,height,rotation,alpha); return; }
        if(profile!=null)profile.blends++;
        Atlas atlas=atlas(id);
        float aw=width*SpriteAtlas.widthScale(id,frameA),ah=height*SpriteAtlas.heightScale(id,frameA);
        float bw=width*SpriteAtlas.widthScale(id,frameB),bh=height*SpriteAtlas.heightScale(id,frameB);
        float ax=-SpriteAtlas.pivotX(id,frameA)*aw,ay=-SpriteAtlas.pivotY(id,frameA)*ah;
        float bx=-SpriteAtlas.pivotX(id,frameB)*bw,by=-SpriteAtlas.pivotY(id,frameB)*bh;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        canvas.saveLayerAlpha(Math.min(ax,bx)-2,Math.min(ay,by)-2,
                Math.max(ax+aw,bx+bw)+2,Math.max(ay+ah,by+bh)+2,Math.round(Math.max(0,Math.min(1,alpha))*255));
        spritePaint.setAlpha(255-weight);
        canvas.drawBitmap(frameImage(atlas,frameA),atlas.source(frameA,frameTextures),new RectF(ax,ay,ax+aw,ay+ah),spritePaint);
        spritePaint.setAlpha(weight); spritePaint.setXfermode(ADD);
        canvas.drawBitmap(frameImage(atlas,frameB),atlas.source(frameB,frameTextures),new RectF(bx,by,bx+bw,by+bh),spritePaint);
        spritePaint.setXfermode(null); canvas.restore(); canvas.restore();
    }
    public void bind(Canvas canvas) { this.canvas=canvas; }
    public void rect(float x,float y,float w,float h,int c) { paint.setColor(c); canvas.drawRect(x,y,x+w,y+h,paint); }
    public void circle(float x,float y,float r,int c) { if(profile!=null)profile.circles++;paint.setColor(c); canvas.drawCircle(x,y,r,paint); }
    public void poly(float[] xy,int c) {
        if(profile!=null)profile.polys++;
        paint.setColor(c); path.reset(); path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]); path.close(); canvas.drawPath(path,paint);
    }
    public void polygons(float[][] xy,int count,int c) {
        if(!compoundPaths){for(int i=0;i<count;i++)poly(xy[i],c);return;}
        if(profile!=null)profile.polys++;
        paint.setColor(c);path.reset();
        for(int n=0;n<count;n++){
            float[] q=xy[n];path.moveTo(q[0],q[1]);
            for(int i=2;i<q.length;i+=2)path.lineTo(q[i],q[i+1]);path.close();
        }
        canvas.drawPath(path,paint);
    }
    public void line(float x,float y,float xx,float yy,float width,int c) {
        if(profile!=null)profile.lines++;
        paint.setColor(c); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x,y,xx,yy,paint);
    }
}
