package com.bargeboss.game;

/** Public drag/tow smoke replay. Compile with core and tests/ReplayInput.java.
 * Whole-job continuity is separately tested; this exercises long physical transit. */
public final class GeographicTransitReview {
    private static void check(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static void drive(Game g,float x,float y){float dx=x-g.tug.x,dy=y-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
        if(!g.pointer)ReplayInput.throttleDown(g);
        ReplayInput.throttle(g,dx/len*46,dy/len*46);g.step();}
    public static void main(String[] args){for(int id:new int[]{0,Challenges.INDIANA}){
        Game g=new Game();g.challengeId=id;g.reset();float[][] route=id==0?CalumetRiver.ROUTE:IndianaHarbor.ROUTE;Body load=g.cargo[0];
        if(id==Challenges.INDIANA){ReplayInput.down(g,load.x,load.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);for(int i=0;i<45;i++)g.step();check(g.tow.connected(load,g.tug),"initial tow catches through actual input");}
        int at=1;for(int tick=0;tick<45000 && at<route.length && g.mode==Game.PLAY;tick++){
            if(Physics.length(route[at][0]-g.tug.x,route[at][1]-g.tug.y)<(id==0?55:160)){at++;continue;}
            drive(g,route[at][0],route[at][1]);
            if(id==Challenges.INDIANA)check(g.tow.connected(load,g.tug),"loaded tow remains connected at route leg "+at);
        }
        check(at==route.length && g.hull==100,"complete geographic transit without bank damage");
        if(g.pointer)ReplayInput.up(g,g.pointerX,g.pointerY);
        if(id==Challenges.INDIANA){
            for(int tick=0;tick<6000 && !g.canTie(load);tick++)drive(g,IndianaHarbor.DOCK_X-170,IndianaHarbor.DOCK_Y);
            if(g.pointer)ReplayInput.up(g,g.pointerX,g.pointerY);
            check(g.canTie(load),"full voyage reaches lasso area");ReplayInput.releaseTow(g);ReplayInput.lasso(g,load);
            for(int i=0;i<400;i++)g.step();
            check(load.deliveryRecorded && g.goalComplete() && g.hull==100,"Indiana completes tow, release, shore throw and unload through public controls");
        }
        System.out.println(Challenges.locationTitle(id)+": route "+at+"/"+route.length+", ticks "+g.ticks+", hull "+g.hull+", delivered "+g.delivered);
    }}
}
