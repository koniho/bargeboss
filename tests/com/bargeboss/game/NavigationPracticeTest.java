package com.bargeboss.game;

/** Real slip contact invalidates the attempt; chart completion needs an actual slider gesture. */
public final class NavigationPracticeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game fixture(int lesson){Game r=new Game();r.startTutorial();Game g=r.activeGame();g.tutorial.lesson=lesson;g.tutorial.setup(g);return g;}
    public static void run(){
        for(float x:new float[]{TrainingChannel.BASIN_LEFT-1,-1900,TrainingChannel.SLIP_LEFT+1})
            for(float y:new float[]{TrainingChannel.SLIP_TOP+1,300,TrainingChannel.SLIP_BOTTOM-1}){
                Game inside=fixture(Tutorial.SLIP);NavigationPractice lesson=inside.tutorial.navigation;
                lesson.slipPhase=2;lesson.reverseTravel=1;inside.tug.x=x;inside.tug.y=y;inside.tug.angle=.6f;
                inside.pointer=true;
                check(!lesson.slip(inside),"holding drive inside the slip cannot complete parking");
                inside.pointer=false;inside.joystick.active=true;
                check(!lesson.slip(inside),"a held neutral joystick still requires lifting the finger");
                inside.joystick.active=false;
                check(lesson.slip(inside),"release anywhere inside completes a clean backing attempt");
            }
        Game outside=fixture(Tutorial.SLIP);NavigationPractice gate=outside.tutorial.navigation;
        gate.slipPhase=2;gate.reverseTravel=1;outside.tug.x=TrainingChannel.BASIN_LEFT;outside.tug.y=300;
        check(!gate.slip(outside),"tug must pass the mouth");
        outside.tug.x=TrainingChannel.BASIN_LEFT-1;gate.reverseTravel=0;
        check(!gate.slip(outside),"entry without actual backing does not count");

        Game g=fixture(Tutorial.SLIP);Body hull=g.tug;
        for(int i=0;i<1500 && g.tutorial.lesson==Tutorial.SLIP;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.MAP && g.tug==hull,"clean public reverse gesture reaches slip without replacing hull");
        g=fixture(Tutorial.SLIP);NavigationPractice n=g.tutorial.navigation;
        n.slipPhase=2;g.tug.x=-1900;g.tug.y=337;g.tug.vy=90;g.tug.vx=0;g.step();
        check(n.retry && n.attempts==1,"significant real side-wall collision fails slip attempt");
        for(int i=0;i<90;i++)g.step();check(n.retry && g.tutorial.lesson==Tutorial.SLIP,"waiting inside cannot reset failed attempt");
        for(int i=0;i<2000 && g.tutorial.lesson==Tutorial.SLIP;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.MAP && n.attempts==1,"pulling completely out permits a fresh clean reverse attempt");
        g=fixture(Tutorial.MAP);n=g.tutorial.navigation;
        g.tug.x=NavigationPractice.OPEN_X;g.tug.y=NavigationPractice.OPEN_Y;n.mapPhase=1;g.step();
        check(n.mapPhase==2 && g.overview.zoom()==OverviewMap.maxZoom(g),"chart lesson starts zoomed in in open water");
        float targetX=OverviewMap.mapX(g,NavigationPractice.TARGET_Y),targetY=OverviewMap.mapY(g,NavigationPractice.TARGET_X);
        check(targetX<0 || targetX>OverviewMap.WIDTH || targetY<0 || targetY>OverviewMap.HEIGHT,"initial zoom hides distant target");
        g.tug.x=NavigationPractice.TARGET_X;g.tug.y=NavigationPractice.TARGET_Y;g.step();
        check(g.tutorial.lesson==Tutorial.MAP && n.mapPhase==2,"finding target without learning slider cannot skip chart exercise");
        g.tug.x=NavigationPractice.OPEN_X;g.tug.y=NavigationPractice.OPEN_Y;g.fitTutorialCamera();
        Layout.Box slider=OverviewMap.sliderBounds(g);g.pointerDown(42,slider.x+slider.w-24,slider.centerY());g.pointerMove(42,slider.x+24,slider.centerY());g.pointerUp(42,slider.x+24,slider.centerY());g.step();
        check(n.mapPhase==3 && g.overview.zoom()==1,"real slider drag reveals chart and enables navigation goal");
        for(int i=0;i<1500 && g.tutorial.lesson==Tutorial.MAP;i++){ReplayInput.tutorial(g);g.step();}
        check(g.tutorial.lesson==Tutorial.CHANNEL,"reaching revealed map target finishes chart lesson before canal and cargo work");
        System.out.println("Slip reverse, real impact retry, chart gesture and target navigation checks passed");
    }
    public static void main(String[] args){run();}
}
