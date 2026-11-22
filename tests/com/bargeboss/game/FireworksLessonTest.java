package com.bargeboss.game;

/** Finale completion requires a new launch, separate rescue tow and aimed water impacts. */
public final class FireworksLessonTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static Game fixture(int lesson){Game root=new Game();root.configureCanvas(1333,600);ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=lesson;g.tutorial.setup(g);return g;}
    public static void run(){
        Game g=fixture(Tutorial.SHOW);Body first=g.cargo[0],second=g.cargo[2],fire=g.cargo[3];
        check(first.ability==BargeAbilities.FIREWORKS && second.ability==BargeAbilities.FIREWORKS && fire.ability==BargeAbilities.FIRE,"two celebration barges and a separate fire barge");
        check(Physics.length(fire.x-FireworksLesson.X,fire.y-FireworksLesson.Y)>210,"fire barge starts outside nozzle range and needs collecting");
        check(fire.x==FireworksLesson.BERTH_X && fire.y==FireworksLesson.BERTH_Y,"rescue craft waits in its separate southern basin");
        check(TrainingChannel.contains(-350,-1050) && TrainingChannel.contains(-50,-1200) && TrainingChannel.contains(100,-1350),"basin and both canal legs are water");
        check(!TrainingChannel.contains(100,-1100),"solid peninsula prevents a direct shortcut to the fire");
        for(float yy=-1479.75f;yy<-800;yy+=11)for(float xx=-519.75f;xx<450;xx+=11)
            check(TrainingChannel.contains(xx,yy)==PolygonWater.contains(TrainingChannel.WATER_BOUNDARY,xx,yy),"water fill and collision shoreline agree");
        steps(g,600);check(g.tutorial.lesson==Tutorial.SHOW && !g.tutorial.fireworks.launched,"waiting cannot finish the fireworks lesson");
        for(int i=0;i<700 && g.tutorial.lesson==Tutorial.SHOW;i++){ReplayInput.tutorial(g);g.step();}
        check(g.abilities.launches>0 && g.tutorial.lesson==Tutorial.FIRE_TOW && g.tutorial.fireworks.heat==FireworksLesson.HEAT,"actual launch causes delayed dock fire, not automatic success");
        steps(g,180);check(g.tutorial.lesson==Tutorial.FIRE_TOW,"fire cannot expire or skip the separate tow");
        for(int i=0;i<3000 && g.tutorial.lesson==Tutorial.FIRE_TOW;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.EXTINGUISH && Tutorial.direct(g,fire),"public rope gesture and towing collect the separate fire barge");
        ReplayInput.down(g,fire.x,fire.y);ReplayInput.up(g,fire.x,fire.y);
        float x=g.abilities.buttonX(g,0),y=g.abilities.buttonY(g);g.pointerDown(88,x,y);
        g.pointerMove(88,g.layout.camera.screenX(fire.x-130,fire.y),g.layout.camera.screenY(fire.x-130,fire.y));
        steps(g,200);check(g.abilities.spraying && g.tutorial.fireworks.heat==FireworksLesson.HEAT,"spraying away from flames earns no credit");
        g.pointerCancel(88);check(!g.abilities.spraying,"cancel stops water emission");
        for(int i=0;i<700 && g.tutorial.lesson!=Tutorial.DONE;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.DONE && g.tutorial.fireworks.done && g.tutorial.fireworks.heat==0,"aimed simulated water hits are required to finish");
        check(g.cargo[0]==first && g.cargo[2]==second && g.cargo[3]==fire,"finale preserves all three original specialty barges");
        steps(g,200);check(g.tutorial.fireworks.steam==0,"completion steam expires even on DONE");
        g=fixture(Tutorial.SHOW);g.tutorial.fireworks.launched(g,g.cargo[0]);steps(g,30);int ticks=g.tutorial.fireworks.showTicks;g.background();steps(g,200);
        check(g.tutorial.fireworks.showTicks==ticks,"background freezes fireworks ignition timer");
        g.mode=Game.PLAY;g.tutorial.setup(g);check(!g.tutorial.fireworks.launched && !g.tutorial.fireworks.done,"reset clears mishap and completion");
        System.out.println("Victory launch, separate fire tow, wrong-aim rejection, water-impact suppression and lifecycle passed");
    }
    public static void main(String[] args){run();}
}
