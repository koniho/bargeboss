package com.bargeboss.game;

/** Actual shared renderer: slip throw, moving tether and released line. */
public final class SlipMooringReview {
    public static void main(String[] args)throws Exception{
        Game g=SlipMooringTest.fixture();g.configureCanvas(1333,600);g.fitTutorialCamera();
        for(int i=0;i<80;i++)g.step();
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.bollardX(g.tug),g.bollardY(g.tug));
        DialogueReview.capture(g,"slip-tether-aim");
        ReplayInput.up(g,g.bollardX(g.tug),g.bollardY(g.tug));
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS+35;i++)g.step();
        DialogueReview.capture(g,"slip-tether-caught");
        ReplayInput.throttle(g,90,0);for(int i=0;i<90;i++)g.step();
        DialogueReview.capture(g,"slip-tether-powered");g.cancelInputs();
        ReplayInput.down(g,g.bollardX(g.tug),g.bollardY(g.tug));ReplayInput.up(g,g.bollardX(g.tug),g.bollardY(g.tug));
        for(int i=0;i<60;i++)g.step();DialogueReview.capture(g,"slip-tether-released");
    }
}
