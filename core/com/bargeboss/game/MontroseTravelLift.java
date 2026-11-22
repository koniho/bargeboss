package com.bargeboss.game;

/** Rigid frame partitions surround two broad webbing slings and exact hoist anchors. */
public final class MontroseTravelLift {
    public static final float X=MontroseHaulout.X,Y=MontroseHaulout.Y;
    private static final int BLUE=0xff407985,DARK=0xff263d43,CREAM=0xffc9c6a6;
    private static final ArticulatedModel FAR=side(1),NEAR=side(-1),TOP=top();
    private static final ArticulatedModel.Pose FAR_POSE=FAR.pose(1),NEAR_POSE=NEAR.pose(1),TOP_POSE=TOP.pose(0);
    static final ArticulatedModel STRAPS=straps();
    static final int STRAP_ROOT=STRAPS.nodeId("travelift-webbing");
    private static final float[] EXHAUST=NEAR_POSE.point(NEAR.nodeId("travelift-exhaust"),0,0,0);
    private static final float[][] APRON={quad(1015,90,1105,205),quad(1015,205,1040,275),quad(1080,205,1105,275)};
    private MontroseTravelLift(){}
    private static float[] quad(float l,float t,float r,float b){return new float[]{MontroseHarbor.mapX(l),MontroseHarbor.mapY(t),MontroseHarbor.mapX(r),MontroseHarbor.mapY(t),MontroseHarbor.mapX(r),MontroseHarbor.mapY(b),MontroseHarbor.mapX(l),MontroseHarbor.mapY(b)};}
    static final class Rig {
        final ArticulatedModel.Motion motion=STRAPS.motion(STRAP_ROOT,0,1,0);
        ArticulatedModel.Pose pose=motion.pose(0);
        void update(float amount){pose=motion.pose(amount);}
        float[] anchor(float x,float y,float z){return pose.point(STRAP_ROOT,x,y,z);}
    }
    private static ArticulatedModel side(int side){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();float x=side*45;
        int root=b.node("travelift-side-"+side,-1,X,Y,MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,0,0);
        b.box(root,x,0,16,9,78,9,BLUE,SurfaceMaterial.STEEL);
        for(int end=-1;end<=1;end+=2){float y=end*30;
            b.box(root,x,y,40,7,8,46,BLUE,SurfaceMaterial.STEEL);
            b.beam(root,x,y,23,x,y-end*18,59,3,DARK,SurfaceMaterial.STEEL);
            int wheel=b.node("travelift-wheel-"+side+"-"+end,root,x+4,y,8,0,(float)Math.PI/2);
            b.cylinder(wheel,0,8,8,8,12,0xff20292b);b.cylinder(wheel,8,1,3,3,10,0xffa9a99c);
        }
        if(side<0){
            b.box(root,-49,4,24,13,22,15,DARK,SurfaceMaterial.STEEL);
            b.box(root,-56,4,31,2,15,5,CREAM,SurfaceMaterial.STEEL);
            b.box(root,-51,20,8,14,10,3,0xff8a8a78,SurfaceMaterial.STEEL);
            b.beam(root,-51,-2,31,-51,-2,44,2.3f,0xff444d4c,SurfaceMaterial.STEEL);
            b.node("travelift-exhaust",root,-51,-2,44,0,0);
        }
        return b.build();
    }
    private static ArticulatedModel top(){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();int root=b.node("travelift-overhead",-1,X,Y,MontroseHarbor.LAND_RISE/ArticulatedModel.Z_SCALE,0,0);
        for(int side=-1;side<=1;side+=2){b.box(root,side*45,0,68,7,76,9,BLUE,SurfaceMaterial.STEEL);
            for(int end=-1;end<=1;end+=2)b.box(root,side*26,end*24,73,9,12,5,CREAM,SurfaceMaterial.STEEL);}
        for(int end=-1;end<=1;end+=2)b.box(root,0,end*30,69,88,8,10,BLUE,SurfaceMaterial.STEEL);
        return b.build();
    }
    private static ArticulatedModel straps(){
        ArticulatedModel.Builder b=new ArticulatedModel.Builder();
        int root=b.movingNode("travelift-webbing",-1,X,Y,52,0,0,0,0,-64);
        for(int end=-1;end<=1;end+=2){float y=end*24;
            // Eight-unit-wide webbing, less than one unit thick; U rests beneath the keel.
            b.beam(root,-26,y,0,-14,y,-12,8,.65f,0xff626950,SurfaceMaterial.STEEL);
            b.beam(root,-14,y,-12,14,y,-12,8,.65f,0xff626950,SurfaceMaterial.STEEL);
            b.beam(root,14,y,-12,26,y,0,8,.65f,0xff626950,SurfaceMaterial.STEEL);
            for(int side=-1;side<=1;side+=2){b.box(root,side*26,y,2,4,9,5,CREAM,SurfaceMaterial.STEEL);
                b.box(root,side*26,y,4,2,6,1,0xff485252,SurfaceMaterial.STEEL);}
        }
        return b.build();
    }
    public static void prepare(){FAR.prepare(FAR_POSE);NEAR.prepare(NEAR_POSE);TOP.prepare(TOP_POSE);}
    static float[] exhaust(){return EXHAUST.clone();}
    static void exhaust(Game g){g.smoke.shore(EXHAUST[0],EXHAUST[1],EXHAUST[2]*ArticulatedModel.Z_SCALE,.65f);}
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
    static float[] shore(Game g){
        if(!g.montroseStage())return ShoreOcclusion.EMPTY;
        RiverCamera c=g.layout.camera;float sx=c.screenX(X,Y),sy=c.screenY(X,Y),pad=180*c.scale;
        if(sx+pad<0 || sx-pad>g.layout.width || sy+pad<0 || sy-pad>g.layout.height)return ShoreOcclusion.EMPTY;
        return ShoreOcclusion.region(g,c,X-80,Y-80,X+80,Y+80,c.screenY(X,Y-24));
    }
    static void drawRange(Game g,Painter p,float min,float max,float[] shore){
        Rig rig=g.haulout.rig;if(rig==null)return;
        // Disjoint X slabs: far chassis x>=36, rig |x|<=28, near chassis x<=-36.
        // Header undersides sit above every moving block and are drawn last.
        FAR.drawRange(g,p,FAR_POSE,min,max);
        p.pushClipOutside(shore);
        try{STRAPS.drawRange(g,p,rig.pose,min,max);
        for(int end=-1;end<=1;end+=2)for(int side=-1;side<=1;side+=2){
            float[] a=rig.anchor(side*26,end*24,4);float depth=a[1]-a[0];if(depth<=min || depth>max)continue;
            RiverCamera c=g.layout.camera;float sx=c.screenX(a[0],a[1]),sy=c.screenY(a[0],a[1]);
            p.line(sx,sy-(MontroseHarbor.LAND_RISE+71*ArticulatedModel.Z_SCALE)*c.scale,sx,sy-a[2]*ArticulatedModel.Z_SCALE*c.scale,Math.max(.6f,.8f*c.scale),0xff6b746f);
        }
        }finally{p.popClip();}
        NEAR.drawRange(g,p,NEAR_POSE,min,max);TOP.drawRange(g,p,TOP_POSE,min,max);
    }
}
