package com.bargeboss.game;

/** White 3-D mooring balls: immutable convex geometry, clipped at the water plane. */
final class MooringBuoyArt {
    static final int WHITE=0xfff1f2ed;
    private static final Face[] FACES=faces();
    private static final class Face {
        final float[] screen=new float[8];
        final float x,y,z,nx,ny,nz;
        Face(float[] p){
            float cx=0,cy=0,cz=0;
            for(int i=0;i<4;i++){int j=i*3;cx+=p[j];cy+=p[j+1];cz+=p[j+2];
                screen[i*2]=RiverCamera.Q*(p[j]+p[j+1]);
                screen[i*2+1]=RiverCamera.Q*RiverCamera.DEPTH*(p[j+1]-p[j])-ArticulatedModel.Z_SCALE*p[j+2];}
            x=cx/4;y=cy/4;z=cz/4;
            float ax=p[3]-p[0],ay=p[4]-p[1],az=p[5]-p[2],bx=p[6]-p[0],by=p[7]-p[1],bz=p[8]-p[2];
            float xx=ay*bz-az*by,yy=az*bx-ax*bz,zz=ax*by-ay*bx;
            float length=(float)StrictMath.sqrt(xx*xx+yy*yy+zz*zz);nx=xx/length;ny=yy/length;nz=zz/length;
        }
    }
    private static Face[] faces(){
        java.util.ArrayList<Face> out=new java.util.ArrayList<Face>();
        float[] lat={-.35f,0,.5f,.8660254f,1};
        for(int band=0;band<lat.length-1;band++)for(int side=0;side<12;side++){
            float[] p=new float[12];
            for(int i=0;i<4;i++){
                float z=lat[band+(i>=2?1:0)],r=(float)StrictMath.sqrt(Math.max(0,1-z*z));
                double a=(side+(i==1 || i==2?1:0))*StrictMath.PI/6;
                p[i*3]=r*(float)StrictMath.cos(a);p[i*3+1]=r*(float)StrictMath.sin(a);p[i*3+2]=z+.35f;
            }
            Face f=new Face(p);float[] q=f.screen;
            if((q[2]-q[0])*(q[5]-q[1])-(q[3]-q[1])*(q[4]-q[0])>0)out.add(f);
        }
        return out.toArray(new Face[out.size()]);
    }
    private MooringBuoyArt(){}
    static void draw(Game g,Painter p,float wx,float wy,float size){
        draw(g,p,wx,wy,size,0);
    }
    static void draw(Game g,Painter p,float wx,float wy,float size,float contactBob){
        RiverCamera c=g.layout.camera;float radius=size*.27f,scale=radius*c.scale;
        float heave=g.water.height(g,wx,wy,g.visualTicks,null)-contactBob,x=c.screenX(wx,wy),y=c.screenY(wx,wy)-heave*c.scale;
        MooredBoatLight.Lights lights=MooredBoatLight.lights(g,wx,wy,heave);
        p.pushClipOutside(ShoreOcclusion.region(g,c,wx-size,wy-size,wx+size,wy+size,c.screenY(wx,wy)));
        try{for(Face f:FACES){float[] q=f.screen;
            int color=MarinaSurfaceLight.shade(g.lighting(),lights,WHITE,f.x*radius,f.y*radius,f.z*radius,f.nx,f.ny,f.nz);
            p.convexQuad(x+q[0]*scale,y+q[1]*scale,x+q[2]*scale,y+q[3]*scale,
                    x+q[4]*scale,y+q[5]*scale,x+q[6]*scale,y+q[7]*scale,color);
        }}finally{p.popClip();}
    }
}
