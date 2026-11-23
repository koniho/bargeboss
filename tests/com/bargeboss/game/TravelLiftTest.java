package com.bargeboss.game;

/** Operator interlock, continuous webbing transforms and bounded water-contact effects. */
public final class TravelLiftTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Game scene(){Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.montroseArrival.clear();g.mission.setup(g);g.dialogue.close(g);return g;}
    static void call(Game g){ReplayInput.tap(g,TravelLiftRadio.x(g),TravelLiftRadio.y(g));check(g.haulout.radio.waiting(),"Tap transmits request and waits for operator");for(int i=0;i<65;i++)g.step();check(g.dialogue.ready(),"Operator offers a selectable radio response");}
    static void reply(Game g,int choice){Layout.Box b=g.dialogue.choiceBox(g,choice);ReplayInput.tap(g,b.centerX(),b.centerY());for(int i=0;i<40;i++)g.step();}
    public static void run(){
        Game g=scene();for(int i=0;i<300;i++)g.step();check(g.haulout.liftTicks==0,"No motion without explicit radio authorization");
        float x=TravelLiftRadio.x(g),y=TravelLiftRadio.y(g);g.pointerDown(9,x,y);g.pointerCancel(9);g.pointerUp(9,x,y);check(!g.haulout.radio.waiting(),"Canceled press cannot call");
        g.pointerDown(9,x,y);g.pointerMove(9,x+100,y);g.pointerUp(9,x,y);check(!g.haulout.radio.waiting(),"Drag away cancels call");
        call(g);reply(g,1);check(!g.haulout.requested,"Stand by never authorizes lowering");
        Body b=g.cargo[0];b.awaitingPickup=false;float bx=b.x,by=b.y;b.x=MontroseHaulout.X;b.y=MontroseHaulout.Y;
        call(g);check(g.dialogue.content().title.contains("NOT CLEAR"),"Occupied well receives refusal");reply(g,0);check(!g.haulout.requested,"Acknowledging refusal does not authorize motion");
        b.x=bx;b.y=by;call(g);b.x=MontroseHaulout.X;b.y=MontroseHaulout.Y;reply(g,0);check(!g.haulout.requested,"Clearance rechecked when player confirms");
        b.x=bx;b.y=by;call(g);reply(g,0);check(g.haulout.requested && g.haulout.liftTicks>0,"Clear radio confirmation starts lowering");
        int phase=g.haulout.liftTicks,puffs=g.haulout.effects.exhaustPuffs,ripples=g.haulout.effects.emitted;float tx=g.tug.x,ty=g.tug.y;
        g.tug.x=MontroseHaulout.X;g.tug.y=MontroseHaulout.Y;
        for(int i=0;i<120;i++)g.haulout.step(g);
        check(g.haulout.blocked && g.haulout.liftTicks==phase,"Entering well pauses at exact phase");
        check(g.haulout.effects.exhaustPuffs==puffs && g.haulout.effects.emitted==ripples,"Stopped hoist emits no new motion effects");
        g.tug.x=tx;g.tug.y=ty;while(!g.haulout.lowered())g.step();check(!g.haulout.blocked,"Clearing well resumes lowering");
        check(g.haulout.effects.emitted>=6 && g.haulout.effects.emitted<=TravelLiftEffects.CAPACITY,"Both straps emit bounded water-crossing ripples");
        check(g.haulout.effects.exhaustPuffs==24,"Exhaust emitted only during all 240 moving ticks");
        puffs=g.haulout.effects.exhaustPuffs;ripples=g.haulout.effects.emitted;for(int i=0;i<150;i++)g.step();
        check(g.haulout.effects.exhaustPuffs==puffs && g.haulout.effects.emitted==ripples,"No endless effects after lowering");
        b.x=MontroseHaulout.X;b.y=MontroseHarbor.mapY(298)+b.half+b.radius-2;b.angle=MontroseHaulout.ANGLE;
        check(MontroseHaulout.inClearance(b),"Hull overhang blocks even with center outside");
        b.deliveryRecorded=true;check(!MontroseHaulout.clear(g),"Retained delivered craft still block lowering");
        b.active=false;check(!MontroseHaulout.inClearance(b),"Inactive hull has no occupancy");
        MooredFleet.FloatBoat npc=g.mooredFleet.boats[0];float ox=npc.x,oy=npc.y;
        npc.x=MontroseHaulout.X;npc.y=MontroseHaulout.Y;
        for(int i=0;i<npc.hull.length;i+=2){npc.hull[i]+=npc.x-ox;npc.hull[i+1]+=npc.y-oy;}
        check(!MontroseHaulout.clear(g),"Moored NPC hull blocks the operating area");
        MontroseTravelLift.Rig rig=new MontroseTravelLift.Rig();ArticulatedModel.Pose live=rig.pose;int fragments=rig.motion.fragmentCount();float z=rig.anchor(0,0,0)[2];
        for(int tick=1;tick<=240;tick++){float amount=tick/240f;rig.update(amount);float[] a=rig.anchor(-26,24,0),c=rig.anchor(26,24,0);
            check(rig.pose==live && rig.motion.fragmentCount()==fragments,"Topology and live pose storage remain bounded");
            check(Math.abs(c[0]-a[0]-52)<.001f && Math.abs(c[2]-a[2])<.001f,"Sling width and hoist alignment stay rigid");
            check(Math.abs(z-a[2]-64/240f)<.001f,"Webbing moves continuously every tick");z=a[2];}
        for(float amount:new float[]{.12345f,.501f,.499f,1,0}){rig.update(amount);float[] a=rig.anchor(14,-24,-12),c=MontroseTravelLift.STRAPS.pose(amount).point(MontroseTravelLift.STRAP_ROOT,14,-24,-12);
            for(int i=0;i<3;i++)check(Math.abs(a[i]-c[i])<.001f,"Live anchor matches exact transform including reversal");}
        g=scene();g.haulout.requested=true;for(int i=0;i<120;i++)g.step();check(g.haulout.effects.emitted==0 && g.haulout.effects.exhaustPuffs==12,"Smoke starts with motor; ripples wait for water contact");
        phase=g.haulout.liftTicks;g.background();for(int i=0;i<90;i++)g.advance(1f/60);check(g.haulout.liftTicks==phase,"Background pause cannot advance mechanism");
        System.out.println(checks+" travelift radio, interlock, continuous straps and water-contact checks passed");
    }
    public static void main(String[] args){run();}
}
