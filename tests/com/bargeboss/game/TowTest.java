package com.bargeboss.game;

/** Real routed gestures plus physical and lifecycle invariants for pulling. */
public final class TowTest {
    private static int checks;
    private static void check(boolean ok,String why) { checks++;if(!ok) throw new AssertionError(why); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game fixture() {
        Game g=new Game();g.reset();
        for(Feeder f:g.feeders) { f.phase=Feeder.IDLE;f.load=null; }
        g.tug=new Body(0,-1,300,570,-(float)Math.PI/2);
        g.cargo[0]=new Body(1,2,300,690,-(float)Math.PI/2);
        return g;
    }
    private static void aim(Game g) {
        Body b=g.cargo[0];ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);
    }
    private static void connect(Game g) { aim(g);ReplayInput.up(g,g.tug.x,g.tug.y);steps(g,18); }
    private static void drive(Game g,float dx,float dy) {
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX+dx,g.downY+dy);
    }
    public static void run() {
        Game g=fixture();Body b=g.cargo[0];
        aim(g);steps(g,40);
        check(g.towAiming && !g.pushing && g.tow.cargo==null && g.ropePhase==0,"aiming never launches or pushes");
        ReplayInput.up(g,g.tug.x,g.tug.y);check(g.ropePhase==1 && g.ropeToTug,"release casts toward tug");
        steps(g,17);check(g.tow.cargo==null,"line must finish flight before catch");
        steps(g,1);check(g.tow.cargo==b && !b.lineAttached,"catch creates tow, never shore mooring");
        check(g.pollHaptic()==Game.HAPTIC_CONTACT,"tow catch has contact haptic");
        float y=b.y;drive(g,0,-100);steps(g,120);
        check(g.tow.cargo==b && b.y<y-60 && g.tug.y<b.y-60,"drag drives a trailing loaded barge: "+b.y);
        check(b.unloadingTicks==0 && g.delivered==0,"tow alone never unloads or scores");
        g.cancel();float vx=b.vx,vy=b.vy;
        ReplayInput.releaseTow(g);
        check(g.tow.cargo==null && b.vx==vx && b.vy==vy && g.tow.releaseTicks>0,"release preserves cargo momentum and animates recoil");
        float lastY=b.y;steps(g,5);check(b.y<lastY,"released load coasts");

        g=fixture();connect(g);b=g.cargo[0];
        ReplayInput.down(g,b.x,b.y);steps(g,25);ReplayInput.up(g,b.x,b.y);
        check(g.tow.cargo==b,"a long hold does not count as a release tap");
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,b.x+45,b.y);ReplayInput.move(g,g.downX,g.downY);ReplayInput.up(g,g.downX,g.downY);
        check(g.tow.cargo==b,"dragging out and back does not release the tow");
        ReplayInput.down(g,b.x,b.y);g.pointerCancel(0);
        check(g.tow.cargo==b,"cancelled cargo tap leaves line attached");
        ReplayInput.releaseTow(g);check(g.tow.cargo==null,"short cargo tap releases after cancelled gesture");
        g=fixture();connect(g);ReplayInput.releaseTow(g);
        check(g.tow.cargo==null && g.ropePhase==0,"cargo tap can release immediately after catch without casting a shore lasso");
        g=fixture();g.cargo[0].x=230;g.cargo[0].y=290;g.tug.x=330;g.tug.y=290;g.tug.angle=0;
        connect(g);steps(g,24);ReplayInput.releaseTow(g);
        check(g.tow.cargo==null && g.ropePhase==0 && !g.cargo[0].lineAttached,"berth-ready cargo tap releases instead of throwing a lasso");
        g=fixture();connect(g);b=g.cargo[0];g.cargo[1]=b;
        g.cargo[0]=new Body(2,1,b.x+37,b.y,b.angle);
        ReplayInput.releaseTow(g);check(g.tow.cargo==null,"connected hull wins over adjacent cargo touch padding");
        g=fixture();aim(g);ReplayInput.move(g,410,600);ReplayInput.up(g,410,600);steps(g,18);
        check(g.ropePhase==2 && g.tow.cargo==null,"missed tug throw retracts");steps(g,18);
        check(g.ropePhase==0,"miss frees gesture for retry");
        g=fixture();aim(g);g.pointerCancel(0);steps(g,30);
        check(g.tow.cargo==null && g.ropePhase==0 && !g.towAiming,"cancel never throws");
        g=fixture();b=g.cargo[0];ReplayInput.down(g,b.x,b.y);steps(g,21);
        float endX=g.tug.x,endY=g.tug.y;
        for(int i=1;i<=12;i++) { ReplayInput.move(g,g.downX+(endX-g.downX)*i/12,g.downY+(endY-g.downY)*i/12);steps(g,4); }
        ReplayInput.up(g,endX,endY);steps(g,18);
        check(g.tow.cargo==null && g.ropePhase==0,"a slow drag after push assist cannot silently turn into a tow throw");
        g=fixture();aim(g);RiverCamera frozen=g.layout.camera;steps(g,20);
        check(g.layout.camera==frozen,"tow aiming freezes camera projection");
        g=fixture();connect(g);g.tug.y-=250;g.step();
        check(g.tow.cargo==null && g.tow.releaseTicks>0,"overstretched line releases with recoil");
        g=fixture();connect(g);g.startTutorial();Game practice=g.activeGame();
        practice.tutorial.lesson=Tutorial.CONNECT;practice.tutorial.setup(practice);
        check(g.tow.cargo==g.cargo[0] && practice.tow.cargo==null,"practice isolates tow from suspended shift");
        ReplayInput.tap(g,Layout.PRACTICE);
        check(g.activeGame()==g && g.tow.cargo==g.cargo[0],"exit practice restores shift and tow");
        g=fixture();g.tug.y=400;check(!g.canTow(g.cargo[0]),"distant loads cannot be connected");
        g=fixture();check(!g.canTow(new Body(-2,2,300,690,0)),"incoming NPC retains load until handoff");
        g.cargo[0].awaitingPickup=false;g.cargo[0].lineAttached=true;
        check(!g.canTow(g.cargo[0]),"secured loads cannot be towed");
        g=fixture();connect(g);g.cargo[1]=new Body(2,1,350,650,0);
        steps(g,Game.ROPE_HIT_TICKS);check(g.canTow(g.cargo[1]),"another barge may link after the previous catch completes");
        g.background();y=g.cargo[0].y;steps(g,60);
        check(g.tow.cargo==g.cargo[0] && g.cargo[0].y==y,"background holds tow without advancing");
        g.configureCanvas(1333,600);g.pause();steps(g,5);
        check(g.tow.cargo==g.cargo[0],"resume and resize retain cable");
        ReplayInput.releaseTow(g);check(g.tow.cargo==null,"responsive barge tap releases tow");
        g=fixture();connect(g);g.reset();check(g.tow.cargo==null,"restart clears tow references");
        g=fixture();connect(g);g.cargo[0].age=g.contractSeconds()+1;g.step();
        check(g.tow.cargo==g.cargo[0] && g.cargo[0].active,"old cargo retains its tow because goal levels have no expiry");
        ReplayInput.releaseTow(g);check(g.tow.cargo==null && g.cargo[0].active,"explicit detach releases an old load without removing it");

        g=new Game();g.reset();steps(g,180);b=g.cargo[0];
        check(b!=null && b.awaitingPickup,"feeder parks delivered load for pickup");
        g.tug=new Body(0,-1,b.x,b.y-120,-(float)Math.PI/2);connect(g);drive(g,0,-100);steps(g,90);
        check(!b.awaitingPickup && g.tow.cargo==b && b.y<Feeder.DROP_Y-30,"tow picks up newly dropped feeder load without bumping it");

        // Slack does nothing; tension conserves momentum and acts at the end of a rotated hull.
        g=fixture();b=g.cargo[0];g.tow.attach(g,b);g.tow.length=200;g.tow.step(g);
        check(b.speed()==0 && g.tug.speed()==0 && b.spin==0,"slack cannot exert force or torque");
        g.tow.length=45;g.tug.x+=20;g.tow.step(g);
        check(b.speed()>0 && Math.abs(b.spin)>.001,"off-axis pull yaws cargo");
        check(Math.abs(b.mass*b.vx+g.tug.mass*g.tug.vx)<.0001
                && Math.abs(b.mass*b.vy+g.tug.mass*g.tug.vy)<.0001,"line conserves linear momentum");
        check(g.tow.tension<=240,"line force is bounded");

        // Recover an angled hull against the bank through the public touch interface.
        g=fixture();b=g.cargo[0];b.x=Physics.LEFT+b.radius;b.y=600;
        g.tug.x=290;g.tug.y=540;g.tug.angle=0;connect(g);
        float oldX=b.x;drive(g,65,-75);steps(g,100);
        check(g.tow.cargo==b && b.x>oldX+30,"tow frees bank-side barge: "+b.x+","+b.y);
        check(g.hull==100,"recovery avoids hard collisions");

        g=fixture();b=g.cargo[0];b.x=230;b.y=290;g.tug.x=330;g.tug.y=290;g.tug.angle=0;
        connect(g);steps(g,24);ReplayInput.lasso(g,b);steps(g,18);
        check(b.lineAttached && g.tow.cargo==null,"shore catch hands tow over to berth winch");
        steps(g,Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS);
        check(g.delivered==1,"towed load can finish ordinary delivery");

        g=fixture();g.challengeId=1;g.tug.x=410;g.tug.y=480;g.cargo[0].x=590;g.cargo[0].y=480;
        check(!g.canTow(g.cargo[0]),"cannot cast through finger pier");
        g=fixture();g.tutorial=new Tutorial();g.bridge.phase=Bridge.CLOSED;g.tutorial.lesson=Tutorial.LINK;g.tug.y=140;g.cargo[0].y=260;
        check(!g.canTow(g.cargo[0]),"cannot cast across closed bridge");
        g.bridge.phase=Bridge.OPEN;connect(g);check(g.tow.cargo==g.cargo[0],"open tutorial bridge permits an actual tow connection");g.bridge.ticks=720;g.bridge.step(g);
        check(g.bridge.phase==Bridge.OPEN,"bridge holds open for tow connection");

        // A bank overlap must not erase outward momentum while a hull turns.
        b=new Body(1,2,188,600,-1.3f);b.vx=12;Physics.banks(b,215,817,false);
        check(b.vx==12,"bank correction preserves velocity away from shore");
        Game low=fixture(),high=fixture();connect(low);connect(high);drive(low,0,-100);drive(high,0,-100);
        for(int i=0;i<90;i++) low.advance(1.0/30);
        for(int i=0;i<432;i++) high.advance(1.0/144);
        check(low.tug.x==high.tug.x && low.tug.y==high.tug.y && low.cargo[0].y==high.cargo[0].y
                && low.cargo[0].angle==high.cargo[0].angle && low.tow.tension==high.tow.tension,"tow replay identical at 30/144 Hz");
        float tension=low.tow.tension;int ticks=low.tow.catchTicks;
        check(Parity.frame(low)==Parity.frame(low) && low.tow.tension==tension && low.tow.catchTicks==ticks,"tow renderer is pure");
        System.out.println(checks+" tow gesture, recovery, physics, lifecycle and delivery assertions passed");
    }
}
