package com.bargeboss.game;

/** Broad luminous guidance with soft colored shoulders and a bright central ridge. */
public final class SoftCue {
    private SoftCue(){}
    public static int alpha(int color,float amount){return ((int)((color>>>24)*Physics.clamp(amount,0,1))<<24)|(color&0xffffff);}
    public static float breath(int tick){return .62f+.38f*(.5f+.5f*(float)StrictMath.sin(tick*.055));}
    public static int animated(Game g,int color){return alpha(color,breath(g.visualTicks));}
    /** Saturated semantic colors keep guidance distinct from warm scenery lamps. */
    static int neon(int color){int r=(color>>16)&255,g=(color>>8)&255,b=color&255,rgb;
        if(r>=g&&r>b)rgb=g>r*.55f?0xffee00:(b>g?0xd529ff:0xff391c);
        else if(b>g&&b>r)rgb=r>g?0xc936ff:0x00dfff;
        else rgb=b>g*.65f?0x00efff:0x22ff72;
        return (color&0xff000000)|rgb;
    }
    private static int highlight(int color){return (Math.min(255,Math.round((color>>>24)*1.3f))<<24)|(neon(color)&0xffffff);}
    public static void line(Painter p,float x,float y,float xx,float yy,float width,int color) {
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        float length=Physics.length(xx-x,yy-y);if(length<.01f)return;
        float core=Physics.clamp(width*3.2f,5.6f,9f)/Math.max(1,zoom),extent=core+16/zoom;
        float dx=xx-x,dy=yy-y;
        float dashes=length*zoom>38?Math.max(2,Math.round(length*zoom/32)):1;
        p.material((x+xx)*.5f,(y+yy)*.5f,dx,dy,-dy/length*extent,dx/length*extent,EffectMaterial.DASH_LINE,core/extent,dashes,highlight(color));
    }
    /** Text belongs to screen overlays; world cues use shape, color and motion only. */
    public static void caption(Painter p,String text,float x,float y,float size,int color) {
        if(!(p instanceof RiverPainter))Lettering.center(p,text,x,y,size,alpha(color,.78f));
    }
    public static void dot(Painter p,float x,float y,float radius,int color) {
        p.material(x,y,radius*3.6f,0,0,radius*3.6f,EffectMaterial.DOT,0,1,neon(color));
    }
    public static void ring(Painter p,float x,float y,float radius,float fraction,int color,float width) {
        if(radius<=0 || fraction<=0)return;
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        color=neon(color);
        float core=Math.min(radius*.35f,Physics.clamp(width*3.2f,5.6f,9f)/Math.max(1,zoom));
        ringBand(p,x,y,radius,fraction,Math.min(radius*.6f,core+11/zoom),alpha(color,.12f));
        ringBand(p,x,y,radius,fraction,Math.min(radius*.5f,core+5/zoom),alpha(color,.42f));
        ringBand(p,x,y,radius,fraction,core,highlight(color));
    }
    private static void ringBand(Painter p,float x,float y,float radius,float fraction,float width,int color) {
        float diameter=(radius*2+width)/.98f;
        p.material(x,y,diameter,0,0,diameter,radius<24?EffectMaterial.SMALL_RING:EffectMaterial.RING,-width/diameter,Physics.clamp(fraction,0,1),color);
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        if(WorldArrows.capture(p,x,y,xx,yy,color))return;
        arrowPlane(p,x,y,xx,yy,color);
    }
    /** Filled ribbons: every corner is projected from the water plane, including glow. */
    static void arrowPlane(Painter p,float x,float y,float xx,float yy,int color) {
        float length=Physics.length(xx-x,yy-y);if(length<3)return;
        float dx=xx-x,dy=yy-y,extent=Math.min(50,length*.8f);
        p.material((x+xx)*.5f,(y+yy)*.5f,dx,dy,-dy/length*extent,dx/length*extent,EffectMaterial.ARROW,0,1,highlight(color));
    }
}
