package com.bargeboss.game;

/** Physical passage constraints, aligned access and full-hull berth fit, without order flags. */
public final class PhysicalBerthsTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static Body barge(int id,int berth,float x,float y){return new Body(id,berth,x,y,-(float)Math.PI/2);}
    private static boolean hullWater(int stage,Body b){
        for(int disc=0;disc<3;disc++)for(int sample=0;sample<16;sample++){
            double a=sample*Math.PI/8;
            if(!PhysicalBerths.contains(stage,b.discX(disc)+b.radius*(float)StrictMath.cos(a),
                    b.discY(disc)+b.radius*(float)StrictMath.sin(a)))return false;
        }
        return true;
    }
    public static void run(){
        for(int stage:new int[]{PhysicalBerths.SHARED,PhysicalBerths.DEEP}){
            check(PhysicalBerths.active(stage),"physical berth stage declares its geometry");
            for(int berth=0;berth<3;berth++){
                Body b=barge(1,berth,PhysicalBerths.dockX(stage,berth),PhysicalBerths.dockY(stage,berth));
                check(PhysicalBerths.fits(stage,b) && hullWater(stage,b),"full aligned hull fits each berth and stays in painted water");
                b.angle=0;check(!PhysicalBerths.fits(stage,b),"sideways hull cannot be winched through a one-hull-wide opening");
                b.angle=-(float)Math.PI/2;b.x+=13;
                check(!PhysicalBerths.fits(stage,b),"center inside is insufficient when the hull reaches the right quay");
            }
            Body incoming=barge(2,2,PhysicalBerths.CENTER,PhysicalBerths.APPROACH);
            float target=PhysicalBerths.dockY(stage,2);
            for(float y=PhysicalBerths.APPROACH;y>=target;y-=2){
                incoming.y=y;check(hullWater(stage,incoming),"aligned push approach has continuous full-hull clearance");
            }
            float[] route=new float[2];
            PhysicalBerths.waypoint(stage,360,730,PhysicalBerths.CENTER,target,42.75f,route);
            check(route[0]==PhysicalBerths.CENTER && route[1]==PhysicalBerths.APPROACH,"incoming assist aligns in the broad basin before the mouth");
            PhysicalBerths.waypoint(stage,PhysicalBerths.CENTER,660,PhysicalBerths.CENTER,target,42.75f,route);
            check(route[0]==PhysicalBerths.CENTER && route[1]==target,"aligned approach proceeds down the actual open slot");
            PhysicalBerths.waypoint(stage,PhysicalBerths.CENTER,480,370,650,30,route);
            check(route[0]==PhysicalBerths.CENTER && route[1]==PhysicalBerths.APPROACH,"empty tow exits through the mouth before turning into the holding basin");
            check(!PhysicalBerths.clear(stage,PhysicalBerths.CENTER,480,350,480,3),"tow cannot shortcut through the right quay");
            check(PhysicalBerths.clear(stage,PhysicalBerths.CENTER,480,PhysicalBerths.CENTER,660,3),"towline can follow the open withdrawal route");
            Body sideways=barge(3,2,PhysicalBerths.RIGHT-5,480);sideways.vx=80;
            PhysicalBerths.collide(stage,sideways);
            check(sideways.x+sideways.radius<=PhysicalBerths.RIGHT+.001f && sideways.vx<=0,"right quay physically rejects an approaching aligned hull");
            check(PhysicalBerths.RIGHT-PhysicalBerths.LEFT<incoming.radius*4,"two barges cannot pass side by side");
            check(PhysicalBerths.RIGHT-PhysicalBerths.LEFT<incoming.radius*2+30,"tug cannot slip past a barge in the single-file channel");
        }
        check(PhysicalBerths.dockY(PhysicalBerths.SHARED,0)==PhysicalBerths.dockY(PhysicalBerths.SHARED,1)
                && PhysicalBerths.dockY(PhysicalBerths.SHARED,1)==PhysicalBerths.dockY(PhysicalBerths.SHARED,2),"all shared-slip loads use exactly one physical berth");
        float[] fromHead=new float[2];
        PhysicalBerths.waypoint(PhysicalBerths.SHARED,300,290,PhysicalBerths.CENTER,480,27.5f,fromHead);
        check(PhysicalBerths.clear(PhysicalBerths.SHARED,300,290,fromHead[0],fromHead[1],20),"approach from behind the slip head routes around concrete instead of through it");
        check(PhysicalBerths.dockY(PhysicalBerths.DEEP,2)<PhysicalBerths.dockY(PhysicalBerths.DEEP,0)
                && PhysicalBerths.dockY(PhysicalBerths.DEEP,0)<PhysicalBerths.dockY(PhysicalBerths.DEEP,1),"A, B, C lie deepest to nearest along one channel");
        float[] escape=new float[2];
        PhysicalBerths.waypoint(PhysicalBerths.DEEP,PhysicalBerths.RIGHT-18,480,PhysicalBerths.CENTER,660,21,escape);
        check(escape[0]==PhysicalBerths.CENTER && escape[1]==PhysicalBerths.APPROACH
                && PhysicalBerths.clear(PhysicalBerths.DEEP,PhysicalBerths.RIGHT-18,480,escape[0],escape[1],14),
                "quay planning margin escapes along the actual eastern channel instead of crossing concrete");
        PhysicalBerths.waypoint(PhysicalBerths.DEEP,PhysicalBerths.CENTER,168,PhysicalBerths.CENTER,480,21,escape);
        check(escape[0]==PhysicalBerths.CENTER && escape[1]>=169
                && PhysicalBerths.clear(PhysicalBerths.DEEP,PhysicalBerths.CENTER,168,escape[0],escape[1],18),
                "north cap routes away from the head into navigable water");
        PhysicalBerths.waypoint(PhysicalBerths.DEEP,410,440,PhysicalBerths.CENTER,220,42.75f,escape);
        check(escape[0]==300 && escape[1]==PhysicalBerths.APPROACH
                && PhysicalBerths.clear(PhysicalBerths.DEEP,410,440,escape[0],escape[1],20),
                "a load west of the relocated quay reaches the basin before crossing to the slip");
        Body parked=barge(4,1,PhysicalBerths.CENTER,480);parked.deliveryRecorded=true;parked.lineAttached=true;
        Body moving=barge(5,2,PhysicalBerths.CENTER,610);
        for(int tick=0;tick<240;tick++){
            moving.y-=1;moving.vy=-60;
            PhysicalBerths.collide(PhysicalBerths.DEEP,moving);
            for(int pass=0;pass<3;pass++){
                Physics.collide(moving,parked);
                // Mooring pins the parked empty, as Game's fixed attachment does.
                parked.x=PhysicalBerths.CENTER;parked.y=480;parked.angle=-(float)Math.PI/2;parked.vx=parked.vy=parked.spin=0;
            }
            PhysicalBerths.collide(PhysicalBerths.DEEP,moving);
            check(moving.y>parked.y,"near empty hull physically blocks the route to a deeper berth");
        }
        parked.x=370;parked.y=730;parked.lineAttached=false;
        for(int tick=0;tick<150;tick++){moving.y-=1;PhysicalBerths.collide(PhysicalBerths.DEEP,moving);Physics.collide(moving,parked);}
        check(moving.y<480 && parked.active,"parking the still-active empty elsewhere reopens the route without an order permission flag");
        settling();
        System.out.println(checks+" physical berth, single-file obstruction, aligned access and quay collision assertions passed");
    }
    /** A centered load still needs gradual keel alignment before a real lasso may catch. */
    private static void settling(){
        for(float angle:new float[]{-2.25f,2.25f}){
            Game g=new Game();g.challengeId=PhysicalBerths.SHARED;g.reset();
            for(Feeder f:g.feeders){f.phase=Feeder.IDLE;f.load=null;}
            g.tug.x=PhysicalBerths.CENTER;g.tug.y=540;
            for(int i=0;i<90;i++)g.step(); // Camera follows the tug before the screen-space press.
            Body b=barge(90,2,PhysicalBerths.CENTER,480);b.angle=angle;g.cargo[0]=b;
            check(!g.canTie(b),"centered but angled full hull cannot tie before alignment");
            ReplayInput.down(g,b.x,b.y);
            check(g.heldCargo()==b,"real projected cargo press selects the settling fixture");
            for(int i=0;i<600 && !g.canTie(b);i++){
                float before=b.angle;g.step();
                check(Math.abs(Physics.angle(b.angle-before))<.06f,"berth assistance turns through bounded motion instead of snapping the hull");
            }
            check(g.canTie(b) && PhysicalBerths.fits(g.challengeId,b),"holding a centered load settles the whole keel into its narrow berth");
            ReplayInput.up(g,b.x,b.y);ReplayInput.lasso(g,b);
            for(int i=0;i<80 && !b.lineAttached;i++)g.step();
            check(b.lineAttached,"settled narrow-berth load accepts the actual lasso gesture");
        }
    }
    public static void main(String[] args){run();}
}
