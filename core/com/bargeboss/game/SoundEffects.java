package com.bargeboss.game;

/** Bounded fixed-step one-shot bus. Radio has priority over contact chatter. */
public final class SoundEffects {
    public static final int COUNT=5,BUMP=2,THUD=3,CRUNCH=4;
    private int sequence,cue,age=1000,cooldown;
    void step(){if(age<1000)age++;if(cooldown>0)cooldown--;}
    void emit(int value){cue=value;sequence++;age=0;}
    void impact(float speed){
        if(!Float.isFinite(speed) || speed<8 || cooldown>0 || cue<2 && age<24)return;
        emit(speed<30?BUMP:speed<65?THUD:CRUNCH);cooldown=18;
    }
    public static int sequence(Game root){return root.activeGame().sounds.sequence;}
    public static int cue(Game root){return root.activeGame().sounds.cue;}
    public static boolean audible(Game root){Game g=root.activeGame();return g.mode==Game.PLAY && g.sounds.age<30;}
    public static String asset(int cue){
        return new String[]{"audio/cb-receive-v1.wav","audio/cb-reply-v1.wav","audio/collision-bump-v1.wav","audio/collision-thud-v1.wav","audio/collision-crunch-v1.wav"}[cue];
    }
}
