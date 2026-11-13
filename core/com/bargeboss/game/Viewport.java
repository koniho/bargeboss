package com.bargeboss.game;

/** Fit, never crop: all devices see the complete landscape board and controls. */
public final class Viewport {
    public final float scale, x, y;
    public Viewport(float width, float height) {
        scale = Math.max(.001f, Math.min(width / Layout.WIDTH, height / Layout.HEIGHT));
        x = (width - Layout.WIDTH * scale) * .5f;
        y = (height - Layout.HEIGHT * scale) * .5f;
    }
    public float worldX(float screenX) { return (screenX - x) / scale; }
    public float worldY(float screenY) { return (screenY - y) / scale; }
    public float zoomFactor() { return scale; }
    public float offsetX() { return x; }
    public float offsetY() { return y; }
}
