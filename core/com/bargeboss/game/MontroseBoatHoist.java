package com.bargeboss.game;

/** Static service-apron boat hoist west of Dock O. Model anchors are ready for a
 * later haulout mechanic; no collision, input, mission or lifting state is added. */
public final class MontroseBoatHoist {
    public static final float MAP_X=780,MAP_Y=200;
    public static final float X=MontroseHarbor.mapX(MAP_X),Y=MontroseHarbor.mapY(MAP_Y);
    public static final String BOOM_ID="montrose-haulout-boom",LIFT_ID="montrose-haulout-lifting-eye";
    static final ArticulatedModel MODEL=HarborCrane.build(X,Y,MontroseHarbor.LAND_RISE,(float)Math.PI/2);
    private static final ArticulatedModel.Pose POSE=MODEL.pose(0);
    private MontroseBoatHoist(){}
    /** Stable part-local XYZ converted through the same skeleton as visible geometry. */
    public static float[] liftingAnchor(){return POSE.point(MODEL.nodeId(LIFT_ID),1.5f,0,-42);}
    public static float depth(Game g){return g.layout.camera.screenY(X,Y);}
    public static void draw(Game g,Painter p){if(g.montroseStage())MODEL.draw(g,p,POSE);}
}
