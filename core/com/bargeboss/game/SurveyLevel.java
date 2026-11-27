package com.bargeboss.game;

/** One surveyed geographic harbor: a single polygon water geometry plus the data the shared
 * renderer needs. Water, shoreline, masks, ground, landmarks, bridges, lights and the overview
 * chart dispatch through {@link Game#level()}, so a new geographic level is one subclass (plus
 * its mission rules) and inherits every shared renderer optimization. Do not add stage-name
 * checks to shared code; extend this class instead (enforced by LevelContractTest).
 *
 * Each level lives in its own lazy holder: entering one stage never initializes another
 * stage's survey classes. */
abstract class SurveyLevel {
    /** Stable small index for per-level caches and layer keys. Append new levels; never reuse. */
    static final int CALUMET=0,INDIANA=1,CHICAGO=2,COUNT=3;
    final int id;
    private ShorelineView.Segment[] shoreline;private HarborPolygonGeometry.Index shorelineIndex;
    private OverviewMap.Chart chart;private NavigationLights.Entrance[] entrances;private SurveyGround ground;
    SurveyLevel(int id){this.id=id;}

    private static final class CalumetHolder {static final SurveyLevel LEVEL=new Calumet();}
    private static final class IndianaHolder {static final SurveyLevel LEVEL=new Indiana();}
    private static final class ChicagoHolder {static final SurveyLevel LEVEL=new Chicago();}
    static SurveyLevel calumet(){return CalumetHolder.LEVEL;}
    static SurveyLevel indiana(){return IndianaHolder.LEVEL;}
    static SurveyLevel chicago(){return ChicagoHolder.LEVEL;}

    /** Lower-case key; profiling fixtures for the level are named "<key>-...". */
    abstract String key();
    // Geography.
    abstract HarborPolygonGeometry geometry();
    abstract float[] coast();
    abstract float minX();abstract float minY();abstract float maxX();abstract float maxY();
    /** Entrance marker pairs {x,y,heading,kind}. */
    abstract float[][] markers();
    abstract LandmarkBridge[] bridges();
    /** Tow-line throw clearance along the whole line (mission obstacles included). */
    abstract boolean clear(Game g,float x,float y,float xx,float yy);
    /** Land cover, area fills and streets for the shared ground cache. */
    abstract SurveyGround createGround();

    final boolean contains(float x,float y){return geometry().contains(x,y);}
    final void paintWater(Painter p,float[] polygon,int color){geometry().paintWater(p,polygon,color);}
    final float collide(Body b,CollisionSplashes splashes){return PolygonWater.collide(b,coast(),true,splashes);}
    final boolean inBounds(float x,float y){return x>=minX() && x<=maxX() && y>=minY() && y<=maxY();}

    /** Wave exposure zones (see WaveZones): open lake water rough, rivers and inner harbours calmer.
     * Every level defines its own; the default is uniformly sheltered water. */
    float[][] waveZones(){return new float[0][];}
    float waveCalm(){return WaveZones.CALM;}
    private float[][] zones;
    final float waveExposure(float x,float y){if(zones==null)zones=waveZones();return WaveZones.exposure(zones,waveCalm(),x,y);}

    // Level-owned moving obstacles (for example Chicago's harbor lock). Defaults: none.
    void integrate(Game g,Body b){Physics.integrate(b);}
    float obstacles(Game g,Body b,CollisionSplashes splashes){return 0;}

    // Rendering.
    int background(LightingProfile light){return light==LightingProfile.DUSK?0xff151b25:light.water;}
    /** Dock line crews stand at this level's berths. */
    boolean dockWorkers(){return true;}
    int landmarkCount(Game g){return 0;}
    float landmarkDepth(Game g,int i){return 0;}
    void drawLandmark(Game g,Painter p,int i){}
    /** Screen rectangle (l,t,r,b) of a landmark's drawing; every landmark must provide one. */
    boolean landmarkBox(Game g,int i,float[] out){return false;}
    /** Landmark ids in painter order; levels with presorted landmarks may override. */
    int[] orderedLandmarks(Game g){int n=landmarkCount(g);int[] ids=new int[n];for(int i=0;i<n;i++)ids[i]=i;return ids;}
    void lights(Game g,SceneryLighting out){}
    /** Level effects on the water surface (reflections), beneath shore walls. */
    void water(Game g,Painter world){}
    /** Level effects on the ground layer (fire wash, smoke), beneath vessels and buildings. */
    void ground(Game g,Painter p){}
    /** Stage-entry preparation: geometry, ground, rigid models and bridges, never in a draw. */
    void prepare(Game g){geometry();ground().prepare(minX(),minY(),maxX(),maxY(),g.tug.x,g.tug.y);shoreline();LocationBridges.prepare(bridges());}

    final synchronized SurveyGround ground(){if(ground==null)ground=createGround();return ground;}
    final synchronized ShorelineView.Segment[] shoreline(){if(shoreline==null){shoreline=ShorelineView.polygon(this);shorelineIndex=ShorelineView.index(shoreline);}return shoreline;}
    final HarborPolygonGeometry.Index shorelineIndex(){shoreline();return shorelineIndex;}
    final synchronized OverviewMap.Chart chart(){if(chart==null)chart=new OverviewMap.Chart(this);return chart;}
    final synchronized NavigationLights.Entrance[] entrances(){if(entrances==null)entrances=NavigationLights.fromMarkers(markers());return entrances;}

    /** Shared screen rectangle for a prepared rigid pose; false if it has no projected nodes. */
    static boolean poseBox(Game g,ArticulatedModel.Pose pose,float[] out){
        out[0]=out[1]=Float.MAX_VALUE;out[2]=out[3]=-Float.MAX_VALUE;pose.screenBounds(g.layout.camera,out);return out[0]<=out[2];}

    private static final class Calumet extends SurveyLevel {
        Calumet(){super(CALUMET);}
        String key(){return "calumet";}
        HarborPolygonGeometry geometry(){return CalumetWaterGeometry.geometry();}
        float[] coast(){return CalumetRiver.COAST;}
        float minX(){return CalumetRiver.MIN_X;}float minY(){return CalumetRiver.MIN_Y;}float maxX(){return CalumetRiver.MAX_X;}float maxY(){return CalumetRiver.MAX_Y;}
        float[][] markers(){return CalumetRiver.MARKERS;}
        LandmarkBridge[] bridges(){return LocationBridges.calumet();}
        boolean clear(Game g,float x,float y,float xx,float yy){return CalumetRiver.clear(x,y,xx,yy,3);}
        SurveyGround createGround(){return CalumetRiverTiles.ground();}
        /** Lake Michigan beyond the river mouth; the river is sheltered upstream. */
        float[][] waveZones(){return new float[][]{{13179,6791,-.14f,.99f,700,350,1}};}
    }
    private static final class Indiana extends SurveyLevel {
        Indiana(){super(INDIANA);}
        String key(){return "indiana";}
        HarborPolygonGeometry geometry(){return IndianaHarbor.geometry();}
        float[] coast(){return IndianaHarbor.COAST;}
        float minX(){return IndianaHarbor.MIN_X;}float minY(){return IndianaHarbor.MIN_Y;}float maxX(){return IndianaHarbor.MAX_X;}float maxY(){return IndianaHarbor.MAX_Y;}
        float[][] markers(){return IndianaHarbor.MARKERS;}
        LandmarkBridge[] bridges(){return LocationBridges.indiana();}
        boolean clear(Game g,float x,float y,float xx,float yy){return IndianaHarbor.clear(x,y,xx,yy);}
        SurveyGround createGround(){return IndianaShoreTiles.ground();}
        /** Open lake north of the outer breakwater; the inner harbour behind the ship-canal mouth is moderate. */
        float[][] waveZones(){return new float[][]{{8991,2745,.96f,.28f,900,450,1},{6120,2937,.71f,.71f,700,500,.5f}};}
        int landmarkCount(Game g){return IndianaView.count();}
        float landmarkDepth(Game g,int i){return IndianaView.depth(g,i);}
        void drawLandmark(Game g,Painter p,int i){IndianaView.landmark(g,p,i);}
        boolean landmarkBox(Game g,int i,float[] out){return IndianaView.box(g,i,out);}
        void lights(Game g,SceneryLighting out){IndianaView.lights(out);}
        void prepare(Game g){super.prepare(g);IndianaView.prepare();}
    }
    private static final class Chicago extends SurveyLevel {
        Chicago(){super(CHICAGO);}
        String key(){return "chicago";}
        HarborPolygonGeometry geometry(){return ChicagoRiver.geometry();}
        float[] coast(){return ChicagoRiver.COAST;}
        float minX(){return ChicagoRiver.MIN_X;}float minY(){return ChicagoRiver.MIN_Y;}float maxX(){return ChicagoRiver.MAX_X;}float maxY(){return ChicagoRiver.MAX_Y;}
        float[][] markers(){return ChicagoRiver.MARKERS;}
        LandmarkBridge[] bridges(){return LocationBridges.chicago();}
        boolean clear(Game g,float x,float y,float xx,float yy){return ChicagoRiver.clear(g,x,y,xx,yy);}
        SurveyGround createGround(){return ChicagoView.ground();}
        /** The lake and Navy Pier water east of the harbour entrance; the river west of it is sheltered. */
        float[][] waveZones(){return new float[][]{{160,4097,0,1,900,600,1}};}
        void integrate(Game g,Body b){g.chicago.lock.integrate(b);}
        float obstacles(Game g,Body b,CollisionSplashes splashes){return g.chicago.lock.collide(b,splashes);}
        int background(LightingProfile light){return light==LightingProfile.DUSK?0xff121820:light.water;}
        boolean dockWorkers(){return false;}
        int landmarkCount(Game g){return ChicagoView.COUNT;}
        float landmarkDepth(Game g,int i){return ChicagoView.depth(g,i);}
        void drawLandmark(Game g,Painter p,int i){ChicagoView.landmark(g,p,i);}
        boolean landmarkBox(Game g,int i,float[] out){return ChicagoView.box(g,i,out);}
        int[] orderedLandmarks(Game g){return ChicagoView.ordered(g);}
        void lights(Game g,SceneryLighting out){ChicagoView.lights(g,out);}
        void water(Game g,Painter world){g.chicago.fire.reflection(g,world);}
        void ground(Game g,Painter p){g.chicago.fire.ground(g,p);}
        void prepare(Game g){super.prepare(g);ChicagoView.prepare();}
    }
}
