package com.bargeboss.game;
/** Contact threshold, priority, cooldown and fixed-step event ownership. */
public final class SoundEffectsTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game g=new Game();g.mode=Game.PLAY;
        for(float speed:new float[]{0,3,7,Float.NaN})g.sounds.impact(speed);
        check(SoundEffects.sequence(g)==0,"resting and invalid contacts remain quiet");
        float[] speeds={10,40,90};
        for(int i=0;i<3;i++){
            g.sounds.impact(speeds[i]);check(SoundEffects.cue(g)==i+2,"severity selects original collision clip");
            int sequence=SoundEffects.sequence(g);g.sounds.impact(200);
            check(SoundEffects.sequence(g)==sequence,"contact chatter is bounded");
            check(!g.radio.transmitting(g,g.tug),"collision never triggers radio animation");
            for(int t=0;t<31;t++)g.sounds.step();check(!SoundEffects.audible(g),"stale event cannot play after lifecycle recovery");
        }
        g.radio.transmit(RadioAudio.TEACHER);int sequence=SoundEffects.sequence(g);g.sounds.impact(120);
        check(SoundEffects.sequence(g)==sequence && SoundEffects.cue(g)==0,"radio takes priority over collisions");
        for(int i=0;i<30;i++)g.sounds.step();g.sounds.impact(100);
        check(SoundEffects.cue(g)==4,"collision can play after radio grace period");
        g.mode=Game.PAUSE;check(!SoundEffects.audible(g),"paused host cannot play cues");
        System.out.println("Collision sound severity, priority, cooldown and stale-event checks passed");
    }
}
