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
        float fog=g.weather.fog(g);if(fog==0)return;
        float w=g.layout.width,h=g.layout.height;
        for(int i=0;i<MAX_FOG_PATCHES;i++){
            double phase=g.visualTicks*.0009+i*2.39996;
            float x=w*((i%4+.5f)/4+.07f*(float)StrictMath.sin(phase));
            float y=h*(.3f+(i/4)*.43f+.06f*(float)StrictMath.cos(phase*.73));
            p.material(x,y,w*.8f,0,w*.08f,h*.27f,EffectMaterial.GLOW,0,1,
                (Math.round(fog*24)<<24)|0x99aeb5);
        }
    }
    /** Scenery haze stays behind the river's vessel/depth merge. The broad upper
     * falloff suggests distance without hard bands or a scene-sized blur target. */
    public static void backgroundFog(Game g,Painter p){
        float fog=g.weather.fog(g);if(fog==0)return;
        float w=g.layout.width,h=g.layout.height;
        p.rect(0,0,w,h,(Math.round(fog*10)<<24)|0x82949a);
        p.material(w*.5f,h*.08f,w*2,0,0,h*1.6f,EffectMaterial.GLOW,0,1,
            (Math.round(fog*20)<<24)|0x9aabb3);
    }
    /** Scenery-level weather, drawn by every stage after its ground and before the vessel depth merge:
     * evening darkening and background haze. */
    public static void scenery(Game g,Painter canvas){g.weather.background(g,canvas);backgroundFog(g,canvas);}
    public static final int MAX_FLAKES=320;
    /** Blizzard: wind-driven snow in three depth layers, gust streaks, and a breathing whiteout veil.
     * Screen-space and stateless (stable flake identities from the fixed noise table). */
    static void snow(Game g,Painter p){
        StageWeather w=g.weather;float b=w.blizzard,gust=w.gust(g.visualTicks);
        float width=g.layout.width,height=g.layout.height,angle=w.windAngle(g.visualTicks);
        float wx=(float)StrictMath.cos(angle),wy=(float)StrictMath.sin(angle);
        // Screen drift: down the screen plus the wind's screen-space push.
        float driftX=-wx*(1.2f+3.5f*gust)-.4f,driftY=1.4f+1.2f*gust;
        int count=Math.min(MAX_FLAKES,Math.round(MAX_FLAKES*Math.min(1,b*1.15f)));
        for(int i=0;i<count;i++){
            float depth=DEPTH[i%MAX_STREAKS],speed=.5f+depth*1.1f,t=g.visualTicks*speed;
            float x=Math.floorMod((int)(X[i%MAX_STREAKS]*(width+80)+noise(i*7+5)*900+t*driftX),(int)width+80)-40;
            float y=Math.floorMod((int)(Y[i%MAX_STREAKS]*(height+80)+noise(i*7+6)*700+t*driftY),(int)height+80)-40;
            x+=3*(float)StrictMath.sin(g.visualTicks*.05f+i);
            float size=1+depth*2.6f;int alpha=Math.round((90+depth*140)*Math.min(1,b*1.5f));
            if(gust>.55f && i%3==0){
                // Gust streak: the flake smeared along the wind.
                float len=(6+depth*16)*gust;p.line(x,y,x-driftX*len*.35f,y-driftY*len*.35f,size*.7f,(Math.round(alpha*.7f)<<24)|0xf2f7fb);
            }else p.rect(x-size*.5f,y-size*.5f,size,size,(alpha<<24)|0xf4f8fc);
        }
        // Whiteout veil: thickens with the blizzard and pulses with the gusts.
        int veil=Math.round(255*(.06f+.22f*b+.12f*gust));
        p.rect(0,0,width,height,(veil<<24)|0xdfe8f0);
        p.material(width*.5f,height*.5f,width*1.4f,0,0,height*1.4f,EffectMaterial.GLOW,0,1,(Math.round(60*gust)<<24)|0xffffff);
    }
    public static void draw(Game g,Painter p){
        float rain=g.weather.rain(g);float flash=flash(g);
        if(g.weather.blizzard>0)snow(g,p);
        if(rain==0 && flash==0 && g.weather.fog(g)==0)return;
        lowFog(g,p);
        float width=g.layout.width,height=g.layout.height;
        float scale=g.layout.camera.scale;
        float distant=1-SeaWaves.smooth(.6f,1.6f,scale);
        float size=1-distant*.55f;
        float desired=rain*64*(1+distant);
        int haze=Math.round(g.weather.fog(g)*6)+Math.round(rain*3*(1-SeaWaves.smooth(.15f,.6f,scale)));
        // Restrained storm tint and diffuse cloud illumination, below every HUD/control.
        if(rain>0)p.rect(0,0,width,height,(Math.round(rain*5)<<24)|0x142032);
        if(haze>0)p.rect(0,0,width,height,(haze<<24)|0x82949a);
        FogLighting.draw(g,p);
        if(flash>0)p.rect(0,0,width,height,(Math.round(flash*34)<<24)|0xc5d9ed);
        for(int i=0;i<Math.min(MAX_STREAKS,(int)StrictMath.ceil(desired+(rain>0?24:0)));i++){
            // Stable streak identities and staggered opacity avoid a density pop.
            float weight=SeaWaves.smooth(i,i+24,desired+24)*Math.min(1,rain*8);
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
