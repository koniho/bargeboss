package com.bargeboss.game;

/** DEEP FREEZE: a blizzard rolls in and the harbor is going to freeze solid. Ten boats are riding it out
 * at anchor around the marina; tow as many as you can into the travel lift's ring before the deep
 * freeze. The lift crew takes each one in, hoists it, swings it to the yard and comes back for the
 * next, so boats queue while it works. Gusts shove everything not on your line, the snow closes into
 * a whiteout, and ice creeps over the water, thickening into slush that drags at the tug.
 * Four boats saves the job; six earn two stars and eight earn three; all ten ends it early. */
final class DeepFreezeJob extends MontroseJob {
    static final int BOATS=10,PASS=4,SILVER=6,GOLD=8,FREEZE_TICKS=200*60,RAMP=40*60;
    /** Lift ring (just off the travel-lift well) and its capture radius. */
    static final float RING_X=MontroseHaulout.X,RING_Y=MontroseHaulout.APPROACH_Y+25,REACH=80;
    /** Lift cycle phases and their lengths. */
    static final int READY=0,WARP=1,HOIST=2,CARRY=3,RETURN=4;
    static final int WARP_TICKS=80,HOIST_TICKS=80,CARRY_TICKS=100,RETURN_TICKS=100;
    static final float YARD=-110;
    final Body[] boats=new Body[BOATS];
    int phase=READY,phaseTicks;Body lifting;float fromX,fromY,fromAngle;boolean frozen;

    void setup(Game g){
        limit=FREEZE_TICKS;
        g.weather.reset();g.weather.blizzard=.05f;
        // Everyone else has run for shelter.
        g.harborTraffic.craft=new HarborTraffic.Craft[0];
        if(g.haulout.rig==null)g.haulout.rig=new MontroseTravelLift.Rig();
        g.haulout.rig.travel(0);g.haulout.rig.update(1);
        float[] at=anchorages();
        for(int i=0;i<BOATS;i++){
            Body b=new Body(g.nextId++,2,at[i*2],at[i*2+1],i*1.3f,false,true);
            b.hullKind=(i*5+1)%6;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;g.cargo[i]=boats[i]=b;
        }
        start(g,RING_X+40,RING_Y+150,(float)Math.PI/2);
    }
    /** Ten anchorages on open fairway water, spread out, away from the lift and from each other. */
    static float[] anchorages(){
        float[] out=new float[BOATS*2];int n=0;
        for(float[] lane:new float[][]{HarborTraffic.INLET_RUN,HarborTraffic.MOUTH,HarborTraffic.NORTH_RUN})
            for(int i=0;i+3<lane.length && n<BOATS;i+=2){
                float ax=lane[i],ay=lane[i+1],bx=lane[i+2],by=lane[i+3],len=Physics.length(bx-ax,by-ay);
                for(float t=60;t<len && n<BOATS;t+=50){
                    float x=ax+(bx-ax)*t/len,y=ay+(by-ay)*t/len;
                    if(y>250 || Physics.length(x-RING_X,y-RING_Y)<320)continue;
                    boolean spaced=true;for(int k=0;k<n;k++)if(Physics.length(x-out[k*2],y-out[k*2+1])<190)spaced=false;
                    if(spaced){out[n*2]=x;out[n*2+1]=y;n++;}
                }
            }
        return out;
    }
    float progress(){return Math.min(1,ticks/(float)FREEZE_TICKS);}
    /** Ice appears at a third of the way and closes in hard at the end. */
    float freeze(){return SeaWaves.smooth(.33f,1,progress());}
    String status(Game g){
        if(frozen)return g.delivered>=PASS?"DEEP FREEZE! "+g.delivered+" BOATS SAFE":"DEEP FREEZE! NOT ENOUGH BOATS SAVED";
        if(phase!=READY)return "LIFT WORKING / BRING THE NEXT BOAT / "+g.delivered+" HAULED";
        for(Body b:boats)if(b!=null && !b.deliveryRecorded && g.tow.connected(b,g.tug))return "TOW IT INTO THE LIFT RING / "+g.delivered+" HAULED";
        return "BLIZZARD! HAUL BOATS TO THE LIFT BEFORE THE FREEZE / "+g.delivered+" HAULED";
    }
    void objective(Game g,Painter canvas){
        for(Body b:boats)if(b!=null && !b.deliveryRecorded && b!=lifting && g.tow.connected(b,g.tug)){arrow(g,canvas,RING_X,RING_Y,phase==READY?"TRAVEL LIFT":"LIFT BUSY / HOLD");return;}
        Body near=null;float best=Float.MAX_VALUE;
        for(Body b:boats){if(b==null || b.deliveryRecorded || b==lifting)continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;near=b;}}
        if(near!=null)arrow(g,canvas,near.x,near.y,"BOAT AT ANCHOR");
    }
    boolean done(Game g){return g.delivered>=BOATS || frozen && g.delivered>=PASS;}
    boolean failed(Game g){return frozen && g.delivered<PASS;}
    int stars(Game g){return g.delivered>=GOLD?3:g.delivered>=SILVER?2:g.delivered>=PASS?1:0;}
    boolean towable(Body b){return !b.deliveryRecorded && b!=lifting;}

    void step(Game g){
        super.step(g);
        StageWeather w=g.weather;
        w.blizzard=Math.min(1,.05f+.95f*ticks/(float)RAMP);w.freeze=freeze();
        if(!frozen && ticks>=FREEZE_TICKS){
            frozen=true;
            RiverCamera c=g.layout.camera;g.splashes.shake.hit(.8f,c.screenX(1,0)-c.screenX(0,0),c.screenY(1,0)-c.screenY(0,0),true);
            g.haptic(Game.HAPTIC_IMPACT);g.tell(g.delivered>=PASS?"DEEP FREEZE! THE HARBOR IS ICED IN":"DEEP FREEZE! TOO MANY BOATS LEFT IN THE ICE");
        }
        lift(g);
        if(phase!=READY || frozen)return;
        for(Body b:boats){
            if(b==null || b.deliveryRecorded || b.awaitingPickup && !g.tow.connected(b,g.tug))continue;
            if(Physics.length(b.x-RING_X,b.y-RING_Y)<REACH && b.speed()<35){
                lifting=b;phase=WARP;phaseTicks=0;fromX=b.x;fromY=b.y;fromAngle=b.angle;g.tow.detachBody(g,b);
                g.tell("LIFT CREW HAS IT / GO GET THE NEXT ONE");g.radio.receiveDockmaster();break;
            }
        }
    }
    /** The travel lift's fast winter cycle: warp in, hoist, swing to the yard, come back with the straps down. */
    private void lift(Game g){
        MontroseTravelLift.Rig rig=g.haulout.rig;if(rig==null || phase==READY)return;
        phaseTicks++;Body b=lifting;
        if(phase==WARP){
            float t=Math.min(1,phaseTicks/(float)WARP_TICKS),e=t*t*(3-2*t);
            float heading=MontroseHaulout.ANGLE;if(Math.abs(Physics.angle(heading-fromAngle))>Math.PI/2)heading+=(float)Math.PI;
            hold(b,fromX+(MontroseHaulout.X-fromX)*e,fromY+(MontroseHaulout.Y-fromY)*e,fromAngle+Physics.angle(heading-fromAngle)*e);
            if(phaseTicks>=WARP_TICKS){phase=HOIST;phaseTicks=0;}
        }else if(phase==HOIST){
            float t=phaseTicks/(float)HOIST_TICKS;rig.update(1-.65f*t);
            b.liftRise=Math.max(.01f,(64*MontroseTravelLift.SCALE*.65f*t-9)*ArticulatedModel.Z_SCALE);
            if(phaseTicks%10==0)MontroseTravelLift.exhaust(g);
            if(phaseTicks>=HOIST_TICKS){phase=CARRY;phaseTicks=0;b.deliveryRecorded=true;
                score(g,b.x,b.y,800+Math.max(0,secondsLeft())*5,g.delivered+1>=BOATS?"EVERY BOAT SAFE!":"HAULED OUT!");}
        }else if(phase==CARRY){
            float t=Math.min(1,phaseTicks/(float)CARRY_TICKS),e=t*t*(3-2*t);rig.travel(YARD*e);hold(b,MontroseHaulout.X,MontroseHaulout.Y+YARD*e,b.tiedAngle);
            if(phaseTicks%12==0)MontroseTravelLift.exhaust(g);
            if(phaseTicks>=CARRY_TICKS){b.active=false;b.liftRise=0;phase=RETURN;phaseTicks=0;lifting=null;}
        }else{
            float t=Math.min(1,phaseTicks/(float)RETURN_TICKS),e=t*t*(3-2*t);rig.travel(YARD*(1-e));rig.update(.35f+.65f*e);
            if(phaseTicks%12==0)MontroseTravelLift.exhaust(g);
            if(phaseTicks>=RETURN_TICKS){phase=READY;phaseTicks=0;g.tell("LIFT READY / STRAPS DOWN");}
        }
    }
    private static void hold(Body b,float x,float y,float angle){b.x=b.tiedX=x;b.y=b.tiedY=y;b.angle=b.tiedAngle=angle;b.vx=b.vy=b.spin=0;b.awaitingPickup=true;}

    void motion(Game g){
        StageWeather w=g.weather;float gust=w.gust(g,g.visualTicks),angle=w.windAngle(g.visualTicks);
        float push=gust*7,fx=(float)StrictMath.cos(angle)*push*Physics.DT,fy=(float)StrictMath.sin(angle)*push*Physics.DT;
        // Gusts shove the tug and every boat not tied or held; slush drags at the tug as the ice closes.
        g.tug.vx+=fx*.6f;g.tug.vy+=fy*.6f;
        float slush=1-.0055f*w.freeze;g.tug.vx*=slush;g.tug.vy*=slush;
        for(Body b:boats){
            if(b==null || !b.active || b.deliveryRecorded || b==lifting || b.awaitingPickup)continue;
            b.vx+=fx;b.vy+=fy;b.vx*=1-.004f*w.freeze;b.vy*=1-.004f*w.freeze;
        }
        if(frozen){g.tug.vx*=.9f;g.tug.vy*=.9f;for(Body b:boats)if(b!=null && b.active && b!=lifting){b.vx*=.85f;b.vy*=.85f;}}
    }
    void water(Game g,Painter p){
        RiverCamera c=g.layout.camera;float f=g.weather.freeze;
        // Ice: a world-anchored field of soft frost floes. Each cell's floe sits anywhere in its cell, at its
        // own angle, size and stretch, so no lattice shows; a coarse noise field makes the freeze creep in
        // drifts (sheltered patches first) rather than cell by cell, until the floes merge into a sheet.
        if(f>0){
            float[] v=SurveyGround.visible(g);float cell=60;
            int c0=(int)StrictMath.floor(v[0]/cell)-2,c1=(int)StrictMath.floor(v[2]/cell)+2,r0=(int)StrictMath.floor(v[1]/cell)-2,r1=(int)StrictMath.floor(v[3]/cell)+2;
            float qx=RiverCamera.Q*c.scale,qy=RiverCamera.Q*RiverCamera.DEPTH*c.scale;
            for(int col=c0;col<=c1;col++)for(int row=r0;row<=r1;row++){
                int h=iceHash(col,row,1);float seed=.45f*(h&1023)/1023f+.55f*drift(col,row);
                float grown=Physics.clamp((f*1.35f-seed)*2.6f,0,1);if(grown<=0)continue;
                int k=iceHash(col,row,2);
                float x=(col+(k&1023)/1023f)*cell,y=(row+((k>>>10)&1023)/1023f)*cell;
                if(!MontroseHarbor.contains(x,y))continue;
                float size=.35f+1.05f*((k>>>20)&255)/255f,r=cell*size*(.55f+.6f*grown);
                float stretch=1+.8f*((h>>>10)&255)/255f,angle=((h>>>18)&1023)*(float)(StrictMath.PI/1024);
                float ux=(float)StrictMath.cos(angle),uy=(float)StrictMath.sin(angle);
                // World axes (long u, short v) projected to screen.
                float ax=qx*(ux+uy)*r*stretch,ay=qy*(uy-ux)*r*stretch,bx=qx*(ux-uy)*r,by=qy*(ux+uy)*r;
                float sx=c.screenX(x,y),sy=c.screenY(x,y),alpha=(.26f+.22f*((k>>>28)&15)/15f)*grown;
                p.material(sx,sy,ax*1.6f,ay*1.6f,bx*1.6f,by*1.6f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffdcebf5,alpha));
                // Only some floes get a brighter, off-centre crust, so highlights don't repeat.
                if(((h>>>28)&3)==0)p.material(sx+ax*.09f,sy+ay*.09f,ax*.7f,ay*.7f,bx*.6f,by*.6f,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xfff6fbff,.28f*grown));
            }
        }
        if(!frozen)ring(g,p,RING_X,RING_Y,REACH*.7f,phase==READY?0xff6dffb0:0xffff6f5a,phase==READY?1:.6f);
        for(Body b:boats)if(b!=null && b.active && !b.deliveryRecorded && b!=lifting && !g.tow.connected(b,g.tug))ring(g,p,b.x,b.y,32,0xff8fd0ff,.6f);
    }
    private static int iceHash(int col,int row,int salt){int h=(col*7919+row*104729+salt*15485863)*0x45d9f3b;h^=h>>>16;h*=0x45d9f3b;return h^(h>>>16);}
    /** Smooth 0..1 value noise over ~5-cell blocks: where the ice forms early. */
    private static float drift(int col,int row){
        int bc=Math.floorDiv(col,5),br=Math.floorDiv(row,5);float fx=(col-bc*5)/5f,fy=(row-br*5)/5f;
        fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
        float a=(iceHash(bc,br,3)&1023)/1023f,b=(iceHash(bc+1,br,3)&1023)/1023f,c=(iceHash(bc,br+1,3)&1023)/1023f,d=(iceHash(bc+1,br+1,3)&1023)/1023f;
        return a+(b-a)*fx+(c-a)*fy+(a-b-c+d)*fx*fy;
    }
    void hud(Game g,Painter p){
        super.hud(g,p);
        if(g.mode!=Game.PLAY)return;
        // Thermometer: falls toward the deep freeze.
        float x=g.layout.width-150,y=86,w=132,t=1-progress();
        p.rect(x-2,y-2,w+4,10,0xe6050a10);p.rect(x,y,w,6,0xcc0e1a24);
        p.rect(x,y,w*t,6,t<.25f?0xffff5a4a:t<.5f?0xffffd166:0xff8fd0ff);
        Lettering.center(p,"FREEZE",x+w*.5f,y+18,11,0xffcfeefa);
        Lettering.center(p,g.delivered+" HAULED",x+w*.5f,y+34,13,0xffffe27a);
    }
}
