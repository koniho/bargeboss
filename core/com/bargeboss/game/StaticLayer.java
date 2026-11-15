package com.bargeboss.game;

/** Immutable local drawing content. Hosts may rasterize and retain it by key.
 * Native layer draws clip to [0,width]x[0,height] and apply alpha to the whole group.
 * The default expansion exists for deterministic traces and non-raster painters. */
public abstract class StaticLayer {
    /** Explicit opt-in: paint uses only immutable commands and sprite metadata. */
    public boolean backgroundSafe(){return false;}
    public abstract int key();
    public abstract float width();
    public abstract float height();
    public abstract void paint(Painter painter);
    public static float rasterScale(float displayScale){return displayScale>1.6f?3:2;}
    public static int cacheBudgetBytes(){return 48*1024*1024;}
    public static int maxBuildsPerFrame(){return 1;}
    public final void draw(Painter painter,float x,float y,float scale,float alpha) {
        paint(ScreenView.scaled(alpha==1?painter:new OpacityPainter(painter,alpha),x,y,scale));
    }
}
