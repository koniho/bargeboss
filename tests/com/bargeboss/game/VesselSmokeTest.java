package com.bargeboss.game;

public final class VesselSmokeTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        Game g=new Game();g.reset();
        for(int heading=0;heading<32;heading++) {
            Body b=new Body(0,-1,300,500,(float)(heading*Math.PI/16));
            b.engineLoad=1;b.floatingInitialized=true;b.heave=1.3f;b.smokeCredit=1;
            VesselSmoke s=new VesselSmoke();s.emit(g,b);
            for(float zoom:new float[]{.7f,1,3}) {
                RiverCamera c=new RiverCamera(51,-94,zoom);
                float size=2*(b.half+b.radius)/TugArt.HULL_FRACTION;
                check(Math.abs(c.screenX(s.x[0],s.y[0])-(c.screenX(b.x,b.y)+SpriteAtlas.stackOffset(b.angle,true,false)*size*zoom))<.001,"registered stack X survives projection");
                check(Math.abs(c.screenY(s.x[0],s.y[0])-(c.screenY(b.x,b.y)+(SpriteAtlas.stackOffset(b.angle,true,true)*size-b.heave)*zoom))<.001,"registered stack Y survives projection");
            }
        }
        Body b=new Body(0,-1,300,500,0);b.engineLoad=1;b.vx=50;b.smokeCredit=1;
        VesselSmoke s=new VesselSmoke();s.emit(g,b);float x=s.x[0],y=s.y[0];
        b.x+=500;b.y-=500;b.angle=2;b.engineLoad=0;b.vx=-100;
        check(s.x[0]==x && s.y[0]==y && s.power[0]==1,"teleport, turn and throttle cannot move or recolor released smoke");
        s.drift();check(Math.abs(s.x[0]-x)<1 && Math.abs(s.y[0]-y)<1 && s.rise[0]>0,"smoke moves only by independent wind and buoyancy");
        for(int i=1;i<420;i++)s.drift();check(s.count()==1,"exhaust remains seven seconds after source disappears");
        for(int i=420;i<VesselSmoke.LIFE;i++)s.drift();check(s.count()==0,"exhaust expires after eight seconds");
        for(int tick=0;tick<1800;tick++){b.smokeCredit=4;s.emit(g,b);s.drift();}
        check(s.count()<=VesselSmoke.CAPACITY,"pool is bounded under extreme emission");
        g=new Game();for(int i=0;i<180;i++)g.advance(1.0/60);
        check(g.titleSmoke.count()>0 && g.smoke.count()==0,"title uses independent fixed-step smoke");
        g.reset();for(int i=0;i<120;i++)g.step();s=g.smoke;
        int count=s.count(),emitted=s.emitted,age=s.age[0];x=s.x[0];
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && s.x[0]==x && s.age[0]==age && s.emitted==emitted,"render does not mutate smoke");
        g.background();g.advance(.25);check(s.age[0]==age && s.count()==count,"pause freezes smoke");
        g.reset();check(g.smoke.count()==0,"restart clears previous trails");
        Game a=new Game(),c=new Game();a.reset();c.reset();
        for(int i=0;i<120;i++)a.advance(1.0/30);
        for(int i=0;i<576;i++)c.advance(1.0/144);
        check(a.smoke.emitted==c.smoke.emitted,"emission is independent of host frame rate");
        for(int i=0;i<VesselSmoke.CAPACITY;i++)check(a.smoke.age[i]==c.smoke.age[i] && a.smoke.x[i]==c.smoke.x[i] && a.smoke.y[i]==c.smoke.y[i],"smoke transport is deterministic across host frame rates");
        System.out.println(checks+" world-space vessel exhaust assertions passed");
    }
}
