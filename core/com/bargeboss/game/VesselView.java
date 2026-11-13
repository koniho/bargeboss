package com.bargeboss.game;

/** Directional painted hulls with deterministic, phase-driven water and exhaust. */
public final class VesselView {
    private VesselView() {}
    public static void draw(Game g,Painter p,Body b,boolean tug) {
        int tick=g.visualTicks;
        float bob=WaterSurface.lift(g,b);
        boolean river=p instanceof RiverPainter;
        float hullX=b.x+(river?bob/RiverCamera.DEPTH:0),hullY=b.y-(river?0:bob);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        ArcadeView.wake(p,b,tick);
        if(river) LightingView.hull(p,b);
        float size=2*(b.half+b.radius)/(tug?.84f:.89f);
        SpriteAtlas.draw(p,tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,hullX,hullY,size,size*(tug?1.05f:1.2f),1);
        if(tug) {
            boolean working=(b==g.tug && (g.pushing || g.navigating)) || b.speed()>8;
            boolean pushing=working && (b==g.tug?(g.control==Game.PUSHING || g.control==Game.STEERING || g.control==Game.ALIGNING):b.vy<0);
            exhaust(p,b,tick,b.engineLoad,bob);
            if(pushing) for(int i=0;i<4;i++) {
                float t=((tick+i*9)%30)/30f,xx=b.half+b.radius+t*9,yy=(i%2==0?-1:1)*(6+t*10);
                p.rect(b.x+c*xx-s*yy,b.y+s*xx+c*yy,2+t*2,1.5f,0xbbe5ffff);
            }
            if(b==g.tug && g.braking && b.speed()>8) for(int i=0;i<5;i++) {
                float t=((tick+i*7)%25)/25f,xx=-b.half-b.radius-t*15,yy=(i-2)*3;
                p.rect(b.x+c*xx-s*yy,b.y+s*xx+c*yy,2+3*t,1.5f,0xaae5ffff);
            }
            if(b==g.tug && g.damageCooldown>50 && (tick/5)%2==0)
                p.circle(b.x,b.y,18,0x44ff7049);
        } else {
            int remaining=b.unloadingTicks==0?3:Math.max(0,3-(b.unloadingTicks+50)/60);
            int atlas=b.berth==0?SpriteAtlas.STEEL:b.berth==2?SpriteAtlas.BULK:SpriteAtlas.CONTAINER;
            // Each load is an independent sprite; the crew removes one per lift.
            for(int i=0;i<remaining;i++) {
                float xx=b.berth==0?0:-17+i*17,yy=b.berth==0?-8+i*8:0;
                float cargoSize=b.berth==0?58:b.berth==2?27:26;
                SpriteAtlas.draw(p,atlas,b.angle,hullX+c*xx-s*yy,hullY+s*xx+c*yy,cargoSize,cargoSize,1);
            }
            if(b.lineAttached || (b.berth>=0 && g.canTie(b))) {
                float pulse=(float)StrictMath.sin(tick*.12)*.5f+.5f;
                p.circle(b.x-c*28+s*9,b.y-s*28-c*9,1.7f+pulse,b.lineAttached?0xffd3e8a1:0xffffd34e);
            }
        }
    }
    private static void exhaust(Painter p,Body b,int tick,float power,float bob) {
        boolean river=p instanceof RiverPainter;
        float size=2*(b.half+b.radius)/.84f;
        float scale=river?((RiverPainter)p).camera.scale:1;
        float sx=river?((RiverPainter)p).camera.screenX(b.x,b.y):b.x;
        float sy=river?((RiverPainter)p).camera.screenY(b.x,b.y):b.y;
        sx+=SpriteAtlas.stackOffset(b.angle,river,false)*size*scale;
        sy+=(SpriteAtlas.stackOffset(b.angle,river,true)*size*1.05f-bob)*scale;
        float trailX=river?b.vy:b.vx;
        if(river) p=((RiverPainter)p).canvas();
        power=Physics.clamp(power,0,1);
        // A small throat plume closes the gap between discrete puffs and the mouth.
        p.circle(sx,sy,(1.8f+power)*scale,0xbb7b8495);
        for(int i=19;i>=0;i--) {
            float density=Physics.clamp(5+15*power-i,0,1);if(density==0) continue;
            float t=((tick+i*31)%150)/150f;
            float r=(1.8f+t*(4+power*6))*scale;
            float x=sx+(-t*(12+power*18)-trailX*t*.16f+3*t*(float)StrictMath.sin(t*9+i))*scale;
            float y=sy-t*(32+power*36)*scale;
            int alpha=(int)((1-t)*(1-t)*density*(135+95*power));
            int shade=(int)(166-75*power),color=(alpha<<24)|(shade<<16)|((shade+9)<<8)|(shade+20);
            p.circle(x,y,r,color);
            p.circle(x-r*.6f,y+r*.15f,r*.7f,color);
            p.circle(x+r*.5f,y-r*.25f,r*.65f,((alpha*2/3)<<24)|0xc1c9d2);
        }
    }
}
