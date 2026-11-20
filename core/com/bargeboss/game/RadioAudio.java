package com.bargeboss.game;

/** Fixed-step one-shot events; hosts play identical predecoded CB clips. */
public final class RadioAudio {
    public static final int TEACHER=0,REPLY=1,COUNT=2,BRIDGE=2;
    private final SoundEffects sounds;
    RadioAudio(SoundEffects sounds){this.sounds=sounds;}
    private int sequence,cue,age=1000,transmitter;
    public void transmit(int cue){if(cue<0 || cue>=COUNT)throw new IllegalArgumentException("Unknown radio cue");this.cue=cue;transmitter=cue;sequence++;age=0;sounds.emit(cue);}
    public void receiveBridge(){transmit(TEACHER);transmitter=BRIDGE;}
    public int transmitter(){return transmitter;}
    public void step(){if(age<1000)age++;}
    public float progress(){return Physics.clamp(age/(cue==TEACHER?54f:30f),0,1);}
    public boolean transmitting(Game g,Body boat){return progress()<1 && sequence>0 && transmitter!=BRIDGE && boat==(cue==TEACHER?g.teacherBoat():g.tug);}
    public static int sequence(Game root){return root.activeGame().radio.sequence;}
    public static int cue(Game root){return root.activeGame().radio.cue;}
    public static boolean audible(Game root){Game g=root.activeGame();return g.mode==Game.PLAY && g.radio.age<30;}
    public static String asset(int cue){return SoundEffects.asset(cue);}
}
