package com.bargeboss.game;

import java.util.ArrayList;

/** Prepared sandy foreshore: the surveyed water edge stays at zero elevation.
 * The inland edge meets the park surface; drawing and solid-shore masks share triangles. */
final class MontroseBeachShore {
    static final float[][] FOOTPRINTS=footprints();
    private static final float[][] TRIANGLES=triangles();
    private MontroseBeachShore(){}
    private static float[] inland(float[] coast,int i){
        int a=Math.max(0,i-2),b=Math.min(coast.length-2,i+2);
        float dx=coast[b]-coast[a],dy=coast[b+1]-coast[a+1],len=Physics.length(dx,dy);
        float nx=-dy/len,ny=dx/len,x=coast[i],y=coast[i+1];
        if(MontroseHarbor.openWater(x+nx*3,y+ny*3)){nx=-nx;ny=-ny;}
        float width=4;
        for(float d=6;d<=72;d+=2){if(MontroseHarbor.openWater(x+nx*d,y+ny*d))break;width=d;}
        return new float[]{x+nx*width,y+ny*width};
    }
    private static float[][] footprints(){
        ArrayList<float[]> out=new ArrayList<float[]>();float[] c=MontroseHarbor.COAST;
        for(int i=0;i<c.length-2;i+=2){
            if(!MontroseBeach.beachEdge(c[i],c[i+1],c[i+2],c[i+3]))continue;
            float[] a=inland(c,i),b=inland(c,i+2);
            out.add(new float[]{c[i],c[i+1],c[i+2],c[i+3],b[0],b[1],a[0],a[1]});
        }
        return out.toArray(new float[out.size()][]);
    }
    private static float[][] triangles(){
        ArrayList<float[]> out=new ArrayList<float[]>();
        float h=MontroseHarbor.LAND_RISE;
        for(float[] q:FOOTPRINTS){
            out.add(new float[]{q[0],q[1],q[2],q[3],q[4],q[5],0,0,h});
            out.add(new float[]{q[0],q[1],q[4],q[5],q[6],q[7],0,h,h});
        }
        return out.toArray(new float[out.size()][]);
    }
    private static float height(float[] tri,float x,float y){
        float area=PolygonWater.cross(tri[0],tri[1],tri[2],tri[3],tri[4],tri[5]);
        float a=PolygonWater.cross(x,y,tri[2],tri[3],tri[4],tri[5])/area;
        float b=PolygonWater.cross(tri[0],tri[1],x,y,tri[4],tri[5])/area;
        return a*tri[6]+b*tri[7]+(1-a-b)*tri[8];
    }
    static float riseAt(float x,float y){
        for(float[] tri:TRIANGLES){
            float a=PolygonWater.cross(tri[0],tri[1],tri[2],tri[3],x,y),b=PolygonWater.cross(tri[2],tri[3],tri[4],tri[5],x,y),c=PolygonWater.cross(tri[4],tri[5],tri[0],tri[1],x,y);
            if((a>=0 && b>=0 && c>=0) || (a<=0 && b<=0 && c<=0))return height(tri,x,y);}
        return MontroseHarbor.LAND_RISE;
    }
    static float[] wholeCut(RiverCamera c){
        float[] out=new float[TRIANGLES.length*6];int at=0;
        for(float[] tri:TRIANGLES)for(int i=0;i<6;i+=2){out[at++]=c.screenX(tri[i],tri[i+1]);out[at++]=c.screenY(tri[i],tri[i+1]);}
        return out;
    }
    static float[] cut(RiverCamera c,float l,float t,float r,float b){
        return MontroseHarbor.projected(FOOTPRINTS,c,l,t,r,b,Float.NEGATIVE_INFINITY);
    }
    static void draw(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        int wet=p.lighting().scenery(0xff686b60),dry=p.lighting().scenery(0xff8b8270);
        for(float[] footprint:FOOTPRINTS){
            if(!MontroseShoreArt.visible(g,footprint,MontroseHarbor.LAND_RISE))continue;
            float[] base=new float[8];for(int i=0;i<4;i++){base[i*2]=c.screenX(footprint[i*2],footprint[i*2+1]);base[i*2+1]=c.screenY(footprint[i*2],footprint[i*2+1])-(i<2?0:MontroseHarbor.LAND_RISE*c.scale);}p.poly(base,dry);
        }
        for(float[] tri:TRIANGLES){
            float[] q=new float[6];for(int i=0;i<3;i++){q[i*2]=c.screenX(tri[i*2],tri[i*2+1]);q[i*2+1]=c.screenY(tri[i*2],tri[i*2+1])-tri[6+i]*c.scale;}
            float[] box=PolygonWater.bounds(q);if(box[2]<0 || box[0]>g.layout.width || box[3]<0 || box[1]>g.layout.height)continue;
            p.shadedTriangle(q[0],q[1],tri[6]==0?wet:dry,q[2],q[3],tri[7]==0?wet:dry,q[4],q[5],tri[8]==0?wet:dry);
        }
    }
    static float[] mask(RiverCamera c,float l,float t,float r,float b,float depth){
        ArrayList<float[]> pieces=new ArrayList<float[]>();int count=0;
        float limit=(depth-c.screenY(0,0))/(RiverCamera.Q*RiverCamera.DEPTH*c.scale);
        for(float[] tri:TRIANGLES){
            float[] q=PolygonWater.rectangle(new float[]{tri[0],tri[1],tri[2],tri[3],tri[4],tri[5]},l,t,r,b);
            if(depth!=Float.NEGATIVE_INFINITY)q=PolygonWater.edge(q,0,limit,1,limit+1,true);
            if(q.length<6)continue;
            for(int i=0;i<q.length;i+=2){float x=q[i],y=q[i+1];q[i]=c.screenX(x,y);q[i+1]=c.screenY(x,y)-height(tri,x,y)*c.scale;}
            pieces.add(q);count+=(q.length/2-2)*6;
        }
        float[] out=new float[count];int at=0;
        for(float[] q:pieces)for(int i=2;i<q.length-2;i+=2){out[at++]=q[0];out[at++]=q[1];out[at++]=q[i];out[at++]=q[i+1];out[at++]=q[i+2];out[at++]=q[i+3];}
        return out;
    }
}
