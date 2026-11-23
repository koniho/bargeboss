package com.bargeboss.game;

/** Real pointer timing: feedback precedes transitions, cancellation and repeated fingers are safe. */
final class MenuFeedbackTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void run(){
        Game g=new Game();Layout.Box b=g.controlBox(Layout.SETTINGS);
        FrameGate gate=new FrameGate();gate.rendered(g);
        g.pointerDown(1,b.centerX(),b.centerY());
        for(int i=0;i<12;i++)g.advance(1.0/60);
        float pressedAlpha=g.menuFeedback.alpha();g.pointerUp(1,b.centerX(),b.centerY());
        check(Math.abs(g.menuFeedback.alpha()-pressedAlpha)<.00001f,"release never restarts pulse brightness");
        check(g.mode==Game.TITLE && g.menuFeedback.busy(),"release acknowledges before switching screen");
        g.advance(1.0/60);check(g.menuFeedback.alpha()<pressedAlpha,"release fades continuously");check(g.mode==Game.TITLE && gate.needsFrame(g),"pulse requests a frame before action");
        g.pointerUp(1,b.centerX(),b.centerY());
        for(int i=0;i<8;i++)g.advance(1.0/60);
        check(g.mode==Game.SETTINGS && !g.menuFeedback.busy(),"single delayed transition commits");
        b=g.controlBox(Layout.SOUND_MUTE);boolean muted=g.settings.soundMuted();
        g.pointerDown(2,b.centerX(),b.centerY());g.pointerUp(2,b.centerX(),b.centerY());
        g.background();for(int i=0;i<12;i++)g.advance(1.0/60);
        check(g.settings.soundMuted()==muted,"background cancels pending selection");
        gate.rendered(g);g.pointerDown(3,b.centerX(),b.centerY());
        g.pointerMove(3,b.x-40,b.y);g.pointerUp(3,b.x-40,b.y);
        check(!g.menuFeedback.busy() && g.settings.soundMuted()==muted,"drag away does not choose");
        g.reset();g.dialogue.show(g,"TEST","REPLY","DETAIL","YES","NO");
        for(int i=0;i<40;i++)g.dialogue.step();b=g.dialogue.choiceBox(g,1);
        g.pointerDown(4,b.centerX(),b.centerY());g.pointerUp(4,b.centerX(),b.centerY());
        check(g.dialogue.result()<0,"reply waits for visible acknowledgement");
        ReplayInput.finishMenu(g);check(g.dialogue.result()==1,"reply commits after pulse");
        g=new Game();g.mode=Game.CHALLENGES;LevelAssets.Plan plan=LevelAssets.upcoming(g);
        for(int id:new int[]{0,Challenges.MONTROSE,Challenges.INDIANA}){
            g.selectChallenge(id);check(LevelAssets.upcoming(g)==plan,"browsing does not replace preparation plan");
        }
        g.reset();check(LevelAssets.upcoming(g)==LevelAssets.challenge(g.challengeId),"departure prepares full selected level");
        System.out.println("Menu acknowledgement timing, cancellation, reply and stable browsing manifest passed");
    }
}
