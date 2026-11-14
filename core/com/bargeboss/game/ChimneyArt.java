package com.bargeboss.game;

/** Two prerendered inland stacks; source-cell points were inspected on the finished artwork. */
public final class ChimneyArt {
    public static final float WORLD_SIZE=172,FOOTPRINT_HALF=27;
    private static final float CELL=887;
    private static final float[] PX={444/CELL,444/CELL},PY={794/CELL,797/CELL};
    private static final float[] SX={443/CELL,444/CELL},SY={67/CELL,293/CELL};
    private static final float[] LX={414/CELL,353/CELL},LY={424/CELL,663/CELL};
    private ChimneyArt() {}
    public static float pivotX(int frame) { return PX[frame]; }
    public static float pivotY(int frame) { return PY[frame]; }
    public static String outletId(int frame) { return frame==0?"stack.steel.top":"stack.brick.top"; }
    public static String lightId(int frame) { return frame==0?"lamp.steel.maintenance":"lamp.brick.inspection"; }
    public static float outletX(int frame) { return SX[frame]; }
    public static float outletY(int frame) { return SY[frame]; }
    public static float lightX(int frame) { return LX[frame]; }
    public static float lightY(int frame) { return LY[frame]; }
    public static void draw(Game g,Painter p,int frame,float wx,float wy) {
        RiverCamera c=g.layout.camera;float size=WORLD_SIZE*c.scale;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy),left=x-PX[frame]*size,top=y-PY[frame]*size;
        if(left+size<0 || left>g.layout.width || top+size<0 || top>g.layout.height) return;
        float outletX=x+(SX[frame]-PX[frame])*size,outletY=y+(SY[frame]-PY[frame])*size;
        // Include the plume in the fading envelope, not only the solid stack.
        float minX=Math.min(x-32*c.scale,outletX-60*c.scale),minY=outletY-92*c.scale;
        float maxX=Math.max(x+32*c.scale,outletX+20*c.scale);
        float alpha=SceneryView.opacity(g,minX,minY,maxX-minX,y+12*c.scale-minY);
        LightingView.shadow(p,x,y,size*.28f,size*.07f,alpha);
        p.sprite(SpriteAtlas.CHIMNEYS,frame,x,y,size,size,0,alpha);
        SceneryEffects.glow(p,x+(LX[frame]-PX[frame])*size,y+(LY[frame]-PY[frame])*size,
                size*.016f,0xffffb34f,alpha,g.visualTicks,frame*1.73f+wx*.011f+wy*.007f);
        SceneryEffects.smoke(p,outletX,outletY,(frame==0?3.6f:5.1f)*c.scale,c.scale,alpha,
                g.visualTicks,frame*2.17f+wx*.003f+wy*.005f);
    }
}
