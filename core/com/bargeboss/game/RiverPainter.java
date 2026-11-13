package com.bargeboss.game;

/** Rotate the full river into a landscape board; physics keeps its world axes. */
public final class RiverPainter implements Painter {
    public static final float SCALE=960f/688, LEFT=0, TOP=(600-320*RiverCamera.DEPTH*SCALE)/2;
    public static final float WORLD_LEFT=140, WORLD_RIGHT=460, WORLD_TOP=148, WORLD_BOTTOM=836;
    private final Painter painter;
    public final RiverCamera camera;
    public RiverPainter(Painter painter) { this(painter,RiverCamera.DEFAULT); }
    public RiverPainter(Painter painter,RiverCamera camera) { this.painter=painter; this.camera=camera; }
    public static float screenX(float x,float y) { return LEFT+(y-WORLD_TOP)*SCALE; }
    public static float screenY(float x,float y) { return TOP+(WORLD_RIGHT-x)*SCALE*RiverCamera.DEPTH; }
    public static float riverX(float x,float y) { return WORLD_RIGHT-(y-TOP)/(SCALE*RiverCamera.DEPTH); }
    public static float riverY(float x,float y) { return WORLD_TOP+(x-LEFT)/SCALE; }
    public static boolean inBoard(float x,float y) {
        return x>=LEFT && x<=screenX(WORLD_RIGHT,WORLD_BOTTOM) && y>=TOP && y<=screenY(WORLD_LEFT,WORLD_BOTTOM);
    }
    public void rect(float x,float y,float w,float h,int c) {
        painter.rect(camera.screenX(x+w,y),camera.screenY(x+w,y),h*camera.scale,w*camera.scale*RiverCamera.DEPTH,c);
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
    public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        painter.sprite(atlas,(frame+24)%32,camera.screenX(cx,cy),camera.screenY(cx,cy),width*camera.scale,height*camera.scale,rotation,alpha);
    }
    public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
        painter.spriteBlend(atlas,(frameA+24)%32,(frameB+24)%32,camera.screenX(cx,cy),camera.screenY(cx,cy),
                width*camera.scale,height*camera.scale,rotation,blend,alpha);
    }
    /** UI labels stay upright instead of inheriting the world's quarter-turn. */
    public static void center(Painter p,String value,float x,float y,float size,int color) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;
            Lettering.center(river.painter,value,river.camera.screenX(x,y),river.camera.screenY(x,y),size*river.camera.scale,color);
        } else Lettering.center(p,value,x,y,size,color);
    }
    public static void text(Painter p,String value,float x,float y,float size,int color) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;
            Lettering.draw(river.painter,value,river.camera.screenX(x,y),river.camera.screenY(x,y),size*river.camera.scale,color);
        } else Lettering.draw(p,value,x,y,size,color);
    }
    public static void centerText(Painter p,String value,float x,float y,float size,int color) { center(p,value,x,y,size,color); }
    public static void drawText(Painter p,String value,float x,float y,float size,int color) { text(p,value,x,y,size,color); }
}
