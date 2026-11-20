package com.bargeboss.game;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
/** Immutable disjoint dock-top cells, prepared before active drawing. */
final class DockTopLight {
    private static final float CELL=8;
    private static final class Geometry {static final Grid GRID=new Grid(make());}
    /** Fixed survey lookup: avoid boxed coordinate keys and hash collision trees per lamp. */
    private static final class Grid {
        final int left,top,right,bottom,columns;
        final float[][][] cells;
        Grid(Map<Long,ArrayList<float[]>> source){
            int l=Integer.MAX_VALUE,t=l,r=Integer.MIN_VALUE,b=r;
            for(long key:source.keySet()){int x=(int)(key>>32),y=(int)key;l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);}
            left=l;top=t;right=r;bottom=b;columns=r-l+1;
            long count=(long)columns*(b-t+1);
            if(count>262144)throw new IllegalStateException("Dock light grid exceeds fixed survey budget");
            cells=new float[(int)count][][];
            for(Map.Entry<Long,ArrayList<float[]>> entry:source.entrySet()){
                long key=entry.getKey();int x=(int)(key>>32),y=(int)key;ArrayList<float[]> pieces=entry.getValue();
                cells[(y-top)*columns+x-left]=pieces.toArray(new float[pieces.size()][]);
            }
        }
    }
    private DockTopLight(){}
    static void prepare(){int count=Geometry.GRID.cells.length;}
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
        Grid grid=Geometry.GRID;
        int left=Math.max(grid.left,cell(lx-radius)),right=Math.min(grid.right,cell(lx+radius)),top=Math.max(grid.top,cell(ly-radius)),bottom=Math.min(grid.bottom,cell(ly+radius));
        for(int y=top;y<=bottom;y++)for(int x=left;x<=right;x++){
            float[][] pieces=grid.cells[(y-grid.top)*grid.columns+x-grid.left];if(pieces==null)continue;
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
