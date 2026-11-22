package com.bargeboss.game;

/** Goal, momentum, physical slip and real public pointer towing replay. */
public final class MontroseTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Game game(){return ReplayInput.montroseJob(Challenges.MONTROSE);}
    static void drive(Game g,float x,float y,float speed){
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Physics.length(dx,dy);
        if(!g.pointer)ReplayInput.throttleDown(g);
        if(len<8)ReplayInput.throttle(g,0,0);
        else ReplayInput.throttle(g,dx/len*speed,dy/len*speed);
    }
    static Game voyage(boolean log){
        Game g=game();Body b=g.cargo[0];
        ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);
        for(int i=0;i<45;i++)g.step();
        check(g.tow.connected(b,g.tug),"Native screen-space release attaches yacht towline");
        for(int tick=0;tick<18000 && !b.deliveryRecorded && g.mode==Game.PLAY;tick++){
            ReplayInput.work(g);g.step();
            if(log && tick%600==0)System.out.println(tick+" tug="+g.tug.x+","+g.tug.y+" yacht="+b.x+","+b.y+" rope="+g.tow.count()+" settle="+g.mission.settledTicks);
        }
        if(log)System.out.println(g.status()+" yacht="+b.x+","+b.y+" angle="+b.angle+" speed="+b.speed());
        check(b.deliveryRecorded && g.challengeId==Challenges.RAFT_RESCUE && g.mode==Game.PLAY,"Public-input yacht tow continues to next harbor job");
        check(g.hull==100 && b.active && b.unloadingTicks==0,"Safe delivery retains yacht without cargo unloading");
        check(g.career.bestStars(Challenges.MONTROSE)==3 && g.score==1200,"Yacht goal rewards once");
        return g;
    }
    public static void run(){
        Game g=game();Body b=g.cargo[0];check(g.feeders.length==0 && b.yacht,"Marina has a staged yacht, no barge feeder");
        check(b.mass>g.tug.mass && b.half*2+b.radius*2==110,"80ft yacht mapped to longer, heavier hull");
        check(Math.abs(g.layout.camera.zoom-Game.TRANSIT_ZOOM*2)<.0001f,"Montrose starts at twice the former default zoom");
        NavigationLights.Entrance entrance=NavigationLights.entrances(g)[0];
        for(Body craft:new Body[]{g.tug,b})for(int i=0;i<3;i++){
            float along=(craft.discX(i)-entrance.x)*entrance.dx+(craft.discY(i)-entrance.y)*entrance.dy;
            check(along+craft.radius<0,"Whole starting hull lies lakeward of both breakwater tips");
            check(MontroseHarbor.contains(craft.discX(i),craft.discY(i)),"Starting hull lies in open lake");
        }
        check(Math.abs(NavigationLights.anchorX(g,0)-MontroseHarbor.mapX(369))<.001f && Math.abs(NavigationLights.anchorY(g,0)-MontroseHarbor.mapY(1258))<.001f,"Port green tower is on western breakwater tip");
        check(Math.abs(NavigationLights.anchorX(g,1)-MontroseHarbor.mapX(548))<.001f && Math.abs(NavigationLights.anchorY(g,1)-MontroseHarbor.mapY(1290))<.001f,"Starboard red tower is on eastern breakwater tip");
        check(MontroseHarbor.PEDESTALS.length>100,"All fifteen dock rows and star hubs have pedestal fixtures");
        for(float[] fixture:MontroseHarbor.PEDESTALS){
            check(Math.abs(Physics.length(fixture[2],fixture[3])-1)<.0001f,"Pedestal has a unit dock direction");
            check(!MontroseHarbor.contains(fixture[0],fixture[1]),"Pedestal is supported by dock geometry");
        }
        float startY=b.y;b.vy=-50;Physics.integrate(b);check(b.vy<0 && b.y<startY,"Released yacht retains directional momentum");
        b.x=MontroseHarbor.SLIP_X;b.y=MontroseHarbor.SLIP_Y;b.angle=MontroseHarbor.SLIP_ANGLE;b.vx=b.vy=b.spin=0;
        for(int i=0;i<100;i++)g.step();check(!g.goalComplete(),"Teleport/push to slip without inlet tow cannot complete");
        check(MontroseHarbor.fits(b),"Complete aligned hull fits enlarged slip");b.angle=0;check(!MontroseHarbor.fits(b),"Sideways hull cannot complete");
        // Approach N's shaft from its enlarged east-side bay; all disc edges stay outside wood.
        Body wall=new Body(8,2,MontroseHarbor.mapX(1007),MontroseHarbor.mapY(420),MontroseHarbor.SLIP_ANGLE,true);wall.vx=-80;
        for(int i=0;i<120;i++){Physics.integrate(wall);MontroseHarbor.collide(wall,null);}
        for(int i=0;i<3;i++)check(MontroseHarbor.contains(wall.discX(i),wall.discY(i)),"Oriented wooden dock blocks yacht discs");
        check(!MontroseHarbor.clear(MontroseHarbor.mapX(980),MontroseHarbor.mapY(325),MontroseHarbor.mapX(980),MontroseHarbor.mapY(315)),"Towline cannot pass through back of wooden slip");
        check(MontroseHarbor.openWater(MontroseHarbor.mapX(850),MontroseHarbor.mapY(580)),"Central fairway preserved from source map");
        check(!MontroseHarbor.openWater(MontroseHarbor.mapX(530),MontroseHarbor.mapY(900)),"Southern hooked peninsula is solid land");
        check(MontroseHarbor.openWater(MontroseHarbor.mapX(400),MontroseHarbor.mapY(600)),"Rounded western mooring basin remains water");
        check(MontroseHarbor.openWater(MontroseHarbor.mapX(1000),MontroseHarbor.mapY(1300)),"Open lake outside peninsula remains water");
        Career c=new Career();check(c.select(Challenges.MONTROSE) && c.record(Challenges.MONTROSE,3,1200),"Independent harbor reward recorded");
        Career restored=new Career();check(restored.restore(c.save()) && restored.bestStars(0)==0 && restored.bestStars(Challenges.MONTROSE)==3,"Optional mission saves without inventing prior wins");
        voyage(false);System.out.println(checks+" Montrose hull, momentum, dock collision, career and playable tow assertions passed");
    }
    public static void main(String[] args){voyage(true);}
}
