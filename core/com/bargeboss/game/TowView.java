package com.bargeboss.game;

/** Shared tow target, sagging line, tension highlights and release recoil. */
public final class TowView {
    private TowView() {}
    public static void draw(Game g,Painter p) {
        Towline line=g.tow;
        Body held=g.heldCargo();
        if(g.canTow(held)) {
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.12);
            ControlView.ring(g,p,g.tug.x,g.tug.y,39+pulse,1,g.towAiming?ControlView.GREEN:ControlView.BLUE,2.5f);
            SoftCue.caption(p,g.towAiming?"RELEASE TO TOW":"DRAG HERE TO TOW",g.tug.x+50,g.tug.y,9,VisualStyle.CREAM);
        }
        if(line.cargo!=null) {
            float x=line.cargoX(),y=line.cargoY(),xx=line.tugX(g.tug),yy=line.tugY(g.tug);
            int color=line.tension>180?VisualStyle.GOLD:0xffffe3a1;
            if(line.catchTicks>0) {
                float t=1-line.catchTicks/24f;
                int glow=((int)(220*(1-t))<<24)|0x92ecc2;
                ControlView.ring(g,p,x,y,8+30*t,1,glow,3);
                ControlView.ring(g,p,xx,yy,8+30*t,1,glow,3);
            }
            ControlView.ring(g,p,line.cargo.x,line.cargo.y,44,1,0x9983c8d5,2);
            if(g.tutorial==null || g.tutorial.lesson!=Tutorial.RELEASE) SoftCue.caption(p,"TAP TO LET GO",line.cargo.x-47,line.cargo.y,8,ControlView.BLUE);
            SoftCue.caption(p,line.tension>12?"PULLING":"LINE SLACK",(x+xx)/2+15,(y+yy)/2,8,color);
        }
    }
    /** Small 3-D spans join the same ground-depth order as the hulls they attach to. */
    public static final class Segments {
        public final int count;
        final float[] x=new float[25],y=new float[25],height=new float[25];
        final int[] order=new int[24];final int color;
        public Segments(Game g) {
            Towline line=g.tow;float sx,sy,ex,ey,slack,h0,h1;
            if(line.cargo!=null) {
                sx=line.cargoX();sy=line.cargoY();ex=line.tugX(g.tug);ey=line.tugY(g.tug);
                slack=Physics.clamp((line.length-Physics.length(ex-sx,ey-sy))*.25f,0,12);
                h0=LassoPath.DECK_HEIGHT+WaterSurface.lift(g,line.cargo);h1=LassoPath.DECK_HEIGHT+WaterSurface.lift(g,g.tug);
                color=line.tension>180?VisualStyle.GOLD:0xffffe3a1;count=24;
            } else if(line.releaseTicks>0) {
                float t=line.releaseTicks/18f;sx=line.releaseToX;sy=line.releaseToY;
                ex=sx+(line.releaseX-sx)*t;ey=sy+(line.releaseY-sy)*t;
                slack=8*t;h0=h1=LassoPath.DECK_HEIGHT;color=((int)(230*t)<<24)|0xffe3a1;count=24;
            } else {count=0;color=0;return;}
            for(int i=0;i<=count;i++) {
                float t=i/(float)count;x[i]=sx+(ex-sx)*t;y[i]=sy+(ey-sy)*t;
                height[i]=Math.max(.5f,h0+(h1-h0)*t-4*t*(1-t)*slack);
            }
            for(int i=0;i<count;i++) {
                int j=i-1;while(j>=0 && groundDepth(order[j])>groundDepth(i)){order[j+1]=order[j];j--;}
                order[j+1]=i;
            }
        }
        private float groundDepth(int i){return (y[i]+y[i+1]-x[i]-x[i+1])*.5f;}
        public float depth(RiverCamera c,int slot){int i=order[slot];return c.screenY((x[i]+x[i+1])*.5f,(y[i]+y[i+1])*.5f);}
        public void draw(Painter p,int slot) {
            int i=order[slot];boolean river=p instanceof RiverPainter;
            float sx=x[i]+LassoPath.liftX(river,height[i]),sy=y[i]+LassoPath.liftY(river,height[i]);
            float ex=x[i+1]+LassoPath.liftX(river,height[i+1]),ey=y[i+1]+LassoPath.liftY(river,height[i+1]);
            p.line(sx+1,sy+2,ex+1,ey+2,4.5f,SoftCue.alpha(VisualStyle.OUTLINE,(color>>>24)/255f));
            p.line(sx,sy,ex,ey,3,color);
            if(i%2==0)p.line(sx,sy-1,ex,ey-1,1,SoftCue.alpha(0xffffe3a1,(color>>>24)/255f));
        }
    }
}
