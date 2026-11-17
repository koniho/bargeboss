package com.bargeboss.game;

/** Fixed, bounded landmark detail; all coordinates are local to the rigid root. */
final class IndianaDetails {
    private static final int DARK=0xff3e3735,IRON=0xff79615c,RUST=0xff956044;
    private IndianaDetails(){}
    static void tower(ArticulatedModel.Builder b,int n){
        // Deep window reveals and end windows make the control room legible on every side.
        for(int side=-1;side<=1;side+=2){
            for(int i=-2;i<=2;i++)b.box(n,i*10,side*24,49,1.2f,2,13,DARK,SurfaceMaterial.STEEL);
            b.box(n,0,side*24,42,63,2,2,IRON,SurfaceMaterial.STEEL);
            b.box(n,side*31.5f,0,49,1,29,9,0xff9a6535,SurfaceMaterial.EMISSIVE);
            for(int j=-1;j<=1;j++)b.box(n,side*32,j*10,49,2,1.4f,12,DARK,SurfaceMaterial.STEEL);
            b.box(n,side*29.5f,0,20,1,12,21,DARK,SurfaceMaterial.STEEL);
        }
        b.box(n,39,-20,39,18,16,3,IRON,SurfaceMaterial.STEEL);
        for(int i=0;i<5;i++)b.beam(n,45,30-i*10,6+i*8,45,30-i*10,17+i*8,1,RUST,SurfaceMaterial.RUST);
        b.beam(n,33,33,12,33,-20,52,1.2f,RUST,SurfaceMaterial.RUST);
        b.box(n,-20,8,65,8,8,10,DARK,SurfaceMaterial.RUST);
        b.beam(n,-20,8,70,-20,8,88,.8f,IRON,SurfaceMaterial.STEEL);
        b.box(n,0,-21.5f,15,11,1,24,DARK,SurfaceMaterial.STEEL);
        b.box(n,0,-24,2,17,7,4,IRON,SurfaceMaterial.CONCRETE);
    }
    static void mill(ArticulatedModel.Builder b,int n,boolean furnace){
        for(int side=-1;side<=1;side+=2){
            for(int j=-2;j<=2;j++)b.box(n,side*56,j*32,31,3,3,62,DARK,SurfaceMaterial.STEEL);
            b.box(n,side*57,0,8,3,160,4,IRON,SurfaceMaterial.RUST);
        }
        b.box(n,0,0,76,24,137,12,DARK,SurfaceMaterial.CORRUGATED);
        for(int j=-2;j<=2;j++)b.box(n,-12.5f,j*25,76,1,17,5,0xff875127,SurfaceMaterial.EMISSIVE);
        b.box(n,0,-81,19,33,2,36,DARK,SurfaceMaterial.CORRUGATED);
        b.box(n,0,-87,38,43,14,3,IRON,SurfaceMaterial.RUST);
        for(int j=-1;j<=1;j++)b.box(n,35,j*48,124,22,22,3,IRON,SurfaceMaterial.STEEL);
        if(!furnace)return;
        // Compact furnace/stove silhouette on the inland half of the existing footprint.
        b.box(n,0,28,92,48,48,35,RUST,SurfaceMaterial.RUST);
        b.box(n,0,28,119,33,33,23,IRON,SurfaceMaterial.STEEL);
        b.box(n,0,28,137,19,19,14,DARK,SurfaceMaterial.RUST);
        b.box(n,0,28,111,62,62,3,IRON,SurfaceMaterial.STEEL);
        for(int side=-1;side<=1;side+=2){
            b.beam(n,side*29,-1,113,side*29,57,113,1.5f,RUST,SurfaceMaterial.RUST);
            b.beam(n,side*29,-1,122,side*29,57,122,1.5f,RUST,SurfaceMaterial.RUST);
            for(int j=0;j<3;j++)b.beam(n,side*29,j*28,112,side*29,j*28,123,1,RUST,SurfaceMaterial.RUST);
        }
        b.beam(n,0,28,142,-38,28,124,7,IRON,SurfaceMaterial.RUST);
        b.beam(n,-38,28,124,-38,28,72,7,IRON,SurfaceMaterial.RUST);
        b.box(n,0,-1,90,20,1,8,0xff8f4523,SurfaceMaterial.EMISSIVE);
    }
    static void light(ArticulatedModel.Builder b,int n){
        for(int side=-1;side<=1;side+=2){
            b.box(n,side*14,0,55,1,8,17,DARK,SurfaceMaterial.STEEL);
            b.box(n,0,side*14,79,8,1,14,DARK,SurfaceMaterial.STEEL);
            b.beam(n,side*18,-18,108,side*18,18,108,1,IRON,SurfaceMaterial.STEEL);
            for(int j=-1;j<=1;j++)b.beam(n,side*18,j*18,103,side*18,j*18,110,1,IRON,SurfaceMaterial.STEEL);
        }
        b.box(n,0,-14,18,10,1,23,DARK,SurfaceMaterial.STEEL);
    }
}
