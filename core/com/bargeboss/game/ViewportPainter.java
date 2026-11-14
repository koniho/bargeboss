package com.bargeboss.game;

/** Reject invisible submissions before crossing the native drawing boundary.
 * Keeps intersecting primitives intact: hosts still do their usual edge clipping. */
public final class ViewportPainter implements Painter {
    private final Painter target;
    private final float width,height;
    public ViewportPainter(Painter target,float width,float height) {this.target=target;this.width=width;this.height=height;}
    public void beginFrame(){target.beginFrame();}
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
        for(int i=0;i<xy.length;i+=2){left=Math.min(left,xy[i]);right=Math.max(right,xy[i]);top=Math.min(top,xy[i+1]);bottom=Math.max(bottom,xy[i+1]);}
        if(visible(left,top,right,bottom))target.poly(xy,color);
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
    public void sprite(int atlas,int frame,float x,float y,float w,float h,float r,float alpha) {
        if(alpha>0 && spriteVisible(atlas,frame,x,y,w,h,r))target.sprite(atlas,frame,x,y,w,h,r,alpha);
    }
    public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float r,float blend,float alpha) {
        if(alpha>0 && (spriteVisible(atlas,a,x,y,w,h,r) || spriteVisible(atlas,b,x,y,w,h,r)))
            target.spriteBlend(atlas,a,b,x,y,w,h,r,blend,alpha);
    }
}
