package com.bargeboss.game;

import java.util.ArrayList;

/** Registered industrial shore props on permanent dry footprints; all missions share placements. */
public final class CalumetArt {
    private static final float[][] FACTORIES={};
    private static final float[][] ITEMS=items();
    private static final float[] LOADING_PX={398/768f,375/768f,397/768f,370/768f},LOADING_PY={354/512f,352/512f,340/512f,336/512f};
    private CalumetArt(){}
    public static float pivotX(int atlas,int frame){
        if(atlas==SpriteAtlas.SHORE_LOADING)return LOADING_PX[frame];
        return .5f;
    }
    public static float pivotY(int atlas,int frame){
        if(atlas==SpriteAtlas.SHORE_LOADING)return LOADING_PY[frame];
        if(atlas==SpriteAtlas.SHORE_TRUCKS)return .72f;
        if(atlas==SpriteAtlas.SHORE_TILES)return .36f;
        return atlas==SpriteAtlas.STEELWORKS?.81f:.78f;
    }
    private static float[][] items(){
        ArrayList<float[]> list=new ArrayList<float[]>();
        for(float[] p:FACTORIES)list.add(new float[]{SpriteAtlas.STEELWORKS,0,p[0],p[1],330,330});
        list.add(new float[]{SpriteAtlas.SHORE_LOADING,0,110,310,170,170*2/3f});
        list.add(new float[]{SpriteAtlas.SHORE_LOADING,1,475,450,170,170*2/3f});
        list.add(new float[]{SpriteAtlas.SHORE_LOADING,2,670,230,150,100});
        list.add(new float[]{SpriteAtlas.SHORE_LOADING,3,670,480,150,100});
        list.add(truck(2,60,370));
        list.add(truck(1,710,550));
        list.add(truck(3,500,-260));
        float[] bank=TrainingChannel.WATER_BOUNDARY;
        for(int i=0;i<bank.length;i+=2){int next=(i+2)%bank.length;float dx=bank[next]-bank[i],dy=bank[next+1]-bank[i+1],len=Physics.length(dx,dy);if(len<70)continue;
            for(float at=40;at<len-24;at+=128){float x=bank[i]+dx/len*at+dy/len*12,y=bank[i+1]+dy/len*at-dx/len*12;
                if(!CalumetMap.contains(x,y))list.add(new float[]{SpriteAtlas.SHORE_BOLLARDS,(i/2)%6,x,y,22,22});}
        }
        float[][] out=list.toArray(new float[list.size()][]);
        java.util.Arrays.sort(out,new java.util.Comparator<float[]>(){public int compare(float[] a,float[] b){return Float.compare(a[3]-a[2],b[3]-b[2]);}});
        return out;
    }
    /** The authored opposite views align with the world X road axis. Pick a dry,
     * connected horizontal lane once, keeping the entire wheelbase on pavement. */
    private static float[] truck(int frame,float preferredX,float preferredY){
        float best=Float.MAX_VALUE,bx=0,by=0;
        for(int row=IndustrialRoadView.MIN_ROW;row<IndustrialRoadView.MAX_ROW;row++)if(Math.floorMod(row,5)==4)
            for(int col=IndustrialRoadView.MIN_COL;col<IndustrialRoadView.MAX_COL;col++){
                float x=col*64+32,y=row*64+32;
                if(!IndustrialRoadView.roadFootprint(x,y,58,22) || !clearSteelworks(x-82,y-46,164,92))continue;
                float d=(x-preferredX)*(x-preferredX)+(y-preferredY)*(y-preferredY);
                if(d<best){best=d;bx=x;by=y;}
            }
        if(best==Float.MAX_VALUE)throw new IllegalStateException("No dry service-road truck placement");
        return new float[]{SpriteAtlas.SHORE_TRUCKS,frame,bx,by,112,112};
    }
    private static boolean clearSteelworks(float x,float y,float width,float height){
        for(float[] f:FACTORIES)if(x<f[0]+100 && x+width>f[0]-100 && y<f[1]+85 && y+height>f[1]-85)return false;
        return true;
    }
    static int count(){return ITEMS.length;}
    static float[] item(int i){return ITEMS[i];}
    static float depth(Game g,int i){return g.layout.camera.screenY(ITEMS[i][2],ITEMS[i][3]);}
    static boolean clearFactory(float x,float y,float width,float height){
        if(!clearSteelworks(x,y,width,height))return false;
        // Clearance includes body overhang and a narrow access apron, not just its anchor.
        for(float[] v:ITEMS)if((int)v[0]==SpriteAtlas.SHORE_TRUCKS && x<v[2]+82 && x+width>v[2]-82 && y<v[3]+46 && y+height>v[3]-46)return false;
        return true;
    }
    static void draw(Game g,Painter p,int i){
        float[] v=ITEMS[i];int atlas=(int)v[0],frame=(int)v[1];RiverCamera c=g.layout.camera;float w=v[4]*c.scale,h=v[5]*c.scale;
        float x=c.screenX(v[2],v[3]),y=c.screenY(v[2],v[3])-GroundArt.LAND_RISE*c.scale;
        if(x+w<0 || x-w>g.layout.width || y+h<0 || y-h>g.layout.height)return;
        float alpha=atlas==SpriteAtlas.STEELWORKS?g.buildingFades.opacity(atlas,frame,v[2],v[3]):1;
        p.sprite(atlas,frame,x,y,w,h,0,alpha);
        if(atlas==SpriteAtlas.STEELWORKS){
            float left=x-w*.5f,top=y-h*.75f*.81f;
            SceneryEffects.compactGlow(p,left+w*.48f,top+h*.75f*.66f,5*c.scale,0xffff9a32,alpha,g.visualTicks,v[2]*.01f);
            SceneryEffects.smoke(p,left+w*.22f,top+h*.75f*.105f,3*c.scale,c.scale,alpha,g.visualTicks,1,true);
            SceneryEffects.smoke(p,left+w*.68f,top+h*.75f*.033f,3*c.scale,c.scale,alpha,g.visualTicks,3,true);
        }
    }
}
