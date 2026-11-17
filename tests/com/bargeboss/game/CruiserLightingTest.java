package com.bargeboss.game;
/** Rescue-only light transitions, compact rigid anchors and all-heading visibility. */
public final class CruiserLightingTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void run(){
        Game g=MontroseRescueTest.game(Challenges.RAFT_RESCUE);Body b=g.cargo[0];
        for(int heading=0;heading<32;heading++){
            b.angle=heading*(float)StrictMath.PI/16;
            for(int f=0;f<6;f++)check(VesselNavigationLights.energy(g,b,false,f)==(f==VesselNavigationLights.ANCHOR?1:0),"Waiting cruiser shows only all-round white at every heading");
            for(int f:new int[]{0,1,2,5}){
                float x=VesselNavigationLights.sourceX(g,b,f)-b.x,y=VesselNavigationLights.sourceY(g,b,f)-b.y;
                check(Physics.length(x,y)<23,"Fixture lies on compact cruiser rather than yacht-length offset");
                float z=VesselNavigationLights.height(b,f);check(z>=4.5f && z<=12,"Fixture follows authored deck/roof height");
            }
        }
        b.angle=-(float)StrictMath.PI/2;g.rescue.rafted=false;
        check(g.tow.attach(g,b,g.tug),"Tow can attach for underway transition");
        for(int heading=0;heading<32;heading++){
            b.angle=heading*(float)StrictMath.PI/16;
            for(int f=0;f<6;f++)check(VesselNavigationLights.energy(g,b,false,f)==(f<3?VesselNavigationLights.visibility(b.angle,f):0),"Tow lights follow hull heading and extinguish anchor lamp");
        }
        for(int heading=0;heading<32;heading++){float angle=heading*(float)StrictMath.PI/16;check(VesselShoreLight.sector(angle,VesselNavigationLights.ANCHOR,13,-7)==1,"Anchor illuminates receivers all round");}
        g.tow.release(g);check(VesselNavigationLights.energy(g,b,false,VesselNavigationLights.ANCHOR)==1,"Detached waiting cruiser restores anchor light");
        b.deliveryRecorded=true;for(int f=0;f<6;f++)check(!VesselNavigationLights.emitting(g,b,f),"Delivered cruiser lamps switch off");
        check(VesselNavigationLights.color(VesselNavigationLights.ANCHOR)==VesselNavigationLights.WHITE,"Anchor is white");
        System.out.println(checks+" cruiser anchor/running-light transition and rigid-anchor assertions passed");
    }
    public static void main(String[] args){run();VesselNavigationLightsTest.run();}
}
