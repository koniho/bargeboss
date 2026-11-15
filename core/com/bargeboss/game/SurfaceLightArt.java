package com.bargeboss.game;

/** Opt-in directional response layers. Identical source-cell pivots, crops and turn frames. */
public final class SurfaceLightArt {
    public static final int FIRST=36,CELL=96,DIRECTIONS=4;
    private static final int[] SOURCES={SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.CONTAINER,SpriteAtlas.BULK,SpriteAtlas.BUOY};
    private static final String[] NAMES={"tug","barge","steel","container","coal","buoy"};
    private SurfaceLightArt(){}
    public static int[] sources(){return SOURCES.clone();}
    public static int forSource(int source){for(int i=0;i<SOURCES.length;i++)if(SOURCES[i]==source)return FIRST+i;return -1;}
    public static boolean atlas(int atlas){return atlas>=FIRST && atlas<FIRST+SOURCES.length;}
    public static int source(int atlas){return SOURCES[atlas-FIRST];}
    public static int sourceFrame(int atlas,int frame){return frame%SpriteAtlas.count(source(atlas));}
    public static int frame(int source,int direction,int frame){return direction*SpriteAtlas.count(source)+frame;}
    public static int count(int atlas){return DIRECTIONS*SpriteAtlas.count(source(atlas));}
    public static int columns(int atlas){return 8;}
    public static int rows(int atlas){return (count(atlas)+7)/8;}
    public static String file(int atlas){return "surface-light-"+NAMES[atlas-FIRST]+"-v1.png";}
    public static float pivot(int atlas,int frame,boolean y){int s=source(atlas),f=sourceFrame(atlas,frame);return y?SpriteAtlas.pivotY(s,f):SpriteAtlas.pivotX(s,f);}
    public static float scale(int atlas,int frame,boolean y){int s=source(atlas),f=sourceFrame(atlas,frame);return y?SpriteAtlas.heightScale(s,f):SpriteAtlas.widthScale(s,f);}
}
