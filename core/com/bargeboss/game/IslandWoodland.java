package com.bargeboss.game;

/** Painted white pines and cedar; six frame-sized textures with registered root contacts. */
public final class IslandWoodland {
    private static final float[] X={.49f,.37f,.53f,.5f,.5f,.45f};
    private static final float[] Y={.93f,.93f,.93f,.91f,.90f,.91f};
    private IslandWoodland(){}
    public static float pivotX(int frame){return 4f/256+248f/256*X[frame];}
    public static float pivotY(int frame){return 4f/256+248f/256*Y[frame];}
    static void draw(Painter p,RiverCamera c,int frame,float x,float y,float z,float size){
        p.sprite(SpriteAtlas.ISLAND_WOODLAND,frame,c.screenX(x,y),c.screenY(x,y)-z*c.scale,size*c.scale,size*c.scale,0,1);
    }
}
