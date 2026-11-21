package com.bargeboss.game;
public final class LessonCoachingTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game root=new Game();root.startTutorial();Game g=root.activeGame();
        LessonFeedback f=g.tutorial.feedback;f.reset();
        f.completed(g,Tutorial.MOVE,Tutorial.FIGURE_EIGHT);
        check(f.active() && f.content().title.startsWith("NICELY"),"Clean completion praises");
        g.tutorial.navigation.retry=true;f.sample(g);g.tutorial.navigation.retry=false;
        f.completed(g,Tutorial.FIGURE_EIGHT,Tutorial.REVERSE);
        for(int i=0;i<240;i++)f.step(g);
        check(f.active() && f.content().title.startsWith("GOOD RECOVERY"),"Retry encouragement queued behind praise");
        f.completed(g,Tutorial.FIGURE_EIGHT,Tutorial.REVERSE);
        for(int i=0;i<240;i++)f.step(g);
        check(!f.active(),"Repeated crossing cannot duplicate earned coaching");
        f.completed(g,Tutorial.REVERSE,Tutorial.FIGURE_EIGHT);
        check(!f.active(),"Fallback does not earn praise");
        f.reset();check(f.content()==null,"Explicit reset clears feedback");
        g.tutorial.lesson=Tutorial.DONE;f.completed(g,Tutorial.RELEASE,Tutorial.DONE);
        for(int i=0;i<241;i++)g.step();
        check(!f.active(),"Final lesson feedback expires on DONE screen");
        System.out.println("Lesson praise, encouragement, queue, dedup and reset passed");
    }
    public static void main(String[] args){run();}
}
