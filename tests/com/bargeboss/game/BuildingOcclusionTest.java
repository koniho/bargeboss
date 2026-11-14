package com.bargeboss.game;

/** Placement gates fading before per-render opaque vessel overlap is considered. */
public final class BuildingOcclusionTest {
    private static int checks;
    private static final int ATLAS=SpriteAtlas.FACTORY,FRAME=0;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static float[] solidPoint() {
        long[] rows=OcclusionMasks.rows(ATLAS)[FRAME];
        for(int y=2;y<62;y++)for(int x=2;x<62;x++) {
            boolean interior=true;
            for(int yy=y-2;yy<=y+2;yy++)for(int xx=x-2;xx<=x+2;xx++)if((rows[yy]&(1L<<xx))==0)interior=false;
            if(interior)return new float[]{(x+.5f)/64,(y+.5f)/64};
        }
        throw new AssertionError("factory fixture needs a verified opaque interior region");
    }
    private static SceneryOcclusion sceneWithBoat(Game g,float wx,float wy,float rise,float size,float offset,float alpha) {
        SceneryOcclusion scene=new SceneryOcclusion(new Parity());RiverCamera c=g.layout.camera;float[] uv=solidPoint();
        float x=c.screenX(wx,wy)+(uv[0]-SpriteAtlas.pivotX(ATLAS,FRAME))*size*c.scale+offset;
        float y=c.screenY(wx,wy)-rise*c.scale+(uv[1]-SpriteAtlas.pivotY(ATLAS,FRAME))*size*c.scale;
        float boat=size/32*c.scale;
        scene.sprite(SpriteAtlas.TUG,0,x,y,boat,boat,0,alpha);return scene;
    }
    private static float alpha(Game g,Painter p,float x,float y,float rise,float size) {
        return SceneryOcclusion.buildingOpacity(g,p,ATLAS,FRAME,x,y,rise,size);
    }
    public static void run() {
        float size=FactoryArt.size(FRAME),rise=GroundArt.LAND_RISE;
        Game g=new Game();g.reset();
        for(Layout camera:new Layout[]{new Layout(960,600),Layout.focused(1333,600,100,450,1.75f,666.5f,300),
                Layout.focused(960,720,-1400,500,1.2f,480,360)}) {
            g.layout=camera;
            float inlandX=1000,inlandY=500,shoreX=170,shoreY=500;
            check(!SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,inlandX,inlandY,rise,size),"inland silhouette is ineligible at every camera position and zoom");
            SceneryOcclusion inland=sceneWithBoat(g,inlandX,inlandY,rise,size,0,1);RiverCamera c=g.layout.camera;
            check(SceneryOcclusion.spriteOpacity(inland,ATLAS,FRAME,c.screenX(inlandX,inlandY),c.screenY(inlandX,inlandY)-rise*c.scale,size*c.scale,size*c.scale)<.3f,
                    "synthetic inland vessel truly overlaps the building's opaque pixels");
            check(alpha(g,inland,inlandX,inlandY,rise,size)==1,"inland buildings stay opaque even with a synthetic overlapping vessel");
            check(SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,shoreX,shoreY,rise,size),"shore silhouette overlaps navigable water regardless of the live camera");
            SceneryOcclusion empty=new SceneryOcclusion(new Parity());
            g.tug.x=shoreX;g.tug.y=shoreY;
            check(alpha(g,empty,shoreX,shoreY,rise,size)==1,"world-position proximity alone cannot fade a building before the vessel is painted");
            check(alpha(g,sceneWithBoat(g,shoreX,shoreY,rise,size,1000,1),shoreX,shoreY,rise,size)==1,
                    "an already painted vessel outside the actual silhouette cannot trigger fading");
            check(alpha(g,sceneWithBoat(g,shoreX,shoreY,rise,size,0,0),shoreX,shoreY,rise,size)==1,
                    "invisible vessel submissions cannot trigger fading");
            SceneryOcclusion overlapping=sceneWithBoat(g,shoreX,shoreY,rise,size,0,1);
            float faded=alpha(g,overlapping,shoreX,shoreY,rise,size);
            check(faded<.3f,"water-overlapping foreground building fades over previously painted opaque vessel pixels");
            check(alpha(g,new RiverPainter(overlapping,c),shoreX,shoreY,rise,size)==faded,"world-painter wrapping preserves the same foreground occlusion decision");
            check(alpha(g,new SceneryOcclusion(new Parity()),shoreX,shoreY,rise,size)==1,"a new frame cannot inherit another frame's recorded vessel overlap");
        }
        // The same placement can be inland in one stage and navigable in another;
        // memoized eligibility must distinguish stage water geometry.
        g=new Game();g.reset();
        check(!SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,300,-400,0,size),"upstream factory is outside first-watch water");
        g.challengeId=1;
        check(SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,300,-400,0,size),"bridge reach uses its own upstream water eligibility");
        g.challengeId=0;
        check(!SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,-1550,500,rise,size),"practice basin placement is inland during ordinary shifts");
        g.tutorial=new Tutorial();
        check(SceneryOcclusion.buildingOverWater(g,ATLAS,FRAME,-1550,500,rise,size),"practice basin uses its permanent connected water geometry");
        System.out.println(checks+" building placement, painted overlap and camera-independent eligibility assertions passed");
    }
    public static void main(String[] args){run();}
}
