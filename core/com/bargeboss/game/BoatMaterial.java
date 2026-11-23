package com.bargeboss.game;

/** Shared illustrated fiberglass material; GPU hosts interpolate these vertex colors. */
final class BoatMaterial {
    static final int HULL=101,PAINT=102,GLASS=103,METAL=104;
    private BoatMaterial(){}
    private static float clamp(float v,float lo,float hi){return v<lo?lo:v>hi?hi:v;}
    private static float falloff(float d,float radius){float v=1-d/(radius*radius);return v>0?v*v:0;}
    /** Vessels retain their painted palette; cool scenery grading would erase cream/teak. */
    static int ambient(LightingProfile profile,int color){
        if(profile==LightingProfile.DUSK)return SceneLighting.scale(color,202,204,210);
        if(profile==LightingProfile.NIGHT)return SceneLighting.scale(color,100,111,135);
        if(profile==LightingProfile.DAWN)return SceneLighting.scale(color,196,180,177);
        return profile.scenery(color);
    }
    static int shade(LightingProfile profile,MooredBoatLight.Lights lights,int material,int albedo,
            float x,float y,float z,float nx,float ny,float nz,float variation){
        float diffuse=clamp((nx*profile.sunX+ny*profile.sunY+nz*profile.sunZ+.22f)/1.22f,0,1);
        float value=.70f+.30f*diffuse;
        // Broad, low-contrast brush variation stays fixed to the object surface.
        float waterShade=material==HULL?.64f+.36f*clamp(z/3.5f,0,1):1;
        if(material==PAINT && nz<.6f)waterShade*=.91f+.09f*clamp((z-3.5f)/3,0,1);
        int base=SceneLighting.scale(albedo,Math.round(value*variation*waterShade*(profile.sunColor>>16&255)),Math.round(value*variation*waterShade*(profile.sunColor>>8&255)),Math.round(value*variation*waterShade*(profile.sunColor&255)));
        int ambient=ambient(profile,base);
        float hx=profile.sunX-.57735f,hy=profile.sunY+.57735f,hz=profile.sunZ+.57735f;
        float inv=1/(float)StrictMath.sqrt(hx*hx+hy*hy+hz*hz),highlight=clamp((nx*hx+ny*hy+nz*hz)*inv,0,1);
        highlight*=highlight;highlight*=highlight;highlight*=highlight;
        highlight*=material==GLASS?.13f:.055f;
        float r=highlight*(profile.sunColor>>16&255)*profile.red/255f;
        float green=highlight*(profile.sunColor>>8&255)*profile.green/255f;
        float blue=highlight*(profile.sunColor&255)*profile.blue/255f;
        for(int i=0;i<lights.count;i++){
            float dx=lights.x[i]-x,dy=lights.y[i]-y,dz=lights.z[i]-z,d2=dx*dx+dy*dy+dz*dz;
            float wrap=clamp(((nx*dx+ny*dy+nz*dz)/(float)StrictMath.sqrt((d2<.001f?.001f:d2))+.15f)/1.15f,0,1);
            float e=lights.energy[i]*profile.lamps*falloff(d2,lights.radius[i])*wrap;
            r+=e*(lights.color[i]>>16&255)*(albedo>>16&255)/255f;
            green+=e*(lights.color[i]>>8&255)*(albedo>>8&255)/255f;
            blue+=e*(lights.color[i]&255)*(albedo&255)/255f;
        }
        return (albedo&0xff000000)|(Math.min(255,(ambient>>16&255)+Math.round(r))<<16)|(Math.min(255,(ambient>>8&255)+Math.round(green))<<8)|Math.min(255,(ambient&255)+Math.round(blue));
    }
}
