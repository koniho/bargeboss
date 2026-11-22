package com.bargeboss.game;

/** Real pointer throws, powered tether motion, and bollard-only release. */
public final class SlipMooringTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
    static Game fixture(){
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.MAP;g.tutorial.setup(g);g.tug.x=-1910;g.tug.y=300;g.tug.angle=0;
        g.tug.vx=g.tug.vy=g.tug.spin=0;g.fitTutorialCamera();return g;
    }
    public static void run(){
        Game g=fixture();float bx=g.bollardX(g.tug),by=g.bollardY(g.tug);
        check(g.canTie(g.tug),"tug in slip can arm a shore throw");
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,bx,by);steps(g,20);
        check(g.ropeDragging && g.ropePhase==0 && !g.tug.lineAttached,"held aim neither drives nor shoots");
        g.pointerCancel(0);check(g.ropePhase==0,"cancel cannot throw");
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,bx+100,by);ReplayInput.up(g,bx+100,by);
        steps(g,Game.ROPE_FLIGHT_TICKS);check(g.ropePhase==2 && !g.tug.lineAttached,"miss retracts");steps(g,Game.ROPE_RETURN_TICKS);
        ReplayInput.lasso(g,g.tug);steps(g,Game.ROPE_FLIGHT_TICKS);
        check(g.tug.lineAttached && g.tug.lineSnapTicks>0,"bollard catch tightens immediately");
        float startX=g.tug.x,startY=g.tug.y,score=g.score;
        ReplayInput.throttle(g,90,0);steps(g,150);
        check(g.navigating && g.tug.engineLoad>.1f,"tied tug retains powered throttle");
        check(Physics.length(g.tug.x-startX,g.tug.y-startY)>5,"tether permits actual movement");
        float[] fit=new float[2];RopeAttachment.mooring(g.tug,bx,by,fit);
        check(Physics.length(fit[0]-SlipMooring.X,fit[1]-SlipMooring.Y)<=g.slipMooring.length+1,"powered tug cannot stretch tether indefinitely");
        check(g.score==score && g.tug.unloadingTicks==0 && !g.tug.deliveryRecorded,"tug mooring never unloads or scores");
        g.cancelInputs();steps(g,30);
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
        check(g.tug.lineAttached,"tapping tug does not cast off");
        ReplayInput.down(g,bx,by);g.pointerCancel(0);check(g.tug.lineAttached,"cancelled bollard tap retains line");
        ReplayInput.down(g,bx,by);ReplayInput.move(g,bx+70,by);ReplayInput.up(g,bx+70,by);
        check(g.tug.lineAttached,"dragging off bollard is not release");
        int before=Parity.frame(g);check(before==Parity.frame(g),"tether drawing is deterministic");
        ReplayInput.down(g,bx,by);check(g.tug.lineAttached,"bollard touchdown waits for release");ReplayInput.up(g,bx,by);
        check(!g.tug.lineAttached,"tap bollard casts off");
        g=fixture();g.slipMooring.attach(g);g.tug.vx=20;g.tug.wakeTrailTicks=7;
        int emitted=g.water.emitted;g.water.floatBoats(g);check(g.water.emitted>emitted,"moving tethered tug still emits a wake trail");
        g.pause();float pausedX=g.tug.x;steps(g,60);check(g.tug.lineAttached && g.tug.x==pausedX,"pause preserves the tether without simulating");g.pause();
        g.tutorial.setup(g);check(!g.tug.lineAttached,"reset lesson replaces tied hull with untied hull");
        g=fixture();g.tug.x=-1750;g.fitTutorialCamera();startX=g.tug.x;startY=g.tug.y;
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.tug.x+100,g.tug.y);steps(g,60);
        check(!g.navigating && !g.pushing && Physics.length(g.tug.x-startX,g.tug.y-startY)<1,"tug drag outside slip never drives");
        g.cancelInputs();ReplayInput.controlFixture(g);Body b=g.cargo[0];ReplayInput.down(g,b.x,b.y);steps(g,12);
        check(g.pushing && g.heldCargo()==b,"cargo hold still starts assisted push");ReplayInput.move(g,g.downX+90,g.downY);
        check(g.manualSteering && !g.towAiming,"cargo drag still steers push assist");
        for(int heading=0;heading<8;heading++){
            g=fixture();g.tug.angle=heading*(float)Math.PI/4;g.slipMooring.attach(g);
            ReplayInput.throttle(g,90*(float)StrictMath.cos(g.tug.angle),90*(float)StrictMath.sin(g.tug.angle));
            for(int tick=0;tick<180;tick++){
                g.step();Body probe=new Body(0,-1,g.tug.x,g.tug.y,g.tug.angle);CalumetMap.collide(probe,null);
                check(Physics.length(probe.x-g.tug.x,probe.y-g.tug.y)<.25f,"tether cannot pull a turning hull into seawall");
            }
        }
        System.out.println("Slip lasso, powered tether, bollard release and throttle-only driving checks passed");
    }
    public static void main(String[] args){run();}
}
