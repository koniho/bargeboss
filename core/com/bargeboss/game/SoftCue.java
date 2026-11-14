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
    /** World labels stay modest in screen space when the camera closes in. */
    public static void caption(Painter p,String text,float x,float y,float size,int color) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;
            Lettering.center(river.canvas(),text,c.screenX(x,y),c.screenY(x,y),Math.min(12,size*c.scale),alpha(color,.78f));
        } else Lettering.center(p,text,x,y,size,alpha(color,.78f));
    }
    public static void dot(Painter p,float x,float y,float radius,int color) {
        p.circle(x,y,radius*1.8f,alpha(color,.045f));
        p.circle(x,y,radius*1.3f,alpha(color,.09f));
        p.circle(x,y,radius*.8f,alpha(color,.35f));
    }
    public static void ring(Painter p,float x,float y,float radius,float fraction,int color,float width) {
        int segments=40;
        for(int i=0;i<segments*fraction;i++) {
            double a=-Math.PI/2+i*Math.PI*2/segments,b=-Math.PI/2+Math.min(i+1,segments*fraction)*Math.PI*2/segments;
            line(p,x+radius*(float)StrictMath.cos(a),y+radius*(float)StrictMath.sin(a),
                    x+radius*(float)StrictMath.cos(b),y+radius*(float)StrictMath.sin(b),width,color);
        }
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
