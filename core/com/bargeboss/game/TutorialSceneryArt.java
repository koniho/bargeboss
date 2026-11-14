package com.bargeboss.game;

/** Isometric practice-yard props with pixel-inspected ground and window anchors. */
public final class TutorialSceneryArt {
    public static final int CRATES=0,PIPES=1,TANKS=2,SHED=3;
    private static final float CELL=627;
    private static final float[] PX={323/CELL,305/CELL,329/CELL,301/CELL};
    private static final float[] PY={440/CELL,451/CELL,394/CELL,410/CELL};
    private static final float[] SIZE={128,152,152,180},HALF={30,44,44,44};
    private static final float[] LX={186/CELL,214/CELL,262/CELL,286/CELL};
    private static final float[] LY={288/CELL,297/CELL,315/CELL,323/CELL};
    private static final String[] LIGHT_IDS={"window.left.pane.left","window.left.pane.right","window.right.pane.left","window.right.pane.right"};
    private TutorialSceneryArt() {}
    public static float pivotX(int frame) { return PX[frame]; }
    public static float pivotY(int frame) { return PY[frame]; }
    public static float size(int frame) { return SIZE[frame]; }
    public static float footprintHalf(int frame) { return HALF[frame]; }
    public static int lightCount(int frame) { return frame==SHED?4:0; }
    public static String lightId(int index) { return LIGHT_IDS[index]; }
    public static float lightX(int index) { return LX[index]; }
    public static float lightY(int index) { return LY[index]; }
    /** All tank plumbing is closed; stored pipe openings are cargo, not exhaust. */
    public static int smokeCount(int frame) { return 0; }
    public static int fromLegacy(int cell) { return cell==3?PIPES:cell==4 || cell==6?TANKS:cell==5?SHED:CRATES; }
    public static void draw(Game g,Painter p,int frame,float wx,float wy) {
        RiverCamera c=g.layout.camera;float size=SIZE[frame]*c.scale;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy),left=x-PX[frame]*size,top=y-PY[frame]*size;
        if(left+size<0 || left>g.layout.width || top+size<0 || top>g.layout.height) return;
        float alpha=frame==SHED?SceneryOcclusion.buildingOpacity(g,p,SpriteAtlas.TUTORIAL_SCENERY,frame,wx,wy,0,SIZE[frame]):1;
        p.sprite(SpriteAtlas.TUTORIAL_SCENERY,frame,x,y,size,size,0,alpha);
        if(frame==SHED) {
            for(int i=0;i<4;i++) {
                float sx=x+(LX[i]-PX[frame])*size,sy=y+(LY[i]-PY[frame])*size;
                SceneryEffects.glow(p,sx,sy,.012f*size,0xffffbd50,alpha,g.visualTicks,i*.89f+wx*.013f+wy*.007f);
            }
        }
    }
}
