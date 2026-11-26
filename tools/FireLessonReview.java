package com.bargeboss.game;
/** Actual shared scene, public activation/nozzle input and the six-fire towed response:
 * all six burning (live camera and a wide overview), mid-extinguish, and all out. */
public final class FireLessonReview {
    public static void main(String[] args)throws Exception{
        Game g=BargeAbilitiesTest.fixture();g.tutorial.lesson=Tutorial.SHOW;g.tutorial.setup(g);
        for(int i=0;i<100;i++)g.step();DialogueReview.capture(g,"fire-lesson-ready");
        for(int i=0;i<95;i++){ReplayInput.tutorial(g);g.step();}
        DialogueReview.capture(g,"fire-lesson-fireworks");
        for(int i=0;i<160;i++)g.step();DialogueReview.capture(g,"fire-lesson-mishap");
        for(int i=0;i<3000 && g.tutorial.lesson!=Tutorial.EXTINGUISH;i++){ReplayInput.tutorial(g);g.step();}
        Game root=g.activeGame();FireworksLesson f=root.tutorial.fireworks;
        DialogueReview.capture(g,"fire-lesson-six-burning");
        Layout live=root.layout;
        root.layout=Layout.focused(live.width,live.height,130,-1340,.85f*live.height/600,live.width*.5f,live.height*.5f);
        DialogueReview.capture(g,"fire-lesson-six-overview");root.layout=live;
        for(int i=0;i<110;i++){ReplayInput.tutorial(g);g.step();}
        DialogueReview.capture(g,"fire-lesson-spray");
        boolean mid=false,towing=false,out=false;
        for(int i=0;i<6000 && root.tutorial.lesson<Tutorial.CHALLENGE;i++){ReplayInput.tutorial(g);g.step();
            if(!towing && f.remaining()==4 && !f.onStation(root.cargo[3])){for(int k=0;k<60;k++){ReplayInput.tutorial(g);g.step();}DialogueReview.capture(g,"fire-lesson-tow-north");towing=true;}
            if(!mid && f.remaining()==3 && root.abilities.spraying){DialogueReview.capture(g,"fire-lesson-mid-extinguish");mid=true;}
            if(!out && f.remaining()==0){for(int k=0;k<40;k++)g.step();DialogueReview.capture(g,"fire-lesson-all-out");out=true;}
        }
        if(root.tutorial.lesson<Tutorial.CHALLENGE)throw new AssertionError("fire lesson stuck; heat="+f.heat);
        for(int i=0;i<80;i++)g.step();DialogueReview.capture(g,"fire-lesson-complete");
        root.layout=Layout.focused(live.width,live.height,130,-1340,.85f*live.height/600,live.width*.5f,live.height*.5f);
        DialogueReview.capture(g,"fire-lesson-out-overview");
    }
}
