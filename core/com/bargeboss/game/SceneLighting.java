package com.bargeboss.game;

/** Reference dusk grade retained for compatibility; stage profiles own other ambience.
 * Generic UI and emissive primitives never enter these transforms. */
public final class SceneLighting {
    public static final String VERSION="industrial-dusk-v1";
    private SceneLighting(){}
    public static int sceneryColor(int argb){return scale(argb,92,108,138);}
    /** Called exactly once at a terminal material painter, after face lighting.
     * Coordinate/opacity wrappers and recorded geometry retain their source colors. */
    public static int surfaceColor(int argb,int material){
        if(material==SurfaceMaterial.EMISSIVE)return argb;
        if(material==SurfaceMaterial.WOOD)return scale(argb,42,51,61);
        if(material==SurfaceMaterial.RUST)return scale(argb,104,100,114);
        if(material==SurfaceMaterial.SAFETY)return scale(argb,116,118,125);
        return sceneryColor(argb);
    }
    static int scale(int argb,int red,int green,int blue){
        if((argb>>>24)==0)return argb;
        int r=(((argb>>>16)&255)*red+127)/255,g=(((argb>>>8)&255)*green+127)/255,b=((argb&255)*blue+127)/255;
        return (argb&0xff000000)|(r<<16)|(g<<8)|b;
    }
}
