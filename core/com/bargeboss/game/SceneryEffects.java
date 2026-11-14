package com.bargeboss.game;

/** Named scenery emitters share deterministic organic light and exhaust animation. */
public final class SceneryEffects {
    private SceneryEffects() {}
    /** Inland landmarks sit behind foreground props and never change collision geometry. */
    public static void industrialStacks(Game g,Painter p) {
        if(g.trainingChannel()) return;
        for(int segment=g.bridgeStage()?-1:0;segment<=0;segment++) {
            ChimneyArt.draw(g,p,1,Physics.RIGHT+78,520+segment*688);
            ChimneyArt.draw(g,p,0,Physics.RIGHT+78,740+segment*688);
        }
    }
    /** Several slow frequencies avoid a synchronized on/off pulse across a harbor. */
    public static float intensity(int ticks,float phase) {
        return .91f+.065f*(float)StrictMath.sin(ticks*.021+phase)
                +.028f*(float)StrictMath.sin(ticks*.067+phase*1.73)
                +.012f*(float)StrictMath.sin(ticks*.137+phase*.61);
    }
    private static int tint(int rgb,float alpha) {
        return ((int)Physics.clamp(alpha,0,255)<<24)|(rgb&0xffffff);
    }
    /** Soft layered light, kept in the same source-cell coordinates as its fixture. */
    public static void glow(Painter p,float x,float y,float radius,int color,float opacity,int ticks,float phase) {
        if(opacity<=0 || radius<=0) return;
        float energy=intensity(ticks,phase),breath=1+.05f*(float)StrictMath.sin(ticks*.016+phase*1.13);
        float driftX=radius*.16f*(float)StrictMath.sin(ticks*.009+phase);
        float driftY=radius*.12f*(float)StrictMath.sin(ticks*.013+phase*1.7);
        // Many weak layers give a smooth falloff without a large hard-edged disc.
        for(int layer=10;layer>=1;layer--) {
            float t=layer/10f,r=radius*(.8f+4.2f*t)*breath;
            p.circle(x+driftX*t,y+driftY*t,r,tint(color,opacity*energy*(1.4f+3.2f*(1-t))));
        }
        for(int lobe=0;lobe<3;lobe++) {
            double angle=phase+lobe*2.1+ticks*.003;
            float dx=(float)StrictMath.cos(angle)*radius*.65f,dy=(float)StrictMath.sin(angle)*radius*.4f;
            p.circle(x+dx,y+dy,radius*1.65f,tint(color,opacity*energy*5));
        }
        p.circle(x,y,radius*.65f,tint(0xffe6ab,opacity*energy*19));
    }
    /** Smoke leaves a declared open top rim; it never creates a new outlet in a painted prop. */
    public static void smoke(Painter p,float x,float y,float radius,float scale,float opacity,int ticks,float phase) {
        if(opacity<=0 || scale<=0 || radius<=0) return;
        float wind=1+.2f*(float)StrictMath.sin(ticks*.005+phase);
        for(int i=17;i>=0;i--) {
            float age=((ticks+i*13+(int)(phase*29))%240+240)%240/240f;
            float fade=(1-age)*(1-age)*Math.min(1,age*16);
            float sway=(float)StrictMath.sin(age*8+phase+ticks*.006)*age*6;
            float px=x-(age*35*wind+sway)*scale;
            float py=y-age*91*scale;
            float r=radius*(1+age*4.4f)*(1+.1f*(float)StrictMath.sin(age*12+phase+i));
            float alpha=opacity*fade*54;
            // Unequal lobes roll into one plume; older puffs soften and disperse.
            p.circle(px,py,r,tint(0x7b8589,alpha));
            p.circle(px-r*.55f,py+r*.11f,r*.74f,tint(0x68767b,alpha*.7f));
            p.circle(px+r*.44f,py-r*.25f,r*.68f,tint(0xb6aaa0,alpha*.48f));
        }
        p.circle(x,y,radius*.8f,tint(0x7e8587,opacity*38));
    }
}
