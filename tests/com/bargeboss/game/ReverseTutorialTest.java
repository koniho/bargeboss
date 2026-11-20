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
            for(int tick=0;tick<180;tick++)g.step();check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.orbitProgress()==0,"waiting cannot complete reverse");
            boolean reversing=false;
            for(int tick=0;tick<5000 && g.tutorial.lesson==Tutorial.REVERSE;tick++){
                float dx=g.tutorial.driveTargetX()-tug.x,dy=g.tutorial.driveTargetY()-tug.y;
                if(side==GameSettings.OFF)ReplayInput.tutorial(g);
                else {
                    if(!g.joystick.active)stick(g,-(float)StrictMath.cos(tug.angle),-(float)StrictMath.sin(tug.angle));
                    stick(g,dx,dy);
                }
                g.step();reversing|=g.reversing;
                check(g.tug==tug && g.inWater(tug.x,tug.y),"reverse keeps the original hull in water");
            }
            check(g.tutorial.lesson==Tutorial.SLIP && reversing,"reverse figure eight completes with control side "+side);
        }
        Game g=fixture(GameSettings.OFF);g.tug.vx=-50;for(int i=0;i<120;i++)g.step();
        check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.orbitProgress()==0,"unpowered momentum earns no reverse progress");
        System.out.println(checks+" reverse tutorial thrust, momentum rejection, target and public-control assertions passed");
    }
    public static void main(String[]args){run();}
}
