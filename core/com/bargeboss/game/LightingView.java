package com.bargeboss.game;

/** Shared warm key light and cool shadows; all scenery uses the same light direction. */
public final class LightingView {
    private LightingView() {}
    public static void quays(Game g,Painter p) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        for(int side=0;side<2;side++) for(int lamp=HarborAtmosphere.firstLamp(g);lamp<=5;lamp++) {
            float bank=HarborAtmosphere.lampBank(side),along=HarborAtmosphere.lampY(lamp);
            if(g.tutorial!=null && TrainingChannel.contains(bank,along)) continue;
            if(!HarborAtmosphere.visible(g,bank,along,55*s)) continue;
            float x=c.screenX(bank,along),foot=c.screenY(bank,along),y=foot-17*s;
            float phase=lamp*1.31f+side*.77f;
            float pulse=SceneryEffects.intensity(g.visualTicks,phase);
            // Cached feathered haze and a wide diffuse pool replace the hard triangle.
            DiffuseLight.lamp(p,x,foot,s,pulse*g.lighting().lamps);
            p.line(x,foot,x,y,2.4f*s,0xff18252d);
            p.line(x+.7f*s,foot,x+.7f*s,y,0.65f*s,0xff8f7654);
            SceneryEffects.glow(p,x,y,3.3f*s,0xffffb354,1,g.visualTicks,phase);
            p.rect(x-3.1f*s,y-2.9f*s,6.2f*s,6.1f*s,0xff20282d);
            p.rect(x-2.1f*s,y-2.0f*s,4.2f*s,3.8f*s,((int)(255*pulse)<<24)|0xffb94d);
            p.rect(x-1.25f*s,y-1.6f*s,2.5f*s,2.3f*s,((int)(255*pulse)<<24)|0xffebba);
            p.line(x-3.7f*s,y-3.4f*s,x+3.7f*s,y-3.4f*s,1.2f*s,0xff4b4438);
        }
    }
}
