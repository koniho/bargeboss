package com.bargeboss.game;
/** Per-vessel health, property-damage record, sinking and the sunk-cargo mission failure. */
public final class HullDamageTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game yard(){
        Game g=new Game();g.resetPracticeYard();g.mode=Game.PLAY;
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;g.addCargo(0,330,500);return g;
    }
    private static void hit(Game g,Body a,Body b,float closing){
        g.splashes.contact(a,b,300,500,closing,0,0,0,closing);
        for(int i=0;i<=CollisionSplashes.COOLDOWN;i++)g.splashes.step(g); // spray dedupes a pair for COOLDOWN ticks
    }
    public static void run(){
        Game g=yard();Body barge=g.cargo[0];
        hit(g,g.tug,barge,77);check(barge.health==100 && g.othersDamage==0,"soft contact leaves health");
        hit(g,g.tug,barge,90);check(barge.health==100-HullDamage.HIT && g.othersDamage==HullDamage.HIT,"hard contact costs the barge and is recorded as others' property");
        for(int t=0;t<20;t++)g.ticks++;hit(g,g.tug,barge,120);
        check(barge.health==100-HullDamage.HIT,"damage cooldown");
        check(g.hull==100 && g.ownDamage==0,"splash contacts never touch the tug hull (Game.damage owns it)");
        Game tutorial=new Game();tutorial.startTutorial();Game t=tutorial.activeGame();t.mode=Game.PLAY;
        Body target=null;for(Body b:t.cargo)if(b!=null)target=b;
        if(target!=null){t.splashes.contact(t.tug,target,0,0,120,0,0,0,120);t.splashes.step(t);check(target.health==100,"practice tutorial takes no damage");}
        // Wreck it: hits spaced past the cooldown until it sinks.
        g=yard();barge=g.cargo[0];int hits=0;
        while(barge.health>0 && hits<40){g.ticks+=HullDamage.COOLDOWN;hit(g,g.tug,barge,100);hits++;}
        check(barge.health==0 && barge.sinkTicks>0 && hits==(100+HullDamage.HIT-1)/HullDamage.HIT,"repeated hard hits sink the vessel");
        check(g.othersDamage==100,"others' damage totals the lost health");
        check(!g.goalFailed() && barge.active,"the mission fails only once the sinking finishes");
        float depth=0;
        for(int i=0;i<HullDamage.SINK;i++){HullDamage.step(g);check(HullDamage.sinkDepth(barge)>=depth,"hull settles monotonically");depth=HullDamage.sinkDepth(barge);}
        check(!barge.active && barge.sunk && !g.goalFailed(),"fail screen waits for the wreck to settle");
        for(int i=0;i<HullDamage.FAIL_DELAY;i++)HullDamage.step(g);
        check(g.cargoSunk && g.goalFailed(),"sunk required cargo fails the mission");
        // Moored marina boats count as others' property, once per cooldown.
        // Moored marina boats have their own health: hit once per cooldown, sink at zero, then leave the water.
        g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.mode=Game.PLAY;
        check(g.mooredFleet.boats.length>0,"Montrose fills its marina");
        MooredFleet.FloatBoat m=g.mooredFleet.boats[0];
        HullDamage.moored(g,m,77);check(m.health==100,"soft bump leaves a marina boat whole");
        HullDamage.moored(g,m,100);HullDamage.moored(g,m,100);
        check(m.health==100-HullDamage.HIT && g.othersDamage==HullDamage.HIT,"moored hit costs health once per cooldown");
        while(m.health>0){g.ticks+=HullDamage.COOLDOWN;HullDamage.moored(g,m,100);}
        check(m.sinkTicks>0 && !m.afloat() && g.othersDamage==100,"a wrecked marina boat sinks and the record totals its health");
        Body probe=new Body(-1,-1,m.x,m.y,0);probe.vx=200;
        check(g.mooredFleet.collide(g,probe,g.splashes)==0,"a sinking marina boat no longer blocks traffic");
        for(int i=0;i<HullDamage.SINK+HullDamage.SLICK+5;i++)g.mooredFleet.step(g);
        check(m.sinkTicks==HullDamage.SINK+HullDamage.SLICK,"marina wreck clock stops after its slick fades");
        System.out.println("Vessel health, property record, sinking and sunk-cargo failure passed");
    }
    public static void main(String[] a){run();}
}
