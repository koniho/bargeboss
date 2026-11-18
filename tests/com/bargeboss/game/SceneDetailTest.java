package com.bargeboss.game;
public final class SceneDetailTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();g.layout=MontroseHarbor.overview(960,600);
        int simple=0;for(MooredBoats.Berth b:MooredBoats.BERTHS){
            float d=SceneDetail.boat(g,b.length,b.x,b.y);if(d==0)simple++;
            check(d>=0&&d<=1,"bounded boat detail");
        }
        check(simple>200,"wide harbor simplifies most moored boats");
        check(SceneDetail.boat(g,39,g.tug.x,g.tug.y)==1,"near-player lighting retained even wide");
        g.layout=Layout.focused(960,600,0,0,3,480,300);
        check(SceneDetail.boat(g,39,5000,5000)==1,"close-view boat detail retained");
        check(SceneDetail.foam(.3f,10,8,1,.2f)==0,"tiny alternate stamp skipped before clipping");
        check(SceneDetail.foam(.3f,10,8,0,.2f)==1,"wide wakes retain a stable stamp subset");
        check(SceneDetail.foam(3,10,8,1,.9f)==1,"large foam retains late life and both stamp subsets");
        float previous=0;for(int i=0;i<=1000;i++){float d=SceneDetail.fade(i*.1f,36,72);check(d>=previous&&d-previous<.005f,"continuous zoom transition");previous=d;}
        System.out.println("Screen-size detail transitions, player-light retention and foam density checks passed; "+simple+" distant boats simplified");
    }
}
