package com.bargeboss.game;

/** Small detached painted bow spray: stable variants and a short ballistic loft. */
final class BowShed {
    static final int FLIGHT=24;
    static float lift(int age){float t=Physics.clamp(age/(float)FLIGHT,0,1);return 24*t*(1-t);}
    static float forward(Body b){return b.half+b.radius*.62f+(!b.yacht && b.berth>=0?7:0);}
    static void stamp(Painter p,int seed,float x,float y,float angle,float length,float width,int age,float alpha){
        float z=lift(age),a=angle+(Math.floorMod(seed,3)-1)*.16f;
        float c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a);
        if(p instanceof RiverPainter){x+=RiverCamera.riseX(z);y+=RiverCamera.riseY(z);}
        else y-=z;
        p.spriteAffine(SpriteAtlas.SPLASH,Math.floorMod(seed,4),x,y,c*length,s*length,-s*width,c*width,alpha);
    }
}
