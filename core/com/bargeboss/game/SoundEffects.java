package com.bargeboss.game;

/** Bounded fixed-step one-shot bus. Radio has priority over contact chatter. */
public final class SoundEffects {
    public static final int COUNT=13,VOICES=5,FIREWORK_LAUNCH=9,FIREWORK_BURST=10,BUMP=2,THUD=3,CRUNCH=4,TUG_THUD=5,SPLASH_SMALL=6,SPLASH_LARGE=7,COMPLETE=8;
    private int sequence,cue,age=1000,cooldown,pendingComplete,impactAge=1000,fireworkBursts;
    void step(){
        if(age<1000)age++;if(impactAge<1000)impactAge++;if(cooldown>0)cooldown--;
        if(pendingComplete>0){
            pendingComplete--;
            if(cooldown==0 && (cue>=2 || age>=54)){
                pendingComplete=0;emit(COMPLETE);cooldown=24;
            }
        }
    }
    /** Coalesce simultaneous milestones and leave room for instructor radio. */
    void complete(){pendingComplete=120;}
    void emit(int value){
        if(value<2 && cue==COMPLETE && age==0)pendingComplete=120;
        cue=value;sequence++;age=0;
    }
    void impact(float speed){
        if(!Float.isFinite(speed) || speed<3 || cooldown>0 || cue<2 && age<30)return;
        impactAge=0;emit(speed<30?BUMP:speed<65?THUD:CRUNCH);cooldown=18;
    }
    /** Real launch transient, emitted with the rocket; never interrupt instructor speech. */
    void firework(){
        if(cue<2 && age<54 || cue>=BUMP && cue<=TUG_THUD && age<6)return;
        emit(FIREWORK_LAUNCH);impactAge=0;
    }
    void fireworkBurst(){
        if(cue<2 && age<54 || cue>=BUMP && cue<=TUG_THUD && age<6)return;
        emit(FIREWORK_BURST+fireworkBursts++%3);impactAge=0;
    }
    public static boolean fireworkCue(int value){return value>=FIREWORK_LAUNCH && value<COUNT;}
    /** Firework tails overlap each other; radio/contact cues remain exclusive. */
    public static boolean interrupts(int incoming,int playing){return incoming==playing || !fireworkCue(incoming) || !fireworkCue(playing);}
    public static float ambienceGain(Game root){SoundEffects s=root.activeGame().sounds;return s.cue<2 && s.age<54?.25f:1;}
    void splash(float power){
        if(!Float.isFinite(power) || power<.18f || cooldown>0 || cue<2 && age<54)return;
        emit(power<.6f?SPLASH_SMALL:SPLASH_LARGE);cooldown=24;
    }
    /** Short engine duck exposes the contact, then smoothly restores the diesel bed. */
    public static float engineGain(Game root){
        float t=Physics.clamp((root.activeGame().sounds.impactAge-6)/18f,0,1);
        return .28f+.72f*t*t*(3-2*t);
    }
    public static float gain(Game root){return .85f*root.activeGame().settings.soundGain();}
    public static int sequence(Game root){return root.activeGame().sounds.sequence;}
    public static int cue(Game root){return root.activeGame().sounds.cue;}
    /** Drop stale contact events instead of replaying a late bump after host recovery. */
    public static boolean audible(Game root){Game g=root.activeGame();return g.mode==Game.PLAY && gain(root)>0 && g.sounds.age<(g.sounds.cue>=BUMP && g.sounds.cue<=TUG_THUD || fireworkCue(g.sounds.cue)?6:30);}
    public static String asset(int cue){
        return new String[]{"audio/radio-receive-recorded-v1.wav","audio/radio-reply-recorded-v1.wav","audio/collision-thunk-light-v1.wav","audio/collision-thunk-medium-v1.wav","audio/collision-thunk-heavy-v1.wav","audio/collision-thunk-medium-v1.wav","audio/water-splash-small-v1.wav","audio/water-splash-large-v1.wav","audio/step-clear-kenney-v1.wav","audio/firework-launch-recorded-v2.wav","audio/firework-burst-a-v1.wav","audio/firework-burst-b-v1.wav","audio/firework-burst-c-v1.wav"}[cue];
    }
}
