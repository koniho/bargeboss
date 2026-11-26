package com.bargeboss.game;

/** Montrose's live pleasure-boat traffic: cruisers working the marina fairways and out through the
 * inlet, plus cruisers and offshore racers on the open lake. Each craft is a real {@link Body}: it
 * collides, takes damage, sinks, throws wakes and is drawn by {@link CruiserView} in its own hull kind.
 *
 * Autopilot (fixed step, deterministic): steer for the next route waypoint, drive along the keel,
 * slow for anything in the lane ahead, swerve right around other traffic, and stop and shout at a tug
 * or tow that is in the way. Routes are either loops or ping-pong lanes (reversing at the ends after a
 * short idle). Traffic exists only on the Montrose stage outside the tutorial. */
final class HarborTraffic {
    /** Hull kinds index {@link MooredBoats#length}; 6 is the offshore racer. */
    static final int RACER=6;
    static final class Craft {
        final Body body;final float[] route;final boolean loop;final float cruise;
        int leg,dir=1,idle,blocked,shoutTicks,shoutCooldown;String shout="";
        Craft(Body body,float[] route,boolean loop,float cruise,int leg){this.body=body;this.route=route;this.loop=loop;this.cruise=cruise;this.leg=leg;}
        float targetX(){return route[leg*2];}
        float targetY(){return route[leg*2+1];}
        int points(){return route.length/2;}
    }
    // Fairway lanes inside the marina, then the open lake. All verified clear by HarborTrafficTest.
    static final float[] INLET_RUN={900,1000,146,630,-82,-30,746,-474,1442,-666,1736,-942};
    static final float[] NORTH_RUN={-60,-100,590,-558,590,-678,446,-870};
    static final float[] MOUTH={-80,400,2,-102,602,-474};
    static final float[] RACE_LOOP={250,800,1800,1000,3100,800,3400,-500,3250,-2600,2950,-1200,2750,300,1500,800};
    static final float[] SOUTH_LANE={100,980,3300,980};
    static final float[] EAST_LANE={3150,700,2858,-3522,2678,-3594,2,-4254};
    static final float[] NORTH_LANE={1200,-3200,3300,-3900};
    static final float[] SHORE_LANE={400,720,2300,560};

    Craft[] craft=new Craft[0];

    void reset(Game g){
        if(!active(g)){craft=new Craft[0];return;}
        craft=new Craft[]{
            make(g,INLET_RUN,false,2,58,0),make(g,INLET_RUN,false,5,52,5),make(g,INLET_RUN,false,0,50,3),
            make(g,NORTH_RUN,false,4,46,0),make(g,NORTH_RUN,false,3,42,3),make(g,MOUTH,false,1,50,2),
            make(g,RACE_LOOP,true,RACER,150,0),make(g,RACE_LOOP,true,RACER,140,4),
            make(g,SOUTH_LANE,false,1,72,0),make(g,EAST_LANE,false,5,78,1),
            make(g,NORTH_LANE,false,2,70,1),make(g,SHORE_LANE,false,3,64,0)
        };
    }
    static boolean active(Game g){return g.montroseStage();}
    private static Craft make(Game g,float[] route,boolean loop,int kind,float cruise,int start){
        int n=route.length/2,from=Math.min(start,n-1),to=loop?(from+1)%n:Math.min(from+1,n-1);
        if(to==from)to=from-1;
        float x=route[from*2],y=route[from*2+1],angle=(float)StrictMath.atan2(route[to*2+1]-y,route[to*2]-x);
        Body b=new Body(g.nextId++,-1,x,y,angle,false,true,kind==RACER);
        b.traffic=true;b.hullKind=kind;b.vx=(float)StrictMath.cos(angle)*cruise*.6f;b.vy=(float)StrictMath.sin(angle)*cruise*.6f;
        Craft c=new Craft(b,route,loop,cruise,to);c.dir=to>from || loop?1:-1;
        return c;
    }

    /** Autopilot, motion and shore/marina collisions for one fixed step. */
    void step(Game g,CollisionSplashes splashes){
        float[] view=craft.length>0?SurveyGround.visible(g):null;
        for(Craft c:craft){
            Body b=c.body;
            if(c.shoutTicks>0)c.shoutTicks--;
            if(c.shoutCooldown>0)c.shoutCooldown--;
            if(!b.active)continue;
            if(b.sinkTicks<=0)pilot(g,c);
            Physics.integrate(b);
            // Lanes are verified clear of shore, docks and hulls; only craft near the view need the full wall test.
            if(HarborTrafficView.near(view,b,500) || b.sinkTicks>0)MontroseHarbor.collide(g,b,splashes);
        }
    }
    private void pilot(Game g,Craft c){
        Body b=c.body;
        float tx=c.targetX(),ty=c.targetY(),dx=tx-b.x,dy=ty-b.y,d=Physics.length(dx,dy);
        if(d<(b.racing?70:38))advance(c);
        if(c.idle>0){c.idle--;brake(b);return;}
        tx=c.targetX();ty=c.targetY();dx=tx-b.x;dy=ty-b.y;d=Math.max(1,Physics.length(dx,dy));
        float cos=(float)StrictMath.cos(b.angle),sin=(float)StrictMath.sin(b.angle);
        // Look down the lane for hulls: stop for the tug and tows, swerve right around other traffic.
        float look=70+c.cruise*.9f,limit=1,swerve=0;Body blocker=null;
        limit=Math.min(limit,clearance(b,g.tug,cos,sin,look));if(limit<1)blocker=g.tug;
        for(Body o:g.cargo)if(o!=null && o.active){float l=clearance(b,o,cos,sin,look);if(l<limit){limit=l;blocker=o;}}
        for(Craft other:craft)if(other!=c && other.body.active){
            float l=clearance(b,other.body,cos,sin,look);
            if(l<1){swerve=1;limit=Math.min(limit,Math.max(.35f,l));}
        }
        if(blocker!=null && limit<.5f){
            c.blocked++;
            // A skipper kept waiting edges around the obstruction instead of deadlocking with it.
            if(c.blocked>150){limit=Math.max(limit,.4f);swerve=1;}
            if(c.blocked>45 && c.shoutCooldown==0)shout(c,COMPLAINTS[(b.id+c.blocked/45)%COMPLAINTS.length]);
        }else c.blocked=0;
        float desired=(float)StrictMath.atan2(dy,dx)-swerve*.55f,error=Physics.angle(desired-b.angle);
        float turn=b.racing?3.2f:4.5f;
        b.spin+=Physics.clamp(error*turn-b.spin*2.6f,-6,6)*Physics.DT;
        float speed=c.cruise*limit*Math.max(.2f,1-Math.abs(error)*.55f);
        // Ease in to the end of a ping-pong lane.
        if(!c.loop && (c.leg==0 || c.leg==c.points()-1))speed=Math.min(speed,(float)StrictMath.sqrt(d*60));
        float dvx=cos*speed-b.vx,dvy=sin*speed-b.vy,force=Physics.length(dvx,dvy),max=(b.racing?140:70)*Physics.DT;
        if(force>max){dvx*=max/force;dvy*=max/force;}
        b.vx+=dvx;b.vy+=dvy;
    }
    private static void brake(Body b){b.vx*=.96f;b.vy*=.96f;b.spin*=.9f;}
    /** 1 when o is clear of b's lane ahead; falls to 0 as o closes inside the stopping distance. */
    private static float clearance(Body b,Body o,float cos,float sin,float look){
        float rx=o.x-b.x,ry=o.y-b.y,ahead=rx*cos+ry*sin;
        if(ahead<=0 || ahead>look+o.half)return 1;
        float lateral=Math.abs(-rx*sin+ry*cos);
        if(lateral>b.radius+o.radius+o.half*.5f+20)return 1;
        float stop=b.half+o.half+18;
        return Physics.clamp((ahead-stop)/Math.max(1,look-stop),0,1);
    }
    private static void advance(Craft c){
        int n=c.points();
        if(c.loop){c.leg=(c.leg+1)%n;return;}
        int next=c.leg+c.dir;
        if(next<0 || next>=n){c.dir=-c.dir;c.idle=150;next=c.leg+c.dir;}
        c.leg=next;
    }
    static final String[] COMPLAINTS={"HONK HONK!","MOVE IT, TUG!","RIGHT OF WAY!","COMING THROUGH!","OUT OF THE LANE!","BEEP BEEP!"};
    static final String[] RAMMED={"MY BOAT!","WATCH THE PAINT!","HEY! I JUST WAXED IT!","YOU'LL PAY FOR THAT!","OW!"};
    private static void shout(Craft c,String words){c.shout=words;c.shoutTicks=110;c.shoutCooldown=260;}
    /** The skipper of a traffic hull the player just damaged yells about it. */
    void rammed(Body b){for(Craft c:craft)if(c.body==b){c.shoutCooldown=0;shout(c,RAMMED[(b.id+b.health)%RAMMED.length]);}}

    /** Contacts between the tug, its cargo and traffic (and traffic with traffic). The tug smashes
     * through pleasure boats: it keeps most of its way and takes no hull damage, while the boat is
     * shoved and spun aside (and charged to the player's property record by HullDamage). Returns 0:
     * traffic never costs the tug hull. */
    float collide(Game g,CollisionSplashes splashes){
        for(int i=0;i<craft.length;i++){
            Body a=craft[i].body;if(!a.active)continue;
            smash(g.tug,a,splashes);
            for(Body o:g.cargo)if(o!=null && o.active && !g.haulout.inLift(o))Physics.collide(o,a,splashes);
            for(int j=i+1;j<craft.length;j++)Physics.collide(a,craft[j].body,splashes);
        }
        return 0;
    }
    /** Share of its pre-contact velocity the tug keeps, and of it handed to the boat it rams. */
    static final float PLOW=.8f,SHOVE=.65f;
    /** The tug rams a light hull: normal contact resolution, then the tug keeps its way and the hull is thrown aside. */
    static float smash(Body tug,Body b,CollisionSplashes splashes){
        float vx=tug.vx,vy=tug.vy,impact=Physics.collide(tug,b,splashes);
        if(impact<=0)return 0;
        tug.vx+=(vx-tug.vx)*PLOW;tug.vy+=(vy-tug.vy)*PLOW;
        b.vx+=(vx-b.vx)*SHOVE;b.vy+=(vy-b.vy)*SHOVE;
        // Spin it away from the side it was struck on.
        float side=(b.x-tug.x)*vy-(b.y-tug.y)*vx;
        b.spin=Physics.clamp(b.spin+Math.signum(side)*Math.min(3,Physics.length(vx,vy)*.03f),-4,4);
        return impact;
    }
    /** Speech above skippers who are complaining. */
    void callouts(Game g,Painter p){
        RiverCamera cam=g.layout.camera;
        for(Craft c:craft){
            if(c.shoutTicks<=0 || !c.body.active)continue;
            Body b=c.body;float sx=cam.screenX(b.x,b.y),sy=cam.screenY(b.x,b.y)-(30*ArticulatedModel.Z_SCALE+WaterSurface.lift(g,b))*cam.scale-26;
            if(sx<-80 || sx>g.layout.width+80 || sy<-20 || sy>g.layout.height+20)continue;
            int age=110-c.shoutTicks;float pop=age<6?1.35f-age*.06f:1,size=Math.min(20,Math.max(12,14*cam.scale))*pop;
            float a=Physics.clamp(c.shoutTicks/20f,0,1);
            Lettering.center(p,c.shout,sx+1.2f,sy+1.2f,size,SoftCue.alpha(0xff0a1016,.8f*a));
            Lettering.center(p,c.shout,sx,sy-(1-a)*8,size,SoftCue.alpha(0xffffe27a,a));
        }
    }
}
