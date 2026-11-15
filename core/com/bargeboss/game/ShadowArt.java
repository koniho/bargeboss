package com.bargeboss.game;

/** Independent soft silhouette layers; source art, pivots and artwork remain untouched. */
public final class ShadowArt {
    public static final int FIRST=24,LAST=30,BUOY=33,CELL=128,INNER=96,PAD=16;
    private static final int[] SOURCES={SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.FACTORY,SpriteAtlas.CHIMNEYS,SpriteAtlas.TUTORIAL_SCENERY,SpriteAtlas.STATIC_HARBOR,SpriteAtlas.BRIDGE};
    private static final String[] NAMES={"tug","barge","factory","chimney","yard-props","warehouse","bridge"};
    private ShadowArt() {}
    /** Foam 31 and color buoys 32 are not shadow atlases. */
    public static int[] atlases(){return new int[]{24,25,26,27,28,29,30,BUOY};}
    public static boolean atlas(int atlas){return (atlas>=FIRST && atlas<=LAST) || atlas==BUOY;}
    public static int source(int atlas){return atlas==BUOY?SpriteAtlas.BUOY:SOURCES[atlas-FIRST];}
    public static int sourceFrame(int atlas,int frame){return frame%SpriteAtlas.count(source(atlas));}
    public static int forSource(int source){if(source==SpriteAtlas.BUOY)return BUOY;for(int i=0;i<SOURCES.length;i++)if(SOURCES[i]==source)return FIRST+i;throw new IllegalArgumentException("No shadow source "+source);}
    public static String file(int atlas){return atlas==BUOY?"shadow-buoy-v1.png":"shadow-"+NAMES[atlas-FIRST]+"-v1.png";}
    public static int count(int atlas){return SpriteAtlas.count(source(atlas))*2;}
    public static int columns(int atlas){return Math.min(8,count(atlas));}
    public static int rows(int atlas){return (count(atlas)+columns(atlas)-1)/columns(atlas);}
    public static float pivot(int atlas,int frame,boolean y){int src=source(atlas),f=sourceFrame(atlas,frame);return (PAD+INNER*(y?SpriteAtlas.pivotY(src,f):SpriteAtlas.pivotX(src,f)))/CELL;}
    public static float scale(int atlas,int frame,boolean y){int src=source(atlas),f=sourceFrame(atlas,frame);return (y?SpriteAtlas.heightScale(src,f):SpriteAtlas.widthScale(src,f))*CELL/INNER;}
}
