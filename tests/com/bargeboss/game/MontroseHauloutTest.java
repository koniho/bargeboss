package com.bargeboss.game;

/** Public radio/tow/release voyage and independent survey/collision acceptance checks. */
public final class MontroseHauloutTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static Game voyage(boolean log){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();Body boat=g.cargo[0],tug=g.tug;
        check(g.challengeId==Challenges.HAULOUT && boat.cruiser,"Montrose opens on disabled cruiser job");
        check(Physics.length(tug.x-MontroseHarbor.SLIP_X,tug.y-MontroseHarbor.SLIP_Y)<200,"Tug starts beside large yacht slip");
        check(g.dialogue.dockmasterConversation() && g.radio.transmitter()==RadioAudio.DOCKMASTER,"Dockmaster dispatches opening mission");
        check(g.dialogue.content().header.equals("DOCKMASTER / MONTROSE") && g.dialogue.content().waiting,"Role label and waiting star, no personal name");
        int initialScore=g.score;
        for(int i=0;i<30;i++)g.step();check(g.dialogue.ready() && g.score==initialScore,"Waiting for reply does not complete job");
        for(int tick=0;tick<6000 && !boat.deliveryRecorded;tick++){
            ReplayInput.work(g);g.step();
            if(log && tick%300==0)System.out.println(tick+" tug="+MontroseHarbor.planX(tug.x)+","+MontroseHarbor.planY(tug.y)+" boat="+MontroseHarbor.planX(boat.x)+","+MontroseHarbor.planY(boat.y)+" lift="+g.haulout.liftTicks+" pushed="+g.haulout.pushed+" tow="+g.tow.count()+" warp="+g.haulout.warping+" speed="+boat.speed()+" hull="+g.hull);
        }
        check(boat.deliveryRecorded && g.haulout.lowered() && g.haulout.pushed && g.haulout.towed && g.haulout.warping,"Public radio, tow, release and physical push complete crew handoff");
        check(g.challengeId==Challenges.MONTROSE && g.tug==tug && g.hull==100,"Safe haulout continues voyage without relocating tug");
        check(g.cargo[g.cargo.length-1]==boat && boat.active && boat.awaitingPickup,"Crew secures and retains disabled boat");
        check(g.career.bestStars(Challenges.HAULOUT)==3 && g.score==1200,"Dedicated save slot and once-only haulout reward");
        check(g.cargo[0].awaitingPickup,"Next yacht waits at the inlet while the tug travels there");
        check(g.dialogue.dockmasterConversation() && g.dialogue.content().title.contains("YACHT"),"Next mission calls out over radio");
        return g;
    }
    public static void run(){
        for(float x:new float[]{1030,1090})for(float y:new float[]{220,260})check(!MontroseHarbor.openWater(MontroseHarbor.mapX(x),MontroseHarbor.mapY(y)),"Four travel-lift wheels have solid land");
        for(int y=210;y<=340;y+=10)check(MontroseHarbor.contains(MontroseHaulout.X,MontroseHarbor.mapY(y)),"Well mouth and centerline remain navigable");
        for(int y=100;y<200;y+=10)check(!MontroseHarbor.openWater(MontroseHaulout.X,MontroseHarbor.mapY(y)),"Connected hardstand behind well");
        Body b=new Body(77,2,MontroseHaulout.X,MontroseHaulout.Y,MontroseHaulout.ANGLE,true,true);
        check(MontroseHaulout.fits(b),"Cruiser fits inside well");b.angle=0;check(!MontroseHaulout.fits(b),"Sideways hull cannot pass alignment check");
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.dialogue.close(g);b=g.cargo[0];b.awaitingPickup=false;b.x=MontroseHaulout.X;b.y=MontroseHaulout.Y;
        for(int i=0;i<180;i++)g.step();check(!b.deliveryRecorded && !g.haulout.warping,"No haulout credit without towing and releasing");
        voyage(false);System.out.println(checks+" haulout survey, radio, public tow, handoff and continuity checks passed");
    }
    public static void main(String[] args){voyage(true);}
}
