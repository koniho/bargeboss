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
    /** Movable crossings from Wolf Point to the lake; OSM bridge-way midpoints and deck axes. */
    private static final class Chicago {static final LandmarkBridge[] ALL={
        chi("grand","GRAND AVENUE",41.89142,-87.64112,-1.67f,LandmarkBridge.BASCULE,2,48),
        chi("kinzie","KINZIE STREET",41.88908,-87.63949,1.46f,LandmarkBridge.BASCULE,3,40),
        chi("kinzie-rail","KINZIE STREET RAIL (RAISED)",41.88851,-87.63921,1.58f,LandmarkBridge.BASCULE,5,26),
        chi("lake","LAKE STREET / L",41.88573,-87.63772,1.57f,LandmarkBridge.BASCULE,1,52),
        chi("randolph","RANDOLPH STREET",41.88446,-87.63798,-1.59f,LandmarkBridge.BASCULE,2,56),
        chi("washington","WASHINGTON STREET",41.88320,-87.63809,1.54f,LandmarkBridge.BASCULE,3,56),
        chi("madison","MADISON STREET",41.88190,-87.63823,-1.59f,LandmarkBridge.BASCULE,2,60),
        chi("franklin","FRANKLIN-ORLEANS",41.88741,-87.63584,-.59f,LandmarkBridge.BASCULE,2,56),
        chi("wells","WELLS STREET / L",41.88755,-87.63400,0,LandmarkBridge.BASCULE,1,56),
        chi("lasalle","LASALLE STREET",41.88752,-87.63251,0,LandmarkBridge.BASCULE,3,62),
        chi("clark","CLARK STREET",41.88751,-87.63103,0,LandmarkBridge.BASCULE,2,58),
        chi("dearborn","DEARBORN STREET",41.88753,-87.62954,0,LandmarkBridge.BASCULE,4,54),
        chi("state","STATE STREET",41.88757,-87.62801,0,LandmarkBridge.BASCULE,3,62),
        chi("wabash","WABASH AVENUE",41.88794,-87.62689,-.43f,LandmarkBridge.BASCULE,2,56),
        chi("michigan","DUSABLE / MICHIGAN AVENUE",41.88888,-87.62438,.17f,LandmarkBridge.BASCULE,4,78),
        chi("columbus","COLUMBUS DRIVE",41.88859,-87.62058,.13f,LandmarkBridge.BASCULE,2,70),
        chi("lsd","LAKE SHORE DRIVE LINK",41.88842,-87.61410,0,LandmarkBridge.BASCULE,1,96),
        chi("lsd-ogden","LAKE SHORE DRIVE / OGDEN SLIP",41.89032,-87.61410,0,LandmarkBridge.BASCULE,1,96)
    };}
    private static LandmarkBridge chi(String id,String name,double lat,double lon,float yaw,int type,int style,float width){return new LandmarkBridge(id,name,ChicagoRiver.north(lat),ChicagoRiver.east(lon),yaw,type,style,width,LandmarkBridge.CHICAGO);}
    static LandmarkBridge[] chicago(){return Chicago.ALL;}
    private static LandmarkBridge cal(String id,String name,double lat,double lon,float yaw,int type,int style,float width){return new LandmarkBridge(id,name,CalumetRiver.north(lat),CalumetRiver.east(lon),yaw,type,style,width,LandmarkBridge.CALUMET);}
    private static LandmarkBridge ind(String id,String name,double lat,double lon,float yaw,int type,int style,float width){return new LandmarkBridge(id,name,IndianaHarbor.north(lat),IndianaHarbor.east(lon),yaw,type,style,width,LandmarkBridge.INDIANA);}
    static LandmarkBridge[] calumet(){return Calumet.ALL;}
    static LandmarkBridge[] indiana(){return Indiana.ALL;}
    static LandmarkBridge[] forGame(Game g){return g.tutorial!=null?TutorialCanalBridge.bridges():g.calumetStage()?calumet():g.indianaStage()?indiana():g.chicagoStage()?chicago():new LandmarkBridge[0];}
    static void shadows(Game g,Painter p){Painter world=new RiverPainter(p,g.layout.camera);for(int i=0;i<g.passages.bridges.length;i++)g.passages.bridges[i].shadow(g,world,g.passages.amount(i));}
    static void prepare(LandmarkBridge[] bridges){for(LandmarkBridge b:bridges)b.prepare();}
}
