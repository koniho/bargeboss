package com.bargeboss.game;

/** Shared tow target, sagging line, tension highlights and release recoil. */
public final class TowView {
    private TowView() {}
    public static void draw(Game g,Painter p) {
        Body held=g.heldCargo();
        if(g.canTow(held))for(int i=-1;i<g.cargo.length;i++) {
            Body target=i<0?g.tug:g.cargo[i];if(!g.canTowTo(held,target))continue;
            float pulse=2*(float)StrictMath.sin(g.visualTicks*.12);
            boolean aimed=g.towAiming && g.towAimBody==target;
            ControlView.ring(g,p,target.x,target.y,target==g.tug?39+pulse:45+pulse,1,aimed?ControlView.GREEN:ControlView.BLUE,2);
            if(aimed)SoftCue.caption(p,"RELEASE TO LINK",target.x+40,target.y,9,VisualStyle.CREAM);
        }
        for(int slot=0;slot<Towline.CAPACITY;slot++) {
        Towline line=g.tow.link(slot);if(line==null)continue;
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
            if((g.tow.count()==1 || held==line.cargo) && (g.tutorial==null || g.tutorial.lesson!=Tutorial.RELEASE)) SoftCue.caption(p,"TAP TO LET GO",line.cargo.x-47,line.cargo.y,8,ControlView.BLUE);
            if(line.leader==g.tug)SoftCue.caption(p,line.tension>12?"PULLING":"LINE SLACK",(x+xx)/2+15,(y+yy)/2,8,color);
        }
        }
    }
    /** Small 3-D spans join the same ground-depth order as the hulls they attach to. */
    public static final class Segments {
        private final Game game;
        public final int count;
        final float[] x=new float[Towline.CAPACITY*25],y=new float[Towline.CAPACITY*25],height=new float[Towline.CAPACITY*25];
        final int[] order=new int[Towline.CAPACITY*24],points=new int[Towline.CAPACITY*24],colors=new int[Towline.CAPACITY*24];
        public Segments(Game g) {
            game=g;int used=0;
            for(int link=0;link<Towline.CAPACITY;link++) {
                Towline line=g.tow.link(link);if(line==null)continue;
                float sx,sy,ex,ey,slack,h0,h1;int color;
                if(line.cargo!=null) {
                    sx=line.cargoX();sy=line.cargoY();ex=line.tugX(g.tug);ey=line.tugY(g.tug);
                    slack=Physics.clamp((line.length-Physics.length(ex-sx,ey-sy))*.25f,0,12);
                    h0=LassoPath.DECK_HEIGHT+WaterSurface.lift(g,line.cargo);h1=LassoPath.DECK_HEIGHT+WaterSurface.lift(g,line.leader);
                    color=line.tension>180?VisualStyle.GOLD:0xffffe3a1;
                } else if(line.releaseTicks>0) {
                    float t=line.releaseTicks/18f;sx=line.releaseToX;sy=line.releaseToY;
                    ex=sx+(line.releaseX-sx)*t;ey=sy+(line.releaseY-sy)*t;
                    slack=8*t;h0=h1=LassoPath.DECK_HEIGHT;color=((int)(230*t)<<24)|0xffe3a1;
                } else continue;
                int base=link*25;
                for(int i=0;i<=24;i++) {
                    float t=i/24f;x[base+i]=sx+(ex-sx)*t;y[base+i]=sy+(ey-sy)*t;
                    height[base+i]=Math.max(.5f,h0+(h1-h0)*t-4*t*(1-t)*slack);
                }
                for(int i=0;i<24;i++){points[used]=base+i;colors[used]=color;used++;}
            }
            count=used;
            for(int i=0;i<count;i++) {
                int j=i-1;while(j>=0 && groundDepth(order[j])>groundDepth(i)){order[j+1]=order[j];j--;}
                order[j+1]=i;
            }
        }
        private float groundDepth(int span){int i=points[span];return (y[i]+y[i+1]-x[i]-x[i+1])*.5f;}
        public float depth(RiverCamera c,int slot){int i=points[order[slot]];return c.screenY((x[i]+x[i+1])*.5f,(y[i]+y[i+1])*.5f);}
        public void draw(Painter p,int slot) {
            int i=points[order[slot]],color=colors[order[slot]];boolean river=p instanceof RiverPainter;
            float sx=x[i]+LassoPath.liftX(river,height[i]),sy=y[i]+LassoPath.liftY(river,height[i]);
            float ex=x[i+1]+LassoPath.liftX(river,height[i+1]),ey=y[i+1]+LassoPath.liftY(river,height[i+1]);
            Painter canvas=river?((RiverPainter)p).canvas():p;
            float[] clip=river?ShoreOcclusion.region(game,((RiverPainter)p).camera,
                    Math.min(x[i],x[i+1])-12,Math.min(y[i],y[i+1])-12,Math.max(x[i],x[i+1])+12,Math.max(y[i],y[i+1])+12,depth(((RiverPainter)p).camera,slot)):ShoreOcclusion.EMPTY;
            if(clip.length>0)canvas.pushClipOutside(clip);
            try {
            p.line(sx+1,sy+2,ex+1,ey+2,4.5f,SoftCue.alpha(VisualStyle.OUTLINE,(color>>>24)/255f));
            p.line(sx,sy,ex,ey,3,color);
            if(i%2==0)p.line(sx,sy-1,ex,ey-1,1,SoftCue.alpha(0xffffe3a1,(color>>>24)/255f));
            } finally {if(clip.length>0)canvas.popClip();}
        }
    }
}
