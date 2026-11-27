package com.bargeboss.game;
/** The six post-storm Montrose jobs: each one completes by its own rules, timed ones fail on the clock,
 * and the Montrose day runs swimmer rescue -> wake patrol -> ... -> fireworks night -> location complete. */
public final class MontroseJobsTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game job(int id){Game g=ReplayInput.montroseJob(id);g.mode=Game.PLAY;check(g.job!=null && g.challengeId==id,"job "+id+" sets up");Parity.frame(g);return g;}
    private static void frames(Game g,int n){for(int i=0;i<n;i++)g.job.step(g);}
    private static void at(Game g,float x,float y){g.tug.x=x;g.tug.y=y;g.tug.vx=g.tug.vy=0;}
    public static void run(){
        // Every job prop sits on water.
        check(MontroseHarbor.contains(SandbarJob.BAR_X,SandbarJob.BAR_Y) && MontroseHarbor.contains(SandbarJob.DOCK_X,SandbarJob.DOCK_Y),"sandbar and service dock are on the water");
        check(MontroseHarbor.contains(FireworksNightJob.PICKUP_X,FireworksNightJob.PICKUP_Y) && MontroseHarbor.contains(FireworksNightJob.MARK_X,FireworksNightJob.MARK_Y),"fireworks pickup and show mark are on the water");
        check(MontroseHarbor.contains(WaterTaxiJob.PIER_X,WaterTaxiJob.PIER_Y),"party pier is on the water");
        for(int i=0;i<RegattaJob.GATES.length;i+=2)check(MontroseHarbor.contains(RegattaJob.GATES[i],RegattaJob.GATES[i+1]),"regatta gate "+i/2+" is on the water");

        Game g=job(Challenges.WAKE_PATROL);WakePatrolJob wake=(WakePatrolJob)g.job;
        for(int t=0;t<120;t++)g.step();
        float moved=0;for(Body b:wake.speeders)moved=Math.max(moved,b.speed());check(moved>40,"speeders tear around the marina");
        for(int i=0;i<WakePatrolJob.SPEEDERS;i++)check(MontroseHarbor.contains(WakePatrolJob.SLOT_X[i],WakePatrolJob.SLOT_Y),"impound slot "+i+" is on the water");
        for(int i=0;i<WakePatrolJob.SPEEDERS;i++){Body b=wake.speeders[i];at(g,b.x-b.half-g.tug.half,b.y);frames(g,1);
            check(wake.ticketed[i] && g.cargo[i]==b && b.awaitingPickup && g.delivered==i,"bumping speeder "+i+" tickets it; it waits dead in the water for a tow");
            check(g.tow.canAttach(g,b,g.tug) || Physics.length(b.x-g.tug.x,b.y-g.tug.y)>0,"a ticketed speeder takes a line");
            b.awaitingPickup=false;b.x=WakePatrolJob.RING_X;b.y=WakePatrolJob.RING_Y;b.vx=b.vy=0;frames(g,WakePatrolJob.WARP+2);
            check(wake.impounded[i] && g.delivered==i+1 && Physics.length(b.x-WakePatrolJob.SLOT_X[wake.slot[i]],b.y-WakePatrolJob.SLOT_Y)<1,"dropping speeder "+i+" at the police boat impounds it in the holding row");
            check(!g.tow.canAttach(g,b,g.tug),"an impounded speeder stays put");}
        for(int t=0;t<300;t++)g.step();
        for(int i=0;i<WakePatrolJob.SPEEDERS && g.challengeId==Challenges.WAKE_PATROL;i++)check(wake.speeders[i].active,"impounded speeders remain in the holding area");
        check(g.challengeId!=Challenges.WAKE_PATROL || g.delivered==WakePatrolJob.SPEEDERS,"six impounded speeders complete wake patrol");

        g=job(Challenges.LOOSE_BOATS);LooseBoatsJob loose=(LooseBoatsJob)g.job;
        float x0=g.cargo[0].x;for(int t=0;t<300;t++)g.step();check(g.cargo[0].x!=x0,"the squall pushes the drifters");
        for(int i=0;i<LooseBoatsJob.BOATS;i++){
            Body b=g.cargo[i];MooredBoats.Berth slip=loose.slips[i];check(MooredFleet.away(slip),"drifter "+i+" returns to an empty slip");
            b.x=slip.x+20;b.y=slip.y;b.vx=b.vy=0;frames(g,LooseBoatsJob.WARP+2);
            check(b.deliveryRecorded && b.awaitingPickup && Physics.length(b.x-slip.x,b.y-slip.y)<1,"dock crew warps drifter "+i+" into its slip");
            check(!g.tow.canAttach(g,b,g.tug),"a docked drifter cannot be towed away again");
        }
        check(g.delivered==LooseBoatsJob.BOATS,"four drifters home completes loose boats");

        g=job(Challenges.WATER_TAXI);WaterTaxiJob taxi=(WaterTaxiJob)g.job;
        for(int guard=0;guard<40000 && g.delivered<g.quota();guard++){
            int target=-1;for(int s=0;s<taxi.waiting.length;s++)if(taxi.waiting[s]>0){target=s;break;}
            if(taxi.aboard>=WaterTaxiJob.SEATS || target<0 && taxi.aboard>0)at(g,WaterTaxiJob.PIER_X,WaterTaxiJob.PIER_Y);
            else if(target>=0)at(g,WaterTaxiJob.PICKUPS[target*2],WaterTaxiJob.PICKUPS[target*2+1]);
            frames(g,1);
        }
        check(g.delivered==10 && !g.job.failed(g),"ten guests ferried in time ("+g.job.ticks/60+" s)");
        g=job(Challenges.WATER_TAXI);frames(g,180*60);check(g.goalFailed(),"the taxi clock runs out");

        g=job(Challenges.SANDBAR);SandbarJob bar=(SandbarJob)g.job;Body yacht=bar.yacht;
        at(g,yacht.x-120,yacht.y+70);check(g.tow.attach(g,yacht,g.tug),"the grounded yacht takes a line");
        for(int t=0;t<60;t++){ReplayInput.throttle(g,-90,60);g.step();}
        check(Physics.length(yacht.x-SandbarJob.BAR_X,yacht.y-SandbarJob.BAR_Y)<1,"the yacht stays aground while hauling");
        check(bar.progress>0,"hauling on the line works it loose");
        for(int t=0;t<6000 && bar.stuck;t++){ReplayInput.throttle(g,-90,60);g.step();}
        g.pointerCancel(0);
        check(!bar.stuck,"enough hauling pops the yacht free");
        g.tow.detachBody(g,yacht);yacht.x=SandbarJob.DOCK_X;yacht.y=SandbarJob.DOCK_Y;yacht.vx=yacht.vy=0;frames(g,75);
        check(yacht.deliveryRecorded && g.delivered==1,"the service dock takes the freed yacht");

        g=job(Challenges.REGATTA);RegattaJob race=(RegattaJob)g.job;
        for(int i=0;i<race.gates();i++){at(g,RegattaJob.GATES[i*2],RegattaJob.GATES[i*2+1]);frames(g,60);}
        check(g.delivered==race.gates() && race.next==race.gates() && !g.goalFailed(),"threading every gate in order finishes the course");
        g=job(Challenges.REGATTA);frames(g,RegattaJob.START_SECONDS*60+120);check(!g.goalFailed(),"the regatta clock waits for gate one");
        at(g,RegattaJob.GATES[0],RegattaJob.GATES[1]);frames(g,(RegattaJob.START_SECONDS+RegattaJob.GATE_SECONDS)*60+60);check(g.goalFailed(),"the regatta clock runs out after gate one");

        g=job(Challenges.FIREWORKS_NIGHT);FireworksNightJob show=(FireworksNightJob)g.job;
        check(g.weather.evening,"fireworks night brings on the dusk");
        show.barge.awaitingPickup=false;show.barge.x=FireworksNightJob.MARK_X;show.barge.y=FireworksNightJob.MARK_Y;frames(g,1);
        check(show.onStation && !g.tow.canAttach(g,show.barge,g.tug),"the barge anchors on the show mark");
        for(int v=0;v<3;v++){g.abilities.launches++;frames(g,1);}
        check(g.delivered==3,"three volleys complete the show");

        // Deep freeze: the blizzard builds, the lift cycles boat by boat, and the freeze decides the outcome.
        g=job(Challenges.DEEP_FREEZE);DeepFreezeJob ice=(DeepFreezeJob)g.job;
        check(g.harborTraffic.craft.length==0,"everyone else has fled the blizzard");
        for(int i=0;i<DeepFreezeJob.BOATS;i++)check(MontroseHarbor.contains(ice.boats[i].x,ice.boats[i].y),"anchorage "+i+" is on the water");
        frames(g,DeepFreezeJob.RAMP);check(g.weather.blizzard==1 && g.weather.freeze==0,"the blizzard is full before the ice starts");
        float bx=ice.boats[0].x;
        Body loose2=ice.boats[0];loose2.awaitingPickup=false;for(int t=0;t<120;t++){g.job.motion(g);Physics.integrate(loose2);}
        check(loose2.x!=bx,"gusts shove a boat that is off its anchor");
        int cycle=DeepFreezeJob.WARP_TICKS+DeepFreezeJob.HOIST_TICKS+DeepFreezeJob.CARRY_TICKS+DeepFreezeJob.RETURN_TICKS+2;
        for(int i=0;i<3;i++){Body b=ice.boats[i];b.awaitingPickup=false;b.x=DeepFreezeJob.RING_X;b.y=DeepFreezeJob.RING_Y;b.vx=b.vy=0;frames(g,1);
            check(ice.lifting==b,"the lift crew takes boat "+i);
            Body next=ice.boats[i+1];next.awaitingPickup=false;next.x=DeepFreezeJob.RING_X+10;next.y=DeepFreezeJob.RING_Y;next.vx=next.vy=0;frames(g,1);
            check(ice.lifting==b,"the next boat waits while the lift works");next.x=ice.boats[i+1].tiedX;next.y=ice.boats[i+1].tiedY+400;
            frames(g,cycle);check(b.deliveryRecorded && !b.active && ice.phase==DeepFreezeJob.READY,"boat "+i+" is hauled out and the straps come back down");}
        check(g.delivered==3 && !g.job.done(g),"three boats hauled");
        frames(g,DeepFreezeJob.FREEZE_TICKS);check(g.weather.freeze==1 && g.goalFailed(),"the deep freeze with three boats saved fails the job");
        g=job(Challenges.DEEP_FREEZE);g.delivered=DeepFreezeJob.SILVER;frames(g,DeepFreezeJob.FREEZE_TICKS);
        check(g.job.done(g) && !g.goalFailed() && g.job.stars(g)==2,"six boats saved by the freeze earn two stars");
        Parity.frame(g);
        // The whole Montrose day hands off job to job and ends after the fireworks.
        g=job(Challenges.WATER_TAXI);int[] order=java.util.Arrays.copyOfRange(MontroseJob.ORDER,1,MontroseJob.ORDER.length);
        for(int k=0;k<order.length;k++){
            check(g.challengeId==order[k] && g.job!=null,"montrose day reaches job "+order[k]);
            float tx=g.tug.x,ty=g.tug.y;
            g.delivered=g.quota();g.step();
            if(k<order.length-1)check(Physics.length(g.tug.x-tx,g.tug.y-ty)<10,"job "+order[k]+" hands off without moving the tug");
            g.dialogue.close(g);g.dialogue.clearQueue();
            if(k<order.length-1){check(g.challengeId==order[k+1] && g.delivered==0,"job "+order[k]+" hands off to "+order[k+1]);Parity.frame(g);}
        }
        check(g.montroseComplete && g.career.bestStars(Challenges.DEEP_FREEZE)>0,"finishing the deep freeze completes Montrose and records stars");
        // Gavin's debrief depends on how the job went, and the next dispatch follows it.
        String[] heard=new String[2];
        for(int stars=3,k=0;stars>=2;stars--,k++){g=job(Challenges.WATER_TAXI);g.dialogue.close(g);MontroseDockmaster.dispatch(g);MontroseDockmaster.debrief(g,Challenges.WATER_TAXI,stars==3?3:1,95);
            check(g.dialogue.active,"debrief shows");heard[k]=g.dialogue.content().title;
            g.dialogue.close(g);check(g.dialogue.showQueued(g) && g.dialogue.active,"the next dispatch follows the debrief");}
        check(!heard[0].equals(heard[1]),"a great job and a rough one get different debriefs");
        g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);g.mode=Game.PLAY;g.delivered=g.quota();g.step();
        check(g.challengeId==Challenges.WATER_TAXI && !g.montroseComplete && g.job!=null,"the swimmer rescue leads into the new day's jobs");
        // Arcade layer: a clean high-speed skim past traffic pays out; finishing under par pays a quick-job bonus and shows the banner.
        g=job(Challenges.REGATTA);HarborTraffic.Craft craft=g.harborTraffic.craft[0];Body boat=craft.body;
        int before=g.score;float side=boat.radius+g.tug.radius+g.tug.half+4;
        float nx=-(float)StrictMath.sin(boat.angle),ny=(float)StrictMath.cos(boat.angle);
        for(int t=0;t<3;t++){at(g,boat.x+nx*side,boat.y+ny*side);g.tug.vx=80;g.arcade.step(g);}
        at(g,boat.x+nx*400,boat.y+ny*400);g.arcade.step(g);
        check(g.arcade.nearMisses==1 && g.score==before+MontroseArcade.NEAR_POINTS,"skimming past traffic at speed is a near miss");
        before=g.score;int bonus=g.arcade.complete(g,Challenges.REGATTA,3,1000);
        check(bonus>0 && g.score==before+bonus && g.arcade.bannerTicks==MontroseArcade.BANNER,"finishing under par pays a quick-job bonus and shows the banner");
        Parity.frame(g);
        System.out.println("Montrose jobs: wake patrol, loose boats, water taxi, sandbar, regatta, fireworks night and the day chain passed");
    }
    public static void main(String[] a){run();}
}
