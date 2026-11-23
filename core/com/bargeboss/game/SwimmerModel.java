package com.bargeboss.game;

/** Small continuous 3-D rescue skeleton. The water plane clips real geometry, not a rotated sprite.
 * One bounded scratch model belongs to each rescue; rendering never advances its animation clock. */
public final class SwimmerModel {
    private static final int MAX_FACES=80;
    private final float[][] xy=new float[MAX_FACES][20];
    private final float[] depth=new float[MAX_FACES];
    private final int[] colors=new int[MAX_FACES],order=new int[MAX_FACES],size=new int[MAX_FACES];
    private final float[][] vertices=new float[16][3];
    private final float[][] clipped=new float[10][3];
    private static final int[][] QUADS={{0,3,2,1},{4,5,6,7},{0,1,5,4},{1,2,6,5},{2,3,7,6},{3,0,4,7}};
    private static final int[][] OCTAGON={{0,1,9,8},{1,2,10,9},{2,3,11,10},{3,4,12,11},{4,5,13,12},{5,6,14,13},{6,7,15,14},{7,0,8,15},{8,9,10,11,12,13,14,15}};
    private static final float[] OX={1,.70710678f,0,-.70710678f,-1,-.70710678f,0,.70710678f};
    private static final float[] OY={0,.70710678f,1,.70710678f,0,-.70710678f,-1,-.70710678f};
    private static final int[] SKIN={0xffd99c73,0xff98674e,0xffe2b28b,0xffbb825d};
    private int count;private float cosine,sine,wx,wy,bob;private RiverCamera camera;
    static float phase(int ticks,int id){return ticks*.075f+id*2.399963f;}
    static float bob(int ticks,int id){return .32f*(float)StrictMath.sin(phase(ticks,id)*.67f);}
    public void draw(Game g,Painter p,int id){
        MontroseRescue r=g.rescue;camera=g.layout.camera;wx=r.x[id];wy=r.y[id];bob=bob(g.visualTicks,id);count=0;
        float phase=phase(g.visualTicks,id),wave=(float)StrictMath.sin(phase),other=(float)StrictMath.sin(phase+1.7f);
        float relief=r.state[id]==0?0:Math.min(1,(60-r.flash[id])/30f);
        // Gentle heading changes retain a stable rescue/hit anchor. Different phases avoid synchronized crowds.
        float heading=id*.71f+.16f*(float)StrictMath.sin(phase*.23f);
        cosine=(float)StrictMath.cos(heading);sine=(float)StrictMath.sin(heading);
        int skin=SKIN[id%SKIN.length],suit=id%2==0?0xffd27943:0xff4c9caa;
        rounded(0,0,-1.3f,1.25f,.8f,3.3f,suit);
        box(0,-.05f,2.05f,.52f,.5f,.6f,skin);
        rounded(0,0,2.65f,.92f,.77f,1.5f,skin);
        rounded(0,-.05f,3.9f,.98f,.8f,.42f,id%2==0?0xffe3b548:0xff9ec9cd);
        // Upper/lower arms pivot continuously at shoulders and elbows. Secured swimmers hold their ring.
        for(int side=-1;side<=1;side+=2){
            float a=side<0?wave:other;
            float ex=side*((2.1f+.25f*a)*(1-relief)+2*relief),ez=(2.1f+1.15f*a)*(1-relief)+.65f*relief;
            float hx=side*((2.6f+.5f*a)*(1-relief)+2.35f*relief),hz=(3.25f+1.85f*a)*(1-relief)+.25f*relief;
            limb(side*1.15f,0,1.65f,ex,.1f,ez,.42f,skin);
            limb(ex,.1f,ez,hx,.15f+1.15f*relief,hz,.36f,skin);
        }
        // Sorting uses transformed 3-D face depths; local yaw never rotates an isometric bitmap.
        for(int i=0;i<count;i++){int j=i;while(j>0 && depth[order[j-1]]>depth[i]){order[j]=order[j-1];j--;}order[j]=i;}
        for(int j=0;j<count;j++){int i=order[j];float[] q=xy[i];
            if(size[i]==4)p.convexQuad(q[0],q[1],q[2],q[3],q[4],q[5],q[6],q[7],colors[i]);
            else for(int k=1;k<size[i]-1;k++)p.convexQuad(q[0],q[1],q[k*2],q[k*2+1],q[(k+1)*2],q[(k+1)*2+1],q[0],q[1],colors[i]);
        }
    }
    private void box(float x,float y,float z,float rx,float ry,float height,int color){
        for(int i=0;i<8;i++){float[] v=vertices[i];v[0]=x+((i==0||i==3||i==4||i==7)?-rx:rx);v[1]=y+((i%4<2)?-ry:ry);v[2]=z+(i>=4?height:0);}
        emit(color,8,QUADS);
    }
    private void limb(float ax,float ay,float az,float bx,float by,float bz,float radius,int color){
        float dx=bx-ax,dz=bz-az,len=(float)StrictMath.sqrt(dx*dx+dz*dz),nx=dz/len*radius,nz=-dx/len*radius;
        for(int i=0;i<8;i++){boolean end=i>=4;float side=(i%4==0||i%4==3)?-1:1;float[] v=vertices[i];
            v[0]=(end?bx:ax)+nx*side;v[1]=(end?by:ay)+(i%4<2?-radius:radius);v[2]=(end?bz:az)+nz*side;}
        emit(color,8,QUADS);
    }
    private void rounded(float x,float y,float z,float rx,float ry,float height,int color){
        for(int i=0;i<16;i++){float[] v=vertices[i];v[0]=x+OX[i%8]*rx;v[1]=y+OY[i%8]*ry;v[2]=z+(i>=8?height:0);}
        emit(color,16,OCTAGON);
    }
    private void emit(int color,int vertexCount,int[][] faces){
        for(int i=0;i<vertexCount;i++){float[] v=vertices[i];float x=v[0],y=v[1];v[0]=x*cosine-y*sine;v[1]=x*sine+y*cosine;v[2]+=bob;}
        for(int[] face:faces){int n=0;
            // Clip submerged limbs/body against z=0; no lower-body pixels above the water surface.
            for(int k=0;k<face.length;k++){float[] a=vertices[face[k]],b=vertices[face[(k+1)%face.length]];
                if(a[2]>=0){System.arraycopy(a,0,clipped[n++],0,3);}
                if((a[2]>=0)!=(b[2]>=0)){float t=a[2]/(a[2]-b[2]);float[] v=clipped[n++];v[0]=a[0]+(b[0]-a[0])*t;v[1]=a[1]+(b[1]-a[1])*t;v[2]=0;}}
            if(n<3)continue;
            float[] q=xy[count];float d=0;
            for(int k=0;k<n;k++){float[] v=clipped[k];q[k*2]=camera.screenX(wx+v[0],wy+v[1]);q[k*2+1]=camera.screenY(wx+v[0],wy+v[1])-v[2]*camera.scale;d+=(-v[0]+v[1])+v[2]*.816497f;}
            if((q[2]-q[0])*(q[5]-q[1])-(q[3]-q[1])*(q[4]-q[0])>=0)continue;
            float[] a=vertices[face[0]],b=vertices[face[1]],c=vertices[face[2]];
            float ux=b[0]-a[0],uy=b[1]-a[1],uz=b[2]-a[2],vx=c[0]-a[0],vy=c[1]-a[1],vz=c[2]-a[2];
            float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
            float light=.64f+.36f*Math.max(0,(-nx*.3f-ny*.4f+nz*.866f)/(float)StrictMath.sqrt(nx*nx+ny*ny+nz*nz));
            colors[count]=0xff000000|((int)(((color>>16)&255)*light)<<16)|((int)(((color>>8)&255)*light)<<8)|(int)((color&255)*light);
            size[count]=n;depth[count++]=d/n;
        }
    }
    /** Bounded, desynchronized water-plane ripples and small treading/kick droplets. */
    public static void water(Game g,Painter p,int id){
        MontroseRescue r=g.rescue;float phase=phase(g.visualTicks,id),safe=r.state[id]==0?1:.5f;
        float age=(phase/(float)(2*StrictMath.PI))%1;if(age<0)age+=1;
        float radius=1.5f+age*4;int alpha=(int)(65*(1-age));
        p.circle(r.x[id],r.y[id],radius,(alpha<<24)|0x91d3d9);
        for(int side=-1;side<=1;side+=2){float t=(age+(side<0?.5f:0))%1;
            float x=r.x[id]+side*(2+t*1.5f),y=r.y[id]+.5f*(float)StrictMath.sin(phase);
            // The painter supplied here is the water-plane wrapper. Lift using paired projection offsets.
            float z=4*t*(1-t)*safe;
            p.circle(x+RiverCamera.riseX(z),y+RiverCamera.riseY(z),.25f+.15f*(1-t),((int)(130*(1-t))<<24)|0xb4e4e8);
        }
    }
}
