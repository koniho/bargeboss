package com.bargeboss.game;

/** Prepared rigid boatyard gantry, split by the same depth stream as hulls and ropes. */
public final class MontroseTravelLift {
    public static final float X=MontroseHaulout.X,Y=MontroseHaulout.Y;
    private static final int BLUE=0xff407985,DARK=0xff263d43,CREAM=0xffc9c6a6;
    static final ArticulatedModel MODEL=build();
    // Wheel axle pitch is authored on a child node; this is one fixed prepared pose.
    private static final ArticulatedModel.Pose POSE=MODEL.pose(1);
    private static final float[][] APRON={quad(1015,90,1105,205),quad(1015,205,1040,275),quad(1080,205,1105,275)};
    private MontroseTravelLift(){}
    private static float[] quad(float l,float t,float r,float b){return new float[]{MontroseHarbor.mapX(l),MontroseHarbor.mapY(t),MontroseHarbor.mapX(r),MontroseHarbor.mapY(t),MontroseHarbor.mapX(r),MontroseHarbor.mapY(b),MontroseHarbor.mapX(l),MontroseHarbor.mapY(b)};}
    private static ArticulatedModel build(){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int root=b.node("montrose-travelift",-1,X,Y,MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,0,0);
        for(int side=-1;side<=1;side+=2){
            float x=side*45;
            b.box(root,x,0,16,9,78,9,BLUE,SurfaceMaterial.STEEL);
            b.box(root,x,0,68,7,76,9,BLUE,SurfaceMaterial.STEEL);
            for(int end=-1;end<=1;end+=2){float y=end*30;
                b.box(root,x,y,42,7,8,48,BLUE,SurfaceMaterial.STEEL);
                b.beam(root,x,y,23,x,y-end*18,59,3,DARK,SurfaceMaterial.STEEL);
                int wheel=b.node("travelift-wheel-"+side+"-"+end,root,x-4,y,8,0,(float)Math.PI/2);
                b.cylinder(wheel,0,8,8,8,12,0xff20292b);
                b.cylinder(wheel,8,1,3,3,10,0xffa9a99c);
                b.box(root,x,y,73,9,12,5,CREAM,SurfaceMaterial.STEEL);
                b.beam(root,x-side*6,y,71,x-side*6,y,10,1,0xff646d68,SurfaceMaterial.STEEL);
                b.box(root,x-side*6,y,10,4,6,5,CREAM,SurfaceMaterial.STEEL);
            }
        }
        for(int end=-1;end<=1;end+=2){float y=end*30;
            b.box(root,0,y,69,88,8,10,BLUE,SurfaceMaterial.STEEL);
            // Twin webbing slings dip below water while the crew warps the hull in.
            int sling=b.node("travelift-sling-"+end,root,0,y,0,0,0);
            b.beam(sling,-39,0,10,-18,0,-17,1.8f,0xff383b36);
            b.beam(sling,18,0,-17,39,0,10,1.8f,0xff383b36);
            b.beam(sling,-18,0,-17,18,0,-17,1.8f,0xff383b36);
        }
        b.box(root,-49,4,24,13,22,15,DARK,SurfaceMaterial.STEEL);
        b.box(root,-56,4,31,2,15,5,CREAM,SurfaceMaterial.STEEL);
        // Machinery and operator step stay on the western wheel track.
        b.box(root,-51,20,8,14,10,3,0xff8a8a78,SurfaceMaterial.STEEL);
        return b.build();
    }
    public static void prepare(){/* Class initialization prepares fixed topology and BSP once. */}
    public static void apron(Game g,Painter p){
        for(float[] q:APRON)MontroseShoreArt.surface(p,g.layout.camera,q,MontroseHarbor.LAND_RISE,SurfaceMaterial.CONCRETE,0xff73796f,.08f);
        RiverCamera c=g.layout.camera;
        Painter ground=new RiverPainter(ScreenView.offset(p,0,-MontroseHarbor.LAND_RISE*c.scale),c);
        for(int side=-1;side<=1;side+=2){float x=X+side*38;
            ground.line(x,MontroseHarbor.mapY(212),x,MontroseHarbor.mapY(272),1.5f,0xffc4a65e);
            for(int i=0;i<5;i++){float y=MontroseHarbor.mapY(135+i*11);ground.line(X+side*42,y,X+side*54,y-8,2,0xffa59b67);}
        }
        // Connected yard access and marked service space, without world-space HUD lettering.
        ground.line(X-54,MontroseHarbor.mapY(98),X+54,MontroseHarbor.mapY(98),1.4f,0xffb5ad87);
    }
    static void drawRange(Game g,Painter p,float min,float max){MODEL.drawRange(g,p,POSE,min,max);}
}
