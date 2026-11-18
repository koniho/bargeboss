package com.bargeboss.game;

/** Surveyed lake entrance to Turning Basin No.5. +X north, +Y east; one unit is .625 m.
 * Source geometry and deliberate extent limits: docs/levels/calumet/survey.json. */
public final class CalumetRiver {
    public static final float[] COAST=CalumetSurveyData.COAST;
    public static final float[][] WATER=PolygonWater.triangles(COAST),ROUTE=CalumetSurveyData.ROUTE;
    public static final float[][] JOB_SITES=CalumetSurveyData.JOB_SITES;
    public static final float[] CONVOY_GATE=CalumetSurveyData.CONVOY_GATE;
    public static final float MIN_X=0,MAX_X=north(41.741),MIN_Y=east(-87.579),MAX_Y=east(-87.518);
    public static final float START_X=ROUTE[0][0],START_Y=ROUTE[0][1],START_ANGLE=(float)StrictMath.atan2(ROUTE[1][1]-START_Y,ROUTE[1][0]-START_X);
    public static final float[][] MARKERS=markers();
    private CalumetRiver(){}
    public static float north(double lat){return (float)((lat-41.659)*111195*1.6);}
    public static float east(double lon){return (float)((lon+87.58)*111195*StrictMath.cos(41.70*StrictMath.PI/180)*1.6);}
    public static boolean contains(float x,float y){return CalumetWaterGeometry.contains(x,y);}
    public static void paintWater(Painter p,float[] points,int color){CalumetWaterGeometry.paintWater(p,points,color);}
    public static float collide(Body b,CollisionSplashes splashes){return PolygonWater.collide(b,COAST,true,splashes);}
    public static boolean clear(float x,float y,float xx,float yy,float pad){
        int steps=Math.max(1,(int)StrictMath.ceil(Physics.length(xx-x,yy-y)/5));
        for(int i=0;i<=steps;i++){float t=i/(float)steps,a=x+(xx-x)*t,b=y+(yy-y)*t;
            if(!contains(a,b)||!contains(a-pad,b)||!contains(a+pad,b)||!contains(a,b-pad)||!contains(a,b+pad))return false;}
        return true;
    }
    private static float[][] markers(){float direction=(float)StrictMath.atan2(ROUTE[3][1]-ROUTE[2][1],ROUTE[3][0]-ROUTE[2][0]);
        return new float[][]{{north(41.73375),east(-87.52875),direction,1},{north(41.73240),east(-87.5290),direction,0}};
    }
}
