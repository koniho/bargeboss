package com.bargeboss.game;

/** Registered freestanding factory modules; all projection and effects are shared. */
public final class FactoryArt {
    public static final int COUNT=16;
    private static final String[] IDS={"blast-furnace","boiler-house","coking-plant","foundry","rolling-mill","assembly-hall","power-station","pipework","tank-farm","filter-bank","coal-bunker","rail-warehouse","pump-house","substation","compressor-house","gatehouse"};
    private static final int[] TX={2,2,2,2,2,2,2,2,1,1,1,1,1,1,1,1},TY={2,2,2,2,1,1,1,1,2,2,2,2,1,1,1,1};
    private static final float[] HEIGHT={175,145,120,145,85,80,150,65,100,155,60,85,70,55,65,95};
    private FactoryArt() {}
    public static String file(int i) { return FactoryArtData.FILES[i]; }
    public static String id(int i) { return IDS[i]; }
    public static float footprintX(int i) { return TX[i]*32; }
    public static float footprintY(int i) { return TY[i]*32; }
    public static float height(int i) { return HEIGHT[i]; }
    public static float pivotX(int i) { return FactoryArtData.PX[i]; }
    public static float pivotY(int i) { return FactoryArtData.PY[i]; }
    public static float size(int i) { return FactoryArtData.SIZE[i]; }
    public static int lightCount(int i) { return FactoryArtData.LIGHTS[i].length/3; }
    public static float lightX(int i,int k) { return FactoryArtData.LIGHTS[i][k*3]; }
    public static float lightY(int i,int k) { return FactoryArtData.LIGHTS[i][k*3+1]; }
    public static int smokeCount(int i) { return FactoryArtData.SMOKE[i].length/3; }
    public static float smokeX(int i,int k) { return FactoryArtData.SMOKE[i][k*3]; }
    public static float smokeY(int i,int k) { return FactoryArtData.SMOKE[i][k*3+1]; }
    public static void draw(Game g,Painter p,int i,float wx,float wy) {
        if((g.renderExclusions&RenderExclusions.ALL_FACTORIES)==RenderExclusions.ALL_FACTORIES)return;
        RiverCamera c=g.layout.camera;float size=size(i)*c.scale;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-(!g.montroseStage()?GroundArt.LAND_RISE*c.scale:0);
        float left=x-pivotX(i)*size,top=y-pivotY(i)*size;
        float[] bounds=FactoryArtData.BOUNDS[i];
        float minX=left+bounds[0]*size,minY=top+bounds[1]*size;
        float maxX=left+bounds[2]*size,maxY=top+bounds[3]*size;
        // Charge-cycle halos can extend beyond the painted building at screen edges.
        for(int k=0;k<lightCount(i);k++) {
            float sx=left+lightX(i,k)*size,sy=top+lightY(i,k)*size;
            float r=FactoryArtData.LIGHTS[i][k*3+2]*c.scale*8.3f;
            minX=Math.min(minX,sx-r);minY=Math.min(minY,sy-r);
            maxX=Math.max(maxX,sx+r);maxY=Math.max(maxY,sy+r);
        }
        // Visibility includes the plume; fading tests only the solid sprite silhouette.
        for(int k=0;k<smokeCount(i);k++) {
            float sx=left+smokeX(i,k)*size,sy=top+smokeY(i,k)*size;
            float r=FactoryArtData.SMOKE[i][k*3+2]*c.scale*5.94f;
            minX=Math.min(minX,sx-48*c.scale-r*1.3f);minY=Math.min(minY,sy-91*c.scale-r*1.25f);
            maxX=Math.max(maxX,sx+6*c.scale+r*1.2f);maxY=Math.max(maxY,sy+r*1.1f);
        }
        if(maxX<0 || minX>g.layout.width || maxY<0 || minY>g.layout.height) return;
        float alpha=g.buildingFades.opacity(SpriteAtlas.FACTORY,i,wx,wy);
        if((g.renderExclusions&RenderExclusions.FACTORY_ART)==0)p.sprite(SpriteAtlas.FACTORY,i,x,y,size,size,0,alpha);
        float phase=i*1.71f+wx*.011f+wy*.007f;
        // A source can be visible while its rooftop emitters are completely offscreen.
        // Cull each emitter separately and use fewer layers for the wider travel view.
        boolean compact=c.zoom<1.35f;
        if((g.renderExclusions&RenderExclusions.FACTORY_LIGHTS)==0)for(int k=0;k<lightCount(i);k++) {
            float sx=left+lightX(i,k)*size,sy=top+lightY(i,k)*size;
            float radius=FactoryArtData.LIGHTS[i][k*3+2]*c.scale;
            float activeRadius=radius*FactoryActivity.radiusGain(i,k,g.visualTicks,phase);
            if(sx+activeRadius*5.5f<0 || sx-activeRadius*5.5f>g.layout.width || sy+activeRadius*5.5f<0 || sy-activeRadius*5.5f>g.layout.height)continue;
            if(compact)SceneryEffects.compactGlow(p,sx,sy,activeRadius,0xffffb34f,alpha,g.visualTicks,phase+k*1.37f);
            else SceneryEffects.glow(p,sx,sy,activeRadius,0xffffb34f,alpha,g.visualTicks,phase+k*1.37f);
            FactoryActivity.light(p,i,k,sx,sy,radius,alpha,g.visualTicks,phase);
        }
        if((g.renderExclusions&RenderExclusions.FACTORY_SMOKE)==0)for(int k=0;k<smokeCount(i);k++) {
            float sx=left+smokeX(i,k)*size,sy=top+smokeY(i,k)*size;
            float radius=FactoryArtData.SMOKE[i][k*3+2]*c.scale,r=radius*5.94f;
            if(sx+6*c.scale+r*1.2f< -1 || sx-48*c.scale-r*1.3f>g.layout.width+1
                    || sy+r*1.1f< -1 || sy-91*c.scale-r*1.25f>g.layout.height+1)continue;
            SceneryEffects.smoke(p,sx,sy,radius,c.scale,alpha,g.visualTicks,phase+k*2.23f,compact);
            FactoryActivity.steam(p,i,k,sx,sy,radius,c.scale,alpha,g.visualTicks,phase);
        }
    }
}
