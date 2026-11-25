package com.bargeboss.game;
/** One bounded frame-local cursor through bridge geometry and the shared depth stream. */
final class BridgeLayers {
    private final Game game;private final Painter painter;private float previous=Float.NEGATIVE_INFINITY;
    private final MontroseTravelLift.Clips liftShore;
    private final int[] cranes;private final CargoTransfer.Pose[] cranePoses;
    /** Screen rectangles (l,t,r,b) of visible bridges, including lamp halos; null when off screen. */
    private final float[][] bridgeBoxes;
    private final float[] landmarkBox=new float[4];
    BridgeLayers(Game g,Painter p){game=g;painter=p;cranes=DockView.visible(g);cranePoses=new CargoTransfer.Pose[cranes.length];
        liftShore=MontroseTravelLift.shore(g);
        for(int i=0;i<cranes.length;i++)cranePoses[i]=new CargoTransfer.Pose(g,g.dockCranes[cranes[i]],cranes[i]);
        int count=g.passages==null?0:g.passages.bridges.length;bridgeBoxes=new float[count][];
        for(int i=0;i<count;i++){float[] box=new float[4];if(g.passages.bridges[i].screenBounds(g,g.passages.amount(i),box))bridgeBoxes[i]=box;}
    }

    /** A landmark only needs bridge faces painted before it when the two overlap on screen.
     * Skipped intervals are painted at the next event, still behind every later item. */
    void landmark(float screenDepth,int id){
        if(cranes.length==0 && !game.montroseStage() && LocationLandmarks.screenBox(game,id,landmarkBox)){
            boolean overlap=false;
            for(float[] b:bridgeBoxes)if(b!=null && b[2]>=landmarkBox[0] && b[0]<=landmarkBox[2] && b[3]>=landmarkBox[1] && b[1]<=landmarkBox[3]){overlap=true;break;}
            if(!overlap)return;
        }
        to(screenDepth);
    }

    void to(float screenDepth){
        RiverCamera c=game.layout.camera;
        float depth=(screenDepth-c.screenY(0,0))/(RiverCamera.Q*RiverCamera.DEPTH*c.scale);
        if(depth<=previous)return;
        for(int i=0;i<bridgeBoxes.length;i++){if(bridgeBoxes[i]==null)continue;LandmarkBridge bridge=game.passages.bridges[i];
            bridge.drawRange(game,painter,game.passages.amount(i),game.passages.passable(i),previous,depth);
        }
        for(int i=0;i<cranes.length;i++)DockView.drawRange(game,painter,cranes[i],cranePoses[i],previous,depth);
        if(game.montroseStage())MontroseTravelLift.drawRange(game,painter,previous,depth,liftShore);
        previous=depth;
    }
}
