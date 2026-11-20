package com.bargeboss.game;

import java.util.ArrayList;
import java.util.Collections;

/** Fixed geometry for connected marina decks and broad lakefront concrete terraces.
 * Prepared once, then culled and painted with shared surface materials. No native map decisions. */
public final class MontroseShoreArt {
    public static final int STEP_COUNT=5;
    public static final float TREAD=12,STEP_RISE=MontroseHarbor.LAND_RISE/STEP_COUNT;
    /** Every entry is a convex tread quad followed by its upper surface elevation. */
    static final float[][] STEPS=steps();
    private static final float[][] STEP_FOOTPRINTS=footprints();
    static final float[][] STEP_BOUNDS=bounds(STEP_FOOTPRINTS);
    private static final float[][][] STEP_SHAPES=stepShapes();
    private static final boolean[] STEP_FRONT=stepFront();
    static final float[][] PROMENADE=promenade();
    static final float[][] ACCESS=accessRoots();
    static final float[][] DOCK_EDGES=dockEdges();
    static final float[][] LAND_TOPS=landTops();
    static final float[][] LAND_TOP_BOUNDS=bounds(LAND_TOPS);
    private static final float[][] ACCESS_QUADS=accessQuads();
    private static final float[][] ACCESS_BOUNDS=bounds(ACCESS_QUADS);
    private static final float[][] ACCESS_GEOMETRY=accessGeometry();
    private static final float[][] DOCK_UVS=dockUVs();
    private static final int[] QUAD_TRIANGLES={0,2,4,0,4,6};
    // Authoritative dock-map labels in ACCESS order: north O..J, peninsula A..I,
    // service and inlet gates have no dock letter. Star floats have hub signs.
    private static final String GATE_LETTERS="ONMLKJABCDEFGHI    ";
    public static final String NAMED_DOCKS="ABCDEFGHIJKLMNOPRSTU";
    private static final float[][][] GLYPHS=glyphs();
    private static final ArticulatedModel[] GATES=gates();
    private static final ArticulatedModel.Pose[] GATE_POSES=gatePoses();
    private static final ArticulatedModel[] STAR_POSTS=starPosts();
    private static final ArticulatedModel.Pose[] STAR_POSES=starPoses();
    private MontroseShoreArt(){}
    private static float[][] bounds(float[][] shapes){float[][] out=new float[shapes.length][];for(int i=0;i<out.length;i++)out[i]=PolygonWater.bounds(shapes[i]);return out;}
    private static float[][][] stepShapes(){float[][][] out=new float[STEPS.length][][];for(int i=0;i<out.length;i++)out[i]=new float[][]{STEP_FOOTPRINTS[i]};return out;}
    private static boolean[] stepFront(){boolean[] out=new boolean[STEPS.length];for(int i=0;i<out.length;i++){float[] s=STEPS[i];float dx=s[2]-s[0],dy=s[3]-s[1],len=Physics.length(dx,dy),nx=dy/len,ny=-dx/len;
        if(!MontroseHarbor.openWater(s[0]+nx*80,s[1]+ny*80)){nx=-nx;ny=-ny;}out[i]=ny-nx>0;}return out;}
    private static float[][] accessQuads(){float[][] out=new float[ACCESS.length][];for(int i=0;i<out.length;i++){float[] a=ACCESS[i];float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy),nx=-dy/len*a[4]*.5f,ny=dx/len*a[4]*.5f;
        out[i]=new float[]{a[0]+nx,a[1]+ny,a[2]+nx,a[3]+ny,a[2]-nx,a[3]-ny,a[0]-nx,a[1]-ny};}return out;}
    private static float[][] accessGeometry(){float[][] out=new float[ACCESS.length][];for(int i=0;i<out.length;i++){float[] a=ACCESS[i];float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy);out[i]=new float[]{dx,dy,len,0,0,len,0,len,a[4],0,a[4]};}return out;}
    private static float[][] dockUVs(){float[][] out=new float[MontroseHarbor.DOCKS.length][];for(int i=0;i<out.length;i++){float[] q=MontroseHarbor.DOCKS[i];float length=Physics.length(q[2]-q[0],q[3]-q[1]),width=Physics.length(q[6]-q[0],q[7]-q[1]);out[i]=new float[]{0,0,length,0,length,width,0,width};}return out;}
    private static boolean overlaps(float[] box,float l,float t,float r,float b){return box[2]>=l && box[0]<=r && box[3]>=t && box[1]<=b;}
    /** Whole terrace cutout, with immutable triangulation and no repeated polygon clipping. */
    static float[] terraceCut(RiverCamera c){float[] out=new float[STEP_FOOTPRINTS.length*12];int at=0;
        for(float[] q:STEP_FOOTPRINTS)for(int k:QUAD_TRIANGLES){out[at++]=c.screenX(q[k],q[k+1]);out[at++]=c.screenY(q[k],q[k+1]);}return out;}
    private static float[][] steps(){
        float[] coast=MontroseHarbor.COAST;int first=-1,last=-1;
        for(int i=0;i<coast.length;i+=2){float x=MontroseHarbor.planX(coast[i]),y=MontroseHarbor.planY(coast[i+1]);
            if(Math.abs(x-546)<.1f && Math.abs(y-1128)<.1f)first=i;
            if(Math.abs(x-1824)<.1f && Math.abs(y-700)<.1f)last=i;}
        if(first<0 || last<=first)throw new IllegalStateException("Lakefront terrace endpoints missing");
        int n=(last-first)/2+1;float[][] edge=new float[n][4];
        for(int k=0;k<n;k++){int at=first+k*2;float x=coast[at],y=coast[at+1];
            int previous=Math.max(first,at-2),next=Math.min(last,at+2);
            float ax=x-coast[previous],ay=y-coast[previous+1],bx=coast[next]-x,by=coast[next+1]-y;
            if(k==0){ax=bx;ay=by;}if(k==n-1){bx=ax;by=ay;}
            float al=Physics.length(ax,ay),bl=Physics.length(bx,by),nx=-ay/al-by/bl,ny=ax/al+bx/bl;
            float length=Physics.length(nx,ny);nx/=length;ny/=length;
            if(MontroseHarbor.openWater(x+nx*8,y+ny*8)){nx=-nx;ny=-ny;}
            // Mitered offsets keep adjacent curved treads joined without cracks.
            float dot=Math.abs(nx*(-by/bl)+ny*(bx/bl)),gain=1/Math.max(.65f,dot);
            edge[k]=new float[]{x,y,nx*gain,ny*gain};
        }
        ArrayList<float[]> out=new ArrayList<float[]>();
        for(int step=0;step<STEP_COUNT;step++)for(int k=0;k<n-1;k++){
            float[] a=edge[k],b=edge[k+1];float near=step*TREAD,far=(step+1)*TREAD;
            out.add(new float[]{a[0]+a[2]*near,a[1]+a[3]*near,b[0]+b[2]*near,b[1]+b[3]*near,
                b[0]+b[2]*far,b[1]+b[3]*far,a[0]+a[2]*far,a[1]+a[3]*far,(step+1)*STEP_RISE});
        }
        return out.toArray(new float[out.size()][]);
    }
    private static float[][] promenade(){float[] coast=MontroseHarbor.COAST;int n=coast.length/2-2;float[][] vertices=new float[n][4];
        for(int k=0;k<n;k++){int at=(k+1)*2,prev=Math.max(2,at-2),next=Math.min(coast.length-4,at+2);float x=coast[at],y=coast[at+1];
            float ax=x-coast[prev],ay=y-coast[prev+1],bx=coast[next]-x,by=coast[next+1]-y;if(k==0){ax=bx;ay=by;}if(k==n-1){bx=ax;by=ay;}
            float al=Physics.length(ax,ay),bl=Physics.length(bx,by),nx=-ay/al-by/bl,ny=ax/al+bx/bl,len=Physics.length(nx,ny);nx/=len;ny/=len;
            if(MontroseHarbor.openWater(x+nx*8,y+ny*8)){nx=-nx;ny=-ny;}float gain=1/Math.max(.65f,Math.abs(nx*(-by/bl)+ny*bx/bl));nx*=gain;ny*=gain;
            // Keep the original survey transition (vertex 38) independent of northern coast length.
            // The lakeside promenade runs above the broad terrace, not across its steps.
            boolean lake=MontroseHarbor.planX(x)>=546 && MontroseHarbor.planY(y)>=1100 || MontroseHarbor.planX(x)>=1256 && MontroseHarbor.planY(y)>=700 && k>38;
            float offset=lake?STEP_COUNT*TREAD+12:15;
            offset+=MontroseEntranceShore.walkwayInset(MontroseHarbor.planX(x),MontroseHarbor.planY(y));
            vertices[k]=new float[]{x+nx*(offset-9),y+ny*(offset-9),x+nx*(offset+9),y+ny*(offset+9)};
        }
        float[][] out=new float[n-1][];for(int k=0;k<n-1;k++){float[] a=vertices[k],b=vertices[k+1];out[k]=new float[]{a[0],a[1],b[0],b[1],b[2],b[3],a[2],a[3]};}return out;
    }
    static void promenade(Game g,Painter p){for(float[] q:PROMENADE)if(!MontroseBeach.sandy(MontroseHarbor.planX(q[0]),MontroseHarbor.planY(q[1])) && visible(g,q,MontroseHarbor.LAND_RISE))surface(p,g.layout.camera,q,MontroseHarbor.LAND_RISE,SurfaceMaterial.CONCRETE,0xff92958b,0);}
    static void docks(Game g,Painter p){RiverCamera c=g.layout.camera;
        for(float[] e:DOCK_EDGES){if(visible(g,e,MontroseHarbor.DOCK_RISE))wall(p,c,e[0],e[1],e[2],e[3],0,MontroseHarbor.DOCK_RISE,SurfaceMaterial.CORRUGATED,0xff414747,.42f);}
        for(int dock=0;dock<MontroseHarbor.DOCKS.length;dock++){float[] q=MontroseHarbor.DOCKS[dock];if(!visible(g,q,MontroseHarbor.DOCK_RISE))continue;float[] xy=new float[8];for(int i=0;i<8;i+=2){xy[i]=c.screenX(q[i],q[i+1]);xy[i+1]=c.screenY(q[i],q[i+1])-MontroseHarbor.DOCK_RISE*c.scale;}
            p.surfacePolygon(xy,DOCK_UVS[dock],SurfaceMaterial.WOOD,0xff8b8170,.13f);
        }
    }
    static float[] quad(float[] step){return new float[]{step[0],step[1],step[2],step[3],step[4],step[5],step[6],step[7]};}
    static boolean terraced(float x,float y){for(int i=0;i<STEP_FOOTPRINTS.length;i++)if(overlaps(STEP_BOUNDS[i],x,y,x,y) && PolygonWater.contains(STEP_FOOTPRINTS[i],x,y))return true;return false;}
    static float riseAt(float x,float y){for(int i=0;i<STEPS.length;i++)if(overlaps(STEP_BOUNDS[i],x,y,x,y) && PolygonWater.contains(STEP_FOOTPRINTS[i],x,y))return STEPS[i][8];return MontroseHarbor.LAND_RISE;}
    static float[][] topFootprints(){return STEP_FOOTPRINTS;}
    private static float[][] footprints(){float[][] q=new float[STEPS.length][];for(int i=0;i<q.length;i++)q[i]=quad(STEPS[i]);return q;}
    private static float[][] landTops(){ArrayList<float[]> out=new ArrayList<float[]>();
        for(float[] land:MontroseHarbor.LAND_TRIANGLES){ArrayList<float[]> pieces=new ArrayList<float[]>();pieces.add(land);
            for(float[] s:STEPS){float[] q=quad(s),box=PolygonWater.bounds(q);ArrayList<float[]> next=new ArrayList<float[]>();
                for(float[] piece:pieces){if(PolygonWater.overlaps(PolygonWater.bounds(piece),box))PolygonWater.subtract(piece,q,next);else next.add(piece);}pieces=next;}
            out.addAll(pieces);
        }
        return out.toArray(new float[out.size()][]);
    }
    static boolean visible(Game g,float[] q,float rise){RiverCamera c=g.layout.camera;float l=Float.MAX_VALUE,t=l,r=-l,b=-l;
        for(int i=0;i<q.length;i+=2){float x=c.screenX(q[i],q[i+1]),y=c.screenY(q[i],q[i+1]);l=Math.min(l,x);r=Math.max(r,x);t=Math.min(t,y-rise*c.scale);b=Math.max(b,y);}
        return r>=-3 && l<=g.layout.width+3 && b>=-3 && t<=g.layout.height+3;
    }
    static void surface(Painter p,RiverCamera c,float[] q,float rise,int material,int color,float wetness){
        float[] xy=new float[q.length];for(int i=0;i<q.length;i+=2){xy[i]=c.screenX(q[i],q[i+1]);xy[i+1]=c.screenY(q[i],q[i+1])-rise*c.scale;}
        p.surfacePolygon(xy,q,material,color,wetness);
    }
    static void wall(Painter p,RiverCamera c,float ax,float ay,float bx,float by,float low,float high,int material,int color,float wetness){
        float x=c.screenX(ax,ay),y=c.screenY(ax,ay),xx=c.screenX(bx,by),yy=c.screenY(bx,by),s=c.scale;
        float u0=(Math.abs(bx-ax)>=Math.abs(by-ay)?ax:ay),u1=u0+Physics.length(bx-ax,by-ay);
        p.surfacePolygon(new float[]{x,y-low*s,xx,yy-low*s,xx,yy-high*s,x,y-high*s},
            new float[]{u0,low,u1,low,u1,high,u0,high},material,color,wetness);
    }
    /** Shared receiver masks use the same elevations as the opaque terraced surfaces. */
    static float[] stepMask(RiverCamera c,float l,float t,float r,float b,float depth){ArrayList<float[]> masks=new ArrayList<float[]>();int count=0;
        for(int index=0;index<STEPS.length;index++){if(!overlaps(STEP_BOUNDS[index],l,t,r,b))continue;
            float[] step=STEPS[index],mask=MontroseHarbor.projected(STEP_SHAPES[index],c,l,t,r,b,depth,step[8]);masks.add(mask);count+=mask.length;
            float ax=step[0],ay=step[1],bx=step[2],by=step[3],dx=bx-ax,dy=by-ay;
            if(!STEP_FRONT[index] || Math.max(ax,bx)<l || Math.min(ax,bx)>r || Math.max(ay,by)<t || Math.min(ay,by)>b)continue;
            float from=0,to=1;
            for(int axis=0;axis<2;axis++){float delta=axis==0?dx:dy,origin=axis==0?ax:ay,min=axis==0?l:t,max=axis==0?r:b;
                if(Math.abs(delta)<.00001f){if(origin<min || origin>max){to=-1;break;}}else{float aa=(min-origin)/delta,bb=(max-origin)/delta;from=Math.max(from,Math.min(aa,bb));to=Math.min(to,Math.max(aa,bb));}}
            float ground=c.screenY(ax,ay),slope=c.screenY(bx,by)-ground;
            if(depth!=Float.NEGATIVE_INFINITY){if(Math.abs(slope)<.00001f){if(ground<depth)continue;}else if(slope>0)from=Math.max(from,(depth-ground)/slope);else to=Math.min(to,(depth-ground)/slope);}
            if(to<=from)continue;float x=c.screenX(ax+dx*from,ay+dy*from),y=c.screenY(ax+dx*from,ay+dy*from),xx=c.screenX(ax+dx*to,ay+dy*to),yy=c.screenY(ax+dx*to,ay+dy*to),low=(step[8]-STEP_RISE)*c.scale,high=step[8]*c.scale;
            mask=new float[]{x,y-low,xx,yy-low,xx,yy-high,x,y-low,xx,yy-high,x,y-high};masks.add(mask);count+=12;
        }
        float[] result=new float[count];int at=0;for(float[] mask:masks){System.arraycopy(mask,0,result,at,mask.length);at+=mask.length;}return result;
    }
    static void terraces(Game g,Painter p){RiverCamera c=g.layout.camera;
        // Ground depth order: farther upper bands first, then the lower foreground risers.
        for(int i=STEPS.length-1;i>=0;i--){float[] s=STEPS[i],q=STEP_FOOTPRINTS[i];if(!visible(g,q,MontroseHarbor.LAND_RISE))continue;
            // The lowest waterline riser continues the hard shore sheet-pile termination.
            boolean waterline=s[8]<=STEP_RISE;
            wall(p,c,s[0],s[1],s[2],s[3],s[8]-STEP_RISE,s[8],waterline?SurfaceMaterial.CORRUGATED:SurfaceMaterial.CONCRETE,waterline?0xff59605e:0xff777b70,waterline?.35f:.12f);
            surface(p,c,q,s[8],SurfaceMaterial.CONCRETE,0xffa3a394,s[8]<=STEP_RISE?.3f:0);
        }
    }
    private static float[][] accessRoots(){float[][] out=new float[MontroseHarbor.ACCESS_LINES.length][];
        for(int i=0;i<out.length;i++){float[] a=MontroseHarbor.ACCESS_LINES[i].clone();float dx=a[2]-a[0],dy=a[3]-a[1],len=Physics.length(dx,dy);dx/=len;dy/=len;
            boolean found=false;for(int d=0;d<=180 && !found;d++)for(int sign:new int[]{1,-1}){float x=a[0]-dx*d*sign,y=a[1]-dy*d*sign;
                for(float[] q:PROMENADE)if(PolygonWater.contains(q,x,y)){a[0]=x;a[1]=y;found=true;break;}if(found)break;}
            if(!found)throw new IllegalStateException("Dock walkway attachment missing: "+i);out[i]=a;
        }return out;
    }
    public static char gateLetter(int gate){if(gate<0 || gate>=ACCESS.length)throw new IllegalArgumentException("Unknown gate");return GATE_LETTERS.charAt(gate);}
    public static int gateForLetter(char letter){int gate=letter==' '?-1:GATE_LETTERS.indexOf(letter);if(gate<0)throw new IllegalArgumentException("Unknown dock letter");return gate;}
    private static float[][][] glyphs(){float[][][] out=new float[ACCESS.length+MontroseHarbor.STAR_CENTERS.length][][];
        for(int i=0;i<out.length;i++){final ArrayList<float[]> cells=new ArrayList<float[]>();Painter collect=new Painter(){
            public void rect(float x,float y,float w,float h,int color){cells.add(new float[]{x,y,x+w,y,x+w,y+h,x,y+h});}
            public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
            public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float blend,float alpha){}
            public void circle(float x,float y,float r,int c){}public void poly(float[] q,int c){}public void line(float x,float y,float xx,float yy,float w,int c){}
        };Lettering.drawGlyphs(collect,String.valueOf(i<ACCESS.length?gateLetter(i):MontroseHarbor.STAR_LETTERS.charAt(i-ACCESS.length)),0,0,7,0xffffffff);out[i]=cells.toArray(new float[cells.size()][]);}return out;
    }
    /** Shared location API covers both shore gates and isolated floating hubs. */
    public static float[] dockLabelAnchor(char letter){int star=MontroseHarbor.STAR_LETTERS.indexOf(letter);if(star>=0)return MontroseHarbor.STAR_CENTERS[star].clone();float[] a=ACCESS[gateForLetter(letter)];return new float[]{a[0],a[1]};}
    private static void plaque(Game g,Painter p,int gate){if(gateLetter(gate)==' ')return;float[] a=ACCESS[gate];sign(g,p,a[0],a[1],MontroseHarbor.LAND_RISE,gate);}
    private static void sign(Game g,Painter p,float wx,float wy,float rise,int glyph){RiverCamera c=g.layout.camera;// Fixed world diagonal faces the shared isometric camera. This is an upright
        // physical sign on a post, independent of dock heading and never a HUD label.
        float nx=RiverCamera.Q,ny=RiverCamera.Q;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy)-(rise+28*ArticulatedModel.Z_SCALE)*c.scale;
        float ux=RiverCamera.Q*(nx+ny)*c.scale,uy=RiverCamera.Q*RiverCamera.DEPTH*(ny-nx)*c.scale,v=ArticulatedModel.Z_SCALE*c.scale;
        plaqueQuad(p,new float[]{-3.25f,-.75f,3.25f,-.75f,3.25f,7.75f,-3.25f,7.75f},x,y,ux,uy,v,0xffd3b567);
        plaqueQuad(p,new float[]{-2.9f,-.4f,2.9f,-.4f,2.9f,7.4f,-2.9f,7.4f},x,y,ux,uy,v,0xff202e30);
        for(float[] cell:GLYPHS[glyph]){float[] q=cell.clone();for(int k=0;k<q.length;k+=2)q[k]-=2.5f;plaqueQuad(p,q,x,y,ux,uy,v,0xfff5e5ac);}
    }
    private static ArticulatedModel[] starPosts(){ArticulatedModel[] out=new ArticulatedModel[MontroseHarbor.STAR_CENTERS.length];for(int i=0;i<out.length;i++){float[] center=MontroseHarbor.STAR_CENTERS[i];ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("star-hub-sign-"+MontroseHarbor.STAR_LETTERS.charAt(i),-1,center[0],center[1],MontroseHarbor.DOCK_RISE/ArticulatedModel.Z_SCALE,0,0);b.box(root,0,0,12,1.4f,1.4f,24,0xff65716e,SurfaceMaterial.STEEL);out[i]=b.build();}return out;}
    private static ArticulatedModel.Pose[] starPoses(){ArticulatedModel.Pose[] out=new ArticulatedModel.Pose[STAR_POSTS.length];for(int i=0;i<out.length;i++)out[i]=STAR_POSTS[i].pose(0);return out;}
    /** Called at the existing hub pedestal's depth; the floats have no gates. */
    static void starSign(Game g,Painter p,float x,float y){for(int i=0;i<MontroseHarbor.STAR_CENTERS.length;i++){float[] center=MontroseHarbor.STAR_CENTERS[i];if(center[0]==x && center[1]==y){STAR_POSTS[i].draw(g,p,STAR_POSES[i]);sign(g,p,x,y,MontroseHarbor.DOCK_RISE,ACCESS.length+i);return;}}}
    private static void plaqueQuad(Painter p,float[] q,float x,float y,float ux,float uy,float v,int color){float[] xy=new float[q.length];for(int i=0;i<q.length;i+=2){xy[i]=x+q[i]*ux;xy[i+1]=y+q[i]*uy+q[i+1]*v;}p.poly(xy,color);}
    public static int gateCount(){return GATES.length;}
    private static ArticulatedModel[] gates(){ArticulatedModel[] out=new ArticulatedModel[ACCESS.length];
        for(int i=0;i<out.length;i++){float[] a=ACCESS[i];float dx=a[2]-a[0],dy=a[3]-a[1],angle=(float)StrictMath.atan2(dy,dx),half=a[4]*.5f;
            ArticulatedModel.Builder b=new ArticulatedModel.Builder();int node=b.node("dock-access-"+i,-1,a[0],a[1],MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,angle,0);
            for(int side=-1;side<=1;side+=2){b.box(node,0,side*half,8,2,2,16,0xff606865,SurfaceMaterial.STEEL);b.box(node,0,side*half,16.8f,2.4f,2.4f,1.6f,0xffc49b46,SurfaceMaterial.SAFETY);
                // Short approach rails join the posts to the walkway edge.
                b.beam(node,-8,side*half,12,0,side*half,12,1.1f,0xffb19a5e,SurfaceMaterial.SAFETY);}
            b.beam(node,0,-half,13,0,half,13,1.3f,0xffb19a5e,SurfaceMaterial.SAFETY);
            b.beam(node,0,-half,4,0,half,4,1.1f,0xff66736f,SurfaceMaterial.STEEL);
            for(int bar=-1;bar<=1;bar++)b.beam(node,0,bar*half*.5f,4,0,bar*half*.5f,13,.7f,0xff66736f,SurfaceMaterial.STEEL);
            if(gateLetter(i)!=' ')b.beam(node,0,0,13,0,0,24,1.15f,0xff66736f,SurfaceMaterial.STEEL);
            out[i]=b.build();
        }return out;
    }
    private static ArticulatedModel.Pose[] gatePoses(){ArticulatedModel.Pose[] out=new ArticulatedModel.Pose[GATES.length];for(int i=0;i<out.length;i++)out[i]=GATES[i].pose(0);return out;}
    static void gates(Game g,Painter p){for(int i=0;i<GATES.length;i++){float[] a=ACCESS[i];if(visible(g,new float[]{a[0]-15,a[1]-15,a[0]+15,a[1]+15},40)){GATES[i].draw(g,p,GATE_POSES[i]);plaque(g,p,i);}}}
    static float[] accessMask(RiverCamera c,float l,float t,float r,float b,float depth){ArrayList<Float> out=new ArrayList<Float>();
        for(int index=0;index<ACCESS.length;index++){if(!overlaps(ACCESS_BOUNDS[index],l,t,r,b))continue;
            float[] a=ACCESS[index],geometry=ACCESS_GEOMETRY[index];float dx=geometry[0],dy=geometry[1],len=geometry[2];
            float[] q=PolygonWater.rectangle(ACCESS_QUADS[index],l,t,r,b);
            if(depth!=Float.NEGATIVE_INFINITY)q=PolygonWater.edge(q,c.worldX(0,depth),c.worldY(0,depth),c.worldX(1,depth),c.worldY(1,depth),true);
            for(int i=0;i<q.length;i+=2){float u=((q[i]-a[0])*dx+(q[i+1]-a[1])*dy)/(len*len),rise=MontroseHarbor.LAND_RISE+(MontroseHarbor.DOCK_RISE-MontroseHarbor.LAND_RISE)*u;
                float x=c.screenX(q[i],q[i+1]),y=c.screenY(q[i],q[i+1])-rise*c.scale;q[i]=x;q[i+1]=y;}
            for(int i=2;i<q.length-2;i+=2)for(float v:new float[]{q[0],q[1],q[i],q[i+1],q[i+2],q[i+3]})out.add(v);
        }float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);return result;
    }
    static void access(Game g,Painter p){RiverCamera c=g.layout.camera;
        for(int index=0;index<ACCESS.length;index++){if(index==MontroseHarbor.GREEN_INLET_ACCESS)continue;float[] a=ACCESS[index],q=ACCESS_QUADS[index],geometry=ACCESS_GEOMETRY[index];float len=geometry[2];if(!visible(g,q,MontroseHarbor.LAND_RISE))continue;
            float[] xy=new float[8];for(int i=0;i<8;i+=2){float rise=i==0 || i==6?MontroseHarbor.LAND_RISE:MontroseHarbor.DOCK_RISE;xy[i]=c.screenX(q[i],q[i+1]);xy[i+1]=c.screenY(q[i],q[i+1])-rise*c.scale;}
            p.surfacePolygon(xy,new float[]{0,0,len,0,len,a[4],0,a[4]},SurfaceMaterial.STEEL,0xff737c7c,.05f);
            for(int side=0;side<2;side++){int from=side==0?0:6,to=side==0?2:4;p.line(xy[from],xy[from+1],xy[to],xy[to+1],.65f*c.scale,0xffb1b2a1);}
        }
    }
    private static void cut(ArrayList<Float> cuts,float ax,float ay,float bx,float by,float cx,float cy,float dx,float dy){
        float ux=bx-ax,uy=by-ay,vx=dx-cx,vy=dy-cy,den=ux*vy-uy*vx;if(Math.abs(den)<.00001f)return;
        float t=((cx-ax)*vy-(cy-ay)*vx)/den,u=((cx-ax)*uy-(cy-ay)*ux)/den;
        if(t>0 && t<1 && u>=0 && u<=1)cuts.add(t);
    }
    /** External outline of the union: no transverse faces at cache tile joins or T junctions. */
    private static float[][] dockEdges(){ArrayList<float[]> out=new ArrayList<float[]>();float[][] docks=MontroseHarbor.DOCKS;
        for(int index=0;index<docks.length;index++){float[] q=docks[index];float sign=PolygonWater.area(q)>0?1:-1;
            for(int edge=0;edge<4;edge++){int a=edge*2,b=(a+2)%8;float ax=q[a],ay=q[a+1],bx=q[b],by=q[b+1],dx=bx-ax,dy=by-ay,len=Physics.length(dx,dy);
                float nx=dy/len*sign,ny=-dx/len*sign;ArrayList<Float> cuts=new ArrayList<Float>();cuts.add(0f);cuts.add(1f);
                for(int j=0;j<docks.length;j++)if(j!=index){float[] other=docks[j],box=PolygonWater.bounds(other);
                    if(Math.max(ax,bx)<box[0] || Math.min(ax,bx)>box[2] || Math.max(ay,by)<box[1] || Math.min(ay,by)>box[3])continue;
                    for(int k=0;k<8;k+=2){int next=(k+2)%8;cut(cuts,ax,ay,bx,by,other[k],other[k+1],other[next],other[next+1]);}}
                float[] coast=MontroseHarbor.COAST;for(int k=0;k<coast.length-2;k+=2)cut(cuts,ax,ay,bx,by,coast[k],coast[k+1],coast[k+2],coast[k+3]);
                Collections.sort(cuts);
                for(int k=0;k<cuts.size()-1;k++){float from=cuts.get(k),to=cuts.get(k+1);if(to-from<.0001f)continue;float mid=(from+to)*.5f,x=ax+dx*mid+nx*.05f,y=ay+dy*mid+ny*.05f;
                    if(!MontroseHarbor.openWater(x,y))continue;boolean covered=false;
                    for(int j=0;j<docks.length && !covered;j++)if(j!=index && PolygonWater.contains(docks[j],x,y))covered=true;
                    if(!covered)out.add(new float[]{ax+dx*from,ay+dy*from,ax+dx*to,ay+dy*to});
                }
            }
        }
        return out.toArray(new float[out.size()][]);
    }
}
