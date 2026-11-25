package com.bargeboss.game;
/** Contact threshold, priority, cooldown and fixed-step event ownership. */
public final class SoundEffectsTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        // Hosts loop the soundtrack: a missing one creates no player, a shipped one must be long.
        // A 50 ms looped stand-in restarted Android's MediaPlayer ~20x/s and cut pacing to ~37 FPS.
        if(MusicAudio.available())try{javax.sound.sampled.AudioFileFormat f=javax.sound.sampled.AudioSystem.getAudioFileFormat(new java.io.File("assets/"+MusicAudio.asset()));
            check(f.getFrameLength()/f.getFormat().getFrameRate()>=10,"looped soundtrack lasts at least 10 s");}catch(Exception e){throw new AssertionError(e);}
        Game g=new Game();g.mode=Game.PLAY;
        for(float speed:new float[]{0,1,2,Float.NaN})g.sounds.impact(speed);
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
        for(float speed:new float[]{3,29.9f,30,64.9f,65,100}){
            Game tugs=new Game();tugs.resetPracticeYard();tugs.mode=Game.PLAY;
            Body other=new Body(0,-1,330,500,0);
            tugs.splashes.contact(tugs.tug,other,300,500,speed,0,0,0,speed);tugs.splashes.step(tugs);
            check(SoundEffects.cue(tugs)==(speed<30?SoundEffects.BUMP:speed<65?SoundEffects.THUD:SoundEffects.CRUNCH),"tug collisions use the same strength-based pitch tiers");
        }
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
        check(SoundEffects.asset(SoundEffects.BUMP).endsWith("collision-thunk-light-v1.wav") && SoundEffects.asset(SoundEffects.THUD).endsWith("collision-thunk-medium-v1.wav") && SoundEffects.asset(SoundEffects.CRUNCH).endsWith("collision-thunk-heavy-v1.wav") && SoundEffects.asset(SoundEffects.TUG_THUD).endsWith("collision-thunk-medium-v1.wav"),"collision manifest uses the CC0 Reitanna thunks");
        // Exercise the actual tutorial seawall solver at gentle driving speed, not an injected sound.
        Game wall=new Game();wall.resetPracticeYard();wall.mode=Game.PLAY;
        wall.tug.x=-1600;wall.tug.y=TrainingChannel.BASIN_TOP+wall.tug.half+wall.tug.radius-.5f;
        wall.tug.angle=-(float)Math.PI/2;wall.tug.vx=0;wall.tug.vy=-5;
        wall.step();wall.step();
        check(wall.splashes.emitted>0 && SoundEffects.sequence(wall)>0 && SoundEffects.cue(wall)==SoundEffects.BUMP && SoundEffects.audible(wall),"gentle seawall hit reaches audible sound bus through real collision solver");
        Game ramp=new Game();ramp.resetPracticeYard();ramp.mode=Game.PLAY;
        ramp.splashes.contact(ramp.tug,null,300,500,3,0,0,0,3);
        ramp.splashes.contact(ramp.tug,null,300,500,70,0,0,0,70);
        ramp.splashes.step(ramp);
        check(ramp.splashes.emitted==1 && SoundEffects.cue(ramp)==SoundEffects.CRUNCH,"spray deduplication cannot hide a stronger seawall impact");
        Game launch=new Game();launch.mode=Game.PLAY;
        launch.sounds.firework();check(SoundEffects.cue(launch)==SoundEffects.FIREWORK_LAUNCH && SoundEffects.audible(launch),"launch cue is immediate and shared");
        check(SoundEffects.engineGain(launch)<.3f,"engine ducks for the launch transient");
        for(int i=0;i<6;i++)launch.sounds.step();check(!SoundEffects.audible(launch),"late launch cue is dropped rather than played out of sync");
        launch.radio.transmit(RadioAudio.TEACHER);int launchSequence=SoundEffects.sequence(launch);launch.sounds.firework();
        check(SoundEffects.sequence(launch)==launchSequence,"rocket cannot cut off instructor radio");
        for(int i=0;i<54;i++)launch.sounds.step();launch.sounds.firework();
        check(SoundEffects.cue(launch)==SoundEffects.FIREWORK_LAUNCH,"new rockets sound after radio grace period");
        System.out.println("Collision sound severity, priority, cooldown and stale-event checks passed");
    }
}
