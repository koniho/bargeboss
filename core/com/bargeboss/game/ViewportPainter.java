package com.bargeboss.game;

/** Reject invisible submissions before crossing the native drawing boundary.
 * Keeps intersecting primitives intact: hosts still do their usual edge clipping. */
public final class ViewportPainter implements Painter {
    private final Painter target;
    private final float width,height;
    public ViewportPainter(Painter target,float width,float height) {this.target=target;this.width=width;this.height=height;}
    public void pushClipOutside(float[] triangles){target.pushClipOutside(triangles);}
    public void popClip(){target.popClip();}
    public void beginFrame(){target.beginFrame();}
    public boolean supportsUnionClips(){return target.supportsUnionClips();}
    public boolean prefersPolygonBatches(){return target.prefersPolygonBatches();}
    public boolean retainedSurfaces(StaticLayer layer,float x,float y,float scale){return target.retainedSurfaces(layer,x,y,scale);}
    public boolean preparesLayers(){return target.preparesLayers();}
    public void prepareLayer(StaticLayer layer,float scale){target.prepareLayer(layer,scale);}
    public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
        if(alpha>0 && visible(x,y,x+layer.width()*scale,y+layer.height()*scale))target.layer(layer,x,y,scale,alpha);
    }
    private boolean visible(float left,float top,float right,float bottom) {
        // One logical pixel retains antialiased coverage just outside an edge.
        return right>=-1 && bottom>=-1 && left<=width+1 && top<=height+1;
    }
    public void rect(float x,float y,float w,float h,int color) {
        if((color>>>24)!=0 && visible(Math.min(x,x+w),Math.min(y,y+h),Math.max(x,x+w),Math.max(y,y+h)))target.rect(x,y,w,h,color);
    }
    public void circle(float x,float y,float r,int color) {
        if(r>0 && (color>>>24)!=0 && visible(x-r,y-r,x+r,y+r))target.circle(x,y,r,color);
    }
    public void line(float x,float y,float xx,float yy,float w,int color) {
        float r=Math.abs(w)*.5f;
        if((color>>>24)!=0 && visible(Math.min(x,xx)-r,Math.min(y,yy)-r,Math.max(x,xx)+r,Math.max(y,yy)+r))target.line(x,y,xx,yy,w,color);
    }
    public void poly(float[] xy,int color) {
        if((color>>>24)==0 || xy.length<6)return;
        float left=Float.MAX_VALUE,top=Float.MAX_VALUE,right=-Float.MAX_VALUE,bottom=-Float.MAX_VALUE;
        for(int i=0;i<xy.length;i+=2){
            float x=xy[i],y=xy[i+1];if(x!=x || y!=y)return;
            if(x<left)left=x;if(x>right)right=x;if(y<top)top=y;if(y>bottom)bottom=y;
        }
        if(visible(left,top,right,bottom))target.poly(xy,color);
    }
    public void polygons(float[][] xy,int count,int color) {
        // WaterBatchPainter already culls individual members; native viewport clipping remains active.
        if((color>>>24)!=0 && count>0)target.polygons(xy,count,color);
    }
    private boolean spriteVisible(int atlas,int frame,float x,float y,float w,float h,float rotation) {
        w*=SpriteAtlas.widthScale(atlas,frame);h*=SpriteAtlas.heightScale(atlas,frame);
        float left=-SpriteAtlas.pivotX(atlas,frame)*w,top=-SpriteAtlas.pivotY(atlas,frame)*h;
        if(rotation==0)return visible(x+left,y+top,x+left+w,y+top+h);
        float c=(float)StrictMath.cos(rotation),s=(float)StrictMath.sin(rotation);
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int i=0;i<4;i++) {float dx=left+(i%2)*w,dy=top+(i/2)*h;
            float sx=x+c*dx-s*dy,sy=y+s*dx+c*dy;
            minX=Math.min(minX,sx);maxX=Math.max(maxX,sx);minY=Math.min(minY,sy);maxY=Math.max(maxY,sy);}
        return visible(minX,minY,maxX,maxY);
    }
    public void beginSurfaceBatch(){target.beginSurfaceBatch();}
    public void endSurfaceBatch(){target.endSurfaceBatch();}
    public void beginWaterFoamBatch(){target.beginWaterFoamBatch();}
    public void endWaterFoamBatch(){target.endWaterFoamBatch();}
    public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        float w=(Math.abs(ax)+Math.abs(bx))*.5f,h=(Math.abs(ay)+Math.abs(by))*.5f;
        if(alpha>.005f && visible(x-w,y-h,x+w,y+h))target.waterFoam(x,y,ax,ay,bx,by,phase,kind,alpha);
    }
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){
        if((color>>>24)==0 || xy.length<6 || xy.length!=uv.length)return;
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<xy.length;i+=2){if(!Float.isFinite(xy[i]) || !Float.isFinite(xy[i+1]))return;l=Math.min(l,xy[i]);r=Math.max(r,xy[i]);t=Math.min(t,xy[i+1]);b=Math.max(b,xy[i+1]);}
        if(visible(l,t,r,b))target.shoreline(xy,uv,phase,energy,kind,color);
    }
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
        if((color>>>24)==0 || xy.length<6 || xy.length!=uv.length)return;
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<xy.length;i+=2){if(!Float.isFinite(xy[i]) || !Float.isFinite(xy[i+1]))return;l=Math.min(l,xy[i]);r=Math.max(r,xy[i]);t=Math.min(t,xy[i+1]);b=Math.max(b,xy[i+1]);}
        if(visible(l,t,r,b))target.surfacePolygon(xy,uv,material,color,wetness);
    }
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){float w=(Math.abs(ax)+Math.abs(bx))*.5f,h=(Math.abs(ay)+Math.abs(by))*.5f;if((color>>>24)>0 && visible(x-w,y-h,x+w,y+h))target.material(x,y,ax,ay,bx,by,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){float w=(Math.abs(ax)+Math.abs(bx))*.5f,h=(Math.abs(ay)+Math.abs(by))*.5f;if((color>>>24)>0 && visible(x-w,y-h,x+w,y+h))target.waterReflection(x,y,ax,ay,bx,by,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){if(visible(Math.min(x,xx)-width,Math.min(y,yy)-width,Math.max(x,xx)+width,Math.max(y,yy)+width))target.ropeSegment(x,y,xx,yy,width,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){if(radius>0 && (color>>>24)>0 && visible(x-radius,y-radius,x+radius,y+radius))target.smokePuff(x,y,radius,phase,color);}
    public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        if(alpha<=0)return;
        float px=SpriteAtlas.pivotX(a,f),py=SpriteAtlas.pivotY(a,f),l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<4;i++){float u=i%2-px,v=i/2-py,xx=x+ax*u+bx*v,yy=y+ay*u+by*v;l=Math.min(l,xx);r=Math.max(r,xx);t=Math.min(t,yy);b=Math.max(b,yy);}
        if(visible(l,t,r,b))target.spriteAffine(a,f,x,y,ax,ay,bx,by,alpha);
    }
    public void normalSprite(int frame,float x,float y,float w,float h,float nx,float ny,float nz,float radius,int color,float alpha){if(alpha>0 && spriteVisible(SpriteAtlas.MOORED_NORMALS,frame,x,y,w,h,0))target.normalSprite(frame,x,y,w,h,nx,ny,nz,radius,color,alpha);}
    public void sprite(int atlas,int frame,float x,float y,float w,float h,float r,float alpha) {
        if(alpha>0 && spriteVisible(atlas,frame,x,y,w,h,r))target.sprite(atlas,frame,x,y,w,h,r,alpha);
    }
    public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float r,float blend,float alpha) {
        if(alpha>0 && (spriteVisible(atlas,a,x,y,w,h,r) || spriteVisible(atlas,b,x,y,w,h,r)))
            target.spriteBlend(atlas,a,b,x,y,w,h,r,blend,alpha);
    }
}
