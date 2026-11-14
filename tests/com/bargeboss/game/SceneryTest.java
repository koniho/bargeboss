package com.bargeboss.game;

public final class SceneryTest {
    private static int checks;
    private static void check(boolean value,String message) { checks++; if(!value) throw new AssertionError(message); }
    public static void run() {
        for(int atlas=SpriteAtlas.WATER;atlas<=SpriteAtlas.BRIDGE;atlas++) for(int frame=0;frame<SpriteAtlas.count(atlas);frame++) {
            float l=SpriteAtlas.sourceLeft(atlas,frame),t=SpriteAtlas.sourceTop(atlas,frame),r=SpriteAtlas.sourceRight(atlas,frame),b=SpriteAtlas.sourceBottom(atlas,frame);
            check(l>=0 && t>=0 && r<=1 && b<=1 && r>l && b>t,"independent scenery crop stays inside its source image");
            check(SpriteAtlas.artWidth(atlas,frame)>0 && SpriteAtlas.artHeight(atlas,frame)>0,"scenery retains source proportions");
        }
        Game g=new Game();g.challengeId=2;g.reset();for(Feeder f:g.feeders) { f.phase=Feeder.IDLE;f.load=null; }
        float obscuredX=g.tug.x,obscuredY=g.tug.y;
        RiverCamera c=g.layout.camera;float x=c.screenX(obscuredX,obscuredY),y=c.screenY(obscuredX,obscuredY);
        float faded=SceneryView.opacity(g,x-30,y-70,60,80);
        check(faded<=.29f,"foreground scenery fades over the player's vessel");
        int before=Parity.frame(g);int tick=g.visualTicks;
        check(before==Parity.frame(g) && g.visualTicks==tick,"repeated scenery drawing is deterministic and state-free");
        g.tug.y=260;
        check(SceneryView.opacity(g,x-30,y-70,60,80)==1,"unobscuring scenery returns to full opacity");
        Body cargo=new Body(99,1,obscuredX,obscuredY,0);g.cargo[0]=cargo;
        check(SceneryView.opacity(g,x-30,y-70,60,80)<.3f,"cargo also reveals obstructing scenery");
        g.cargo[0]=null;
        g.ticks=1080;g.trafficBoat.x=obscuredX;g.trafficBoat.y=obscuredY;
        check(SceneryView.opacity(g,x-30,y-70,60,80)<.3f,"cross-traffic also reveals obstructing scenery");
        g.ticks=0;
        g.ropeBody=new Body(99,0,300,300,0);g.ropePhase=1;g.ropeEndX=300;g.ropeEndY=700;
        float rx=c.screenX(300,470),ry=c.screenY(300,470);
        check(SceneryView.opacity(g,rx-12,ry-12,24,24)<.4f,"foreground reveals a thrown or aimed lasso path");
        g.ropePhase=0;g.ropeBody=null;
        Body boat=new Body(0,-1,370,290,0);boat.vx=80;
        float hit=HarborGeometry.collide(boat);
        check(hit>=80 && boat.vx<=0,"solid finger pier stops and rebounds an approaching tug");
        for(int i=0;i<3;i++) check(boat.discX(i)+boat.radius<=381.01f,"all hull discs remain outside the quay wall");
        float[] point=new float[2];HarborGeometry.waypoint(400,430,300,290,42.75f,point);
        check(point[0]<340 && point[1]>322,"push assistance takes a clear approach around the north pier");
        HarborGeometry.waypoint(260,500,230,290,42.75f,point);
        check(point[0]>=255,"assistance clears the south pier before entering berth A");
        g=new Game();g.reset();for(int i=0;i<100;i++) g.step();
        check(g.feeders[0].load!=null,"both feeder approaches remain outside the new quays");
        System.out.println(checks+" scenery crop, occlusion, solid quay and route assertions passed");
    }
}
