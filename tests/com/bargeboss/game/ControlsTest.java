package com.bargeboss.game;

public final class ControlsTest {
    private static int count;
    private static void check(boolean ok,String message) { count++; if(!ok) throw new AssertionError(message); }
    private static Game game() { Game g=new Game(); ReplayInput.controlFixture(g); return g; }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game ready() {
        Game g=game(); Body b=g.cargo[0]; b.x=Game.DOCK_X[2]; b.y=Game.DOCK_Y[2]; return g;
    }
    public static void run() {
        Game g=game(); ReplayInput.tap(g,RiverPainter.screenX(360,580),RiverPainter.screenY(360,580));
        check(!g.navigating,"water taps no longer set a route");
        ReplayInput.down(g,g.tug.x,g.tug.y); ReplayInput.move(g,g.downX,g.downY-100);
        float originY=g.downY; steps(g,60);
        check(g.navigating && g.tug.y<originY-50,"held drag drives tug");
        check(g.downY==originY,"drag origin stays anchored as boat moves");
        g.pointerDown(2,200,400); g.pointerMove(2,100,400); g.pointerUp(2,100,400);
        check(g.downY==originY && g.pointer,"second finger cannot steal drag");
        ReplayInput.move(g,g.downX,g.downY); steps(g,30);
        check(g.braking && g.tug.speed()<5,"centering drag brakes");
        ReplayInput.move(g,g.downX,g.downY-100); steps(g,30); ReplayInput.up(g,g.pointerX,g.pointerY);
        check(!g.pointer && !g.navigating && !g.braking,"release removes thrust and coasts");
        ReplayInput.down(g,g.tug.x,g.tug.y); ReplayInput.move(g,g.downX,g.downY-100); steps(g,15);
        g.background(); check(!g.pointer && !g.navigating && g.mode==Game.PAUSE,"background cancels drive");
        g=game(); g.cargo[1].x=300; g.cargo[1].y=559;
        g.pointerDown(0,300,559); steps(g,20);
        check(g.heldCargo()==null && !g.pushing,"footer coordinates cannot grab cargo at the same numeric world coordinates");
        g.pointerUp(0,300,559);
        ReplayInput.down(g,g.tug.x,g.tug.y); ReplayInput.move(g,g.downX,g.downY-100); steps(g,15);
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
        ReplayInput.move(g,Game.TIE_X[1],Game.DOCK_Y[1]); steps(g,30);
        check(g.ropePhase==0 && !b.lineAttached,"dragging only aims: no rope launches before release");
        ReplayInput.up(g,Game.TIE_X[1],Game.DOCK_Y[1]);
        check(g.ropePhase==1 && !b.lineAttached,"finger release launches a missed throw too");
        steps(g,Game.ROPE_FLIGHT_TICKS);
        check(g.ropePhase==2 && !b.lineAttached && g.delivered==0,"miss enters visible retraction after reaching endpoint");
        steps(g,Game.ROPE_RETURN_TICKS);
        check(g.ropePhase==0 && g.ropeBody==null,"miss reels all the way back and rearms");
        ReplayInput.down(g,b.x,b.y); ReplayInput.move(g,Game.TIE_X[2],Game.DOCK_Y[2]); g.pointerCancel(0);
        check(!b.lineAttached && !g.ropeDragging && g.ropePhase==0,"cancelled aim never shoots");
        ReplayInput.lasso(g,b);
        check(g.ropePhase==1 && !b.lineAttached && !g.ropeDragging,"correct throw still waits for flight impact");
        g.background(); steps(g,60); check(g.ropeTicks==0 && !b.lineAttached,"background freezes an in-flight throw"); g.pause();
        steps(g,Game.ROPE_FLIGHT_TICKS-1); check(!b.lineAttached,"line cannot attach before reaching bollard");
        g.step(); check(g.ropePhase==3 && b.lineAttached && g.pollHaptic()==Game.HAPTIC_CONTACT,"bollard hit attaches with visual event and contact feedback");
        float x=b.x,y=b.y; steps(g,60); check(g.delivered==0 && b.unloadingTicks==0,"line tightens before unloading");
        while(b.unloadingTicks==0) g.step();
        check(b.lineSnapTicks==18 && g.pollHaptic()==Game.HAPTIC_CONTACT,"taut line emits one snap highlight and feedback before unloading");
        g.background(); int pausedUnload=b.unloadingTicks; steps(g,60);
        check(b.unloadingTicks==pausedUnload && g.delivered==0,"background freezes crew and unloading clock");
        g.pause(); g.tug.x=b.x+40; g.tug.y=b.y; g.tug.vx=-70;
        steps(g,Game.UNLOAD_TICKS-2);
        check(b.active && b.unloadingTicks==Game.UNLOAD_TICKS-1 && g.score==0,"unloading occupies the berth for three seconds before scoring");
        g.step(); check(!b.active && g.delivered==1 && g.score>0,"completed unloading delivers once and crew returns idle");
        check(b.x==x && b.y==y,"tied barge stays secured during unloading");
        check(b.lineSnapTicks==0,"snap effect completes while the crew works");
        int score=g.score; steps(g,30); check(g.score==score,"unloading cannot score twice");
        g=ready(); b=g.cargo[0]; b.vx=50; ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"fast cargo cannot start a tie");
        g=ready(); b=g.cargo[0]; b.x+=40; ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"whole hull must fit before tying");
        g=game(); g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==Game.HAPTIC_TICK,"semantic haptic drains once");
        check(g.pollHaptic()==0,"haptics do not repeat without event");
        g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==0,"ticks are rate limited");
        g.haptic(Game.HAPTIC_IMPACT); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==Game.HAPTIC_SUCCESS,"success wins feedback priority");
        ReplayInput.tap(g,Layout.HAPTICS); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==0,"haptics switch disables feedback");
        Game root=new Game(); ReplayInput.tap(root,Layout.LEARN); g=root.activeGame();
        steps(root,2000); check(g.tutorial.lesson==Tutorial.MOVE && g.hull==100,"waiting never advances practice");
        int seen=0;
        for(int i=0;i<3000 && g.tutorial.lesson!=Tutorial.DONE;i++) {
            seen|=1<<g.tutorial.lesson; ReplayInput.tutorial(root); root.step();
        }
        check(g.tutorial.lesson==Tutorial.DONE && seen==63,"all six lessons require real gestures and unloading: lesson="+g.tutorial.lesson);
        ReplayInput.tap(root,Layout.START); check(root.activeGame()==root && root.mode==Game.PLAY && root.ticks==0,"completed tutorial starts shift");
        root=game(); root.score=321; steps(root,12); int ticks=root.ticks; x=root.tug.x; y=root.tug.y;
        ReplayInput.tap(root,Layout.PRACTICE); steps(root,300); ReplayInput.tap(root,Layout.PRACTICE);
        check(root.activeGame()==root && root.mode==Game.PLAY && root.score==321 && root.ticks==ticks && root.tug.x==x && root.tug.y==y,"practice preserves suspended shift");
        root=new Game(); ReplayInput.tap(root,Layout.LEARN); g=root.activeGame();
        g.tutorial.lesson=Tutorial.TIE; g.tutorial.setup(g); Body oldCargo=g.cargo[0];
        ReplayInput.lasso(root,oldCargo); check(g.ropePhase==1,"practice lasso enters flight");
        ReplayInput.tap(root,Layout.HELP);
        check(g.cargo[0]!=oldCargo && g.ropePhase==0 && g.ropeBody==null,"reset lesson removes in-flight rope and replaces cargo");
        steps(root,Game.ROPE_FLIGHT_TICKS+1);
        check(!g.cargo[0].lineAttached && !oldCargo.lineAttached && g.delivered==0,"reset throw cannot attach an obsolete body or score later");
        root=new Game(); ReplayInput.tap(root,Layout.LEARN); root.background();
        check(root.activeGame().mode==Game.PAUSE,"practice pauses on background");
        ReplayInput.tap(root,Layout.RESUME); ReplayInput.tap(root,Layout.PRACTICE);
        check(root.activeGame()==root && root.mode==Game.TITLE,"early practice exit returns to title");
        System.out.println(count+" drag, lasso, unloading, haptic and tutorial assertions passed");
    }
}
