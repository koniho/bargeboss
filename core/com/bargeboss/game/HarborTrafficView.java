package com.bargeboss.game;

/** Cheap screen culling for harbor traffic: off-screen hulls skip the depth-sorted vessel pass. */
final class HarborTrafficView {
    private HarborTrafficView(){}
    static boolean visible(Game g,Body b){
        RiverCamera c=g.layout.camera;float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y),pad=MooredBoats.CELL_WORLD*c.scale;
        return x>-pad && x<g.layout.width+pad && y>-pad && y<g.layout.height+pad;
    }
}
