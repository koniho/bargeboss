package com.bargeboss.game;

import java.util.ArrayList;
import java.util.HashMap;

/** Per-render foreground occlusion. Only already painted vessel pixels can trigger fading.
 * Masks are conservative opaque interiors, shared by every native host. No proximity radius,
 * smoke, wake, rope or objective participates. BuildingFades separately animates
 * the overlap target at fixed steps; these mask queries are stateless. */
public final class SceneryOcclusion implements Painter {
    private final Painter target;
    private final ArrayList<Sprite> vessels=new ArrayList<Sprite>();
    private float recordingDepth=Float.NEGATIVE_INFINITY;
    private float[] recordingClip=ShoreOcclusion.EMPTY;
    private final ArrayList<float[]> clipStack=new ArrayList<float[]>();
    public void pushClipOutside(float[] triangles) {
        clipStack.add(recordingClip);float[] combined=new float[recordingClip.length+triangles.length];
        System.arraycopy(recordingClip,0,combined,0,recordingClip.length);System.arraycopy(triangles,0,combined,recordingClip.length,triangles.length);
        recordingClip=combined;target.pushClipOutside(triangles);
    }
    public void popClip(){recordingClip=clipStack.remove(clipStack.size()-1);target.popClip();}
    public SceneryOcclusion(Painter target) {this.target=target;}
    public void beginWaterFoamBatch(){target.beginWaterFoamBatch();}
    public void endWaterFoamBatch(){target.endWaterFoamBatch();}
    public void waterFoam(float xx,float yy,float ax,float ay,float bx,float by,float phase,int kind,float alpha){target.waterFoam(xx,yy,ax,ay,bx,by,phase,kind,alpha);}
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){target.shoreline(xy,uv,phase,energy,kind,color);}
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){target.surfacePolygon(xy,uv,material,color,wetness);}
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){target.material(x,y,ax,ay,bx,by,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){target.waterReflection(x,y,ax,ay,bx,by,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){target.ropeSegment(x,y,xx,yy,width,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){target.smokePuff(x,y,radius,phase,color);}
    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){target.normalSprite(frame,x,y,w,h,nx,ny,nz,radius,color,alpha);}
    public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){target.spriteAffine(a,f,x,y,ax,ay,bx,by,alpha);}
    public void beginFrame(){target.beginFrame();}
    public boolean supportsUnionClips(){return target.supportsUnionClips();}
    public boolean prefersPolygonBatches(){return target.prefersPolygonBatches();}
    public boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){return target.retainedSurfaces(layer,x,y,scale);}
    public boolean preparesLayers(){return target.preparesLayers();}
    public void prepareLayer(StaticLayer layer,float scale){target.prepareLayer(layer,scale);}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha){target.layer(layer,x,y,scale,alpha);}
    private interface Shape {boolean contains(float x,float y);float left();float top();float right();float bottom();}
    private static final class Sprite implements Shape {
        final long[] rows;final float x,y,w,h,px,py,c,s,l,t,r,b;
        float groundDepth=Float.NEGATIVE_INFINITY;
        float[] hidden=ShoreOcclusion.EMPTY;
        Sprite(int atlas,int frame,float x,float y,float w,float h,float rotation) {
            this.rows=OcclusionMasks.rows(atlas)[frame];this.x=x;this.y=y;
            this.w=w*SpriteAtlas.widthScale(atlas,frame);this.h=h*SpriteAtlas.heightScale(atlas,frame);
            px=SpriteAtlas.pivotX(atlas,frame);py=SpriteAtlas.pivotY(atlas,frame);
            c=(float)StrictMath.cos(rotation);s=(float)StrictMath.sin(rotation);
            float left=Float.MAX_VALUE,top=left,right=-left,bottom=-left;
            for(int i=0;i<4;i++){float dx=(i%2-px)*this.w,dy=(i/2-py)*this.h;
                float xx=x+c*dx-s*dy,yy=y+s*dx+c*dy;
                left=Math.min(left,xx);right=Math.max(right,xx);top=Math.min(top,yy);bottom=Math.max(bottom,yy);}
            l=left;t=top;r=right;b=bottom;
        }
        public float left(){return l;}public float top(){return t;}public float right(){return r;}public float bottom(){return b;}
        public boolean contains(float sx,float sy) {
            float dx=sx-x,dy=sy-y,u=(c*dx+s*dy)/w+px,v=(-s*dx+c*dy)/h+py;
            if(u<0 || u>=1 || v<0 || v>=1)return false;
            return (rows[(int)(v*64)]&(1L<<(int)(u*64)))!=0 && !ShoreOcclusion.contains(hidden,sx,sy);
        }
        int overlap(Shape shape) {
            if(shape.right()<=l || shape.left()>=r || shape.bottom()<=t || shape.top()>=b)return 0;
            int firstX=0,lastX=63,firstY=0,lastY=63;
            if(s==0 && c==1) {
                firstX=Math.max(0,(int)StrictMath.floor((shape.left()-l)/w*64));lastX=Math.min(63,(int)StrictMath.floor((shape.right()-l)/w*64));
                firstY=Math.max(0,(int)StrictMath.floor((shape.top()-t)/h*64));lastY=Math.min(63,(int)StrictMath.floor((shape.bottom()-t)/h*64));
            }
            int hits=0;
            for(int yy=firstY;yy<=lastY;yy++) {
                long bits=rows[yy];
                for(int xx=firstX;xx<=lastX;xx++)if((bits&(1L<<xx))!=0) {
                    float dx=((xx+.5f)/64-px)*w,dy=((yy+.5f)/64-py)*h;
                    if(shape.contains(x+c*dx-s*dy,y+s*dx+c*dy) && !ShoreOcclusion.contains(hidden,x+c*dx-s*dy,y+s*dx+c*dy) && ++hits==4)return hits;
                }
            }
            return hits;
        }
    }
    private static final class Polygon implements Shape {
        final float[] xy;final float l,t,r,b;
        Polygon(float[] xy) {this.xy=xy;float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
            for(int i=0;i<xy.length;i+=2){l=Math.min(l,xy[i]);r=Math.max(r,xy[i]);t=Math.min(t,xy[i+1]);b=Math.max(b,xy[i+1]);}
            this.l=l;this.t=t;this.r=r;this.b=b;}
        public float left(){return l;}public float top(){return t;}public float right(){return r;}public float bottom(){return b;}
        public boolean contains(float x,float y) {
            boolean inside=false;
            for(int i=0,j=xy.length-2;i<xy.length;j=i,i+=2) {
                float ax=xy[i],ay=xy[i+1],bx=xy[j],by=xy[j+1];
                if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax)inside=!inside;
            }
            return inside;
        }
    }
    private static final class Stroke implements Shape {
        final float x,y,xx,yy,r;
        Stroke(float x,float y,float xx,float yy,float width){this.x=x;this.y=y;this.xx=xx;this.yy=yy;r=width*.5f;}
        public float left(){return Math.min(x,xx)-r;}public float top(){return Math.min(y,yy)-r;}
        public float right(){return Math.max(x,xx)+r;}public float bottom(){return Math.max(y,yy)+r;}
        public boolean contains(float sx,float sy){float dx=xx-x,dy=yy-y,d=dx*dx+dy*dy;
            float t=d==0?0:Physics.clamp(((sx-x)*dx+(sy-y)*dy)/d,0,1);dx=sx-x-t*dx;dy=sy-y-t*dy;return dx*dx+dy*dy<r*r;}
    }
    private static SceneryOcclusion context(Painter p) {
        if(p instanceof RiverPainter)p=((RiverPainter)p).canvas();
        return p instanceof SceneryOcclusion?(SceneryOcclusion)p:null;
    }
    private float opacity(Shape shape) {return opacity(shape,Float.POSITIVE_INFINITY);}
    private float opacity(Shape shape,float depth) {
        int hits=0;
        for(Sprite vessel:vessels)if(vessel.groundDepth<=depth){hits+=vessel.overlap(shape);if(hits>=4)return .28f;}
        // Easing starts inside actual opaque overlap, never outside the silhouette.
        float t=hits/4f;t=t*t*(3-2*t);return 1-.72f*t;
    }
    private static final class WaterPlacement {
        final float rise,size;final boolean overlaps;
        WaterPlacement(float rise,float size,boolean overlaps){this.rise=rise;this.size=size;this.overlaps=overlaps;}
    }
    @SuppressWarnings("unchecked")
    private static final HashMap<Long,WaterPlacement>[] WATER_BUILDINGS=new HashMap[5*SpriteAtlas.ATLAS_COUNT*32];
    /** Placement-only eligibility: an inland silhouette never enters the fading path. */
    static boolean buildingOverWater(Game g,int atlas,int frame,float wx,float wy,float rise,float size) {
        int kind=g.tutorial!=null?1:g.lockStage()?2:g.sharedBerthStage()?3:g.depthStage()?4:0;
        int bucket=(kind*SpriteAtlas.ATLAS_COUNT+atlas)*32+frame;
        HashMap<Long,WaterPlacement> placements=WATER_BUILDINGS[bucket];
        if(placements==null)WATER_BUILDINGS[bucket]=placements=new HashMap<Long,WaterPlacement>();
        long key=((long)Float.floatToIntBits(wx)<<32)|(Float.floatToIntBits(wy)&0xffffffffL);
        WaterPlacement cached=placements.get(key);
        if(cached!=null && cached.rise==rise && cached.size==size)return cached.overlaps;
        long[] rows=OcclusionMasks.rows(atlas)[frame];
        float px=SpriteAtlas.pivotX(atlas,frame),py=SpriteAtlas.pivotY(atlas,frame);
        boolean overlaps=false;
        for(int y=0;y<64 && !overlaps;y+=2)for(int x=0;x<64;x+=2)if((rows[y]&(1L<<x))!=0) {
            float dx=((x+.5f)/64-px)*size,dy=((y+.5f)/64-py)*size-rise;
            float ex=wx+(dx/RiverCamera.Q-dy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f;
            float ey=wy+(dx/RiverCamera.Q+dy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f;
            if(g.inWater(ex,ey)){overlaps=true;break;}
        }
        if(placements.size()>1024)placements.clear();
        placements.put(key,new WaterPlacement(rise,size,overlaps));return overlaps;
    }
    private static final Painter SINK=new Painter() {
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}public void poly(float[] xy,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    };
    /** Only vessel artwork is sampled; no effects or renderer calls advance the fade. */
    static SceneryOcclusion snapshot(Game g) {
        SceneryOcclusion scene=new SceneryOcclusion(SINK);
        scene.register(g,g.tug);
        for(Body b:g.cargo)if(b!=null && b.active)scene.register(g,b);
        if(g.tutorial==null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE){scene.register(g,f.boat);if(f.load!=null)scene.register(g,f.load);}
        if(g.trafficActive())scene.register(g,g.trafficBoat);
        return scene;
    }
    private void register(Game g,Body b) {
        if(b==null || !b.active)return;
        recordingDepth=g.layout.camera.screenY(b.x,b.y);
        recordingClip=ShoreOcclusion.vessel(g,g.layout.camera,b,false);
        Painter p=new RiverPainter(this,g.layout.camera);boolean tug=b.berth<0;
        float bob=WaterSurface.lift(g,b),x=b.x+RiverCamera.riseX(bob),y=b.y+RiverCamera.riseY(bob);
        float size=2*(b.half+b.radius)/(b.yacht?YachtArt.HULL_FRACTION:tug?TugArt.HULL_FRACTION:BargeArt.HULL_FRACTION);
        SpriteAtlas.draw(p,b.yacht?SpriteAtlas.YACHT:tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,x,y,size,size,1);
        if(tug || b.yacht)return;
        if(b.berth==2){if(b.unloadingTicks<Game.UNLOAD_TICKS)SpriteAtlas.draw(p,SpriteAtlas.BULK,b.angle,x,y,size,size,1-b.unloadingTicks/(float)Game.UNLOAD_TICKS);return;}
        int remaining=b.unloadingTicks==0?3:Math.max(0,3-(b.unloadingTicks+50)/60);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle),cargoSize=b.berth==0?58:26;
        for(int i=0;i<remaining;i++) {
            float xx=b.berth==0?0:-17+i*17,yy=b.berth==0?-8+i*8:0;
            SpriteAtlas.draw(p,b.berth==0?SpriteAtlas.STEEL:SpriteAtlas.CONTAINER,b.angle,x+c*xx-s*yy,y+s*xx+c*yy,cargoSize,cargoSize,1);
        }
    }
    float buildingTarget(Game g,int atlas,int frame,float wx,float wy,float rise,float size) {
        RiverCamera c=g.layout.camera;float depth=c.screenY(wx,wy),x=c.screenX(wx,wy),y=depth-rise*c.scale;
        float w=size*c.scale*SpriteAtlas.widthScale(atlas,frame),h=size*c.scale*SpriteAtlas.heightScale(atlas,frame);
        float l=x-SpriteAtlas.pivotX(atlas,frame)*w,t=y-SpriteAtlas.pivotY(atlas,frame)*h;
        // Reject distant silhouettes before allocating masks or checking water eligibility.
        boolean candidate=false;
        for(Sprite vessel:vessels)if(vessel.groundDepth<=depth && vessel.r>l && vessel.l<l+w && vessel.b>t && vessel.t<t+h){candidate=true;break;}
        if(!candidate || !buildingOverWater(g,atlas,frame,wx,wy,rise,size))return 1;
        return opacity(new Sprite(atlas,frame,x,y,size*c.scale,size*c.scale,0),depth);
    }
    /** Instant overlap target for diagnostics; production buildings read BuildingFades. */
    public static float buildingOpacity(Game g,Painter p,int atlas,int frame,float wx,float wy,float rise,float size) {
        if(!buildingOverWater(g,atlas,frame,wx,wy,rise,size))return 1;
        RiverCamera c=g.layout.camera;
        return spriteOpacity(p,atlas,frame,c.screenX(wx,wy),c.screenY(wx,wy)-rise*c.scale,size*c.scale,size*c.scale);
    }
    public static float spriteOpacity(Painter p,int atlas,int frame,float x,float y,float w,float h) {
        SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?1:scene.opacity(new Sprite(atlas,frame,x,y,w,h,0));
    }
    public static float polygonOpacity(Painter p,float[] xy) {
        SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?1:scene.opacity(new Polygon(xy));
    }
    /** Code-drawn foreground pieces test their actual polygons/strokes, not a group rectangle. */
    public static Painter obstacles(Painter p) {SceneryOcclusion scene=context(p);return scene==null || scene.vessels.isEmpty()?p:new Obstacles(p,scene);}
    private static final class Obstacles implements Painter {
        final Painter p;final SceneryOcclusion scene;Obstacles(Painter p,SceneryOcclusion scene){this.p=p;this.scene=scene;}
        private int color(int color,Shape shape){if((color>>>24)<160)return color;return (((int)((color>>>24)*scene.opacity(shape)))<<24)|(color&0xffffff);}
        public void pushClipOutside(float[] triangles){p.pushClipOutside(triangles);}
        public void popClip(){p.popClip();}
        public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){p.shoreline(xy,uv,phase,energy,kind,color(color,new Polygon(xy)));}
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){p.surfacePolygon(xy,uv,material,color(color,new Polygon(xy)),wetness);}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){p.material(x,y,ax,ay,bx,by,kind,detail,amount,color);}
        public void smokePuff(float x,float y,float radius,float phase,int color){p.smokePuff(x,y,radius,phase,color);}
        public void rect(float x,float y,float w,float h,int c){p.rect(x,y,w,h,color(c,new Polygon(new float[]{x,y,x+w,y,x+w,y+h,x,y+h})));}
        public void poly(float[] xy,int c){p.poly(xy,color(c,new Polygon(xy)));}
        public void line(float x,float y,float xx,float yy,float w,int c){p.line(x,y,xx,yy,w,color(c,new Stroke(x,y,xx,yy,w)));}
        public void circle(float x,float y,float r,int c){p.circle(x,y,r,color(c,new Stroke(x,y,x,y,r*2)));}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){p.sprite(a,f,x,y,w,h,r,alpha);}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){p.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);}
    }
    private void remember(int atlas,int frame,float x,float y,float w,float h,float r,float alpha) {
        if((atlas==SpriteAtlas.YACHT || atlas>=SpriteAtlas.TUG && atlas<=SpriteAtlas.BULK) && alpha>=.15f && w>0 && h>0)
            {Sprite vessel=new Sprite(atlas,frame,x,y,w,h,r);vessel.groundDepth=recordingDepth;vessel.hidden=recordingClip;vessels.add(vessel);}
    }
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){remember(a,f,x,y,w,h,r,alpha);target.sprite(a,f,x,y,w,h,r,alpha);}
    public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){
        remember(a,f,x,y,w,h,r,(1-mix)*alpha);remember(a,ff,x,y,w,h,r,mix*alpha);
        target.spriteBlend(a,f,ff,x,y,w,h,r,mix,alpha);
    }
    public void rect(float x,float y,float w,float h,int c){target.rect(x,y,w,h,c);}
    public void circle(float x,float y,float r,int c){target.circle(x,y,r,c);}
    public void poly(float[] xy,int c){target.poly(xy,c);}
    public void polygons(float[][] xy,int count,int c){target.polygons(xy,count,c);}
    public void line(float x,float y,float xx,float yy,float w,int c){target.line(x,y,xx,yy,w,c);}
}
