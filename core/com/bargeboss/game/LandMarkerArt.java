package com.bargeboss.game;

/** Fixed channel beacons bolted to a raised quay. No wave motion or water masking. */
public final class LandMarkerArt {
    public static final int COUNT=2,SOURCE_SIZE=1254;
    public static final float SIZE=36,FOOTPRINT_HALF=9;
    // Center of the foundation's bottom footprint, not its front corner.
    public static final float PIVOT_X=627f/SOURCE_SIZE,PIVOT_Y=1103f/SOURCE_SIZE;
    public static final float LAMP_X=626f/SOURCE_SIZE,LAMP_Y=315f/SOURCE_SIZE;
    private LandMarkerArt(){}
    public static String file(int frame){return frame==0?"land-marker-red-v1.png":"land-marker-green-v1.png";}
    public static float screenX(Game g,float x,float y){return g.layout.camera.screenX(x,y);}
    public static float screenY(Game g,float x,float y){return g.layout.camera.screenY(x,y)-GroundArt.LAND_RISE*g.layout.camera.scale;}
    public static void draw(Game g,Painter p,float wx,float wy,int frame,int color,float power){
        float x=screenX(g,wx,wy),y=screenY(g,wx,wy),size=SIZE*g.layout.camera.scale;
        p.sprite(SpriteAtlas.LAND_MARKER,frame,x,y,size,size,0,1);
        float lx=x+(LAMP_X-PIVOT_X)*size,ly=y+(LAMP_Y-PIVOT_Y)*size;
        SceneryEffects.glow(p,lx,ly,size*.15f,color,power,g.visualTicks,wx*.013f);
        if(power>0){p.circle(lx,ly,size*.032f,SoftCue.alpha(color,.75f*power));p.circle(lx,ly,size*.012f,SoftCue.alpha(0xfffff5dc,power));}
    }
    /** Keep ground light and both shadow masks on the raised deck, below objects. */
    public static void ground(Game g,Painter p,float wx,float wy,int frame,int color,float power){
        p.pushClipOutside(waterCutout(g,wx,wy));
        try{
            Painter world=new RiverPainter(GroundArt.raisedCanvas(g,p),g.layout.camera);
            for(int i=4;i>=1;i--)world.circle(wx,wy,6+i*4,SoftCue.alpha(color,power*(5-i)*.012f));
            ShadowView.scenery(g,p,SpriteAtlas.LAND_MARKER,frame,wx,wy,GroundArt.LAND_RISE,SIZE);
        }finally{p.popClip();}
    }
    /** Water rectangles projected at deck height exclude any off-quay shadow spill. */
    static float[] waterCutout(Game g,float wx,float wy){
        RiverCamera c=g.layout.camera;float rise=GroundArt.LAND_RISE*c.scale;
        float[] result=new float[TrainingChannel.WATER_RECTANGLES.length*12];int n=0;
        for(float[] r:TrainingChannel.WATER_RECTANGLES){
            float l=Math.max(r[0],wx-SIZE*2),t=Math.max(r[1],wy-SIZE*2);
            float rr=Math.min(r[2],wx+SIZE*2),b=Math.min(r[3],wy+SIZE*2);
            if(l>=rr || t>=b)continue;
            float ax=c.screenX(l,t),ay=c.screenY(l,t)-rise,bx=c.screenX(rr,t),by=c.screenY(rr,t)-rise;
            float cx=c.screenX(rr,b),cy=c.screenY(rr,b)-rise,dx=c.screenX(l,b),dy=c.screenY(l,b)-rise;
            result[n++]=ax;result[n++]=ay;result[n++]=bx;result[n++]=by;result[n++]=cx;result[n++]=cy;
            result[n++]=ax;result[n++]=ay;result[n++]=cx;result[n++]=cy;result[n++]=dx;result[n++]=dy;
        }
        float[] out=new float[n];System.arraycopy(result,0,out,0,n);return out;
    }
}
