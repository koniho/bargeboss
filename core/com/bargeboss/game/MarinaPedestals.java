package com.bargeboss.game;

/** Fixed dock power/light pedestals. Rigid world geometry, independent animated illumination.
 * Ground depth merges with vessels/rope; light pools are a separate pre-vessel pass. */
public final class MarinaPedestals {
    // Dock rise is four projected world units; convert once to physical model Z.
    public static final float DECK_Z=4/ArticulatedModel.Z_SCALE,LAMP_Z=13,LIGHT_RADIUS=90;
    private static final float CELL=96;
    private static final Fixture[] FIXTURES=make();
    private static final int[] ORDER=order();
    private static final Grid GRID=new Grid();
    // Borrowed per-thread polygon buffer: native painters consume the array within poly().
    // Terrain preparation never draws these live fixtures or mutates their world geometry.
    private static final ThreadLocal<float[]> SCREEN=new ThreadLocal<float[]>() {
        protected float[] initialValue(){return new float[8];}
    };
    private static final StaticLayer POOL=new Kernel(false),HALO=new Kernel(true);
    private MarinaPedestals(){}

    private static final class Face {
        final float[] projected;final int color;final boolean luminous;
        Face(float[] xyz,int color,boolean luminous){this.color=color;this.luminous=luminous;projected=new float[8];
            for(int i=0;i<4;i++){projected[2*i]=RiverCamera.Q*(xyz[3*i]+xyz[3*i+1]);
                projected[2*i+1]=RiverCamera.Q*RiverCamera.DEPTH*(xyz[3*i+1]-xyz[3*i])-ArticulatedModel.Z_SCALE*xyz[3*i+2];}
        }
    }
    private static final class Fixture {
        final float x,y,phase;final Face[] faces;
        Fixture(int id,float[] source){x=source[0];y=source[1];phase=id*1.719f;
            java.util.ArrayList<Face> out=new java.util.ArrayList<Face>();
            box(out,source[2],source[3],1.6f,DECK_Z,12,false,false);
            box(out,source[2],source[3],1.8f,12,14,true,false);
            box(out,source[2],source[3],2.3f,14,14.8f,false,true);
            faces=out.toArray(new Face[out.size()]);
        }
    }
    /** Rotate local rigid points before camera projection; no painted bitmap rotation. */
    private static void box(java.util.ArrayList<Face> out,float dx,float dy,float half,float bottom,float top,boolean light,boolean lid){
        float[][] corners=new float[4][2];
        for(int i=0;i<4;i++){float u=(i==0||i==3)?-half:half,v=i<2?-half:half;corners[i][0]=dx*u-dy*v;corners[i][1]=dy*u+dx*v;}
        for(int i=0;i<4;i++){int j=(i+1)%4;float nx=corners[j][1]-corners[i][1],ny=-(corners[j][0]-corners[i][0]);
            if(-nx+ny<=.0001f)continue; // Camera-facing sides of the rigid box only.
            float[] a=corners[i],b=corners[j];
            int color=light?0xffffd68d:(nx<0?0xff465260:0xff303b47);
            out.add(new Face(new float[]{a[0],a[1],bottom,b[0],b[1],bottom,b[0],b[1],top,a[0],a[1],top},color,light));
        }
        if(lid){float[] q=new float[12];for(int i=0;i<4;i++){q[3*i]=corners[i][0];q[3*i+1]=corners[i][1];q[3*i+2]=top;}out.add(new Face(q,0xff6d7983,false));}
    }
    private static Fixture[] make(){Fixture[] out=new Fixture[MontroseHarbor.PEDESTALS.length];for(int i=0;i<out.length;i++)out[i]=new Fixture(i,MontroseHarbor.PEDESTALS[i]);return out;}
    private static int[] order(){int[] out=new int[FIXTURES.length];for(int i=0;i<out.length;i++){int j=i;float depth=FIXTURES[i].y-FIXTURES[i].x;
        while(j>0 && FIXTURES[out[j-1]].y-FIXTURES[out[j-1]].x>depth){out[j]=out[j-1];j--;}out[j]=i;}return out;}
    public static int count(Game g){return g.montroseStage()?ORDER.length:0;}
    public static float depth(Game g,int sortedIndex){Fixture f=FIXTURES[ORDER[sortedIndex]];return g.layout.camera.screenY(f.x,f.y);}
    private static float energy(Game g,Fixture f){return SceneryEffects.intensity(g.visualTicks,f.phase);}
    private static boolean visible(Game g,float x,float y,float padding){return x+padding>=0 && x-padding<=g.layout.width && y+padding>=0 && y-padding<=g.layout.height;}

    /** Called by the river depth merge; no building fades, no per-fixture/frame geometry allocation. */
    public static void draw(Game g,Painter canvas,int sortedIndex){
        Fixture f=FIXTURES[ORDER[sortedIndex]];RiverCamera c=g.layout.camera;
        float x=c.screenX(f.x,f.y),y=c.screenY(f.x,f.y),s=c.scale;
        if(!visible(g,x,y,25*s))return;
        float power=energy(g,f);float[] q=SCREEN.get();
        for(Face face:f.faces){for(int i=0;i<8;i+=2){q[i]=x+face.projected[i]*s;q[i+1]=y+face.projected[i+1]*s;}
            int color=face.luminous?((int)(225+30*power)<<24)|(face.color&0xffffff):face.color;canvas.poly(q,color);
        }
        float halo=1+.04f*(float)StrictMath.sin(g.visualTicks*.016+f.phase),scale=s*.65f*halo;
        canvas.layer(HALO,x-20*scale,y-ArticulatedModel.Z_SCALE*LAMP_Z*s-20*scale,scale,.86f*power);
    }
    /** Draw after static docks and before craft/rope, so pools never paint over a vessel. */
    public static void drawPools(Game g,Painter canvas){
        if(!g.montroseStage())return;RiverCamera c=g.layout.camera;
        if(canvas.preparesLayers()){
            canvas.prepareLayer(POOL,StaticLayer.rasterScale(c.scale));
            float lo=StaticLayer.rasterScale(c.scale*.65f*.96f),hi=StaticLayer.rasterScale(c.scale*.65f*1.04f);
            canvas.prepareLayer(HALO,lo);if(hi!=lo)canvas.prepareLayer(HALO,hi);
        }
        for(Fixture f:FIXTURES){float x=c.screenX(f.x,f.y),y=c.screenY(f.x,f.y),s=c.scale;
            if(!visible(g,x,y,48*s))continue;
            canvas.layer(POOL,x-48*s,y-32*s,s,.88f*energy(g,f));
        }
    }

    /** Two bounded, immutable raster kernels shared by every pedestal; no runtime blur. */
    private static final class Kernel extends StaticLayer {
        final boolean halo;final float[][] rings;final int[] colors;
        Kernel(boolean halo){this.halo=halo;rings=new float[48][96];colors=new int[rings.length];
            float cx=halo?20:48,cy=halo?20:32;
            float previous=0;
            for(int r=0;r<rings.length;r++){float t=(rings.length-r)/(float)rings.length,rx=(halo?19:46)*t,ry=(halo?19:26.56f)*t;
                float fall=1-t*t,target=(halo?.78f:.32f)*fall*fall*fall;
                int alpha=Math.round(255*(target-previous)/(1-previous));previous=target;
                colors[r]=(alpha<<24)|(halo?0xffdc99:0xf2b55b);
                for(int k=0;k<48;k++){double a=k*StrictMath.PI/24;rings[r][k*2]=cx+rx*(float)StrictMath.cos(a);rings[r][k*2+1]=cy+ry*(float)StrictMath.sin(a);}}
        }
        public int key(){return halo?0x42000102:0x42000101;}
        public float width(){return halo?40:96;}
        public float height(){return halo?40:64;}
        public boolean backgroundSafe(){return true;}
        public void paint(Painter p){for(int i=0;i<rings.length;i++)if((colors[i]>>>24)>0)p.poly(rings[i],colors[i]);}
    }

    /** Dense immutable spatial bins avoid scanning all dock fixtures for each hull response. */
    private static final class Grid {
        final int left,top,width,height;final int[][] bins;
        Grid(){int l=Integer.MAX_VALUE,t=l,r=Integer.MIN_VALUE,b=r;
            for(Fixture f:FIXTURES){int x=cell(f.x),y=cell(f.y);l=Math.min(l,x);t=Math.min(t,y);r=Math.max(r,x);b=Math.max(b,y);}
            left=l;top=t;width=r-l+1;height=b-t+1;bins=new int[width*height][];int[] sizes=new int[bins.length];
            for(Fixture f:FIXTURES)sizes[(cell(f.y)-top)*width+cell(f.x)-left]++;
            for(int i=0;i<bins.length;i++)bins[i]=new int[sizes[i]];
            for(int i=0;i<FIXTURES.length;i++){Fixture f=FIXTURES[i];int k=(cell(f.y)-top)*width+cell(f.x)-left;bins[k][--sizes[k]]=i;}
        }
    }
    private static int cell(float value){return (int)StrictMath.floor(value/CELL);}
    static void sample(Game g,float[] weights,float x,float y){
        if(!g.montroseStage())return;int col=cell(x)-GRID.left,row=cell(y)-GRID.top;
        for(int yy=Math.max(0,row-1);yy<=Math.min(GRID.height-1,row+1);yy++)for(int xx=Math.max(0,col-1);xx<=Math.min(GRID.width-1,col+1);xx++)
            for(int id:GRID.bins[yy*GRID.width+xx]){Fixture f=FIXTURES[id];
                if(Math.abs(f.x-x)>=LIGHT_RADIUS || Math.abs(f.y-y)>=LIGHT_RADIUS)continue;
                SurfaceLighting.add(weights,x,y,f.x,f.y,LAMP_Z*ArticulatedModel.Z_SCALE,LIGHT_RADIUS,.34f*energy(g,f));
            }
    }
}
