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
            g.sounds.impact(speeds[i]);check(SoundEffects.cue(g)==i+2,"severity selects collision cue");
            int sequence=SoundEffects.sequence(g);g.sounds.impact(200);
            check(SoundEffects.sequence(g)==sequence,"contact chatter is bounded");
            check(!g.radio.transmitting(g,g.tug),"collision never triggers radio animation");
            for(int t=0;t<31;t++)g.sounds.step();check(!SoundEffects.audible(g),"stale event cannot play after lifecycle recovery");
        }
        g.radio.transmit(RadioAudio.TEACHER);int sequence=SoundEffects.sequence(g);g.sounds.impact(120);
        check(SoundEffects.sequence(g)==sequence && SoundEffects.cue(g)==0,"radio takes priority over collisions");
        for(int i=0;i<30;i++)g.sounds.step();g.sounds.impact(100);
        check(SoundEffects.cue(g)==4,"collision can play after radio grace period");
        for(int i=0;i<30;i++)g.sounds.step();g.sounds.impact(100,true);
        check(SoundEffects.cue(g)==SoundEffects.TUG_THUD,"even a hard tug-to-tug hit uses the muffled fender thud");
        for(int i=0;i<30;i++)g.sounds.step();g.sounds.splash(.3f);
        check(SoundEffects.cue(g)==SoundEffects.SPLASH_SMALL,"small spray return uses light splash");
        for(int i=0;i<30;i++)g.sounds.step();g.sounds.splash(.9f);
        check(SoundEffects.cue(g)==SoundEffects.SPLASH_LARGE,"heavy spray return uses full splash");
        g.radio.transmit(RadioAudio.TEACHER);sequence=SoundEffects.sequence(g);g.sounds.splash(1);
        check(SoundEffects.sequence(g)==sequence,"water Foley cannot interrupt the instructor");
        g.sounds.complete();g.sounds.complete();
        for(int i=0;i<53;i++)g.sounds.step();
        check(SoundEffects.sequence(g)==sequence,"completion waits for radio grace period");
        g.sounds.step();
        check(SoundEffects.cue(g)==SoundEffects.COMPLETE && SoundEffects.sequence(g)==sequence+1,"simultaneous milestones produce one completion cue");
        sequence=SoundEffects.sequence(g);g.sounds.impact(100);
        check(SoundEffects.sequence(g)==sequence,"completion is protected from contact chatter");
        g.radio.transmit(RadioAudio.REPLY);sequence=SoundEffects.sequence(g);
        for(int i=0;i<54;i++)g.sounds.step();
        check(SoundEffects.sequence(g)==sequence+1 && SoundEffects.cue(g)==SoundEffects.COMPLETE,"same-tick radio takes priority without losing completion");
        sequence=SoundEffects.sequence(g);
        for(int i=0;i<180;i++)g.sounds.step();
        check(SoundEffects.sequence(g)==sequence,"completion never repeats without a milestone");
        g.sounds.complete();
        for(int i=0;i<150;i++){if(i%30==0)g.radio.transmit(RadioAudio.TEACHER);g.sounds.step();}
        sequence=SoundEffects.sequence(g);
        for(int i=0;i<60;i++)g.sounds.step();
        check(SoundEffects.sequence(g)==sequence,"long radio exchange expires old milestone instead of playing a stale reward");
        g.mode=Game.PAUSE;check(!SoundEffects.audible(g),"paused host cannot play cues");
        // One real contact produces its thud at the next simulation update, never on spray landing.
        Game hit=new Game();hit.resetPracticeYard();hit.mode=Game.PLAY;
        hit.splashes.contact(hit.tug,null,300,500,70,0,0,0,70);
        hit.splashes.step(hit);
        check(SoundEffects.sequence(hit)==1 && SoundEffects.cue(hit)==SoundEffects.CRUNCH && SoundEffects.audible(hit),"contact thud is immediate on effect update");
        for(int t=0;t<6;t++)hit.sounds.step();
        check(!SoundEffects.audible(hit),"late host must drop an impact older than 100 ms of simulation");
        for(int t=0;t<150;t++){hit.sounds.step();hit.splashes.step(hit);}
        check(SoundEffects.sequence(hit)==1,"falling collision spray never emits delayed static or another hit");
        check(SoundEffects.asset(SoundEffects.BUMP).endsWith("light-kenney-v2.wav") && SoundEffects.asset(SoundEffects.CRUNCH).endsWith("heavy-kenney-v2.wav"),"collision manifest uses quiet sampled thuds");
        System.out.println("Collision sound severity, priority, cooldown and stale-event checks passed");
    }
}
