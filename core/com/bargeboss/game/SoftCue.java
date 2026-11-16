package com.bargeboss.game;

/** Quiet guidance: translucent feathered strokes, with width capped at close zoom. */
public final class SoftCue {
    private SoftCue(){}
    public static int alpha(int color,float amount){return ((int)((color>>>24)*Physics.clamp(amount,0,1))<<24)|(color&0xffffff);}
    public static float breath(int tick){return .72f+.28f*(.5f+.5f*(float)StrictMath.sin(tick*.045));}
    public static int animated(Game g,int color){return alpha(color,breath(g.visualTicks));}
    public static void line(Painter p,float x,float y,float xx,float yy,float width,int color) {
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        float core=Math.min(1.4f,width)/Math.max(1,zoom);
        p.line(x,y,xx,yy,core+5/zoom,alpha(color,.055f));
        p.line(x,y,xx,yy,core+2.5f/zoom,alpha(color,.11f));
        p.line(x,y,xx,yy,core,alpha(color,.52f));
    }
    /** Text belongs to screen overlays; world cues use shape, color and motion only. */
    public static void caption(Painter p,String text,float x,float y,float size,int color) {
        if(!(p instanceof RiverPainter))Lettering.center(p,text,x,y,size,alpha(color,.78f));
    }
    public static void dot(Painter p,float x,float y,float radius,int color) {
        p.material(x,y,radius*3.6f,0,0,radius*3.6f,EffectMaterial.DOT,0,1,color);
    }
    public static void ring(Painter p,float x,float y,float radius,float fraction,int color,float width) {
        if(radius<=0 || fraction<=0)return;
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        float core=Math.min(1.4f,width)/Math.max(1,zoom);
        ringBand(p,x,y,radius,fraction,core+5/zoom,alpha(color,.055f));
        ringBand(p,x,y,radius,fraction,core+2.5f/zoom,alpha(color,.11f));
        ringBand(p,x,y,radius,fraction,core,alpha(color,.52f));
    }
    private static void ringBand(Painter p,float x,float y,float radius,float fraction,float width,int color) {
        float diameter=(radius*2+width)/.98f;
        p.material(x,y,diameter,0,0,diameter,EffectMaterial.RING,width/diameter,Physics.clamp(fraction,0,1),color);
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);if(length<3)return;
        dx/=length;dy/=length;
        line(p,x,y,xx,yy,1.3f,color);
        float head=Math.min(10,length*.25f),side=head*.5f;
        line(p,xx-dx*head-dy*side,yy-dy*head+dx*side,xx,yy,1.3f,color);
        line(p,xx-dx*head+dy*side,yy-dy*head-dx*side,xx,yy,1.3f,color);
    }
}
