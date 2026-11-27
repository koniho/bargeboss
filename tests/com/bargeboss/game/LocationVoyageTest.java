package com.bargeboss.game;

/** Job boundary tests deliberately exercise completion logic without replaying long transit legs. */
public final class LocationVoyageTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){run();}
    public static void run(){
        check(Challenges.LOCATION_COUNT==5 && Challenges.pageCount()==2 && Challenges.pageFor(Challenges.CHICAGO)==1 && Challenges.pageFor(Challenges.BOAT_TAG)==1,"five locations (four harbors and the tag circuit) page three cards at a time");
        check(Challenges.menuId(0)==0 && Challenges.menuId(1)==4 && Challenges.menuId(2)==5 && Challenges.menuId(3)==Challenges.CHICAGO && Challenges.menuId(4)==Challenges.BOAT_TAG && Challenges.menuId(5)==-1,"location selector excludes individual jobs");
        for(int id=0;id<4;id++)check(Challenges.stageEntry(id)==0,"historic Calumet selection enters full voyage");
        check(Challenges.stageEntry(6)==4 && Challenges.stageEntry(7)==4,"historic Montrose rescue selection enters full harbor");
        calumet();montrose();
        System.out.println(checks+" continuous location, stable destinations, retained craft and reward assertions passed");
    }
    private static void calumet(){
        Game g=new Game();g.reset();Body tug=g.tug,a=g.cargo[0],b=g.cargo[1];
        check(g.calumetStage() && !g.lockStage() && g.feeders.length==0,"surveyed river uses voyage jobs without a fabricated lock");
        for(Body c:g.cargo)if(c!=null){check(g.inWater(c.x,c.y),"initial cargo spawned on surveyed water");check(c.destination>=0,"cargo has immutable geographic destination");}
        float ax=g.dockX(a),ay=g.dockY(a),x=tug.x,y=tug.y;int clock=g.visualTicks=91;
        a.deliveryRecorded=b.deliveryRecorded=true;g.voyage.step(g);
        check(g.voyage.job==1 && g.mode==Game.PLAY,"first delivery job continues without finish screen");
        check(g.tug==tug && tug.x==x && tug.y==y && g.visualTicks==clock,"job handoff retains tug position and world clock");
        check(g.cargo[0]==a && g.cargo[1]==b && a.active && b.active,"delivered barges stay in the same world");
        check(g.dockX(a)==ax && g.dockY(a)==ay,"earlier barge berth does not remap when the active job changes");
        check(g.career.bestStars(0)==3 && !g.goalComplete(),"first legacy reward credited once, stage remains unfinished");
        Body incoming=g.cargo[2];float oldX=a.x,oldY=a.y;
        incoming.x=g.dockX(incoming);incoming.y=g.dockY(incoming);incoming.awaitingPickup=false;
        a.x=incoming.x;a.y=incoming.y;
        check(g.berthBlocker(incoming.berth,incoming)==a && !g.canTie(incoming),"retained empty physically blocks the shared berth");
        a.x=oldX;a.y=oldY;
        check(g.canTie(incoming),"pulling empty clear reopens shared berth without a letter-order flag");
        Body first=null,second=null;
        for(Body c:g.cargo)if(c!=null && c.voyageJob==1){c.deliveryRecorded=true;if(first==null)first=c;else if(second==null)second=c;}
        g.voyage.step(g);check(g.voyage.job==2 && first!=null && second!=null,"exchange empties become the convoy assignment");
        float[] gate=CalumetRiver.CONVOY_GATE;
        tug.x=gate[0]+gate[2]*160;tug.y=gate[1]+gate[3]*160;
        check(g.voyage.progress(g)==0,"tug alone crossing the bridge cannot finish convoy");
        first.x=gate[0]+gate[2]*100;first.y=gate[1]+gate[3]*100;first.angle=(float)StrictMath.atan2(gate[3],gate[2]);
        second.x=gate[0]+gate[2]*45;second.y=gate[1]+gate[3]*45;second.angle=first.angle;
        check(g.tow.attach(g,first,tug) && g.tow.attach(g,second,first),"both retained empties can be linked into a tow train");
        check(g.voyage.progress(g)==1,"trailing hull must fully clear bridge plane");
        second.x=gate[0]+gate[2]*110;second.y=gate[1]+gate[3]*110;
        g.voyage.step(g);check(g.voyage.job==3 && g.tow.connected(second,tug),"convoy handoff preserves links and activates terminal job");
        int hulls=0;for(Body c:g.cargo)if(c!=null){hulls++;if(c.voyageJob==3)c.deliveryRecorded=true;}
        check(hulls==8,"all eight loaded and empty hulls coexist without slot loss");
        g.voyage.step(g);check(g.goalComplete() && g.career.bestStars(3)==3,"terminal job finishes full river and records final reward");
        int stars=g.career.totalStars();g.voyage.step(g);check(g.career.totalStars()==stars,"completed voyage cannot farm repeated rewards");
    }
    private static void montrose(){
        Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);Body tug=g.tug,first=g.cargo[0],second=g.cargo[1];
        first.deliveryRecorded=second.deliveryRecorded=true;g.delivered=2;g.ticks=500;g.visualTicks=500;float x=tug.x,y=tug.y;g.step();
        check(g.challengeId==Challenges.MONTROSE && g.mode==Game.PLAY,"cruiser rescue continues into yacht tow");
        check(g.tug==tug && Physics.length(tug.x-x,tug.y-y)<1 && g.ticks==501,"Montrose transition preserves tug and simulation clock");
        Body yacht=g.cargo[0];check(yacht.yacht && !yacht.cruiser,"new yacht uses its own actor slot");
        yacht.deliveryRecorded=true;yacht.awaitingPickup=true;yacht.tiedX=yacht.x;yacht.tiedY=yacht.y;yacht.tiedAngle=yacht.angle;
        g.delivered=1;g.step();
        check(g.challengeId==Challenges.SWIMMER_RESCUE && g.mode==Game.PLAY,"yacht continues into swimmer rescue on same scene");
        boolean a=false,b=false,c=false;for(Body boat:g.cargo){a|=boat==first;b|=boat==second;c|=boat==yacht;}
        check(a && b && c && yacht.active,"all delivered watercraft survive later Montrose job");
        check(g.rescue.x[0]!=0 && g.delivered==0,"new job initializes only its swimmer objectives and counter");
        for(int i=0;i<MontroseRescue.PEOPLE;i++)g.rescue.state[i]=4;g.delivered=20;g.step();
        check(g.challengeId==Challenges.WAKE_PATROL && !g.montroseComplete && g.mode==Game.PLAY,"swimmer rescue continues into the new day's jobs");
        a=b=c=false;for(Body boat:g.cargo){a|=boat==first;b|=boat==second;c|=boat==yacht;}
        check(a && b && c,"delivered watercraft stay berthed through the new day");
        for(int job=Challenges.WAKE_PATROL;job<=Challenges.DEEP_FREEZE;job++){check(g.challengeId==job && g.mode==Game.PLAY,"job "+job+" is live before the finale");g.dialogue.close(g);g.delivered=g.quota();g.step();}
        check(g.montroseComplete && g.mode==Game.FINISH,"only final Montrose job reaches finish screen");
        check(g.career.bestStars(4)==3 && g.career.bestStars(6)==3 && g.career.bestStars(7)==3,"each legacy Montrose reward keeps its existing save slot");
    }
}
