package com.bargeboss.game;

/** Operator interlock, continuous webbing transforms and bounded water-contact effects. */
public final class TravelLiftTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static Game scene(){Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.montroseArrival.clear();g.mission.setup(g);g.dialogue.close(g);return g;}
    static void call(Game g){ReplayInput.tap(g,TravelLiftRadio.x(g),TravelLiftRadio.y(g));check(g.haulout.radio.waiting(),"Tap transmits request and waits for operator");for(int i=0;i<65;i++)g.step();check(g.dialogue.ready(),"Operator offers a selectable radio response");}
    static void reply(Game g,int choice){Layout.Box b=g.dialogue.choiceBox(g,choice);ReplayInput.tap(g,b.centerX(),b.centerY());for(int i=0;i<40;i++)g.step();}
    private static final class Clips implements Painter {
        int pushes,pops,draws;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){draws++;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){draws++;}
        public void pushClipOutside(float[] triangles){pushes++;}
        public void popClip(){pops++;}
        public void rect(float x,float y,float w,float h,int color){draws++;}
        public void circle(float x,float y,float r,int color){draws++;}
        public void poly(float[] xy,int color){draws++;}
        public void line(float x,float y,float xx,float yy,float w,int color){draws++;}
    }
    private static boolean covered(float[] triangles,float x,float y){
        for(int i=0;i<triangles.length;i+=6){float[] q={triangles[i],triangles[i+1],triangles[i+2],triangles[i+3],triangles[i+4],triangles[i+5]};if(PolygonWater.contains(q,x,y))return true;}return false;
    }
    private static void cableOcclusion(){
        Game g=scene();g.layout=Layout.focused(960,600,MontroseTravelLift.X,MontroseTravelLift.Y,2.4f,480,400);
        g.haulout.rig.update(1);MontroseTravelLift.Clips clips=MontroseTravelLift.shore(g);RiverCamera c=g.layout.camera;
        float[] oldShore=ShoreOcclusion.region(g,c,MontroseTravelLift.X-80,MontroseTravelLift.Y-80,MontroseTravelLift.X+80,MontroseTravelLift.Y+80,Float.NEGATIVE_INFINITY);
        int retained=0;
        for(int i=0;i<4;i++){
            float[] a=clips.anchors[i];int restored=0;
            for(float z=a[2];z<60;z+=.5f){float x=c.screenX(a[0],a[1]),y=c.screenY(a[0],a[1])-z*ArticulatedModel.Z_SCALE*c.scale;
                boolean old=covered(oldShore,x,y),now=covered(clips.cords[i],x,y);
                if(old&&!now)restored++;if(now)retained++;
                if(z*ArticulatedModel.Z_SCALE>MontroseHarbor.LAND_RISE+.1f)check(!now,"raised cable cannot be hidden by lower quay behind it");
            }
            check(restored>0,"each cable restores pixels incorrectly clipped by whole-shore mask");
        }
        check(retained>0,"foreground quay still hides lower near cables");
        g.haulout.rig.update(175/240f);clips=MontroseTravelLift.shore(g);
        for(int end=0;end<2;end++){float[] a=clips.anchors[end*2+1];
            check(!covered(clips.slings[end],c.screenX(a[0],a[1]),c.screenY(a[0],a[1])-a[2]*ArticulatedModel.Z_SCALE*c.scale),"far attachment block stays visible at water contact");}

    }
    private static void emptySlices(){
        Game g=scene();float depth=MontroseTravelLift.Y-MontroseTravelLift.X;
        g.layout=Layout.focused(960,600,MontroseTravelLift.X,MontroseTravelLift.Y,2.4f,480,400);
        MontroseTravelLift.Clips shore=MontroseTravelLift.shore(g);check(shore.slings[0].length+shore.slings[1].length>0,"lift fixture has foreground shore mask");
        for(int phase=0;phase<=240;phase++){
            g.haulout.rig.update(phase/240f);Clips empty=new Clips();
            MontroseTravelLift.drawRange(g,empty,depth-900,depth-800,shore);
            MontroseTravelLift.drawRange(g,empty,depth+800,depth+900,shore);
            check(empty.pushes==0 && empty.pops==0 && empty.draws==0,"empty lift slices never install masks");
            for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2){
                float[] a=g.haulout.rig.anchor(side*26,end*24,4);float d=a[1]-a[0];
                check(g.haulout.rig.pose.overlapsDepth(d-.1f,d+.1f),"every hoist cord lies within suspension depth bounds");
                float[] split=g.haulout.rig.slingPose[end<0?0:1].point(MontroseTravelLift.STRAP_ROOT,side*26*MontroseTravelLift.SCALE,end*24*MontroseTravelLift.SCALE,4*MontroseTravelLift.SCALE);
                for(int k=0;k<3;k++)check(split[k]==a[k],"split sling and cable share exact continuous attachment transform");
            }
        }
        shore=MontroseTravelLift.shore(g);
        Clips full=new Clips();MontroseTravelLift.drawRange(g,full,Float.NEGATIVE_INFINITY,Float.POSITIVE_INFINITY,shore);
        check(full.pushes>0 && full.pushes==full.pops && full.draws>0,"nonempty suspension preserves balanced shoreline clipping");
    }
    public static void run(){
        emptySlices();cableOcclusion();
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
            check(Math.abs(c[0]-a[0]-52*MontroseTravelLift.SCALE)<.001f && Math.abs(c[2]-a[2])<.001f,"Sling width and hoist alignment stay rigid");
            check(Math.abs(z-a[2]-64*MontroseTravelLift.SCALE/240f)<.001f,"Webbing moves continuously every tick");z=a[2];}
        for(float amount:new float[]{.12345f,.501f,.499f,1,0}){rig.update(amount);float[] a=rig.anchor(14,-24,-12),c=MontroseTravelLift.STRAPS.pose(amount).point(MontroseTravelLift.STRAP_ROOT,14*MontroseTravelLift.SCALE,-24*MontroseTravelLift.SCALE,-12*MontroseTravelLift.SCALE);
            for(int i=0;i<3;i++)check(Math.abs(a[i]-c[i])<.001f,"Live anchor matches exact transform including reversal");}
        g=scene();g.haulout.requested=true;for(int i=0;i<120;i++)g.step();check(g.haulout.effects.emitted==0 && g.haulout.effects.exhaustPuffs==12,"Smoke starts with motor; ripples wait for water contact");
        phase=g.haulout.liftTicks;g.background();for(int i=0;i<90;i++)g.advance(1f/60);check(g.haulout.liftTicks==phase,"Background pause cannot advance mechanism");
        System.out.println(checks+" travelift radio, interlock, continuous straps and water-contact checks passed");
    }
    public static void main(String[] args){run();}
}
