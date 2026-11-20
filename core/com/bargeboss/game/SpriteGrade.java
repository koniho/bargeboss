package com.bargeboss.game;

/** One-time scenery grading and teacher livery. Hosts cache the result per palette. */
public final class SpriteGrade {
    private SpriteGrade() {}
    public static boolean applies(int atlas) {
        return atlas==SpriteAtlas.TEACHER_TUG || atlas==SpriteAtlas.MARINA || atlas==SpriteAtlas.NORTH_QUAY || atlas==SpriteAtlas.SOUTH_QUAY
                || atlas==SpriteAtlas.SCENERY || atlas==SpriteAtlas.PIERS || atlas==SpriteAtlas.BRIDGE
                || atlas==SpriteAtlas.TITLE_HARBOR || atlas==SpriteAtlas.TITLE_LAND
                || atlas==SpriteAtlas.STATIC_HARBOR || atlas==SpriteAtlas.TUTORIAL_SCENERY
                || atlas==SpriteAtlas.CHIMNEYS || atlas==SpriteAtlas.FACTORY || atlas==SpriteAtlas.GROUND
                || atlas==SpriteAtlas.SHORE_BOLLARDS || atlas==SpriteAtlas.SHORE_LOADING || atlas==SpriteAtlas.SHORE_TRUCKS
                || atlas==SpriteAtlas.SHORE_TILES || atlas==SpriteAtlas.STEELWORKS;
    }
    /** The same PNG can be used by differently graded atlas regions. */
    public static String cacheKey(int atlas,String path) {return path+(atlas==SpriteAtlas.TEACHER_TUG?"#teacher-green-v1":applies(atlas)?"#"+SceneLighting.VERSION+(windows(atlas)?"-windows":atlas==SpriteAtlas.PIERS?"-dock":"-solid"):"#source");}
    public static int argb(int atlas,int pixel) {return applies(atlas)?grade(atlas,pixel):pixel;}
    private static boolean windows(int atlas){return atlas==SpriteAtlas.FACTORY || atlas==SpriteAtlas.STEELWORKS;}
    private static int grade(int atlas,int pixel) {
        if((pixel>>>24)==0) return pixel;
        int r=(pixel>>>16)&255,g=(pixel>>>8)&255,b=pixel&255;
        if(atlas==SpriteAtlas.TEACHER_TUG){
            if(r>b+18 && r>g+8){int warmth=r-b;return (pixel&0xff000000)|(Math.max(0,r-warmth)<<16)|(Math.min(255,g+warmth)<<8)|Math.min(255,b+warmth/4);}
            return pixel;
        }
        // Retain bright warm windows, not the darker rusty brick around them.
        int warm=windows(atlas)?Math.min(ramp(r-160,80),Math.min(ramp(g-b-20,65),ramp(r-b-45,95))):0;
        int base=atlas==SpriteAtlas.PIERS?SceneLighting.surfaceColor(pixel,SurfaceMaterial.WOOD):SceneLighting.sceneryColor(pixel);
        int red=(base>>>16)&255,green=(base>>>8)&255,blue=base&255;
        red+=(warm*(r-red)+127)/255;green+=(warm*(g-green)+127)/255;blue+=(warm*(b-blue)+127)/255;
        return (pixel&0xff000000)|(red<<16)|(green<<8)|blue;
    }
    private static int ramp(int value,int span) {return value<=0?0:value>=span?255:(value*255+span/2)/span;}
    public static void applyArgb(int atlas,int[] pixels) {
        if(!applies(atlas)) return;
        for(int i=0;i<pixels.length;i++) pixels[i]=grade(atlas,pixels[i]);
    }
    /** Core Graphics supplies explicit RGBA8 premultiplied bytes; conversion is shared too. */
    public static void applyPremultipliedRgba(int atlas,byte[] pixels) {
        if(!applies(atlas)) return;
        if(pixels.length%4!=0) throw new IllegalArgumentException("RGBA pixels require four bytes");
        for(int i=0;i<pixels.length;i+=4) {
            int a=pixels[i+3]&255;if(a==0) continue;
            int r=straight(pixels[i]&255,a),g=straight(pixels[i+1]&255,a),b=straight(pixels[i+2]&255,a);
            int graded=grade(atlas,(a<<24)|(r<<16)|(g<<8)|b);
            pixels[i]=(byte)premultiplied((graded>>>16)&255,a);
            pixels[i+1]=(byte)premultiplied((graded>>>8)&255,a);
            pixels[i+2]=(byte)premultiplied(graded&255,a);
        }
    }
    public static int straight(int channel,int alpha) {return alpha==0?0:Math.min(255,(channel*255+alpha/2)/alpha);}
    public static int premultiplied(int channel,int alpha) {return (channel*alpha+127)/255;}
}
