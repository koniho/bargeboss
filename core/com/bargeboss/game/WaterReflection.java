package com.bargeboss.game;

/** Colored light shimmer; shared anchors and shore clips, never a screen-space postprocess. */
public final class WaterReflection {
    private WaterReflection() {}
    public static void draw(Game g,Painter world,float x,float y,float width,float length,int color,int seed) {
        if((color>>>24)==0)return;
        float cx=x-length*.5f,cy=y+length*.5f;
        RiverPainter river=world instanceof RiverPainter?(RiverPainter)world:null;
        Painter canvas=river==null?world:river.canvas();float extent=(width+length)*.5f+3;
        if(river!=null) {
            RiverCamera c=river.camera;float sx=c.screenX(cx,cy),sy=c.screenY(cx,cy),r=extent*c.scale*1.5f;
            if(sx+r<0 || sy+r<0 || sx-r>g.layout.width || sy-r>g.layout.height)return;
        }
        float[] clip=river==null?ShoreOcclusion.EMPTY:ShoreOcclusion.region(g,river.camera,cx-extent,cy-extent,cx+extent,cy+extent,Float.NEGATIVE_INFINITY);
        if(clip.length>0)canvas.pushClipOutside(clip);
        try {world.waterReflection(cx,cy,width,width,-length,length,WaterFoam.phase(g.visualTicks,seed),color);}
        finally {if(clip.length>0)canvas.popClip();}
    }
    public static void fallback(Painter p,float x,float y,float ax,float ay,float bx,float by,float phase,int color) {
        for(int i=0;i<9;i++) {
            float v=(i+.5f)/9,drift=.06f*(float)StrictMath.sin(phase+i*1.7f);
            float span=(.18f+.17f*(float)StrictMath.sin(phase+i*.9f))*(1-v*.5f);
            float cx=x+bx*(v-.5f)+ax*drift,cy=y+by*(v-.5f)+ay*drift;
            int a=(int)((color>>>24)*(1-v)*.65f);
            p.line(cx-ax*span,cy-ay*span,cx+ax*span,cy+ay*span,Math.max(.5f,Physics.length(bx,by)*.018f),(a<<24)|(color&0xffffff));
        }
    }
}
