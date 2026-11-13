package com.bargeboss.game;

/** Exercise camera motion together with real gestures and the physical bridge gate. */
public final class StageCameraTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game stage(int id) { Game g=new Game();g.challengeId=id;g.reset();return g; }
    public static void run() {
        Game g=stage(0);
        check(g.feeders.length==1 && g.berthEnabled(2) && !g.berthEnabled(0) && !g.berthEnabled(1),"first watch has one feeder and one berth");
        steps(g,180);Body b=g.cargo[0];
        check(g.feederDrops()==1 && b.berth==2 && g.berthLetter(b.berth)=='A',"first feeder announces its sole berth A");
        check(g.layout.camera.zoom==1,"waiting feeder cargo does not trigger docking zoom");
        ReplayInput.down(g,b.x,b.y);float px=g.layout.camera.screenX(g.downX,g.downY),py=g.layout.camera.screenY(g.downX,g.downY);
        steps(g,480);
        check(g.canTie(b) && g.layout.camera.zoom>1.39f,"real assisted arrival settles at berth and smoothly zooms in");
        check(!g.manualSteering && Math.abs(px-g.layout.camera.screenX(g.downX,g.downY))<.05f
                && Math.abs(py-g.layout.camera.screenY(g.downX,g.downY))<.05f,"camera cannot move a resting finger or steer its barge");
        ReplayInput.up(g,g.pointerX,g.pointerY);steps(g,180);
        check(g.canTie(b),"released cargo remains ready through a three-second aiming pause");
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,Game.TIE_X[b.berth],g.dockY(b.berth));RiverCamera frozen=g.layout.camera;
        steps(g,15);check(g.layout.camera==frozen,"lasso aim holds the projection still");
        ReplayInput.up(g,Game.TIE_X[b.berth],g.dockY(b.berth));steps(g,18);
        check(b.lineAttached && g.layout.camera==frozen,"throw uses the same projected bollard until hit");
        steps(g,Mooring.TIGHTEN_TICKS+Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS+180);
        check(g.delivered==1 && g.layout.camera.zoom<1.01f,"camera returns to overview after the crew clears the load");
        g=stage(1);check(g.channelTop()<Physics.TOP-688 && g.feeders.length==2,"bridge run extends beyond a screen and has two feeders");
        for(int side=-1;side<=1;side+=2) for(int angle=0;angle<8;angle++) {
            Body hull=new Body(1,2,300,Bridge.Y+side*100,(float)(angle*Math.PI/4));hull.vy=-side*100;
            for(int i=0;i<120;i++) { Physics.integrate(hull);g.bridge.collide(hull);
                for(int disc=0;disc<3;disc++) check(side*(hull.discY(disc)-Bridge.Y)>=hull.radius+11.9f,"closed bridge blocks the full rotated hull from either direction");
            }
        }
        g.tug.x=300;g.tug.y=Bridge.Y+150;g.step();
        check(g.bridge.phase==Bridge.WAITING,"approaching requests an opening automatically");
        steps(g,359);check(!g.bridge.passable(),"bridge enforces the six-second wait");
        steps(g,61);check(g.bridge.phase==Bridge.OPENING && !g.bridge.passable(),"moving bridge is blocked until clearance exists");
        int tick=g.bridge.ticks;RiverCamera paused=g.layout.camera;g.background();g.advance(.25);
        check(g.bridge.ticks==tick && g.layout.camera==paused,"background freezes bridge and camera");g.pause();steps(g,60);
        check(g.bridge.phase==Bridge.OPEN && g.bridge.passable(),"raised bridge grants passage");
        g.tug.y=Bridge.Y;steps(g,750);check(g.bridge.phase==Bridge.OPEN,"bridge never closes on a vessel underneath");
        g.tug.y=-300;steps(g,1);check(g.bridge.phase==Bridge.CLOSING,"bridge closes after its passage window clears");
        g.tug.y=Bridge.Y;g.step();check(g.bridge.phase==Bridge.OPENING,"a vessel entering during closing reverses the bridge");
        g.tug.y=-400;steps(g,200);
        RiverCamera c=g.layout.camera;
        check(c.worldTop<0 && c.contains(c.screenX(g.tug.x,g.tug.y),c.screenY(g.tug.x,g.tug.y)),"camera follows the tug into the upstream reach");
        for(float x:new float[]{176,230,300,424}) for(float y:new float[]{-450,-400,-100,200}) {
            float sx=c.screenX(x,y),sy=c.screenY(x,y);
            check(Math.abs(c.worldX(sx,sy)-x)<.001f && Math.abs(c.worldY(sx,sy)-y)<.001f,"scrolled camera has an exact input inverse");
        }
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && c==g.layout.camera,"drawing bridge and scrolling scene does not mutate the camera");
        g.reset();check(g.layout.camera.zoom==1 && g.layout.camera.worldTop==148 && g.bridge.phase==Bridge.CLOSED,"restart restores overview and closed bridge");
        System.out.println(checks+" stage, docking zoom, gesture stability and bridge safety assertions passed");
    }
}
