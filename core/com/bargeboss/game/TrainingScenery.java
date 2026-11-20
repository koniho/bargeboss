package com.bargeboss.game;

/** Finite tutorial scenery shared by terrain, camera coverage and the overhead chart. */
public final class TrainingScenery {
    public static final float MARGIN=256;
    private static final float[] WATER=PolygonWater.bounds(TrainingChannel.WATER_BOUNDARY);
    public static final int MIN_COL=(int)StrictMath.floor((WATER[0]-MARGIN)/64),
            MAX_COL=(int)StrictMath.ceil((WATER[2]+MARGIN)/64),
            MIN_ROW=(int)StrictMath.floor((WATER[1]-MARGIN)/64),
            MAX_ROW=(int)StrictMath.ceil((WATER[3]+MARGIN)/64);
    public static final float MIN_X=MIN_COL*64,MAX_X=MAX_COL*64,MIN_Y=MIN_ROW*64,MAX_Y=MAX_ROW*64;
    private TrainingScenery() {}
    static float[] bounds(){return new float[]{MIN_X,MIN_Y,MAX_X,MAX_Y};}
    /** Keep a single shallow district beside the water, rather than filling the hinterland. */
    static boolean nearWater(float x,float y,float w,float h){
        return !IndustrialRoadView.clearance(x,y,w,h,MARGIN);
    }
    /** Keep the player's hull visible while choosing the nearest legal camera position. */
    static Layout constrain(Layout wanted,float tugX,float tugY){
        return CameraCoverage.follow(wanted,MIN_X,MIN_Y,MAX_X,MAX_Y,GroundArt.LAND_RISE,tugX,tugY);
    }
    static Layout constrain(Layout wanted){
        return CameraCoverage.constrain(wanted,MIN_X,MIN_Y,MAX_X,MAX_Y,GroundArt.LAND_RISE);
    }
}
