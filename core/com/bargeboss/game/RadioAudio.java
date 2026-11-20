package com.bargeboss.game;

/** Fixed-step one-shot events; hosts play identical predecoded CB clips. */
public final class RadioAudio {
    public static final int TEACHER=0,REPLY=1,COUNT=2;
    private int sequence,cue,age=1000;
    public void transmit(int cue){if(cue<0 || cue>=COUNT)throw new IllegalArgumentException("Unknown radio cue");this.cue=cue;sequence++;age=0;}
    public void step(){if(age<1000)age++;}
    public static int sequence(Game root){return root.activeGame().radio.sequence;}
    public static int cue(Game root){return root.activeGame().radio.cue;}
    public static boolean audible(Game root){Game g=root.activeGame();return g.mode==Game.PLAY && g.radio.age<30;}
    public static String asset(int cue){return cue==TEACHER?"audio/cb-receive-v1.wav":"audio/cb-reply-v1.wav";}
}
