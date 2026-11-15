package com.bargeboss.game;

/** Reproduce real screen-space throws from hulls pressed against either bank. */
public final class MooringAccessTest {
    private static int checks;
    private static void check(boolean ok,String why) { checks++;if(!ok) throw new AssertionError(why); }
    public static void run() {
        for(int stage=0;stage<3;stage++) for(int berth=0;berth<3;berth++) {
            Game probe=new Game();probe.challengeId=stage;probe.reset();if(!probe.berthEnabled(berth)) continue;
            for(float width:new float[]{960,1333.3334f}) for(int heading=0;heading<8;heading++) for(float offset:new float[]{-40,40}) {
                Game g=new Game();g.challengeId=stage;g.reset();
                // Advance only the ordered-level fixture to this berth's legal turn.
                int deliveredBefore=g.orderStage()?(berth==2?0:berth==0?1:2):0;
                g.delivered=deliveredBefore;
                for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
                Body b=new Body(1,berth,0,g.dockY(berth)+offset,(float)(heading*Math.PI/4));
                float extent=b.radius+b.half*(float)StrictMath.abs(StrictMath.cos(b.angle));
                b.x=berth==1?Physics.RIGHT-extent:Physics.LEFT+extent;
                g.cargo[0]=b;g.tug.x=300;g.tug.y=g.dockY(berth)+130;
                g.layout=new Layout(width,600,g.dockY(berth)-344/1.25f,berth==1?500:380,1.25f);
                check(!g.inDock(b),"shore-contact fixture must reproduce the rejected full-hull fit");
                check(g.canTie(b),"shore contact must allow lasso readiness, stage="+stage+", berth="+berth+", heading="+heading);
                ReplayInput.down(g,b.x,b.y);check(g.ropeDragging && g.heldCargo()==b,"visible shore barge arms through native pointer coordinates");
                ReplayInput.move(g,Game.TIE_X[berth],g.dockY(berth));
                for(int i=0;i<12;i++) g.step();
                check(g.ropePhase==0 && !b.lineAttached,"shore throw still waits for finger release");
                ReplayInput.up(g,Game.TIE_X[berth],g.dockY(berth));
                for(int i=0;i<Game.ROPE_FLIGHT_TICKS;i++) g.step();
                check(b.lineAttached && b.lineSnapTicks>0,"shore throw catches and snaps taut");
                while(b.mooringTicks<Mooring.ALIGN_TICKS) {
                    float x=b.x,y=b.y;g.step();
                    check(Physics.length(b.x-x,b.y-y)<3,"shore catch winches continuously without teleporting");
                }
                check(g.inDock(b) && b.unloadingTicks==1,"crew waits until the shore catch is fully aligned");
                for(int i=1;i<Game.UNLOAD_TICKS;i++) g.step();
                check(g.delivered==deliveredBefore+1 && !b.active,"shore catch unloads and scores exactly once");
            }
        }
        Game root=new Game();root.startTutorial();Game practice=root.activeGame();
        practice.tutorial.lesson=Tutorial.DOCK;practice.tutorial.setup(practice);
        Body b=practice.cargo[0];b.x=Physics.LEFT+b.radius;b.angle=(float)Math.PI/2;b.vy=20;
        practice.tutorial.update(practice);
        check(practice.tutorial.lesson==Tutorial.TIE && practice.canTie(b),"practice accepts a moving shoreline arrival and teaches the same lasso readiness");
        ReplayInput.lasso(root,b);
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS+Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS;i++) root.step();
        check(practice.tutorial.lesson==Tutorial.CONNECT,"shoreline practice throw advances to tow training");
        System.out.println(checks+" shoreline lasso, rotated hull, screen input and winch assertions passed");
    }
}
