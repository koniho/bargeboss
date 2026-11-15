package com.bargeboss.game;

/** Uniform opacity for one scene object, including its animated parts. */
public final class OpacityPainter implements Painter {
    private final Painter p; private final float alpha;
    public OpacityPainter(Painter p,float alpha) { this.p=p; this.alpha=Physics.clamp(alpha,0,1); }
    private int color(int c) { return (Math.round((c>>>24)*alpha)<<24)|(c&0xffffff); }
    public void pushClipOutside(float[] triangles){p.pushClipOutside(triangles);}
    public void popClip(){p.popClip();}
    public void rect(float x,float y,float w,float h,int c) { p.rect(x,y,w,h,color(c)); }
    public void circle(float x,float y,float r,int c) { p.circle(x,y,r,color(c)); }
    public void poly(float[] xy,int c) { p.poly(xy,color(c)); }
    public void line(float x,float y,float xx,float yy,float w,int c) { p.line(x,y,xx,yy,w,color(c)); }
    public boolean prefersPolygonBatches(){return p.prefersPolygonBatches();}
    public boolean preparesLayers(){return p.preparesLayers();}
    public void prepareLayer(StaticLayer layer,float scale){p.prepareLayer(layer,scale);}
    public void layer(StaticLayer layer,float x,float y,float scale,float opacity){p.layer(layer,x,y,scale,opacity*alpha);}
    public void spriteAffine(int a,int f,float x,float y,float ax,float ay,float bx,float by,float opacity){p.spriteAffine(a,f,x,y,ax,ay,bx,by,opacity*alpha);}
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float opacity) { p.sprite(a,f,x,y,w,h,r,opacity*alpha); }
    public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float opacity) { p.spriteBlend(a,f,ff,x,y,w,h,r,blend,opacity*alpha); }
}
