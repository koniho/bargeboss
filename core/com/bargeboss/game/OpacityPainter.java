package com.bargeboss.game;

/** Uniform opacity for one scene object, including its animated parts. */
public final class OpacityPainter implements Painter {
    private final Painter p; private final float alpha;
    public OpacityPainter(Painter p,float alpha) { this.p=p; this.alpha=Physics.clamp(alpha,0,1); }
    private int color(int c) { return (Math.round((c>>>24)*alpha)<<24)|(c&0xffffff); }
    public void rect(float x,float y,float w,float h,int c) { p.rect(x,y,w,h,color(c)); }
    public void circle(float x,float y,float r,int c) { p.circle(x,y,r,color(c)); }
    public void poly(float[] xy,int c) { p.poly(xy,color(c)); }
    public void line(float x,float y,float xx,float yy,float w,int c) { p.line(x,y,xx,yy,w,color(c)); }
    public void sprite(int a,int f,float x,float y,float w,float h,float r,float opacity) { p.sprite(a,f,x,y,w,h,r,opacity*alpha); }
    public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float opacity) { p.spriteBlend(a,f,ff,x,y,w,h,r,blend,opacity*alpha); }
}
