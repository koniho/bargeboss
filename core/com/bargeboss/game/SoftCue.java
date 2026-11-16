package com.bargeboss.game;

/** Broad luminous guidance with soft colored shoulders and a bright central ridge. */
public final class SoftCue {
    private SoftCue(){}
    public static int alpha(int color,float amount){return ((int)((color>>>24)*Physics.clamp(amount,0,1))<<24)|(color&0xffffff);}
    public static float breath(int tick){return .62f+.38f*(.5f+.5f*(float)StrictMath.sin(tick*.055));}
    public static int animated(Game g,int color){return alpha(color,breath(g.visualTicks));}
    private static int highlight(int color){int a=Math.min(255,Math.round((color>>>24)*1.55f));int r=(color>>16)&255,g=(color>>8)&255,b=color&255;
        return (a<<24)|((r+(255-r)*2/3)<<16)|((g+(255-g)*2/3)<<8)|(b+(255-b)*2/3);
    }
    public static void line(Painter p,float x,float y,float xx,float yy,float width,int color) {
        float zoom=p instanceof RiverPainter?((RiverPainter)p).camera.scale:1;
        float core=Physics.clamp(width*1.6f,2.8f,4.5f)/Math.max(1,zoom);
        p.line(x,y,xx,yy,core+11/zoom,alpha(color,.12f));
        p.line(x,y,xx,yy,core+5/zoom,alpha(color,.42f));
        p.line(x,y,xx,yy,core,highlight(color));
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
        float core=Physics.clamp(width*1.6f,2.8f,4.5f)/Math.max(1,zoom);
        ringBand(p,x,y,radius,fraction,core+11/zoom,alpha(color,.12f));
        ringBand(p,x,y,radius,fraction,core+5/zoom,alpha(color,.42f));
        ringBand(p,x,y,radius,fraction,core,highlight(color));
    }
    private static void ringBand(Painter p,float x,float y,float radius,float fraction,float width,int color) {
        float diameter=(radius*2+width)/.98f;
        p.material(x,y,diameter,0,0,diameter,EffectMaterial.RING,width/diameter,Physics.clamp(fraction,0,1),color);
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy);if(length<3)return;
        dx/=length;dy/=length;
        line(p,x,y,xx,yy,1.3f,color);
        float head=Math.min(21,length*.36f),side=head*.62f;
        line(p,xx-dx*head-dy*side,yy-dy*head+dx*side,xx,yy,1.3f,color);
        line(p,xx-dx*head+dy*side,yy-dy*head-dx*side,xx,yy,1.3f,color);
    }
}
