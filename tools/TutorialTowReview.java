package com.bargeboss.game;

/** Shared-renderer captures of the actual staged single-load and convoy lessons. */
public final class TutorialTowReview {
    private static void capture(Game g,String name)throws Exception{g.cancelInputs();for(int i=0;i<650 && (i<120 || g.tutorial.feedback.active());i++)g.step();for(int i=0;i<100;i++)g.step();DialogueReview.capture(g,name);}
    public static void main(String[] args)throws Exception{
        Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.CONNECT;g.tutorial.setup(g);for(int i=0;i<70;i++)g.step();
        capture(g,"tow-course-single-load");
        boolean first=false,entry=false,pair=false,exit=false;
        for(int tick=0;tick<6000 && g.tutorial.lesson<Tutorial.LINK;tick++){
            Tutorial t=g.tutorial;
            if(!first && t.lesson==Tutorial.CASTOFF){capture(g,"tow-course-cast-off");first=true;}
            if(!entry && t.firstTowParked && t.towBridges.contactable(g)){capture(g,"tow-course-entry-call");entry=true;}
            if(!pair && g.passages.passable(1) && t.lesson==Tutorial.DOUBLE){capture(g,"tow-course-two-barges");pair=true;}
            if(!exit && t.lesson==Tutorial.BRIDGE && t.towBridges.contactable(g)){capture(g,"tow-course-convoy-call");exit=true;}
            ReplayInput.tutorial(g);g.step();
        }
        if(!first || !entry || !pair || !exit || g.tutorial.lesson!=Tutorial.LINK)throw new AssertionError("tow course did not traverse both radio gates");
        capture(g,"tow-course-convoy-clear");
    }
}
