package com.bargeboss.game;

/** Measured warehouse anchors; quay cells remain review candidates, not level geometry. */
public final class StaticHarborArt {
    public static final int WAREHOUSE=2;
    public static final float WORLD_SIZE=144, FOOTPRINT_HALF=44;
    private static final float CELL=724;
    // Center of the ground foundation inferred from its three visible bottom corners.
    private static final float PX=359/CELL,PY=518/CELL;
    private static final String[] LIGHT_IDS={"window.front.left","window.front.middle","window.front.right",
            "window.side.near.left","window.side.near.right","window.side.far.left","window.side.far.right"};
    private static final float[] LIGHT_X={184,231,283,442,477,541,575};
    private static final float[] LIGHT_Y={347,368,389,463,444,411,392};
    private StaticHarborArt() {}
    public static float pivotX(int frame) { return frame==WAREHOUSE?PX:.5f; }
    public static float pivotY(int frame) { return frame==WAREHOUSE?PY:.72f; }
    public static int lightCount(int frame) { return frame==WAREHOUSE?LIGHT_IDS.length:0; }
    public static String lightId(int index) { return LIGHT_IDS[index]; }
    public static float lightX(int index) { return LIGHT_X[index]/CELL; }
    public static float lightY(int index) { return LIGHT_Y[index]/CELL; }
    public static float lightRadius(int index) { return 13/CELL; }
    public static int lightColor(int index) { return 0xffffbd50; }
    /** The roof pipe is a closed return bend; no visible exhaust opening was found. */
    public static int smokeCount(int frame) { return 0; }
    public static void lights(Painter p,float x,float y,float size,float alpha,int ticks) {
        for(int i=0;i<LIGHT_IDS.length;i++) {
            float sx=x+(lightX(i)-PX)*size,sy=y+(lightY(i)-PY)*size;
            SceneryEffects.glow(p,sx,sy,lightRadius(i)*size,lightColor(i),alpha,ticks,i*.79f+x*.002f);
        }
    }
}
