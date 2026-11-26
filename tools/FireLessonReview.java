package com.bargeboss.game;
/** Actual shared scene, public activation/nozzle input and bounded effect command counts. */
public final class FireLessonReview {
    public static void main(String[] args)throws Exception{
        Game g=BargeAbilitiesTest.fixture();g.tutorial.lesson=Tutorial.SHOW;g.tutorial.setup(g);
        for(int i=0;i<100;i++)g.step();DialogueReview.capture(g,"fire-lesson-ready");
        for(int i=0;i<95;i++){ReplayInput.tutorial(g);g.step();}
        DialogueReview.capture(g,"fire-lesson-fireworks");
        for(int i=0;i<160;i++)g.step();DialogueReview.capture(g,"fire-lesson-mishap");
        for(int i=0;i<3000 && g.tutorial.lesson!=Tutorial.EXTINGUISH;i++){ReplayInput.tutorial(g);g.step();}
        for(int i=0;i<110;i++){ReplayInput.tutorial(g);g.step();}
        DialogueReview.capture(g,"fire-lesson-spray");
        for(int i=0;i<700 && g.tutorial.lesson<Tutorial.CHALLENGE;i++){ReplayInput.tutorial(g);g.step();}
        if(g.tutorial.lesson<Tutorial.CHALLENGE)throw new AssertionError("fire lesson stuck; heat="+g.tutorial.fireworks.heat);
        for(int i=0;i<80;i++)g.step();DialogueReview.capture(g,"fire-lesson-complete");
    }
}
