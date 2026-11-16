package com.bargeboss.game;

/** Reverse practice requires real astern thrust/displacement with either public control scheme. */
public final class ReverseTutorialTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Game fixture(int side){Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();g.settings.setJoystickSide(side);g.tutorial.lesson=Tutorial.REVERSE;g.tutorial.setup(g);return g;}
    private static void stick(Game g,float dx,float dy){RiverCamera c=g.layout.camera;float sx=c.screenX(g.tug.x+dx,g.tug.y+dy)-c.screenX(g.tug.x,g.tug.y),sy=c.screenY(g.tug.x+dx,g.tug.y+dy)-c.screenY(g.tug.x,g.tug.y),len=Math.max(1,Physics.length(sx,sy));
        if(!g.joystick.active)g.pointerDown(17,Joystick.centerX(g),Joystick.centerY(g));g.pointerMove(17,Joystick.centerX(g)+Joystick.TRAVEL*sx/len,Joystick.centerY(g)+Joystick.TRAVEL*sy/len);}
    public static void run(){
        for(int side:new int[]{GameSettings.OFF,GameSettings.LEFT,GameSettings.RIGHT}){
            Game g=fixture(side);Body tug=g.tug;check(g.tutorial.reverseArmed,"Clear initial basin latches a reachable reverse target");float x=tug.x,y=tug.y;
            check(Math.abs(Physics.length(g.tutorial.reverseTargetX-x,g.tutorial.reverseTargetY-y)-Tutorial.REVERSE_DISTANCE)<.01f,"Normal reverse target is sixty world units astern");
            for(int tick=0;tick<180;tick++)g.step();check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.reverseTravel==0,"Waiting and initial coast momentum cannot complete reverse");
            boolean reversing=false;for(int tick=0;tick<600 && g.tutorial.lesson==Tutorial.REVERSE;tick++){
                if(side==GameSettings.OFF)ReplayInput.tutorial(g);else stick(g,g.tutorial.reverseTargetX-tug.x,g.tutorial.reverseTargetY-tug.y);
                g.step();reversing|=g.reversing;check(g.tug==tug && g.inWater(tug.x,tug.y),"Reverse training preserves the same physical tug in water");
            }
            check(g.tutorial.lesson==Tutorial.CHANNEL && reversing && g.tutorial.reverseTravel>=Tutorial.REVERSE_REQUIRED,"Public tug drag/left/right joystick completes genuine backward travel: side="+side);
            check(Physics.length(tug.x-g.tutorial.reverseTargetX,tug.y-g.tutorial.reverseTargetY)<=Tutorial.REVERSE_RADIUS+1,"Actual tug reaches its soft reverse target");
        }
        Game g=fixture(GameSettings.OFF);Tutorial t=g.tutorial;
        // Negative fixture: moving astern with no commanded thrust must not earn progress.
        g.tug.vx=-50;g.tug.vy=0;for(int i=0;i<120;i++)g.step();check(t.lesson==Tutorial.REVERSE && t.reverseTravel==0,"Backward momentum by itself earns no reverse progress");
        g=fixture(GameSettings.OFF);t=g.tutorial;float tx=t.reverseTargetX,ty=t.reverseTargetY;
        g.tug.angle=(float)StrictMath.atan2(ty-g.tug.y,tx-g.tug.x);
        for(int tick=0;tick<200;tick++){ReplayInput.tutorial(g);g.step();if(Physics.length(g.tug.x-tx,g.tug.y-ty)<15)break;}
        check(t.lesson==Tutorial.REVERSE && t.reverseTravel==0,"Turning around and driving bow-first to the ring cannot skip reverse instruction");
        System.out.println(checks+" reverse tutorial thrust, momentum rejection, target and public-control assertions passed");
    }
    public static void main(String[]args){run();}
}
