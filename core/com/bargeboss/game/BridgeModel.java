package com.bargeboss.game;

import java.util.ArrayList;

/** Industrial through-truss bascule: fixed towers, two rigid hinged spans and attached fixtures. */
public final class BridgeModel {
    public static final float LENGTH=130,HINGE_HEIGHT=12,HALF_WIDTH=21,TRUSS_HEIGHT=35,TOWER_SETBACK=45;
    public static final int WARM=0,SIGNAL=1,TIP=2;
    public static final int RUST=0xff684337,STEEL=0xff202e3a,CONCRETE=0xff48545d,SAFETY=0xff91783e;
    public static final ArticulatedModel MODEL;
    public static final int LEFT_LEAF,RIGHT_LEAF;
    public static final Light[] LIGHTS;
    public static final class Light {
        public final String id;public final int node,kind,color;public final float x,y,z,nx,ny,nz,radius;
        private Light(String id,int node,int kind,float x,float y,float z,float nx,float ny,float nz,float radius) {
            this.id=id;this.node=node;this.kind=kind;this.x=x;this.y=y;this.z=z;this.nx=nx;this.ny=ny;this.nz=nz;this.radius=radius;
            color=kind==WARM?0xffffb65b:0xffff5945;
        }
    }
    private BridgeModel() {}
    static {
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();ArrayList<Light> lights=new ArrayList<Light>();
        int root=b.node("bridge-ground",-1,0,0,0,0,0),left=-1,right=-1;
        for(int bank=0;bank<2;bank++) {
            String name=bank==0?"west":"east";
            int shore=b.node(name+"-abutment",root,bank==0?Physics.LEFT:Physics.RIGHT,Bridge.Y,0,bank==0?0:(float)StrictMath.PI,0);
            foundation(b,shore);
            int tower=b.node(name+"-mechanism-tower",shore,-TOWER_SETBACK,0,0,0,0);
            tower(b,tower);
            int leaf=b.node(name+"-leaf",shore,0,0,HINGE_HEIGHT,0,(float)(StrictMath.PI*.5));
            if(bank==0)left=leaf;else right=leaf;
            span(b,leaf);
            fixture(b,lights,name+"-shore-signal",shore,SIGNAL,-23,35,28,0,0,1,3.2f);
            fixture(b,lights,name+"-tower-beacon",tower,TIP,-21,-24,76,0,0,1,3);
            for(int side=-1;side<=1;side+=2)fixture(b,lights,name+(side<0?"-upstream":"-downstream")+"-base-lantern",tower,WARM,-21,side*31,16,0,0,1,4);
            for(int rail=-1;rail<=1;rail+=2) {
                String side=rail<0?"upstream":"downstream";
                fixture(b,lights,name+"-"+side+"-deck-lamp",leaf,WARM,45,rail*HALF_WIDTH,TRUSS_HEIGHT+7,0,0,1,3);
                fixture(b,lights,name+"-"+side+"-tip-marker",leaf,TIP,LENGTH-2,rail*HALF_WIDTH,TRUSS_HEIGHT+3,0,0,1,2);
            }
        }
        LEFT_LEAF=left;RIGHT_LEAF=right;LIGHTS=lights.toArray(new Light[lights.size()]);MODEL=b.build();
    }
    private static void foundation(ArticulatedModel.Builder b,int n) {
        b.box(n,-43,0,3,92,66,10,CONCRETE);
        b.box(n,-43,0,9,94,69,3,0xff65707a);
        b.beam(n,0,-29,HINGE_HEIGHT,0,29,HINGE_HEIGHT,7,STEEL);
        for(int y=-29;y<=29;y+=58)b.box(n,-3,y,13,12,8,11,SAFETY);
        b.beam(n,-23,35,10,-23,35,27,2.5f,STEEL);
    }
    private static void tower(ArticulatedModel.Builder b,int n) {
        for(int x=-30;x<=-12;x+=18)for(int y=-24;y<=24;y+=48)b.beam(n,x,y,10,x,y,67,4.5f,STEEL);
        for(int y=-24;y<=24;y+=48) {
            b.beam(n,-30,y,13,-12,y,63,3,RUST);b.beam(n,-12,y,13,-30,y,63,3,RUST);
            b.beam(n,-32,y,67,-10,y,67,4,RUST);
        }
        for(int x=-30;x<=-12;x+=18) {
            b.beam(n,x,-24,14,x,24,63,3,RUST);b.beam(n,x,24,14,x,-24,63,3,RUST);
            b.beam(n,x,-24,36,x,24,36,3,STEEL);
        }
        b.box(n,-21,0,68,31,60,4,RUST);
        b.box(n,-21,0,30,22,27,17,0xff242f3a);
        for(int y=-28;y<=28;y+=56)b.beam(n,-34,y,75,-8,y,75,2.1f,SAFETY);
        for(int x=-34;x<=-8;x+=26)for(int y=-28;y<=28;y+=56)b.beam(n,x,y,69,x,y,75,1.8f,SAFETY);
    }
    private static void span(ArticulatedModel.Builder b,int n) {
        b.box(n,LENGTH*.5f,0,-2,LENGTH,HALF_WIDTH*2,4,0xff293441);
        b.box(n,-13,0,-1,24,30,14,0xff2b3540);
        for(int side=-1;side<=1;side+=2) {
            float y=side*HALF_WIDTH;
            b.beam(n,0,y,0,LENGTH,y,0,4.3f,RUST);
            b.beam(n,0,y,TRUSS_HEIGHT,LENGTH,y,TRUSS_HEIGHT,3.7f,RUST);
            for(int j=0;j<4;j++) {
                float a=j*LENGTH/4,bx=(j+1)*LENGTH/4;
                b.beam(n,a,y,j%2==0?0:TRUSS_HEIGHT,bx,y,j%2==0?TRUSS_HEIGHT:0,3.4f,RUST);
            }
            b.beam(n,0,y,0,0,y,TRUSS_HEIGHT,4,RUST);
            b.beam(n,LENGTH,y,0,LENGTH,y,TRUSS_HEIGHT,4,RUST);
            b.beam(n,45,y,TRUSS_HEIGHT,45,y,TRUSS_HEIGHT+5,1.7f,STEEL);
        }
        for(int j=0;j<=2;j++) {
            float x=j*LENGTH*.5f;b.beam(n,x,-HALF_WIDTH,TRUSS_HEIGHT,x,HALF_WIDTH,TRUSS_HEIGHT,3.2f,STEEL);
        }
        for(int side=-1;side<=1;side+=2)b.box(n,LENGTH*.5f,side*(HALF_WIDTH-3),.35f,LENGTH-4,1.3f,.7f,SAFETY);
    }
    private static void fixture(ArticulatedModel.Builder b,ArrayList<Light> lights,String id,int node,int kind,
                                float x,float y,float z,float nx,float ny,float nz,float radius) {
        b.box(node,x,y,z,4.6f,4.6f,3,STEEL);
        b.box(node,x,y,z+1.65f,3.2f,3.2f,.3f,kind==WARM?0xffefb968:0xffc65040);
        lights.add(new Light(id,node,kind,x,y,z+1.9f,nx,ny,nz,radius));
    }
}
