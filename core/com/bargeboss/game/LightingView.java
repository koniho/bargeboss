package com.bargeboss.game;

/** Shared warm key light and cool shadows; all scenery uses the same light direction. */
public final class LightingView {
    private LightingView() {}
    public static void shadow(Painter p,float x,float y,float width,float depth,float alpha) {
        p.poly(new float[]{x-width*.5f,y-depth*.3f,x+width*.4f,y-depth*.3f,
                x+width*.65f,y+depth,x-width*.25f,y+depth},((int)(65*alpha)<<24)|0x162c40);
    }
    public static void quays(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int side=0;side<2;side++) for(int dock=0;dock<3;dock++) if(g.berthEnabled(dock)) {
            float x=c.screenX(300,g.dockY(dock));
            float y=c.screenY(side==0?Physics.RIGHT:Physics.LEFT,492)+(side==0?-16:15)*s;
            float pulse=1+.04f*(float)StrictMath.sin(g.visualTicks*.035+dock);
            for(int ring=12;ring>=1;ring--) {
                float radius=(3+ring*2.3f)*s*pulse;
                p.circle(x,y,radius,0x04ffc55e);
            }
            p.rect(x-4*s,y-3*s,8*s,8*s,0xff223448);
            p.rect(x-3*s,y-4*s,6*s,5*s,0xffffd577);
            p.rect(x-2*s,y-4*s,4*s,2*s,0xfffff0b0);
        }
    }
    public static void hull(Painter p,Body b) {
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        // Offset to screen lower-right in the world plane; fixed across every heading.
        p.line(b.x-c*b.half-4,b.y-s*b.half+5,b.x+c*b.half-4,b.y+s*b.half+5,b.radius*1.7f,0x44203443);
    }
}
