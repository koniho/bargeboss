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
        for(int side=0;side<2;side++) for(int lamp=HarborAtmosphere.firstLamp(g);lamp<=5;lamp++) {
            float bank=HarborAtmosphere.lampBank(side),along=HarborAtmosphere.lampY(lamp);
            if(!HarborAtmosphere.visible(g,bank,along,55*s)) continue;
            float x=c.screenX(bank,along),foot=c.screenY(bank,along),y=foot-17*s;
            float phase=lamp*1.31f+side*.77f;
            float pulse=SceneryEffects.intensity(g.visualTicks,phase);
            // Light spills onto the quay; the emissive lens remains a small crisp focal point.
            p.poly(new float[]{x,y+2*s,x-11*s,foot+4*s,x+16*s,foot+5*s},((int)(16*pulse)<<24)|0xffbc68);
            p.line(x,foot,x,y,2.4f*s,0xff18252d);
            p.line(x+.7f*s,foot,x+.7f*s,y,0.65f*s,0xff8f7654);
            SceneryEffects.glow(p,x,y,3.3f*s,0xffffb354,1,g.visualTicks,phase);
            p.rect(x-3.1f*s,y-2.9f*s,6.2f*s,6.1f*s,0xff20282d);
            p.rect(x-2.1f*s,y-2.0f*s,4.2f*s,3.8f*s,0xffffb94d);
            p.rect(x-1.25f*s,y-1.6f*s,2.5f*s,2.3f*s,0xffffebba);
            p.line(x-3.7f*s,y-3.4f*s,x+3.7f*s,y-3.4f*s,1.2f*s,0xff4b4438);
        }
    }
    public static void hull(Painter p,Body b) {
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
        // Offset to screen lower-right in the world plane; fixed across every heading.
        p.line(b.x-c*b.half-4,b.y-s*b.half+5,b.x+c*b.half-4,b.y+s*b.half+5,b.radius*1.7f,0x44203443);
    }
}
