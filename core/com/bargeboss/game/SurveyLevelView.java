package com.bargeboss.game;

/** The one draw pipeline for every surveyed level. Levels supply data through SurveyLevel. */
final class SurveyLevelView {
    private SurveyLevelView(){}
    static void draw(Game g,Painter p,SurveyLevel level){
        int water=level.background(g.lighting());p.rect(0,0,g.layout.width,g.layout.height,water);WaterTexture.draw(g,p,water);
        WaveView.surface(g,p);ArcadeView.water(g,new RiverPainter(p,g.layout.camera));
        ShorelineView.water(g,p);LocationBridges.shadows(g,p);
        level.water(g,new RiverPainter(p,g.layout.camera));
        SurveyGround.walls(g,p,level);level.ground().draw(g,p);
        ShorelineView.walls(g,p);
        VesselShoreLight.draw(g,p);
        level.ground(g,p);
        WeatherView.scenery(g,p);
        Renderer.river(g,new RiverPainter(p,g.layout.camera));
    }
}
