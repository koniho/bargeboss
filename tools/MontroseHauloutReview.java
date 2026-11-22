package com.bargeboss.game;

/** Actual shared renderer: dispatch, surveyed haulout, retained crew handoff and chart. */
public final class MontroseHauloutReview {
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.configureCanvas(1333,750);g.selectChallenge(Challenges.MONTROSE);g.reset();
        for(int i=0;i<24;i++)g.step();DialogueReview.capture(g,"montrose-dockmaster");
        g.dialogue.close(g);g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHarbor.mapY(255),2.4f,666,420);
        DialogueReview.capture(g,"montrose-haulout-dusk");
        g.haulout.requested=true;
        for(int tick=1;tick<=240;tick++){
            g.step();g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHarbor.mapY(255),2.4f,666,420);
            if(tick==90 || tick==175 || tick==240)DialogueReview.capture(g,"montrose-lift-"+tick);
        }
        g=MontroseHauloutTest.voyage(false);g.configureCanvas(1333,750);g.dialogue.close(g);
        g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHarbor.mapY(255),2.4f,666,420);
        DialogueReview.capture(g,"montrose-haulout-delivered");
        g.layout=MontroseHarbor.overview(1333,750,false);DialogueReview.capture(g,"montrose-haulout-overview");
    }
}
