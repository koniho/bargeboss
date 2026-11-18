package com.bargeboss.game;

/** Exact rig/cargo attachment, dry receivers and deterministic three-second choreography. */
public final class CargoTransferTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static float distance(float x,float y,float xx,float yy){return Physics.length(x-xx,y-yy);}
    public static void run(){
        for(int location=0;location<3;location++){
            Game root=new Game(),g=root;
            if(location==1){g.challengeId=Challenges.INDIANA;g.reset();}
            if(location==2){root.startTutorial();g=root.activeGame();}
            for(int dock=0;dock<g.dockCranes.length;dock++)if(g.calumetStage() || g.berthDisplayEnabled(dock)){
                DockView.Crane c=g.dockCranes[dock];
                for(int kind=0;kind<3;kind++){
                    for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
                    Body b=new Body(1000+kind,kind,c.pickX,c.pickY,g.calumetStage()?g.voyage.heading(dock):g.indianaStage()?(float)StrictMath.atan2(IndianaHarbor.DOCK_NX,-IndianaHarbor.DOCK_NY):(float)Math.PI/2);
                    b.destination=g.calumetStage()?dock:-1;
                    if(!g.calumetStage() && kind!=dock)continue;
                    b.lineAttached=true;b.tiedAngle=b.angle;g.cargo[0]=b;
                    float[] previous=null;int fragments=c.motion.fragmentCount();
                    for(int tick=1;tick<=Game.UNLOAD_TICKS;tick++){
                        b.unloadingTicks=tick;CargoTransfer.Pose s=new CargoTransfer.Pose(g,c,dock);
                        ArticulatedModel.Pose mesh=c.motion.pose(s.phase);
                        float[] eye=mesh.point(c.model.nodeId(MontroseBoatHoist.LIFT_ID),0,0,0);
                        check(distance(eye[0],eye[1],s.tipX,s.tipY)<.003f && Math.abs(eye[2]-s.tipZ)<.003f,"cable remains on exact rigid jib tip");
                        check(s.hookZ<=s.tipZ && s.hookZ>=s.z,"cable hangs below sheave");
                        check(c.motion.fragmentCount()==fragments,"one bounded topology throughout offloading");
                        check(CargoArt.quantity(b)==(tick<CargoTransfer.PICK?1:0),"handoff removes hull load only at pickup");
                        if(s.cargo() && CargoTransfer.attached(tick))check(Math.abs(s.hookZ-s.z-CargoTransfer.SLING)<.001f,"sling/load attachment remains fixed");
                        if(previous!=null)check(distance(previous[0],previous[1],s.x,s.y)<c.reach*.05f && Math.abs(previous[2]-s.z)<4,"no load teleport during lift, travel or landing");
                        previous=new float[]{s.x,s.y,s.z};
                    }
                    check(distance(c.x,c.y,c.landX(),c.landY())>52,"loaded tray clears mast and footing before lowering");
                    float co=(float)StrictMath.cos(b.angle),si=(float)StrictMath.sin(b.angle);
                    for(int x=-30;x<=30;x+=10)for(int y=-12;y<=12;y+=6)
                        check(!g.inWater(c.landX()+co*x-si*y,c.landY()+si*x+co*y),"complete landed cargo stays dry: region="+location+" dock="+dock);
                    b.deliveryRecorded=true;b.x+=400;b.y+=400;
                    CargoTransfer.Pose idle=new CargoTransfer.Pose(g,c,dock);
                    check(idle.phase==0 && idle.x==c.landX() && idle.y==c.landY(),"crane returns idle; delivered load stays ashore as hull leaves");
                    check(CargoArt.quantity(b)==0,"empty barge does not regain cargo");
                }
            }
        }
        System.out.println(checks+" continuous crane, cargo attachment, shore receiver and idle assertions passed");
    }
    public static void main(String[] args){run();}
}
