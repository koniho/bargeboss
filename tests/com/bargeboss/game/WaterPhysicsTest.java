package com.bargeboss.game;

public final class WaterPhysicsTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static Game isolated() { Game g=new Game();g.reset();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}g.tug.x=300;g.tug.y=450;return g; }
    public static void run() {
        Body along=new Body(1,0,300,450,0),sideways=new Body(2,0,300,450,(float)Math.PI/2),reverse=new Body(3,0,300,450,0);
        along.vx=sideways.vx=60;reverse.vx=-60;
        for(int i=0;i<120;i++) { Physics.integrate(along);Physics.integrate(sideways);Physics.integrate(reverse); }
        check(along.vx>30 && along.vx<40 && along.x-300>85,"unpowered barge keeps meaningful forward momentum for two physics seconds");
        check(along.vx>sideways.vx*2 && along.x-300>(sideways.x-300)*1.5f,"broadside drag depends on hull orientation relative to motion");
        check(Math.abs(along.vx+reverse.vx)<.1f,"reverse keel motion retains symmetric water resistance");
        Body barge=new Body(1,0,300,450,0),tug=new Body(0,-1,300,450,0);barge.spin=tug.spin=1;
        for(int i=0;i<120;i++) { Physics.integrate(barge);Physics.integrate(tug); }
        check(barge.spin>tug.spin*3,"barge yaw carries inertia longer than the small tug");
        Game dock=isolated();Body load=new Body(1,2,230,700,0);load.vx=15;dock.cargo[0]=load;
        for(int i=0;i<30;i++) dock.step();
        check(load.speed()>6 && load.speed()<10,"an unmoored barge does not snap to a stop just for entering a berth");
        Game g=isolated();g.tug.vx=40;int emissions=0;
        for(int i=0;i<500;i++) {
            float before=g.tug.heaveVelocity;int count=g.water.emitted;g.visualTicks++;g.water.step(g);
            if(g.water.emitted>count) {
                emissions++;check(before<0 && g.tug.heaveVelocity>=0,"underway wake starts at the bottom of the fall");
                check(g.tug.wakeKickTicks==18,"trough starts one bounded foam burst");
            }
        }
        check(emissions>=3 && emissions<=6,"wake generation follows wave cycles instead of emitting every frame");
        Game still=isolated();for(int i=0;i<500;i++){still.visualTicks++;still.water.step(still);}
        check(still.water.emitted==0,"stationary floating hulls bob without creating underway wakes");
        int latest=-1;for(int i=0;i<WaterSurface.CAPACITY;i++) if(g.water.age[i]<WaterSurface.LIFE && (latest<0 || g.water.age[i]<g.water.age[latest]))latest=i;
        float x=g.water.x[latest]+g.water.age[latest]*.55f,y=g.water.y[latest];
        check(g.water.energy(x,y,null)>g.water.energy(x+500,y+500,null),"wake energy spreads locally rather than changing the whole river");
        WaterSurface quiet=new WaterSurface();float roughMax=0,quietMax=0;
        for(int i=0;i<250;i++) { roughMax=Math.max(roughMax,Math.abs(g.water.height(x,y,i,null)));quietMax=Math.max(quietMax,Math.abs(quiet.height(x,y,i,null))); }
        check(roughMax>quietMax*1.2f,"passing wakes increase local rise-and-fall amplitude");
        float height=g.tug.heave;int age=g.water.age[latest];g.background();g.advance(.25);
        check(g.tug.heave==height && g.water.age[latest]==age,"background pause freezes floating motion and persistent wakes");
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && height==g.tug.heave,"water rendering is pure");
        System.out.println(checks+" directional drag, glide, heave and trough-timed wake assertions passed");
    }
}
