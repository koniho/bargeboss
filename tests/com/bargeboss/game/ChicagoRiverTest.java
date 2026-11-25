package com.bargeboss.game;

/** Chicago River survey, harbor-lock cycle, fire-barge tow and wheel fire contract. */
public final class ChicagoRiverTest {
    private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
    private static Game stage(){Game g=new Game();g.challengeId=Challenges.CHICAGO;g.reset();return g;}
    private static void place(Body b,HarborLock lock,float along,float across,float angle){
        b.x=lock.originX+lock.axisX*along-lock.axisY*across;b.y=lock.originY+lock.axisY*along+lock.axisX*across;b.angle=angle;b.vx=b.vy=b.spin=0;
    }
    private static int stepUntil(Game g,int limit,java.util.function.BooleanSupplier done){int n=0;while(n<limit && !done.getAsBoolean()){g.step();n++;}return n;}
    public static void run(){
        Game g=stage();
        check(g.chicagoStage() && g.surveyStage() && !g.indianaStage() && !g.montroseStage(),"Chicago is its own surveyed stage");
        check(Challenges.locationTitle(Challenges.CHICAGO).equals("CHICAGO RIVER") && Challenges.pageFor(Challenges.CHICAGO)==1,"Chicago is a selectable harbor on the second page");
        check(ChicagoRiver.COAST.length>800 && ChicagoRiver.WATER.length>300,"survey retains the detailed river and harbor ring");
        check(g.inWater(g.tug.x,g.tug.y),"tug starts in the Navy Pier basin");
        Body barge=g.chicago.barge;
        check(barge!=null && barge.ability==BargeAbilities.FIRE && barge.awaitingPickup && g.inWater(barge.x,barge.y),"fire barge waits at the Engine 2 pier");
        // Geography: confluence, both branches, Ogden Slip, the lock chamber; Navy Pier and the wheel are dry.
        check(ChicagoRiver.contains(ChicagoRiver.north(41.88690),ChicagoRiver.east(-87.63790)),"Wolf Point confluence is water");
        check(ChicagoRiver.contains(ChicagoRiver.north(41.88908),ChicagoRiver.east(-87.63949)),"North Branch is water");
        check(ChicagoRiver.contains(ChicagoRiver.north(41.88400),ChicagoRiver.east(-87.63800)),"South Branch is water");
        check(ChicagoRiver.contains(ChicagoRiver.north(41.89040),ChicagoRiver.east(-87.61600)),"Ogden Slip is water");
        check(!ChicagoRiver.contains(ChicagoRiver.north(41.88770),ChicagoRiver.east(-87.63780)),"Wolf Point itself is land");
        check(!ChicagoRiver.contains(ChicagoRiver.WHEEL_X,ChicagoRiver.WHEEL_Y),"wheel stands on Navy Pier");
        check(!ChicagoRiver.contains(ChicagoRiver.north(41.89170),ChicagoRiver.east(-87.60200)),"Navy Pier is solid");
        HarborLock lock=g.chicago.lock;
        check(lock.length>480 && lock.halfWidth*2>=2.8f*2*new Body(0,-1,0,0,0).radius && lock.halfWidth*2<=3.4f*2*new Body(0,-1,0,0,0).radius,"lock chamber fits about three tug beams");
        for(LandmarkBridge b:LocationBridges.chicago()){
            check(!b.opens() && b.rise>0,b.id+" stays closed: the tug passes beneath");
            check(b.height-18>=LandmarkBridge.CHICAGO_RISE,b.id+" deck clears the tug funnel");}
        check(ChicagoRiver.contains(lock.centerX(),lock.centerY()),"lock chamber is navigable");
        // Monitors reach every burning pavilion from the open-water spray station; flames sit
        // on the pier deck, low enough for the monitors' flat arc.
        for(int i=0;i<ChicagoFire.PILES;i++)check(!ChicagoRiver.contains(ChicagoFire.x(i),ChicagoFire.y(i)) && ChicagoFire.rise(i)<=25,"fire "+i+" burns on the pier, within the spray arc");
        check(ChicagoRiver.contains(ChicagoMission.SPRAY[0],ChicagoMission.SPRAY[1]),"spray station is open water");
        for(int i=0;i<ChicagoFire.PILES;i++)check(Physics.length(ChicagoFire.x(i)-ChicagoMission.SPRAY[0],ChicagoFire.y(i)-ChicagoMission.SPRAY[1])<200,"fire "+i+" within monitor range");
        check(LocationBridges.chicago().length>=15,"downtown movable bridges are represented");

        // Closed gates are solid.
        place(g.tug,lock,lock.length*.5f,0,(float)StrictMath.atan2(lock.axisY,lock.axisX));
        g.tug.vx=lock.axisX*160;g.tug.vy=lock.axisY*160;
        for(int i=0;i<180;i++){g.tug.vx=lock.axisX*160;g.tug.vy=lock.axisY*160;g.step();}
        check(lock.along(g.tug.x,g.tug.y)<lock.length,"closed lake gate stops the tug");

        // Inbound: approach from the lake, levels, opens, admits, lowers and releases upstream.
        g=stage();lock=g.chicago.lock;final Game a=g;final HarborLock la=lock;
        float west=(float)StrictMath.atan2(-lock.axisY,-lock.axisX);
        place(g.tug,lock,lock.length+110,0,west);
        stepUntil(g,2400,()->la.phase==HarborLock.OPEN && la.gate==HarborLock.LAKE);
        check(lock.phase==HarborLock.OPEN && lock.gate==HarborLock.LAKE && lock.level==1,"lake approach levels the chamber and opens the lake gates");
        place(g.tug,lock,lock.length*.5f,0,west);
        stepUntil(g,2400,()->la.phase==HarborLock.OPEN && la.gate==HarborLock.RIVER);
        check(lock.phase==HarborLock.OPEN && lock.gate==HarborLock.RIVER && lock.level==0,"enclosed tug is lowered and the river gates open");
        place(g.tug,lock,-160,0,west);
        stepUntil(g,120,()->a.chicago.phase!=ChicagoMission.TO_LOCK);
        check(g.chicago.phase==ChicagoMission.FETCH,"passing the river gate starts the fire-barge fetch");

        // Fetch: a public line throw connects the Engine 2 barge.
        barge=g.chicago.barge;final Body fire=barge;
        g.tug.x=barge.x+20;g.tug.y=barge.y-110;g.tug.angle=west;g.tug.vx=g.tug.vy=0;
        g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y,Game.WORK_ZOOM,g.layout.width*.5f,g.layout.height*.52f);
        for(int i=0;i<10;i++)g.step();
        ReplayInput.down(g,barge.x,barge.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
        stepUntil(g,240,()->a.tow.connected(fire,a.tug));g.step();
        check(g.tow.connected(barge,g.tug) && g.chicago.phase==ChicagoMission.TOW_OUT,"line throw collects the fire barge");

        // Outbound: tug and tow inside rise to the lake and clear into the harbor.
        float east=(float)StrictMath.atan2(lock.axisY,lock.axisX);
        place(g.tug,lock,-120,0,east);place(barge,lock,-210,0,east);
        stepUntil(g,2400,()->la.phase==HarborLock.OPEN && la.gate==HarborLock.RIVER);
        check(lock.phase==HarborLock.OPEN && lock.gate==HarborLock.RIVER,"river approach opens the river gates");
        place(g.tug,lock,lock.length-70,0,east);place(barge,lock,lock.length-170,0,east);
        stepUntil(g,2400,()->la.phase==HarborLock.OPEN && la.gate==HarborLock.LAKE);
        check(lock.phase==HarborLock.OPEN && lock.gate==HarborLock.LAKE && lock.level==1,"tow is raised and the lake gates open");
        place(g.tug,lock,lock.length+220,0,east);place(barge,lock,lock.length+130,0,east);
        stepUntil(g,120,()->a.chicago.phase==ChicagoMission.FIGHT);
        check(g.chicago.phase==ChicagoMission.FIGHT && g.tow.connected(barge,g.tug),"towing clear of the lock starts the firefight");

        // Only droplets at the burning pavilions cool them.
        ChicagoFire f=g.chicago.fire;float floor=GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE;
        check(!f.hit(ChicagoMission.SPRAY[0],ChicagoMission.SPRAY[1],floor+10,floor),"open water is not on fire");
        for(int pile=0;pile<ChicagoFire.PILES;pile++)for(int n=0;n<ChicagoFire.PILE_HEAT;n++)check(f.hit(ChicagoFire.x(pile),ChicagoFire.y(pile),floor+ChicagoFire.rise(pile),floor),"droplet cools pile "+pile);
        g.step();
        check(f.out() && g.chicago.complete() && g.goalComplete() && g.mode==Game.FINISH,"extinguishing the wheel completes the call");
        System.out.println("Chicago River survey, harbor lock and Navy Pier fire pass");
    }
    public static void main(String[] args){run();}
}
