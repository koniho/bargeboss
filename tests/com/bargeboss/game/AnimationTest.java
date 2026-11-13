package com.bargeboss.game;

/** Presentation clocks must animate menus without advancing or mutating the simulation. */
public final class AnimationTest {
    private static int assertions;
    private static void check(boolean ok,String message) { assertions++; if(!ok) throw new AssertionError(message); }
    public static void run() {
        Game g=new Game(); float x=g.tug.x,y=g.tug.y;
        int first=Parity.frame(g);
        for(int i=0;i<45;i++) g.advance(1.0/60);
        check(g.visualTicks==45 && Parity.frame(g)!=first,"title artwork animates on the shared presentation clock");
        check(g.ticks==0 && g.tug.x==x && g.tug.y==y && g.delivered==0 && g.pollHaptic()==0,"title animation cannot drive gameplay or feedback");
        int frame=Parity.frame(g),clock=g.visualTicks;
        check(Parity.frame(g)==frame && g.visualTicks==clock,"drawing a menu is pure and repeatable");
        Game low=new Game(),high=new Game();
        for(int i=0;i<90;i++) low.advance(1.0/30);
        for(int i=0;i<432;i++) high.advance(1.0/144);
        check(low.visualTicks==high.visualTicks && Parity.frame(low)==Parity.frame(high),"menu frames match at 30 and 144 Hz");
        g.reset(); for(int i=0;i<30;i++) g.step(); g.background();
        frame=Parity.frame(g); clock=g.visualTicks;
        g.advance(.25); check(g.visualTicks==clock && Parity.frame(g)==frame,"pause freezes both world and presentation");
        g.pause(); g.mode=Game.HELP; frame=Parity.frame(g); g.advance(.25);
        check(g.visualTicks==clock && Parity.frame(g)==frame,"help freezes the suspended stage");
        g.mode=Game.FINISH; g.delivered=Game.QUOTA; g.score=4321;
        frame=Parity.frame(g); int ticks=g.ticks; x=g.tug.x; y=g.tug.y;
        for(int i=0;i<60;i++) g.advance(1.0/60);
        check(Parity.frame(g)!=frame && g.visualTicks==clock+60,"shift-complete scene animates");
        check(g.ticks==ticks && g.tug.x==x && g.tug.y==y && g.score==4321,"results animation cannot change final score or physics");
        clock=g.visualTicks; g.advance(Double.NaN); g.advance(-1);
        check(g.visualTicks==clock,"invalid presentation deltas ignored");
        g.advance(60); check(g.visualTicks==clock+15,"presentation catch-up bounded after a stalled host");
        System.out.println(assertions+" animation clock and render purity assertions passed");
    }
}
