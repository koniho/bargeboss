package com.bargeboss.game;
/** The highlighted throttle can complete every departure requirement through real input. */
public final class HelmPracticeTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void move(Game g,float dx,float dy){
        float sx=RiverCamera.Q*(dx+dy),sy=RiverCamera.Q*RiverCamera.DEPTH*(dy-dx),d=Math.max(.001f,Physics.length(sx,sy));
        g.pointerMove(18,Joystick.centerX(g)+50*sx/d,Joystick.centerY(g)+50*sy/d);
    }
    public static void run(){
        for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT}){
            Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();g.settings.setJoystickSide(side);int seen=0;
            for(int tick=0;tick<2000 && g.tutorial.lesson==Tutorial.MOVE;tick++){
                HelmPractice h=g.tutorial.helm;seen|=1<<h.phase;
                if(!g.joystick.active){
                    g.pointerDown(18,Joystick.centerX(g),Joystick.centerY(g));float sign=h.phase==HelmPractice.REVERSE?-1:1;
                    move(g,sign*(float)StrictMath.cos(g.tug.angle),sign*(float)StrictMath.sin(g.tug.angle));
                }
                move(g,h.targetX-g.tug.x,h.targetY-g.tug.y);g.step();
            }
            check(seen==31 && g.tutorial.lesson==Tutorial.FIGURE_EIGHT,"joystick on either side teaches all four helm actions then rendezvous");
        }
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();g.tug.vx=80;
        for(int i=0;i<120;i++)g.step();check(g.tutorial.helm.phase==0,"coasting without throttle cannot earn powered departure");
        System.out.println("Left/right throttle helm phases and unpowered-motion rejection passed");
    }
}
