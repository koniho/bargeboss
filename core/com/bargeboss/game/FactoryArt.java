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
        RiverCamera c=g.layout.camera;float size=size(i)*c.scale;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-(g.tutorial!=null?GroundArt.LAND_RISE*c.scale:0);
        float left=x-pivotX(i)*size,top=y-pivotY(i)*size;
        float[] bounds=FactoryArtData.BOUNDS[i];
        float minX=left+bounds[0]*size,minY=top+bounds[1]*size;
        float maxX=left+bounds[2]*size,maxY=top+bounds[3]*size;
        // The shared fading envelope includes both solid art and its animated plume.
        for(int k=0;k<smokeCount(i);k++) {
            float sx=left+smokeX(i,k)*size,sy=top+smokeY(i,k)*size;
            minX=Math.min(minX,sx-65*c.scale);minY=Math.min(minY,sy-117*c.scale);
            maxX=Math.max(maxX,sx+28*c.scale);
        }
        if(maxX<0 || minX>g.layout.width || maxY<0 || minY>g.layout.height) return;
        float alpha=SceneryView.opacity(g,minX,minY,maxX-minX,maxY-minY);
        p.sprite(SpriteAtlas.FACTORY,i,x,y,size,size,0,alpha);
        float phase=i*1.71f+wx*.011f+wy*.007f;
        for(int k=0;k<lightCount(i);k++)
            SceneryEffects.glow(p,left+lightX(i,k)*size,top+lightY(i,k)*size,
                    FactoryArtData.LIGHTS[i][k*3+2]*c.scale,0xffffb34f,alpha,g.visualTicks,phase+k*1.37f);
        for(int k=0;k<smokeCount(i);k++)
            SceneryEffects.smoke(p,left+smokeX(i,k)*size,top+smokeY(i,k)*size,
                    FactoryArtData.SMOKE[i][k*3+2]*c.scale,c.scale,alpha,g.visualTicks,phase+k*2.23f);
    }
}
