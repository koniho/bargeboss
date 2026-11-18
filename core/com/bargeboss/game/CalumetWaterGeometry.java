package com.bargeboss.game;

/** Lazy shared river geometry. No native map interpretation or bitmap-sized collision masks. */
final class CalumetWaterGeometry {
    private static final class Holder {static final HarborPolygonGeometry DATA=new HarborPolygonGeometry(CalumetRiver.COAST,CalumetRiver.WATER,CalumetRiver.MAX_X,CalumetRiver.MAX_Y);}
    private CalumetWaterGeometry(){}
    static HarborPolygonGeometry geometry(){return Holder.DATA;}
    static void prepare(){geometry();}
    static boolean contains(float x,float y){return geometry().contains(x,y); }
    static void paintWater(Painter p,float[] points,int color){geometry().paintWater(p,points,color);}
    static float[][] dry(float l,float t,float r,float b){return geometry().dry(l,t,r,b);}
    static float[] tops(RiverCamera c,float l,float t,float r,float b,float depth){return geometry().tops(c,l,t,r,b,depth);}
    static float[] shore(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){return geometry().shore(c,l,t,r,b,depth,union);}
}
