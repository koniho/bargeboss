package com.bargeboss.game;

/** Public pointer gestures choose thrust direction once; steering and throttle stay live. */
public final class DriveDirectionTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Game fixture(int side){Game root=new Game();root.startTutorial();Game g=root.activeGame();g.settings.setJoystickSide(side);return g;}
    private static void down(Game g,int side){if(side==GameSettings.OFF)ReplayInput.down(g,g.tug.x,g.tug.y);else g.pointerDown(21,Joystick.centerX(g),Joystick.centerY(g));}
    private static void move(Game g,int side,float angle,boolean neutral){
        if(side==GameSettings.OFF){ReplayInput.move(g,g.downX+(neutral?0:85*(float)StrictMath.cos(angle)),g.downY+(neutral?0:85*(float)StrictMath.sin(angle)));return;}
        float x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle),sx=RiverCamera.Q*(x+y),sy=RiverCamera.Q*RiverCamera.DEPTH*(y-x),length=Physics.length(sx,sy);
        g.pointerMove(21,Joystick.centerX(g)+(neutral?0:42*sx/length),Joystick.centerY(g)+(neutral?0:42*sy/length));
    }
    private static void step(Game g,int n){for(int i=0;i<n;i++)g.step();}
    private static void release(Game g,int side){if(side==GameSettings.OFF)ReplayInput.up(g,g.pointerX,g.pointerY);else g.pointerUp(21,Joystick.centerX(g),Joystick.centerY(g));}
    public static void run(){
        // Thumb placement and a small initial movement must not commit the
        // opposite gear before the intended drag, even after several ticks.
        for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT})for(int pose=0;pose<16;pose++)for(int direction:new int[]{-1,1}){
            Game g=fixture(side);g.tug.angle=pose*(float)Math.PI/8;
            float angle=g.tug.angle+(direction<0?(float)Math.PI:0),wx=(float)StrictMath.cos(angle),wy=(float)StrictMath.sin(angle);
            float sx=RiverCamera.Q*(wx+wy),sy=RiverCamera.Q*RiverCamera.DEPTH*(wy-wx),len=Physics.length(sx,sy);sx/=len;sy/=len;
            float x=Joystick.centerX(g)-sx*35,y=Joystick.centerY(g)-sy*35;
            g.pointerDown(21,x,y);step(g,4);
            check(g.driveDirection()==0 && !g.navigating && g.joystick.throttle()==0,"off-center touchdown cannot select gear or thrust");
            g.pointerMove(21,x-sx*9,y-sy*9);step(g,4);
            check(g.driveDirection()==0 && !g.navigating,"initial opposite jitter stays neutral");
            g.pointerMove(21,x+sx*13,y+sy*13);step(g,2);
            check(g.driveDirection()==0 && !g.navigating,"wait for enough finger travel before choosing direction");
            g.pointerMove(21,x+sx*22,y+sy*22);step(g,1);
            check(g.driveDirection()==direction && g.reversing==(direction<0) && g.navigating,"intended drag chooses gear despite landing on opposite side of pad");
            check(Math.abs(g.tug.spin)<.01f,"straight intended drag does not trigger a turnaround");
            g.pointerMove(21,x,y);step(g,1);
            check(!g.navigating && g.driveDirection()==direction,"return to actual touchdown coasts without switching gear");
            release(g,side);
        }

        for(int side:new int[]{GameSettings.OFF,GameSettings.LEFT,GameSettings.RIGHT})for(int pose=0;pose<8;pose++)for(int direction:new int[]{-1,1}){
            Game g=fixture(side);g.tug.angle=pose*(float)Math.PI/4;float angle=g.tug.angle+(direction<0?(float)Math.PI:0);
            down(g,side);move(g,side,angle,true);step(g,1);check(g.driveDirection()==0,"neutral first press does not choose direction");
            move(g,side,angle,false);check(g.driveDirection()==direction,"first meaningful pointer input chooses direction before next tick");
            move(g,side,angle+(float)Math.PI,false);check(g.driveDirection()==direction,"opposite movement between ticks cannot replace first intent");
            step(g,13);check(g.reversing==(direction<0) && g.navigating,"held gesture steers in its chosen direction");
            check(Math.abs(g.tug.spin)>.01f,"locked thrust still permits turning");
            move(g,side,angle,true);step(g,3);check(!g.navigating && g.driveDirection()==direction,"return to neutral cuts thrust without unlatching direction");
            g.tug.angle+=.8f;move(g,side,g.tug.angle+(direction>0?(float)Math.PI:0),false);step(g,1);
            check(g.driveDirection()==direction && g.reversing==(direction<0),"new heading and opposite thrust region cannot flip a held gesture");
            float speed=g.tug.speed();release(g,side);check(g.driveDirection()==0 && !g.navigating && g.tug.speed()==speed,"release clears chosen direction and preserves momentum");
            float next=g.tug.angle+(direction>0?(float)Math.PI:0);down(g,side);move(g,side,next,false);step(g,13);
            check(g.driveDirection()==-direction && g.reversing==(direction>0),"next gesture can deliberately choose opposite thrust");
            g.cancelInputs();check(g.driveDirection()==0,"explicit cancellation clears latch");
        }
        // Isolate engine translation from retained momentum: crossing the selection
        // boundary may turn the bow but cannot command the opposite keel direction.
        for(int side:new int[]{GameSettings.OFF,GameSettings.LEFT,GameSettings.RIGHT})for(int pose=0;pose<8;pose++)for(int direction:new int[]{-1,1}){
            Game g=fixture(side);float heading=pose*(float)Math.PI/4;g.tug.angle=heading;
            down(g,side);move(g,side,heading+(direction<0?(float)Math.PI:0),false);step(g,13);
            check(g.driveDirection()==direction,"physical thrust fixture already latched");
            g.tug.vx=g.tug.vy=g.tug.spin=0;g.tug.angle=heading;
            move(g,side,heading+(direction>0?(float)Math.PI:0),false);step(g,1);
            float along=g.tug.vx*(float)StrictMath.cos(g.tug.angle)+g.tug.vy*(float)StrictMath.sin(g.tug.angle);
            check(along*direction>.1f,"opposite drag region cannot reverse physical commanded thrust from rest");
            check(Math.abs(g.tug.spin)>.01f,"keel-aligned thrust still turns toward the drag heading");
        }
        for(int side:new int[]{GameSettings.OFF,GameSettings.LEFT,GameSettings.RIGHT})for(int action=0;action<5;action++){
            Game g=fixture(side);down(g,side);move(g,side,g.tug.angle,false);check(g.driveDirection()==1,"lifecycle fixture chose forward");
            switch(action){case 0:g.pause();break;case 1:g.background();break;case 2:g.configureCanvas(1000,600);break;case 3:g.openSettings();break;default:g.reset();}
            check(g.driveDirection()==0 && !g.pointer,"pause/background/resize/settings/reset clear gesture mode");
        }
        System.out.println(checks+" latched thrust, neutral, steering, release and lifecycle assertions passed");
    }
    public static void main(String[]args){run();}
}
