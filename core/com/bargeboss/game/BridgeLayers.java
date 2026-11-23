package com.bargeboss.game;
/** One bounded frame-local cursor through bridge geometry and the shared depth stream. */
final class BridgeLayers {
    private final Game game;private final Painter painter;private float previous=Float.NEGATIVE_INFINITY;
    private final MontroseTravelLift.Clips liftShore;
    private final int[] cranes;private final CargoTransfer.Pose[] cranePoses;
    BridgeLayers(Game g,Painter p){game=g;painter=p;cranes=DockView.visible(g);cranePoses=new CargoTransfer.Pose[cranes.length];
        liftShore=MontroseTravelLift.shore(g);
        for(int i=0;i<cranes.length;i++)cranePoses[i]=new CargoTransfer.Pose(g,g.dockCranes[cranes[i]],cranes[i]);}

    void to(float screenDepth){
        RiverCamera c=game.layout.camera;
        float depth=(screenDepth-c.screenY(0,0))/(RiverCamera.Q*RiverCamera.DEPTH*c.scale);
        if(depth<=previous)return;
        for(int i=0;game.passages!=null && i<game.passages.bridges.length;i++){LandmarkBridge bridge=game.passages.bridges[i];
            bridge.drawRange(game,painter,game.passages.amount(i),game.passages.passable(i),previous,depth);
        }
        for(int i=0;i<cranes.length;i++)DockView.drawRange(game,painter,cranes[i],cranePoses[i],previous,depth);
        if(game.montroseStage())MontroseTravelLift.drawRange(game,painter,previous,depth,liftShore);
        previous=depth;
    }
}
