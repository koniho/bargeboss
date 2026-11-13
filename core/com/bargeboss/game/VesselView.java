package com.bargeboss.game;

/** Directional painted hulls with deterministic, phase-driven water and exhaust. */
public final class VesselView {
    private VesselView() {}
    public static void draw(Game g,Painter p,Body b,boolean tug) {
        int tick=g.visualTicks;
        float bob=b.lineAttached?0:(float)StrictMath.sin(tick*.055+b.id*1.7)*(tug?.55f:.35f);
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        ArcadeView.wake(p,b,tick);
        float size=2*(b.half+b.radius)/(tug?.84f:.89f);
        SpriteAtlas.draw(p,tug?SpriteAtlas.TUG:SpriteAtlas.BARGE,b.angle,b.x,b.y+bob,size,size*(tug?1.05f:1.2f),1);
        if(tug) {
            boolean working=(b==g.tug && (g.pushing || g.navigating)) || b.speed()>8;
            boolean pushing=working && (b==g.tug?(g.control==Game.PUSHING || g.control==Game.STEERING || g.control==Game.ALIGNING):b.vy<0);
            exhaust(p,b,tick,working?1:.35f,pushing);
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
                SpriteAtlas.draw(p,atlas,b.angle,b.x+c*xx-s*yy,b.y+s*xx+c*yy+bob,cargoSize,cargoSize,1);
            }
            if(b.lineAttached || (b.berth>=0 && g.canTie(b))) {
                float pulse=(float)StrictMath.sin(tick*.12)*.5f+.5f;
                p.circle(b.x-c*28+s*9,b.y-s*28-c*9,1.7f+pulse,b.lineAttached?0xffd3e8a1:0xffffd34e);
            }
        }
    }
    private static void exhaust(Painter p,Body b,int tick,float power,boolean strain) {
        // Chimney smoke rises in screen space, independently of the river rotation.
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        float sx=b.x-c*14,sy=b.y-s*14;
        boolean river=p instanceof RiverPainter;
        for(int i=5;i>=0;i--) {
            float t=((tick*(strain?2:1)+i*13)%84)/84f,r=1.3f+t*(3+power*3);
            float dx=-5-t*(12+power*12)+(float)StrictMath.sin(t*7)*2;
            float dy=-19-t*(13+power*16);
            float x=sx+(river?-dy:dx),y=sy+(river?dx:dy);
            int alpha=(int)((1-t)*(strain?155:105));
            int color=(alpha<<24)|(strain?0x708493:0xc4d2d2);
            p.circle(x,y,r,color);
        }
    }
}
