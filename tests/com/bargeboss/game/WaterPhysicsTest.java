package com.bargeboss.game;

public final class WaterPhysicsTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++; if(!ok) throw new AssertionError(message); }
    private static Game isolated() { Game g=new Game();g.resetPracticeYard();for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}g.tug.x=300;g.tug.y=450;return g; }
    public static void run() {
        orbitalCompatibility();
        Body along=new Body(1,0,300,450,0),sideways=new Body(2,0,300,450,(float)Math.PI/2),reverse=new Body(3,0,300,450,0);
        along.vx=sideways.vx=60;reverse.vx=-60;
        for(int i=0;i<120;i++) { Physics.integrate(along);Physics.integrate(sideways);Physics.integrate(reverse); }
        check(along.vx>30 && along.vx<40 && along.x-300>85,"unpowered barge keeps meaningful forward momentum for two physics seconds");
        check(along.vx>sideways.vx*2 && along.x-300>(sideways.x-300)*1.5f,"broadside drag depends on hull orientation relative to motion");
        check(Math.abs(along.vx+reverse.vx)<.1f,"reverse keel motion retains symmetric water resistance");
        Body barge=new Body(1,0,300,450,0),tug=new Body(0,-1,300,450,0);barge.spin=tug.spin=1;
        for(int i=0;i<120;i++) { Physics.integrate(barge);Physics.integrate(tug); }
        check(barge.spin>tug.spin*3,"barge yaw carries inertia longer than the small tug");
        Game dock=isolated();Body load=new Body(1,2,230,dock.dockY(2),0);load.vx=15;dock.cargo[0]=load;
        for(int i=0;i<30;i++) dock.step();
        check(load.speed()>6 && load.speed()<10,"an unmoored barge does not snap to a stop just for entering a berth");
        Game g=isolated();g.tug.vx=40;int emissions=0;
        for(int i=0;i<500;i++) {
            float before=g.tug.heaveVelocity;int count=g.water.pulses;g.visualTicks++;g.water.step(g);
            if(g.water.pulses>count) {
                emissions++;check(before<0 && g.tug.heaveVelocity>=0,"underway wake starts at the bottom of the fall");
                check(g.tug.wakeKickTicks==18,"trough starts one bounded foam burst");
            }
        }
        check(g.water.emitted>50,"underway trail is continuous between trough pulses");
        check(emissions>=3 && emissions<=6,"wake generation follows wave cycles instead of emitting every frame");
        Game still=isolated();for(int i=0;i<500;i++){still.visualTicks++;still.water.step(still);}
        check(still.water.emitted==0,"stationary floating hulls bob without creating underway wakes");
        int latest=-1;for(int i=0;i<WaterSurface.CAPACITY;i++) if(g.water.age[i]<WaterSurface.LIFE && (latest<0 || g.water.age[i]<g.water.age[latest]))latest=i;
        float x=g.water.x[latest]-WaterSurface.wakeRadius(g.water.age[latest]),y=g.water.y[latest];
        check(g.water.energy(g,x,y,null)>g.water.energy(g,x+500,y+500,null),"wake energy spreads locally rather than changing the whole river");
        WaterSurface quiet=new WaterSurface();float roughMax=0,quietMax=0;
        for(int i=0;i<250;i++) { roughMax=Math.max(roughMax,Math.abs(g.water.height(g,x,y,i,null)));quietMax=Math.max(quietMax,Math.abs(quiet.height(g,x,y,i,null))); }
        check(roughMax>quietMax*1.2f,"passing wakes increase local rise-and-fall amplitude");
        float height=g.tug.heave;int age=g.water.age[latest];g.background();g.advance(.25);
        check(g.tug.heave==height && g.water.age[latest]==age,"background pause freezes floating motion and persistent wakes");
        int frame=Parity.frame(g);check(frame==Parity.frame(g) && height==g.tug.heave,"water rendering is pure");
        WaterSurface trail=new WaterSurface();trail.x[0]=300;trail.y[0]=500;trail.strength[0]=1;trail.heading[0]=0;trail.age[0]=20;
        g.water=trail;WaveBounds early=new WaveBounds();trail.draw(g,early);
        trail.age[0]=100;WaveBounds later=new WaveBounds();trail.draw(g,later);
        check(later.maxY-later.minY>(early.maxY-early.minY)*2,"emitted wake spreads outward through the water");
        float anchorX=trail.x[0],anchorY=trail.y[0];g.tug.x=410;g.tug.y=760;g.tug.vx=g.tug.vy=0;
        for(int i=0;i<30;i++) { g.visualTicks++;trail.step(g); }
        check(trail.x[0]==anchorX && trail.y[0]==anchorY && trail.age[0]==130,"old wake stays in the river and continues after its boat moves away or stops");
        System.out.println(checks+" directional drag, glide, heave and trough-timed wake assertions passed");
    }
    private static void orbitalCompatibility(){
        Game g=new Game();g.challengeId=Challenges.MONTROSE;float[] pair=new float[2];
        for(int power:new int[]{0,4,12})for(int bearing=0;bearing<24;bearing++){
            g.settings.resetWaves();g.settings.adjustWaves(power-4);g.settings.turnWaves(bearing);
            for(int sample=0;sample<20;sample++){
                float x=MontroseHarbor.mapX(-100+sample*151),y=MontroseHarbor.mapY(-2100+sample*173),tick=sample*41.25f;
                SeaWaves.montroseDisplacement(g,x,y,tick,pair);
                for(int axis=0;axis<2;axis++)check(Float.floatToIntBits(pair[axis])==Float.floatToIntBits(SeaWaves.value(g,x,y,tick,axis+3)),"Paired orbital displacement preserves scalar wave field exactly");
            }
        }
        for(float x:new float[]{Float.NEGATIVE_INFINITY,-100,-1,-0f,0,.01f,.5f,1,2,Float.POSITIVE_INFINITY,Float.NaN}){
            float t=Physics.clamp(x,0,1),expected=t*t*(3-2*t);
            check(Float.floatToIntBits(SeaWaves.smooth(0,1,x))==Float.floatToIntBits(expected),"Fast smooth keeps endpoint, NaN and signed-zero behavior");
        }
    }
    private static final class WaveBounds implements Painter {
        float minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        public void line(float x,float y,float xx,float yy,float width,int color) { minY=Math.min(minY,Math.min(y,yy));maxY=Math.max(maxY,Math.max(y,yy)); }
        public void circle(float x,float y,float r,int color) {}
        public void rect(float x,float y,float w,float h,int color) {}
        public void poly(float[] xy,int color) { for(int i=1;i<xy.length;i+=2) {minY=Math.min(minY,xy[i]);maxY=Math.max(maxY,xy[i]);} }
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float angle,float alpha) {}
        public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float angle,float blend,float alpha) {}
    }
}
