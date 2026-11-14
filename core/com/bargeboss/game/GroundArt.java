package com.bargeboss.game;

/** Registered texture and one shared deck elevation for the industrial practice yard. */
public final class GroundArt {
    public static final float LAND_RISE=18,TILE=64;
    public static final float PIVOT_X=.499601275f,PIVOT_Y=.499202549f,SIZE=121.389435f;
    public static final int BASE=0xff19222d;
    private GroundArt() {}
    public static Painter raisedCanvas(Game g,Painter canvas) {
        return ScreenView.offset(canvas,0,-LAND_RISE*g.layout.camera.scale);
    }
    public static void tile(Game g,Painter canvas,float x,float y) {
        RiverCamera c=g.layout.camera;float size=SIZE*c.scale;
        canvas.sprite(SpriteAtlas.GROUND,0,c.screenX(x,y),c.screenY(x,y)-LAND_RISE*c.scale,size,size,0,.48f);
    }
}
