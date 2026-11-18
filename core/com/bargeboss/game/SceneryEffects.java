package com.bargeboss.game;

/** Named scenery emitters share deterministic organic light and exhaust animation. */
public final class SceneryEffects {
    private SceneryEffects() {}
    private static final float KERNEL_RADIUS=4,PEAK_ENERGY=2.45f;
    /** Inland landmarks sit behind foreground props and never change collision geometry. */
    public static void industrialStacks(Game g,Painter p) {
        if(g.trainingChannel()) return;
        for(int segment=g.lockStage()?-1:0;segment<=0;segment++) {
            ChimneyArt.draw(g,p,1,Physics.RIGHT+78,520+segment*688);
            ChimneyArt.draw(g,p,0,Physics.RIGHT+78,740+segment*688);
        }
    }
    /** Several slow frequencies avoid a synchronized on/off pulse across a harbor. */
    public static float intensity(int ticks,float phase) {
        return .91f+.025f*(float)StrictMath.sin(ticks*.0031+phase)
                +.011f*(float)StrictMath.sin(ticks*.0067+phase*1.73);
    }
    /** Work lamps share one subtle supply variation with their spill/reflection. */
    public static float workIntensity(int ticks,float phase) {
        return .91f+(intensity(ticks,phase)-.91f)*.4f;
    }
    private static int tint(int rgb,float alpha) {
        return ((int)Physics.clamp(alpha,0,255)<<24)|(rgb&0xffffff);
    }
    /** Soft layered light, kept in the same source-cell coordinates as its fixture. */
    public static void glow(Painter p,float x,float y,float radius,int color,float opacity,int ticks,float phase) {
        glow(p,x,y,radius,color,opacity,ticks,phase,intensity(ticks,phase));
    }
    /** Explicit energy lets navigation signals bypass ambient variation. */
    public static void glow(Painter p,float x,float y,float radius,int color,float opacity,int ticks,float phase,float power) {
        if(opacity<=0 || radius<=0) return;
        float energy=2.4f*power,breath=1+.05f*(float)StrictMath.sin(ticks*.016+phase*1.13);
        float driftX=radius*.16f*(float)StrictMath.sin(ticks*.009+phase);
        float driftY=radius*.12f*(float)StrictMath.sin(ticks*.013+phase*1.7);
        float scale=radius*breath/KERNEL_RADIUS;
        p.material(x+driftX*.65f,y+driftY*.65f,40*scale,0,0,40*scale,EffectMaterial.GLOW,0,1,tint(color,120*Physics.clamp(opacity*energy/PEAK_ENERGY,0,1)));
        for(int lobe=0;lobe<3;lobe++) {
            double angle=phase+lobe*2.1+ticks*.003;
            float dx=(float)StrictMath.cos(angle)*radius*.65f,dy=(float)StrictMath.sin(angle)*radius*.4f;
            p.circle(x+dx,y+dy,radius*1.65f,tint(color,opacity*energy*5));
        }
        p.circle(x,y,radius*.65f,tint(0xffe6ab,opacity*energy*19));
    }
    /** Small distant fixtures need fewer overlapping layers than a close landmark. */
    public static void compactGlow(Painter p,float x,float y,float radius,int color,float opacity,int ticks,float phase) {
        if(opacity<=0 || radius<=0)return;
        float energy=2.4f*intensity(ticks,phase),breath=1+.05f*(float)StrictMath.sin(ticks*.016+phase*1.13);
        float dx=radius*.16f*(float)StrictMath.sin(ticks*.009+phase);
        float dy=radius*.12f*(float)StrictMath.sin(ticks*.013+phase*1.7);
        float scale=radius*breath/KERNEL_RADIUS;
        p.material(x+dx*.6f,y+dy*.6f,36.64f*scale,0,0,36.64f*scale,EffectMaterial.GLOW,0,1,tint(color,120*Physics.clamp(opacity*energy/PEAK_ENERGY,0,1)));
        p.circle(x,y,radius*.65f,tint(0xffe6ab,opacity*energy*19));
    }
    /** Smoke leaves a declared open top rim; it never creates a new outlet in a painted prop. */
    public static void smoke(Painter p,float x,float y,float radius,float scale,float opacity,int ticks,float phase) {
        smoke(p,x,y,radius,scale,opacity,ticks,phase,false);
    }
    public static void smoke(Painter p,float x,float y,float radius,float scale,float opacity,int ticks,float phase,boolean compact) {
        if(opacity<=0 || scale<=0 || radius<=0) return;
        float wind=1+.2f*(float)StrictMath.sin(ticks*.005+phase);
        for(int i=17;i>=0;i--) {
            float age=((ticks+i*13+(int)(phase*29))%240+240)%240/240f;
            float fade=(1-age)*(1-age)*Math.min(1,age*16);
            float sway=(float)StrictMath.sin(age*8+phase+ticks*.006)*age*6;
            float px=x-(age*35*wind+sway)*scale;
            float py=y-age*91*scale;
            float r=radius*(1+age*4.4f)*(1+.1f*(float)StrictMath.sin(age*12+phase+i));
            float alpha=opacity*fade*(compact?145:175);
            // Dirty olive-brown soot, with rolling detail inside the same bounded puff.
            p.smokePuff(px,py,r*1.2f,WaterFoam.phase(ticks,i+(int)(phase*29)),tint(SmokePuff.INDUSTRIAL,alpha));
        }
        p.circle(x,y,radius*.8f,tint(SmokePuff.SOOT,opacity*100));
    }
}
