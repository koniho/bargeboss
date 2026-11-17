package com.bargeboss.game;

/** Painted side elevations; shared offsets preserve independent horizontal parallax. */
final class TitleShoreView {
    private TitleShoreView() {}
    static void draw(Game g,Painter p){
        float h=TitleView.horizon(g),extent=g.layout.width;
        layer(p,SpriteAtlas.TITLE_STEEL,extent,h+22,780,(g.visualTicks*.12f)%780);
        float offset=(g.visualTicks*.21f+Math.max(0,585-(extent-352-100)))%960;
        layer(p,SpriteAtlas.TITLE_ROCKS,extent,h+54,960,offset);
    }
    private static void layer(Painter p,int atlas,float extent,float bottom,float width,float offset){
        float height=width/3;
        for(float x=-offset;x<extent;x+=width)
            p.sprite(atlas,0,x+width*.5f,bottom-height*.5f,width,height,0,1);
    }
}
