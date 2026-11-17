package com.bargeboss.game;

/** Winding around each actual buoy, not waiting near it or touching a target. */
public final class BuoyOrbitTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Game practice(){Game g=new Game();ReplayInput.startPractice(g);return g.activeGame();}
    private static void point(Game g,int buoy,float radius,double angle){
        g.tug.x=Tutorial.DRIVE_X[buoy]+radius*(float)StrictMath.cos(angle);
        g.tug.y=Tutorial.DRIVE_Y[buoy]+radius*(float)StrictMath.sin(angle);
        g.navigating=true;g.tutorial.update(g);
    }
    private static void circle(Game g,int buoy,int direction){for(int i=0;i<=132;i++)point(g,buoy,90,direction*i*StrictMath.PI/64);}
    public static void run(){
        Game g=practice();
        for(int i=0;i<300;i++){g.tug.x=Tutorial.DRIVE_X[0];g.tug.y=Tutorial.DRIVE_Y[0];g.navigating=true;g.tutorial.update(g);}
        check(g.tutorial.driveMarker==0 && !g.tutorial.orbitArmed,"touching or waiting on buoy does not count as circling");
        for(int x=-140;x<=140;x+=4){g.tug.x=Tutorial.DRIVE_X[0]+x;g.tug.y=Tutorial.DRIVE_Y[0];g.tutorial.update(g);}
        check(g.tutorial.driveMarker==0,"driving across buoy cannot finish the lesson");
        g=practice();circle(g,1,1);check(g.tutorial.driveMarker==0,"second buoy cannot be credited before first");
        g=practice();for(int i=0;i<=64;i++)point(g,0,90,i*StrictMath.PI/64);
        check(g.tutorial.orbitProgress()>.49f && g.tutorial.orbitProgress()<.51f,"half orbit produces half progress");
        for(int i=63;i>=0;i--)point(g,0,90,i*StrictMath.PI/64);
        check(g.tutorial.orbitProgress()<.001f && g.tutorial.driveMarker==0,"retracing the same arc cancels progress");
        for(int i=0;i<6;i++){point(g,0,90,0);point(g,0,90,StrictMath.PI);}
        check(g.tutorial.driveMarker==0 && g.tutorial.orbitProgress()==0,"large jumps cannot forge angular travel");
        g=practice();for(int i=0;i<=64;i++)point(g,0,90,i*StrictMath.PI/64);
        point(g,0,200,StrictMath.PI);point(g,0,90,StrictMath.PI);
        check(g.tutorial.orbitProgress()==0,"leaving the buoy area restarts an incomplete lap");
        point(g,0,20,StrictMath.PI);check(!g.tutorial.orbitArmed,"cutting inside the buoy exclusion area disarms lap");
        for(int direction:new int[]{-1,1}) {
            g=practice();circle(g,0,direction);
            check(g.tutorial.driveMarker==1 && g.tutorial.lesson==Tutorial.MOVE,"full winding advances only the first buoy in either direction");
            check(g.tutorial.buoyClearTicks[0]>0,"first completed buoy starts feedback while next buoy is active");
            circle(g,1,-direction);check(g.tutorial.driveMarker==2 && g.tutorial.lesson==Tutorial.COAST,"opposite winding around second buoy advances into coasting");
            check(g.tutorial.buoyClearTicks[1]>0,"last buoy feedback survives transition to coasting");
            int remaining=g.tutorial.buoyClearTicks[1];TutorialView.buoyFeedback(g,new Parity());
            check(g.tutorial.buoyClearTicks[1]==remaining,"drawing completion feedback does not age it");
            for(int tick=0;tick<Tutorial.BUOY_CLEAR_LIFE;tick++)g.tutorial.update(g);
            check(g.tutorial.buoyClearTicks[0]==0 && g.tutorial.buoyClearTicks[1]==0,"completion feedback expires");
            check(g.tutorial.channelGate==1,"circling never bypasses canal navigation");
        }
        g=practice();for(int i=0;i<32;i++)point(g,0,90,i*StrictMath.PI/64);
        float progress=g.tutorial.orbitProgress(),angle=g.tutorial.orbitAngle;
        Renderer.draw(g,new Parity());Renderer.draw(g,new Parity());
        check(progress==g.tutorial.orbitProgress() && angle==g.tutorial.orbitAngle,"lit buoy rendering does not advance winding progress");
        g.background();g.advance(1);check(progress==g.tutorial.orbitProgress(),"pause freezes lap");
        g.tutorial.setup(g);check(!g.tutorial.orbitArmed && g.tutorial.orbitProgress()==0 && g.tutorial.driveMarker==0,"reset clears lap state");
        for(int buoy=0;buoy<2;buoy++)for(int i=0;i<64;i++) {
            double angleAt=i*StrictMath.PI/32;
            check(TrainingChannel.contains(Tutorial.DRIVE_X[buoy]+(Tutorial.DRIVE_RADIUS+30)*(float)StrictMath.cos(angleAt),
                    Tutorial.DRIVE_Y[buoy]+(Tutorial.DRIVE_RADIUS+30)*(float)StrictMath.sin(angleAt)),"complete tug has water clearance around both suggested orbits");
        }
        System.out.println(checks+" full buoy orbit, direction, shortcut, clearance and pause assertions passed");
    }
    public static void main(String[] args){run();}
}
