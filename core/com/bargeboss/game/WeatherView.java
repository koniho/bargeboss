package com.bargeboss.game;

/** Bounded, session-only weather experiment. Uses simulation time; no particle state or textures. */
public final class WeatherView {
    private WeatherView(){}
    public static final int MAX_STREAKS=192;
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
    public static void draw(Game g,Painter p){
        if(!g.montroseStage())return;
        int rain=g.settings.rainLevel();float flash=flash(g);
        if(rain==0 && flash==0)return;
        float width=g.layout.width,height=g.layout.height;
        // Restrained storm tint and diffuse cloud illumination, below every HUD/control.
        if(rain>0)p.rect(0,0,width,height,((rain*5)<<24)|0x142032);
        if(flash>0)p.rect(0,0,width,height,(Math.round(flash*34)<<24)|0xc5d9ed);
        for(int i=0;i<rain*64;i++){
            float depth=DEPTH[i],span=height+48;
            int cycle=150-(int)(depth*70);
            float fall=(Y[i]*span+Math.floorMod(g.visualTicks,cycle)*span/cycle)%span;
            float y=fall-24,x=(X[i]*(width+32)-fall*.18f+width+32)%(width+32)-16;
            float length=9+depth*12;
            int alpha=22+(int)(depth*32)+rain*3;
            p.line(x,y,x-length*.18f,y+length,.55f+depth*.55f,(alpha<<24)|0x9ebccf);
        }
    }
}
