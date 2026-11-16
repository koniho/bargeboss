package com.bargeboss.game;
/** CPU reference for the packed octahedral-normal / world-height receiver texture. */
public final class BoatSurfaceLight {
    private BoatSurfaceLight(){}
    public static float response(int packed,float u,float v,float lx,float ly,float lz,float radius){
        float nx=((packed>>16)&255)/127.5f-1,ny=((packed>>8)&255)/127.5f-1,nz=1-Math.abs(nx)-Math.abs(ny);
        if(nz<0){float old=nx;nx=(1-Math.abs(ny))*(nx>=0?1:-1);ny=(1-Math.abs(old))*(ny>=0?1:-1);}
        float length=(float)StrictMath.sqrt(nx*nx+ny*ny+nz*nz);nx/=length;ny/=length;nz/=length;
        float z=(packed&255)*64/255f,sum=(u-.5f)*96/RiverCamera.Q,diff=((v-.7f)*96+ArticulatedModel.Z_SCALE*z)/(RiverCamera.Q*RiverCamera.DEPTH);
        float dx=lx-(sum-diff)*.5f,dy=ly-(sum+diff)*.5f,dz=lz-z,d2=dx*dx+dy*dy+dz*dz;
        return Math.max(0,(nx*dx+ny*dy+nz*dz)/(float)StrictMath.sqrt(Math.max(.0001f,d2)))*VesselShoreLight.falloff(d2,radius);
    }
}
