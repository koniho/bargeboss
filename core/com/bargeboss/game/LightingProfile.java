package com.bargeboss.game;

/** Immutable stage-entry lighting. Values are shared by renderers and preparation workers. */
public final class LightingProfile {
    public static final LightingProfile DAWN=new LightingProfile(0,"DAWN",180,155,154,0xff304b59,0xffacc1bb,.38f,.65f,.35f,.55f,0xffffd3ab,.65f,.5f,.55f,0x06d6aaa0,0x53606a);
    public static final LightingProfile DAYLIGHT=new LightingProfile(1,"DAYLIGHT",242,246,244,0xff163f50,0xff79afb8,.17f,-.35f,-.25f,.9f,0xfffff2d9,.85f,.08f,.12f,0x03c1d9dc,0x64717a);
    public static final LightingProfile DUSK=new LightingProfile(2,"DUSK",92,108,138,0xff081c27,0xff83b3b4,.68f,-.35f,-.25f,.9f,0xffffffff,1,1,1,0x04c89972,0x1c201b);
    public static final LightingProfile NIGHT=new LightingProfile(3,"NIGHT",55,72,104,0xff040c19,0xff638797,.76f,-.25f,-.4f,.65f,0xffb9cdeb,.8f,1.12f,1.08f,0x032f4968,0x101a2a);
    public final int id,red,green,blue,water,glint,sunColor,haze,smoke;
    public final String name;
    public final float ambientShade,sunX,sunY,sunZ,shadowStrength,lamps,reflections;
    private LightingProfile(int id,String name,int r,int g,int b,int water,int glint,float shade,float sx,float sy,float sz,int sun,float shadow,float lamps,float reflections,int haze,int smoke){
        this.id=id;this.name=name;red=r;green=g;blue=b;this.water=water;this.glint=glint;ambientShade=shade;sunX=sx;sunY=sy;sunZ=sz;sunColor=sun;shadowStrength=shadow;this.lamps=lamps;this.reflections=reflections;this.haze=haze;this.smoke=smoke;
    }
    public static LightingProfile preset(int id){switch(id){case 0:return DAWN;case 1:return DAYLIGHT;case 2:return DUSK;case 3:return NIGHT;default:throw new IllegalArgumentException("Unknown lighting preset");}}
    public String key(){return this==DUSK?SceneLighting.VERSION:"lighting-v1-"+id;}
    public int scenery(int color){return SceneLighting.scale(color,red,green,blue);}
    public int surface(int color,int material){
        if(material==SurfaceMaterial.EMISSIVE)return color;
        if(this==DUSK)return SceneLighting.surfaceColor(color,material);
        if(material==SurfaceMaterial.WOOD)return SceneLighting.scale(color,red*3/4,green*3/4,blue*3/4);
        return scenery(color);
    }
    public float face(float nx,float ny,float nz){return .62f+.38f*Math.max(0,nx*sunX+ny*sunY+nz*sunZ);}
}
