package com.bargeboss.game;

/** Empty hulls remain physical cargo; a successful tow catch transfers the shore line. */
public final class PersistentBargeTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static Game fixture(){
        Game g=new Game();g.resetPracticeYard();
        for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
        g.cargo[0]=new Body(10,2,g.dockX(2),g.dockY(2),(float)Math.PI/2);
        Mooring.attach(g.cargo[0]);return g;
    }
    private static void approach(Game g){
        Body b=g.cargo[0];g.tug.x=b.x+110;g.tug.y=b.y+15;g.tug.vx=g.tug.vy=g.tug.spin=0;
        g.layout=Layout.focused(1333,600,b.x+50,b.y,1.5f,650,360);
    }
    public static void run(){
        Game g=fixture();Body b=g.cargo[0];
        approach(g);check(!g.canTow(b),"a crew still unloading cannot be unmoored by a tow throw");
        steps(g,Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS);
        check(g.delivered==1 && b.active && b.deliveryRecorded && b.lineAttached,"unloading scores once and leaves an occupied physical hull");
        check(b.unloadingTicks==Game.UNLOAD_TICKS,"empty appearance retains completed unloading state");
        int score=g.score;float x=b.x,y=b.y;
        steps(g,300);check(g.delivered==1 && g.score==score && b.x==x && b.y==y,"completed barge stays secured without repeated score or drift");
        check(!g.canTie(b),"an empty barge cannot be counted as a fresh delivery");
        approach(g);check(g.canTow(b),"completed moored hull can receive a towline");
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);
        ReplayInput.up(g,g.tug.x+85,g.tug.y+85);steps(g,Game.ROPE_FLIGHT_TICKS+Game.ROPE_RETURN_TICKS+2);
        check(b.lineAttached && g.tow.incoming(b)==null,"a missed tow throw leaves the shore mooring intact");
        ReplayInput.down(g,b.x,b.y);steps(g,18);
        check(!g.pushing && g.canTow(b),"holding an empty moored hull does not steal the towing gesture");
        ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
        check(b.lineAttached,"shore mooring holds while the tow rope is in flight");
        steps(g,Game.ROPE_FLIGHT_TICKS+1);
        check(!b.lineAttached && g.tow.incoming(b)!=null,"successful catch releases shore mooring and connects the same empty hull");
        check(b.active && b.deliveryRecorded && b.unloadingTicks==Game.UNLOAD_TICKS,"tow transfer preserves the identity and empty state");
        steps(g,Game.ROPE_HIT_TICKS+1);
        x=b.x;y=b.y;ReplayInput.throttle(g,80,30);
        steps(g,120);ReplayInput.up(g,g.pointerX,g.pointerY);
        check(Physics.length(b.x-x,b.y-y)>10,"normal tug thrust physically pulls the retained empty hull away");
        ReplayInput.releaseTow(g);steps(g,30);
        check(b.active && b.deliveryRecorded && g.delivered==1 && g.score==score,"releasing the tow neither deletes the empty nor awards it twice");
        Body obstacle=new Body(99,0,b.x,b.y,b.angle);obstacle.vx=20;
        float before=Physics.length(obstacle.x-b.x,obstacle.y-b.y);Physics.collide(b,obstacle);
        check(Physics.length(obstacle.x-b.x,obstacle.y-b.y)>before,"an empty delivered hull still participates in hull collision");
        System.out.println(checks+" retained empty-hull, shore-to-tow transfer and score-once assertions passed");
    }
    public static void main(String[] args){run();}
}
