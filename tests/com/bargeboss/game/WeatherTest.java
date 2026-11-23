package com.bargeboss.game;

/** Weather stays bounded, deterministic, optional and routed away from steering. */
public final class WeatherTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Trace implements Painter {
        int lines,rects,materials,beaconGlows;float beaconWidth;long fogHash;long hash,firstHash;float length,width;
        public void line(float x,float y,float xx,float yy,float w,int c){lines++;length+=Math.abs(yy-y);width+=w;hash=hash*31+Float.floatToIntBits(x);hash=hash*31+Float.floatToIntBits(y);if(lines==192)firstHash=hash;check(Float.isFinite(x+y+xx+yy+w),"finite rain");}
        public void rect(float x,float y,float w,float h,int c){rects++;check((c>>>24)<=63,"weather wash keeps boats readable");}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){
            if((color&0xffffff)==(NavigationLights.RED&0xffffff) || (color&0xffffff)==(NavigationLights.GREEN&0xffffff)){beaconGlows++;beaconWidth=Math.max(beaconWidth,ax);}
            materials++;check(Float.isFinite(x+y+ax+ay+bx+by),"finite fog patch");
            check(kind==EffectMaterial.GLOW && (color>>>24)<=72,"bounded soft fog");
            fogHash=fogHash*31+Float.floatToIntBits(x);fogHash=fogHash*31+Float.floatToIntBits(y);
        }
        public void poly(float[] p,int c){throw new AssertionError("no weather mesh builds");}
        public void circle(float x,float y,float r,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){throw new AssertionError("no weather textures");}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){throw new AssertionError("no weather textures");}
    }
    private static Trace draw(Game g){Trace t=new Trace();WeatherView.backgroundFog(g,t);WeatherView.draw(g,t);return t;}
    private static void tap(Game g,float x,float y){g.pointerDown(93,x,y);g.pointerUp(93,x,y);ReplayInput.finishMenu(g);}
    public static void run(){
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
        Trace clear=draw(g);check(clear.lines+clear.rects+clear.materials==0,"clear weather costs no primitives");
        g.settings.adjustRain(99);g.settings.adjustLightning(99);
        for(int tick:new int[]{0,120,124,130,143,144,539,540,544,35999,36000}){
            g.visualTicks=tick;float x=g.tug.x,y=g.tug.y;Trace a=draw(g),b=draw(g);
            check(a.lines>0 && a.lines<=WeatherView.MAX_STREAKS && a.rects<=3,"fixed heavy weather submission cap");
            check(a.hash==b.hash && a.lines==b.lines,"draw time does not advance rain");
            check(g.visualTicks==tick && g.tug.x==x && g.tug.y==y,"weather rendering cannot mutate gameplay");
        }
        g.settings.adjustLightning(-99);
        g.layout=Layout.focused(960,600,g.tug.x,g.tug.y,2.5f,480,300);Trace near=draw(g);
        g.layout=Layout.focused(960,600,g.tug.x,g.tug.y,.65f,480,300);Trace far=draw(g);
        check(far.lines>near.lines,"zoom out increases rain density");
        check(near.firstHash==far.firstHash,"zoom preserves existing rain positions");
        check(far.length/far.lines<near.length/near.lines && far.width/far.lines<near.width/near.lines,"zoom out makes rain finer");
        int previous=0;
        for(int step=0;step<=100;step++){
            float zoom=2.5f-step*.024f;g.layout=Layout.focused(960,600,g.tug.x,g.tug.y,zoom,480,300);Trace t=draw(g);
            check(t.lines>=previous && t.lines<=WeatherView.MAX_STREAKS,"rain density monotonic and capped through zoom");previous=t.lines;
        }
        g.settings.resetWeather();g.settings.adjustFog(99);Trace fog=draw(g);
        check(fog.lines==0 && fog.rects==2 && fog.materials>=WeatherView.MAX_FOG_PATCHES+1 && fog.materials<=WeatherView.MAX_FOG_PATCHES+1+FogLighting.MAX_LIGHTS,"fog has bounded background, low mist and foreground layers");
        check(draw(g).fogHash==fog.fogHash,"fog draw is deterministic");
        g.visualTicks+=120;check(draw(g).fogHash!=fog.fogHash,"fog drifts with simulation ticks");
        for(float zoom:new float[]{.1f,.65f,1,4.5f}){
            g.layout=Layout.focused(960,600,g.tug.x,g.tug.y,zoom,480,300);
            check(draw(g).materials>=9 && draw(g).materials<=9+FogLighting.MAX_LIGHTS,"fog and scattered lights bounded across zoom");
        }
        g.settings.adjustFog(-99);check(draw(g).rects==0,"fog off restores clear");
        Game lamps=new Game();lamps.selectChallenge(Challenges.MONTROSE);lamps.reset();lamps.tug.active=false;
        for(Body b:lamps.cargo)if(b!=null)b.active=false;
        lamps.layout=Layout.focused(960,600,MontroseBeaconArt.x(0),MontroseBeaconArt.y(0),1,480,350);
        lamps.settings.adjustFog(3);lamps.visualTicks=15;Trace lit=new Trace();FogLighting.draw(lamps,lit);
        check(lit.beaconGlows>0 && lit.beaconWidth>200*lamps.layout.camera.scale,"beacon fog glow is broad and colored");
        check(lit.materials<=FogLighting.MAX_LIGHTS,"scattered lights have fixed budget");
        lamps.visualTicks=100;Trace dark=new Trace();FogLighting.draw(lamps,dark);check(dark.beaconGlows==0,"beacon haze follows true dark interval");
        lamps.settings.resetWeather();Trace off=new Trace();FogLighting.draw(lamps,off);check(off.materials==0,"no scattering without fog");
        g.settings.resetWeather();g.settings.developerWaves=true;tap(g,30,90);
        tap(g,250,196);check(g.settings.rainLevel()==1,"rain control");
        tap(g,250,228);check(g.settings.lightningLevel()==1,"lightning control");
        g.pointerDown(93,250,196);g.pointerCancel(93);g.pointerUp(93,250,196);ReplayInput.finishMenu(g);check(g.settings.rainLevel()==1,"cancel weather tap");
        tap(g,250,260);check(g.settings.fogLevel()==1,"fog control");
        tap(g,60,330);g.visualTicks+=4;check(WeatherView.flash(g)==1,"test flash peaks after four ticks");
        check(!g.pointer,"weather taps never steer");tap(g,200,330);
        check(g.settings.fogLevel()==0 && g.settings.rainLevel()==0 && g.settings.lightningLevel()==0 && draw(g).lines==0 && WeatherView.flash(g)==0,"reset all returns clear");
        System.out.println(checks+" weather budget, determinism, flash and input assertions passed");
    }
    public static void main(String[] args){run();}
}
