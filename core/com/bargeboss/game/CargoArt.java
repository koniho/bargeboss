package com.bargeboss.game;

/** Whole-hold cargo: common 3D guide, waterline datum and registered 64-pose art. */
public final class CargoArt {
    public static final float HOLD_LENGTH=62,HOLD_WIDTH=24,FLOOR=3.05f,RIM=11;
    private CargoArt() {}
    public static boolean atlas(int atlas){return atlas>=SpriteAtlas.STEEL && atlas<=SpriteAtlas.BULK;}
    public static float scale(int atlas){return 1;}
    public static float pivotX(int atlas,int frame){return .5f;}
    public static float pivotY(int atlas,int frame){return .72f;}
    public static float quantity(Body b){return b.deliveryRecorded || b.unloadingTicks>=CargoTransfer.PICK?0:1;}
}
