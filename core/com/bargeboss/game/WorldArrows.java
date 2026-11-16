package com.bargeboss.game;

/** Frame-local guidance queue merged with the same ground-depth order as vessels.
 * The cue-only prepass executes shared view rules against a sink, never native drawing. */
final class WorldArrows {
    private static final ThreadLocal<WorldArrows> LOCAL=new ThreadLocal<WorldArrows>(){protected WorldArrows initialValue(){return new WorldArrows();}};
    private final float[][] arrows=new float[16][5];private final int[] colors=new int[16];
    private int count,next;private boolean active,collecting;
    static void begin(Game g){WorldArrows q=LOCAL.get();q.count=q.next=0;q.active=q.collecting=true;
        Painter p=SINK;
        ControlView.draw(g,new RiverPainter(p,g.layout.camera));HarborView.objective(g,p);TrainingChannelView.gateIndicator(g,p);
        if(g.tutorial!=null&&g.mode!=Game.HELP&&g.mode!=Game.PAUSE&&g.mode!=Game.FINISH)TutorialView.draw(g,p);
        q.collecting=false;
    }
    static void end(){LOCAL.get().active=false;}
    static boolean capture(Painter p,float x,float y,float xx,float yy,int color){WorldArrows q=LOCAL.get();if(!q.active||!(p instanceof RiverPainter))return false;
        if(!(Physics.length(xx-x,yy-y)>=3))return true;
        if(q.collecting){if(q.count==q.arrows.length)throw new IllegalStateException("World arrow budget exceeded");
            float depth=((RiverPainter)p).camera.screenY((x+xx)*.5f,(y+yy)*.5f);int at=q.count++;
            while(at>0&&depth<q.arrows[at-1][4]){System.arraycopy(q.arrows[at-1],0,q.arrows[at],0,5);q.colors[at]=q.colors[at-1];at--;}
            float[] a=q.arrows[at];a[0]=x;a[1]=y;a[2]=xx;a[3]=yy;a[4]=depth;q.colors[at]=color;
        }return true;
    }
    static float depth(){WorldArrows q=LOCAL.get();return q.active&&q.next<q.count?q.arrows[q.next][4]:Float.MAX_VALUE;}
    static boolean pending(){WorldArrows q=LOCAL.get();return q.active&&q.next<q.count;}
    static void draw(Painter world){WorldArrows q=LOCAL.get();float[] a=q.arrows[q.next];SoftCue.arrowPlane(world,a[0],a[1],a[2],a[3],q.colors[q.next++]);}
    private static final Painter SINK=new Painter(){
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}public void poly(float[] xy,int c){}
        public void material(float x,float y,float ax,float ay,float bx,float by,int k,float detail,float amount,int c){}
        public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int c){}
        public void layer(StaticLayer l,float x,float y,float s,float a){}
    };
}
