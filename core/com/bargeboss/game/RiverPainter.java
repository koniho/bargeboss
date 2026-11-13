package com.bargeboss.game;

/** Rotate the full river into a landscape board; physics keeps its world axes. */
public final class RiverPainter implements Painter {
    public static final float SCALE=1.35f, LEFT=16, TOP=84;
    public static final float WORLD_LEFT=140, WORLD_RIGHT=460, WORLD_TOP=148, WORLD_BOTTOM=836;
    private final Painter painter;
    public RiverPainter(Painter painter) { this.painter=painter; }
    public static float screenX(float x,float y) { return LEFT+(y-WORLD_TOP)*SCALE; }
    public static float screenY(float x,float y) { return TOP+(WORLD_RIGHT-x)*SCALE; }
    public static float riverX(float x,float y) { return WORLD_RIGHT-(y-TOP)/SCALE; }
    public static float riverY(float x,float y) { return WORLD_TOP+(x-LEFT)/SCALE; }
    public static boolean inBoard(float x,float y) {
        return x>=LEFT && x<=screenX(WORLD_RIGHT,WORLD_BOTTOM) && y>=TOP && y<=screenY(WORLD_LEFT,WORLD_BOTTOM);
    }
    public void rect(float x,float y,float w,float h,int c) {
        painter.rect(screenX(x+w,y),screenY(x+w,y),h*SCALE,w*SCALE,c);
    }
    public void circle(float x,float y,float r,int c) { painter.circle(screenX(x,y),screenY(x,y),r*SCALE,c); }
    public void poly(float[] xy,int c) {
        float[] result=new float[xy.length];
        for(int i=0;i<xy.length;i+=2) { result[i]=screenX(xy[i],xy[i+1]); result[i+1]=screenY(xy[i],xy[i+1]); }
        painter.poly(result,c);
    }
    public void line(float x,float y,float xx,float yy,float w,int c) {
        painter.line(screenX(x,y),screenY(x,y),screenX(xx,yy),screenY(xx,yy),w*SCALE,c);
    }
    public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        painter.sprite(atlas,(frame+6)%8,screenX(cx,cy),screenY(cx,cy),width*SCALE,height*SCALE,rotation,alpha);
    }
    /** UI labels stay upright instead of inheriting the world's quarter-turn. */
    public static void center(Painter p,String value,float x,float y,float size,int color) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;
            Lettering.center(river.painter,value,screenX(x,y),screenY(x,y),size*SCALE,color);
        } else Lettering.center(p,value,x,y,size,color);
    }
    public static void text(Painter p,String value,float x,float y,float size,int color) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;
            Lettering.draw(river.painter,value,screenX(x,y),screenY(x,y),size*SCALE,color);
        } else Lettering.draw(p,value,x,y,size,color);
    }
    public static void centerText(Painter p,String value,float x,float y,float size,int color) { center(p,value,x,y,size,color); }
    public static void drawText(Painter p,String value,float x,float y,float size,int color) { text(p,value,x,y,size,color); }
}
