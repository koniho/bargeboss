package com.bargeboss.game;

/** Shared continuous engine rumble strength; native hosts render a sustained texture, not taps. */
public final class EngineHaptics {
    private int quiet;
    private float strength;
    void reset(){quiet=0;strength=0;}
    void interrupt(){quiet=24;strength=0;}
    void step(Game g){
        if(!enabled(g)){strength=0;return;}
        if(quiet>0){quiet--;strength=0;return;}
        float target=.10f+.18f*Physics.clamp(g.tug.engineLoad,0,1);
        strength+=(target-strength)*.12f;
    }
    /** Idle occupies band one even during fade-in; frequency stays at device minimum. */
    public static int band(float strength){return strength<.025f?0:1+Math.round(Physics.clamp((strength-.10f)/.18f,0,1)*4);}
    public static float frequency(int band,float minimum,float maximum){
        return minimum+(Math.min(maximum,Math.max(minimum,90))-minimum)*Physics.clamp((band-1)/4f,0,1);
    }
    public static float amplitude(int band){return .10f+.18f*Physics.clamp((band-1)/4f,0,1);}
    private boolean enabled(Game g){return g.mode==Game.PLAY && g.hapticsEnabled && !g.conversation() && g.tug!=null;}
    public float strength(Game g){return enabled(g) && quiet==0?strength:0;}
}
