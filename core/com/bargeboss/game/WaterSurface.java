package com.bargeboss.game;

/** Shared wave field. Hulls emit expanding wake packets at the trough of each moving cycle. */
public final class WaterSurface {
    public static final int CAPACITY=192,LIFE=300;
    public final float[] x=new float[CAPACITY],y=new float[CAPACITY],strength=new float[CAPACITY],heading=new float[CAPACITY];
    public final int[] age=new int[CAPACITY];
    private final Body[] source=new Body[CAPACITY];
    public final VesselFoam foam=new VesselFoam();
    private int cursor;
    public int emitted,pulses;
    public WaterSurface() { for(int i=0;i<CAPACITY;i++) age[i]=LIFE; }
    public float energy(float wx,float wy,Body ignore) {
        float total=0;
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE && source[i]!=ignore) {
            float dx=wx-x[i],dy=wy-y[i],radius=age[i]*.55f;
            // Outside the crest's bounding square the contribution is exactly zero.
            // A one-unit guard keeps this rejection clear of float rounding at the band edge.
            float reach=radius+33;
            if(Math.abs(dx)>reach || Math.abs(dy)>reach)continue;
            float distance=Physics.length(dx,dy);
            float band=Math.max(0,1-Math.abs(distance-radius)/32);
            total+=strength[i]*band*(1-age[i]/(float)LIFE);
        }
        return Math.min(1.8f,total);
    }
    public float height(float wx,float wy,int tick,Body ignore) {
        float local=energy(wx,wy,ignore);
        float wave=(float)StrictMath.sin(tick*.048+wx*.017+wy*.009);
        float cross=(float)StrictMath.sin(tick*.031-wx*.013+wy*.021);
        return (.65f+local*1.2f)*wave+.22f*cross;
    }
    /** Horizontal orbital motion of floats: arriving wake packets push then draw back.
     * Only nearby crest bands need a shoreline visibility check. No temporary allocations. */
    public void buoyForce(Game g,float wx,float wy,float[] out){
        out[0]=.65f*(float)StrictMath.sin(g.visualTicks*.048+wx*.017+wy*.009);
        out[1]=.5f*(float)StrictMath.sin(g.visualTicks*.031-wx*.013+wy*.021);
        for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE && source[i]!=null){
            float dx=wx-x[i],dy=wy-y[i],reach=36+age[i]*.55f;
            if(Math.abs(dx)>reach || Math.abs(dy)>reach)continue;
            float distance=Physics.length(dx,dy),offset=distance-(9+age[i]*.55f);
            if(Math.abs(offset)>=26 || distance<.001f)continue;
            // Match the rear-facing fan used by draw(), not a full circular blast.
            float angle=Physics.angle((float)StrictMath.atan2(dy,dx)-heading[i]-(float)StrictMath.PI);
            if(Math.abs(angle)>(float)StrictMath.PI*.4f)continue;
            boolean clear=true;int samples=Math.max(1,(int)(distance/8));
            for(int k=0;k<=samples;k++)if(!WaterEffectsPainter.contains(g,x[i]+dx*k/samples,y[i]+dy*k/samples)){clear=false;break;}
            if(!clear)continue;
            float envelope=1-Math.abs(offset)/26;
            float push=22*strength[i]*(1-age[i]/(float)LIFE)*envelope*(float)StrictMath.sin(offset*StrictMath.PI/26);
            out[0]+=push*dx/distance;out[1]+=push*dy/distance;
        }
        out[0]=Physics.clamp(out[0],-35,35);out[1]=Physics.clamp(out[1],-35,35);
    }
    public void step(Game g) {
        foam.step();
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) age[i]++;
        floatBoat(g,g.tug);
        if(g.teacherBoat()!=null)floatBoat(g,g.teacherBoat());
        for(Body b:g.cargo) if(b!=null && b.active) floatBoat(g,b);
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            floatBoat(g,f.boat);if(f.load!=null) floatBoat(g,f.load);
        }
        if(g.trafficActive()) floatBoat(g,g.trafficBoat);
    }
    private void floatBoat(Game g,Body b) {
        foam.emit(g,b);
        float old=b.heave,velocity=b.heaveVelocity;
        float response=b.berth>=0?.76f:1;
        if(b.lineAttached || b.awaitingPickup) response*=.45f;
        float value=height(b.x,b.y,g.visualTicks,b)*response;
        // Hull response is filtered, so nearby wakes cannot make the sprite snap vertically.
        b.heave+= (value-b.heave)*.18f;
        b.heaveVelocity=(b.heave-old)/Physics.DT;
        if(b.wakeKickTicks>0) b.wakeKickTicks--;
        if(b.wakeCooldown>0) b.wakeCooldown--;
        if(b.floatingInitialized && velocity<0 && b.heaveVelocity>=0 && b.speed()>6 && b.wakeCooldown==0) {
            emit(b,1);pulses++;b.wakeKickTicks=18;b.wakeCooldown=35;
        }
        if(b.speed()>6 && !b.lineAttached && !b.awaitingPickup) {
            if(++b.wakeTrailTicks>=8) { emit(b,.34f);b.wakeTrailTicks=0; }
        } else b.wakeTrailTicks=0;
        b.floatingInitialized=true;
    }
    private void emit(Body b,float intensity) {
        int i=cursor;cursor=(cursor+1)%CAPACITY;age[i]=0;source[i]=b;
        float angle=(float)StrictMath.atan2(b.vy,b.vx);
        heading[i]=angle;
        float stern=b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(angle-b.angle));
        x[i]=b.x-(float)StrictMath.cos(angle)*stern;y[i]=b.y-(float)StrictMath.sin(angle)*stern;
        strength[i]=intensity*Math.min(1,b.speed()/50)*(b.berth>=0?1.15f:.9f);emitted++;
    }
    public static float lift(Game g,Body b) {
        if(b.floatingInitialized) return b.heave;
        // Title/preview hulls have no simulation clock, but still ride ambient water.
        return g.water.height(b.x,b.y,g.visualTicks,b)*(b.lineAttached?.25f:b.berth>=0?.5f:.75f);
    }
    public void draw(Game g,Painter p) {
        foam.draw(g,p);
        RiverPainter river=p instanceof RiverPainter?(RiverPainter)p:null;
        Painter canvas=river==null?p:river.canvas();float[] clip=ShoreOcclusion.EMPTY;
        if(river!=null) {
            RiverCamera c=river.camera;float l=Float.MAX_VALUE,top=l,right=-l,bottom=-l;
            for(int corner=0;corner<4;corner++) {
                float sx=(corner%2)*g.layout.width,sy=(corner/2)*g.layout.height;
                float wx=c.worldX(sx,sy),wy=c.worldY(sx,sy);
                l=Math.min(l,wx);right=Math.max(right,wx);top=Math.min(top,wy);bottom=Math.max(bottom,wy);
            }
            clip=ShoreOcclusion.region(g,c,l-8,top-8,right+8,bottom+8,Float.NEGATIVE_INFINITY,canvas.supportsUnionClips());
        } else p=new WaterEffectsPainter(g,p);
        if(clip.length>0)canvas.pushClipOutside(clip);
        p.beginWaterFoamBatch();
        try {drawCrests(g,p);} finally {p.endWaterFoamBatch();if(clip.length>0)canvas.popClip();}
    }
    private void drawCrests(Game g,Painter p) {
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) {
            float t=age[i]/(float)LIFE,r=9+age[i]*.55f;
            int alpha=(int)Math.min(255,330*strength[i]*(1-t)*(1-t*.35f));
            if(alpha<3)continue;
            float start=heading[i]+(float)Math.PI*.6f,end=heading[i]+(float)Math.PI*1.4f;
            for(int j=0;j<16;j++) {
                float a=start+(end-start)*j/16,b=start+(end-start)*(j+1)/16;
                float ra=r*(1+.018f*(float)StrictMath.sin(a*7+i*1.1)),rb=r*(1+.018f*(float)StrictMath.sin(b*7+i*1.1));
                float xx=x[i]+ra*(float)StrictMath.cos(a),yy=y[i]+ra*(float)StrictMath.sin(a);
                float bx=x[i]+rb*(float)StrictMath.cos(b),by=y[i]+rb*(float)StrictMath.sin(b);
                // Full stroke clipping prevents crests cutting across quay corners.
                // A slight local swell keeps expanding packets from reading as compass arcs.
                float crest=.55f+.45f*(float)StrictMath.sin(i*1.73+j*.81+age[i]*.055);
                if(crest<.17f && age[i]>45) continue;
                float dx=bx-xx,dy=by-yy,len=Physics.length(dx,dy),width=10+strength[i]*8;
                if(len<.01f)continue;
                p.waterFoam((xx+bx)*.5f,(yy+by)*.5f,dx,dy,-dy/len*width,dx/len*width,
                        WaterFoam.phase(g.visualTicks,i)+j*.7f,5,alpha*crest/255f);
            }
        }
    }
}
