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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    private WaterFoamShader waterFoamShader;
    private FoamMesh foamMesh;
    private boolean meshFoam=true,inFoamBatch;
    public void profileMeshFoam(boolean enabled){meshFoam=enabled;}
    public void beginWaterFoamBatch(){inFoamBatch=meshFoam && foamMesh!=null && canvas.isHardwareAccelerated();}
    public void endWaterFoamBatch(){flushFoam();inFoamBatch=false;}
    private void flushFoam(){if(foamMesh!=null)foamMesh.flush(canvas);}
    private NormalSpriteShader normalSpriteShader;
    private final ConcurrentMap<Integer,Atlas> atlases=new ConcurrentHashMap<Integer,Atlas>();
    private final ConcurrentMap<Integer,Object> atlasLocks=new ConcurrentHashMap<Integer,Object>();
    private final ConcurrentMap<String,Bitmap> sourceImages=new ConcurrentHashMap<String,Bitmap>();
    // Only cropped cells allocate extra CPU storage. Full-image cells reuse the source.
    // Bound this independently of terrain/UI rasters; never recycle a bitmap that a
    // hardware display list may still reference.
    private static final long FRAME_TEXTURE_BUDGET=160L*1024*1024;
    private long frameBudget=FRAME_TEXTURE_BUDGET;
    private final Object frameLock=new Object();
    private volatile boolean closed,rotationsReady;
    private boolean rotationPrefetch=true,rotationStarted,rotationWorking;
    private volatile int rotationGeneration;
    private int[] rotationPlan;
    private Set<Integer> pinnedAtlases=new HashSet<Integer>();
    private Thread rotationWorker;
    private volatile int rotationFrames;
    private volatile long rotationNs;
    private volatile String rotationFailure;
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
    public void close(){
        if(retainedSurfaces!=null){retainedSurfaces.clear();retainedSurfaces=null;}
        synchronized(frameLock){closed=true;frameImages.clear();frameBytes=0;atlases.clear();sourceImages.clear();}
        if(rotationWorker!=null)rotationWorker.interrupt();
        if(terrain!=null){terrain.close();terrain=null;}
    }
    /** Tooling can prepare the complete catalog; gameplay supplies its upcoming-level manifest. */
    public void prepareRotations(){prepareRotations(SpriteAtlas.rotationAtlases());}
    public void prepareRotations(int[] atlasIds) {
        int[] plan=atlasIds.clone();Arrays.sort(plan);
        Set<Integer> pins=new HashSet<Integer>();
        for(int id:plan) {
            if(id<0 || id>=SpriteAtlas.ATLAS_COUNT || !pins.add(id))throw new IllegalArgumentException("Invalid/duplicate preparation atlas: "+id);
        }
        synchronized(frameLock) {
            if(closed || !rotationPrefetch || !frameTextures || Arrays.equals(plan,rotationPlan))return;
            // Replace speculation with the newest level. Shared cached cells remain reusable;
            // old-only cells become ordinary LRU candidates instead of staying pinned forever.
            rotationPlan=plan;pinnedAtlases=pins;rotationGeneration++;rotationStarted=true;
            rotationsReady=false;rotationFrames=0;rotationNs=0;rotationFailure=null;
            if(rotationWorking)return;
            rotationWorking=true;
            rotationWorker=new Thread(this::prepareRotationPlans,"bargeboss-rotations");
            rotationWorker.start();
        }
    }
    private boolean currentPlan(int generation){return !closed && (generation<0 || generation==rotationGeneration);}
    private void prepareRotationPlans() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        while(true) {
            int generation;int[] plan;
            synchronized(frameLock) {
                if(closed){rotationWorking=false;return;}
                generation=rotationGeneration;plan=rotationPlan;
            }
            long start=System.nanoTime();String failure=null;
            try {
                for(int id:plan) {
                    if(!currentPlan(generation))break;
                    Atlas a=atlas(id,generation);
                    for(int f=0;f<a.frames.length;f++) {
                        if(!currentPlan(generation))break;
                        frameImage(a,f,generation);
                        synchronized(frameLock){if(currentPlan(generation))rotationFrames++;}
                    }
                }
            } catch(RuntimeException error) {
                if(currentPlan(generation)) {
                    failure=error.toString();
                    android.util.Log.e("BargeBoss","Rotation preparation failed; demand loading remains available",error);
                }
            }
            synchronized(frameLock) {
                if(closed){rotationWorking=false;return;}
                if(generation!=rotationGeneration)continue;
                rotationNs=System.nanoTime()-start;rotationFailure=failure;
                rotationsReady=failure==null;rotationWorking=false;return;
            }
        }
    }
    // Diagnostic switches are set before startup. No mutable profile counters on the worker.
    void profileRotationCache(int mib,boolean prefetch) {
        synchronized(frameLock) {
            if(rotationStarted || frameBytes!=0)throw new IllegalStateException("Configure frame cache before use");
            if(mib<1 || mib>160)throw new IllegalArgumentException("Frame budget must be 1..160 MiB");
            frameBudget=mib*1048576L;rotationPrefetch=prefetch;
        }
    }
    long profileFrameBudget(){return frameBudget;}
    boolean profileRotationWorking(){synchronized(frameLock){return rotationWorking;}}
    boolean profileHasFrame(int id,int frame){synchronized(frameLock){return frameImages.containsKey(((long)id<<32)|(frame&0xffffffffL));}}
    boolean profileHasAtlas(int id){return atlases.containsKey(id);}
    boolean profileRotationsReady(){return rotationsReady;}
    int profileRotationFrames(){return rotationFrames;}
    long profileRotationNs(){return rotationNs;}
    String profileRotationFailure(){return rotationFailure;}
    void profileAwaitRotations(long millis)throws InterruptedException {if(rotationWorker!=null)rotationWorker.join(millis);}
    Bitmap profileFactoryOriginal(int frame){return atlas(SpriteAtlas.FACTORY).factoryOriginals[frame];}
    Rect profileFactoryBounds(int frame){return new Rect(atlas(SpriteAtlas.FACTORY).factoryOffsets[frame]);}
    Bitmap profileFrameImage(int id,int frame){return frameImage(atlas(id),frame);}

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
    // contours. Skip shared grouping as well when individual paths are preferred.
    private boolean compoundPaths=false,frameTextures=true,forceWaterGrouping=false;
    public boolean prefersPolygonBatches(){return compoundPaths || forceWaterGrouping;}
    private boolean profileSkipFoam,profileFlatSurfaces;
    public void profileFlatSurfaces(boolean enabled){profileFlatSurfaces=enabled;}
    /** Developer-only effect isolation; default production appearance is unchanged. */
    public void profileSkipFoam(boolean enabled){profileSkipFoam=enabled;}
    private RetainedSurfaces retainedSurfaces;
    private boolean retainSurfaces=true,surfaceMeshes,recordingSurfaceMesh;
    private SurfaceMesh surfaceMesh,liveSurfaceMesh;
    private boolean liveSurfaceMeshes;private int liveSurfaceDepth;
    public boolean profileLiveSurfaceMeshes(){return liveSurfaceMeshes;}
    public void profileLiveSurfaceMeshes(boolean enabled){
        liveSurfaceMeshes=enabled;
        if(enabled && liveSurfaceMesh==null && android.os.Build.VERSION.SDK_INT>=34)liveSurfaceMesh=new SurfaceMesh(assets);
    }
    public void beginSurfaceBatch(){
        flushFoam();
        if(liveSurfaceMeshes && liveSurfaceMesh!=null && canvas.isHardwareAccelerated()){
            if(liveSurfaceDepth++==0)liveSurfaceMesh.begin();
        }
    }
    public void endSurfaceBatch(){if(liveSurfaceDepth>0){flushSurfaceMesh();liveSurfaceDepth--;}}
    public void profileSurfaceMeshes(boolean enabled){
        if(surfaceMeshes==enabled)return;
        surfaceMeshes=enabled;
        if(retainedSurfaces!=null)retainedSurfaces.clear();
        if(enabled && surfaceMesh==null && android.os.Build.VERSION.SDK_INT>=34)surfaceMesh=new SurfaceMesh(assets);
    }
    private void flushSurfaceMesh(){if(recordingSurfaceMesh)surfaceMesh.flush(canvas);if(liveSurfaceDepth>0)liveSurfaceMesh.flush(canvas);}
    public void profileRetainedSurfaces(boolean enabled){retainSurfaces=enabled;}
    public long profileSurfaceBytes(){return retainedSurfaces==null?0:retainedSurfaces.bytes();}
    public boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){
        if(!retainSurfaces || profileFlatSurfaces || android.os.Build.VERSION.SDK_INT<33 || waterFoamShader==null || !canvas.isHardwareAccelerated())return false;
        if(retainedSurfaces==null)retainedSurfaces=new RetainedSurfaces();
        android.graphics.RenderNode node=retainedSurfaces.get(layer.key());
        if(node==null){
            node=new android.graphics.RenderNode("shore-surfaces");node.setPosition(0,0,(int)layer.width(),(int)layer.height());node.setClipToBounds(false);
            Canvas previous=canvas;canvas=node.beginRecording();
            recordingSurfaceMesh=surfaceMeshes && surfaceMesh!=null;
            if(recordingSurfaceMesh)surfaceMesh.begin();
            long meshBytes;
            try{layer.paint(this);flushSurfaceMesh();meshBytes=recordingSurfaceMesh?surfaceMesh.recordedBytes():0;}
            finally{recordingSurfaceMesh=false;canvas=previous;node.endRecording();}
            retainedSurfaces.put(layer.key(),node,meshBytes);
        }
        int save=canvas.save();try{canvas.translate(x,y);canvas.scale(scale,scale);canvas.drawRenderNode(node);}finally{canvas.restoreToCount(save);}
        return true;
    }
    public void profileWaterGrouping(boolean enabled){forceWaterGrouping=enabled;}
    /** Diagnostic host strategies; only the debuggable session calls this. */
    public void profileStrategies(boolean compoundPaths,boolean frameTextures){this.compoundPaths=compoundPaths;this.frameTextures=frameTextures;}
    public long profileSourceBytes(){long bytes=0;for(Bitmap b:sourceImages.values())bytes+=b.getAllocationByteCount();Atlas factory=atlases.get(SpriteAtlas.FACTORY);if(factory!=null)for(int i=0;i<factory.images.length;i++){bytes+=factory.images[i].getAllocationByteCount();if(factory.factoryOriginals[i]!=null && factory.factoryOriginals[i]!=factory.images[i])bytes+=factory.factoryOriginals[i].getAllocationByteCount();}return bytes;}
    public long profileFrameBytes(){synchronized(frameLock){return frameBytes;}}
    void profileResetTextures(){synchronized(frameLock){
        if(rotationStarted)throw new IllegalStateException("Cannot reset a prefetching painter");
        atlases.clear();sourceImages.clear();frameImages.clear();frameBytes=0;
    }}
    public Profile enableProfile(){if(profile==null)profile=new Profile();return profile;}
    public long profileCacheBytes(){return layerBytes;}

    public boolean supportsUnionClips(){return true;}
    public void pushClipOutside(float[] triangles) {
        flushSurfaceMesh();
        flushFoam();
        if(profile!=null)profile.clips++;
        canvas.save();path.reset();path.setFillType(Path.FillType.WINDING);
        for(int i=0;i<triangles.length;i+=6){path.moveTo(triangles[i],triangles[i+1]);path.lineTo(triangles[i+2],triangles[i+3]);path.lineTo(triangles[i+4],triangles[i+5]);path.close();}
        canvas.clipPath(path,android.graphics.Region.Op.DIFFERENCE);
    }
    public void popClip(){flushSurfaceMesh();flushFoam();canvas.restore();}
    public void beginFrame() {if(!paintingLayer) {layerBuilds=0;if(profile!=null)profile.clear();if(terrain!=null){terrain.beginFrame();terrain.telemetry(profile);}}}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha) { flushSurfaceMesh();
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
        Canvas previous=canvas;boolean wasPainting=paintingLayer,wasCompound=compoundPaths;
        // Font slabs share edges: fill together to avoid antialias seams.
        if((layer.key()&0xff000000)==0x60000000)compoundPaths=true;
        Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);
        paintingLayer=true;canvas=new Canvas(image);canvas.scale(density,density);canvas.clipRect(0,0,layer.width(),layer.height());
        try {layer.paint(this);} finally {canvas=previous;paintingLayer=wasPainting;compoundPaths=wasCompound;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
        return image;
    }
    private void drawLayerDirect(StaticLayer layer,float x,float y,float scale,float alpha) {
        int saved=canvas.save();boolean previousLayer=paintingLayer,wasCompound=compoundPaths;
        if((layer.key()&0xff000000)==0x60000000)compoundPaths=true;
        Paint previousPaint=new Paint(paint),previousSpritePaint=new Paint(spritePaint);paintingLayer=true;
        try {
            canvas.clipRect(x,y,x+layer.width()*scale,y+layer.height()*scale);
            if(alpha<1) canvas.saveLayerAlpha(x,y,x+layer.width()*scale,y+layer.height()*scale,Math.round(Math.max(0,alpha)*255));
            canvas.translate(x,y);canvas.scale(scale,scale);layer.paint(this);
        } finally {canvas.restoreToCount(saved);paintingLayer=previousLayer;compoundPaths=wasCompound;paint.set(previousPaint);spritePaint.set(previousSpritePaint);}
    }
    public CanvasPainter(AssetManager assets) {this(assets,true);}
    CanvasPainter(AssetManager assets,boolean preloadVessels) {
        this.assets=assets;spritePaint.setFilterBitmap(false);
        if(preloadVessels){
            atlas(0);atlas(1);
            if(android.os.Build.VERSION.SDK_INT>=33) {
                try {waterFoamShader=new WaterFoamShader(assets);normalSpriteShader=new NormalSpriteShader(assets);}
                catch(IllegalArgumentException ex) {android.util.Log.w("BargeBoss","Water shader unavailable; using textured fallback",ex);}
                if(android.os.Build.VERSION.SDK_INT>=34 && waterFoamShader!=null){
                    try {surfaceMesh=new SurfaceMesh(assets);surfaceMeshes=true;}
                    catch(IllegalArgumentException ex){android.util.Log.w("BargeBoss","Surface mesh unavailable; retaining path renderer",ex);}
                    try {liveSurfaceMesh=new SurfaceMesh(assets);liveSurfaceMeshes=true;}
                    catch(IllegalArgumentException ex){android.util.Log.w("BargeBoss","Live surface mesh unavailable; retaining path renderer",ex);}
                    try {foamMesh=new FoamMesh(assets);}
                    catch(IllegalArgumentException ex){android.util.Log.w("BargeBoss","Foam mesh unavailable; retaining patch renderer",ex);}
                }
            }
        }
    }
    private static final class Atlas {
        final Bitmap[] images;
        final Rect[] frames,authoredFrames,factoryOffsets;
        final Bitmap[] factoryOriginals;
        final int id;
        final Rect[] cellFrames;
        // The title sky uses very thin adjacent fade strips. Keep their shared
        // texture so filtered sampling at the strip edges remains unchanged.
        boolean cropped(boolean enabled){return enabled && id!=SpriteAtlas.TITLE_BACKDROP;}
        Rect source(int frame,boolean cropped){return cropped(cropped)?cellFrames[frame]:frames[frame];}
        Atlas(int atlas,AssetManager assets,ConcurrentMap<String,Bitmap> sources,Object publicationLock,CanvasPainter owner,int generation) throws IOException {
            int count=SpriteAtlas.count(atlas);
            id=atlas;images=new Bitmap[count];frames=new Rect[count];authoredFrames=new Rect[count];factoryOffsets=new Rect[count];factoryOriginals=new Bitmap[count];cellFrames=new Rect[count];
            for(int f=0;f<count;f++) {
                if(!owner.currentPlan(generation))throw new java.util.concurrent.CancellationException("Preparation superseded or painter closed");
                String path="sprites/"+SpriteAtlas.file(atlas,f);
                String key=SpriteGrade.cacheKey(atlas,path);
                Bitmap image=atlas==SpriteAtlas.FACTORY?null:sources.get(key);
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
                    synchronized(publicationLock) {
                        if(atlas!=SpriteAtlas.FACTORY && owner.currentPlan(generation)){Bitmap published=sources.putIfAbsent(key,image);if(published!=null)image=published;}
                    }
                }
                images[f]=image;
                frames[f]=new Rect(Math.round(image.getWidth()*SpriteAtlas.sourceLeft(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceTop(atlas,f)),
                        Math.round(image.getWidth()*SpriteAtlas.sourceRight(atlas,f)),Math.round(image.getHeight()*SpriteAtlas.sourceBottom(atlas,f)));
                authoredFrames[f]=new Rect(frames[f]);
                if(atlas==SpriteAtlas.FACTORY){
                    Rect crop=FactoryTextureCrop.bounds(image,frames[f]);factoryOffsets[f]=crop;
                    if(owner.keepFactoryOriginals)factoryOriginals[f]=image;
                    Bitmap trimmed=Bitmap.createBitmap(image,crop.left,crop.top,crop.width(),crop.height());
                    trimmed.setDensity(Bitmap.DENSITY_NONE);images[f]=trimmed;
                    // Factory sources belong to this atlas. Never publish a crop as a source
                    // for a cancelled preparation to decode again with the wrong registration.
                    frames[f]=new Rect(0,0,trimmed.getWidth(),trimmed.getHeight());
                }
                cellFrames[f]=new Rect(0,0,frames[f].width(),frames[f].height());
            }
        }
    }
    private Atlas atlas(int id) {return atlas(id,-1);}
    private Atlas atlas(int id,int generation) {
        boolean background=generation>=0;
        Atlas cached=atlases.get(id);if(cached!=null)return cached;
        Object candidate=new Object(),lock=atlasLocks.putIfAbsent(id,candidate);
        if(lock==null)lock=candidate;
        // Only callers of the same atlas wait; decoding never holds the frame LRU lock.
        long start=background || profile==null?0:System.nanoTime();
        synchronized(lock) {
            cached=atlases.get(id);
            if(cached!=null){if(!background && profile!=null)profile.decodeNs+=System.nanoTime()-start;return cached;}
            try {
                cached=new Atlas(id,assets,sourceImages,frameLock,this,generation);
                synchronized(frameLock){if(currentPlan(generation))atlases.put(id,cached);}
                if(!background && profile!=null)profile.decodeNs+=System.nanoTime()-start;
                return cached;
            } catch(IOException e) {throw new IllegalStateException("Missing sprite atlas for asset "+id,e);}
        }
    }
    private Bitmap frameImage(Atlas atlas,int frame){return frameImage(atlas,frame,-1);}
    private Bitmap frameImage(Atlas atlas,int frame,int generation) {
        boolean background=generation>=0;
        Bitmap original=atlas.images[frame],image=original;
        Rect r=atlas.frames[frame];
        if(atlas.cropped(frameTextures) && (r.left!=0 || r.top!=0 || r.right!=original.getWidth() || r.bottom!=original.getHeight())) {
            long key=((long)atlas.id<<32)|(frame&0xffffffffL);
            synchronized(frameLock){image=frameImages.get(key);}
            if(image==null) {
                long start=background || profile==null?0:System.nanoTime();
                // Copy the complete authored crop, including transparent margins.
                // Crop outside the lock; adopt a concurrently published result below.
                image=Bitmap.createBitmap(original,r.left,r.top,r.width(),r.height());
                image.setDensity(Bitmap.DENSITY_NONE);
                long bytes=image.getAllocationByteCount();
                synchronized(frameLock) {
                    Bitmap existing=frameImages.get(key);
                    if(existing!=null)image=existing;
                    else if(currentPlan(generation)) {
                        Iterator<Map.Entry<Long,Bitmap>> entries=frameImages.entrySet().iterator();
                        while(frameBytes+bytes>frameBudget && entries.hasNext()) {
                            Map.Entry<Long,Bitmap> entry=entries.next();
                            // Complete rotation sets survive unrelated scenery pressure.
                            if(pinnedAtlases.contains((int)(entry.getKey()>>>32)))continue;
                            frameBytes-=entry.getValue().getAllocationByteCount();entries.remove();
                            if(!background && profile!=null)profile.frameEvictions++;
                        }
                        if(frameBytes+bytes<=frameBudget){frameImages.put(key,image);frameBytes+=bytes;}
                        else if(background)throw new IllegalStateException("Rotation set exceeds frame cache budget");
                    }
                }
                if(!background && profile!=null){profile.frameBuildNs+=System.nanoTime()-start;profile.frameBuilds++;}
            }
        }
        if(!background && profile!=null)profile.texture(image);
        return image;
    }

    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){ flushSurfaceMesh();
        if(alpha<=.001f)return;
        if(normalSpriteShader==null || !canvas.isHardwareAccelerated() || !frameTextures){Painter.super.normalSprite(frame,x,y,w,h,nx,ny,nz,radius,color,alpha);return;}
        normalSpriteShader.draw(canvas,frameImage(atlas(SpriteAtlas.MOORED_NORMALS),frame),x-w*.5f,y-h*.7f,w,h,nx,ny,nz,radius,color,alpha);
    }
    private boolean trimFactories=true,keepFactoryOriginals;
    void profileKeepFactoryOriginals(boolean enabled){keepFactoryOriginals=enabled;}
    public void profileTrimFactories(boolean enabled){trimFactories=enabled;}
    private void drawSpriteImage(Atlas atlas,int frame,RectF destination,Paint paint){
        if(atlas.id==SpriteAtlas.FACTORY && !trimFactories && atlas.factoryOriginals[frame]!=null){
            Bitmap original=atlas.factoryOriginals[frame];if(profile!=null)profile.texture(original);
            canvas.drawBitmap(original,atlas.authoredFrames[frame],destination,paint);return;
        }
        if(atlas.id==SpriteAtlas.FACTORY){
            Rect a=atlas.authoredFrames[frame],r=atlas.factoryOffsets[frame];
            float x=destination.left,y=destination.top,w=destination.width()/a.width(),h=destination.height()/a.height();
            destination=new RectF(x+(r.left-a.left)*w,y+(r.top-a.top)*h,x+(r.right-a.left)*w,y+(r.bottom-a.top)*h);
        }
        canvas.drawBitmap(frameImage(atlas,frame),atlas.source(frame,frameTextures),destination,paint);
    }
    public void sprite(int id,int frame,float cx,float cy,float width,float height,float rotation,float alpha) { flushSurfaceMesh();
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid sprite frame: "+frame);
        if(profile!=null)profile.sprites++;
        width*=SpriteAtlas.widthScale(id,frame); height*=SpriteAtlas.heightScale(id,frame);
        Atlas atlas=atlas(id);
        float left=-SpriteAtlas.pivotX(id,frame)*width,top=-SpriteAtlas.pivotY(id,frame)*height;
        canvas.save(); canvas.translate(cx,cy); canvas.rotate(rotation*180f/(float)Math.PI);
        spritePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        drawSpriteImage(atlas,frame,new RectF(left,top,left+width,top+height),spritePaint);
        canvas.restore();
    }
    /** Map the complete cropped source cell onto supplied axes without another size correction. */
    public void spriteAffine(int id,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) { flushSurfaceMesh();
        if(frame<0 || frame>=SpriteAtlas.count(id)) throw new IllegalArgumentException("Invalid affine sprite frame: "+frame);
        if(!(alpha>0) || ax*by-ay*bx==0) return;
        if(profile!=null)profile.affines++;
        Atlas atlas=atlas(id);float px=SpriteAtlas.pivotX(id,frame),py=SpriteAtlas.pivotY(id,frame);
        affineValues[0]=ax;affineValues[1]=bx;affineValues[2]=x-ax*px-bx*py;
        affineValues[3]=ay;affineValues[4]=by;affineValues[5]=y-ay*px-by*py;
        affineValues[6]=affineValues[7]=0;affineValues[8]=1;affineMatrix.setValues(affineValues);
        affinePaint.setAlpha(Math.round(Math.max(0,Math.min(1,alpha))*255));
        int saved=canvas.save();
        try {canvas.concat(affineMatrix);drawSpriteImage(atlas,frame,affineUnit,affinePaint);}
        finally {canvas.restoreToCount(saved);}
    }
    public void spriteBlend(int id,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) { flushSurfaceMesh();
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
        drawSpriteImage(atlas,frameA,new RectF(ax,ay,ax+aw,ay+ah),spritePaint);
        spritePaint.setAlpha(weight); spritePaint.setXfermode(ADD);
        drawSpriteImage(atlas,frameB,new RectF(bx,by,bx+bw,by+bh),spritePaint);
        spritePaint.setXfermode(null); canvas.restore(); canvas.restore();
    }
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color) { flushSurfaceMesh();
        if((color>>>24)==0)return;
        if(waterFoamShader==null || !canvas.isHardwareAccelerated() || !waterFoamShader.shoreline(canvas,xy,uv,phase,energy,kind,color))
            Painter.super.shoreline(xy,uv,phase,energy,kind,color);
    }
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness) {
        if(recordingSurfaceMesh && material!=SurfaceMaterial.EMISSIVE && (color>>>24)==255){
            surfaceMesh.add(canvas,xy,uv,material,SceneLighting.surfaceColor(color,material),wetness);return;
        }
        if(liveSurfaceDepth>0 && !profileFlatSurfaces && material!=SurfaceMaterial.EMISSIVE && (color>>>24)==255){
            liveSurfaceMesh.add(canvas,xy,uv,material,SceneLighting.surfaceColor(color,material),wetness);return;
        }
        flushSurfaceMesh();
        if(material==SurfaceMaterial.EMISSIVE){poly(xy,color);return;}
        if((color>>>24)==0)return;
        if(profileFlatSurfaces || waterFoamShader==null || !canvas.isHardwareAccelerated() || !waterFoamShader.surface(canvas,xy,uv,material,SceneLighting.surfaceColor(color,material),wetness))
            Painter.super.surfacePolygon(xy,uv,material,color,wetness);
    }
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color) { flushSurfaceMesh();
        if((color>>>24)==0 || amount<=0)return;
        if(waterFoamShader!=null && canvas.isHardwareAccelerated())waterFoamShader.material(canvas,x,y,ax,ay,bx,by,kind,detail,amount,color);
        else Painter.super.material(x,y,ax,ay,bx,by,kind,detail,amount,color);
    }
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color) { flushSurfaceMesh();
        if((color>>>24)==0)return;
        if(waterFoamShader!=null && canvas.isHardwareAccelerated())
            waterFoamShader.draw(canvas,x,y,ax,ay,bx,by,phase,6,(color>>>24)/255f,color);
        else Painter.super.waterReflection(x,y,ax,ay,bx,by,phase,color);
    }
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color) { flushSurfaceMesh();
        if(waterFoamShader!=null && canvas.isHardwareAccelerated())waterFoamShader.rope(canvas,x,y,xx,yy,width,from,to,color);
        else Painter.super.ropeSegment(x,y,xx,yy,width,from,to,color);
    }
    public void smokePuff(float x,float y,float radius,float phase,int color) { flushSurfaceMesh();
        if(radius<=0 || (color>>>24)==0)return;
        if(waterFoamShader!=null && canvas.isHardwareAccelerated())
            waterFoamShader.draw(canvas,x,y,radius*2,0,0,radius*2,phase,7,(color>>>24)/255f,color);
        else Painter.super.smokePuff(x,y,radius,phase,color);
    }
    public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) { flushSurfaceMesh();
        if(profileSkipFoam || alpha<=.005f)return;
        if(inFoamBatch && kind==5){foamMesh.add(canvas,x,y,ax,ay,bx,by,phase,alpha);return;}
        if(inFoamBatch)flushFoam();
        if(waterFoamShader!=null && canvas.isHardwareAccelerated())
            waterFoamShader.draw(canvas,x,y,ax,ay,bx,by,phase,kind>=8?kind+22:kind,alpha,0xffe5fcf7);
        else Painter.super.waterFoam(x,y,ax,ay,bx,by,phase,kind,alpha);
    }
    public void bind(Canvas canvas) { this.canvas=canvas; }
    public void rect(float x,float y,float w,float h,int c) { flushSurfaceMesh(); paint.setColor(c); canvas.drawRect(x,y,x+w,y+h,paint); }
    public void circle(float x,float y,float r,int c) { flushSurfaceMesh(); if(profile!=null)profile.circles++;paint.setColor(c); canvas.drawCircle(x,y,r,paint); }
    public void poly(float[] xy,int c) { flushSurfaceMesh();
        if(profile!=null)profile.polys++;
        paint.setColor(c); path.reset(); path.moveTo(xy[0],xy[1]);
        for(int i=2;i<xy.length;i+=2) path.lineTo(xy[i],xy[i+1]); path.close(); canvas.drawPath(path,paint);
    }
    public void polygons(float[][] xy,int count,int c) { flushSurfaceMesh();
        if(!compoundPaths){for(int i=0;i<count;i++)poly(xy[i],c);return;}
        if(profile!=null)profile.polys++;
        paint.setColor(c);path.reset();
        for(int n=0;n<count;n++){
            float[] q=xy[n];path.moveTo(q[0],q[1]);
            for(int i=2;i<q.length;i+=2)path.lineTo(q[i],q[i+1]);path.close();
        }
        canvas.drawPath(path,paint);
    }
    public void line(float x,float y,float xx,float yy,float width,int c) { flushSurfaceMesh();
        if(profile!=null)profile.lines++;
        paint.setColor(c); paint.setStrokeWidth(width); paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(x,y,xx,yy,paint);
    }
}
