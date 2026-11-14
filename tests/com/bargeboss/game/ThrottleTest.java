package com.bargeboss.game;

public final class ThrottleTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static Game moving(float angle) {
        Game g=new Game();g.reset();for(Feeder f:g.feeders) { f.phase=Feeder.IDLE;f.load=null; }
        g.tug=new Body(0,-1,300,500,angle);g.tug.vx=60*(float)StrictMath.cos(angle);g.tug.vy=60*(float)StrictMath.sin(angle);return g;
    }
    public static void run() {
        for(float angle:new float[]{0,(float)Math.PI/2,(float)Math.PI,-(float)Math.PI/2}) {
            Game g=moving(angle),coast=moving(angle);float cx=(float)StrictMath.cos(angle),cy=(float)StrictMath.sin(angle);
            ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX-cx*90,g.downY-cy*90);
            for(int i=0;i<30;i++) { g.step();coast.step(); }
            check(g.reversing && g.control==Game.REVERSING && g.tug.engineLoad>.5f,"reverse uses engine thrust");
            check(g.tug.vx*cx+g.tug.vy*cy<coast.tug.vx*cx+coast.tug.vy*cy-8,"counter-thrust slows more than coasting");
            check(Math.abs(Physics.angle(g.tug.angle-angle))<.1f,"astern thrust keeps bow orientation");
            ReplayInput.up(g,g.pointerX,g.pointerY);g.step();check(!g.reversing && !g.navigating,"releasing reverse cuts throttle");
        }
        Game centered=moving(0),released=moving(0);
        ReplayInput.down(centered,centered.tug.x,centered.tug.y);
        for(int i=0;i<15;i++) { centered.step();released.step(); }
        check(centered.tug.vx==released.tug.vx && centered.tug.vy==released.tug.vy,"neutral control has same drag as released throttle");
        check(centered.tug.speed()>30 && centered.tug.engineLoad==0,"neutral does not brake or apply hidden engine force");
        System.out.println(checks+" neutral throttle, coasting and reverse thrust assertions passed");
    }
}
