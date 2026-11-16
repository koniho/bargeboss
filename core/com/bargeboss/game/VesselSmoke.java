package com.bargeboss.game;

/** Fixed-step exhaust. A puff owns its world position once it leaves the stack. */
public final class VesselSmoke {
    public static final int CAPACITY=384,LIFE=480;
    final float[] x=new float[CAPACITY],y=new float[CAPACITY],rise=new float[CAPACITY];
    final float[] vx=new float[CAPACITY],vy=new float[CAPACITY],power=new float[CAPACITY];
    final int[] age=new int[CAPACITY];
    private int cursor;
    public int emitted;
    private final Body titleTug=new Body(0,-1,300,230,0);
    public VesselSmoke(){for(int i=0;i<CAPACITY;i++)age[i]=LIFE;}
    public void step(Game g) {
        drift();emit(g,g.tug);
        if(g.tutorial==null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE)emit(g,f.boat);
        if(g.trafficActive())emit(g,g.trafficBoat);
    }
    public void titleStep(Game g) {
        drift();TitleView.tugPose(titleTug,g.visualTicks);emit(g,titleTug);
    }
    /** Wind and buoyancy continue after an emitter stops, turns or leaves the scene. */
    public void drift() {
        for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE) {
            age[i]++;
            float curl=(float)StrictMath.sin(age[i]*.025+i*2.4);
            vx[i]+=(-5+curl*2-vx[i])*.018f;
            vy[i]+=(-10-curl*2-vy[i])*.018f;
            x[i]+=vx[i]/60;y[i]+=vy[i]/60;
            rise[i]+=(11+power[i]*4)/60;
        }
    }
    public void emit(Game g,Body b) {
        float load=Physics.clamp(b.engineLoad,0,1);
        b.smokeCredit+=(3+9*load)/60;
        while(b.smokeCredit>=1) {
            b.smokeCredit-=1;
            int i=cursor;cursor=(cursor+1)%CAPACITY;emitted++;
            float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
            float ox=SpriteAtlas.stackOffset(b.angle,true,false)*size;
            float oy=SpriteAtlas.stackOffset(b.angle,true,true)*size-WaterSurface.lift(g,b);
            // Invert the registered screen offset into a camera-independent world anchor.
            x[i]=b.x+(ox/RiverCamera.Q-oy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f;
            y[i]=b.y+(ox/RiverCamera.Q+oy/(RiverCamera.Q*RiverCamera.DEPTH))*.5f;
            vx[i]=-5+b.vx*.10f;vy[i]=-10+b.vy*.10f;
            rise[i]=0;power[i]=load;age[i]=0;
        }
    }
    public int count(){int n=0;for(int a:age)if(a<LIFE)n++;return n;}
    public void draw(Painter p,RiverCamera c,float width,float height) {
        // Oldest first: fresh, darker exhaust covers older diffuse clouds.
        for(int n=0;n<CAPACITY;n++) {
            int i=(cursor+n)%CAPACITY;if(age[i]>=LIFE)continue;
            float t=age[i]/(float)LIFE;
            float fade=Physics.clamp((1-t)/.6f,0,1);fade=fade*fade*(3-2*fade);
            float alpha=fade*Physics.clamp(age[i]/5f,0,1)*(.62f+.38f*power[i]);
            float radius=3.5f+power[i]*2+3*(float)StrictMath.sqrt(age[i]/60f);
            float scale=radius*c.scale/17;
            float sx=c.screenX(x[i],y[i]),sy=c.screenY(x[i],y[i])-rise[i]*c.scale;
            if(alpha<.008f || sx+20*scale<0 || sx-20*scale>width || sy+20*scale<0 || sy-20*scale>height)continue;
            p.smokePuff(sx,sy,20*scale,WaterFoam.phase(age[i],i),(Math.round(alpha*225)<<24)|SmokePuff.SOOT);
        }
    }
}
