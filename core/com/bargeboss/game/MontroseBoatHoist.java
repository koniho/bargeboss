package com.bargeboss.game;

/** Static service-apron boat hoist west of Dock O. Model anchors are ready for a
 * later haulout mechanic; no collision, input, mission or lifting state is added. */
public final class MontroseBoatHoist {
    public static final float MAP_X=780,MAP_Y=200;
    public static final float X=MontroseHarbor.mapX(MAP_X),Y=MontroseHarbor.mapY(MAP_Y);
    public static final String BOOM_ID="montrose-haulout-boom",LIFT_ID="montrose-haulout-lifting-eye";
    static final ArticulatedModel MODEL=build();
    private static final ArticulatedModel.Pose POSE=MODEL.pose(0);
    private MontroseBoatHoist(){}
    private static ArticulatedModel build(){ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int base=b.node("montrose-haulout-pedestal",-1,X,Y,MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,(float)Math.PI/2,0);
        b.box(base,0,0,1.5f,22,22,3,0xff8c8e82,SurfaceMaterial.CONCRETE);
        b.box(base,0,0,5,15,15,4,0xff404a4b,SurfaceMaterial.STEEL);
        for(int x=-1;x<=1;x+=2)for(int y=-1;y<=1;y+=2)b.box(base,x*6,y*6,7.5f,2,2,2,0xffaca58c,SurfaceMaterial.STEEL);
        b.box(base,0,0,26,8,9,38,0xffbb8d28,SurfaceMaterial.RUST);
        b.box(base,0,0,45,13,13,4,0xff48514b,SurfaceMaterial.STEEL);
        // Slewing head and jib remain separate rigid parts about the mast pivot.
        int boom=b.node(BOOM_ID,base,0,0,47,0,.6f);
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
        int lifting=b.node(LIFT_ID,boom,66,0,15,0,0);
        b.beam(lifting,0,0,0,0,0,-35,.8f,0xff5c6563,SurfaceMaterial.STEEL);
        b.box(lifting,0,0,-36,4,5,5,0xffcfa340,SurfaceMaterial.RUST);
        b.beam(lifting,0,0,-38,0,0,-42,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        b.beam(lifting,0,0,-42,3,0,-42,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        b.beam(lifting,3,0,-42,3,0,-40,1.2f,0xff343e3e,SurfaceMaterial.STEEL);
        return b.build();
    }
    /** Stable part-local XYZ converted through the same skeleton as visible geometry. */
    public static float[] liftingAnchor(){return POSE.point(MODEL.nodeId(LIFT_ID),1.5f,0,-42);}
    public static float depth(Game g){return g.layout.camera.screenY(X,Y);}
    public static void draw(Game g,Painter p){if(g.montroseStage())MODEL.draw(g,p,POSE);}
}
