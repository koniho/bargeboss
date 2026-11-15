package com.bargeboss.game;

/** Exercise camera motion together with real gestures and timed lock passage. */
public final class StageCameraTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game stage(int id) { Game g=new Game();g.challengeId=id;g.reset();return g; }
    public static void run() {
        Game g=stage(0);
        check(g.feeders.length==2 && g.berthEnabled(2) && g.berthEnabled(1) && !g.berthEnabled(0),"first level has two feeder slots and opposite-bank berths");
        steps(g,180);Body b=g.cargo[0];
        check(g.feederDrops()==2 && b.berth==2 && g.berthLetter(b.berth)=='A',"first feeder announces berth A while the second supplies B");
        check(Feeder.DROP_Y-g.dockY(2)>400,"first berth is at the far end opposite the feeder");
        check(Math.abs(g.layout.camera.zoom-Game.WORK_ZOOM)<.01f,"waiting feeder uses the closer working view");
        ReplayInput.down(g,b.x,b.y);float px=g.layout.camera.screenX(g.downX,g.downY),py=g.layout.camera.screenY(g.downX,g.downY);
        for(int i=0;i<1800 && !g.canTie(b);i++) g.step();
        steps(g,180);
        check(g.canTie(b) && g.layout.camera.zoom>1.39f,"real assisted arrival settles at berth and smoothly zooms in");
        for(float[] point:new float[][]{{g.tug.x,g.tug.y},{b.x,b.y},{Game.TIE_X[b.berth],g.dockY(b.berth)}}) {
            float sx=g.layout.camera.screenX(point[0],point[1]),sy=g.layout.camera.screenY(point[0],point[1]);
            check(sx>32 && sx<g.layout.width-32 && sy>64 && sy<g.layout.height-48,
                    "docking zoom keeps both hull centers and the inland tie target clear of HUD edges");
        }
        check(!g.manualSteering && Math.abs(px-g.layout.camera.screenX(g.downX,g.downY))<.05f
                && Math.abs(py-g.layout.camera.screenY(g.downX,g.downY))<.05f,"camera cannot move a resting finger or steer its barge");
        ReplayInput.up(g,g.pointerX,g.pointerY);steps(g,180);
        check(g.canTie(b),"released cargo remains ready through a three-second aiming pause");
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,Game.TIE_X[b.berth],g.dockY(b.berth));RiverCamera frozen=g.layout.camera;
        steps(g,15);check(g.layout.camera==frozen,"lasso aim holds the projection still");
        ReplayInput.up(g,Game.TIE_X[b.berth],g.dockY(b.berth));steps(g,18);
        check(b.lineAttached && g.layout.camera==frozen,"throw uses the same projected bollard until hit");
        for(int i=0;i<Mooring.TIGHTEN_TICKS+Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS+60 && g.delivered==0;i++) g.step();
        steps(g,135);
        check(g.delivered==1 && g.layout.camera.zoom<1.01f && g.cameraOverviewTicks>0,"camera opens the view while leaving a completed delivery");
        steps(g,240);check(g.layout.camera.zoom>1.49f,"camera restores the closer default after the delivery overview");
        g=stage(0);for(Feeder feeder:g.feeders){feeder.phase=Feeder.IDLE;feeder.load=null;}
        b=new Body(1,2,230,g.dockY(2),0);g.cargo[0]=b;g.tug.x=300;g.tug.y=b.y;
        steps(g,180);check(g.layout.camera.zoom>1.3f,"nearby cargo triggers docking view");
        Mooring.attach(b);b.mooringTicks=Mooring.ALIGN_TICKS;b.x=197.5f;b.angle=(float)Math.PI/2;
        b.unloadingTicks=Game.UNLOAD_TICKS-1;
        Body next=new Body(2,1,265,b.y+100,0);g.cargo[1]=next;
        g.step();check(g.delivered==1 && g.cameraOverviewTicks>0,"delivery starts an overview grace period");
        int grace=g.cameraOverviewTicks;g.background();steps(g,60);
        check(g.cameraOverviewTicks==grace,"pause freezes overview grace period");g.pause();steps(g,120);
        check(next.active && g.layout.camera.zoom<1.02f && g.cameraOverviewTicks>0,"nearby next load cannot prevent delivery zoom-out");
        g.reset();check(g.cameraOverviewTicks==0,"restart clears overview grace period");
        g=stage(1);check(g.orderStage() && !g.hasBridge(),"ordered-berth challenge has no unrelated bridge timer");
        // The continuous tutorial still uses the bridge; preserve its all-heading collision regression.
        for(int side=-1;side<=1;side+=2)for(int angle=0;angle<8;angle++){
            Body hull=new Body(1,2,300,Bridge.Y+side*100,(float)(angle*Math.PI/4));hull.vy=-side*100;
            for(int i=0;i<120;i++){
                Physics.integrate(hull);g.bridge.collide(hull);
                for(int disc=0;disc<3;disc++)check(side*(hull.discY(disc)-Bridge.Y)>=hull.radius+11.9f,"closed tutorial bridge blocks the full rotated hull from either direction");
            }
        }
        g=stage(2);check(g.lockStage() && g.channelTop()<Physics.TOP-688 && g.feeders.length==2,"lock reach extends beyond a screen and has two feeders");
        steps(g,180);check(g.layout.camera.zoom>1.49f,"lock feeder pickup uses the close working view");
        g.tug.x=300;g.tug.y=Lock.ENTRY_Y+130;g.step();
        check(g.lock.phase==Lock.ENTRY,"loaded lock approach starts entry window");
        steps(g,Lock.GATE_TICKS);check(g.lock.entryPassable() && !g.lock.exitPassable(),"only the lower gate opens for entry");
        check(g.layout.camera.zoom<Game.WORK_ZOOM,"lock transit retains room ahead");
        int tick=g.lock.ticks;RiverCamera paused=g.layout.camera;g.background();g.advance(.25);
        check(g.lock.ticks==tick && g.layout.camera==paused,"background freezes lock window and camera");g.pause();
        g.lock.phase=Lock.EXIT;g.lock.ticks=0;g.tug.y=-300;steps(g,180);
        check(g.layout.camera.zoom>=Game.TRANSIT_ZOOM-.01f,"upstream passage keeps a readable vessel scale");
        RiverCamera c=g.layout.camera;
        check(c.worldTop<0 && c.contains(c.screenX(g.tug.x,g.tug.y),c.screenY(g.tug.x,g.tug.y)),"camera follows the tug into the upstream reach");
        for(float x:new float[]{176,230,300,424}) for(float y:new float[]{-450,-400,-100,200}) {
            float sx=c.screenX(x,y),sy=c.screenY(x,y);
            check(Math.abs(c.worldX(sx,sy)-x)<.001f && Math.abs(c.worldY(sx,sy)-y)<.001f,"scrolled camera has an exact input inverse");
        }
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && c==g.layout.camera,"drawing lock and scrolling scene does not mutate the camera");
        g.reset();check(g.layout.camera.zoom==1 && g.layout.camera.worldTop==148 && g.lock.phase==Lock.READY,"restart restores overview and ready lock");
        System.out.println(checks+" stage, docking zoom, gesture stability, lock lifecycle and bridge collision assertions passed");
    }
}
