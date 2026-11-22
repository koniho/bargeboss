package com.bargeboss.game;
/** Reproduces the idle instructor radio prompt with a moving arrival velocity. */
public final class InstructorIdleWakeReview {
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);
        Game g=root.activeGame();g.tutorial.lesson=Tutorial.MOVE;g.tutorial.setup(g);
        g.tutorial.entering=false;g.dialogue.close(g);g.cancelInputs();
        Body b=g.teacherBoat();b.x=-1770;b.y=510;b.vx=70;b.vy=20;b.engineLoad=1;
        g.tutorial.instructorRadio.introduce(g);
        for(int i=0;i<780;i++)g.step();
        g.layout=Layout.focused(1333,600,b.x,b.y,1.8f,620,350);
        DialogueReview.capture(g,args.length==0?"instructor-idle-wake-fixed":args[0]);
    }
}
