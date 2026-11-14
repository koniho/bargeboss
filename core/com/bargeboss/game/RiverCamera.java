package com.bargeboss.game;

/** Shared orthographic camera: 45 degree ground yaw, 35.264 degree elevation. */
public final class RiverCamera {
    public static final float Q=.70710678f, DEPTH=.57735027f;
    public static final int DIRECTION_OFFSET=28;
    public static final float BOARD_WIDTH=1008*Q, BOARD_HEIGHT=1008*Q*DEPTH;
    public static final float DEFAULT_SCALE=Math.min(960/BOARD_WIDTH,496/BOARD_HEIGHT);
    public static final RiverCamera DEFAULT=new RiverCamera((960-BOARD_WIDTH*DEFAULT_SCALE)/2,
            (600-BOARD_HEIGHT*DEFAULT_SCALE)/2,DEFAULT_SCALE);
    public final float left,top,scale,worldTop,worldRight,zoom;
    public RiverCamera(float left,float top,float scale) { this(left,top,scale,148,460,1); }
    public RiverCamera(float left,float top,float scale,float worldTop,float worldRight,float zoom) {
        this.left=left;this.top=top;this.scale=Math.max(.001f,scale);this.worldTop=worldTop;this.worldRight=worldRight;this.zoom=zoom;
    }
    public float screenX(float x,float y) { return left+Q*((x-worldRight)+(y-worldTop)+320)*scale; }
    public float screenY(float x,float y) { return top+Q*DEPTH*((y-worldTop)-(x-worldRight))*scale; }
    public float worldX(float x,float y) { return worldRight+((x-left)/(Q*scale)-320-(y-top)/(Q*DEPTH*scale))*.5f; }
    public float worldY(float x,float y) { return worldTop+((x-left)/(Q*scale)-320+(y-top)/(Q*DEPTH*scale))*.5f; }
    public float width() { return BOARD_WIDTH*scale/zoom; }
    public float height() { return BOARD_HEIGHT*scale/zoom; }
    public boolean contains(float x,float y) { return x>=0 && x<=2*left+width() && y>=0 && y<=2*top+height(); }
    /** World offsets that lift an upright sprite without drifting sideways. */
    public static float riseX(float height) { return height/(2*Q*DEPTH); }
    public static float riseY(float height) { return -riseX(height); }
}
