package com.bargeboss.game;

/** Reviewed crossing inventory; independent of OSM way segmentation. North is world +X. */
final class LocationBridges {
    private LocationBridges(){}
    private static final class Calumet {static final LandmarkBridge[] ALL={
        cal("cn-710","CN / FORMER EJ&E 710",41.72855,-87.53935,-.428f,LandmarkBridge.LIFT,0,30),
        cal("ewing-92","92ND / EWING",41.72751,-87.54150,-.947f,LandmarkBridge.BASCULE,1,64),
        cal("95th","95TH STREET",41.72286,-87.54310,1.57f,LandmarkBridge.BASCULE,2,62),
        cal("rail-north","PITTSBURGH FORT WAYNE / NORTH",41.71989,-87.54294,-.80f,LandmarkBridge.LIFT,5,29),
        cal("rail-south","LAKE SHORE / SOUTH",41.71946,-87.54298,-.80f,LandmarkBridge.LIFT,5,29),
        cal("skyway","CHICAGO SKYWAY",41.71862,-87.54280,-.785f,LandmarkBridge.SKYWAY,0,82),
        cal("100th","100TH STREET",41.71383,-87.54249,1.57f,LandmarkBridge.BASCULE,3,64),
        cal("106th","106TH STREET",41.70276,-87.54594,1.57f,LandmarkBridge.BASCULE,4,64),
        cal("cwi","CHICAGO AND WESTERN INDIANA",41.66910,-87.55980,0,LandmarkBridge.LIFT,5,30),
        cal("torrence","TORRENCE AVENUE",41.66932,-87.55928,0,LandmarkBridge.LIFT,6,68)
    };}
    private static final class Indiana {static final LandmarkBridge[] ALL={
        ind("cliffs","CLEVELAND CLIFFS RAIL",41.66074,-87.45144,-.82f,LandmarkBridge.ROLLING,0,26),
        ind("eje-main","EJ&E MAIN CHANNEL",41.66043,-87.45181,-.82f,LandmarkBridge.ROLLING,0,30),
        ind("csx","CSX RAIL",41.66025,-87.45211,-.82f,LandmarkBridge.ROLLING,1,30),
        ind("ns","NORFOLK SOUTHERN / AMTRAK",41.65993,-87.45249,-.82f,LandmarkBridge.ROLLING,1,36),
        ind("dickey","DICKEY ROAD",41.65518,-87.45919,-.82f,LandmarkBridge.BASCULE,2,62),
        ind("cline","CLINE AVENUE",41.651236,-87.46500,-.758f,LandmarkBridge.FIXED,0,70),
        ind("whiting","EJ&E 631 / WHITING BRANCH",41.64860,-87.46847,-.79f,LandmarkBridge.ROLLING,1,28),
        ind("indianapolis","INDIANAPOLIS BOULEVARD",41.64642,-87.480888,0,LandmarkBridge.BASCULE,2,68),
        ind("columbus","COLUMBUS DRIVE",41.63931,-87.47128,1.57f,LandmarkBridge.BASCULE,2,64),
        ind("east-works","EAST WORKS CROSSING",41.65637,-87.43309,1.20f,LandmarkBridge.BASCULE,2,50)
    };}
    private static LandmarkBridge cal(String id,String name,double lat,double lon,float yaw,int type,int style,float width){return new LandmarkBridge(id,name,CalumetRiver.north(lat),CalumetRiver.east(lon),yaw,type,style,width,false);}
    private static LandmarkBridge ind(String id,String name,double lat,double lon,float yaw,int type,int style,float width){return new LandmarkBridge(id,name,IndianaHarbor.north(lat),IndianaHarbor.east(lon),yaw,type,style,width,true);}
    static LandmarkBridge[] calumet(){return Calumet.ALL;}
    static LandmarkBridge[] indiana(){return Indiana.ALL;}
    static LandmarkBridge[] forGame(Game g){return g.calumetStage()?calumet():g.indianaStage()?indiana():new LandmarkBridge[0];}
    static void shadows(Game g,Painter p){Painter world=new RiverPainter(p,g.layout.camera);for(int i=0;i<g.passages.bridges.length;i++)g.passages.bridges[i].shadow(g,world,g.passages.amount(i));}
    static void prepare(LandmarkBridge[] bridges){for(LandmarkBridge b:bridges)b.prepare();}
}
