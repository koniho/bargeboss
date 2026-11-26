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
        ReplayInput.hoseAim(g,88,fire.x-130,fire.y);
        steps(g,200);check(g.abilities.spraying && g.tutorial.fireworks.heat==FireworksLesson.HEAT,"spraying away from flames earns no credit");
        g.pointerCancel(88);check(!g.abilities.spraying,"cancel stops water emission");
        boolean towed=false;
        for(int i=0;i<5000 && g.tutorial.lesson<Tutorial.MOOR;i++){ReplayInput.tutorial(g);g.step();
            if(g.tutorial.lesson==Tutorial.EXTINGUISH && g.tutorial.fireworks.heat>0)towed|=g.tutorial.fireworks.targetGroup()>0 && Physics.length(fire.x-FireworksLesson.TOW_X,fire.y-FireworksLesson.TOW_Y)>150;}
        check(towed,"later quay groups require towing the fire barge away from the marked water");
        check(g.tutorial.lesson==Tutorial.MOOR && g.tutorial.fireworks.done && g.tutorial.fireworks.heat==0,"aimed simulated water hits are required to finish the fire lesson");
        check(g.cargo[0]==first && g.cargo[2]==second && g.cargo[3]==fire,"finale preserves all three original specialty barges");
        steps(g,200);check(g.tutorial.fireworks.steam==0,"completion steam expires during the towing challenge");
        g=fixture(Tutorial.EXTINGUISH);FireworksLesson fires=g.tutorial.fireworks;
        check(FireworksLesson.PILES==6 && FireworksLesson.GROUPS==3,"six fires in three quay groups");
        for(int pile=0;pile<FireworksLesson.PILES;pile++){
            check(!TrainingChannel.contains(FireworksLesson.x(pile)+15,FireworksLesson.y(pile)),"each timber pile sits on the dry quay");
            int k=FireworksLesson.group(pile);
            check(TrainingChannel.contains(FireworksLesson.STATION_X[k],FireworksLesson.STATION_Y[k]),"each barge station is open water");
            check(Physics.length(FireworksLesson.x(pile)-FireworksLesson.STATION_X[k],FireworksLesson.y(pile)-FireworksLesson.STATION_Y[k])<=FireworksLesson.REACH,"each fire is within nozzle reach of its group's station");
            for(int other=0;other<k;other++)check(Physics.length(FireworksLesson.x(pile)-FireworksLesson.STATION_X[other],FireworksLesson.y(pile)-FireworksLesson.STATION_Y[other])>210,"later fires are beyond reach of earlier stations, so the barge must move");
            for(int i=0;i<pile;i++)check(Physics.length(FireworksLesson.x(pile)-FireworksLesson.x(i),FireworksLesson.y(pile)-FireworksLesson.y(i))>100,"fires are spread apart");
        }
        for(int i=0;i<FireworksLesson.PILE_HEAT;i++)check(fires.waterHit(FireworksLesson.x(0),FireworksLesson.y(0)),"first pile accepts independent water impacts");
        check(!fires.done && fires.remaining()==5 && fires.heat(1)==FireworksLesson.PILE_HEAT,"one extinguished pile cannot finish the lesson or cool its neighbor");
        check(FireLight.energy(g,0)==0 && FireLight.energy(g,1)>0,"each fire light follows its own heat");
        check(!fires.waterHit(FireworksLesson.x(0),FireworksLesson.y(0)),"spent pile cannot absorb more credit");
        for(int pile=1;pile<FireworksLesson.PILES;pile++){
            check(!fires.done,"every pile is required for completion");
            for(int i=0;i<FireworksLesson.PILE_HEAT;i++)check(fires.waterHit(FireworksLesson.x(pile)+FireworksLesson.FOOT_OFFSET,FireworksLesson.y(pile)),"each pile accepts water on its footprint");
            check(fires.remaining()==FireworksLesson.PILES-1-pile,"cooling counts down one fire at a time");
        }
        check(fires.done && fires.heat==0 && fires.remaining()==0,"all six piles out completes the phase");
        g=fixture(Tutorial.SHOW);g.tutorial.fireworks.launched(g,g.cargo[0]);steps(g,30);int ticks=g.tutorial.fireworks.showTicks;g.background();steps(g,200);
        check(g.tutorial.fireworks.showTicks==ticks,"background freezes fireworks ignition timer");
        g.mode=Game.PLAY;g.tutorial.setup(g);check(!g.tutorial.fireworks.launched && !g.tutorial.fireworks.done,"reset clears mishap and completion");
        System.out.println("Victory launch, separate fire tow, six-fire towed response, wrong-aim rejection, water-impact suppression and lifecycle passed");
    }
    public static void main(String[] args){run();}
}
