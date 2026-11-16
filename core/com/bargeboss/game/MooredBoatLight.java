package com.bargeboss.game;

/** RGB incoming light only: moored hulls are receivers, never lamp sources. */
final class MooredBoatLight {
    private MooredBoatLight(){}
    static float[] sample(Game g,float x,float y){float[] rgb=new float[12];
        for(float[] fixture:MontroseHarbor.PEDESTALS)add(rgb,x,y,fixture[0],fixture[1],90,.16f,0xffffd68d,0,-1);
        vessel(g,rgb,x,y,g.tug);
        for(Body b:g.cargo)if(b!=null && b.active)vessel(g,rgb,x,y,b);
        float total=rgb[0]+rgb[1]+rgb[2];if(total>.65f)for(int channel=0;channel<3;channel++)rgb[channel]*=.65f/total;
        for(int ch=0;ch<3;ch++){int k=3+ch*3;float n=Physics.length(rgb[k],rgb[k+1]);n=(float)StrictMath.sqrt(n*n+rgb[k+2]*rgb[k+2]);if(n>0){rgb[k]/=n;rgb[k+1]/=n;rgb[k+2]/=n;}}
        return rgb;
    }
    private static void vessel(Game g,float[] rgb,float x,float y,Body b){if(!b.active)return;
        if(b.berth<0)add(rgb,x,y,b.x,b.y,100,.65f,0xffffbc6b,0,-1);
        for(int fixture=0;fixture<VesselNavigationLights.count(b.berth<0);fixture++)add(rgb,x,y,VesselNavigationLights.sourceX(g,b,fixture),VesselNavigationLights.sourceY(g,b,fixture),55,.55f,VesselNavigationLights.color(fixture),VesselNavigationLights.sourceHeading(g,b),fixture);
    }
    static void add(float[] rgb,float x,float y,float lx,float ly,float radius,float energy,int color,float angle,int fixture){float dx=x-lx,dy=y-ly;
        if(Math.abs(dx)>radius||Math.abs(dy)>radius)return;
        float power=energy*VesselShoreLight.falloff(dx*dx+dy*dy,radius)*VesselShoreLight.sector(angle,fixture,dx,dy);
        float distance=(float)StrictMath.sqrt(dx*dx+dy*dy+144);
        if(rgb.length>=12)for(int ch=0;ch<3;ch++){float e=power*((color>>(16-8*ch))&255)/255;int k=3+3*ch;rgb[k]-=e*dx/distance;rgb[k+1]-=e*dy/distance;rgb[k+2]+=e*12/distance;}
        rgb[0]+=power*((color>>16)&255)/255;rgb[1]+=power*((color>>8)&255)/255;rgb[2]+=power*(color&255)/255;
    }
}
