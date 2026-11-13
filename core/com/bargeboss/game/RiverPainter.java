package com.bargeboss.game;

/** Fits the complete river above the controls without changing its physics coordinates. */
public final class RiverPainter implements Painter {
    public static final float SCALE=.82f, LEFT=54, TOP=148;
    private final Painter painter;
    public RiverPainter(Painter painter) { this.painter=painter; }
    public static float screenX(float x) { return LEFT+x*SCALE; }
    public static float screenY(float y) { return TOP+(y-TOP)*SCALE; }
    public static float riverX(float x) { return (x-LEFT)/SCALE; }
    public static float riverY(float y) { return TOP+(y-TOP)/SCALE; }
    public void rect(float x,float y,float w,float h,int c) { painter.rect(screenX(x),screenY(y),w*SCALE,h*SCALE,c); }
    public void circle(float x,float y,float r,int c) { painter.circle(screenX(x),screenY(y),r*SCALE,c); }
    public void poly(float[] xy,int c) {
        float[] result=new float[xy.length];
        for(int i=0;i<xy.length;i+=2) { result[i]=screenX(xy[i]); result[i+1]=screenY(xy[i+1]); }
        painter.poly(result,c);
    }
    public void line(float x,float y,float xx,float yy,float w,int c) { painter.line(screenX(x),screenY(y),screenX(xx),screenY(yy),w*SCALE,c); }
}
