package com.bargeboss.game;

/** The entire native drawing boundary. Coordinates are in the shared landscape stage. */
public interface Painter {
    /** Draw one shared atlas cell around its normalized hull pivot, then rotate in radians. */
    void sprite(int atlas, int frame, float cx, float cy, float width, float height, float rotation, float alpha);
    /** Additively blend registered atlas cells in a transparent layer, then composite once. */
    void spriteBlend(int atlas, int frameA, int frameB, float cx, float cy, float width, float height,
                     float rotation, float blend, float alpha);
    void rect(float x, float y, float width, float height, int argb);
    void circle(float x, float y, float radius, int argb);
    void poly(float[] xy, int argb);
    void line(float x1, float y1, float x2, float y2, float width, int argb);
}
