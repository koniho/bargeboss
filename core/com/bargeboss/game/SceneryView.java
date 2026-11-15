package com.bargeboss.game;

/** Independent painted scenery cells, registered to the shared ground plane. */
public final class SceneryView {
    private SceneryView() {}
    public static void ground(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        WaveView.draw(g,p);
        Painter world=new RiverPainter(p,c);
        ArcadeView.water(g,world);
        // Clip the visible world with the real banks. Both coordinates of every
        // corner pass through the same camera as the collision footprints.
        float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=corner%2*g.layout.width,sy=corner/2*g.layout.height;
            float x=c.worldX(sx,sy),y=c.worldY(sx,sy);
            minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
        }
        minX-=80;maxX+=80;minY-=80;maxY+=80;
        if(minX<Physics.LEFT) world.rect(minX,minY,Physics.LEFT-minX,maxY-minY,HarborAtmosphere.SHORE_DARK);
        if(maxX>Physics.RIGHT) world.rect(Physics.RIGHT,minY,maxX-Physics.RIGHT,maxY-minY,HarborAtmosphere.SHORE_LIT);
        for(int bank=0;bank<2;bank++) {
            float edge=bank==0?Physics.RIGHT:Physics.LEFT,side=bank==0?1:-1;
            world.line(edge,minY,edge,maxY,6,VisualStyle.OUTLINE);
            world.line(edge+side*3,minY,edge+side*3,maxY,2,HarborAtmosphere.QUAY_EDGE);
            for(int row=(int)StrictMath.floor(minY/64);row*64<maxY;row++) {
                float y=row*64;
                world.line(edge+side*5,y,edge+side*65,y,1,0x4446576b);
                for(int mark=0;mark<3;mark++) {
                    float yy=y+mark*7;
                    world.line(edge+side*5,yy,edge+side*9,yy+3,2,bank==0?0xffe4b859:0xff233647);
                }
            }
        }
        HarborAtmosphere.bankHaze(g,p);
        LightingView.quays(g,p);
        ShadowView.harbor(g,p);
    }
    public static void props(Game g,Painter p,boolean foreground) {
        if(!foreground) SceneryEffects.industrialStacks(g,p);
        int[] cells=foreground?new int[]{5,3,7,6}:new int[]{0,4,2,1};
        float[] along={70,250,465,625};
        float bank=foreground?Physics.LEFT-82:Physics.RIGHT+48;
        for(int segment=g.lockStage()?-2:0;segment<=0;segment++) for(int i=0;i<cells.length;i++) {
            float worldY=148+along[i]+segment*688;
            prop(g,p,cells[i],bank,worldY);
        }
    }
    /** Buildings share one accepted view and ground pivot, including in practice. */
    static void prop(Game g,Painter p,int cell,float worldX,float worldY) {
        if(cell!=0 && cell!=1) {
            TutorialSceneryArt.draw(g,p,TutorialSceneryArt.fromLegacy(cell),worldX,worldY);return;
        }
        RiverCamera c=g.layout.camera;float s=c.scale;
        float size=StaticHarborArt.WORLD_SIZE*s;
        float x=c.screenX(worldX,worldY),foot=c.screenY(worldX,worldY);
        float y=foot,px=StaticHarborArt.pivotX(StaticHarborArt.WAREHOUSE),py=StaticHarborArt.pivotY(StaticHarborArt.WAREHOUSE);
        float left=x-px*size,top=y-py*size;
        if(left+size<0 || left>g.layout.width || top+size<0 || top>g.layout.height) return;
        float alpha=SceneryOcclusion.buildingOpacity(g,p,SpriteAtlas.STATIC_HARBOR,StaticHarborArt.WAREHOUSE,worldX,worldY,0,StaticHarborArt.WORLD_SIZE);
        p.sprite(SpriteAtlas.STATIC_HARBOR,StaticHarborArt.WAREHOUSE,x,y,size,size,0,alpha);
        StaticHarborArt.lights(p,x,y,size,alpha,g.visualTicks);
    }
    public static void bridge(Game g,Painter p) {
        if(g.challengeId==0 || g.lockStage()) return;
        if(g.bridgeStage()) { BridgeView.draw(g,p);return; }
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int frame=0;frame<2;frame++) {
            float bank=frame==0?Physics.RIGHT+35:Physics.LEFT-35;
            float x=c.screenX(bank,178),foot=c.screenY(bank,178),size=108*s,y=foot-size*.4f;
            float alpha=1;
            p.sprite(SpriteAtlas.BRIDGE,frame,x,y,size,size,0,alpha);
        }
    }
    public static void piers(Game g,Painter p) {
        if(!g.orderStage()) return;
        RiverCamera c=g.layout.camera;Painter world=new RiverPainter(p,c);
        for(float[] r:PhysicalBerths.solids(g.challengeId)) {
            // Project the exact solid rectangle; the old painted pier crops had
            // baked-in axes and could no longer mark the playable edge reliably.
            world.rect(r[0],r[1],r[2]-r[0],r[3]-r[1],VisualStyle.OUTLINE);
            world.rect(r[0]+2,r[1]+2,r[2]-r[0]-4,r[3]-r[1]-4,HarborAtmosphere.SHORE_LIT);
            world.line(r[0],r[1],r[2],r[1],2,HarborAtmosphere.QUAY_EDGE);
            world.line(r[2],r[1],r[2],r[3],2,HarborAtmosphere.QUAY_EDGE);
            for(float y=r[1]+9;y<r[3]-4;y+=14) {
                world.line(r[0]+3,y,r[0]+6,y+5,3,VisualStyle.GOLD);
                world.line(r[2]-6,y,r[2]-3,y+5,3,VisualStyle.OUTLINE);
            }
        }
    }
}
