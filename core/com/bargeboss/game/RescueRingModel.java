package com.bargeboss.game;

/** Continuous 3-D torus, four reflective bands, no rotated flat ring image. */
final class RescueRingModel {
    private static final ArticulatedModel.Rigid RING=mesh().rigid();
    private static ArticulatedModel mesh(){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("rescue-ring",-1,0,0,0,0,0);
        for(int i=0;i<16;i++)for(int j=0;j<6;j++){
            float[] face=new float[12];int[] da={0,1,1,0},dt={0,0,1,1};
            for(int k=0;k<4;k++){double a=(i+da[k])*2*StrictMath.PI/16,t=(j+dt[k])*2*StrictMath.PI/6;float r=3.4f+.85f*(float)StrictMath.cos(t);
                face[k*3]=r*(float)StrictMath.cos(a);face[k*3+1]=r*(float)StrictMath.sin(a);face[k*3+2]=.85f*(float)StrictMath.sin(t);}
            b.materialPolygon(root,i%4==0?0xfffff1c4:0xfff58a26,SurfaceMaterial.SAFETY,face);
        }return b.build();
    }
    static void draw(Game g,Painter p,float x,float y,float z,float yaw,float pitch,float roll){
        RING.drawSolid(g,p,x,y,z,yaw,pitch,roll);
    }
    static void prepare(){}
    private RescueRingModel(){}
}
