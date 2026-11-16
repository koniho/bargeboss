package com.bargeboss.game;

/** Project the ground plane; upright sprites and labels keep their source perspective. */
public final class RiverPainter implements Painter {
    public static final float SCALE=RiverCamera.DEFAULT.scale, LEFT=RiverCamera.DEFAULT.left, TOP=RiverCamera.DEFAULT.top;
    public static final float WORLD_LEFT=140, WORLD_RIGHT=460, WORLD_TOP=148, WORLD_BOTTOM=836;
    private final Painter painter;
    public final RiverCamera camera;
    public RiverPainter(Painter painter) { this(painter,RiverCamera.DEFAULT); }
    public RiverPainter(Painter painter,RiverCamera camera) { this.painter=painter; this.camera=camera; }
    public static float screenX(float x,float y) { return RiverCamera.DEFAULT.screenX(x,y); }
    public static float screenY(float x,float y) { return RiverCamera.DEFAULT.screenY(x,y); }
    public static float riverX(float x,float y) { return RiverCamera.DEFAULT.worldX(x,y); }
    public static float riverY(float x,float y) { return RiverCamera.DEFAULT.worldY(x,y); }
    public static boolean inBoard(float x,float y) {
        float wx=riverX(x,y),wy=riverY(x,y);
        return wx>=WORLD_LEFT && wx<=WORLD_RIGHT && wy>=WORLD_TOP && wy<=WORLD_BOTTOM;
    }
    public void pushClipOutside(float[] points) {
        float[] projected=new float[points.length];
        for(int i=0;i<points.length;i+=2){projected[i]=camera.screenX(points[i],points[i+1]);projected[i+1]=camera.screenY(points[i],points[i+1]);}
        painter.pushClipOutside(projected);
    }
    public boolean prefersPolygonBatches(){return painter.prefersPolygonBatches();}
    public void popClip(){painter.popClip();}
    public void rect(float x,float y,float w,float h,int c) {
        poly(new float[]{x,y,x+w,y,x+w,y+h,x,y+h},c);
    }
    public Painter canvas() { return painter; }
    public void circle(float x,float y,float r,int c) {
        float[] points=new float[40];
        for(int i=0;i<20;i++) { double a=i*Math.PI/10;
            points[i*2]=x+r*(float)StrictMath.cos(a); points[i*2+1]=y+r*(float)StrictMath.sin(a); }
        poly(points,c);
    }
    public void poly(float[] xy,int c) {
        float[] result=new float[xy.length];
        for(int i=0;i<xy.length;i+=2) { result[i]=camera.screenX(xy[i],xy[i+1]); result[i+1]=camera.screenY(xy[i],xy[i+1]); }
        painter.poly(result,c);
    }
    public void line(float x,float y,float xx,float yy,float w,int c) {
        painter.line(camera.screenX(x,y),camera.screenY(x,y),camera.screenX(xx,yy),camera.screenY(xx,yy),w*camera.scale,c);
    }
    public void waterFoam(float x,float y,float ax,float ay,float bx,float by,float phase,int kind,float alpha) {
        float s=camera.scale;
        painter.waterFoam(camera.screenX(x,y),camera.screenY(x,y),RiverCamera.Q*(ax+ay)*s,RiverCamera.Q*RiverCamera.DEPTH*(ay-ax)*s,RiverCamera.Q*(bx+by)*s,RiverCamera.Q*RiverCamera.DEPTH*(by-bx)*s,phase,kind,alpha);
    }
    public void shoreline(float[] xy,float[] uv,float phase,float energy,int kind,int color){
        float[] projected=new float[xy.length];for(int i=0;i<xy.length;i+=2){projected[i]=camera.screenX(xy[i],xy[i+1]);projected[i+1]=camera.screenY(xy[i],xy[i+1]);}
        painter.shoreline(projected,uv,phase,energy,kind,color);
    }
    public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
        float[] projected=new float[xy.length];for(int i=0;i<xy.length;i+=2){projected[i]=camera.screenX(xy[i],xy[i+1]);projected[i+1]=camera.screenY(xy[i],xy[i+1]);}
        painter.surfacePolygon(projected,uv,material,color,wetness);
    }
    public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){float s=camera.scale;painter.material(camera.screenX(x,y),camera.screenY(x,y),RiverCamera.Q*(ax+ay)*s,RiverCamera.Q*RiverCamera.DEPTH*(ay-ax)*s,RiverCamera.Q*(bx+by)*s,RiverCamera.Q*RiverCamera.DEPTH*(by-bx)*s,kind,detail,amount,color);}
    public void waterReflection(float x,float y,float ax,float ay,float bx,float by,float phase,int color){float s=camera.scale;painter.waterReflection(camera.screenX(x,y),camera.screenY(x,y),RiverCamera.Q*(ax+ay)*s,RiverCamera.Q*RiverCamera.DEPTH*(ay-ax)*s,RiverCamera.Q*(bx+by)*s,RiverCamera.Q*RiverCamera.DEPTH*(by-bx)*s,phase,color);}
    public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color){painter.ropeSegment(camera.screenX(x,y),camera.screenY(x,y),camera.screenX(xx,yy),camera.screenY(xx,yy),width*camera.scale,from,to,color);}
    public void smokePuff(float x,float y,float radius,float phase,int color){painter.smokePuff(camera.screenX(x,y),camera.screenY(x,y),radius*camera.scale,phase,color);}
    public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float alpha){
        float s=camera.scale;
        painter.spriteAffine(a,f,camera.screenX(x,y),camera.screenY(x,y),RiverCamera.Q*(ax+ay)*s,RiverCamera.Q*RiverCamera.DEPTH*(ay-ax)*s,RiverCamera.Q*(bx+by)*s,RiverCamera.Q*RiverCamera.DEPTH*(by-bx)*s,alpha);
    }
    public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        painter.sprite(atlas,(frame+RiverCamera.DIRECTION_OFFSET)%32,camera.screenX(cx,cy),camera.screenY(cx,cy),width*camera.scale,height*camera.scale,rotation,alpha);
    }
    public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
        painter.spriteBlend(atlas,(frameA+RiverCamera.DIRECTION_OFFSET)%32,(frameB+RiverCamera.DIRECTION_OFFSET)%32,camera.screenX(cx,cy),camera.screenY(cx,cy),
                width*camera.scale,height*camera.scale,rotation,blend,alpha);
    }
    /** World rendering has no text. Screen-overlay callers retain authored lettering. */
    public static void center(Painter p,String value,float x,float y,float size,int color) {
        if(!(p instanceof RiverPainter))Lettering.center(p,value,x,y,size,color);
    }
    public static void text(Painter p,String value,float x,float y,float size,int color) {
        if(!(p instanceof RiverPainter))Lettering.draw(p,value,x,y,size,color);
    }
    public static void centerText(Painter p,String value,float x,float y,float size,int color) { center(p,value,x,y,size,color); }
    public static void drawText(Painter p,String value,float x,float y,float size,int color) { text(p,value,x,y,size,color); }
}
