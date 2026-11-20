package com.bargeboss.game;
/** Actual renderer snapshots earned through the public continuous tutorial replay. */
public final class TutorialNavigationReview {
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        boolean slip=false,map=false,reverse=false;
        for(int tick=0;tick<15000 && g.tutorial.lesson<Tutorial.CHANNEL;tick++){
            if(!slip && g.tutorial.lesson==Tutorial.SLIP && g.tug.x<-1860){DialogueReview.capture(root,"tutorial-slip-reverse");slip=true;}
            if(!reverse && g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.orbitProgress()>.4f){DialogueReview.capture(root,"tutorial-reverse-eight");reverse=true;}
            if(!map && g.tutorial.lesson==Tutorial.MAP && g.tutorial.navigation.mapPhase==2){
                for(int wait=0;wait<180;wait++)root.step();DialogueReview.capture(root,"tutorial-map-zoom-in");map=true;
            }
            ReplayInput.tutorial(root);root.step();
        }
        if(!slip || !map || !reverse)throw new AssertionError("Missing navigation review phase");
        DialogueReview.capture(root,"tutorial-map-target-reached");
    }
}
