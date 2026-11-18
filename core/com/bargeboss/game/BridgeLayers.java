package com.bargeboss.game;
/** One bounded frame-local cursor through bridge geometry and the shared depth stream. */
final class BridgeLayers {
    private final Game game;private final Painter painter;private float previous=Float.NEGATIVE_INFINITY;
    BridgeLayers(Game g,Painter p){game=g;painter=p;}
    void to(float screenDepth){
        if(game.passages==null || game.passages.bridges.length==0)return;
        RiverCamera c=game.layout.camera;
        float depth=(screenDepth-c.screenY(0,0))/(RiverCamera.Q*RiverCamera.DEPTH*c.scale);
        if(depth<=previous)return;
        for(int i=0;i<game.passages.bridges.length;i++){LandmarkBridge bridge=game.passages.bridges[i];
            bridge.drawRange(game,painter,game.passages.amount(i),game.passages.passable(i),previous,depth);
        }
        previous=depth;
    }
}
