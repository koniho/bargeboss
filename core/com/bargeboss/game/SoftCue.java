package com.bargeboss.game;

/** Shared arcade guidance; open centers, bounded strokes and simulation-timed motion. */
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
    /** Four inward chevrons breathe around an objective; progress remains a true continuous arc. */
    public static void target(Game g,Painter p,float x,float y,float radius,float fraction,int color,float width) {
        ring(p,x,y,radius,fraction,animated(g,color),width);
        if(fraction<.999f)return;
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        float phase=(g.visualTicks%90)/90f;
        float inset=(3+4*(float)StrictMath.cos(phase*StrictMath.PI*2))/Math.max(1,zoom);
        float r=radius+8/Math.max(1,zoom)+inset,h=5/Math.max(1,zoom);
        int ink=alpha(color,.62f);
        for(int i=0;i<4;i++){
            float dx=i==0?1:i==2?-1:0,dy=i==1?1:i==3?-1:0;
            float xx=x+dx*r,yy=y+dy*r;
            p.line(xx+dx*h-dy*h,yy+dy*h+dx*h,xx,yy,1.8f/Math.max(1,zoom),ink);
            p.line(xx+dx*h+dy*h,yy+dy*h-dx*h,xx,yy,1.8f/Math.max(1,zoom),ink);
        }
    }
    /** Short outward sparkle accents only for confirmed actions; no persistent vessel halo. */
    public static void burst(Painter p,float x,float y,float radius,float t,int color) {
        if(t<0 || t>=1)return;
        float ease=1-(1-t)*(1-t),r=radius+ease*28;
        int ink=alpha(color,(1-t)*(1-t));
        for(int i=0;i<8;i++){
            double a=i*StrictMath.PI/4;
            float dx=(float)StrictMath.cos(a),dy=(float)StrictMath.sin(a);
            float len=(i%2==0?12:7)*(1-t);
            line(p,x+dx*r,y+dy*r,x+dx*(r+len),y+dy*(r+len),2,ink);
        }
    }
    private static void ringBand(Painter p,float x,float y,float radius,float fraction,float width,int color) {
        float diameter=(radius*2+width)/.98f;
        p.material(x,y,diameter,0,0,diameter,EffectMaterial.RING,width/diameter,Physics.clamp(fraction,0,1),color);
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        if(WorldArrows.capture(p,x,y,xx,yy,color))return;
        arrowPlane(p,x,y,xx,yy,color);
    }
    static void arrowPlane(Painter p,float x,float y,float xx,float yy,int color) {
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);if(length<3)return;
        dx/=length;dy/=length;
        line(p,x,y,xx,yy,1.3f,color);
        float head=Math.min(10,length*.25f),side=head*.5f;
        line(p,xx-dx*head-dy*side,yy-dy*head+dx*side,xx,yy,1.3f,color);
        line(p,xx-dx*head+dy*side,yy-dy*head-dx*side,xx,yy,1.3f,color);
    }
}
