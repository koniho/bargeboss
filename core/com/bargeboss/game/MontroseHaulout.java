package com.bargeboss.game;

/** Radio clearance, continuous sling lowering, then a physical push into the well. */
public final class MontroseHaulout {
    public static final float X=MontroseHarbor.mapX(1060),Y=MontroseHarbor.mapY(240),
            APPROACH_Y=MontroseHarbor.mapY(310),ANGLE=-(float)Math.PI/2;
    public static final int LOWER_TICKS=240;
    public boolean towed,warping,requested,blocked,pushed,approachAligned;
    public int liftTicks,hoistTicks;
    public static final int HOIST_TICKS=180;
    private Body liftedBoat,parkedBoat;
    static final int TRAVEL_TICKS=360,YARD_TICKS=300,RETURN_TICKS=360;
    static final float YARD_OFFSET=-110;
    int carryTicks,returnTicks;
    boolean returning,readyAgain;
    boolean carrying(){return liftedBoat!=null && hoistTicks>=HOIST_TICKS;}
    float travelY(){return rig==null?0:rig.offsetY;}
    private static float ease(float t){return t*t*(3-2*t);}
    private static void pin(Body b,float x,float y){b.x=x;b.y=y;b.angle=b.tiedAngle;b.vx=b.vy=b.spin=0;}

    public boolean hoisting(){return liftedBoat!=null && hoistTicks<HOIST_TICKS;}
    public float vesselRise(Body b){return b==parkedBoat?MontroseHarbor.LAND_RISE+1:b==liftedBoat?Math.max(0,64*MontroseTravelLift.SCALE*.65f*hoistTicks/HOIST_TICKS-9)*ArticulatedModel.Z_SCALE:0;}
    final DockLineCrew lineCrew=new DockLineCrew(true);
    public final TravelLiftRadio radio=new TravelLiftRadio();
    public final TravelLiftEffects effects=new TravelLiftEffects();
    MontroseTravelLift.Rig rig;
    public boolean lowered(){return liftTicks==LOWER_TICKS;}
    public float lowerAmount(){return liftTicks/(float)LOWER_TICKS;}
    /** All three hull discs, including overhang; inactive/retained craft do not get exemptions. */
    public static boolean inClearance(Body b){
        if(b==null || !b.active)return false;
        for(int i=0;i<3;i++){
            float x=b.discX(i),y=b.discY(i),cx=Physics.clamp(x,X-37,X+37),cy=Physics.clamp(y,Y-55,MontroseHarbor.mapY(298));
            float dx=x-cx,dy=y-cy;if(dx*dx+dy*dy<=(b.radius+4)*(b.radius+4))return true;
        }return false;
    }
    public static boolean clear(Game g){
        if(inClearance(g.tug))return false;
        for(Body b:g.cargo)if(inClearance(b))return false;
        for(MooredFleet.FloatBoat b:g.mooredFleet.boats){if(!b.afloat())continue;float r=b.berth.length*.6f+4;
            if(Math.abs(b.x-X)<37+r && b.y+r>Y-55 && b.y-r<MontroseHarbor.mapY(298)
                    && PolygonWater.rectangle(b.hull,X-41,Y-59,X+41,MontroseHarbor.mapY(298)+4).length>=6)return false;
        }
        return true;
    }
    public int settled;
    static MooredBoats.Berth pickupBerth(){
        int star=0;float far=-1;for(int i=0;i<MontroseHarbor.STAR_CENTERS.length;i++){float[] p=MontroseHarbor.STAR_CENTERS[i];float d=Physics.length(p[0]-X,p[1]-Y);if(d>far){far=d;star=i;}}
        for(MooredBoats.Berth b:MooredBoats.BERTHS)if(b.star==star && b.arm==3)return b;
        throw new IllegalStateException("Missing farthest-star haulout berth");
    }
    static boolean reserved(MooredBoats.Berth b){return b==pickupBerth();}
    boolean inLift(Body b){return b!=null && (b==liftedBoat || b==parkedBoat);}

    public static boolean active(Game g){return g.tutorial==null && g.challengeId==Challenges.HAULOUT;}
    public void setup(Game g){
        rig=new MontroseTravelLift.Rig();
        g.tug.x=MontroseHarbor.mapX(1090);g.tug.y=MontroseHarbor.mapY(350);g.tug.angle=ANGLE;
        MooredBoats.Berth berth=pickupBerth();float extra=(56-berth.length)*.5f;
        Body b=new Body(g.nextId++,2,berth.x+(float)StrictMath.cos(berth.angle)*extra,berth.y+(float)StrictMath.sin(berth.angle)*extra,berth.angle,true,true,true);
        b.voyageJob=Challenges.HAULOUT;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;g.cargo[0]=b;
        g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y+35,Game.TRANSIT_ZOOM*MontroseHarbor.ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    public String status(){
        if(returning)return "LIFT RETURNING / KEEP THE WELL CLEAR";
        if(liftedBoat!=null)return hoisting()?"HOISTING / KEEP CLEAR OF THE LIFT":"CARRYING BOAT TO YARD / KEEP CLEAR";
        if(!requested)return "TAP TRAVEL LIFT TO RADIO / KEEP THE WELL CLEAR";
        if(!lowered())return blocked?"LIFT STOPPED / CLEAR ALL BOATS FROM THE WELL":"LOWERING STRAPS / STAY OUT OF THE WELL";
        if(!towed)return "STRAPS DOWN / TOW AIN'T BROKE FROM STAR DOCK S";
        return warping?"DOCK CREW SECURING BOAT":"RELEASE TOW / HOLD BOAT AND PUSH INTO STRAPS";
    }
    public static boolean fits(Body b){
        for(int i=0;i<3;i++)if(Math.abs(b.discX(i)-X)+b.radius>27*MontroseTravelLift.SCALE || Math.abs(b.discY(i)-Y)+b.radius>45*MontroseTravelLift.SCALE)return false;
        return Math.abs(StrictMath.cos(b.angle))<.22f;
    }
    /** Crew capture is wider than final hoist registration: the center must enter
     * the well, the full hull must fit between its sides, and either end may lead. */
    static boolean captureArea(Body b){
        if(b.y<Y-35 || b.y>Y+45 || Math.abs(StrictMath.cos(b.angle))>.5f)return false;
        for(int i=0;i<3;i++)if(Math.abs(b.discX(i)-X)+b.radius>27*MontroseTravelLift.SCALE+2)return false;
        return true;
    }
    public void step(Game g){
        lineCrew.step();
        if(parkedBoat!=null)pin(parkedBoat,parkedBoat.tiedX,parkedBoat.tiedY);
        if(returning){
            returnTicks++;rig.update(.35f*(1-ease(Math.min(1,returnTicks/90f))));rig.travel(YARD_OFFSET*(1-ease(Math.min(1,returnTicks/(float)RETURN_TICKS))));
            if(returnTicks%12==0)MontroseTravelLift.exhaust(g);
            if(returnTicks==RETURN_TICKS){returning=false;readyAgain=true;requested=blocked=warping=pushed=towed=approachAligned=false;liftTicks=hoistTicks=settled=0;rig.update(0);}
            return;
        }
        if(liftedBoat!=null){
            Body b=liftedBoat;pin(b,b.tiedX,b.tiedY+travelY());
            if(hoistTicks<HOIST_TICKS){
                hoistTicks++;rig.update(1-.65f*hoistTicks/HOIST_TICKS);
                if(hoistTicks%10==0)MontroseTravelLift.exhaust(g);
                if(hoistTicks==HOIST_TICKS){
                    b.deliveryRecorded=true;g.delivered=1;g.score+=1200;g.celebrationTicks=90;g.celebrationPoints=1200;g.celebrationX=b.x;g.celebrationY=b.y;
                    g.haptic(Game.HAPTIC_SUCCESS);g.tell("BOAT LIFTED CLEAR / HAULOUT COMPLETE");
                }
            }else{
                carryTicks++;
                if(carryTicks<=TRAVEL_TICKS){rig.travel(YARD_OFFSET*ease(carryTicks/(float)TRAVEL_TICKS));pin(b,b.tiedX,b.tiedY+travelY());if(carryTicks%12==0)MontroseTravelLift.exhaust(g);}
                // Set the boat on the yard supports, then raise empty slings before returning.
                if(carryTicks==TRAVEL_TICKS+YARD_TICKS){parkedBoat=b;b.tiedX=b.x;b.tiedY=b.y;liftedBoat=null;returning=true;returnTicks=0;}

            }
            return;
        }
        if(!active(g))return;
        if(requested && !lowered()){
            boolean occupied=!clear(g);
            if(occupied && !blocked){g.tell("DOCKMASTER: STOPPING LIFT. CLEAR THE WELL.");g.radio.receiveDockmaster();}
            blocked=occupied;
            if(!blocked){float before=lowerAmount();liftTicks++;rig.update(lowerAmount());effects.lowering(g,before,lowerAmount());
                if(lowered()){g.tell("DOCKMASTER: STRAPS DOWN. RELEASE AND PUSH THE BOAT IN.");g.radio.receiveDockmaster();}}
        }
        Body b=g.cargo[0];if(b==null || !b.active || b.deliveryRecorded)return;
        if(g.tow.connected(b,g.tug))towed=true;
        float distance=Physics.length(b.x-X,b.y-Y);
        if(lowered() && g.tow.incoming(b)==null && Physics.touching(g.tug,b,5) && g.tug.engineLoad>.1f)pushed=true;
        if(lowered() && towed && !warping && g.tow.incoming(b)==null && b.speed()<28 && captureArea(b)
                && MontroseHarbor.clear(b.x,b.y,X,Y)){
            warping=true;lineCrew.start(b);b.awaitingPickup=false;g.tell("DOCKMASTER: CREW HAS YOUR LINE. KEEP CLEAR.");g.radio.receiveDockmaster();
        }
        if(warping && !lineCrew.caught()){
            b.vx*=.92f;b.vy*=.92f;b.spin*=.9f;return;
        }
        if(warping){
            if(g.tow.incoming(b)!=null)warping=false;
            else {
                float speed=Math.min(12,distance*.8f),denom=Math.max(1,distance);
                b.vx=(X-b.x)/denom*speed;b.vy=(Y-b.y)/denom*speed;
                float heading=ANGLE;if(Math.abs(Physics.angle(heading-b.angle))>Math.PI/2)heading+=(float)Math.PI;
                b.spin=Physics.clamp(Physics.angle(heading-b.angle)*.8f,-.3f,.3f);
            }
        }
        if(warping && fits(b) && distance<8 && b.speed()<8 && Math.abs(b.spin)<.3f && g.tow.incoming(b)==null)settled++;else settled=0;
        if(settled==60){
            lineCrew.finish();liftedBoat=b;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;
            g.cancelInputs();g.tell("DOCKMASTER: STRAPS SECURE. LIFTING NOW!");g.radio.receiveDockmaster();
        }
    }
    public static void cue(Game g,Painter p){
        int color=SoftCue.animated(g,g.haulout.lowered()?0xbb75cfb2:0xbbffe69c);
        for(int side=-1;side<=1;side+=2){float x=X+side*23;
            SoftCue.line(p,x,APPROACH_Y-24,x,APPROACH_Y+24,1.2f,color);
            SoftCue.line(p,x,APPROACH_Y-24,x-side*10,APPROACH_Y-24,1.2f,color);
        }
    }
}
