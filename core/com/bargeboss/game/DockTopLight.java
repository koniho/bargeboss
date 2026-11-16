package com.bargeboss.game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/** Immutable disjoint dock-top cells, prepared before active drawing. */
final class DockTopLight {
    private static final float CELL=8;
    private static final class Geometry {static final Map<Long,ArrayList<float[]>> CELLS=make();}
    private DockTopLight(){}
    static void prepare(){Geometry.CELLS.size();}
    private static long key(int x,int y){return ((long)x<<32)|(y&0xffffffffL);}
    private static int cell(float x){return (int)StrictMath.floor(x/CELL);}
    private static Map<Long,ArrayList<float[]>> make(){Map<Long,ArrayList<float[]>> map=new HashMap<Long,ArrayList<float[]>>();Map<Long,ArrayList<float[]>> originals=new HashMap<Long,ArrayList<float[]>>();
        for(float[] dock:MontroseHarbor.DOCKS){float[] b=PolygonWater.bounds(dock);
            for(int y=cell(b[1]);y<=cell(b[3]);y++)for(int x=cell(b[0]);x<=cell(b[2]);x++){
                float[] clipped=PolygonWater.rectangle(dock,x*CELL,y*CELL,(x+1)*CELL,(y+1)*CELL);if(clipped.length<6 || Math.abs(PolygonWater.area(clipped))<.001f)continue;
                long k=key(x,y);ArrayList<float[]> old=map.get(k);if(old==null){old=new ArrayList<float[]>();map.put(k,old);originals.put(k,new ArrayList<float[]>());}
                ArrayList<float[]> pending=new ArrayList<float[]>();pending.add(clipped);
                for(float[] occupied:originals.get(k)){ArrayList<float[]> next=new ArrayList<float[]>();for(float[] p:pending){if(PolygonWater.overlaps(PolygonWater.bounds(p),PolygonWater.bounds(occupied)))PolygonWater.subtract(p,occupied,next);else next.add(p);}pending=new ArrayList<float[]>();for(float[] p:next)if(Math.abs(PolygonWater.area(p))>.001f)pending.add(p);}
                old.addAll(pending);originals.get(k).add(clipped);
            }
        }return map;
    }
    static float heightFactor(float lampHeight){return Physics.clamp((lampHeight-MontroseHarbor.DOCK_RISE)/8,0,1);}
    static void draw(Game g,Painter p,float lx,float ly,float height,float radius,int color,float energy,float angle,int fixture){float above=heightFactor(height);if(above<=0)return;RiverCamera c=g.layout.camera;
        for(int y=cell(ly-radius);y<=cell(ly+radius);y++)for(int x=cell(lx-radius);x<=cell(lx+radius);x++){
            ArrayList<float[]> pieces=Geometry.CELLS.get(key(x,y));if(pieces==null)continue;
            float mx=(x+.5f)*CELL,my=(y+.5f)*CELL,dx=mx-lx,dy=my-ly;
            float power=energy*above*VesselShoreLight.falloff(dx*dx+dy*dy+(height-4)*(height-4),radius)*VesselShoreLight.sector(angle,fixture,dx,dy);
            int alpha=(int)(power*255);if(alpha<2 || !clear(lx,ly,mx,my,height))continue;
            for(float[] shape:pieces){float[] q=new float[shape.length];for(int i=0;i<q.length;i+=2){q[i]=c.screenX(shape[i],shape[i+1]);q[i+1]=c.screenY(shape[i],shape[i+1])-4*c.scale;}p.poly(q,(alpha<<24)|(color&0xffffff));}
        }
    }
    private static boolean clear(float x,float y,float xx,float yy,float height){int n=Math.max(1,(int)(Physics.length(xx-x,yy-y)/6));
        for(int i=1;i<n;i++){float t=i/(float)n,rayHeight=height+(4-height)*t;if(rayHeight<MontroseHarbor.LAND_RISE && !MontroseHarbor.openWater(x+(xx-x)*t,y+(yy-y)*t))return false;}return true;
    }
}
