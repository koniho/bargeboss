package com.bargeboss.game;

/** Uniform pixels, responsive composition: the logical canvas covers every available pixel. */
public final class Viewport {
    public final float scale, x, y;
    private final float logicalWidth,logicalHeight;
    public Viewport(float width, float height) {
        scale = Math.max(.001f, Math.min(width / Layout.WIDTH, height / Layout.HEIGHT));
        x=0; y=0;
        logicalWidth=Math.max(Layout.WIDTH,width/scale);
        logicalHeight=Math.max(Layout.HEIGHT,height/scale);
    }
    public float worldX(float screenX) { return (screenX - x) / scale; }
    public float worldY(float screenY) { return (screenY - y) / scale; }
    public float logicalWidth() { return logicalWidth; }
    public float logicalHeight() { return logicalHeight; }
    public float zoomFactor() { return scale; }
    public float offsetX() { return x; }
    public float offsetY() { return y; }
}
