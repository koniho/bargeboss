package com.bargeboss.game;

/** Real shoreline, complete route hulls and screen-space mooring on geographic stages. */
public final class SurveyedLocationTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run(){
        for(int stage:new int[]{0,Challenges.INDIANA}){
            Game g=new Game();g.challengeId=stage;g.reset();
            float[][] route=g.calumetStage()?CalumetRiver.ROUTE:IndianaHarbor.ROUTE;
            check(route.length>12,"route retains geographic bends");
            RiverCamera camera=g.layout.camera;
            check(camera.screenX(100,0)>camera.screenX(0,0) && camera.screenY(100,0)<camera.screenY(0,0),"north projects upper right");
            for(int i=0;i<route.length-1;i++){
                float[] a=route[i],b=route[i+1];float heading=(float)StrictMath.atan2(b[1]-a[1],b[0]-a[0]);
                int steps=Math.max(1,(int)(Physics.length(b[0]-a[0],b[1]-a[1])/25));
                for(int n=0;n<=steps;n++){
                    Body hull=new Body(80,0,a[0]+(b[0]-a[0])*n/steps,a[1]+(b[1]-a[1])*n/steps,heading);
                    float x=hull.x,y=hull.y;check(g.inWater(x,y),"route center is navigable water: "+stage+" leg "+i);
                    if(stage==0)CalumetRiver.collide(hull,null);else IndianaHarbor.collide(hull,null);
                    check(Physics.length(x-hull.x,y-hull.y)<.1f,"loaded hull fits route: "+stage+" leg "+i);
                }
            }
            for(int i=0;i<NavigationLights.count(g);i++)check(g.inWater(NavigationLights.anchorX(g,i),NavigationLights.anchorY(g,i)),"entrance buoy has wet footprint center");
            for(int site=0;site<(stage==0?6:1);site++)for(float width:new float[]{960,1333})moor(stage,site,width);
        }
        System.out.println(checks+" surveyed route, north orientation, marker and real lasso/unload assertions passed");
    }
    private static void moor(int stage,int site,float width){
        Game g=new Game();g.challengeId=stage;g.reset();for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        float[] s=stage==0?CalumetRiver.JOB_SITES[site]:new float[]{IndianaHarbor.DOCK_X,IndianaHarbor.DOCK_Y,0,-1};
        g.voyage.job=site<2?0:site==2?1:3;
        int kind=stage==0?(site==1?1:2):0;
        Body b=new Body(90,kind,s[0]-s[2]*8,s[1]-s[3]*8,(float)StrictMath.atan2(s[2],-s[3]));
        b.destination=site;b.voyageJob=g.voyage.job;g.cargo[0]=b;
        g.tug.x=b.x-s[2]*150;g.tug.y=b.y-s[3]*150;
        g.layout=Layout.focused(width,600,b.x,b.y,1.7f,width*.5f,330);
        check(!g.inWater(s[0]+s[2]*64,s[1]+s[3]*64),"bollard supported by dry shore");
        check(g.canTie(b),"moving arrival can arm at geographic berth");
        b.vx=-s[2]*4;b.vy=-s[3]*4;
        ReplayInput.down(g,b.x,b.y);check(g.ropeDragging,"projected hull press arms lasso");
        ReplayInput.move(g,g.bollardX(b),g.bollardY(b));
        check(g.ropePhase==0,"rope waits for release");
        ReplayInput.up(g,g.bollardX(b),g.bollardY(b));
        for(int i=0;i<Game.ROPE_FLIGHT_TICKS+Mooring.ALIGN_TICKS+Game.UNLOAD_TICKS+45;i++)g.step();
        check(b.deliveryRecorded && b.active && b.lineAttached,"geographic berth retains unloaded hull: "+stage+" site "+site);
        check(g.inWater(b.x,b.y) && g.hull==100,"mooring stays navigable without bank damage");
        int score=g.score;for(int i=0;i<30;i++)g.step();check(g.score==score,"retained empty scores once");
    }
    public static void main(String[] args){run();}
}
