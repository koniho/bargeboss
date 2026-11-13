package com.bargeboss.game;

/** The entire native drawing boundary. Coordinates are in the shared 600 x 960 stage. */
public interface Painter {
    void rect(float x, float y, float width, float height, int argb);
    void circle(float x, float y, float radius, int argb);
    void poly(float[] xy, int argb);
    void line(float x1, float y1, float x2, float y2, float width, int argb);
}
