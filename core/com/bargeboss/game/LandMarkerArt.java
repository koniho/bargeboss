package com.bargeboss.game;

/** Fixed beacons bolted to their stage deck. No wave motion or floating collision body. */
public final class LandMarkerArt {
    public static final int COUNT=2,SOURCE_SIZE=1254;
    public static final float SIZE=36,FOOTPRINT_HALF=9;
    // Center of the foundation's bottom footprint, not its front corner.
    public static final float PIVOT_X=627f/SOURCE_SIZE,PIVOT_Y=1103f/SOURCE_SIZE;
    public static final float LAMP_X=626f/SOURCE_SIZE,LAMP_Y=315f/SOURCE_SIZE;
    private static final float[][] MARINA_CUTOUT=marinaCutouts();
    private LandMarkerArt(){}
    public static float baseRise(Game g){return g.montroseStage()?MontroseHarbor.DOCK_RISE:GroundArt.LAND_RISE;}
    /** Two tiny immutable receiver masks; never subtract the full harbor during an active draw. */
    private static float[][] marinaCutouts(){float[][] result=new float[2][];
        for(int i=0;i<2;i++){
            int dock=MontroseHarbor.DOCK_LINES.length-2+i;float[] line=MontroseHarbor.DOCK_LINES[dock];float x=line[2],y=line[3],pad=SIZE*2;
            java.util.ArrayList<float[]> outside=new java.util.ArrayList<float[]>();
            PolygonWater.subtract(new float[]{x-pad,y-pad,x+pad,y-pad,x+pad,y+pad,x-pad,y+pad},MontroseHarbor.DOCKS[dock],outside);
            java.util.ArrayList<float[]> triangles=new java.util.ArrayList<float[]>();int length=0;
            for(float[] polygon:outside)for(float[] triangle:PolygonWater.triangles(polygon)){triangles.add(triangle);length+=triangle.length;}
            result[i]=new float[length];int at=0;for(float[] triangle:triangles){System.arraycopy(triangle,0,result[i],at,triangle.length);at+=triangle.length;}
        }
        return result;
    }
    public static String file(int frame){return frame==0?"land-marker-red-v1.png":"land-marker-green-v1.png";}
    public static float screenX(Game g,float x,float y){return g.layout.camera.screenX(x,y);}
    public static float screenY(Game g,float x,float y){return g.layout.camera.screenY(x,y)-baseRise(g)*g.layout.camera.scale;}
    public static void draw(Game g,Painter p,float wx,float wy,int frame,int color,float power){
        if(g.montroseStage()){MontroseBeaconArt.draw(g,p,color==NavigationLights.RED?MontroseBeaconArt.EAST:MontroseBeaconArt.WEST);return;}
        float x=screenX(g,wx,wy),y=screenY(g,wx,wy),size=SIZE*g.layout.camera.scale;
        p.sprite(SpriteAtlas.LAND_MARKER,frame,x,y,size,size,0,1);
        SpotlightArt.draw(g,p,SpriteAtlas.LAND_MARKER,frame,wx,wy,x,y,size,size,1);
        float lx=x+(LAMP_X-PIVOT_X)*size,ly=y+(LAMP_Y-PIVOT_Y)*size;
        SceneryEffects.glow(p,lx,ly,size*.15f,color,power,g.visualTicks,wx*.013f);
        if(power>0){p.circle(lx,ly,size*.032f,SoftCue.alpha(color,.75f*power));p.circle(lx,ly,size*.012f,SoftCue.alpha(0xfffff5dc,power));}
    }
    /** Keep ground light and both shadow masks on the raised deck, below objects. */
    public static void ground(Game g,Painter p,float wx,float wy,int frame,int color,float power){
        p.pushClipOutside(waterCutout(g,wx,wy));
        try{
            Painter world=new RiverPainter(ScreenView.offset(p,0,-baseRise(g)*g.layout.camera.scale),g.layout.camera);
            for(int i=4;i>=1;i--)world.circle(wx,wy,6+i*4,SoftCue.alpha(color,power*(5-i)*.012f));
            if(g.montroseStage())MontroseBeaconArt.ground(g,p,color==NavigationLights.RED?MontroseBeaconArt.EAST:MontroseBeaconArt.WEST);
            else ShadowView.scenery(g,p,SpriteAtlas.LAND_MARKER,frame,wx,wy,baseRise(g),SIZE);
        }finally{p.popClip();}
    }
    /** Water rectangles projected at deck height exclude any off-quay shadow spill. */
    static float[] waterCutout(Game g,float wx,float wy){
        RiverCamera c=g.layout.camera;float rise=baseRise(g)*c.scale;
        if(g.montroseStage()){
            float[] first=MontroseHarbor.DOCK_LINES[MontroseHarbor.DOCK_LINES.length-2];
            int index=Physics.length(wx-first[2],wy-first[3])<1?0:1;
            float[] source=MARINA_CUTOUT[index],out=new float[source.length];
            for(int i=0;i<source.length;i+=2){out[i]=c.screenX(source[i],source[i+1]);out[i+1]=c.screenY(source[i],source[i+1])-rise;}
            return out;
        }
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
