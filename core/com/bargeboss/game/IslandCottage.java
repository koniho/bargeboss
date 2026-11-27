package com.bargeboss.game;

/** Small clapboard summer houses: pitched roofs, chimneys, porch posts and warm divided windows. */
final class IslandCottage {
    private IslandCottage(){}
    static ArticulatedModel build(int id,float x,float y){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int n=b.node("island-cottage-"+id,-1,x,y,IslandArt.TOP/ArticulatedModel.Z_SCALE,0,0);
        int wall=id%2==0?0xffc4bca0:0xff9c5547,trim=0xffc6c8b5,roof=0xff566975;
        b.box(n,0,0,2,49,35,4,0xff737f79,SurfaceMaterial.CONCRETE);
        b.box(n,0,0,12,46,32,20,wall,SurfaceMaterial.WOOD);
        b.polygon(n,wall,-23,-16,22,-23,0,33,-23,16,22);
        b.polygon(n,wall,23,16,22,23,0,33,23,-16,22);
        b.materialPolygon(n,roof,SurfaceMaterial.STEEL,-26,-19,22,26,-19,22,26,0,34,-26,0,34);
        b.materialPolygon(n,0xff71808a,SurfaceMaterial.STEEL,-26,0,34,26,0,34,26,19,22,-26,19,22);
        b.beam(n,-26,0,34,26,0,34,1.2f,0xff9baba8);
        for(int side=-1;side<=1;side+=2){
            b.beam(n,-26,side*19,22,26,side*19,22,1.6f,trim);
            b.beam(n,-26,side*19,22,-26,0,34,1.5f,trim);
        }
        // Clapboard courses are coplanar face strips, not separate thick boxes.
        for(float z=6;z<22;z+=3){
            b.polygon(n,0xff8e8c78,-23,16.1f,z,23,16.1f,z,23,16.1f,z+.35f,-23,16.1f,z+.35f);
            b.polygon(n,0xff7d8070,-23.1f,-16,z,-23.1f,16,z,-23.1f,16,z+.35f,-23.1f,-16,z+.35f);
        }
        for(int side=-1;side<=1;side+=2){
            b.box(n,side*22.3f,16.3f,12,1.8f,1,20,trim);
            window(b,n,side*13,16.4f,14,9,10,trim);
        }
        b.box(n,0,16.4f,10,7,1,16,0xff344b52);
        b.box(n,0,17,12,4,.25f,8,0xffa08a5c,SurfaceMaterial.EMISSIVE);
        b.box(n,-23.5f,0,14,.6f,10,11,trim);
        b.box(n,-23.9f,0,14,.3f,7,8,0xff8b7855,SurfaceMaterial.EMISSIVE);
        b.box(n,-24.1f,0,14,.3f,.7f,9,0xffb8c0ae);
        // Wrap of the sheltered veranda, and its steps down to the granite.
        b.box(n,0,23,3,49,15,2,0xffa0997d,SurfaceMaterial.WOOD);
        b.box(n,0,23,22,51,18,1.7f,roof);
        for(int k=-1;k<=1;k++)b.box(n,k*23,29,12,1.7f,1.7f,18,trim);
        b.box(n,0,32,1.8f,12,4,2,0xff929783,SurfaceMaterial.CONCRETE);
        b.box(n,13,-4,32,5,6,15,0xff8c7968,SurfaceMaterial.CONCRETE);
        b.box(n,13,-4,40,7,8,1.5f,0xff52616b);
        ArticulatedModel model=b.build();model.prepare(model.pose(0));return model;
    }
    private static void window(ArticulatedModel.Builder b,int n,float x,float y,float z,float w,float h,int trim){
        b.box(n,x,y,z,w+2,.6f,h+2,trim);
        b.box(n,x,y+.4f,z,w,.3f,h,0xff9c8356,SurfaceMaterial.EMISSIVE);
        b.box(n,x,y+.6f,z,.7f,.3f,h,trim);
        b.box(n,x,y+.6f,z,w,.3f,.7f,trim);
    }
}
