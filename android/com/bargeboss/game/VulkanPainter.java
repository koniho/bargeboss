package com.bargeboss.game;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.Surface;
import java.nio.*;
import java.util.*;

/** Ordered GPU command encoder. All placement and simulation remain in shared Java. */
final class VulkanPainter implements Painter,AutoCloseable {
    private static final int STRIDE=16,MAX_FLOATS=8*1024*1024,MAX_COMMANDS=65536;
    private static final long TEXTURE_BUDGET=160L*1024*1024;
    private static boolean loaded;
    static final class PreparingLayer extends RuntimeException {
        PreparingLayer(){super("Preparing raster layer",null,false,false);}
    }
    private long nativeHandle;
    private final CanvasPainter resources;
    private float[] vertices=new float[262144];private int used;
    // Command first offsets are floats, allowing 6-float flat and 16-float full vertices.
    private int[] commands=new int[4096];private int commandUsed;
    private ByteBuffer vertexBuffer=ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder());
    private ByteBuffer commandBuffer=ByteBuffer.allocateDirect(commands.length*4).order(ByteOrder.nativeOrder());
    private final int[] clipFirst=new int[64],clipCount=new int[64];
    private int clipDepth,clipCoverageTotal;
    private final int[] clipCoverage=new int[64];
    private float[] clipStarts=new float[512],clipEnds=new float[512];
    private boolean preparingLayers;
    private final LinkedHashMap<Long,Image> images=new LinkedHashMap<Long,Image>(64,.75f,true);
    private final IdentityHashMap<Bitmap,Image> bitmapImages=new IdentityHashMap<Bitmap,Image>();
    private final HashSet<Integer> referenced=new HashSet<Integer>();
    private long textureBytes;private int nextImage=1;
    private float width,height;
    private final LinkedHashMap<Integer,Mesh> meshes=new LinkedHashMap<Integer,Mesh>(32,.75f,true);
    private long meshBytes;
    private VulkanPainter recorder;
    private boolean recording;
    private static final class Mesh {
        final float[] vertices;final int[] commands;
        Mesh(VulkanPainter p){vertices=Arrays.copyOf(p.vertices,p.used);commands=Arrays.copyOf(p.commands,p.commandUsed);}
        long bytes(){return 4L*(vertices.length+commands.length);}
    }
    private VulkanPainter(CanvasPainter resources){this.resources=resources;recording=true;}

    private static final class Image {int id;long key,bytes;Bitmap bitmap;Image(long key,int id,Bitmap b){this.key=key;this.id=id;bitmap=b;bytes=(long)b.getWidth()*b.getHeight()*4;}}
    VulkanPainter(Surface surface,AssetManager assets,CanvasPainter resources,int experimentFlags){
        this.resources=resources;
        synchronized(VulkanPainter.class){if(!loaded){System.loadLibrary("bargeboss_vulkan");loaded=true;}}
        nativeHandle=create(surface,assets,experimentFlags);
    }
    void start(float w,float h){width=w;height=h;used=commandUsed=0;clipDepth=clipCoverageTotal=0;preparingLayers=false;referenced.clear();}
    private LightingProfile lightingProfile=LightingProfile.DUSK;
    public LightingProfile lighting(){return lightingProfile;}
    public void lighting(LightingProfile next){
        if(next==lightingProfile)return;
        for(Image image:images.values())if(nativeHandle!=0)remove(nativeHandle,image.id);
        images.clear();bitmapImages.clear();textureBytes=0;meshes.clear();meshBytes=0;
        lightingProfile=next;resources.lighting(next);
    }
    public void beginFrame(){resources.beginFrame();}
    void present(Viewport v){
        // Never publish an incomplete shore frame. Continue traversing layer requests
        // first so the bounded worker can prepare every visible tile.
        if(preparingLayers)throw new PreparingLayer();
        if(clipDepth!=0)throw new IllegalStateException("Unbalanced Vulkan clipping");
        vertexBuffer.clear();vertexBuffer.asFloatBuffer().put(vertices,0,used);
        commandBuffer.clear();commandBuffer.asIntBuffer().put(commands,0,commandUsed);
        submit(nativeHandle,vertexBuffer,used*4,commandBuffer,commandUsed/5,width,height,v.scale,v.x,v.y);
    }
    public void close(){if(nativeHandle!=0){destroy(nativeHandle);nativeHandle=0;}images.clear();bitmapImages.clear();textureBytes=0;}
    private void space(int count){if(used+count>MAX_FLOATS)throw new IllegalStateException("Vulkan vertex limit");if(used+count>vertices.length){vertices=Arrays.copyOf(vertices,Math.min(MAX_FLOATS,Math.max(used+count,vertices.length*2)));vertexBuffer=ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder());}}
    private static int shaderFamily(float kind){return kind==40?40:kind>=32?32:kind==30?30:kind>=20?20:kind>=14?14:kind>=11?11:kind==10?10:kind>=2?2:0;}
    private void batch(int first,int count,int a,int b,int mode){
        if(count==0 && mode<3)return;referenced.add(a);referenced.add(b);
        int at=commandUsed-5;if((mode<3 || mode==5) && at>=0 && commands[at]+commands[at+1]*(mode==5?6:STRIDE)==first && commands[at+2]==a && commands[at+3]==b && commands[at+4]==mode && (mode!=0 || shaderFamily(vertices[commands[at]+8])==shaderFamily(vertices[first+8]))){commands[at+1]+=count;return;}
        if(commandUsed/5>=MAX_COMMANDS)throw new IllegalStateException("Vulkan command limit");
        if(commandUsed+5>commands.length){commands=Arrays.copyOf(commands,commands.length*2);commandBuffer=ByteBuffer.allocateDirect(commands.length*4).order(ByteOrder.nativeOrder());}
        commands[commandUsed++]=first;commands[commandUsed++]=count;commands[commandUsed++]=a;commands[commandUsed++]=b;commands[commandUsed++]=mode;
    }
    private void vertex(float x,float y,float u,float v,int color,float alpha,float kind,float phase,float detail,float lx,float ly,float lz,float radius){
        space(STRIDE);float a=(color>>>24)/255f*alpha;
        vertices[used++]=x;vertices[used++]=y;vertices[used++]=u;vertices[used++]=v;
        vertices[used++]=((color>>16)&255)/255f*a;vertices[used++]=((color>>8)&255)/255f*a;vertices[used++]=(color&255)/255f*a;vertices[used++]=a;
        vertices[used++]=kind;vertices[used++]=phase;vertices[used++]=detail;vertices[used++]=0;
        vertices[used++]=lx;vertices[used++]=ly;vertices[used++]=lz;vertices[used++]=radius;
    }
    private boolean visible(float x,float y,float ax,float ay,float bx,float by){float loX=Math.min(Math.min(x,x+ax),Math.min(x+bx,x+ax+bx)),hiX=Math.max(Math.max(x,x+ax),Math.max(x+bx,x+ax+bx));float loY=Math.min(Math.min(y,y+ay),Math.min(y+by,y+ay+by)),hiY=Math.max(Math.max(y,y+ay),Math.max(y+by,y+ay+by));return recording || (hiX>=0&&hiY>=0&&loX<=width&&loY<=height);}
    private void quad(float x,float y,float ax,float ay,float bx,float by,int color,float alpha,int texture,float kind,float phase,float detail,float u0,float v0,float u1,float v1,float lx,float ly,float lz,float radius){
        if(alpha<=0 || (color>>>24)==0 || !visible(x,y,ax,ay,bx,by))return;int first=used;
        for(int k=0;k<6;k++){int i=QUAD[k];float u=(i&1),v=(i>>1);vertex(x+ax*u+bx*v,y+ay*u+by*v,u0+(u1-u0)*u,v0+(v1-v0)*v,color,alpha,kind,phase,detail,lx,ly,lz,radius);}batch(first,6,texture,texture,0);
    }
    private static final int[] QUAD={0,1,2,1,3,2};
    private int texture(long key,Bitmap bitmap){
        if(recording)throw new IllegalStateException("Retained surfaces cannot contain textures");
        Image entry=images.get(key);if(entry!=null && entry.bitmap==bitmap){referenced.add(entry.id);return entry.id;}
        Image shared=bitmapImages.get(bitmap);if(shared!=null){images.get(shared.key);referenced.add(shared.id);return shared.id;}
        long bytes=(long)bitmap.getWidth()*bitmap.getHeight()*4;if(bytes>TEXTURE_BUDGET)throw new IllegalStateException("Texture exceeds GPU budget");
        Iterator<Map.Entry<Long,Image>> it=images.entrySet().iterator();
        while(textureBytes+bytes>TEXTURE_BUDGET && it.hasNext()){Image old=it.next().getValue();if(referenced.contains(old.id))continue;remove(nativeHandle,old.id);textureBytes-=old.bytes;bitmapImages.remove(old.bitmap);it.remove();}
        if(textureBytes+bytes>TEXTURE_BUDGET)throw new IllegalStateException("Visible textures exceed GPU budget");
        if(entry!=null){if(referenced.contains(entry.id))throw new IllegalStateException("Texture changed inside frame");remove(nativeHandle,entry.id);textureBytes-=entry.bytes;bitmapImages.remove(entry.bitmap);images.remove(key);}
        entry=new Image(key,nextImage++,bitmap);upload(nativeHandle,entry.id,bitmap);images.put(key,entry);bitmapImages.put(bitmap,entry);textureBytes+=bytes;referenced.add(entry.id);return entry.id;
    }
    public void rect(float x,float y,float w,float h,int color){quad(x,y,w,0,0,h,color,1,0,0,0,0,0,0,1,1,0,0,0,0);}
    public void circle(float x,float y,float radius,int color){if(radius>0)quad(x-radius,y-radius,2*radius,0,0,2*radius,color,1,0,1,0,0,0,0,1,1,0,0,0,0);}
    public void line(float x,float y,float xx,float yy,float w,int color){float dx=xx-x,dy=yy-y,len=(float)StrictMath.hypot(dx,dy);if(len<=0||w<=0)return;float nx=-dy/len*w*.5f,ny=dx/len*w*.5f;quad(x-nx,y-ny,dx,dy,2*nx,2*ny,color,1,0,0,0,0,0,0,1,1,0,0,0,0);// Half-discs meet the shaft without translucent overlap at either endpoint.
        float tx=dx/len*w*.5f,ty=dy/len*w*.5f;
        quad(x-nx,y-ny,2*nx,2*ny,-tx,-ty,color,1,0,1,0,0,0,.5f,1,1,0,0,0,0);
        quad(xx-nx,yy-ny,2*nx,2*ny,tx,ty,color,1,0,1,0,0,0,.5f,1,1,0,0,0,0);}
    private void triangle(float[] xy,int a,int b,int c,int color,int mode){
        int first=used;
        if(!recording && mode==0){
            // Flat fill: position + full-precision premultiplied color, no unused
            // UV/material/light channels. Commands address float offsets.
            space(18);float alpha=(color>>>24)/255f;
            float red=((color>>16)&255)/255f*alpha,green=((color>>8)&255)/255f*alpha,blue=(color&255)/255f*alpha;
            int at=used;float[] out=vertices;
            for(int i=0;i<3;i++){int point=(i==0?a:i==1?b:c)*2;
                out[at++]=xy[point];out[at++]=xy[point+1];
                out[at++]=red;out[at++]=green;out[at++]=blue;out[at++]=alpha;}
            used=at;batch(first,3,0,0,5);return;
        }
        vertex(xy[a*2],xy[a*2+1],0,0,color,1,0,0,0,0,0,0,0);
        vertex(xy[b*2],xy[b*2+1],0,0,color,1,0,0,0,0,0,0,0);
        vertex(xy[c*2],xy[c*2+1],0,0,color,1,0,0,0,0,0,0,0);batch(first,3,0,0,mode);
    }
    private void colorVertex(float x,float y,int c){float a=(c>>>24)/255f;vertices[used++]=x;vertices[used++]=y;vertices[used++]=(c>>16&255)/255f*a;vertices[used++]=(c>>8&255)/255f*a;vertices[used++]=(c&255)/255f*a;vertices[used++]=a;}
    public void shadedTriangle(float ax,float ay,int ac,float bx,float by,int bc,float cx,float cy,int cc){
        int first=used;if(!recording){space(18);colorVertex(ax,ay,ac);colorVertex(bx,by,bc);colorVertex(cx,cy,cc);batch(first,3,0,0,5);}
        else{vertex(ax,ay,0,0,ac,1,0,0,0,0,0,0,0);vertex(bx,by,0,0,bc,1,0,0,0,0,0,0,0);vertex(cx,cy,0,0,cc,1,0,0,0,0,0,0,0);batch(first,3,0,0,0);}
    }
    private final float[] convexScratch=new float[8];
    public void convexQuad(float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy,int color){
        if((color>>>24)==0)return;
        float[] q=convexScratch;q[0]=ax;q[1]=ay;q[2]=bx;q[3]=by;q[4]=cx;q[5]=cy;q[6]=dx;q[7]=dy;
        triangle(q,3,0,1,color,0);triangle(q,3,1,2,color,0);
    }
    public void poly(float[] xy,int color){
        int n=xy.length/2;if(n<3||(color>>>24)==0)return;int[] index=new int[n];double area=0;
        // Measure winding relative to the first vertex. Absolute screen products
        // cancel for thin distant ripples, changing their winding as zoom moves.
        for(int i=0;i<n;i++){index[i]=i;int j=(i+1)%n;
            area+=((double)xy[i*2]-xy[0])*((double)xy[j*2+1]-xy[1])
                    -((double)xy[j*2]-xy[0])*((double)xy[i*2+1]-xy[1]);}float sign=area<0?-1:1;int remaining=n,guard=n*n;
        while(remaining>=3 && guard-->0){boolean found=false;for(int i=0;i<remaining;i++){int a=index[(i+remaining-1)%remaining],b=index[i],c=index[(i+1)%remaining];if(cross(xy,a,b,c)*sign<=0)continue;boolean inside=false;for(int j=0;j<remaining;j++){int k=index[j];if(k!=a&&k!=b&&k!=c&&cross(xy,a,b,k)*sign>=0&&cross(xy,b,c,k)*sign>=0&&cross(xy,c,a,k)*sign>=0){inside=true;break;}}if(inside)continue;triangle(xy,a,b,c,color,0);System.arraycopy(index,i+1,index,i,remaining-i-1);remaining--;found=true;break;}if(!found)break;}
    }
    private static float cross(float[] p,int a,int b,int c){return (p[b*2]-p[a*2])*(p[c*2+1]-p[a*2+1])-(p[b*2+1]-p[a*2+1])*(p[c*2]-p[a*2]);}
    public boolean supportsUnionClips(){return true;}
    // Native commands already merge adjacent compatible primitives.
    public boolean prefersPolygonBatches(){return false;}
    private void mask(float[] triangles,int mode){for(int i=0;i+5<triangles.length;i+=6){int first=used;for(int j=0;j<6;j+=2)vertex(triangles[i+j],triangles[i+j+1],0,0,0xffffffff,1,0,0,0,0,0,0,0);batch(first,3,0,0,mode);}}
    /** Maximum overlap of triangle bounds along either axis conservatively bounds
     * actual pixel coverage, without confusing a large disjoint map with overlaps. */
    private int clipOverlapBound(float[] triangles){
        int n=triangles.length/6;if(n<=32)return n;
        if(clipStarts.length<n){clipStarts=new float[n];clipEnds=new float[n];}
        int bound=n;
        for(int axis=0;axis<2;axis++){
            for(int i=0;i<n;i++){int at=i*6+axis;float a=triangles[at],b=triangles[at+2],c=triangles[at+4];clipStarts[i]=Math.min(a,Math.min(b,c));clipEnds[i]=Math.max(a,Math.max(b,c));}
            Arrays.sort(clipStarts,0,n);Arrays.sort(clipEnds,0,n);
            int end=0,max=0;
            for(int start=0;start<n;start++){while(end<n && clipEnds[end]<clipStarts[start])end++;max=Math.max(max,start-end+1);}
            bound=Math.min(bound,max);
        }
        return bound;
    }
    public void pushClipOutside(float[] triangles){
        if(clipDepth>=clipFirst.length)throw new IllegalStateException("Clip nesting limit");
        // Stencil counts overlapping triangles, not parity. Reserve one count for
        // every remaining nesting level; extreme masks take the exact disjoint path.
        int coverage=clipOverlapBound(triangles);
        if(clipCoverageTotal+coverage>255-(clipFirst.length-clipDepth)){
            triangles=ClipUnion.disjoint(triangles);coverage=triangles.length==0?0:1;
        }
        clipCoverage[clipDepth]=coverage;clipCoverageTotal+=coverage;
        int first=used;batch(first,0,0,0,3);mask(triangles,1);
        clipFirst[clipDepth]=first;clipCount[clipDepth++]=(used-first)/STRIDE;
    }
    public void popClip(){
        if(clipDepth==0)throw new IllegalStateException("Clip underflow");
        // Vertex storage is append-only until present: replay the captured mask,
        // even if the caller has since changed its source array. Nested scopes
        // retain distinct ranges; retained meshes relocate both references.
        --clipDepth;clipCoverageTotal-=clipCoverage[clipDepth];batch(clipFirst[clipDepth],clipCount[clipDepth],0,0,2);
        batch(used,0,0,0,4);
    }
    private void patch(float x,float y,float ax,float ay,float bx,float by,int color,float alpha,int kind,float phase,float detail){quad(x-(ax+bx)*.5f,y-(ay+by)*.5f,ax,ay,bx,by,color,alpha,0,kind,phase,detail,0,0,1,1,0,0,0,0);}
    public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha){if(resources!=null && resources.profileFoamSkipped())return;patch(x,y,ax,ay,bx,by,0xffe5fcf7,alpha,kind>=8?kind+24:kind+2,phase,0);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){patch(x,y,ax,ay,bx,by,color,1,8,phase,0);}
    public void smokePuff(float x,float y,float radius,float phase,int color){patch(x,y,2*radius,0,0,2*radius,color,1,9,phase,0);}
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){patch(x,y,ax,ay,bx,by,color,1,11+kind,amount,detail);}
    private void face(float[] xy,float[] uv,int kind,int color,float phase,float detail){if((color>>>24)==0)return;int first=used;for(int i=2;i<xy.length-2;i+=2)for(int j=0;j<3;j++){int at=j==0?0:j==1?i:i+2;vertex(xy[at],xy[at+1],uv[at],uv[at+1],color,1,kind,phase,detail,0,0,0,0);}batch(first,(used-first)/STRIDE,0,0,0);}
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){if(material==SurfaceMaterial.EMISSIVE){poly(xy,color);return;}face(xy,uv,14+SurfaceMaterial.base(material),lightingProfile.surface(color,material),wetness,0);}
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){face(xy,uv,20+kind,color,phase,energy);}
    public void ropeSegment(float x,float y,float xx,float yy,float w,float from,float to,int color){float dx=xx-x,dy=yy-y,len=(float)StrictMath.hypot(dx,dy);if(len<=0||w<=0)return;float nx=-dy/len*w,ny=dx/len*w;quad(x-nx*.5f,y-ny*.5f,dx,dy,nx,ny,color,1,0,10,0,0,from,0,to,1,0,0,0,0);}
    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){Bitmap bitmap=resources.profileFrameImage(SpriteAtlas.MOORED_NORMALS,frame);int image=texture(((long)SpriteAtlas.MOORED_NORMALS<<32)|frame,bitmap);quad(x-w*.5f,y-h*.7f,w,0,0,h,color,alpha,image,30,0,0,0,0,1,1,nx,ny,nz,radius);}
    public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha){w*=SpriteAtlas.widthScale(atlas,frame);h*=SpriteAtlas.heightScale(atlas,frame);float c=(float)StrictMath.cos(rotation),s=(float)StrictMath.sin(rotation);spriteAffine(atlas,frame,x,y,w*c,w*s,-h*s,h*c,alpha);}
    public void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha){
        float px=SpriteAtlas.pivotX(atlas,frame),py=SpriteAtlas.pivotY(atlas,frame),left=x-ax*px-bx*py,top=y-ay*px-by*py;if(alpha<=0||!visible(left,top,ax,ay,bx,by))return;
        Bitmap bitmap=resources.profileFrameImage(atlas,frame);Rect src=resources.gpuSource(atlas,frame);float[] crop=resources.gpuCrop(atlas,frame);int image=texture(((long)atlas<<32)|frame,bitmap);
        quad(left+ax*crop[0]+bx*crop[1],top+ay*crop[0]+by*crop[1],ax*crop[2],ay*crop[2],bx*crop[3],by*crop[3],0xffffffff,alpha,image,0,0,0,src.left/(float)bitmap.getWidth(),src.top/(float)bitmap.getHeight(),src.right/(float)bitmap.getWidth(),src.bottom/(float)bitmap.getHeight(),0,0,0,0);
    }
    public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float rotation,float blend,float alpha){
        blend=Math.max(0,Math.min(1,blend));if(a==b||blend==0||blend==1){sprite(atlas,blend==1?b:a,x,y,w,h,rotation,alpha);return;}
        float aw=w*SpriteAtlas.widthScale(atlas,a),ah=h*SpriteAtlas.heightScale(atlas,a),bw=w*SpriteAtlas.widthScale(atlas,b),bh=h*SpriteAtlas.heightScale(atlas,b);
        float al=-SpriteAtlas.pivotX(atlas,a)*aw,at=-SpriteAtlas.pivotY(atlas,a)*ah,bl=-SpriteAtlas.pivotX(atlas,b)*bw,bt=-SpriteAtlas.pivotY(atlas,b)*bh;
        float left=Math.min(al,bl),top=Math.min(at,bt),right=Math.max(al+aw,bl+bw),bottom=Math.max(at+ah,bt+bh),c=(float)StrictMath.cos(rotation),s=(float)StrictMath.sin(rotation);
        if(alpha<=0 || !visible(x+c*left-s*top,y+s*left+c*top,c*(right-left),s*(right-left),-s*(bottom-top),c*(bottom-top)))return;
        Bitmap ba=resources.profileFrameImage(atlas,a),bb=resources.profileFrameImage(atlas,b);int ta=texture(((long)atlas<<32)|a,ba),tb=texture(((long)atlas<<32)|b,bb);
        // Rotational families are full cropped cells, with transparent authored margins.
        int first=used;for(int k:QUAD){float xx=(k&1)==0?left:right,yy=(k>>1)==0?top:bottom;vertex(x+c*xx-s*yy,y+s*xx+c*yy,(xx-al)/aw,(yy-at)/ah,0xffffffff,alpha,40,blend,0,(xx-bl)/bw,(yy-bt)/bh,0,0);}batch(first,6,ta,tb,0);
    }
    public boolean preparesLayers(){return true;}
    public void prepareLayer(StaticLayer layer,float scale){resources.prepareLayer(layer,scale);}
    public boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){
        Mesh mesh=meshes.get(layer.key());
        if(mesh==null){
            if(recorder==null)recorder=new VulkanPainter(resources);
            recorder.start(layer.width(),layer.height());
            layer.paint(recorder);
            if(recorder.clipDepth!=0)throw new IllegalStateException("Unbalanced retained clipping");
            mesh=new Mesh(recorder);
            for(int i=0;i<mesh.commands.length;i+=5)
                if(mesh.commands[i+2]!=0 || mesh.commands[i+3]!=0)throw new IllegalStateException("Retained surfaces must be texture-free");
            if(mesh.bytes()<=8L*1024*1024){
                meshes.put(layer.key(),mesh);meshBytes+=mesh.bytes();
                while(meshes.size()>96 || meshBytes>8L*1024*1024){
                    Iterator<Mesh> oldest=meshes.values().iterator();Mesh old=oldest.next();meshBytes-=old.bytes();oldest.remove();
                }
            }
        }
        int first=used;space(mesh.vertices.length);
        System.arraycopy(mesh.vertices,0,vertices,used,mesh.vertices.length);
        for(int i=used;i<used+mesh.vertices.length;i+=STRIDE){vertices[i]=x+vertices[i]*scale;vertices[i+1]=y+vertices[i+1]*scale;}
        used+=mesh.vertices.length;
        for(int i=0;i<mesh.commands.length;i+=5)batch(first+mesh.commands[i],mesh.commands[i+1],0,0,mesh.commands[i+4]);
        return true;
    }
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha){
        if(alpha<=0||!visible(x,y,layer.width()*scale,0,0,layer.height()*scale))return;
        Bitmap bitmap=resources.gpuLayer(layer,scale);
        if(bitmap==null){
            // Cold terrain must not expand into hundreds of thousands of primitives.
            // Keep the last complete frame while the existing bounded worker prepares it.
            // This also preserves group opacity without a hardware Canvas fallback.
            if(layer.backgroundSafe() || alpha<1){preparingLayers=true;return;}
            float left=Math.max(0,Math.min(width,x)),right=Math.max(left,Math.min(width,x+layer.width()*scale));
            float top=Math.max(0,Math.min(height,y)),bottom=Math.max(top,Math.min(height,y+layer.height()*scale));
            float[] outside=new float[48];int at=0;
            at=clipRect(outside,at,0,0,left,height);at=clipRect(outside,at,right,0,width,height);
            at=clipRect(outside,at,left,0,right,top);clipRect(outside,at,left,bottom,right,height);
            pushClipOutside(outside);try{layer.draw(this,x,y,scale,1);}finally{popClip();}return;
        }
        long key=Long.MIN_VALUE^((long)layer.key()<<32)^(Float.floatToIntBits(StaticLayer.rasterScale(scale))&0xffffffffL);
        int image=texture(key,bitmap);quad(x,y,layer.width()*scale,0,0,layer.height()*scale,0xffffffff,alpha,image,0,0,0,0,0,1,1,0,0,0,0);
    }
    private static int clipRect(float[] q,int at,float x,float y,float xx,float yy){
        float[] v={x,y,xx,y,x,yy,xx,y,xx,yy,x,yy};System.arraycopy(v,0,q,at,12);return at+12;
    }
    private static native long create(Surface surface,AssetManager assets,int experimentFlags);
    private static native void destroy(long handle);
    private static native void upload(long handle,int id,Bitmap bitmap);
    private static native void remove(long handle,int id);
    private static native void submit(long handle,ByteBuffer vertices,int bytes,ByteBuffer commands,int count,float w,float h,float scale,float x,float y);
}
