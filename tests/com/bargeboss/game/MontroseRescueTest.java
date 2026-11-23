package com.bargeboss.game;

/** Rescue transitions use public gestures; docking fixtures isolate each surveyed slip. */
public final class MontroseRescueTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static Game game(int id){return ReplayInput.montroseJob(id);}
    static void steps(Game g,int count){for(int i=0;i<count;i++)g.step();}
    static void tapWorld(Game g,float x,float y){ReplayInput.down(g,x,y);ReplayInput.up(g,x,y);}
    static void focus(Game g,float x,float y){g.layout=Layout.focused(g.layout.width,g.layout.height,x,y,3.6f,g.layout.width*.5f,g.layout.height*.52f);}
    static Game swimmers(){Game g=game(Challenges.SWIMMER_RESCUE);g.tug.x=660;g.tug.y=640;focus(g,660,590);return g;}
    public static void run(){
        CruiserLightingTest.run();
        Game g=game(Challenges.RAFT_RESCUE);Body a=g.cargo[0],b=g.cargo[1];
        check(a.cruiser && b.cruiser && a.awaitingPickup && b.awaitingPickup,"Two anchored cabin cruisers");
        check(g.mooredFleet.boats.length==MooredBoats.BERTHS.length-2,"Only two Dock C mission slips are reserved");
        check(!g.tow.canAttach(g,a,g.tug),"Raft must be separated before towing");
        tapWorld(g,(a.x+b.x)*.5f,(a.y+b.y)*.5f);check(!g.rescue.rafted,"Public tap releases rafting line");
        ReplayInput.down(g,a.x,a.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);steps(g,60);
        check(g.tow.connected(a,g.tug) && g.rescue.towed[0],"Separated cruiser accepts actual tow throw");
        ReplayInput.releaseTow(g);steps(g,2);check(!a.awaitingPickup,"Tow release leaves free physical hull");
        for(int i=0;i<2;i++){
            Body boat=g.cargo[i];MooredBoats.Berth berth=MontroseRescue.berth(i);
            boat.x=berth.x+35*(float)StrictMath.cos(berth.angle);boat.y=berth.y+35*(float)StrictMath.sin(berth.angle);boat.angle=berth.angle;boat.vx=boat.vy=boat.spin=0;boat.awaitingPickup=false;
            g.tug.x=300;g.tug.y=-400;g.rescue.towed[i]=true;
            steps(g,600);check(boat.deliveryRecorded,"Dock C slot "+i+" holds real hull clear of all docks/NPCs");
        }
        check(g.challengeId==Challenges.MONTROSE && g.career.bestStars(Challenges.RAFT_RESCUE)>0,"Both returned cruisers complete mission");
        g=swimmers();for(int i=0;i<20;i++)check(MontroseHarbor.contains(g.rescue.x[i],g.rescue.y[i]),"Swimmer in open lake");
        ReplayInput.down(g,g.rescue.x[0],g.rescue.y[0]);float zoom=g.layout.camera.zoom;steps(g,10);
        check(g.rescue.aiming==0 && g.layout.camera.zoom==zoom,"Rescue aiming freezes projection");g.cancel();check(g.rescue.aiming==-1,"Cancellation clears rescue aim");
        for(int i=0;i<20;i++){
            // Independent stationary rescue stations isolate every target and throw lifecycle.
            g.tug.x=g.rescue.x[i];g.tug.y=g.rescue.y[i]+80;g.tug.vx=g.tug.vy=g.tug.spin=0;focus(g,g.tug.x,g.tug.y-35);
            tapWorld(g,g.rescue.x[i],g.rescue.y[i]);check(g.rescue.flying==i,"Pointer tap launches buoy for "+i);
            steps(g,48);check(g.rescue.state[i]==1,"Buoy flight secures swimmer "+i);
        }
        check(g.goalComplete() && g.delivered==20 && g.score==2000,"Exactly twenty rescues complete and score once");
        g=swimmers();g.tug.x=g.rescue.x[0];g.tug.y=g.rescue.y[0];g.step();check(g.goalFailed() && g.rescue.failed,"Hull contact causes non-graphic failure");
        g=swimmers();g.tug.vx=30;tapWorld(g,g.rescue.x[0],g.rescue.y[0]);check(g.rescue.flying<0,"Fast vessel cannot throw");
        g=game(Challenges.SWIMMER_RESCUE);g.tug.x=450;g.tug.y=880;steps(g,1);check(!g.rescue.closeCamera,"Wide approach camera");
        g.tug.x=600;g.tug.y=720;steps(g,120);check(g.rescue.closeCamera && g.layout.camera.zoom>3,"Camera eases closer near rescue");
        g=game(Challenges.SWIMMER_RESCUE);for(int i=0;i<4000 && g.mode==Game.PLAY;i++){ReplayInput.work(g);g.step();}
        check(g.goalComplete() && g.hull==100 && !g.rescue.failed,"Continuous public-input swimmer voyage completes safely");
        System.out.println(checks+" rescue input, mission, geometry, camera and safety assertions passed");
    }
    public static void trace(Painter painter){
        Game g=game(Challenges.RAFT_RESCUE);Renderer.draw(g,painter);tapWorld(g,(g.cargo[0].x+g.cargo[1].x)*.5f,g.cargo[0].y);steps(g,30);Renderer.draw(g,painter);
        g=swimmers();Renderer.draw(g,painter);tapWorld(g,g.rescue.x[0],g.rescue.y[0]);steps(g,18);Renderer.draw(g,painter);steps(g,30);Renderer.draw(g,painter);
    }
    public static void main(String[] args){run();}
}
