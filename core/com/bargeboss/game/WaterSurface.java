package com.bargeboss.game;

/** Shared wave field. Hulls emit expanding wake packets at the trough of each moving cycle. */
public final class WaterSurface {
    public static final int CAPACITY=192,LIFE=300;
    public final float[] x=new float[CAPACITY],y=new float[CAPACITY],strength=new float[CAPACITY],heading=new float[CAPACITY];
    public final int[] age=new int[CAPACITY];
    private final Body[] source=new Body[CAPACITY];
    public final VesselFoam foam=new VesselFoam();
    private int cursor;
    /** Diagnostic/reference path; default uses bit-equivalent paired sampling. */
    public boolean referenceSampling;
    private final float[] flowScratch=new float[4];
    private boolean wakeBoundsReady;
    private float wakeLeft,wakeTop,wakeRight,wakeBottom;
    /** Conservative aggregate bound, updated in the existing age loop and on emission.
     * It only rejects impossible samples; near samples retain the exact packet order. */
    private void includeWake(int i){
        float reach=wakeRadius(age[i])+WAKE_WIDTH+2;
        wakeLeft=Math.min(wakeLeft,x[i]-reach);wakeRight=Math.max(wakeRight,x[i]+reach);
        wakeTop=Math.min(wakeTop,y[i]-reach);wakeBottom=Math.max(wakeBottom,y[i]+reach);
    }
    private boolean outsideWakes(float x,float y){return wakeBoundsReady && (x<wakeLeft || x>wakeRight || y<wakeTop || y>wakeBottom);}
    void rebuildWakeBounds(){
        wakeLeft=wakeTop=Float.POSITIVE_INFINITY;wakeRight=wakeBottom=Float.NEGATIVE_INFINITY;
        for(int i=0;i<CAPACITY;i++)if(age[i]<LIFE)includeWake(i);
        wakeBoundsReady=true;
    }
    private final WaterClip surfaceClip=new WaterClip();
    final WaterClip ambientClip=new WaterClip();
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
        float sum=0;for(int i=0;i<CAPACITY;i++){float w=wake(g,i,wx,wy,ignore);if(w!=0)sum+=w<0?-w:w;}
        return Math.min(1.8f,sum);
    }
    /** Height in sprite-rise units; orbital flow in world units/second. */
    public float value(Game g,float wx,float wy,float tick,Body ignore,int component){
        float sea=SeaWaves.value(g,wx,wy,tick,component),total=g.montroseStage()?0:sea;
        if(!referenceSampling && outsideWakes(wx,wy))return g.montroseStage()?sea:Physics.clamp(total,component==0?-6:-12,component==0?6:12);
        for(int i=0;i<CAPACITY;i++){
            float h=wake(g,i,wx,wy,ignore)*1.2f;if(h==0)continue;
            if(component==0)total+=h;
            else {float dx=wx-x[i],dy=wy-y[i],d=Physics.length(dx,dy);
                total+=h*(WAKE_SPEED/Physics.DT)*(float)StrictMath.PI/WAKE_WIDTH*(component==1?dx:dy)/d;}
        }
        return g.montroseStage()?sea+Physics.clamp(total,component==0?-3:-8,component==0?3:8)
                :Physics.clamp(total,component==0?-6:-12,component==0?6:12);
    }
    public float height(Game g,float wx,float wy,float tick,Body ignore){return value(g,wx,wy,tick,ignore,0);}
    /** Total X/Y flow followed by ambient X/Y; caller owns reusable four-float storage. */
    void flow(Game g,float wx,float wy,float tick,Body ignore,float[] out){
        if(referenceSampling){
            out[0]=value(g,wx,wy,tick,ignore,1);out[1]=value(g,wx,wy,tick,ignore,2);
            out[2]=SeaWaves.value(g,wx,wy,tick,1);out[3]=SeaWaves.value(g,wx,wy,tick,2);return;
        }
        SeaWaves.flow(g,wx,wy,tick,out);
        float seaX=out[0],seaY=out[1];boolean marina=g.montroseStage();
        float sx=marina?0:seaX,sy=marina?0:seaY;
        if(!outsideWakes(wx,wy))for(int i=0;i<CAPACITY;i++){
            float h=wake(g,i,wx,wy,ignore)*1.2f;if(h==0)continue;
            float dx=wx-x[i],dy=wy-y[i],d=Physics.length(dx,dy);
            sx+=h*(WAKE_SPEED/Physics.DT)*(float)StrictMath.PI/WAKE_WIDTH*dx/d;
            sy+=h*(WAKE_SPEED/Physics.DT)*(float)StrictMath.PI/WAKE_WIDTH*dy/d;
        }
        out[0]=marina?seaX+Physics.clamp(sx,-8,8):Physics.clamp(sx,-12,12);
        out[1]=marina?seaY+Physics.clamp(sy,-8,8):Physics.clamp(sy,-12,12);
        out[2]=seaX;out[3]=seaY;
    }
    public void buoyForce(Game g,float wx,float wy,float[] out){
        flow(g,wx,wy,g.visualTicks,null,flowScratch);
        out[0]=flowScratch[0]*1.4f;out[1]=flowScratch[1]*1.4f;
    }
    /** Four hull samples share the wave phase. No direct position/heading edits. */
    public void affect(Game g,Body b){
        if(!g.montroseStage() || b.lineAttached || b.awaitingPickup)return;
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float ax=0,ay=0,torque=0;
        for(int i=0;i<4;i++){
            float along=i<2?(i==0?b.half:-b.half):0,across=i<2?0:(i==2?b.radius:-b.radius);
            float rx=c*along-s*across,ry=s*along+c*across;
            flow(g,b.x+rx,b.y+ry,g.visualTicks,b,flowScratch);
            float ux=flowScratch[0],uy=flowScratch[1];
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
        wakeLeft=wakeTop=Float.POSITIVE_INFINITY;wakeRight=wakeBottom=Float.NEGATIVE_INFINITY;
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE){age[i]++;if(age[i]<LIFE)includeWake(i);}
        wakeBoundsReady=true;
    }
    public void floatBoats(Game g){
        floatBoat(g,g.tug);
        if(g.teacherBoat()!=null)floatBoat(g,g.teacherBoat());
        for(Body b:g.cargo) if(b!=null && b.active) floatBoat(g,b);
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            floatBoat(g,f.boat);if(f.load!=null) floatBoat(g,f.load);
        }
        if(g.trafficActive()) floatBoat(g,g.trafficBoat);
        for(HarborTraffic.Craft c:g.harborTraffic.craft)if(c.body.active)floatBoat(g,c.body);
        if(g.job!=null)for(Body b:g.job.bodies())if(b!=null && b.active)floatBoat(g,b);
    }
    private void floatBoat(Game g,Body b) {
        foam.emit(g,b);
        float old=b.heave,velocity=b.heaveVelocity;
        float response=b.berth>=0?.76f:1;
        if(b.lineAttached && b!=g.tug || b.awaitingPickup) response*=.45f;
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        if(g.montroseStage()){
            float ambient=0,wakes=0;
            for(int i=0;i<4;i++){
                float along=i<2?(i==0?b.half:-b.half):0,across=i<2?0:(i==2?b.radius:-b.radius);
                float x=b.x+c*along-s*across,y=b.y+s*along+c*across;
                float sea=SeaWaves.value(g,x,y,g.visualTicks,0);
                ambient+=sea*.25f;wakes+=(height(g,x,y,g.visualTicks,b)-sea)*.25f;
            }
            // Spatial averaging smooths ambient waves without delaying their phase.
            // Only arriving local wake disturbances retain a damped response.
            b.wakeHeave+=(wakes*response-b.wakeHeave)*(b.cruiser?.18f:b.berth>=0?.075f:.12f);
            b.heave=ambient+b.wakeHeave;
        }else {
            float value=(height(g,b.x+c*b.half,b.y+s*b.half,g.visualTicks,b)
                    +height(g,b.x-c*b.half,b.y-s*b.half,g.visualTicks,b)
                    +height(g,b.x-s*b.radius,b.y+c*b.radius,g.visualTicks,b)
                    +height(g,b.x+s*b.radius,b.y-c*b.radius,g.visualTicks,b))*.25f*response;
            b.heave+=(value-b.heave)*(b.cruiser?.18f:b.berth>=0?.075f:.12f);
        }
        b.heaveVelocity=(b.heave-old)/Physics.DT;
        if(b.wakeKickTicks>0) b.wakeKickTicks--;
        if(b.wakeCooldown>0) b.wakeCooldown--;
        if(b.floatingInitialized && velocity<0 && b.heaveVelocity>=0 && b.speed()>6 && b.wakeCooldown==0) {
            emit(b,1);pulses++;b.wakeKickTicks=18;b.wakeCooldown=35;
        }
        if(b.speed()>6 && (!b.lineAttached || b==g.tug) && !b.awaitingPickup) {
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
        if(wakeBoundsReady)includeWake(i);
    }
    public static float lift(Game g,Body b) {
        if(b.floatingInitialized) return b.heave;
        // Title/preview hulls have no simulation clock, but still ride ambient water.
        return g.water.height(g,b.x,b.y,g.visualTicks,b)*(b.lineAttached && b!=g.tug?.25f:b.berth>=0?.5f:.75f);
    }
    public void draw(Game g,Painter p) {
        if(g.settings.waterDetail<1)return;
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
            clip=g.montroseStage()?surfaceClip.get(g,c,l-8,top-8,right+8,bottom+8,canvas.supportsUnionClips())
                    :ShoreOcclusion.view(g,c,8,canvas.supportsUnionClips());
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
                WavePatch.foam(g,p,(xx+bx)*.5f,(yy+by)*.5f,dx,dy,-dy/len*width,dx/len*width,
                        WaterFoam.phase(g.visualTicks,i)+j*.7f,5,alpha*crest/255f);
            }
        }
    }
}
