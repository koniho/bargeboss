package com.bargeboss.game;

/** Montrose-inspired marina. Game distances/slips are enlarged for an 80-foot tow.
 * One geometry source drives collision, water effects, solid-shore cutouts and scenery. */
public final class MontroseHarbor {
    public static final float LAND_RISE=18,DOCK_RISE=4,ZOOM_MULTIPLIER=2;
    public static final float MAP_SCALE=1.5f;
    public static float mapX(float x){return x*MAP_SCALE-600;}
    public static float mapY(float y){return y*MAP_SCALE-1300;}
    public static float planX(float x){return (x+600)/MAP_SCALE;}
    public static float planY(float y){return (y+1300)/MAP_SCALE;}
    public static final float SLIP_X=mapX(1015),SLIP_Y=mapY(422),SLIP_ANGLE=-1.726f;
    // Ordered survey-image landmarks, traced from the official 1824x1368 dock map.
    // The two bottom/right extensions are open lake, outside the reference crop.
    private static final float[] COAST_MAP={210,1600,225,1368,242,1210,257,1050,255,980,238,932,205,891,155,843,111,769,84,691,76,620,83,551,108,479,146,410,194,359,255,323,332,296,455,277,650,248,820,220,1000,188,1185,153,1350,130,1440,125,1515,137,1585,165,1645,203,1695,252,1725,308,1736,365,1726,424,1703,479,1664,543,1623,599,1563,663,1494,730,1423,794,1348,851,1264,907,1176,949,1090,970,1005,977,930,973,853,955,779,928,716,894,653,853,596,811,554,783,527,777,498,783,469,800,441,824,419,855,405,894,401,935,409,981,426,1023,452,1064,491,1100,546,1128,634,1151,763,1180,910,1208,1049,1230,1150,1234,1256,1210,1360,1166,1468,1101,1567,1023,1665,925,1747,824,1824,700,1935,530,2050,370,2200,220};
    public static final float[] COAST=world(COAST_MAP),WATER_POLYGON=water(),LAND_POLYGON=land();
    private static final RayIndex WATER_RAYS=new RayIndex(WATER_POLYGON);
    public static final float[][] WATER_TRIANGLES=PolygonWater.triangles(WATER_POLYGON),LAND_TRIANGLES=PolygonWater.triangles(LAND_POLYGON);
    // Source-map spine endpoints drive docks and lights together; do not duplicate in rendering.
    private static final float[][] ROWS_MAP={{823,225,866,503},{951,204,995,485},{1135,174,1180,456},{1230,154,1270,424},{1331,144,1368,356},{1425,131,1453,283},
        {972,969,640,760},{1116,950,748,689},{1258,903,870,625},{1395,836,1018,586},{1525,741,1220,543},
        {1609,617,1404,470},{1673,524,1472,400},{1710,418,1547,303},{1718,287,1566,193}};
    private static final float[][] STARS_MAP={{475,380},{605,350},{490,505},{615,480},{725,450}};
    // Small service floats and land-connected access piers, traced from the north shore.
    private static final float[][] SERVICE_MAP={{415,300,625,270},{525,263,528,284},{764,225,772,267}};
    public static final int GREEN_INLET_ACCESS=ROWS_MAP.length+SERVICE_MAP.length-1;
    public static final String STAR_LETTERS="PRSTU";
    public static final float[][] STAR_CENTERS=starCenters();
    private static float[][] starCenters(){float[][] out=new float[STARS_MAP.length][];for(int i=0;i<out.length;i++)out[i]=world(STARS_MAP[i]);return out;}
    public static final float[][] DOCK_LINES=dockLines(),DOCKS=docks();
    /** Sloped access roots connect every long floating spine to the land walkway. */
    public static final float[][] ACCESS_LINES=accessLines();
    /** Fixed dock fixtures: world X/Y and unit spine direction X/Y. Between finger pairs. */
    public static final float[][] PEDESTALS=pedestals();
    private static final float[][] WATER_BOUNDS=bounds(WATER_TRIANGLES),DOCK_BOUNDS=bounds(DOCKS),LAND_BOUNDS=bounds(LAND_TRIANGLES);
    public static final float[][] MOORINGS=moorings();
    private static float[] world(float[] p){float[] out=p.clone();for(int i=0;i<p.length;i+=2){out[i]=mapX(p[i]);out[i+1]=mapY(p[i+1]);}return out;}
    private static float[] water(){return append(COAST,new float[]{10000,220,10000,10000,-10000,10000,-10000,1600});}
    private static float[] land(){return append(COAST,new float[]{10000,220,10000,-10000,-10000,-10000,-10000,1600});}
    private static float[] append(float[] first,float[] map){float[] tail=world(map),p=new float[first.length+tail.length];System.arraycopy(first,0,p,0,first.length);System.arraycopy(tail,0,p,first.length,tail.length);return p;}
    private static final float[] PLAY_BOUNDS=world(new float[]{-200,-200,2200,-200,2200,1600,-200,1600});
    private static final BoxIndex DOCK_INDEX=new BoxIndex(DOCK_BOUNDS),WATER_INDEX=new BoxIndex(WATER_BOUNDS);
    /** Fixed survey geometry, indexed once. Candidate IDs retain original clipping order. */
    private static final class BoxIndex {
        static final float CELL=128;
        final float left=PLAY_BOUNDS[0],top=PLAY_BOUNDS[1];
        final int cols=(int)StrictMath.ceil((PLAY_BOUNDS[2]-left)/CELL),rows=(int)StrictMath.ceil((PLAY_BOUNDS[5]-top)/CELL);
        final int[] all;final int[][] points,nearby;
        BoxIndex(float[][] bounds){
            all=new int[bounds.length];for(int i=0;i<all.length;i++)all[i]=i;
            points=new int[cols*rows][];nearby=new int[cols*rows][];
            int[] work=new int[bounds.length];
            for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)for(int margin=0;margin<=1;margin++){
                float l=left+(x-margin)*CELL,t=top+(y-margin)*CELL,r=left+(x+1+margin)*CELL,b=top+(y+1+margin)*CELL;int n=0;
                for(int i=0;i<bounds.length;i++){float[] q=bounds[i];if(q[0]<=r && q[2]>=l && q[1]<=b && q[3]>=t)work[n++]=i;}
                (margin==0?points:nearby)[y*cols+x]=java.util.Arrays.copyOf(work,n);
            }
        }
        private int cell(float x,float y){
            if(!(x>=left && y>=top && x<left+cols*CELL && y<top+rows*CELL))return -1;
            return (int)(((double)y-top)/CELL)*cols+(int)(((double)x-left)/CELL);
        }
        int[] point(float x,float y){int i=cell(x,y);return i<0?all:points[i];}
        int[] area(float[] b){
            if(!(b[2]-b[0]<=CELL && b[3]-b[1]<=CELL))return all;
            int i=cell((b[0]+b[2])*.5f,(b[1]+b[3])*.5f);return i<0?all:nearby[i];
        }
    }
    /** Ray casting needs only edges crossing this Y band; use the exact original arithmetic. */
    private static final class RayIndex {
        static final float CELL=128;
        final float[] polygon;final float top,bottom;final int[][] edges;
        RayIndex(float[] p){
            polygon=p;float[] bounds=PolygonWater.bounds(p);top=bounds[1];bottom=bounds[3];
            edges=new int[Math.max(1,(int)StrictMath.ceil((bottom-top)/CELL))][];int[] work=new int[p.length/2];
            for(int row=0;row<edges.length;row++){float lo=top+row*CELL,hi=lo+CELL;int n=0;
                for(int i=0,j=p.length-2;i<p.length;j=i,i+=2)if(Math.min(p[i+1],p[j+1])<=hi && Math.max(p[i+1],p[j+1])>=lo)work[n++]=i;
                edges[row]=java.util.Arrays.copyOf(work,n);
            }
        }
        boolean contains(float x,float y){
            if(!(y>=top && y<bottom))return false;
            float[] p=polygon;boolean in=false;int[] candidates=edges[(int)(((double)y-top)/CELL)];
            for(int i:candidates){int j=i==0?p.length-2:i-2;float ax=p[i],ay=p[i+1],bx=p[j],by=p[j+1];
                if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax)in=!in;
            }return in;
        }
    }
    private static float[][] bounds(float[][] p){float[][] b=new float[p.length][];for(int i=0;i<p.length;i++)b[i]=PolygonWater.bounds(p[i]);return b;}
    private static void line(java.util.ArrayList<float[]> lines,float x,float y,float xx,float yy,float width){lines.add(new float[]{mapX(x),mapY(y),mapX(xx),mapY(yy),width*MAP_SCALE});}
    private static float[][] dockLines(){
        java.util.ArrayList<float[]> lines=new java.util.ArrayList<float[]>();
        // O through J, then A through I. Endpoints retain the map's differing row lengths/angles.
        float[][] rows=ROWS_MAP;
        for(int i=0;i<rows.length;i++){
            float[] r=rows[i];
            float length=Physics.length(r[2]-r[0],r[3]-r[1]);
            line(lines,r[0]-(r[2]-r[0])/length*30,r[1]-(r[3]-r[1])/length*30,r[2],r[3],7);
            float dx=r[2]-r[0],dy=r[3]-r[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            for(float at=30;at<len-8;at+=30)for(int side=-1;side<=1;side+=2){
                float x=r[0]+dx*at,y=r[1]+dy*at;
                // N's lower east-side short fingers become one enlarged 80ft slip.
                if(i==1 && side==-1 && y>300)continue;
                float arm=i<2?42:i<6?28:32;
                line(lines,x,y,x-dy*side*arm,y+dx*side*arm,4);
            }
        }
        // Only this N-dock bay is enlarged: a back crossbar and parallel outer finger.
        line(lines,969,320,1034,320,5);line(lines,1034,320,1058,485,5);
        for(float[] star:STARS_MAP){
            for(int arm=0;arm<8;arm++){double a=arm*Math.PI/4;line(lines,star[0],star[1],star[0]+(float)StrictMath.cos(a)*42,star[1]+(float)StrictMath.sin(a)*42,5);}
        }
        for(int i=0;i<SERVICE_MAP.length;i++){float[] service=SERVICE_MAP[i];float dx=service[2]-service[0],dy=service[3]-service[1],len=Physics.length(dx,dy),extra=i==0?0:20;
            line(lines,service[0]-dx/len*extra,service[1]-dy/len*extra,service[2],service[3],7);}
        // Actual short breakwaters bracket the southwestern harbor mouth.
        line(lines,232,1247,369,1258,12);line(lines,585,1138,548,1290,12);
        return lines.toArray(new float[lines.size()][]);
    }
    private static float[][] accessLines(){java.util.ArrayList<float[]> out=new java.util.ArrayList<float[]>();
        for(float[] r:ROWS_MAP){float dx=r[2]-r[0],dy=r[3]-r[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            out.add(new float[]{mapX(r[0]-dx*15),mapY(r[1]-dy*15),mapX(r[0]+dx*10),mapY(r[1]+dy*10),7*MAP_SCALE});}
        for(int i=1;i<SERVICE_MAP.length;i++){float[] a=SERVICE_MAP[i];float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy);dx/=len;dy/=len;out.add(new float[]{mapX(a[0]-dx*12),mapY(a[1]-dy*12),mapX(a[0]+dx*8),mapY(a[1]+dy*8),7*MAP_SCALE});}
        for(float[] a:new float[][]{{232,1247,369,1258},{585,1138,548,1290}}){float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy);dx/=len;dy/=len;out.add(new float[]{mapX(a[0]-dx*15),mapY(a[1]-dy*15),mapX(a[0]+dx*10),mapY(a[1]+dy*10),12*MAP_SCALE});}
        // Survey roots are approximate; locate the actual bank along each spine so
        // every gate lands on the same continuous shore promenade.
        for(float[] a:out){float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            float x=a[0],y=a[1];int guard=0;
            if(openWater(x,y)){while(openWater(x,y) && guard++<300){x-=dx;y-=dy;}}
            else {while(!openWater(x,y) && guard++<300){x+=dx;y+=dy;}}
            a[0]=x-dx*15;a[1]=y-dy*15;a[2]=x+dx*15;a[3]=y+dy*15;
        }
        return out.toArray(new float[out.size()][]);
    }
    private static float[][] pedestals(){
        java.util.ArrayList<float[]> fixtures=new java.util.ArrayList<float[]>();
        for(float[] r:ROWS_MAP){float dx=r[2]-r[0],dy=r[3]-r[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            // Fingers are at 30,60,...; only emit between two existing finger stations.
            for(float at=45;at+15<len-8;at+=30)fixtures.add(new float[]{mapX(r[0]+dx*at),mapY(r[1]+dy*at),dx,dy});
        }
        for(float[] star:STARS_MAP)fixtures.add(new float[]{mapX(star[0]),mapY(star[1]),1,0});
        for(float[] service:SERVICE_MAP){float dx=service[2]-service[0],dy=service[3]-service[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            int count=Math.max(1,(int)(len/60));for(int i=0;i<count;i++){float at=len*(i+.5f)/count;fixtures.add(new float[]{mapX(service[0]+dx*at),mapY(service[1]+dy*at),dx,dy});}
        }
        return fixtures.toArray(new float[fixtures.size()][]);
    }
    private static float[][] docks(){float[][] out=new float[DOCK_LINES.length][];for(int i=0;i<out.length;i++){
        float[] l=DOCK_LINES[i];float d=Physics.length(l[2]-l[0],l[3]-l[1]),nx=-(l[3]-l[1])/d*l[4]/2,ny=(l[2]-l[0])/d*l[4]/2;
        out[i]=new float[]{l[0]+nx,l[1]+ny,l[2]+nx,l[3]+ny,l[2]-nx,l[3]-ny,l[0]-nx,l[1]-ny};}return out;}
    private static float[][] moorings(){java.util.ArrayList<float[]> p=new java.util.ArrayList<float[]>();for(int row=0;row<7;row++)for(int col=0;col<4;col++){
        float x=174+col*64+row*5,y=740-row*57-col*7;
        if(row==6 && col<2)continue;p.add(new float[]{mapX(x),mapY(y)});
    }return p.toArray(new float[p.size()][]);}
    public boolean wasTowed,entered;
    public int settledTicks;
    public static boolean openWater(float x,float y){return WATER_RAYS.contains(x,y);}
    public static boolean contains(float x,float y){if(!openWater(x,y))return false;for(int i:DOCK_INDEX.point(x,y)){float[] b=DOCK_BOUNDS[i];if(x>=b[0] && x<=b[2] && y>=b[1] && y<=b[3] && PolygonWater.contains(DOCKS[i],x,y))return false;}return true;}
    public static boolean clear(float x,float y,float xx,float yy){int steps=Math.max(1,(int)(Physics.length(xx-x,yy-y)/3));for(int i=0;i<=steps;i++)if(!contains(x+(xx-x)*i/steps,y+(yy-y)*i/steps))return false;return true;}
    public static float collide(Body b,CollisionSplashes splashes){float impact=0;for(int pass=0;pass<3;pass++){
        impact=Math.max(impact,PolygonWater.collide(b,PLAY_BOUNDS,true,splashes));
        impact=Math.max(impact,PolygonWater.collide(b,WATER_POLYGON,true,splashes));
        float e=b.half+b.radius;float[] region={b.x-e,b.y-e,b.x+e,b.y+e};
        for(int i=0;i<DOCKS.length;i++)if(PolygonWater.overlaps(region,DOCK_BOUNDS[i]))impact=Math.max(impact,PolygonWater.collide(b,DOCKS[i],false,splashes));
    }return impact;}
    public static boolean fits(Body b){float c=(float)StrictMath.cos(SLIP_ANGLE),s=(float)StrictMath.sin(SLIP_ANGLE);
        for(int i=0;i<3;i++){float dx=b.discX(i)-SLIP_X,dy=b.discY(i)-SLIP_Y;if(Math.abs(dx*c+dy*s)+b.radius>100 || Math.abs(-dx*s+dy*c)+b.radius>40)return false;}
        return Math.abs(StrictMath.sin(b.angle-SLIP_ANGLE))<.23f;
    }
    /** Clips water effects to exact shoreline triangles and oriented dock footprints. */
    static void paintWater(Painter p,float[] points,int color){float[] box=PolygonWater.bounds(points);int[] docks=DOCK_INDEX.area(box);
        for(int i:WATER_INDEX.area(box))if(PolygonWater.overlaps(box,WATER_BOUNDS[i])){
            float[] q=PolygonWater.clip(points,WATER_TRIANGLES[i]);if(q.length<6)continue;
            java.util.ArrayList<float[]> pieces=new java.util.ArrayList<float[]>();pieces.add(q);
            for(int h:docks)if(PolygonWater.overlaps(box,DOCK_BOUNDS[h])){
                java.util.ArrayList<float[]> next=new java.util.ArrayList<float[]>();for(float[] piece:pieces)PolygonWater.subtract(piece,DOCKS[h],next);pieces=next;
            }
            for(float[] piece:pieces)p.poly(piece,color);
        }
    }
    private static final class Floats {
        float[] values=new float[96];int count;
        void triangle(float ax,float ay,float bx,float by,float cx,float cy){
            if(count+6>values.length)values=java.util.Arrays.copyOf(values,Math.max(count+6,values.length*2));
            values[count++]=ax;values[count++]=ay;values[count++]=bx;values[count++]=by;values[count++]=cx;values[count++]=cy;
        }
        float[] finish(){return java.util.Arrays.copyOf(values,count);}
    }
    static float[] projected(float[][] shapes,RiverCamera c,float l,float t,float r,float b,float depth){return projected(shapes,c,l,t,r,b,depth,0);}
    static float[] projected(float[][] shapes,RiverCamera c,float l,float t,float r,float b,float depth,float rise){
        Floats result=new Floats();
        float[][] boxes=shapes==LAND_TRIANGLES?LAND_BOUNDS:shapes==DOCKS?DOCK_BOUNDS:shapes==WATER_TRIANGLES?WATER_BOUNDS:shapes==MontroseShoreArt.LAND_TOPS?MontroseShoreArt.LAND_TOP_BOUNDS:shapes==MontroseShoreArt.topFootprints()?MontroseShoreArt.STEP_BOUNDS:null;
        for(int shapeIndex=0;shapeIndex<shapes.length;shapeIndex++){
            float[] shape=shapes[shapeIndex],box=boxes==null?PolygonWater.bounds(shape):boxes[shapeIndex];
            if(l>=box[2] || r<=box[0] || t>=box[3] || b<=box[1])continue;
            float[] q=PolygonWater.rectangle(shape,l,t,r,b);
            if(q.length<6)continue;for(int i=0;i<q.length;i+=2){float x=c.screenX(q[i],q[i+1]);q[i+1]=c.screenY(q[i],q[i+1])-rise*c.scale;q[i]=x;}
            if(depth!=Float.NEGATIVE_INFINITY)q=PolygonWater.edge(q,0,depth-rise*c.scale,1,depth-rise*c.scale,true);
            for(int i=2;i<q.length-2;i+=2)result.triangle(q[0],q[1],q[i],q[i+1],q[i+2],q[i+3]);
        }
        return result.finish();
    }
    /** Camera-facing edges never change with pan/zoom. Prepare geometry and bounds once. */
    private static final float[][] SHORE_FACES=shoreFaces();
    private static float[][] shoreFaces(){java.util.ArrayList<float[]> out=new java.util.ArrayList<float[]>();
        for(int group=0;group<2;group++){
            int n=group==0?COAST.length/2-1:DOCKS.length*4;
            for(int k=0;k<n;k++){
                float ax,ay,bx,by,rise;
                if(group==0){ax=COAST[k*2];ay=COAST[k*2+1];bx=COAST[k*2+2];by=COAST[k*2+3];rise=LAND_RISE;}
                else{float[] d=DOCKS[k/4];int i=k%4*2,j=(i+2)%8;ax=d[i];ay=d[i+1];bx=d[j];by=d[j+1];rise=DOCK_RISE;}
                float dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy);if(len<.001f)continue;
                float nx=dy/len,ny=-dx/len;
                if(group==0){if(!openWater((ax+bx)/2+nx*8,(ay+by)/2+ny*8)){nx=-nx;ny=-ny;}}
                else if(PolygonWater.area(DOCKS[k/4])<0){nx=-nx;ny=-ny;}
                if(ny-nx<=0)continue;
                out.add(new float[]{ax,ay,bx,by,rise,dx,dy,Math.min(ax,bx),Math.min(ay,by),Math.max(ax,bx),Math.max(ay,by)});
            }
        }
        return out.toArray(new float[out.size()][]);
    }
    /** At most 32 exact masks / 256KiB float payload per render thread. Geometry is immutable. */
    private static final class ShoreCache {
        RiverCamera camera;final float[][] keys=new float[32][6],masks=new float[32][];int next,retained;
        float[] get(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){
            if(camera!=c){java.util.Arrays.fill(masks,null);camera=c;next=retained=0;return null;}
            for(int i=0;i<masks.length;i++){float[] key=keys[i];if(masks[i]!=null && key[0]==l && key[1]==t && key[2]==r && key[3]==b && key[4]==depth && key[5]==(union?1:0))return masks[i];}
            return null;
        }
        void put(float l,float t,float r,float b,float depth,boolean union,float[] mask){
            if(mask.length>65536)return;
            if(masks[next]!=null){retained-=masks[next].length;masks[next]=null;}
            while(retained+mask.length>65536){if(masks[next]!=null){retained-=masks[next].length;masks[next]=null;}next=(next+1)%masks.length;}
            float[] key=keys[next];key[0]=l;key[1]=t;key[2]=r;key[3]=b;key[4]=depth;key[5]=union?1:0;masks[next]=mask;retained+=mask.length;next=(next+1)%masks.length;
        }
    }
    private static final ThreadLocal<ShoreCache> SHORE_CACHE=new ThreadLocal<ShoreCache>(){
        protected ShoreCache initialValue(){return new ShoreCache();}
    };
    static float[] shore(RiverCamera c,float l,float t,float r,float b,float depth){
        return shore(c,l,t,r,b,depth,false);
    }
    static float[] shore(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){
        ShoreCache cache=SHORE_CACHE.get();float[] mask=cache.get(c,l,t,r,b,depth,union);
        if(mask!=null)return mask;mask=buildShore(c,l,t,r,b,depth,union);cache.put(l,t,r,b,depth,union,mask);return mask;
    }
    private static float[] buildShore(RiverCamera c,float l,float t,float r,float b,float depth,boolean union){
        float[] land=projected(MontroseShoreArt.LAND_TOPS,c,l,t,r,b,depth,LAND_RISE),dock=projected(DOCKS,c,l,t,r,b,depth,DOCK_RISE),steps=MontroseShoreArt.stepMask(c,l,t,r,b,depth),access=MontroseShoreArt.accessMask(c,l,t,r,b,depth);
        java.util.ArrayList<float[]> faces=new java.util.ArrayList<float[]>();int count=land.length+dock.length+steps.length+access.length;
        for(float[] face:SHORE_FACES){
                if(face[7]>r || face[9]<l || face[8]>b || face[10]<t)continue;
                float ax=face[0],ay=face[1],bx=face[2],by=face[3],rise=face[4],dx=face[5],dy=face[6];
                if(rise==LAND_RISE){float len=Physics.length(dx,dy),nx=-dy/len,ny=dx/len;if(openWater((ax+bx)*.5f+nx*2,(ay+by)*.5f+ny*2)){nx=-nx;ny=-ny;}
                    if(MontroseShoreArt.terraced((ax+bx)*.5f+nx*2,(ay+by)*.5f+ny*2))continue;}

                float start=0,end=1;
                for(int axis=0;axis<2;axis++){
                    float delta=axis==0?dx:dy,origin=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                    if(Math.abs(delta)<.001f){if(origin<min || origin>max){end=-1;break;}}
                    else{float low=(min-origin)/delta,high=(max-origin)/delta;start=Math.max(start,Math.min(low,high));end=Math.min(end,Math.max(low,high));}
                }
                if(end<=start)continue;
                float ground=c.screenY(ax,ay),slope=c.screenY(bx,by)-ground;
                // Clip the physical ground edge before extruding height. A projected cut through
                // the wall would leave the lower pixels of a background edge in front of a hull.
                if(depth!=Float.NEGATIVE_INFINITY){
                    if(Math.abs(slope)<.00001f){if(ground<depth)continue;}
                    else if(slope>0)start=Math.max(start,(depth-ground)/slope);
                    else end=Math.min(end,(depth-ground)/slope);
                }
                if(end<=start)continue;
                float x=c.screenX(ax+dx*start,ay+dy*start),y=c.screenY(ax+dx*start,ay+dy*start);
                float xx=c.screenX(ax+dx*end,ay+dy*end),yy=c.screenY(ax+dx*end,ay+dy*end),h=rise*c.scale;
                float[] q={x,y,xx,yy,xx,yy-h,x,y-h};
                faces.add(q);count+=12;
        }
        float[] result=new float[count];System.arraycopy(land,0,result,0,land.length);System.arraycopy(dock,0,result,land.length,dock.length);System.arraycopy(steps,0,result,land.length+dock.length,steps.length);System.arraycopy(access,0,result,land.length+dock.length+steps.length,access.length);int at=land.length+dock.length+steps.length+access.length;
        for(float[] q:faces)for(int i=2;i<q.length-2;i+=2){result[at++]=q[0];result[at++]=q[1];result[at++]=q[i];result[at++]=q[i+1];result[at++]=q[i+2];result[at++]=q[i+3];}
        if(!union)return disjoint(result);
        // Nonzero winding includes overlapping receivers without re-triangulating their union.
        for(int i=0;i<result.length;i+=6)if(PolygonWater.cross(result[i],result[i+1],result[i+2],result[i+3],result[i+4],result[i+5])<0){
            float x=result[i+2],y=result[i+3];result[i+2]=result[i+4];result[i+3]=result[i+5];result[i+4]=x;result[i+5]=y;
        }
        return result;
    }
    /** Painter's even-odd native cutout requires nonoverlapping holes. Dock junctions and
     * raised receivers overlap, so retain their union as disjoint convex pieces before fanning. */
    private static float[] disjoint(float[] triangles){
        java.util.ArrayList<float[]> pieces=new java.util.ArrayList<float[]>(),blockers=new java.util.ArrayList<float[]>(),boxes=new java.util.ArrayList<float[]>();
        for(int at=0;at<triangles.length;at+=6){
            float[] source={triangles[at],triangles[at+1],triangles[at+2],triangles[at+3],triangles[at+4],triangles[at+5]};
            if(!solid(source))continue;float[] box=PolygonWater.bounds(source);
            java.util.ArrayList<float[]> pending=new java.util.ArrayList<float[]>();pending.add(source);
            for(int i=0;i<blockers.size() && !pending.isEmpty();i++){
                if(!PolygonWater.overlaps(box,boxes.get(i)))continue;
                java.util.ArrayList<float[]> next=new java.util.ArrayList<float[]>();
                for(float[] candidate:pending){
                    if(!PolygonWater.overlaps(PolygonWater.bounds(candidate),boxes.get(i))){next.add(candidate);continue;}
                    PolygonWater.subtract(candidate,blockers.get(i),next);
                }
                for(int j=next.size()-1;j>=0;j--)if(!solid(next.get(j)))next.remove(j);
                pending=next;
            }
            for(float[] candidate:pending)if(solid(candidate))pieces.add(candidate);
            blockers.add(source);boxes.add(box);
        }
        int count=0;for(float[] piece:pieces)count+=(piece.length/2-2)*6;float[] result=new float[count];int at=0;
        for(float[] p:pieces)for(int i=2;i<p.length-2;i+=2){result[at++]=p[0];result[at++]=p[1];result[at++]=p[i];result[at++]=p[i+1];result[at++]=p[i+2];result[at++]=p[i+3];}
        return result;
    }
    private static boolean solid(float[] p){
        if(p.length<6)return false;double area=0;
        for(int i=2;i<p.length-2;i+=2)area+=(double)(p[i]-p[0])*(p[i+3]-p[1])-(double)(p[i+2]-p[0])*(p[i+1]-p[1]);
        return Math.abs(area)>.0001;
    }
    public void setup(Game g){
        g.tug.x=mapX(453);g.tug.y=mapY(1340);g.tug.angle=-(float)Math.PI/2;
        g.cargo[0]=new Body(g.nextId++,2,mapX(453),mapY(1417),-(float)Math.PI/2,true);
        g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y+55,Game.TRANSIT_ZOOM*ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    public String status(Game g){
        if(!wasTowed)return "DRAG FROM YACHT TO TUG / RELEASE TO LINK";
        if(!entered)return "TOW THROUGH THE RED AND GREEN INLET";
        if(settledTicks>0)return "HOLD STEADY IN THE SLIP";
        return "TOW INTO THE GLOWING SLIP / SLOW TO DOCK";
    }
    public void step(Game g){
        Body b=g.cargo[0];if(b==null || !b.active || b.deliveryRecorded)return;
        boolean towing=g.tow.connected(b,g.tug);
        if(towing)wasTowed=true;
        if(towing && planY(b.y)<1050)entered=true;
        if(entered && fits(b) && b.speed()<6 && Math.abs(b.spin)<.12f)settledTicks++;else settledTicks=0;
        if(settledTicks==90){
            b.deliveryRecorded=true;g.delivered=1;g.score+=1200;g.celebrationTicks=90;
            g.celebrationX=b.x;g.celebrationY=b.y;g.celebrationPoints=1200;
            g.haptic(Game.HAPTIC_SUCCESS);g.tell("YACHT SAFELY IN ITS SLIP");
        }
    }
    /** Fit the complete footprint; gameplay retains its close, following camera. */
    public static Layout overview(float width,float height){
        RiverCamera c=RiverCamera.DEFAULT;float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(float[] a:new float[][]{{0,90,1824,1460}})for(int k=0;k<4;k++){
            float wx=mapX(a[k%2==0?0:2]),wy=mapY(a[k/2==0?1:3]);
            float x=c.screenX(wx,wy)/c.scale,y=c.screenY(wx,wy)/c.scale;
            l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y);b=Math.max(b,y);
        }
        Layout base=new Layout(width,height);
        float scale=Math.min((width-190)/(r-l),(height-180)/(b-t));
        return Layout.focused(width,height,c.worldX((l+r)*.5f*c.scale,(t+b)*.5f*c.scale),c.worldY((l+r)*.5f*c.scale,(t+b)*.5f*c.scale),scale/base.camera.scale,width*.5f,height*.52f);
    }
}
