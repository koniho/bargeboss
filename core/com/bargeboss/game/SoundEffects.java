package com.bargeboss.game;

/** Bounded fixed-step one-shot bus. Radio has priority over contact chatter. */
public final class SoundEffects {
    public static final int COUNT=9,BUMP=2,THUD=3,CRUNCH=4,TUG_THUD=5,SPLASH_SMALL=6,SPLASH_LARGE=7,COMPLETE=8;
    private int sequence,cue,age=1000,cooldown,pendingComplete;
    void step(){
        if(age<1000)age++;if(cooldown>0)cooldown--;
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
    void impact(float speed){impact(speed,false);}
    void impact(float speed,boolean tugContact){
        if(!Float.isFinite(speed) || speed<8 || cooldown>0 || cue<2 && age<30)return;
        emit(tugContact?TUG_THUD:speed<30?BUMP:speed<65?THUD:CRUNCH);cooldown=18;
    }
    void splash(float power){
        if(!Float.isFinite(power) || power<.18f || cooldown>0 || cue<2 && age<54)return;
        emit(power<.6f?SPLASH_SMALL:SPLASH_LARGE);cooldown=24;
    }
    public static int sequence(Game root){return root.activeGame().sounds.sequence;}
    public static int cue(Game root){return root.activeGame().sounds.cue;}
    /** Drop stale contact events instead of replaying a late bump after host recovery. */
    public static boolean audible(Game root){Game g=root.activeGame();return g.mode==Game.PLAY && g.sounds.age<(g.sounds.cue>=BUMP && g.sounds.cue<=TUG_THUD?6:30);}
    public static String asset(int cue){
        return new String[]{"audio/radio-receive-recorded-v1.wav","audio/radio-reply-recorded-v1.wav","audio/fender-light-kenney-v2.wav","audio/fender-medium-kenney-v2.wav","audio/fender-heavy-kenney-v2.wav","audio/fender-medium-kenney-v2.wav","audio/water-splash-small-v1.wav","audio/water-splash-large-v1.wav","audio/step-clear-kenney-v1.wav"}[cue];
    }
}
