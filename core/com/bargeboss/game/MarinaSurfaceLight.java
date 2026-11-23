package com.bargeboss.game;

/** Geometry-normal lighting for small marina receivers. Source coordinates are relative to their anchor. */
final class MarinaSurfaceLight {
    private MarinaSurfaceLight(){}
    static int shade(LightingProfile profile,MooredBoatLight.Lights lights,int albedo,
            float x,float y,float z,float nx,float ny,float nz){
        return shade(profile,lights,albedo,x,y,z,nx,ny,nz,false);
    }
    static int vessel(LightingProfile profile,MooredBoatLight.Lights lights,int albedo,float x,float y,float z,float nx,float ny,float nz){
        return shade(profile,lights,albedo,x,y,z,nx,ny,nz,true);
    }
    private static int shade(LightingProfile profile,MooredBoatLight.Lights lights,int albedo,float x,float y,float z,float nx,float ny,float nz,boolean vessel){
        float face=profile.face(nx,ny,nz);
        int shaded=SceneLighting.scale(albedo,Math.round(face*(profile.sunColor>>16&255)),
                Math.round(face*(profile.sunColor>>8&255)),Math.round(face*(profile.sunColor&255)));
        int ambient=vessel?BoatMaterial.ambient(profile,shaded):profile.scenery(shaded);
        float red=0,green=0,blue=0;
        for(int i=0;i<lights.count;i++){
            float dx=lights.x[i]-x,dy=lights.y[i]-y,dz=lights.z[i]-z,d2=dx*dx+dy*dy+dz*dz;
            float diffuse=Math.max(0,(nx*dx+ny*dy+nz*dz)/(float)StrictMath.sqrt(Math.max(.001f,d2)));
            float energy=lights.energy[i]*profile.lamps*VesselShoreLight.falloff(d2,lights.radius[i])*diffuse;
            red+=energy*(lights.color[i]>>16&255)/255f;
            green+=energy*(lights.color[i]>>8&255)/255f;
            blue+=energy*(lights.color[i]&255)/255f;
        }
        return (ambient&0xff000000)|(Math.min(255,(ambient>>16&255)+Math.round((albedo>>16&255)*red))<<16)
                |(Math.min(255,(ambient>>8&255)+Math.round((albedo>>8&255)*green))<<8)
                |Math.min(255,(ambient&255)+Math.round((albedo&255)*blue));
    }
}
