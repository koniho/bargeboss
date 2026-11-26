package com.bargeboss.game;

/** Buoy displacement, finite moorings, vessel momentum, wakes and shared-step lifecycle. */
public final class BuoyPhysicsTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static Game practice(){Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tutorial.setup(g);return g;}
    private static Buoys.Buoy first(Game g){return g.buoys.get(g,NavigationLights.count(g)-3);}
    private static float distance(Buoys.Buoy b){return Physics.length(b.x-b.anchorX,b.y-b.anchorY);}
    private static void approach(Body hull,Buoys.Buoy b,float angle){
        hull.angle=angle;float c=(float)StrictMath.cos(angle),s=(float)StrictMath.sin(angle);
        hull.x=b.x-c*(hull.half+hull.radius+b.radius-2);hull.y=b.y-s*(hull.half+hull.radius+b.radius-2);
        hull.vx=50*c;hull.vy=50*s;
    }
    public static void run(){
        for(int heading=0;heading<32;heading++)for(boolean cargo:new boolean[]{false,true}){
            Game g=practice();Buoys.Buoy b=first(g);
            Body hull=cargo?new Body(91,0,0,0,0):g.tug;
            float a=(float)(heading*StrictMath.PI/16);approach(hull,b,a);
            float px=hull.mass*hull.vx,py=hull.mass*hull.vy;
            Buoys.contact(g,b,hull,false);
            check(distance(b)>0 && Physics.length(b.vx,b.vy)>1,"all hull headings transfer displacement and motion to float");
            check(hull.speed()<50 && Math.abs(hull.mass*hull.vx+b.mass*b.vx-px)<.001f
                    && Math.abs(hull.mass*hull.vy+b.mass*b.vy-py)<.001f,"free contact exchanges momentum with tug and loaded barge");
            check(Math.abs(b.rollVelocity)>0 || Math.abs((float)StrictMath.cos(a)+(float)StrictMath.sin(a))<.001f,"contact excites visible roll except along screen depth");
        }
        Game g=practice();Buoys.Buoy b=first(g);approach(g.tug,b,0);g.tug.y+=9;
        Buoys.contact(g,b,g.tug,false);check(Math.abs(g.tug.spin)>.001f,"off-center float contact transfers yaw to hull");
        g=practice();b=first(g);approach(g.tug,b,0);g.step();
        check(distance(b)>1 && b.vx>0,"actual game motion step resolves buoy collision");
        check(g.hull==100,"small floating fenders do not cause hull damage");
        int index=NavigationLights.count(g)-3;
        check(NavigationLights.x(g,index)==b.x && NavigationLights.y(g,index)==b.y,"depth, lights, shadows and draw location use displaced body");
        float x=b.x,y=b.y,roll=b.roll,vx=b.vx;
        Renderer.draw(g,new Parity());Renderer.draw(g,new Parity());
        check(b.x==x && b.y==y && b.roll==roll && b.vx==vx,"rendering does not integrate buoy state");
        g.background();g.advance(.2);check(b.x==x && b.y==y && b.roll==roll,"pause freezes mooring physics");
        g.tutorial.setup(g);b=first(g);check(distance(b)==0 && b.vx==0 && b.roll==0,"tutorial restart resets moorings");
        g=practice();b=first(g);g.tug.x=b.anchorX-250;
        b.x+=b.reach*.85f;b.vx=80;b.roll=.15f;
        for(int tick=0;tick<1200;tick++){
            g.visualTicks++;g.buoys.step(g);
            check(distance(b)<=b.reach+.001f && Buoys.onWater(g,b,b.x,b.y),"tether and water bounds hold while settling");
        }
        check(distance(b)<2 && Math.abs(b.roll)<.03f,"released float settles near its anchor with damped rocking");
        // Sustained outward pushes load the chain and move the boat back, rather than detaching the buoy.
        g=practice();b=first(g);b.x+=b.reach;
        for(int tick=0;tick<180;tick++){
            g.tug.x=b.x-b.radius-g.tug.radius-g.tug.half+1;g.tug.y=b.y;g.tug.angle=0;g.tug.vx=70;
            float before=g.tug.x;g.buoys.step(g);
            check(distance(b)<=b.reach+.001f && g.tug.x<before,"taut chain resists continued hull penetration");
        }
        // NPC and secured hulls keep their path/berth while pushing the lightweight float.
        g=practice();b=first(g);Body fixed=new Body(80,0,0,0,0);approach(fixed,b,0);
        x=fixed.x;y=fixed.y;vx=fixed.vx;Buoys.contact(g,b,fixed,true);
        check(fixed.x==x && fixed.y==y && fixed.vx==vx && b.vx>0,"kinematic NPC/secured hull pushes float without moving off its route");
        // Career marks float just inside the banks; their full footprint must stay wet under force.
        for(int stage=0;stage<4;stage++){
            g=new Game();g.challengeId=stage;g.resetPracticeYard();
            for(int i=0;i<NavigationLights.count(g);i++){
                b=g.buoys.get(g,i);check(b!=null,"career navigation marks are physical floats");
                for(int tick=0;tick<80;tick++){
                    b.vx=i%2==0?-100:100;g.buoys.step(g);
                    check(Buoys.onWater(g,b,b.x,b.y) && distance(b)<=b.reach+.001f,"bank-side float remains inside water and tether radius");
                }
            }
        }
        g=practice();for(int i=0;i<NavigationLights.count(g)-3;i++)check(g.buoys.get(g,i)==null,"land markers have no floating collision body");
        check(g.buoys.get(g,NavigationLights.count(g)-1)!=null,"practice mooring ball is a floating body");
        g=new Game();g.resetPracticeYard();b=g.buoys.get(g,0);b.x+=b.reach;
        g.tug.x=Physics.LEFT+g.tug.radius;g.tug.y=b.y;g.tug.angle=(float)Math.PI/2;
        g.buoys.step(g);
        for(int disc=0;disc<3;disc++)check(g.tug.discX(disc)-g.tug.radius>=Physics.LEFT-.001f,"buoy contact cannot push hull through adjacent bank");
        float[] route=new float[2];g.tug.x=400;g.tug.y=805;g.buoys.pickupWaypoint(g,400,720,route);
        check(route[0]<400,"pickup assistance routes around solid entrance mark on open-water side");
        // A barge can be pushed along either end of its keel, including after a float deflects it.
        g=practice();g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);
        Body load=g.cargo[0];load.x=230;load.y=600;load.angle=(float)Math.PI/2;load.awaitingPickup=false;
        g.tug.x=230;g.tug.y=674;g.tug.angle=-(float)Math.PI/2;
        g.down(load.x,load.y);
        for(int tick=0;tick<120;tick++)g.step();
        check(Math.abs(Physics.angle(load.angle-(float)Math.PI/2))<.15f && load.y<580,"assist uses nearest keel alignment rather than turning a barge end-for-end");
        // Nearby passing traffic sends a delayed rear wake over the float without touching it.
        Game calm=practice(),wake=practice();Buoys.Buoy cb=first(calm),wb=first(wake);
        wake.tug.x=wb.x+100;wake.tug.y=wb.y;wake.tug.vx=50;wake.tug.wakeTrailTicks=7;wake.water.step(wake);
        wake.tug.x=wb.x+350;wake.tug.vx=0;calm.tug.x=cb.x+350;
        float maxDifference=0,rollDifference=0;
        for(int tick=0;tick<220;tick++){
            calm.visualTicks++;wake.visualTicks++;calm.water.step(calm);wake.water.step(wake);
            calm.buoys.step(calm);wake.buoys.step(wake);
            maxDifference=Math.max(maxDifference,Physics.length(cb.x-wb.x,cb.y-wb.y));
            rollDifference=Math.max(rollDifference,Math.abs(cb.roll-wb.roll));
        }
        check(maxDifference>.15f && rollDifference>.001f,"arriving real wake moves and rocks a noncontact float");
        Game blocked=new Game();blocked.challengeId=1;blocked.resetPracticeYard();
        blocked.tug.x=599;blocked.tug.y=500;blocked.tug.vx=50;blocked.tug.wakeTrailTicks=7;blocked.water.step(blocked);
        float[] screened=new float[2],open=new float[2];boolean differs=false;
        Game openWater=new Game();openWater.visualTicks=blocked.visualTicks;
        for(int age=160;age<250;age++){
            blocked.water.age[0]=age;blocked.water.rebuildWakeBounds();blocked.water.buoyForce(blocked,200,500,screened);
            blocked.water.buoyForce(openWater,200,500,open);
            float ambient=SeaWaves.value(blocked,200,500,blocked.visualTicks,1)*1.4f;
            check(Math.abs(screened[0]-ambient)<.0001f,"solid quay screens horizontal wake force");
            if(Math.abs(open[0]-screened[0])>.01f)differs=true;
        }
        check(!differs,"all Calumet missions retain the same quay screening");
        Game low=practice(),high=practice();approach(low.tug,first(low),0);approach(high.tug,first(high),0);
        for(int i=0;i<120;i++)low.advance(1.0/30);
        for(int i=0;i<576;i++)high.advance(1.0/144);
        Buoys.Buoy l=first(low),h=first(high);
        check(l.x==h.x && l.y==h.y && l.vx==h.vx && l.roll==h.roll,"30 and 144 Hz produce identical buoy simulation");
        System.out.println(checks+" buoy collision, mooring, wake, bank and lifecycle assertions passed");
    }
    public static void main(String[] args){run();}
}
