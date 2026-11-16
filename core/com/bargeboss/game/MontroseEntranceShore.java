package com.bargeboss.game;

import java.util.ArrayList;
/** Angular limestone revetment at the green inlet pier. All rock geometry stays
 * in the original dry-shore volume (rise <= LAND_RISE), so existing opaque shore
 * masks remain conservative and no collision or navigation footprint changes. */
public final class MontroseEntranceShore {
    private static final float[][] BAND=band();
    private static final Rock[] ROCKS=rocks();
    private static final ThreadLocal<float[][]> SCREEN=new ThreadLocal<float[][]>(){protected float[][] initialValue(){return new float[][]{new float[6],new float[8],new float[12]};}};
    private static final int[] QUAD_TRIANGLES={0,2,4,0,4,6};
    private static final class Rock {
        final float[][] faces,projected,uv;final float[] bounds,screenBounds;final int[] colors;final float wetness;
        Rock(float[][] faces,int color,float wetness){this.faces=faces;this.wetness=wetness;projected=new float[faces.length][];uv=new float[faces.length][];colors=new int[faces.length];
            float l=Float.MAX_VALUE,t=l,r=-l,b=-l,sl=l,st=l,sr=-l,sb=-l;
            for(int index=0;index<faces.length;index++){float[] f=faces[index];projected[index]=new float[f.length/3*2];uv[index]=new float[projected[index].length];
                for(int i=0,j=0;i<f.length;i+=3,j+=2){l=Math.min(l,f[i]);t=Math.min(t,f[i+1]);r=Math.max(r,f[i]);b=Math.max(b,f[i+1]);float x=RiverCamera.Q*(f[i]+f[i+1]),y=RiverCamera.Q*RiverCamera.DEPTH*(f[i+1]-f[i])-f[i+2];projected[index][j]=x;projected[index][j+1]=y;uv[index][j]=f[i];uv[index][j+1]=f[i+1]+f[i+2];sl=Math.min(sl,x);sr=Math.max(sr,x);st=Math.min(st,y);sb=Math.max(sb,y);}
                float shade=.78f+(index%4)*.07f;colors[index]=0xff000000|((int)(((color>>16)&255)*shade)<<16)|((int)(((color>>8)&255)*shade)<<8)|(int)((color&255)*shade);
            }bounds=new float[]{l,t,r,b};screenBounds=new float[]{sl,st,sr,sb};}
    }
    private MontroseEntranceShore(){}
    /** Gentle local detour leaves space for the reference's broad rock band. */
    static float walkwayInset(float mapX,float mapY){return mapX<280?24*Physics.clamp(Math.min((mapY-1100)/80,(1600-mapY)/100),0,1):0;}
    private static float[][] band(){ArrayList<float[]> out=new ArrayList<float[]>();float[] c=MontroseHarbor.COAST;
        for(int i=0;i<c.length-2;i+=2){float ax=c[i],ay=c[i+1],bx=c[i+2],by=c[i+3],dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy),nx=-dy/len,ny=dx/len;
            float wa=walkwayInset(MontroseHarbor.planX(ax),MontroseHarbor.planY(ay)),wb=walkwayInset(MontroseHarbor.planX(bx),MontroseHarbor.planY(by));if(wa==0 && wb==0)continue;
            if(MontroseHarbor.openWater((ax+bx)*.5f+nx*5,(ay+by)*.5f+ny*5)){nx=-nx;ny=-ny;}
            wa+=5;wb+=5;out.add(new float[]{ax,ay,bx,by,bx+nx*wb,by+ny*wb,ax+nx*wa,ay+ny*wa});}
        return out.toArray(new float[out.size()][]);
    }
    static float[][] footprints(){return BAND;}
    static float[] wholeTerrainCut(RiverCamera c){float[] out=new float[BAND.length*12];int at=0;for(float[] q:BAND)for(int k:QUAD_TRIANGLES){out[at++]=c.screenX(q[k],q[k+1]);out[at++]=c.screenY(q[k],q[k+1]);}return out;}
    static float[] terrainCut(RiverCamera c,float l,float t,float r,float b){return MontroseHarbor.projected(BAND,c,l,t,r,b,Float.NEGATIVE_INFINITY);}
    static void substrate(Game g,Painter p){for(float[] q:BAND)if(MontroseShoreArt.visible(g,q,MontroseHarbor.LAND_RISE))MontroseShoreArt.surface(p,g.layout.camera,q,0,SurfaceMaterial.CONCRETE,0xff454e46,.9f);}
    private static Rock[] rocks(){ArrayList<Rock> out=new ArrayList<Rock>();float[] coast=MontroseHarbor.COAST;int serial=0;
        for(int i=0;i<coast.length-2;i+=2){float ax=coast[i],ay=coast[i+1],dx=coast[i+2]-ax,dy=coast[i+3]-ay,len=Physics.length(dx,dy),tx=dx/len,ty=dy/len,nx=-ty,ny=tx;
            if(MontroseHarbor.openWater(ax+dx*.5f+nx*5,ay+dy*.5f+ny*5)){nx=-nx;ny=-ny;}
            int count=Math.max(1,(int)(len/22));for(int piece=0;piece<count;piece++)for(int row=0;row<3;row++){
                int seed=(serial++*37+row*17)%101;float stagger=row==1?.16f:row==2?-.18f:0,along=(piece+.5f+stagger)*len/count,x=ax+tx*along,y=ay+ty*along,mx=MontroseHarbor.planX(x),my=MontroseHarbor.planY(y),band=walkwayInset(mx,my)+4;
                if(mx>=280 || band<13 || Math.abs(my-1247)<20)continue;
                float center=band*(row==0?.13f:row==1?.44f:.8f),depth=band*(row==0?.22f:row==1?.44f:.39f),width=len/count*(.91f+(seed%5)*.025f),rise=row==0?4+seed%4:13+seed%6;
                float angle=(seed%9-4)*.08f,ca=(float)StrictMath.cos(angle),sa=(float)StrictMath.sin(angle);
                float[][] edge=new float[6][3],top=new float[6][3];float[] us={-.5f,-.34f,.4f,.5f,.28f,-.46f},vs={-.25f,-.5f,-.43f,.3f,.5f,.42f};
                float topShift=(seed%3-1)*1.1f;
                for(int k=0;k<6;k++){float u=width*us[k],v=depth*vs[k],alongU=u*ca-v*sa,inland=Physics.clamp(center+u*sa+v*ca,.2f,band-.25f);
                    edge[k]=new float[]{x+tx*alongU+nx*inland,y+ty*alongU+ny*inland,rise-2-(seed+k)%3*.3f};
                    top[k]=new float[]{x+tx*(alongU*.76f+topShift)+nx*(center+(inland-center)*.76f),y+ty*(alongU*.76f+topShift)+ny*(center+(inland-center)*.76f),rise};
                }
                ArrayList<float[]> faces=new ArrayList<float[]>();float low=row==0?0:4;
                for(int k=0;k<6;k++){int next=(k+1)%6;float fx=edge[next][1]-edge[k][1],fy=-(edge[next][0]-edge[k][0]);
                    if(fy-fx>0)faces.add(new float[]{edge[k][0],edge[k][1],low,edge[next][0],edge[next][1],low,edge[next][0],edge[next][1],edge[next][2],edge[k][0],edge[k][1],edge[k][2]});
                    faces.add(new float[]{edge[k][0],edge[k][1],edge[k][2],edge[next][0],edge[next][1],edge[next][2],top[next][0],top[next][1],top[next][2],top[k][0],top[k][1],top[k][2]});
                }
                float[] cap=new float[18];for(int k=0;k<6;k++)System.arraycopy(top[k],0,cap,k*3,3);faces.add(cap);
                out.add(new Rock(faces.toArray(new float[faces.size()][]),row==0?0xff526058:(seed%3==0?0xffb4b1a0:seed%3==1?0xffc3c0ad:0xffa6a89d),row==0?.85f:.15f));
            }
        }
        java.util.Collections.sort(out,new java.util.Comparator<Rock>(){public int compare(Rock a,Rock b){return Float.compare(a.bounds[1]-a.bounds[0],b.bounds[1]-b.bounds[0]);}});
        return out.toArray(new Rock[out.size()]);
    }
    static int rockCount(){return ROCKS.length;}
    static boolean geometryWithinShore(){for(Rock rock:ROCKS)for(float[] f:rock.faces)for(int i=0;i<f.length;i+=3)if(MontroseHarbor.openWater(f[i],f[i+1]) || f[i+2]>MontroseHarbor.LAND_RISE || f[i+2]<0)return false;return true;}
    static void rocks(Game g,Painter p){RiverCamera c=g.layout.camera;float originX=c.screenX(0,0),originY=c.screenY(0,0),scale=c.scale;float[][] scratch=SCREEN.get();
        for(Rock rock:ROCKS){float[] b=rock.screenBounds;if(originX+b[2]*scale<0 || originX+b[0]*scale>g.layout.width || originY+b[3]*scale<0 || originY+b[1]*scale>g.layout.height)continue;
            for(int face=0;face<rock.projected.length;face++){float[] source=rock.projected[face],xy=scratch[source.length==6?0:source.length==8?1:2];for(int i=0;i<source.length;i+=2){xy[i]=originX+source[i]*scale;xy[i+1]=originY+source[i+1]*scale;}p.surfacePolygon(xy,rock.uv[face],SurfaceMaterial.CONCRETE,rock.colors[face],rock.wetness);}
        }
    }
    /** Retain the exact collision footprint and receiver elevation of the green pier. */
    static void pier(Game g,Painter p){float[] q=MontroseHarbor.DOCKS[MontroseHarbor.DOCKS.length-2];if(MontroseShoreArt.visible(g,q,MontroseHarbor.DOCK_RISE))MontroseShoreArt.surface(p,g.layout.camera,q,MontroseHarbor.DOCK_RISE,SurfaceMaterial.CONCRETE,0xffada999,.25f);}
    static float[][] stepGeometry(){float[] a=MontroseShoreArt.ACCESS[MontroseHarbor.GREEN_INLET_ACCESS];float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy),nx=-dy/len,ny=dx/len;float[][] steps=new float[5][];
        for(int i=0;i<5;i++){float from=i/5f,to=(i+1)/5f,w0=(38+(a[4]-38)*Math.min(1,from*2))*.5f,w1=(38+(a[4]-38)*Math.min(1,to*2))*.5f;w0-=.06f;w1-=.06f;
            steps[i]=new float[]{a[0]+dx*from+nx*w0,a[1]+dy*from+ny*w0,a[0]+dx*to+nx*w1,a[1]+dy*to+ny*w1,a[0]+dx*to-nx*w1,a[1]+dy*to-ny*w1,a[0]+dx*from-nx*w0,a[1]+dy*from-ny*w0,MontroseHarbor.LAND_RISE+(MontroseHarbor.DOCK_RISE-MontroseHarbor.LAND_RISE)*to};}
        return steps;
    }
    private static final class Steps {static final float[][] ALL=stepGeometry();}
    static void steps(Game g,Painter p){RiverCamera c=g.layout.camera;for(float[] s:Steps.ALL){float[] q=MontroseShoreArt.quad(s);if(!MontroseShoreArt.visible(g,q,MontroseHarbor.LAND_RISE))continue;
        MontroseShoreArt.wall(p,c,s[0],s[1],s[6],s[7],s[8],s[8]+(MontroseHarbor.LAND_RISE-MontroseHarbor.DOCK_RISE)/5,SurfaceMaterial.CONCRETE,0xff81877e,.18f);
        MontroseShoreArt.surface(p,c,q,s[8],SurfaceMaterial.CONCRETE,0xffb1b0a0,.08f);
    }}
}
