package com.bargeboss.game;

/** The entire native drawing boundary. Coordinates are in the shared landscape stage. */
public interface Painter {
    /** Native cache bookkeeping only; never advances simulation. */
    default void beginFrame() {}
    /** Save the current clip and exclude screen-space triangles (six floats each).
     * Triangles must be disjoint unless supportsUnionClips() is true; union triangles
     * must have consistent winding.
     * Native hosts execute this primitive; diagnostic painters record the boundary. */
    default void pushClipOutside(float[] triangles) {}
    /** True when consistently wound overlapping triangles exclude their solid union. */
    default boolean supportsUnionClips(){return false;}
    /** Restore the matching saved clip. Scopes must be balanced, including on errors. */
    default void popClip() {}
    /** Optional native cache preparation. Hints never draw pixels or advance gameplay. */
    /** Optional retained commands for immutable surface-only chunks. False requests direct drawing. */
    default boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){return false;}
    default boolean preparesLayers(){return false;}
    /** Scale is the actual display scale, exactly as supplied to layer(); hosts choose raster density. */
    default void prepareLayer(StaticLayer layer,float scale) {}
    /** Immutable static content, clipped to layer bounds with group opacity. */
    default void layer(StaticLayer layer,float x,float y,float scale,float alpha) {layer.draw(this,x,y,scale,alpha);}
    /** Affine cell basis, already scaled by caller; normalized atlas pivot stays at x,y.
     * Native hosts draw the image. Diagnostic painters may use its projected bounds. */
    default void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        float px=SpriteAtlas.pivotX(atlas,frame),py=SpriteAtlas.pivotY(atlas,frame);
        float[] q=new float[8];int[] corners={0,1,3,2};
        for(int i=0;i<4;i++){float u=corners[i]%2-px,v=corners[i]/2-py;q[i*2]=x+ax*u+bx*v;q[i*2+1]=y+ay*u+by*v;}
        poly(q,((int)(255*alpha)<<24)|0x020913);
    }
    /** Optional ordered batching scope: contains only foam patches; no visual semantic change. */
    default void beginWaterFoamBatch() {}
    default void endWaterFoamBatch() {}
    /** Local water-foam patch centered at x,y with full affine basis vectors.
     * Modes 0/1: shoulder foam, 2/3: prop wash, 4: bow crest, 5: spreading crest; 8: ballistic spray, 9: impact crown.
     * Phase is supplied by shared simulation ticks. Hosts never advance it.
     * The default retains the authored foam textures on non-shader backends. */
    default void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        if(kind<4) spriteAffine(SpriteAtlas.FOAM,kind,x,y,ax,ay,bx,by,alpha);
        else WaterFoam.fallback(this,x,y,ax,ay,bx,by,phase,kind,alpha);
    }
    /** Colored reflection on a centered water-plane patch; shared light state supplies ARGB. */
    default void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color) {
        WaterReflection.fallback(this,x,y,ax,ay,bx,by,phase,color);
    }
    /** Screen-space rope span; strand coordinates stay continuous along the shared curve. */
    default void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color) {
        RopeMaterial.fallback(this,x,y,xx,yy,width,from,to,color);
    }
    /** Centered affine procedural material. Shared code owns geometry and parameters.
     * See EffectMaterial for modes; fallback keeps reference/older hosts functional. */
    default void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color) {
        EffectMaterial.fallback(this,x,y,ax,ay,bx,by,kind,detail,amount,color);
    }
    /** Convex planar face; xy and uv are paired arrays in matching vertex order.
     * UVs are stable model units, never screen pixels. Wetness is shared state in [0,1]. */
    default void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness) {
        poly(xy,SceneLighting.surfaceColor(color,material));
    }
    /** Convex shoreline quad; UV is world-distance along shore and across water/up wall.
     * Kinds: 0 foam, 1 wet wall. Phase is simulation seconds; energy is shared wake strength. */
    default void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color) {
        ShorelineMaterial.fallback(this,xy,uv,phase,energy,kind,color);
    }
    /** Screen-space rolling exhaust. Shared callers own source, transport, palette and opacity. */
    default void smokePuff(float x,float y,float radius,float phase,int color) {
        SmokePuff.fallback(this,x,y,radius,color);
    }
    /** Packed geometric normal/height cell receiving ONE point light. nx/ny/nz are
     * the lamp position relative to the boat origin, radius is in world units.
     * Shared callers select lights; native hosts evaluate local surface response. */
    default void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha) {
        VesselShoreLight.normalSpriteFallback(this,frame,x,y,w,h,nx,ny,nz,radius,color,alpha);
    }
    /** Draw one shared atlas cell around its normalized hull pivot, then rotate in radians. */
    void sprite(int atlas, int frame, float cx, float cy, float width, float height, float rotation, float alpha);
    /** Additively blend registered atlas cells in a transparent layer, then composite once. */
    void spriteBlend(int atlas, int frameA, int frameB, float cx, float cy, float width, float height,
                     float rotation, float blend, float alpha);
    void rect(float x, float y, float width, float height, int argb);
    void circle(float x, float y, float radius, int argb);
    void poly(float[] xy, int argb);
    /** Host preference only: skipping batches preserves geometry and translucent overlap order. */
    default boolean prefersPolygonBatches(){return true;}
    /** Same-color polygons with disjoint antialiased coverage, in one fill.
     * Arrays are borrowed only for this call. Fallbacks retain identical geometry. */
    default void polygons(float[][] xy,int count,int argb) {
        for(int i=0;i<count;i++)poly(xy[i],argb);
    }
    void line(float x1, float y1, float x2, float y2, float width, int argb);
}
