package com.bargeboss.game;

/** Real fixed-step entrance, collision clearance, input handoff and pause. */
public final class TutorialEntranceTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){for(float width:new float[]{960,1333,1600}){
        Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();Body tug=g.tug;
        check(g.tutorial.entering && tug.x<TrainingChannel.BASIN_LEFT,"starts outside the new entrance");
        ReplayInput.down(root,tug.x,tug.y);ReplayInput.move(root,tug.x-90,tug.y);
        check(!g.pointer && !g.joystick.active,"arrival cannot inherit a drive gesture");
        float start=tug.x;g.pause();for(int i=0;i<90;i++)root.step();
        check(g.tug.x==start && g.tutorial.entranceTicks==0,"pause freezes arrival");g.pause();
        int ticks=0;while(g.tutorial.entering && ticks++<480){float x=tug.x;root.step();
            check(g.tug==tug && tug.x>=x && tug.x-x<2,"arrival moves continuously with the original hull");
            check(CalumetMap.clear(tug.x-20,tug.y,tug.x+20,tug.y,12),"entry has clearance for the whole hull");
            check(g.tutorial.driveMarker==0 && g.tutorial.channelGate==1,"arrival earns no tutorial goals");
        }
        check(!g.tutorial.entering && ticks>120 && ticks<480,"bounded arrival hands off inside harbor");
        check(tug.x>TrainingChannel.BASIN_LEFT+80 && tug.speed()>0,"handoff preserves physical momentum");
        ReplayInput.down(root,tug.x,tug.y);check(g.holdingTug(),"player can immediately take the helm");
    }System.out.println("Tutorial entrance, hull clearance, pause and control handoff passed");}
}
