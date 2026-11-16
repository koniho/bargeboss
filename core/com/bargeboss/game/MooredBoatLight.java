package com.bargeboss.game;

/** RGB incoming light only: moored hulls are receivers, never lamp sources. */
final class MooredBoatLight {
    private MooredBoatLight(){}
    /** Borrowed render scratch; consumed immediately, never retained by a painter. */
    static final class Lights {
        int count;final float[] x=new float[4],y=new float[4],z=new float[4],radius=new float[4],energy=new float[4],score=new float[4];final int[] color=new int[4];
        void add(float dx,float dy,float dz,float r,float e,int c){
            if(e<=0||Math.abs(dx)>r+28||Math.abs(dy)>r+28)return;
            float distance=(float)StrictMath.sqrt(dx*dx+dy*dy+dz*dz);
            float priority=e*VesselShoreLight.falloff(Math.max(0,distance-28)*Math.max(0,distance-28),r);
            if(priority<.002f)return;int at=count; if(at==4){at=3;if(priority<=score[at])return;}else count++;
            while(at>0&&priority>score[at-1]){x[at]=x[at-1];y[at]=y[at-1];z[at]=z[at-1];radius[at]=radius[at-1];energy[at]=energy[at-1];color[at]=color[at-1];score[at]=score[at-1];at--;}
            x[at]=dx;y[at]=dy;z[at]=dz;radius[at]=r;energy[at]=e;color[at]=c;score[at]=priority;
        }
    }
    private static final ThreadLocal<Lights> SCRATCH=new ThreadLocal<Lights>(){protected Lights initialValue(){return new Lights();}};
    static Lights lights(Game g,float x,float y,float heave){Lights out=SCRATCH.get();out.count=0;float z=heave/ArticulatedModel.Z_SCALE;
        for(float[] f:MontroseHarbor.PEDESTALS)out.add(f[0]-x,f[1]-y,MarinaPedestals.LAMP_Z-z,90,.16f,0xffffd68d);
        points(g,out,x,y,z,g.tug);for(Body b:g.cargo)if(b!=null&&b.active)points(g,out,x,y,z,b);return out;
    }
    private static void points(Game g,Lights out,float x,float y,float z,Body b){if(!b.active)return;
        if(b.berth<0)out.add(b.x-x,b.y-y,22-z,100,.65f,0xffffbc6b);
        for(int f=0;f<VesselNavigationLights.count(b.berth<0);f++){
            float lx=VesselNavigationLights.sourceX(g,b,f),ly=VesselNavigationLights.sourceY(g,b,f);
            float e=.55f*VesselShoreLight.sector(VesselNavigationLights.sourceHeading(g,b),f,x-lx,y-ly);
            float height=b.yacht?(f==VesselNavigationLights.STERN?12.1f:15.4f):VesselNavigationLights.height(f);
            out.add(lx-x,ly-y,height-z,55,e,VesselNavigationLights.color(f));
        }
    }
    static void add(float[] rgb,float x,float y,float lx,float ly,float radius,float energy,int color,float angle,int fixture){float dx=x-lx,dy=y-ly;
        if(Math.abs(dx)>radius||Math.abs(dy)>radius)return;
        float power=energy*VesselShoreLight.falloff(dx*dx+dy*dy,radius)*VesselShoreLight.sector(angle,fixture,dx,dy);
        rgb[0]+=power*((color>>16)&255)/255;rgb[1]+=power*((color>>8)&255)/255;rgb[2]+=power*(color&255)/255;
    }
}
