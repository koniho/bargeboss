package com.bargeboss.game;

/** One bounded water-only shore mask, retained independently of per-vessel masks. */
final class WaterClip {
    private static final RiverCamera BASIS=new RiverCamera(0,0,1,0,0,1);
    private float left,top,right,bottom;
    private float[] source,projected;
    private RiverCamera lastCamera;
    private boolean union;
    float[] get(Game g,RiverCamera camera,float l,float t,float r,float b,boolean unions){
        if(source==null || union!=unions || l<left || t<top || r>right || b>bottom){
            // A small guard band absorbs camera follow without changing any visible cutout.
            left=l-96;top=t-96;right=r+96;bottom=b+96;union=unions;
            source=ShoreOcclusion.region(g,BASIS,left,top,right,bottom,Float.NEGATIVE_INFINITY,union);
            if(source.length>262144){float[] result=ShoreOcclusion.region(g,camera,l,t,r,b,Float.NEGATIVE_INFINITY,union);source=null;return result;}
            projected=new float[source.length];lastCamera=null;
        }
        if(lastCamera!=camera){
            float x=camera.screenX(0,0)-BASIS.screenX(0,0)*camera.scale;
            float y=camera.screenY(0,0)-BASIS.screenY(0,0)*camera.scale;
            for(int i=0;i<source.length;i+=2){projected[i]=source[i]*camera.scale+x;projected[i+1]=source[i+1]*camera.scale+y;}
            lastCamera=camera;
        }
        return projected;
    }
}
