package com.bargeboss.game;

/** Visible rigid leaves are solid watercraft targets at every opening, including folded panels. */
public final class LockDoorCollisionTest {
    private static int checks;
    private static void check(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    private static Lock pose(int gate,int tick){Lock l=new Lock();l.phase=gate==0?Lock.OPENING:Lock.ENTRY;l.ticks=tick;return l;}
    private static float opening(Lock l,int gate){return gate==0?l.exitOpening():l.entryOpening();}
    private static float clearance(Watercraft craft,int gate,float open){
        Body b=craft instanceof Body?(Body)craft:null;float min=Float.MAX_VALUE;
        for(int side=0;side<2;side++)for(int disc=0;disc<(b==null?1:3);disc++){
            float x=b==null?craft.waterX():b.discX(disc),y=b==null?craft.waterY():b.discY(disc);
            min=Math.min(min,LockDoors.pointDistance(x,y,LockDoors.hingeX(side),LockDoors.hingeY(gate),LockDoors.tipX(gate,side,open),LockDoors.tipY(gate,side,open)));
        }
        return min;
    }
    public static void run(){
        for(int gate=0;gate<2;gate++)for(int side=0;side<2;side++)for(int tick:new int[]{0,75,150,225,300}){
            Lock l=pose(gate,tick);float open=opening(l,gate),a=LockDoors.angle(gate,side,open);
            ArticulatedModel model=LockView.model(gate,open);ArticulatedModel.Pose rendered=model.pose(0);
            float[] tip=rendered.point(model.nodeId(side==0?"left-leaf":"right-leaf"),LockDoors.LENGTH,0,0);
            check(Math.abs(tip[0]-LockDoors.tipX(gate,side,open))<.001f && Math.abs(tip[1]-LockDoors.tipY(gate,side,open))<.001f,"collider endpoints match rendered rigid leaf endpoints");
            float ux=(float)StrictMath.cos(a),uy=(float)StrictMath.sin(a),nx=-uy,ny=ux;
            if(side==0?nx<0:nx>0){nx=-nx;ny=-ny;}
            float px=LockDoors.hingeX(side)+ux*LockDoors.LENGTH*.52f,py=LockDoors.hingeY(gate)+uy*LockDoors.LENGTH*.52f;
            for(int frame=0;frame<32;frame++)for(int kind=0;kind<2;kind++){
                float angle=frame*(float)Math.PI/16;
                Body b=new Body(kind==0?0:10,kind==0?-1:0,0,0,angle);
                float projected=b.half*Math.abs((float)StrictMath.cos(angle)*nx+(float)StrictMath.sin(angle)*ny);
                b.x=px+nx*(b.radius+Lock.GATE_HALF+projected-2);b.y=py+ny*(b.radius+Lock.GATE_HALF+projected-2);
                b.vx=-nx*45;b.vy=-ny*45;CollisionSplashes spray=new CollisionSplashes();
                float hit=l.collide(b,spray);
                check(hit>0 && spray.emitted>0,"tug/barge contact stops and splashes at every leaf angle and hull heading");
                check(clearance(b,gate,open)>=b.radius+Lock.GATE_HALF-.02f,"all hull discs stay outside visible leaf footprint");
                for(int disc=0;disc<3;disc++)check(b.discX(disc)-b.radius>=Physics.LEFT-.01f && b.discX(disc)+b.radius<=Physics.RIGHT+.01f,"door correction cannot push hull through bank");
            }
            for(float size:new float[]{BuoyArt.CHANNEL_SIZE,BuoyArt.MARKER_SIZE,BuoyArt.PRACTICE_SIZE}){
                float offset=size*.27f+Lock.GATE_HALF-2;
                Buoys.Buoy buoy=new Buoys.Buoy(px+nx*offset,py+ny*offset,size);buoy.vx=-nx*35;buoy.vy=-ny*35;
                CollisionSplashes spray=new CollisionSplashes();float hit=l.collide(buoy,spray);
                check(hit>0 && spray.emitted>0 && clearance(buoy,gate,open)>=buoy.radius+Lock.GATE_HALF-.02f,"floating buoys/channel marks collide and splash against each door pose");
            }
            check(l.blocksLine(px+nx*30,py+ny*30,px-nx*30,py-ny*30),"tow throws cannot cross a visible folded or swinging panel");
        }
        for(int gate=0;gate<2;gate++){
            Lock l=pose(gate,300);Body middle=new Body(0,-1,300,LockDoors.hingeY(gate),-(float)Math.PI/2);
            middle.vy=-50;float x=middle.x,y=middle.y;
            check(l.collide(middle,null)==0 && middle.x==x && middle.y==y,"fully open center channel remains unobstructed");
            check(!l.blocksLine(300,y-50,300,y+50),"towline can pass through open central gap");
            l=pose(gate,200);float open=opening(l,gate);middle.y=LockDoors.hingeY(gate);
            check(l.collide(middle,null)==0 && clearance(middle,gate,open)>middle.radius,"partly open gap has no invisible full-width collision wall");
        }
        Game g=new Game();g.challengeId=2;g.reset();
        g.tug.x=300;g.tug.y=Lock.ENTRY_Y+g.tug.radius+g.tug.half+Lock.GATE_HALF;g.tug.angle=-(float)Math.PI/2;
        g.cargo[0]=new Body(40,0,300,g.tug.y+30,-(float)Math.PI/2);g.cargo[0].vy=-90;
        g.step();check(Lock.front(g.tug)>=Lock.ENTRY_Y+Lock.GATE_HALF-.01f,"post-contact solve prevents barge from pushing tug through sealed gate");
        // A failed cycle freezes the visible door pose; it does not delete its collision target.
        g=new Game();g.challengeId=2;g.reset();g.lock.phase=Lock.ENTRY;g.lock.ticks=150;g.lock.step(g);
        check(g.lock.failed(),"missing target fixture freezes lock mid-opening");
        float open=g.lock.entryOpening(),a=LockDoors.angle(1,0,open),ux=(float)StrictMath.cos(a),uy=(float)StrictMath.sin(a);
        Body frozen=new Body(0,-1,Physics.LEFT+ux*70+uy*18,Lock.ENTRY_Y+uy*70-ux*18,0);frozen.vx=-uy*40;frozen.vy=ux*40;
        check(g.lock.collide(frozen,g.splashes)>0,"failed lock retains collision for its frozen visible panel");
        System.out.println(checks+" lock-door pose, all-watercraft collision, splash, bank and post-contact assertions passed");
    }
    public static void main(String[] args){run();}
}
