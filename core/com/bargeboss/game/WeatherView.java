package com.bargeboss.game;

/** Bounded, session-only weather experiment. Uses simulation time; no particle state or textures. */
public final class WeatherView {
    private WeatherView(){}
    public static final int MAX_STREAKS=384, MAX_FOG_PATCHES=8;
    private static final float[] X=new float[MAX_STREAKS],Y=new float[MAX_STREAKS],DEPTH=new float[MAX_STREAKS];
    static {for(int i=0;i<MAX_STREAKS;i++){X[i]=noise(i*3+1);Y[i]=noise(i*3+2);DEPTH[i]=noise(i*3+3);}}
    private static float noise(int seed){int h=seed*0x45d9f3b;h=(h^(h>>>16))*0x45d9f3b;return (h&65535)/65535f;}
    private static float pulse(int age){
        if(age<0 || age>=24)return 0;
        return age<4?SeaWaves.smooth(0,4,age):1-SeaWaves.smooth(4,24,age);
    }
    static float flash(Game g){
        int level=g.settings.lightningLevel();
        float automatic=level==0?0:pulse(Math.floorMod(g.visualTicks,level==1?720:420)-120)*(level==1?.7f:1);
        return Math.max(automatic,pulse(g.visualTicks-g.settings.weatherFlashTick));
    }
    /** Airborne mist overlays the world, including vessels. Fixed screen
     * coverage avoids multiplying patches at overview zoom; phases never wrap visibly. */
    public static void lowFog(Game g,Painter p){
        int fog=g.settings.fogLevel();if(!g.montroseStage() || fog==0)return;
        float w=g.layout.width,h=g.layout.height;
        for(int i=0;i<MAX_FOG_PATCHES;i++){
            double phase=g.visualTicks*.0009+i*2.39996;
            float x=w*((i%4+.5f)/4+.07f*(float)StrictMath.sin(phase));
            float y=h*(.3f+(i/4)*.43f+.06f*(float)StrictMath.cos(phase*.73));
            p.material(x,y,w*.8f,0,w*.08f,h*.27f,EffectMaterial.GLOW,0,1,
                ((fog*24)<<24)|0x99aeb5);
        }
    }
    /** Scenery haze stays behind the river's vessel/depth merge. The broad upper
     * falloff suggests distance without hard bands or a scene-sized blur target. */
    public static void backgroundFog(Game g,Painter p){
        int fog=g.settings.fogLevel();if(!g.montroseStage() || fog==0)return;
        float w=g.layout.width,h=g.layout.height;
        p.rect(0,0,w,h,((fog*10)<<24)|0x82949a);
        p.material(w*.5f,h*.08f,w*2,0,0,h*1.6f,EffectMaterial.GLOW,0,1,
            ((fog*20)<<24)|0x9aabb3);
    }
    public static void draw(Game g,Painter p){
        if(!g.montroseStage())return;
        int rain=g.settings.rainLevel();float flash=flash(g);
        if(rain==0 && flash==0 && g.settings.fogLevel()==0)return;
        lowFog(g,p);
        float width=g.layout.width,height=g.layout.height;
        float scale=g.layout.camera.scale;
        float distant=1-SeaWaves.smooth(.6f,1.6f,scale);
        float size=1-distant*.55f;
        float desired=rain*64*(1+distant);
        int haze=g.settings.fogLevel()*6+Math.round(rain*3*(1-SeaWaves.smooth(.15f,.6f,scale)));
        // Restrained storm tint and diffuse cloud illumination, below every HUD/control.
        if(rain>0)p.rect(0,0,width,height,((rain*5)<<24)|0x142032);
        if(haze>0)p.rect(0,0,width,height,(haze<<24)|0x82949a);
        FogLighting.draw(g,p);
        if(flash>0)p.rect(0,0,width,height,(Math.round(flash*34)<<24)|0xc5d9ed);
        for(int i=0;i<Math.min(MAX_STREAKS,(int)StrictMath.ceil(desired));i++){
            // Stable streak identities and staggered opacity avoid a density pop.
            float weight=i<rain*64?1:SeaWaves.smooth(i,i+24,desired);
            float depth=DEPTH[i],span=height+48;
            int cycle=150-(int)(depth*70);
            float fall=(Y[i]*span+Math.floorMod(g.visualTicks,cycle)*span/cycle)%span;
            float y=fall-24,x=(X[i]*(width+32)-fall*.18f+width+32)%(width+32)-16;
            float length=(9+depth*12)*size;
            int alpha=Math.round((22+(int)(depth*32)+rain*3)*(1-distant*.25f)*weight);
            if(alpha==0)continue;
            p.line(x,y,x-length*.18f,y+length,(.55f+depth*.55f)*(1-distant*.3f),(alpha<<24)|0x9ebccf);
        }
    }
}
