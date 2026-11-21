package com.bargeboss.game;
/** Rigid 64-pose hull, registered at the model waterline origin. */
public final class BargeArt {
    public static final float HULL_FRACTION=.78f,HULL_SCALE=1,COAL_SCALE=1;
    private BargeArt(){}
    public static float pivotX(boolean coal,int frame){return .5f;}
    public static float pivotY(boolean coal,int frame){return .72f;}
}
