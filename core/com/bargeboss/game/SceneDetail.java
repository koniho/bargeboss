package com.bargeboss.game;

/** Continuous screen-size detail weights; no draw-time state or zoom thresholds to chatter. */
final class SceneDetail {
    private SceneDetail(){}
    static float fade(float pixels,float low,float high){float t=Physics.clamp((pixels-low)/(high-low),0,1);return t*t*(3-2*t);}
    static float boat(Game g,float length,float x,float y){
        float size=fade(length*g.layout.camera.scale,36,72);
        float near=1-fade(Physics.length(x-g.tug.x,y-g.tug.y),70,180);
        return Math.max(size,near);
    }
    static float foam(float scale,float length,float width,int seed,float age){
        float detail=fade((length+width)*scale,12,28);
        float density=(seed&1)==0?1:detail;
        return density*(1-(1-detail)*fade(age,.55f,.95f));
    }
}
