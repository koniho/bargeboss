package com.bargeboss.game;

/** One rigid crane design for every harbor. Placement rotates XYZ before projection. */
public final class HarborCrane {
    static final class Lamp {
        final String id,parent;final float x,y,z,nx=0,ny=0,nz=1;
        Lamp(String id,String parent,float x,float y,float z){this.id=id;this.parent=parent;this.x=x;this.y=y;this.z=z;}
    }
    static final Lamp[] CARGO_LIGHTS={new Lamp("cargo-mast-worklight","cargo-pedestal",-7,-8,CargoTransfer.PIVOT-4),new Lamp("cargo-tip-worklight",MontroseBoatHoist.LIFT_ID,0,0,4)};
    // Electric shore machinery: no smoke or steam outlets.
    private HarborCrane(){}
    /** Cargo-duty luffing crane. Mast and jib remain separated vertically over full travel. */
    static ArticulatedModel cargo(float x,float y,float heading,float reach){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int base=b.node("cargo-pedestal",-1,x,y,GroundArt.LAND_RISE/ArticulatedModel.Z_SCALE,heading,0);
        b.box(base,0,0,2,26,26,4,0xff85897f,SurfaceMaterial.CONCRETE);
        for(int side=-1;side<=1;side+=2){
            b.beam(base,side*10,-8,4,side*5,-5,CargoTransfer.PIVOT,4,0xffb48a34,SurfaceMaterial.RUST);
            b.beam(base,side*10,8,4,side*5,5,CargoTransfer.PIVOT,4,0xffb48a34,SurfaceMaterial.RUST);
            for(int j=0;j<6;j++)b.beam(base,side*9,-7,j*16+5,side*6,7,j*16+21,1.7f,0xff656b65,SurfaceMaterial.STEEL);
        }
        b.box(base,0,0,CargoTransfer.PIVOT-1,18,18,6,0xff4c5853,SurfaceMaterial.STEEL);
        int boom=b.node(MontroseBoatHoist.BOOM_ID,base,0,0,CargoTransfer.PIVOT,0,CargoTransfer.LUFF);
        b.box(base,-13,0,CargoTransfer.PIVOT-10,22,20,12,0xff626861,SurfaceMaterial.STEEL);
        b.box(base,-13,0,CargoTransfer.PIVOT-3,24,22,3,0xffc89e42,SurfaceMaterial.RUST);
        for(int side=-1;side<=1;side+=2){
            b.beam(boom,4,side*5,4,reach,side*2,4,3.2f,0xffd2a239,SurfaceMaterial.RUST);
            b.beam(boom,4,side*5,16,reach,side*2,9,2.5f,0xffd2a239,SurfaceMaterial.RUST);
            for(int i=0;i<7;i++)b.beam(boom,4+i*(reach-4)/7,side*(5-i*3/7f),4,4+(i+1)*(reach-4)/7,side*(5-(i+1)*3/7f),16-(i+1),1.5f,0xffad842f,SurfaceMaterial.STEEL);
        }
        b.beam(boom,reach,-3,4,reach,3,4,4,0xff48524c,SurfaceMaterial.STEEL);
        b.node(MontroseBoatHoist.LIFT_ID,boom,reach,0,4,0,0);
        return b.build();
    }
    static ArticulatedModel build(float worldX,float worldY,float rise,float heading){ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int base=b.node("montrose-haulout-pedestal",-1,worldX,worldY,rise/ArticulatedModel.Z_SCALE,heading,0);
        b.box(base,0,0,1.5f,22,22,3,0xff8c8e82,SurfaceMaterial.CONCRETE);
        b.box(base,0,0,5,15,15,4,0xff404a4b,SurfaceMaterial.STEEL);
        for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)b.box(base,x*6,y*6,7.5f,2,2,2,0xffaca58c,SurfaceMaterial.STEEL);
        b.box(base,0,0,26,8,9,38,0xffbb8d28,SurfaceMaterial.RUST);
        b.box(base,0,0,45,13,13,4,0xff48514b,SurfaceMaterial.STEEL);
        // Slewing head and jib remain separate rigid parts about the mast pivot.
        int boom=b.node(MontroseBoatHoist.BOOM_ID,base,0,0,47,0,.6f);
        b.box(boom,-9,0,-3,17,17,13,0xff575e54,SurfaceMaterial.STEEL);
        b.box(boom,-9,0,5,18,18,3,0xffd0a039,SurfaceMaterial.RUST);
        for(int side=-1;side<=1;side+=2){
            b.beam(boom,0,side*4,0,66,side*2,15,3.2f,0xffd2a239,SurfaceMaterial.RUST);
            b.beam(boom,0,side*4,11,66,side*2,18,2.4f,0xffe1b44a,SurfaceMaterial.RUST);
            for(int i=0;i<5;i++){float x=i*13.2f,xx=(i+1)*13.2f,yy=side*(4-i*.4f),yyy=side*(4-(i+1)*.4f);
                b.beam(boom,x,yy,x*15/66,xx,yyy,11+xx*7/66,1.2f,0xffba8625,SurfaceMaterial.STEEL);}
        }
        b.beam(boom,66,-3,16.5f,66,3,16.5f,4,0xff414b4b,SurfaceMaterial.STEEL);
        b.beam(boom,2,0,11,61,0,18,.8f,0xffa7aaa0,SurfaceMaterial.STEEL);
        int lifting=b.node(MontroseBoatHoist.LIFT_ID,boom,66,0,15,0,0);
        b.beam(lifting,0,0,0,0,0,-35,.8f,0xff5c6563,SurfaceMaterial.STEEL);
        b.box(lifting,0,0,-36,4,5,5,0xffcfa340,SurfaceMaterial.RUST);
        b.beam(lifting,0,0,-38,0,0,-42,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        b.beam(lifting,0,0,-42,3,0,-42,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        b.beam(lifting,3,0,-42,3,0,-40,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        return b.build();
    }
}
