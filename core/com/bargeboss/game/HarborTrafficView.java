package com.bargeboss.game;

/** Cheap screen culling for harbor traffic: off-screen hulls skip the depth-sorted vessel pass. */
final class HarborTrafficView {
    private HarborTrafficView(){}
    /** Within `pad` world units of the current view: worth floating, waking and colliding with the shore. */
    static boolean near(float[] view,Body b,float pad){return b.x>view[0]-pad && b.x<view[2]+pad && b.y>view[1]-pad && b.y<view[3]+pad;}
    static boolean visible(Game g,Body b){
        RiverCamera c=g.layout.camera;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y),pad=MooredBoats.CELL_WORLD*c.scale;
        return x>-pad && x<g.layout.width+pad && y>-pad && y<g.layout.height+pad;
    }
}
