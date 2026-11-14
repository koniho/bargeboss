package com.bargeboss.game;

/** Redraw cadence must follow shared state without changing simulation or event delivery. */
public final class FrameGateTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok)throw new AssertionError(message);}
    public static void run() {
        for(int hz:new int[]{60,120,144}) for(boolean play:new boolean[]{false,true}) {
            Game game=new Game(),control=new Game();if(play){game.reset();control.reset();}
            FrameGate gate=new FrameGate();check(gate.needsFrame(game),"first frame is mandatory");gate.rendered(game);
            int frames=0;
            for(int i=0;i<hz*2;i++) {
                game.advance(1.0/hz);control.advance(1.0/hz);
                game.pollHaptic();control.pollHaptic();
                if(gate.needsFrame(game)) {frames++;gate.rendered(game);}
            }
            check(frames==120,"60 Hz visual clock yields120 redraws in2seconds at host "+hz+"Hz, play="+play+", actual="+frames);
            check(game.ticks==control.ticks && game.visualTicks==control.visualTicks && game.tug.x==control.tug.x && game.tug.y==control.tug.y,
                    "redraw decisions never alter simulation");
        }
        Game game=new Game();game.reset();FrameGate gate=new FrameGate();gate.rendered(game);
        game.pause();check(gate.needsFrame(game),"pause mode changes redraw without a simulation step");gate.rendered(game);
        for(int i=0;i<288;i++) {game.advance(1.0/144);check(!gate.needsFrame(game),"paused idle has no redundant redraws");}
        gate.markDirty();check(gate.needsFrame(game) && gate.needsFrame(game),"request stays dirty until actual draw, even if native draws coalesce");
        gate.rendered(game);check(!gate.needsFrame(game),"completed draw consumes dirtiness");
        game.hapticsEnabled=!game.hapticsEnabled;check(gate.needsFrame(game),"haptics-menu state redraws while paused");gate.rendered(game);
        game.configureCanvas(1333,600);check(gate.needsFrame(game),"resizing redraws even at unchanged clock");gate.rendered(game);
        game.pause();gate.rendered(game);int tick=game.ticks;
        ReplayInput.down(game,game.tug.x,game.tug.y);gate.markDirty();check(gate.needsFrame(game) && game.ticks==tick,"pointer state redraws immediately before next step");gate.rendered(game);
        ReplayInput.move(game,game.tug.x+60,game.tug.y);gate.markDirty();check(gate.needsFrame(game),"drag target redraws before next step");gate.rendered(game);
        game.cancelInputs();gate.markDirty();check(gate.needsFrame(game),"input cancellation redraws");gate.rendered(game);
        game.background();gate.markDirty();check(gate.needsFrame(game),"background renders paused state");gate.rendered(game);
        gate.markDirty();check(gate.needsFrame(game),"native resume/focus exposure redraws an otherwise unchanged paused scene");gate.rendered(game);
        game.pause();gate.rendered(game);game.hapticsEnabled=true;gate.rendered(game);game.haptic(Game.HAPTIC_SUCCESS);
        check(!gate.needsFrame(game) && game.pollHaptic()==Game.HAPTIC_SUCCESS,"haptic polling remains independent of rendering");

        Game root=new Game();gate.rendered(root);root.startTutorial();Game practice=root.activeGame();
        check(practice!=root && gate.needsFrame(root),"practice active-game identity invalidates previous frame");gate.rendered(root);
        root.advance(1.0/120);check(!gate.needsFrame(root),"practice half-step does not redraw root title");
        root.advance(1.0/120);check(gate.needsFrame(root),"practice visual clock triggers redraw although root clock is unchanged");gate.rendered(root);
        root.background();check(gate.needsFrame(root),"practice background observes delegated mode");gate.rendered(root);
        root.advance(.2);check(!gate.needsFrame(root),"paused practice remains idle");
        root.pause();gate.rendered(root);ReplayInput.tap(root,Layout.PRACTICE);
        check(root.activeGame()==root && gate.needsFrame(root),"exiting practice redraws restored game even without fixed step");gate.rendered(root);
        root.mode=Game.HELP;check(gate.needsFrame(root),"help state invalidates paused/static frame");gate.rendered(root);
        root.advance(.2);check(!gate.needsFrame(root),"static help does not redraw repeatedly");
        check(gate.needsFrame(new Game()),"new root identity redraws even at matching clocks");
        System.out.println(checks+" shared frame cadence, input, practice, lifecycle and haptic assertions passed");
    }
    public static void main(String[] args) {run();}
}
