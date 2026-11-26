package com.bargeboss.game;

/** One of Montrose's arcade jobs that follow the storm rescue (Challenges ids WAKE_PATROL..FIREWORKS_NIGHT).
 * A job owns its own props and rules; the shared Montrose flow owns dispatch, scoring records and the
 * hand-off to the next job. Fixed-step and deterministic; drawing never changes job state.
 *
 * Hooks: {@link #setup} once, {@link #step} every frame, {@link #motion} every physics sub-step,
 * {@link #water} under the vessels, {@link #overlay} over the world, {@link #hud} in screen space. */
abstract class MontroseJob {
    /** Frame clock since the job was dispatched, and the time limit in frames (0: untimed). */
    int ticks,limit;
    /** Floating "+points" pops in world space. */
    private final float[] popX=new float[8],popY=new float[8];private final int[] popAge=new int[8];private final String[] popText=new String[8];private int popNext;

    static boolean is(int id){return id>=Challenges.WAKE_PATROL && id<=Challenges.DEEP_FREEZE;}
    static MontroseJob create(int id){
        switch(id){
            case Challenges.WAKE_PATROL:return new WakePatrolJob();
            case Challenges.LOOSE_BOATS:return new LooseBoatsJob();
            case Challenges.WATER_TAXI:return new WaterTaxiJob();
            case Challenges.SANDBAR:return new SandbarJob();
            case Challenges.REGATTA:return new RegattaJob();
            case Challenges.DEEP_FREEZE:return new DeepFreezeJob();
            default:return new FireworksNightJob();
        }
    }
    /** The job after this one in the Montrose day; -1 ends the location. */
    static int next(int id){return id==Challenges.SWIMMER_RESCUE?Challenges.WAKE_PATROL:is(id) && id<Challenges.DEEP_FREEZE?id+1:-1;}

    abstract void setup(Game g);
    abstract String status(Game g);
    /** The off-screen arrow to whatever the player should head for next. */
    abstract void objective(Game g,Painter canvas);
    static void arrow(Game g,Painter canvas,float x,float y,String label){HarborView.destination(g,canvas,x,y,label);}
    void step(Game g){ticks++;for(int i=0;i<popAge.length;i++)if(popAge[i]>0)popAge[i]++;}
    void motion(Game g){}
    /** Hulls this job owns outside the cargo slots (drawn, floated and collided like traffic). */
    Body[] bodies(){return NONE;}
    static final Body[] NONE=new Body[0];
    /** Whether the tow line may take this cargo hull now. */
    boolean towable(Body b){return !b.deliveryRecorded;}
    boolean failed(Game g){return limit>0 && ticks>=limit && g.delivered<g.quota();}
    /** Whether the job is finished (hands off to the next job). */
    boolean done(Game g){return g.delivered>=g.quota();}
    /** Stars for the finished job. */
    int stars(Game g){return Challenges.stars(g.challengeId,g.delivered,g.lost,g.hull);}
    int secondsLeft(){return limit<=0?0:Math.max(0,(limit-ticks+59)/60);}
    void water(Game g,Painter p){}
    void overlay(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        for(int i=0;i<popAge.length;i++){
            if(popAge[i]<=0 || popAge[i]>70)continue;
            int a=popAge[i];float u=a/70f,pop=a<6?1.45f-a*.075f:1,size=Math.min(28,Math.max(15,18*c.scale))*pop;
            float x=c.screenX(popX[i],popY[i]),y=c.screenY(popX[i],popY[i])-(30+u*36)*c.scale;
            float alpha=1-Physics.clamp((a-45)/25f,0,1);
            Lettering.center(p,popText[i],x+1.5f,y+1.5f,size,SoftCue.alpha(0xff0a1016,.8f*alpha));
            Lettering.center(p,popText[i],x,y,size,SoftCue.alpha(0xffffe27a,alpha));
        }
    }
    /** Countdown under the objective banner; pulses red in the last ten seconds. */
    void hud(Game g,Painter p){
        if(limit<=0 || g.mode!=Game.PLAY)return;
        int left=secondsLeft();String text=left/60+":"+(left%60<10?"0":"")+left%60;
        boolean hurry=left<=10;float pulse=hurry?.5f+.5f*(float)StrictMath.sin(g.visualTicks*.3f):0;
        float x=g.layout.width-84,y=66,size=hurry?26+4*pulse:24;
        Lettering.center(p,text,x+1.5f,y+1.5f,size,0xcc0a1016);
        Lettering.center(p,text,x,y,size,hurry?0xffff5a4a:0xffffe27a);
    }

    /** One unit of job progress: score, cheer, pop and the shared delivery count. */
    void score(Game g,float x,float y,int points,String cheer){
        g.delivered++;g.score+=points;g.haptic(Game.HAPTIC_SUCCESS);g.sounds.complete();
        pop(x,y,"+"+points);
        g.tell(cheer+" / "+g.delivered+" OF "+g.quota());
    }
    void pop(float x,float y,String text){int i=popNext++%popAge.length;popX[i]=x;popY[i]=y;popText[i]=text;popAge[i]=1;}
    /** A soft pulsing target ring on the water. */
    static void ring(Game g,Painter p,float x,float y,float radius,int color,float strength){
        RiverCamera c=g.layout.camera;float sx=c.screenX(x,y),sy=c.screenY(x,y),r=radius*c.scale*RiverCamera.Q*2;
        float breathe=.85f+.15f*(float)StrictMath.sin(g.visualTicks*.09f+x*.01f);
        if(sx<-r*1.4f || sx>g.layout.width+r*1.4f || sy<-r || sy>g.layout.height+r)return;
        p.material(sx,sy,r*2*breathe,0,0,r*2*breathe*RiverCamera.DEPTH,EffectMaterial.RING,.05f,1,SoftCue.alpha(color,.75f*strength));
        p.material(sx,sy,r*1.6f,0,0,r*1.6f*RiverCamera.DEPTH,EffectMaterial.GLOW,0,1,SoftCue.alpha(color,.18f*strength));
    }
    /** Hull-to-hull contacts for this job's own hulls against the tug, cargo and each other. */
    static float collide(Game g,Body[] hulls,CollisionSplashes splashes){
        float hit=0;
        for(int i=0;i<hulls.length;i++){
            Body a=hulls[i];if(a==null || !a.active)continue;
            if(a.traffic)HarborTraffic.smash(g.tug,a,splashes);else hit=Math.max(hit,Physics.collide(g.tug,a,splashes));
            for(Body o:g.cargo)if(o!=null && o.active)Physics.collide(o,a,splashes);
            for(int j=i+1;j<hulls.length;j++)if(hulls[j]!=null)Physics.collide(a,hulls[j],splashes);
        }
        return hit;
    }
    /** Places the tug and frames it for a fresh job. */
    static void start(Game g,float x,float y,float angle){
        g.tug.x=x;g.tug.y=y;g.tug.angle=angle;g.tug.vx=g.tug.vy=g.tug.spin=0;
        g.layout=Layout.focused(g.layout.width,g.layout.height,x,y,Game.TRANSIT_ZOOM*MontroseHarbor.ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    /** Ties a hull in place (frozen by the shared motion step) and closes its delivery. */
    static void secure(Body b,float x,float y,float angle){
        b.x=b.tiedX=x;b.y=b.tiedY=y;b.angle=b.tiedAngle=angle;b.vx=b.vy=b.spin=0;b.awaitingPickup=true;b.deliveryRecorded=true;
    }
}
