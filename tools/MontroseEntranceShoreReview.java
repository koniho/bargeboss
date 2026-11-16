package com.bargeboss.game;
/** Actual shared-renderer green-marker shoreline and concrete landing. */
public final class MontroseEntranceShoreReview {
    public static void main(String[] args)throws Exception{Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.configureCanvas(1333,750);
        for(float zoom:new float[]{1.2f,2.4f}){g.layout=Layout.focused(1333,750,MontroseHarbor.mapX(275),MontroseHarbor.mapY(1250),zoom,666,400);MontroseReview.capture(g,zoom<2?"montrose-green-shore-context-v1":"montrose-green-shore-close-v1");}
    }
}
