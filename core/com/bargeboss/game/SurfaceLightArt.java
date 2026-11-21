package com.bargeboss.game;

/** Opt-in directional response layers. Identical source-cell pivots, crops and turn frames. */
public final class SurfaceLightArt {
    public static final int FIRST=36,CELL=96,DIRECTIONS=4;
    private static final int[] SOURCES={SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.CONTAINER,SpriteAtlas.BULK,SpriteAtlas.BUOY,SpriteAtlas.YACHT,SpriteAtlas.FIREWORKS,SpriteAtlas.FIRE_BARGE};
    private static final String[] NAMES={"tug","barge","steel","container","coal","buoy","yacht","fireworks","fire"};
    private SurfaceLightArt(){}
    public static int[] sources(){return SOURCES.clone();}
    public static int forSource(int source){for(int i=0;i<SOURCES.length;i++)if(SOURCES[i]==source)return i<7?FIRST+i:66+i-7;return -1;}
    public static boolean atlas(int atlas){return atlas>=FIRST && atlas<FIRST+7 || atlas==66 || atlas==67;}
    public static int source(int atlas){return SOURCES[atlas>=66?atlas-66+7:atlas-FIRST];}
    public static int sourceFrame(int atlas,int frame){return frame%SpriteAtlas.count(source(atlas));}
    public static int frame(int source,int direction,int frame){return direction*SpriteAtlas.count(source)+frame;}
    public static int count(int atlas){return DIRECTIONS*SpriteAtlas.count(source(atlas));}
    public static int columns(int atlas){return 8;}
    public static int rows(int atlas){return (count(atlas)+7)/8;}
    public static String file(int atlas){return "surface-shade-"+NAMES[atlas>=66?atlas-66+7:atlas-FIRST]+"-v2.png";}
    public static float pivot(int atlas,int frame,boolean y){int s=source(atlas),f=sourceFrame(atlas,frame);return y?SpriteAtlas.pivotY(s,f):SpriteAtlas.pivotX(s,f);}
    public static float scale(int atlas,int frame,boolean y){int s=source(atlas),f=sourceFrame(atlas,frame);return y?SpriteAtlas.heightScale(s,f):SpriteAtlas.widthScale(s,f);}
}
