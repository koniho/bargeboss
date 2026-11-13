package com.bargeboss.game;

/** Immutable world projection, independent of HUD layout and simulation state. */
public final class RiverCamera {
    public static final float DEPTH=.75f;
    public static final RiverCamera DEFAULT=new RiverCamera(0,(600-320*DEPTH*(960f/688))/2,960f/688);
    public final float left,top,scale,worldTop,worldRight,zoom;
    public RiverCamera(float left,float top,float scale) {
        this(left,top,scale,148,460,1);
    }
    public RiverCamera(float left,float top,float scale,float worldTop,float worldRight,float zoom) {
        this.left=left;this.top=top;this.scale=Math.max(.001f,scale);this.worldTop=worldTop;this.worldRight=worldRight;this.zoom=zoom;
    }
    public float screenX(float x,float y) { return left+(y-worldTop)*scale; }
    public float screenY(float x,float y) { return top+(worldRight-x)*scale*DEPTH; }
    public float worldX(float x,float y) { return worldRight-(y-top)/(scale*DEPTH); }
    public float worldY(float x,float y) { return worldTop+(x-left)/scale; }
    public float width() { return 688*scale/zoom; }
    public float height() { return 320*scale*DEPTH/zoom; }
    public boolean contains(float x,float y) { return x>=left && x<=left+width() && y>=top && y<=top+height(); }
}
