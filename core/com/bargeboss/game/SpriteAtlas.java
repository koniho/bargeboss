package com.bargeboss.game;

/** PNG names and registration points shared by all native sprite adapters. */
public final class SpriteAtlas {
    public static final int TUG=0, BARGE=1, STEEL=2, CONTAINER=3, BULK=4;
    private static final String[] FILES={"tug.png","barge.png","steel.png","container.png","bulk.png"};
    private static final float[] TUG_X={.50f,.50f,.51f,.50f,.50f,.50f,.51f,.50f};
    private static final float[] TUG_Y={.69f,.67f,.72f,.68f,.69f,.68f,.73f,.68f};
    private SpriteAtlas() {}
    public static String file(int atlas) { return FILES[atlas]; }
    public static float pivotX(int atlas,int frame) { return atlas==TUG?TUG_X[frame]:.5f; }
    public static float pivotY(int atlas,int frame) {
        if(atlas==TUG) return TUG_Y[frame];
        if(atlas==BARGE) return .56f;
        // Register the load near its deck footprint, leaving room for its height.
        return .62f;
    }
    public static int direction(float angle) {
        int value=Math.round(angle/(float)(Math.PI/4)); return (value%8+8)%8;
    }
    public static float turn(float angle,int frame) { return Physics.angle(angle-frame*(float)(Math.PI/4)); }
}
