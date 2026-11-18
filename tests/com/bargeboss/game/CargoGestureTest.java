package com.bargeboss.game;

/** A cargo push cannot turn into a line throw as either hull or the finger moves. */
public final class CargoGestureTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static void steps(Game g,int count) {for(int i=0;i<count;i++) g.step();}
    private static Game fixture(float width) {
        Game g=new Game();g.configureCanvas(width,600);g.resetPracticeYard();
        for(Feeder feeder:g.feeders) {feeder.phase=Feeder.IDLE;feeder.load=null;}
        g.tug=new Body(0,-1,300,570,-(float)Math.PI/2);
        g.cargo[0]=new Body(1,2,300,690,-(float)Math.PI/2);
        return g;
    }
    private static void noThrow(Game g,String message) {
        check(!g.towAiming && !g.ropeDragging && g.ropePhase==0 && g.tow.cargo==null,message);
    }
    public static void run() {
        for(float width:new float[]{960,1333.3334f}) {
            Game g=fixture(width);Body b=g.cargo[0];
            ReplayInput.down(g,b.x,b.y);steps(g,12);
            check(g.pushing && !g.manualSteering,"still hold commits assisted pushing");
            ReplayInput.move(g,g.tug.x,g.tug.y);
            check(g.pushing && g.manualSteering && !g.canTow(b),"steering toward tug keeps the established push and hides its tow affordance");
            noThrow(g,"moving a push finger over the tug cannot arm either lasso");
            ReplayInput.move(g,g.downX,g.downY);
            check(g.pushing && !g.manualSteering,"centering a committed push still returns to hold assistance");
            ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
            noThrow(g,"release over tug after a push cannot launch a towline");

            g=fixture(width);b=g.cargo[0];ReplayInput.down(g,b.x,b.y);steps(g,12);
            ReplayInput.up(g,g.tug.x,g.tug.y);
            noThrow(g,"pointer-up without a preceding move cannot bypass push ownership");

            g=fixture(width);b=g.cargo[0];ReplayInput.down(g,b.x,b.y);
            ReplayInput.move(g,g.downX+90,g.downY);
            check(g.manualSteering && !g.towAiming,"initial drag away from tug selects pushing before hold threshold");
            g.tug.x=g.pointerX;g.tug.y=g.pointerY;
            ReplayInput.move(g,g.pointerX,g.pointerY);
            noThrow(g,"tug crossing beneath an unchanged steering finger cannot reinterpret initial drag intent");
            steps(g,12);check(g.pushing,"moving-tug regression still engages pushing");
            ReplayInput.up(g,g.tug.x,g.tug.y);noThrow(g,"tug under release point cannot steal push gesture");

            g=fixture(width);b=g.cargo[0];ReplayInput.down(g,b.x,b.y);steps(g,12);
            // Reaching a berth enables a new mooring gesture, not the push already held.
            b.x=230;b.y=290;g.tug.x=330;g.tug.y=290;
            check(g.canTie(b),"pushed hull becomes eligible at its assigned berth");
            ReplayInput.move(g,120,290);noThrow(g,"berth arrival cannot arm a held push as a mooring throw");
            ReplayInput.up(g,120,290);noThrow(g,"lifting the original push does not throw at a newly reached berth");
            ReplayInput.lasso(g,b);steps(g,Game.ROPE_FLIGHT_TICKS);
            check(b.lineAttached,"fresh touch on ready barge still throws to bollard");

            for(boolean cancel:new boolean[]{false,true}) {
                g=fixture(width);b=g.cargo[0];ReplayInput.down(g,b.x,b.y);steps(g,12);
                if(cancel) g.pointerCancel(0);else ReplayInput.up(g,g.pointerX,g.pointerY);
                ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);
                check(g.towAiming && !g.pushing,"lift or cancel permits a fresh barge-to-tug gesture");
                steps(g,30);check(g.towAiming && !g.pushing,"an established tow aim can be held without turning into pushing");
                ReplayInput.up(g,g.tug.x,g.tug.y);steps(g,Game.ROPE_FLIGHT_TICKS);
                check(g.tow.cargo==b,"fresh tow gesture still catches after earlier push");
            }
        }
        System.out.println(checks+" cargo gesture ownership and fresh lasso assertions passed");
    }
}
