package com.bargeboss.game;

/** Reproduce real screen-space throws from hulls pressed against either bank. */
public final class MooringAccessTest {
    private static int checks;
    private static void check(boolean ok,String why) { checks++;if(!ok) throw new AssertionError(why); }
    private static void narrow(int stage,int berth){
        for(float width:new float[]{960,1333.3334f})for(float heading:new float[]{-(float)Math.PI/2,(float)Math.PI/2}){
            Game g=new Game();g.challengeId=stage;g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
            Body b=new Body(1,berth,g.dockX(berth),g.dockY(berth)+10,heading);g.cargo[0]=b;
            g.tug.x=PhysicalBerths.CENTER;g.tug.y=b.y+95;
            g.layout=Layout.focused(width,600,b.x,b.y,1.5f,width*.5f,330);
            check(g.inDock(b) && g.canTie(b),"a complete aligned hull in the narrow berth can be lassod");
            b.angle=0;check(!g.canTie(b),"a sideways barge cannot winch through concrete walls");b.angle=heading;
            ReplayInput.down(g,b.x,b.y);check(g.ropeDragging && g.heldCargo()==b,"narrow berth arms the same native lasso gesture");
            ReplayInput.move(g,g.bollardX(berth),g.bollardY(berth));ReplayInput.up(g,g.bollardX(berth),g.bollardY(berth));
            for(int tick=0;tick<Game.ROPE_FLIGHT_TICKS+Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS;tick++)g.step();
            check(b.active && b.deliveryRecorded && b.lineAttached && g.delivered==1,"narrow berth unloads once and retains its moored empty hull");
            int score=g.score;for(int tick=0;tick<60;tick++)g.step();
            check(g.delivered==1 && g.score==score,"persistent empty cannot unload or score again");
        }
    }
    public static void run() {
        for(int stage=0;stage<Challenges.COUNT;stage++) for(int berth=0;berth<3;berth++) {
            Game probe=new Game();probe.challengeId=stage;probe.reset();if(Challenges.standalone(stage) || !probe.berthEnabled(berth)) continue;
            if(probe.orderStage()){narrow(stage,berth);continue;}
            for(float width:new float[]{960,1333.3334f}) for(int heading=0;heading<8;heading++) for(float offset:new float[]{-40,40}) {
                Game g=new Game();g.challengeId=stage;g.reset();
                int deliveredBefore=0;
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
                ReplayInput.move(g,g.bollardX(berth),g.bollardY(berth));
                for(int i=0;i<12;i++) g.step();
                check(g.ropePhase==0 && !b.lineAttached,"shore throw still waits for finger release");
                ReplayInput.up(g,g.bollardX(berth),g.bollardY(berth));
                for(int i=0;i<Game.ROPE_FLIGHT_TICKS;i++) g.step();
                check(b.lineAttached && b.lineSnapTicks>0,"shore throw catches and snaps taut");
                while(b.mooringTicks<Mooring.ALIGN_TICKS) {
                    float x=b.x,y=b.y;g.step();
                    check(Physics.length(b.x-x,b.y-y)<3,"shore catch winches continuously without teleporting");
                }
                check(g.inDock(b) && b.unloadingTicks==1,"crew waits until the shore catch is fully aligned");
                for(int i=1;i<Game.UNLOAD_TICKS;i++) g.step();
                check(g.delivered==deliveredBefore+1 && b.active && b.deliveryRecorded,"shore catch unloads and scores exactly once");
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
