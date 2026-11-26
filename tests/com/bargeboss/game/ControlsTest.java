package com.bargeboss.game;

public final class ControlsTest {
    private static int count;
    private static void check(boolean ok,String message) { count++; if(!ok) throw new AssertionError(message); }
    private static Game game() { Game g=new Game(); ReplayInput.controlFixture(g); return g; }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game ready() {
        Game g=game(); Body b=g.cargo[0]; b.x=g.dockX(2); b.y=g.dockY(2);
        // Keep this local lasso fixture visible in the close gameplay camera.
        g.tug.x=b.x+110;g.tug.y=b.y+60;return g;
    }
    public static void run() {
        Game g=game(); ReplayInput.tap(g,RiverPainter.screenX(360,580),RiverPainter.screenY(360,580));
        check(!g.navigating,"water taps no longer set a route");
        // This is a straight-drive distance test. Face the requested course;
        // locked-thrust turning is exercised separately by DriveDirectionTest.
        g.tug.angle=-(float)Math.PI/2;
        ReplayInput.throttle(g,0,-100);
        float originY=g.tug.y,screenOrigin=g.downX; steps(g,60);
        check(g.navigating && g.tug.y<originY-35,"held drag drives tug");
        check(Math.abs(g.downX-screenOrigin)<.02f,"drag origin stays anchored as boat moves");
        g.pointerDown(2,200,400); g.pointerMove(2,100,400); g.pointerUp(2,100,400);
        check(Math.abs(g.downX-screenOrigin)<.02f && g.pointer,"second finger cannot steal drag");
        ReplayInput.throttle(g,0,0); steps(g,30);
        check(!g.navigating && !g.reversing && g.tug.speed()>5 && g.control==Game.COASTING,"centering drag cuts throttle and coasts");
        ReplayInput.throttle(g,0,-100); steps(g,30); ReplayInput.up(g,g.pointerX,g.pointerY);
        check(!g.pointer && !g.navigating && !g.reversing,"release removes thrust and coasts");
        ReplayInput.throttle(g,0,-100); steps(g,15);
        g.background(); check(!g.pointer && !g.navigating && g.mode==Game.PAUSE,"background cancels drive");
        g=game(); g.cargo[1].x=300; g.cargo[1].y=559;
        g.pointerDown(0,300,559); steps(g,20);
        check(g.heldCargo()==null && !g.pushing,"footer coordinates cannot grab cargo at the same numeric world coordinates");
        g.pointerUp(0,300,559);
        ReplayInput.throttle(g,0,-100); steps(g,15);
        g.pointerMove(0,Layout.HELP.centerX(),Layout.HELP.centerY());
        g.pointerUp(0,Layout.HELP.centerX(),Layout.HELP.centerY());
        check(g.mode==Game.PLAY && !g.pointer,"board drag released over footer never opens a menu");
        // Two original motion steps per real tick; contract clocks still measure real seconds.
        g=game(); g.cargo[1].active=false; Body b=g.cargo[0]; b.x=300; b.y=550; b.vx=10;
        Body reference=new Body(b.id,b.berth,b.x,b.y,b.angle); reference.vx=10;
        for(int i=0;i<120;i++) Physics.integrate(reference);
        steps(g,60);
        check(b.x==reference.x && b.y==reference.y,"one real second advances two seconds of original cargo motion");
        check(Math.abs(b.age-1)<.0001 && g.ticks==60,"movement boost leaves shift and contract clocks unchanged");
        g=ready(); b=g.cargo[0]; steps(g,180);
        check(g.delivered==0 && b.dockTime==0,"entering berth alone never scores");
        ReplayInput.down(g,b.x,b.y); check(g.ropeDragging,"ready cargo arms the lasso");
        ReplayInput.move(g,g.bollardX(1),g.bollardY(1)); steps(g,30);
        check(g.ropePhase==0 && !b.lineAttached,"dragging only aims: no rope launches before release");
        ReplayInput.up(g,g.bollardX(1),g.bollardY(1));
        check(g.ropePhase==1 && !b.lineAttached,"finger release launches a missed throw too");
        steps(g,Game.ROPE_FLIGHT_TICKS);
        check(g.ropePhase==2 && !b.lineAttached && g.delivered==0,"miss enters visible retraction after reaching endpoint");
        steps(g,Game.ROPE_RETURN_TICKS);
        check(g.ropePhase==0 && g.ropeBody==null,"miss reels all the way back and rearms");
        ReplayInput.down(g,b.x,b.y); ReplayInput.move(g,g.bollardX(2),g.bollardY(2)); g.pointerCancel(0);
        check(!b.lineAttached && !g.ropeDragging && g.ropePhase==0,"cancelled aim never shoots");
        ReplayInput.lasso(g,b);
        check(g.ropePhase==1 && !b.lineAttached && !g.ropeDragging,"correct throw still waits for flight impact");
        g.background(); steps(g,60); check(g.ropeTicks==0 && !b.lineAttached,"background freezes an in-flight throw"); g.pause();
        steps(g,Game.ROPE_FLIGHT_TICKS-1); check(!b.lineAttached,"line cannot attach before reaching bollard");
        g.step(); check(g.ropePhase==3 && b.lineAttached && g.pollHaptic()==Game.HAPTIC_CONTACT,"bollard hit attaches with visual event and contact feedback");
        check(b.lineSnapTicks>0 && b.tieBurstTicks>0 && b.dockTime==1.2f && Mooring.aligning(b),"catch immediately snaps taut, bursts outward and starts winching");
        float x=b.x,y=b.y; g.step();
        check(b.x!=x && b.unloadingTicks==0,"winch moves the hull on the next tick without a tightening delay");
        while(b.mooringTicks<Mooring.ALIGN_TICKS) g.step();
        check(b.unloadingTicks==1 && Math.abs(StrictMath.cos(b.angle))<.0001,"crew starts only after the hull is parallel to shore");
        x=b.x;y=b.y;
        g.background(); int pausedUnload=b.unloadingTicks; steps(g,60);
        check(b.unloadingTicks==pausedUnload && g.delivered==0,"background freezes crew and unloading clock");
        g.pause(); g.tug.x=b.x+40; g.tug.y=b.y; g.tug.vx=-70;
        steps(g,Game.UNLOAD_TICKS-2);
        check(b.active && b.unloadingTicks==Game.UNLOAD_TICKS-1 && g.score==0,"unloading occupies the berth for three seconds before scoring");
        g.step(); check(b.active && b.deliveryRecorded && b.lineAttached && g.delivered==1 && g.score>0,"completed unloading delivers once and crew returns idle");
        check(b.x==x && b.y==y,"tied barge stays secured during unloading");
        check(b.lineSnapTicks==0,"snap effect completes while the crew works");
        int score=g.score; steps(g,30); check(g.score==score,"unloading cannot score twice");
        g=ready(); b=g.cargo[0]; b.vx=50; ReplayInput.down(g,b.x,b.y);
        check(g.ropeDragging,"moving cargo can start a tie");
        ReplayInput.move(g,g.bollardX(2),g.bollardY(2));ReplayInput.up(g,g.bollardX(2),g.bollardY(2));
        steps(g,Game.ROPE_FLIGHT_TICKS-1);
        check(!g.inDock(b),"moving hull may drift outside the berth while the rope is flying");g.step();
        check(b.lineAttached && b.speed()==0 && b.tieBurstTicks>0,"moving catch arrests momentum and shows success burst");
        while(b.mooringTicks<Mooring.ALIGN_TICKS) {
            x=b.x;y=b.y;g.step();
            check(Physics.length(b.x-x,b.y-y)<3,"moving catch is drawn alongside continuously");
        }
        check(g.inDock(b) && b.unloadingTicks==1,"moving catch unloads after alignment");
        g=ready();b=g.cargo[0];ReplayInput.lasso(g,b);b.y+=150;
        steps(g,Game.ROPE_FLIGHT_TICKS);
        check(!b.lineAttached && g.ropePhase==2,"cargo drifting out of rope reach misses and retracts");
        g=ready(); b=g.cargo[0]; b.x+=40; ReplayInput.down(g,b.x,b.y);
        check(g.ropeDragging && !g.inDock(b),"center inside assigned berth can arm before the full hull fits");
        g=ready();b=g.cargo[0];b.x+=70;ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"cargo outside its assigned berth cannot arm");
        g=ready();b=g.cargo[0];b.y=g.dockY(1);ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"cargo in another berth cannot arm");
        g=ready();b=g.cargo[0];b.awaitingPickup=true;ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"load still secured in feeder slot must be picked up first");
        g=game(); g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==Game.HAPTIC_TICK,"semantic haptic drains once");
        check(g.pollHaptic()==0,"haptics do not repeat without event");
        g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==0,"ticks are rate limited");
        g.haptic(Game.HAPTIC_IMPACT); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==Game.HAPTIC_SUCCESS,"success wins feedback priority");
        g.openSettings();ReplayInput.tap(g,Layout.HAPTICS); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==0,"haptics switch disables feedback");
        Game root=new Game(); ReplayInput.tap(root,Layout.LEARN); g=root.activeGame();
        steps(root,2000); check(g.tutorial.lesson==Tutorial.MOVE && g.hull==100,"waiting never advances practice");
        int seen=0;
        for(int i=0;i<19500 && g.tutorial.lesson!=Tutorial.DONE;i++) {
            seen|=1<<g.tutorial.lesson; ReplayInput.tutorial(root); root.step();
        }
        check(g.tutorial.lesson==Tutorial.DONE && seen==(((1<<Tutorial.DONE)-1)&~(1<<Tutorial.PULL)),"all fourteen lessons require real gestures and unloading: lesson="+g.tutorial.lesson);
        ReplayInput.tap(root,Layout.START); check(root.activeGame()==root && root.mode==Game.TITLE,"completed training harbor returns to title");
        root=game(); root.score=321; steps(root,12); int ticks=root.ticks; x=root.tug.x; y=root.tug.y;
        ReplayInput.tap(root,Layout.PRACTICE);
        check(root.activeGame()==root && root.tutorial==null && root.mode==Game.PLAY && root.score==321 && root.ticks==ticks,"mission HUD has no practice button");
        root=new Game(); ReplayInput.tap(root,Layout.LEARN); g=root.activeGame();
        g.tutorial.lesson=Tutorial.TIE; g.tutorial.setup(g); Body oldCargo=g.cargo[0];
        ReplayInput.lasso(root,oldCargo); check(g.ropePhase==1,"practice lasso enters flight");
        ReplayInput.tap(root,Layout.HELP);
        check(g.cargo[0]!=oldCargo && g.ropePhase==0 && g.ropeBody==null,"reset lesson removes in-flight rope and replaces cargo");
        steps(root,Game.ROPE_FLIGHT_TICKS+1);
        check(!g.cargo[0].lineAttached && !oldCargo.lineAttached && g.delivered==0,"reset throw cannot attach an obsolete body or score later");
        root=new Game(); ReplayInput.tap(root,Layout.LEARN); root.background();
        check(root.activeGame().mode==Game.PAUSE,"practice pauses on background");
        ReplayInput.tap(root,Layout.TITLE_SCREEN);
        check(root.activeGame()==root && root.mode==Game.TITLE,"early practice exit returns to title");
        for(int context=0;context<3;context++) {
            root=ReplayInput.veteran(context==1?new Game():game());
            root.configureCanvas(1280,720);
            if(context>0) root.startTutorial();
            g=root.activeGame();
            String progress=root.saveProgress();
            ReplayInput.throttle(root,0,-100);
            root.back();
            check(g.mode==Game.PAUSE,"Back pauses the active voyage or nested tutorial");
            check(!g.pointer && !g.joystick.active && !g.navigating,"pause clears held drive before title exit");
            ReplayInput.tap(root,Layout.TITLE_SCREEN);
            check(root.activeGame()==root && root.mode==Game.TITLE,"pause title button exits voyage and either practice origin: "+context);
            check(!root.pointer && !root.joystick.active && !root.navigating,"title exit leaves no held controls");
            check(root.saveProgress().equals(progress),"title exit preserves career and settings");
            int stoppedTicks=root.ticks; root.advance(.1);
            check(root.ticks==stoppedTicks,"title does not advance discarded voyage");
            ReplayInput.tap(root,Layout.START);
            check(root.activeGame()==root && root.mode==Game.PLAY && root.tutorial==null && root.ticks==0,"start after title exit creates fresh voyage");
        }
        // The removed header controls must not leave invisible UI interception.
        for(float width:new float[]{960,1333})for(float fromRight:new float[]{213,111,32}) {
            g=new Game();g.reset();g.configureCanvas(width,600);
            float sx=width-fromRight,sy=24;
            RiverCamera camera=g.layout.camera;
            g.tug.x=camera.worldX(sx,sy);g.tug.y=camera.worldY(sx,sy);
            boolean enabled=g.hapticsEnabled;
            g.pointerDown(0,sx,sy);
            check(g.pointer && !g.joystick.active && !g.navigating,"old top-button area routes tug ability touch without starting drive");
            g.pointerUp(0,sx,sy);
            check(g.mode==Game.PLAY && g.hapticsEnabled==enabled,"removed buttons cannot pause, open settings or toggle haptics");
        }
        System.out.println(count+" drag, lasso, unloading, haptic and tutorial assertions passed");
    }
}
