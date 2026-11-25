package com.bargeboss.game;

/** The one draw pipeline for every surveyed level. Levels supply data through SurveyLevel. */
final class SurveyLevelView {
    private SurveyLevelView(){}
    static void draw(Game g,Painter p,SurveyLevel level){
        p.rect(0,0,g.layout.width,g.layout.height,level.background(g.lighting()));
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);LocationBridges.shadows(g,p);
        SurveyGround.walls(g,p,level);level.ground().draw(g,p);
        ShorelineView.walls(g,p);
        VesselShoreLight.draw(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
}
