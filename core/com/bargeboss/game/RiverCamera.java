package com.bargeboss.game;

/** Immutable world projection, independent of HUD layout and simulation state. */
public final class RiverCamera {
    public static final RiverCamera DEFAULT=new RiverCamera(0,(600-320*(960f/688))/2,960f/688);
    public final float left,top,scale;
    public RiverCamera(float left,float top,float scale) {
        this.left=left; this.top=top; this.scale=Math.max(.001f,scale);
    }
    public float screenX(float x,float y) { return left+(y-RiverPainter.WORLD_TOP)*scale; }
    public float screenY(float x,float y) { return top+(RiverPainter.WORLD_RIGHT-x)*scale; }
    public float worldX(float x,float y) { return RiverPainter.WORLD_RIGHT-(y-top)/scale; }
    public float worldY(float x,float y) { return RiverPainter.WORLD_TOP+(x-left)/scale; }
    public float width() { return (RiverPainter.WORLD_BOTTOM-RiverPainter.WORLD_TOP)*scale; }
    public float height() { return (RiverPainter.WORLD_RIGHT-RiverPainter.WORLD_LEFT)*scale; }
    public boolean contains(float x,float y) { return x>=left && x<=left+width() && y>=top && y<=top+height(); }
}
