package com.bargeboss.game;

/** Chicago M3 reference model. +X bow, +Y starboard, +Z up from waterline.
 * Offline authoring geometry and runtime attachment locations share this coordinate system. */
final class PoliceBoatModel {
    static final int KIND=7,BASE=KIND*32;
    static final int METAL=0,GLASS=1,RUBBER=2,PAINT=3;
    static final float[][] STROBES={{-5,-4.3f,16.4f},{-5,4.3f,16.4f}};
    static final float[][] LIGHTS={{5,-5.5f,14.4f},{5,5.5f,14.4f},{-21,0,7},{-6,0,22}};
    static final float[][] TOW_EYES={{-21,0,4.5f},{21,0,4.5f}};
    static final float[][] STACKS={},TIRES={};
    interface Sink {void face(int color,int material,float... xyz);}
    private final Sink sink;
    private PoliceBoatModel(Sink sink){this.sink=sink;}
    static void build(Sink sink){new PoliceBoatModel(sink).build();}
    private void face(int color,int material,float... xyz){sink.face(color,material,xyz);}
    private void box(float x,float y,float z,float l,float w,float h,int c,int m){
        face(c,m,x,y,z,x+l,y,z,x+l,y,z+h,x,y,z+h);
        face(c,m,x+l,y,z,x+l,y+w,z,x+l,y+w,z+h,x+l,y,z+h);
        face(c,m,x+l,y+w,z,x,y+w,z,x,y+w,z+h,x+l,y+w,z+h);
        face(c,m,x,y+w,z,x,y,z,x,y,z+h,x,y+w,z+h);
        face(c,m,x,y,z+h,x+l,y,z+h,x+l,y+w,z+h,x,y+w,z+h);
    }
    private void rod(float ax,float ay,float az,float bx,float by,float bz,float radius,int c,int mat){
        float dx=bx-ax,dy=by-ay,dz=bz-az,len=(float)StrictMath.sqrt(dx*dx+dy*dy+dz*dz);dx/=len;dy/=len;dz/=len;
        float ux=-dy,uy=dx,uz=0;if(Math.abs(dz)>.95f){ux=1;uy=0;}
        float ul=(float)StrictMath.sqrt(ux*ux+uy*uy);ux/=ul;uy/=ul;
        float vx=dy*uz-dz*uy,vy=dz*ux-dx*uz,vz=dx*uy-dy*ux;
        for(int i=0;i<8;i++){double a=i*Math.PI/4,b=(i+1)*Math.PI/4;
            float x=(ux*(float)StrictMath.cos(a)+vx*(float)StrictMath.sin(a))*radius,y=(uy*(float)StrictMath.cos(a)+vy*(float)StrictMath.sin(a))*radius,z=(uz*(float)StrictMath.cos(a)+vz*(float)StrictMath.sin(a))*radius;
            float xx=(ux*(float)StrictMath.cos(b)+vx*(float)StrictMath.sin(b))*radius,yy=(uy*(float)StrictMath.cos(b)+vy*(float)StrictMath.sin(b))*radius,zz=(uz*(float)StrictMath.cos(b)+vz*(float)StrictMath.sin(b))*radius;
            face(c,mat,ax+x,ay+y,az+z,ax+xx,ay+yy,az+zz,bx+xx,by+yy,bz+zz,bx+x,by+y,bz+z);
        }
    }
    private void build(){
        int silver=0xffadb7b8,dark=0xff242d33,blue=0xff1857c3,glass=0xff193c49,rail=0xffc1cbcb;
        float[] xs={-22,-18,-10,0,10,17,22,24},ws={6.7f,8,8.2f,8.2f,7.7f,5.8f,2.7f,.35f};
        for(int i=0;i<xs.length-1;i++){
            float x=xs[i],n=xs[i+1],w=ws[i],v=ws[i+1];
            // Aluminum deck above a deep-V black hull; collar is a rounded rigid tube.
            face(0xff687478,METAL,x,-w*.86f,5,n,-v*.86f,5,n,v*.86f,5,x,w*.86f,5);
            for(int s=-1;s<=1;s+=2){
                float[] q={x,s*w*.68f,0,n,s*v*.68f,0,n,s*v*.89f,4.5f,x,s*w*.89f,4.5f};
                if(s>0)reverse(q);face(dark,RUBBER,q);
                // Oval collar cross section. Local color banding gives the black rub strip a real surface.
                for(int k=0;k<12;k++){
                    double a=k*Math.PI/6,b=(k+1)*Math.PI/6;
                    float ya=s*(w+1.05f*(float)StrictMath.cos(a)),yb=s*(w+1.05f*(float)StrictMath.cos(b));
                    float yc=s*(v+1.05f*(float)StrictMath.cos(b)),yd=s*(v+1.05f*(float)StrictMath.cos(a));
                    float za=4.2f+1.45f*(float)StrictMath.sin(a),zb=4.2f+1.45f*(float)StrictMath.sin(b);
                    q=new float[]{x,ya,za,n,yd,za,n,yc,zb,x,yb,zb};if(s<0)reverse(q);
                    face(k==0||k==11?0xff303a43:blue,k==0||k==11?RUBBER:PAINT,q);
                }
            }
        }
        box(-22,-6.5f,.4f,.6f,13,4.5f,dark,RUBBER);
        // Full-height center wheelhouse leaves generous fore and aft working decks.
        box(-9,-5.6f,5,18,11.2f,8.5f,silver,METAL);
        box(-9.7f,-6,13.5f,19,12,.55f,0xff586169,RUBBER);
        box(-9.2f,-5.6f,14.05f,18,11.2f,.25f,0xffa4b0b4,METAL);
        for(int s=-1;s<=1;s+=2){float y=s*5.63f;
            for(int i=0;i<3;i++){float x=-7.8f+i*5.35f;
                float[] q={x,y,8,x+4.5f,y,8,x+4.5f,y,12.7f,x,y,12.7f};if(s>0)reverse(q);face(glass,GLASS,q);
                rod(x,y,8,x+4.5f,y,8,.13f,rail,METAL);
            }
            rod(-3,y,5.3f,-3,y,13.2f,.12f,rail,METAL);
            rod(-3,y,5.3f,1.8f,y,5.3f,.12f,rail,METAL);
            box(-2.6f,y-.12f,7,.65f,.24f,.18f,dark,METAL);
            // Foredeck and stern rails; mechanical posts, upper and mid rails.
            for(int i=0;i<3;i++){float x=-21+i*4.8f;rod(x,s*6.8f,5,x,s*6.8f,9,.17f,rail,METAL);}
            rod(-21,s*6.8f,9,-10,s*6.8f,9,.17f,rail,METAL);
            rod(-21,s*6.8f,7,-10,s*6.8f,7,.12f,rail,METAL);
            rod(10,s*6.6f,5,10,s*6.6f,9,.17f,rail,METAL);
            rod(17,s*4.8f,5,17,s*4.8f,9,.17f,rail,METAL);
            rod(10,s*6.6f,9,21,s*2.4f,9,.17f,rail,METAL);
            rod(21,s*2.4f,5,21,s*2.4f,9,.17f,rail,METAL);
            for(int x=-19;x<=17;x+=36)box(x,s*5.6f-.4f,5.1f,2,.8f,.45f,rail,METAL);
        }
        // Front twin windshields and aft cabin door, registered on their actual planes.
        for(int s=-1;s<=1;s+=2){float y=s<0?-5:-.0f;face(glass,GLASS,9.03f,y,8.8f,9.03f,y+4.8f,8.8f,9.03f,y+4.8f,12.8f,9.03f,y,12.8f);}
        box(-9.04f,-2,5.2f,.08f,4,7.8f,0xff7c8b90,METAL);
        face(glass,GLASS,-9.1f,1.6f,9,-9.1f,-1.6f,9,-9.1f,-1.6f,12.5f,-9.1f,1.6f,12.5f);
        box(14,-2.3f,5.05f,4,4.6f,.35f,dark,RUBBER);
        // Tall bow push pad and stern swim platform.
        box(23,-2.3f,3,.8f,4.6f,6,silver,METAL);box(23.8f,-1.65f,3.5f,.5f,3.3f,5,dark,RUBBER);
        box(-24,-6,1.5f,2,12,.5f,silver,METAL);
        for(int s=-1;s<=1;s+=2)box(-22.5f,s*3-.9f,2.2f,1.8f,1.8f,4.2f,dark,RUBBER);
        // Radar mast and searchlight assembly.
        rod(-6,0,14.3f,-7,0,21,.35f,silver,METAL);
        rod(-7,0,20,-3,0,20,.25f,rail,METAL);
        box(-5,-3,20,1,6,.65f,0xffe0e1da,PAINT);
        box(3,-1.1f,14.4f,2.2f,2.2f,1.8f,0xffd4d9d7,PAINT);
        rod(-7,2,15,-7,2,25,.10f,rail,METAL);
        rod(2,-2,14.4f,2,-2,21,.10f,rail,METAL);
        box(-6.4f,-.45f,21.5f,.8f,.9f,.6f,0xffe3e4d9,GLASS);
        for(int i=0;i<2;i++){float[] a=STROBES[i];box(a[0]-.65f,a[1]-.55f,a[2]-1,1.3f,1.1f,1, i==0?0xffb72e3b:0xff205fe2,GLASS);}
        // Orange aft life ring, a true open torus in the XZ plane.
        float cx=-15,cy=-7.05f,cz=7.2f;
        for(int i=0;i<24;i++){double a=i*Math.PI/12,b=(i+1)*Math.PI/12;
            float ca=(float)StrictMath.cos(a),sa=(float)StrictMath.sin(a),cb=(float)StrictMath.cos(b),sb=(float)StrictMath.sin(b);
            face(i%6==0?0xffdddacb:0xffed5727,PAINT,cx+ca*2,cy,cz+sa*2,cx+cb*2,cy,cz+sb*2,cx+cb*1.25f,cy,cz+sb*1.25f,cx+ca*1.25f,cy,cz+sa*1.25f);
        }
        rod(-20,5,5,-20,5,12,.2f,rail,METAL);rod(-20,5,12,-16,5,12,.2f,rail,METAL);
        for(int side=-1;side<=1;side+=2){
            text("CHICAGO POLICE",side,0,9.27f,5.35f,1.55f);
            text("M3",side,15,6.6f,5.4f,1.65f);
        }
        // Stern flag, with horizontal stripes and a navy canton.
        rod(-10,2,14,-10,2,19,.1f,rail,METAL);
        for(int i=0;i<7;i++)face(i%2==0?0xffb34b4b:0xffc9ceca,PAINT,-10,2,18-i*.3f,-13.5f,2,17.8f-i*.3f,-13.5f,2,17.5f-i*.3f,-10,2,17.7f-i*.3f);
        face(0xff293e67,PAINT,-10,1.98f,18,-11.6f,1.98f,17.91f,-11.6f,1.98f,16.95f,-10,1.98f,17.04f);
    }
    private void text(String word,int side,float center,float y,float top,float size){
        float cursor=-Lettering.width(word,size)*.5f;
        for(int n=0;n<word.length();n++){int index=BodyGlyphs.CHARS.indexOf(word.charAt(n));
            if(index>=0){float[] data=BodyGlyphs.FACES[index];
                for(int i=0;i<data.length;i+=8){float[] q=new float[12];
                    for(int j=0;j<4;j++){q[j*3]=center+side*(cursor+data[i+j*2]*size);q[j*3+1]=side*y;q[j*3+2]=top-data[i+j*2+1]*size;}
                    if(side<0)reverse(q);face(0xffe4e9e6,PAINT,q);
                }
            }
            cursor+=(index<0?.4f:BodyGlyphs.ADVANCES[index])*size;
        }
    }
    private static void reverse(float[] q){for(int i=0,j=q.length-3;i<j;i+=3,j-=3)for(int k=0;k<3;k++){float v=q[i+k];q[i+k]=q[j+k];q[j+k]=v;}}
}
