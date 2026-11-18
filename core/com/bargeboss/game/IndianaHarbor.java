package com.bargeboss.game;

/** Survey-derived Indiana Harbor and its two branches. North is world +X.
 * Geographic provenance, simplification and game boundaries: docs/maps/indiana-harbor.md. */
public final class IndianaHarbor {
    public static final float[] COAST=IndianaSurvey.COAST;
    public static final float[][] WATER=PolygonWater.triangles(COAST),ROUTE=IndianaSurvey.ROUTE;
    public static final float MIN_X=north(41.632),MAX_X=north(41.705),MIN_Y=east(-87.500),MAX_Y=east(-87.400);
    public static final float START_X=ROUTE[0][0],START_Y=ROUTE[0][1],START_ANGLE=(float)StrictMath.atan2(ROUTE[1][1]-START_Y,ROUTE[1][0]-START_X);
    public static final float DOCK_X=ROUTE[ROUTE.length-1][0],DOCK_Y=ROUTE[ROUTE.length-1][1],DOCK_NX=0,DOCK_NY=-1;
    /** Entrance pairs only; green then red, looking from lake into the harbor. */
    public static final float[][] MARKERS=markers();
    private static final class Geometry {static final HarborPolygonGeometry DATA=new HarborPolygonGeometry(COAST,WATER,MAX_X,MAX_Y,MIN_X);}
    private IndianaHarbor(){}
    static HarborPolygonGeometry geometry(){return Geometry.DATA;}
    public static boolean contains(float x,float y){return geometry().contains(x,y);}
    public static void paintWater(Painter p,float[] polygon,int color){geometry().paintWater(p,polygon,color);}
    public static float collide(Body b,CollisionSplashes splashes){return PolygonWater.collide(b,COAST,true,splashes);}
    public static float north(double lat){return (float)((lat-41.64)*222264);}
    public static float east(double lon){return (float)((lon+87.46)*111320*StrictMath.cos(41.66*StrictMath.PI/180)*2);}
    private static float[][] markers(){
        float direction=START_ANGLE;
        return new float[][]{{north(41.6801),east(-87.4419),direction,0},{north(41.6808),east(-87.4451),direction,1},
            {north(41.6672),east(-87.4419),-2.50f,0},{north(41.66787),east(-87.44279),-2.50f,1}};
    }
    public static void setup(Game g){
        IndianaView.prepare();
        g.tug.x=START_X;g.tug.y=START_Y;g.tug.angle=START_ANGLE;
        g.cargo[0]=new Body(g.nextId++,0,START_X-105*(float)StrictMath.cos(START_ANGLE),START_Y-105*(float)StrictMath.sin(START_ANGLE),START_ANGLE);
        g.layout=Layout.focused(g.layout.width,g.layout.height,START_X,START_Y,Game.TRANSIT_ZOOM,g.layout.width*.5f,g.layout.height*.52f);
    }
    public static boolean clear(float x,float y,float xx,float yy){int count=Math.max(1,(int)(Physics.length(xx-x,yy-y)/3));
        for(int i=0;i<=count;i++)if(!contains(x+(xx-x)*i/count,y+(yy-y)*i/count))return false;return true;}
}
