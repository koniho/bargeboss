package com.bargeboss.game;

/** The entire native drawing boundary. Coordinates are in the shared landscape stage. */
public interface Painter {
    /** Native cache bookkeeping only; never advances simulation. */
    default void beginFrame() {}
    /** Immutable static content, clipped to layer bounds with group opacity. */
    default void layer(StaticLayer layer,float x,float y,float scale,float alpha) {layer.draw(this,x,y,scale,alpha);}
    /** Affine cell basis, already scaled by caller; normalized atlas pivot stays at x,y.
     * Native hosts draw the image. Diagnostic painters may use its projected bounds. */
    default void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        float px=SpriteAtlas.pivotX(atlas,frame),py=SpriteAtlas.pivotY(atlas,frame);
        float[] q=new float[8];int[] corners={0,1,3,2};
        for(int i=0;i<4;i++){float u=corners[i]%2-px,v=corners[i]/2-py;q[i*2]=x+ax*u+bx*v;q[i*2+1]=y+ay*u+by*v;}
        poly(q,((int)(255*alpha)<<24)|0x020913);
    }
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
