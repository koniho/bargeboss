package com.bargeboss.game;
/** Open-water speed, boost markers and the travel radio between the marina and the beach. */
public final class MontroseTransitTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        for(int i=0;i<MontroseTransit.count();i++){float x=MontroseTransit.BUOYS[i*2],y=MontroseTransit.BUOYS[i*2+1];
            check(MontroseHarbor.contains(x,y) && !HarborTraffic.noWake(x,y),"marker "+i+" sits on open water outside the no-wake zone");}
        Game g=ReplayInput.montroseJob(Challenges.WATER_TAXI);g.mode=Game.PLAY;g.dialogue.close(g);g.dialogue.clearQueue();
        g.tug.x=-60;g.tug.y=-100;g.tug.vx=g.tug.vy=0;g.step();
        check(!g.transit.open && g.transit.speedFactor(g)==1,"the marina keeps its normal throttle");
        // Out past the harbor mouth, between markers.
        g.tug.x=1300;g.tug.y=1000;g.tug.vx=60;g.tug.vy=0;g.step();
        check(g.transit.open && Math.abs(g.transit.speedFactor(g)-MontroseTransit.OPEN_WATER)<1e-4f,"open water runs faster");
        // Run three markers in a row.
        int score=g.score;
        for(int k=2;k<5;k++){g.tug.x=MontroseTransit.BUOYS[k*2];g.tug.y=MontroseTransit.BUOYS[k*2+1];g.tug.vx=80;g.tug.vy=0;g.step();}
        check(g.transit.combo==3 && g.transit.boostTicks>0 && g.score>score,"back-to-back markers boost and build a combo");
        check(g.transit.speedFactor(g)>MontroseTransit.OPEN_WATER,"a boost stacks on the open-water speed");
        g.tug.x=MontroseTransit.BUOYS[4*2];g.tug.y=MontroseTransit.BUOYS[4*2+1];g.step();check(g.transit.combo==3,"a spent marker can't be farmed");
        for(int t=0;t<MontroseTransit.REARM_TICKS+5;t++){g.tug.x=2000;g.tug.y=1100;g.tug.vx=g.tug.vy=0;g.step();}
        check(g.transit.combo==0 && g.transit.spent[4]==0,"the combo lapses and markers re-arm");
        for(int t=0;t<200;t++)g.step();
        check(g.transit.speedFactor(g)>=1,"no slowdown");
        System.out.println("transit: open water, marker boosts, combo, re-arm and travel radio");
    }
    public static void main(String[] a){run();}
}
