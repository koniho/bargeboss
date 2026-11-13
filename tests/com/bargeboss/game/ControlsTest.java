package com.bargeboss.game;

public final class ControlsTest {
    private static int count;
    private static void check(boolean ok,String message) { count++; if(!ok) throw new AssertionError(message); }
    private static Game game() { Game g=new Game(); g.reset(); return g; }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game ready() {
        Game g=game(); Body b=g.cargo[0]; b.x=Game.DOCK_X[2]; b.y=Game.DOCK_Y[2]; return g;
    }
    public static void run() {
        Game g=game(); ReplayInput.tap(g,RiverPainter.screenX(360),RiverPainter.screenY(580));
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
        // Two original motion steps per real tick; contract clocks still measure real seconds.
        g=game(); g.cargo[1].active=false; Body b=g.cargo[0]; b.x=300; b.y=550; b.vx=10;
        Body reference=new Body(b.id,b.berth,b.x,b.y,b.angle); reference.vx=10;
        for(int i=0;i<120;i++) Physics.integrate(reference);
        steps(g,60);
        check(b.x==reference.x && b.y==reference.y,"one real second advances two seconds of original cargo motion");
        check(Math.abs(b.age-1)<.0001 && g.ticks==60,"movement boost leaves shift and contract clocks unchanged");
        g=ready(); b=g.cargo[0]; steps(g,180);
        check(g.delivered==0 && b.dockTime==0,"entering berth alone never scores");
        ReplayInput.down(g,b.x,b.y); check(g.ropeDragging,"ready cargo starts a rope drag");
        ReplayInput.move(g,Game.TIE_X[1],Game.DOCK_Y[1]); ReplayInput.up(g,Game.TIE_X[1],Game.DOCK_Y[1]);
        check(!b.lineAttached && g.delivered==0,"wrong bollard cancels throw");
        ReplayInput.down(g,b.x,b.y); ReplayInput.move(g,Game.TIE_X[2],Game.DOCK_Y[2]); g.pointerCancel(0);
        check(!b.lineAttached && !g.ropeDragging,"cancelled touch cannot attach rope");
        ReplayInput.lasso(g,b); check(b.lineAttached && !g.ropeDragging,"drop over correct bollard attaches line");
        float x=b.x,y=b.y; steps(g,60); check(g.delivered==0 && b.unloadingTicks==0,"line tightens before unloading");
        while(b.unloadingTicks==0) g.step();
        g.background(); int pausedUnload=b.unloadingTicks; steps(g,60);
        check(b.unloadingTicks==pausedUnload && g.delivered==0,"background freezes crew and unloading clock");
        g.pause(); g.tug.x=b.x+40; g.tug.y=b.y; g.tug.vx=-70;
        steps(g,Game.UNLOAD_TICKS-2);
        check(b.active && b.unloadingTicks==Game.UNLOAD_TICKS-1 && g.score==0,"unloading occupies the berth for three seconds before scoring");
        g.step(); check(!b.active && g.delivered==1 && g.score>0,"completed unloading delivers once and crew returns idle");
        check(b.x==x && b.y==y,"tied barge stays secured during unloading");
        int score=g.score; steps(g,30); check(g.score==score,"unloading cannot score twice");
        g=ready(); b=g.cargo[0]; b.vx=50; ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"fast cargo cannot start a tie");
        g=ready(); b=g.cargo[0]; b.x+=40; ReplayInput.down(g,b.x,b.y);
        check(!g.ropeDragging,"whole hull must fit before tying");
        g=game(); g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==Game.HAPTIC_TICK,"semantic haptic drains once");
        check(g.pollHaptic()==0,"haptics do not repeat without event");
        g.haptic(Game.HAPTIC_TICK); check(g.pollHaptic()==0,"ticks are rate limited");
        g.haptic(Game.HAPTIC_IMPACT); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==Game.HAPTIC_SUCCESS,"success wins feedback priority");
        ReplayInput.tap(g,438,55); g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==0,"haptics switch disables feedback");
        Game root=new Game(); ReplayInput.tap(root,300,608); g=root.activeGame();
        steps(root,2000); check(g.tutorial.lesson==Tutorial.MOVE && g.hull==100,"waiting never advances practice");
        int seen=0;
        for(int i=0;i<3000 && g.tutorial.lesson!=Tutorial.DONE;i++) {
            seen|=1<<g.tutorial.lesson; ReplayInput.tutorial(root); root.step();
        }
        check(g.tutorial.lesson==Tutorial.DONE && seen==63,"all six lessons require real gestures and unloading: lesson="+g.tutorial.lesson);
        ReplayInput.tap(root,300,680); check(root.activeGame()==root && root.mode==Game.PLAY && root.ticks==0,"completed tutorial starts shift");
        root=game(); root.score=321; steps(root,12); int ticks=root.ticks; x=root.tug.x; y=root.tug.y;
        ReplayInput.tap(root,480,910); steps(root,300); ReplayInput.tap(root,480,910);
        check(root.activeGame()==root && root.mode==Game.PLAY && root.score==321 && root.ticks==ticks && root.tug.x==x && root.tug.y==y,"practice preserves suspended shift");
        root=new Game(); ReplayInput.tap(root,300,608); root.background();
        check(root.activeGame().mode==Game.PAUSE,"practice pauses on background");
        ReplayInput.tap(root,300,570); ReplayInput.tap(root,480,910);
        check(root.activeGame()==root && root.mode==Game.TITLE,"early practice exit returns to title");
        System.out.println(count+" drag, lasso, unloading, haptic and tutorial assertions passed");
    }
}
