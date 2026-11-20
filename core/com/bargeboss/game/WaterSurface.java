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
    static final float WAKE_SPEED=.55f, WAKE_WIDTH=26;
    /** All consumers use the same signed packet, fan, decay and shore visibility. */
    static float wakeRadius(int age){return 9+age*WAKE_SPEED;}
    static float wakeProfile(float offset){
        if(Math.abs(offset)>=WAKE_WIDTH)return 0;
        double phase=offset*StrictMath.PI/WAKE_WIDTH;
        return (float)(StrictMath.cos(phase)*(1+StrictMath.cos(phase))*.5);
    }
    private float wake(Game g,int i,float wx,float wy,Body ignore){
        if(age[i]>=LIFE || ignore!=null && source[i]==ignore)return 0;
        float dx=wx-x[i],dy=wy-y[i],reach=wakeRadius(age[i])+WAKE_WIDTH;
        if(Math.abs(dx)>reach || Math.abs(dy)>reach)return 0;
        float d=Physics.length(dx,dy),profile=wakeProfile(d-wakeRadius(age[i]));
        if(profile==0 || d<.001f)return 0;
        float angle=Math.abs(Physics.angle((float)StrictMath.atan2(dy,dx)-heading[i]-(float)StrictMath.PI));
        float fan=1-SeaWaves.smooth(.32f*(float)StrictMath.PI,.4f*(float)StrictMath.PI,angle);
        if(fan==0)return 0;
        // The exact same visibility rule applies to height and orbital flow.
        int samples=Math.max(1,(int)StrictMath.ceil(d/8));
        for(int k=0;k<=samples;k++)if(!WaterEffectsPainter.contains(g,x[i]+dx*k/samples,y[i]+dy*k/samples))return 0;
        return strength[i]*(1-age[i]/(float)LIFE)*profile*fan;
    }
    public float energy(Game g,float wx,float wy,Body ignore){
        float sum=0;for(int i=0;i<CAPACITY;i++)sum+=Math.abs(wake(g,i,wx,wy,ignore));
        return Math.min(1.8f,sum);
    }
    /** Height in sprite-rise units; orbital flow in world units/second. */
    public float value(Game g,float wx,float wy,float tick,Body ignore,int component){
        float total=SeaWaves.value(g,wx,wy,tick,component);
        for(int i=0;i<CAPACITY;i++){
            float h=wake(g,i,wx,wy,ignore)*1.2f;if(h==0)continue;
            if(component==0)total+=h;
            else {float dx=wx-x[i],dy=wy-y[i],d=Physics.length(dx,dy);
                total+=h*(WAKE_SPEED/Physics.DT)*(float)StrictMath.PI/WAKE_WIDTH*(component==1?dx:dy)/d;}
        }
        return Physics.clamp(total,component==0?-6:-12,component==0?6:12);
    }
    public float height(Game g,float wx,float wy,float tick,Body ignore){return value(g,wx,wy,tick,ignore,0);}
    public void buoyForce(Game g,float wx,float wy,float[] out){
        out[0]=value(g,wx,wy,g.visualTicks,null,1)*1.4f;
        out[1]=value(g,wx,wy,g.visualTicks,null,2)*1.4f;
    }
    /** Four hull samples share the wave phase. No direct position/heading edits. */
    public void affect(Game g,Body b){
        if(!g.montroseStage() || b.lineAttached || b.awaitingPickup)return;
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float ax=0,ay=0,torque=0;
        for(int i=0;i<4;i++){
            float along=i<2?(i==0?b.half:-b.half):0,across=i<2?0:(i==2?b.radius:-b.radius);
            float rx=c*along-s*across,ry=s*along+c*across;
            float ux=value(g,b.x+rx,b.y+ry,g.visualTicks,b,1),uy=value(g,b.x+rx,b.y+ry,g.visualTicks,b,2);
            // Existing hull drag still handles still-water resistance. Add only its wave contribution.
            float forward=ux*c+uy*s,side=-ux*s+uy*c;
            float fx=.22f*forward*c-1.2f*side*s,fy=.22f*forward*s+1.2f*side*c;
            ax+=fx*.25f;ay+=fy*.25f;torque+=(rx*fy-ry*fx)*b.mass*.25f;
        }
        // Called once per real 60 Hz tick, before the game's two motion steps.
        b.vx+=Physics.clamp(ax,-5,5)*Physics.DT;b.vy+=Physics.clamp(ay,-5,5)*Physics.DT;
        b.spin+=Physics.clamp(torque/b.inertia,-.12f,.12f)*Physics.DT;
    }
    public void step(Game g) { advanceWakes();floatBoats(g); }
    public void advanceWakes(){
        foam.step();
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) age[i]++;
    }
    public void floatBoats(Game g){
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
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float value=(height(g,b.x+c*b.half,b.y+s*b.half,g.visualTicks,b)
                +height(g,b.x-c*b.half,b.y-s*b.half,g.visualTicks,b)
                +height(g,b.x-s*b.radius,b.y+c*b.radius,g.visualTicks,b)
                +height(g,b.x+s*b.radius,b.y-c*b.radius,g.visualTicks,b))*.25f*response;
        // Hull response is filtered, so nearby wakes cannot make the sprite snap vertically.
        b.heave+= (value-b.heave)*(b.cruiser?.18f:b.berth>=0?.075f:.12f);
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
        return g.water.height(g,b.x,b.y,g.visualTicks,b)*(b.lineAttached?.25f:b.berth>=0?.5f:.75f);
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
        try {SeaWaves.draw(g,p);drawCrests(g,p);} finally {p.endWaterFoamBatch();if(clip.length>0)canvas.popClip();}
    }
    private void drawCrests(Game g,Painter p) {
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) {
            float t=age[i]/(float)LIFE,r=wakeRadius(age[i]);
            int alpha=(int)Math.min(255,330*strength[i]*(1-t)*(1-t*.35f));
            if(alpha<3)continue;
            float start=heading[i]+(float)Math.PI*.6f,end=heading[i]+(float)Math.PI*1.4f;
            for(int j=0;j<16;j++) {
                float a=start+(end-start)*j/16,b=start+(end-start)*(j+1)/16;
                float ra=r,rb=r;
                float xx=x[i]+ra*(float)StrictMath.cos(a),yy=y[i]+ra*(float)StrictMath.sin(a);
                float bx=x[i]+rb*(float)StrictMath.cos(b),by=y[i]+rb*(float)StrictMath.sin(b);
                // Full stroke clipping prevents crests cutting across quay corners.
                // Fan opacity follows the same shore-visible packet sampled by hulls.
                float mid=(a+b)*.5f;
                float crest=Math.max(0,wake(g,i,x[i]+r*(float)StrictMath.cos(mid),y[i]+r*(float)StrictMath.sin(mid),null));
                crest/=Math.max(.001f,strength[i]*(1-t));
                if(crest<.17f && age[i]>45) continue;
                float dx=bx-xx,dy=by-yy,len=Physics.length(dx,dy),width=10+strength[i]*8;
                if(len<.01f)continue;
                p.waterFoam((xx+bx)*.5f,(yy+by)*.5f,dx,dy,-dy/len*width,dx/len*width,
                        WaterFoam.phase(g.visualTicks,i)+j*.7f,5,alpha*crest/255f);
            }
        }
    }
}
