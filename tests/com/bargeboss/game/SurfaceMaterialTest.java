package com.bargeboss.game;

import java.util.Arrays;
import java.util.HashSet;

/** Stable material coordinates through rigid motion, BSP splitting and Painter wrappers. */
public final class SurfaceMaterialTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Capture implements Painter {
        int faces,vertices,color;float firstX,firstY,wetness,maxError;float[] lastUV;
        final HashSet<String> uvFaces=new HashSet<String>();
        public void surfacePolygon(float[] xy,float[] uv,int material,int color,float wetness){
            check(xy.length==uv.length && xy.length>=6 && xy.length%2==0,"surface vertices pair with UVs");
            check(material>=SurfaceMaterial.RUST && material<=SurfaceMaterial.CORRUGATED,"known shared material");
            check(Float.isFinite(wetness) && wetness>=0 && wetness<=1,"bounded shared wetness");
            faces++;vertices+=xy.length/2;this.color=color;this.wetness=wetness;firstX=xy[0];firstY=xy[1];lastUV=uv.clone();
            uvFaces.add(Arrays.toString(uv));
            for(int i=0;i<xy.length;i++)check(Float.isFinite(xy[i]) && Float.isFinite(uv[i]),"finite projected geometry and local UVs");
            // Compare every vertex with an affine map derived from three source vertices.
            // Use the largest basis to avoid magnifying tiny BSP sliver roundoff.
            int a=2,b=4;float determinant=0;
            for(int i=2;i<uv.length;i+=2)for(int j=i+2;j<uv.length;j+=2){
                float d=(uv[i]-uv[0])*(uv[j+1]-uv[1])-(uv[i+1]-uv[1])*(uv[j]-uv[0]);
                if(Math.abs(d)>Math.abs(determinant)){determinant=d;a=i;b=j;}
            }
            if(Math.abs(determinant)<.0001f)return;
            for(int i=0;i<uv.length;i+=2){
                float du=uv[i]-uv[0],dv=uv[i+1]-uv[1];
                float u=(du*(uv[b+1]-uv[1])-dv*(uv[b]-uv[0]))/determinant;
                float v=((uv[a]-uv[0])*dv-(uv[a+1]-uv[1])*du)/determinant;
                float x=xy[0]+u*(xy[a]-xy[0])+v*(xy[b]-xy[0]);
                float y=xy[1]+u*(xy[a+1]-xy[1])+v*(xy[b+1]-xy[1]);
                float error=Math.max(Math.abs(x-xy[i]),Math.abs(y-xy[i+1]));maxError=Math.max(maxError,error);
                check(error<.12f,"BSP split preserves affine face material mapping: "+error);
            }
        }
        public void poly(float[] xy,int color){}
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float b,float alpha){}
    }
    public static void run(){
        float[] xy={1,2,11,2,11,12,1,12},uv={10,20,20,20,20,30,10,30};
        Capture forwarded=new Capture();
        Painter wrappers=ScreenView.scaled(new SceneryOcclusion(new OpacityPainter(new ViewportPainter(forwarded,1000,1000),.5f)),20,30,2);
        wrappers.surfacePolygon(xy,uv,SurfaceMaterial.RUST,0xc0a06040,.65f);
        check(forwarded.faces==1 && forwarded.color==0x60a06040,"surface alpha and RGB survive actual wrapper chain");
        check(forwarded.firstX==22 && forwarded.firstY==34 && forwarded.wetness==.65f,"screen transform preserves material parameters");
        check(Arrays.equals(uv,forwarded.lastUV),"screen transform does not rescale material UVs");
        wrappers.surfacePolygon(new float[]{-500,-500,-490,-500,-490,-490},new float[]{0,0,10,0,10,10},SurfaceMaterial.CONCRETE,0xffffffff,0);
        check(forwarded.faces==1,"offscreen surfaces are culled before native submission");
        RiverCamera camera=new RiverCamera(50,60,2);Capture projected=new Capture();
        new RiverPainter(projected,camera).surfacePolygon(xy,uv,SurfaceMaterial.STEEL,0xffffffff,0);
        check(projected.firstX==camera.screenX(1,2) && projected.firstY==camera.screenY(1,2),"river projection transforms geometry");
        check(Arrays.equals(uv,projected.lastUV),"river projection preserves world-unit UVs");

        Game g=new Game();g.challengeId=1;g.reset();g.layout=Layout.focused(1800,1200,300,Bridge.Y,1.5f,900,600);
        ArticulatedModel.Builder builder=new ArticulatedModel.Builder();
        int node=builder.node("test-leaf",-1,300,Bridge.Y,10,.2f,.3f);
        builder.box(node,4,7,9,12,8,6,0xffa06040,SurfaceMaterial.RUST);ArticulatedModel model=builder.build();
        HashSet<String> common=null;float previousX=Float.NaN;
        for(float opening:new float[]{0,.5f,1}){
            Capture c=new Capture();model.draw(g,c,model.pose(opening));
            check(c.faces>=2,"moving rigid part exposes material faces");
            if(common==null)common=new HashSet<String>(c.uvFaces);else common.retainAll(c.uvFaces);
            if(Float.isFinite(previousX))check(c.firstX!=previousX,"rigid opening moves geometry while preserving material coordinates");
            previousX=c.firstX;
        }
        check(common.size()>=2,"same part-local face UVs survive all opening states");

        Capture fragments=new Capture();Painter p=new SceneryOcclusion(new OpacityPainter(new ViewportPainter(fragments,1800,1200),.75f));
        int ticks=g.ticks,visualTicks=g.visualTicks;
        for(float lift:new float[]{0,.25f,.5f,.75f,1})BridgeModel.MODEL.draw(g,p,BridgeModel.MODEL.pose(lift));
        for(int gate=0;gate<2;gate++){
            g.layout=Layout.focused(1800,1200,300,LockView.gateY(gate),1.5f,900,600);
            for(float opening:new float[]{0,.5f,1}){ArticulatedModel m=LockView.model(gate,opening);m.draw(g,p,m.pose(0));}
        }
        check(fragments.faces>1000,"real bridge and lock BSP fragments reach surface path");
        check(g.ticks==ticks && g.visualTicks==visualTicks,"material rendering never advances simulation");
        System.out.println(checks+" surface UV, BSP, opacity and wrapper assertions passed; "+fragments.faces+" fragments, max affine error "+fragments.maxError);
    }
    public static void main(String[] args){run();}
}
