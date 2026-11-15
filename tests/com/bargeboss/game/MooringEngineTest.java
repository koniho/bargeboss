package com.bargeboss.game;

public final class MooringEngineTest {
    private static int checks;
    private static void check(boolean ok,String why) { checks++;if(!ok) throw new AssertionError(why); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    public static void run() {
        for(int berth=1;berth<3;berth++) for(float heading:new float[]{0,.6f,2.8f,-.6f,-2.8f}) {
            Game g=new Game();g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
            int deliveredBefore=0;g.delivered=deliveredBefore;
            g.tug.x=300;g.tug.y=600;
            Body b=new Body(1,berth,g.dockX(berth),g.dockY(berth)+10,heading);g.cargo[0]=b;
            Mooring.attach(b);float startX=b.x,startY=b.y;
            steps(g,Mooring.TIGHTEN_TICKS);
            check(b.x==startX && b.y==startY && b.angle==heading && b.unloadingTicks==0,"catch preserves the initial hull pose before the first winch step");
            check(b.lineSnapTicks==18,"snap starts immediately on attachment");
            steps(g,30);check(Mooring.aligning(b) && b.unloadingTicks==0,"crew waits while winch draws and turns hull");
            float x=b.x,y=b.y,angle=b.angle;int clock=b.mooringTicks;
            g.background();steps(g,100);g.advance(.25);
            check(b.x==x && b.y==y && b.angle==angle && b.mooringTicks==clock,"pause freezes automatic docking");g.pause();
            for(int i=30;i<Mooring.ALIGN_TICKS;i++) {
                x=b.x;y=b.y;g.step();
                check(b.lineAttached && g.inDock(b) && Physics.length(b.x-x,b.y-y)<3,"winching is continuous and stays inside the berth");
            }
            check(Math.abs(StrictMath.cos(b.angle))<.0001 && Math.abs(Physics.angle(b.angle-heading))<=Math.PI/2+.0001,"parallel berth heading uses the shortest turn");
            check(b.y==g.dockY(berth) && Math.abs(b.x-(berth==1?402.5f:197.5f))<.001,"hull ends alongside the correct bank with fender clearance");
            check(b.unloadingTicks==1 && g.delivered==deliveredBefore,"three-second unload starts after alignment");
            steps(g,Game.UNLOAD_TICKS-1);check(g.delivered==deliveredBefore+1 && b.active && b.deliveryRecorded && b.lineAttached,"aligned cargo scores once and remains as a secured empty hull");
        }
        Game g=new Game();g.challengeId=1;g.reset();steps(g,60);check(g.tug.engineLoad<.01f,"idle engine carries no propulsion load");
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX,g.downY-100);steps(g,45);
        check(g.tug.engineLoad>.5f,"acceleration increases exhaust load");
        ReplayInput.up(g,g.pointerX,g.pointerY);steps(g,90);
        check(g.tug.engineLoad<.03f,"coasting exhaust returns toward idle without depending on boat speed");
        ReplayInput.controlFixture(g);ReplayInput.down(g,g.cargo[0].x,g.cargo[0].y);
        float max=0;for(int i=0;i<500;i++){g.step();max=Math.max(max,g.tug.engineLoad);}
        check(max>.85f,"loaded push produces a dense high-load plume");
        float load=g.tug.engineLoad;int frame=Parity.frame(g);g.background();g.advance(.25);
        check(g.tug.engineLoad==load,"pause freezes engine effort");
        g.pause();frame=Parity.frame(g);check(Parity.frame(g)==frame && g.tug.engineLoad==load,"smoke rendering cannot change engine state");
        g=new Game();g.reset();VesselSmoke idle=new VesselSmoke(),loaded=new VesselSmoke();
        Body idleBoat=new Body(0,-1,300,500,0),loadedBoat=new Body(0,-1,300,500,0);loadedBoat.engineLoad=1;
        for(int i=0;i<120;i++){idle.emit(g,idleBoat);loaded.emit(g,loadedBoat);}
        check(loaded.count()>idle.count()*2,"higher engine loading increases emitted smoke quantity");
        System.out.println(checks+" automatic docking, unload ordering and engine smoke assertions passed");
    }
}
