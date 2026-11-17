package com.bargeboss.game;

/** Raised industrial tops hide live wall effects at inside corners. One bounded view cache. */
final class ShoreTopClip {
    private static float x,y,scale,width,height;
    private static boolean indiana;
    private static float[] cached;
    private ShoreTopClip(){}
    static float[] get(Game g){
        if(g.montroseStage())return ShoreOcclusion.EMPTY;
        RiverCamera c=g.layout.camera;float ox=c.screenX(0,0),oy=c.screenY(0,0);
        if(cached!=null && x==ox && y==oy && scale==c.scale && width==g.layout.width
                && height==g.layout.height && indiana==g.indianaStage())return cached;
        float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<4;i++){
            float sx=(i%2==0?-32:g.layout.width+32),sy=(i<2?-32:g.layout.height+32)+GroundArt.LAND_RISE*c.scale;
            float wx=c.worldX(sx,sy),wy=c.worldY(sx,sy);
            l=Math.min(l,wx);t=Math.min(t,wy);r=Math.max(r,wx);b=Math.max(b,wy);
        }
        cached=ShoreOcclusion.industrial(c,l,t,r,b,Float.NEGATIVE_INFINITY,g.indianaStage());
        x=ox;y=oy;scale=c.scale;width=g.layout.width;height=g.layout.height;indiana=g.indianaStage();
        return cached;
    }
}
