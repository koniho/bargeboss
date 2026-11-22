package com.bargeboss.game;

/** Rope selection cannot turn a hold, drag, cancellation or competing pointer into cast-off. */
public final class TowReleaseTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game fixture(){Game g=BargeAbilitiesTest.fixture();g.tow.attach(g,g.cargo[0]);return g;}
    public static void run(){
        for(int mode=0;mode<4;mode++){
            Game g=fixture();Body b=g.cargo[0];Towline l=g.tow.incoming(b);
            float x=TowRelease.point(g,l,.5f,true),y=TowRelease.point(g,l,.5f,false);
            g.pointerDown(77,x,y);check(g.towRelease.owns(77),"visible rope captures its own pointer");
            if(mode==0){g.pointerMove(77,x+30,y);g.pointerMove(77,x,y);g.pointerUp(77,x,y);}
            if(mode==1){g.pointerCancel(77);g.pointerUp(77,x,y);}
            if(mode==2){for(int i=0;i<20;i++)g.step();g.pointerUp(77,x,y);}
            if(mode==3){g.pointerDown(78,x,y);g.pointerUp(78,x,y);check(g.towRelease.owns(77),"second pointer cannot steal rope gesture");g.pointerCancel(77);}
            check(g.tow.incoming(b)!=null,"drag, cancel, hold or competing pointer cannot release");
        }
        Game g=fixture();Body b=g.cargo[0];Towline l=g.tow.incoming(b);
        float x=TowRelease.point(g,l,.5f,true),y=TowRelease.point(g,l,.5f,false);
        ReplayInput.tap(g,x,y);check(g.tow.incoming(b)==null && g.towReleases==1,"short rope tap releases exactly its connection");
        System.out.println("Towline tap, hold/drag rejection, cancellation and pointer ownership passed");
    }
    public static void main(String[] args){run();}
}
