package com.bargeboss.game;
/** Full shared-renderer review of the service crane west of Dock O. */
public final class MontroseBoatHoistReview {
    public static void main(String[] args)throws Exception{
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.configureCanvas(1333,750);
        for(float zoom:new float[]{1.25f,3f}){g.layout=Layout.focused(1333,750,MontroseBoatHoist.X+20,MontroseBoatHoist.Y+40,zoom,666,435);MontroseReview.capture(g,zoom<2?"montrose-haulout-context-v1":"montrose-haulout-close-v1");}
    }
}
