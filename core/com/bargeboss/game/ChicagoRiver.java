package com.bargeboss.game;

/** Survey-derived Chicago River main stem: Navy Pier and the harbor lock west to Wolf Point.
 * North is world +X (upper right); east is world +Y. Provenance: docs/maps/chicago-river.md. */
public final class ChicagoRiver {
    public static final double ORIGIN_LAT=41.888,ORIGIN_LON=-87.62;
    public static final float[] COAST=ChicagoSurvey.COAST;
    public static final float[][] WATER=ChicagoSurvey.WATER,ROUTE=ChicagoSurvey.ROUTE;
    public static final float MIN_X=north(41.8815),MAX_X=north(41.8945),MIN_Y=east(-87.6430),MAX_Y=east(-87.5940);
    public static final float START_X=ChicagoSurvey.START[0],START_Y=ChicagoSurvey.START[1];
    /** Bow toward the lock approach; the burning wheel stays in the opening view. */
    public static final float START_ANGLE=(float)StrictMath.atan2(ROUTE[1][1]-START_Y,ROUTE[1][0]-START_X);
    /** Fire barge alongside the Engine 2 fireboat pier, bow west (world -Y). */
    public static final float FIRE_DOCK_X=ChicagoSurvey.FIRE_DOCK[0],FIRE_DOCK_Y=ChicagoSurvey.FIRE_DOCK[1],FIRE_DOCK_ANGLE=-(float)Math.PI/2;
    public static final float WHEEL_X=ChicagoSurvey.WHEEL[0],WHEEL_Y=ChicagoSurvey.WHEEL[1],WHEEL_HEIGHT=ChicagoSurvey.WHEEL[2];
    /** Lake-side lock entrance pair, red to starboard when returning upstream (westbound). */
    public static final float[][] MARKERS=markers();
    private static final class Geometry {static final HarborPolygonGeometry DATA=new HarborPolygonGeometry(COAST,WATER,MAX_X,MAX_Y,MIN_X,MIN_Y);}
    private ChicagoRiver(){}
    static HarborPolygonGeometry geometry(){return Geometry.DATA;}
    public static boolean contains(float x,float y){return geometry().contains(x,y);}
    public static void paintWater(Painter p,float[] polygon,int color){geometry().paintWater(p,polygon,color);}
    public static float collide(Body b,CollisionSplashes splashes){return PolygonWater.collide(b,COAST,true,splashes);}
    public static float north(double lat){return (float)((lat-ORIGIN_LAT)*111132*ChicagoSurvey.UNITS_PER_METRE);}
    public static float east(double lon){return (float)((lon-ORIGIN_LON)*111320*StrictMath.cos(ORIGIN_LAT*StrictMath.PI/180)*ChicagoSurvey.UNITS_PER_METRE);}
    private static float[][] markers(){float[][] out=new float[ChicagoSurvey.MARKERS.length][];
        for(int i=0;i<out.length;i++){float[] m=ChicagoSurvey.MARKERS[i];out[i]=new float[]{m[0],m[1],-(float)Math.PI/2,m[2]};}
        return out;
    }
    public static boolean inBounds(float x,float y){return x>=MIN_X && x<=MAX_X && y>=MIN_Y && y<=MAX_Y;}
    /** Tow throws need open water along the whole line; closed lock leaves also block. */
    public static boolean clear(Game g,float x,float y,float xx,float yy){int count=Math.max(1,(int)(Physics.length(xx-x,yy-y)/3));
        for(int i=0;i<=count;i++)if(!contains(x+(xx-x)*i/count,y+(yy-y)*i/count))return false;
        return !g.chicago.lock.blocksLine(x,y,xx,yy);
    }
    public static void setup(Game g){
        ChicagoView.prepare();
        g.tug.x=START_X;g.tug.y=START_Y;g.tug.angle=START_ANGLE;
        g.chicago.setup(g);
        g.layout=Layout.focused(g.layout.width,g.layout.height,START_X,START_Y,Game.TRANSIT_ZOOM,g.layout.width*.5f,g.layout.height*.52f);
    }
}
