package com.bargeboss.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local queries against the surveyed river. Geometry is shared, immutable and camera independent. */
final class HarborPolygonGeometry {
    static final int CELL=256,DRY_CACHE_LIMIT=128;
    private final float[] coast;
    private final float[][] water,waterBounds,edges,edgeBounds;
    private final Index waterIndex,edgeIndex;
    private final float maxX,maxY,openMinX,openMinY;
    private final LinkedHashMap<Long,float[][]> DRY=new LinkedHashMap<Long,float[][]>(64,.75f,true){
        protected boolean removeEldestEntry(Map.Entry<Long,float[][]> e){return size()>DRY_CACHE_LIMIT;}
    };
    private final ThreadLocal<MaskCache> maskCache=new ThreadLocal<MaskCache>(){protected MaskCache initialValue(){return new MaskCache();}};
    private static final class MaskCache {
        RiverCamera camera;final float[][] keys=new float[32][6],masks=new float[32][];int next,retained;
        float[] get(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){
            if(camera!=c){Arrays.fill(masks,null);camera=c;next=retained=0;return null;}
            for(int i=0;i<masks.length;i++){float[] k=keys[i];if(masks[i]!=null && k[0]==l && k[1]==t && k[2]==r && k[3]==b && k[4]==depth && k[5]==(union?1:0))return masks[i];}return null;
        }
        void put(float l,float t,float r,float b,float depth,boolean union,float[] mask){if(mask.length>65536)return;
            if(masks[next]!=null){retained-=masks[next].length;masks[next]=null;}
            while(retained+mask.length>65536){if(masks[next]!=null){retained-=masks[next].length;masks[next]=null;}next=(next+1)%masks.length;}
            float[] k=keys[next];k[0]=l;k[1]=t;k[2]=r;k[3]=b;k[4]=depth;k[5]=union?1:0;masks[next]=mask;retained+=mask.length;next=(next+1)%masks.length;
        }
    }
    HarborPolygonGeometry(float[] coast,float[][] water,float maxX,float maxY){this(coast,water,maxX,maxY,Float.NaN);}
    HarborPolygonGeometry(float[] coast,float[][] water,float maxX,float maxY,float openMinX){this(coast,water,maxX,maxY,openMinX,Float.NaN);}
    HarborPolygonGeometry(float[] coast,float[][] water,float maxX,float maxY,float openMinX,float openMinY){
        this.coast=coast;this.water=water;this.maxX=maxX;this.maxY=maxY;this.openMinX=openMinX;this.openMinY=openMinY;
        waterBounds=bounds(water);waterIndex=new Index(waterBounds);
        edges=edges();edgeBounds=bounds(edges);edgeIndex=new Index(edgeBounds);
    }
    private static float[][] bounds(float[][] shapes){float[][] result=new float[shapes.length][];for(int i=0;i<shapes.length;i++)result[i]=PolygonWater.bounds(shapes[i]);return result;}
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    /** Exact floor for in-range cells without Android's bit-level StrictMath.floor. */
    private static int cell(float v){float q=v/CELL;int i=(int)q;return q<i?i-1:i;}
    /** Immutable bucket contents; query scratch is per thread and never escapes. */
    static final class Index {
        private static final int[] EMPTY=new int[0];
        final HashMap<Long,int[]> buckets=new HashMap<Long,int[]>();
        final int count;
        final ThreadLocal<Scratch> scratch;
        Index(float[][] boxes){count=boxes.length;
            HashMap<Long,ArrayList<Integer>> build=new HashMap<Long,ArrayList<Integer>>();
            for(int i=0;i<boxes.length;i++){float[] b=boxes[i];for(int x=cell(b[0]);x<=cell(b[2]);x++)for(int y=cell(b[1]);y<=cell(b[3]);y++){
                Long k=key(x,y);ArrayList<Integer> values=build.get(k);if(values==null){values=new ArrayList<Integer>();build.put(k,values);}values.add(i);
            }}
            for(Map.Entry<Long,ArrayList<Integer>> e:build.entrySet()){int[] v=new int[e.getValue().size()];for(int i=0;i<v.length;i++)v[i]=e.getValue().get(i);buckets.put(e.getKey(),v);}
            scratch=new ThreadLocal<Scratch>(){protected Scratch initialValue(){return new Scratch(count);}};
        }
        int[] area(float l,float t,float r,float b){int x0=cell(l),x1=cell(r),y0=cell(t),y1=cell(b);
            if(x0==x1 && y0==y1){int[] v=buckets.get(key(x0,y0));return v==null?EMPTY:v;}
            Scratch s=scratch.get();if(++s.generation==0){Arrays.fill(s.seen,0);s.generation=1;}int n=0;
            for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++){int[] v=buckets.get(key(x,y));if(v==null)continue;
                for(int i:v)if(s.seen[i]!=s.generation){s.seen[i]=s.generation;s.ids[n++]=i;}}
            Arrays.sort(s.ids,0,n);return n==0?EMPTY:Arrays.copyOf(s.ids,n);
        }
        private static final class Scratch {final int[] seen,ids;int generation;Scratch(int n){seen=new int[n];ids=new int[n];}}
    }
    /** Interior point lookup visits only local triangle buckets, not the full coastline. */
    boolean contains(float x,float y){
        for(int i:waterIndex.area(x,y,x,y)){float[] b=waterBounds[i];if(x<b[0] || x>b[2] || y<b[1] || y>b[3])continue;
            float[] p=water[i];boolean positive=PolygonWater.area(p)>0,inside=true;
            for(int j=0;j<p.length;j+=2){int next=(j+2)%p.length;float cross=PolygonWater.cross(p[j],p[j+1],p[next],p[next+1],x,y);if(positive?cross<-.001f:cross>.001f){inside=false;break;}}
            if(inside)return true;
        }return false;
    }
    int waterCandidates(float l,float t,float r,float b){return waterIndex.area(l,t,r,b).length;}
    void paintWater(Painter p,float[] points,int color){if(points.length<6 || (color>>>24)==0)return;
        float[] box=PolygonWater.bounds(points);
        for(int i:waterIndex.area(box[0],box[1],box[2],box[3]))if(PolygonWater.overlaps(box,waterBounds[i])){
            float[] q=PolygonWater.clip(points,water[i]);if(solid(q))p.poly(q,color);
        }
    }
    private synchronized float[][] dryCell(int col,int row){Long key=key(col,row);float[][] cached=DRY.get(key);if(cached!=null)return cached;
        float l=col*CELL,t=row*CELL,r=l+CELL,b=t+CELL;ArrayList<float[]> pieces=new ArrayList<float[]>();pieces.add(new float[]{l,t,r,t,r,b,l,b});
        float[] box={l,t,r,b};
        for(int i:waterIndex.area(l,t,r,b))if(PolygonWater.overlaps(box,waterBounds[i])){
            ArrayList<float[]> next=new ArrayList<float[]>();
            for(float[] piece:pieces){if(PolygonWater.overlaps(PolygonWater.bounds(piece),waterBounds[i]))PolygonWater.subtract(piece,water[i],next);else next.add(piece);}
            for(int j=next.size()-1;j>=0;j--)if(!solid(next.get(j)))next.remove(j);pieces=next;if(pieces.isEmpty())break;
        }
        cached=pieces.toArray(new float[pieces.size()][]);DRY.put(key,cached);return cached;
    }
    /** Convex dry pieces, bounded to the query and disjoint; callers may safely transform the result. */
    float[][] dry(float l,float t,float r,float b){ArrayList<float[]> result=new ArrayList<float[]>();
        if(r<=l || b<=t)return new float[0][];
        for(int x=cell(l);x<=cell(r);x++)for(int y=cell(t);y<=cell(b);y++)for(float[] p:dryCell(x,y)){
            float[] q=PolygonWater.rectangle(p,l,t,r,b);if(solid(q))result.add(q);
        }return result.toArray(new float[result.size()][]);
    }
    int dryCacheSize(){synchronized(this){return DRY.size();}}
    private float[][] edges(){ArrayList<float[]> out=new ArrayList<float[]>();
        for(int i=0;i<coast.length;i+=2){int j=(i+2)%coast.length;float ax=coast[i],ay=coast[i+1],bx=coast[j],by=coast[j+1];
            if(Math.abs(ax-bx)+Math.abs(ay-by)<.01f || !shoreEdge(ax,ay,bx,by))continue;
            out.add(new float[]{ax,ay,bx,by});
        }return out.toArray(new float[out.size()][]);
    }
    /** The clipped outer lake edge is a play bound, not a seawall. */
    boolean shoreEdge(float ax,float ay,float bx,float by){
        return !(Math.abs(ax-maxX)<.1f && Math.abs(bx-maxX)<.1f
                || Math.abs(ay-maxY)<.1f && Math.abs(by-maxY)<.1f
                || Math.abs(ax-openMinX)<.1f && Math.abs(bx-openMinX)<.1f
                || Math.abs(ay-openMinY)<.1f && Math.abs(by-openMinY)<.1f);
    }
    float[] tops(RiverCamera c,float l,float t,float r,float b,float depth){ArrayList<float[]> pieces=new ArrayList<float[]>();
        float rise=GroundArt.LAND_RISE*c.scale;
        for(float[] q:dry(l,t,r,b)){for(int i=0;i<q.length;i+=2){float sx=c.screenX(q[i],q[i+1]);q[i+1]=c.screenY(q[i],q[i+1])-rise;q[i]=sx;}
            if(depth!=Float.NEGATIVE_INFINITY)q=PolygonWater.edge(q,0,depth-rise,1,depth-rise,true);
            if(solid(q))pieces.add(q);
        }return fan(pieces);
    }
    float[] shore(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){MaskCache cache=maskCache.get();
        float[] value=cache.get(c,l,t,r,b,depth,union);if(value!=null)return value;
        value=depth==Float.NEGATIVE_INFINITY && union?worldMask.get().project(this,c,l,t,r,b):buildShore(c,l,t,r,b,depth,union);
        cache.put(l,t,r,b,depth,union,value);return value;
    }
    /** Full-depth union masks (the per-frame water surface clip) are camera-affine: world
     * triangles with a raised/ground flag, rebuilt only when the padded world rect no longer
     * covers the view. A moving camera then only re-projects instead of re-clipping land. */
    private final ThreadLocal<WorldMask> worldMask=new ThreadLocal<WorldMask>(){protected WorldMask initialValue(){return new WorldMask();}};
    private static final class WorldMask {
        float l,t,r,b;float[] world;boolean ready;
        float[] project(HarborPolygonGeometry geo,RiverCamera c,float ql,float qt,float qr,float qb){
            float w=qr-ql,h=qb-qt;
            if(!ready || ql<l || qt<t || qr>r || qb>b || (r-l)*(b-t)>9*w*h){
                l=ql-w*.5f;t=qt-h*.5f;r=qr+w*.5f;b=qb+h*.5f;world=geo.worldShore(l,t,r,b);ready=true;}
            float rise=GroundArt.LAND_RISE*c.scale;float[] out=new float[world.length/3*2];int at=0;
            for(int i=0;i<world.length;i+=9){
                // Only triangles touching the query rect; the padding exists for reuse, not drawing.
                float x0=world[i],x1=world[i+3],x2=world[i+6],y0=world[i+1],y1=world[i+4],y2=world[i+7];
                if(x0<ql && x1<ql && x2<ql || x0>qr && x1>qr && x2>qr || y0<qt && y1<qt && y2<qt || y0>qb && y1>qb && y2>qb)continue;
                float ax=c.screenX(world[i],world[i+1]),ay=c.screenY(world[i],world[i+1])-world[i+2]*rise;
                float bx=c.screenX(world[i+3],world[i+4]),by=c.screenY(world[i+3],world[i+4])-world[i+5]*rise;
                float cx=c.screenX(world[i+6],world[i+7]),cy=c.screenY(world[i+6],world[i+7])-world[i+8]*rise;
                float area=PolygonWater.cross(ax,ay,bx,by,cx,cy);if(area<=.0001f && area>=-.0001f)continue;
                out[at++]=ax;out[at++]=ay;
                if(area<0){out[at++]=cx;out[at++]=cy;out[at++]=bx;out[at++]=by;}else{out[at++]=bx;out[at++]=by;out[at++]=cx;out[at++]=cy;}
            }
            float[] mask=at==out.length?out:Arrays.copyOf(out,at);sealRasterSeams(mask);return mask;
        }
    }
    /** World triangles (x,y,raised) of land tops and visible seawall faces inside a rect. */
    private float[] worldShore(float l,float t,float r,float b){ArrayList<float[]> tris=new ArrayList<float[]>();
        for(float[] q:dry(l,t,r,b))for(int i=2;i<q.length-2;i+=2)tris.add(new float[]{q[0],q[1],1,q[i],q[i+1],1,q[i+2],q[i+3],1});
        for(int i:edgeIndex.area(l,t,r,b)){float[] e=edges[i];float ax=e[0],ay=e[1],dx=e[2]-ax,dy=e[3]-ay,len=Physics.length(dx,dy);
            float nx=-dy/len,ny=dx/len;if(!contains(ax+dx*.5f+nx,ay+dy*.5f+ny)){nx=-nx;ny=-ny;}
            if(ny-nx<=0)continue;
            float start=0,end=1;
            for(int axis=0;axis<2;axis++){float d=axis==0?dx:dy,o=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                if(Math.abs(d)<.001f){if(o<min || o>max){end=-1;break;}}
                else{float low=(min-o)/d,high=(max-o)/d;start=Math.max(start,Math.min(low,high));end=Math.min(end,Math.max(low,high));}}
            if(end<=start)continue;
            float x=ax+dx*start,y=ay+dy*start,xx=ax+dx*end,yy=ay+dy*end;
            tris.add(new float[]{x,y,0,xx,yy,0,xx,yy,1});tris.add(new float[]{x,y,0,xx,yy,1,x,y,1});
        }
        float[] out=new float[tris.size()*9];for(int i=0;i<tris.size();i++)System.arraycopy(tris.get(i),0,out,i*9,9);return out;
    }
    private float[] buildShore(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){
        float[] top=tops(c,l,t,r,b,depth);ArrayList<float[]> pieces=new ArrayList<float[]>();
        for(int i=0;i<top.length;i+=6)pieces.add(Arrays.copyOfRange(top,i,i+6));
        for(int i:edgeIndex.area(l,t,r,b)){float[] e=edges[i];float ax=e[0],ay=e[1],dx=e[2]-ax,dy=e[3]-ay,len=Physics.length(dx,dy);
            float nx=-dy/len,ny=dx/len;if(!contains(ax+dx*.5f+nx,ay+dy*.5f+ny)){nx=-nx;ny=-ny;}
            if(ny-nx<=0)continue;
            float start=0,end=1;
            for(int axis=0;axis<2;axis++){float d=axis==0?dx:dy,o=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                if(Math.abs(d)<.001f){if(o<min || o>max){end=-1;break;}}
                else{float low=(min-o)/d,high=(max-o)/d;start=Math.max(start,Math.min(low,high));end=Math.min(end,Math.max(low,high));}}
            if(end<=start)continue;
            float ground=c.screenY(ax,ay),slope=c.screenY(ax+dx,ay+dy)-ground;
            if(depth!=Float.NEGATIVE_INFINITY){if(Math.abs(slope)<.00001f){if(ground<depth)continue;}
                else if(slope>0)start=Math.max(start,(depth-ground)/slope);else end=Math.min(end,(depth-ground)/slope);}
            if(end<=start)continue;
            float x=c.screenX(ax+dx*start,ay+dy*start),y=c.screenY(ax+dx*start,ay+dy*start);
            float xx=c.screenX(ax+dx*end,ay+dy*end),yy=c.screenY(ax+dx*end,ay+dy*end),h=GroundArt.LAND_RISE*c.scale;
            pieces.add(new float[]{x,y,xx,yy,xx,yy-h,x,y-h});
        }
        if(!union)pieces=disjoint(pieces);
        float[] mask=fan(pieces);
        if(union)sealRasterSeams(mask);
        return mask;
    }
    /** Conservative 1/8-screen-pixel guard prevents native hard clips from
     * rasterizing cracks between independently clipped geographic triangles.
     * Same positive winding is required by the union-capable painter contract. */
    /** Render-only: Math.sqrt is correctly rounded and uses the Android intrinsic. */
    private static void sealRasterSeams(float[] mask){float[] original=new float[6];
        for(int i=0;i<mask.length;i+=6){
            System.arraycopy(mask,i,original,0,6);
            for(int v=0;v<3;v++){
                int a=(v+2)%3*2,b=v*2,c=(v+1)%3*2;
                float dx=original[b]-original[a],dy=original[b+1]-original[a+1],ex=original[c]-original[b],ey=original[c+1]-original[b+1];
                float dl=(float)Math.sqrt(dx*dx+dy*dy),el=(float)Math.sqrt(ex*ex+ey*ey);if(dl<.0001f || el<.0001f)continue;
                float nx=dy/dl,ny=-dx/dl,mx=ey/el,my=-ex/el,den=1+nx*mx+ny*my;
                if(den<.01f)continue;
                float ox=(nx+mx)*.125f/den,oy=(ny+my)*.125f/den,len=(float)Math.sqrt(ox*ox+oy*oy);
                if(len>1){ox/=len;oy/=len;}
                mask[i+b]+=ox;mask[i+b+1]+=oy;
            }
        }
    }
    private static ArrayList<float[]> disjoint(ArrayList<float[]> sources){ArrayList<float[]> output=new ArrayList<float[]>(),blockers=new ArrayList<float[]>(),boxes=new ArrayList<float[]>();
        for(float[] source:sources){if(!solid(source))continue;float[] box=PolygonWater.bounds(source);ArrayList<float[]> pending=new ArrayList<float[]>();pending.add(source);
            for(int i=0;i<blockers.size()&&!pending.isEmpty();i++)if(PolygonWater.overlaps(box,boxes.get(i))){ArrayList<float[]> next=new ArrayList<float[]>();
                for(float[] piece:pending){if(PolygonWater.overlaps(PolygonWater.bounds(piece),boxes.get(i)))PolygonWater.subtract(piece,blockers.get(i),next);else next.add(piece);}
                for(int j=next.size()-1;j>=0;j--)if(!solid(next.get(j)))next.remove(j);pending=next;}
            output.addAll(pending);blockers.add(source);boxes.add(box);
        }return output;
    }
    private static boolean solid(float[] p){
        if(p.length<6)return false;
        float perimeter=0;for(int i=0;i<p.length;i+=2){int j=(i+2)%p.length;perimeter+=Physics.length(p[j]-p[i],p[j+1]-p[i+1]);}
        // Repeated geographic triangle subtraction can leave sub-millimetre
        // slivers. They have no visible surface but destabilize native path unions.
        return Math.abs(PolygonWater.area(p))>Math.max(.0001f,perimeter*.003f);
    }
    private static float[] fan(ArrayList<float[]> pieces){int n=0;for(float[] q:pieces)n+=(q.length/2-2)*6;float[] result=new float[n];int at=0;
        for(float[] q:pieces)for(int i=2;i<q.length-2;i+=2){float area=PolygonWater.cross(q[0],q[1],q[i],q[i+1],q[i+2],q[i+3]);if(Math.abs(area)<=.0001f)continue;boolean reverse=area<0;
            result[at++]=q[0];result[at++]=q[1];int a=reverse?i+2:i,b=reverse?i:i+2;result[at++]=q[a];result[at++]=q[a+1];result[at++]=q[b];result[at++]=q[b+1];}
        return at==result.length?result:Arrays.copyOf(result,at);
    }
}
