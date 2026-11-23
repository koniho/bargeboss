package com.bargeboss.game;

/** Small registered albedo receiver frames. Prepared at stage entry, never baked per beam pose. */
public final class SpotlightArt {
    static final int FIRST=82;
    static final int[] SOURCES={SpriteAtlas.FACTORY,SpriteAtlas.TUTORIAL_SCENERY,SpriteAtlas.STATIC_HARBOR,
        SpriteAtlas.MARINA,SpriteAtlas.SHORE_BOLLARDS,SpriteAtlas.SHORE_LOADING,SpriteAtlas.SHORE_TRUCKS,
        SpriteAtlas.STEELWORKS,SpriteAtlas.LAND_MARKER,SpriteAtlas.CHIMNEYS};
    public static boolean atlas(int id){return id>=FIRST && id<FIRST+SOURCES.length;}
    public static int source(int id){return atlas(id)?SOURCES[id-FIRST]:id;}
    static int receiver(int source){for(int i=0;i<SOURCES.length;i++)if(SOURCES[i]==source)return FIRST+i;return -1;}
    static int[] prepare(int[] originals){
        java.util.ArrayList<Integer> ids=new java.util.ArrayList<Integer>();
        for(int source:originals){ids.add(source);int light=receiver(source);if(light>=0)ids.add(light);}
        int[] result=new int[ids.size()];for(int i=0;i<result.length;i++)result[i]=ids.get(i);return result;
    }
    static float power(Game g,float x,float y){return Physics.clamp(g.spotlight.energy(g,x,y,12)*.65f,0,.9f);}
    static void draw(Game g,Painter p,int atlas,int frame,float wx,float wy,float x,float y,float w,float h,float alpha){
        int light=receiver(atlas);float power=power(g,wx,wy)*alpha;if(light<0 || power<.005f)return;
        p.sprite(light,frame,x,y,w,h,0,power);
    }
    private SpotlightArt(){}
}
