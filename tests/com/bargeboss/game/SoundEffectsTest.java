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
        // Collisions make no sound: contacts of every strength, against hulls or walls, stay quiet.
        for(float speed:new float[]{3,29.9f,30,64.9f,65,100,200}){
            Game tugs=new Game();tugs.resetPracticeYard();tugs.mode=Game.PLAY;
            Body other=new Body(0,-1,330,500,0);
            tugs.splashes.contact(tugs.tug,other,300,500,speed,0,0,0,speed);tugs.splashes.contact(tugs.tug,null,300,520,speed,0,0,0,speed);
            for(int t=0;t<80;t++)tugs.splashes.step(tugs);
            check(tugs.splashes.emitted>0 && SoundEffects.sequence(tugs)==0,"collisions emit spray but no sound");
        }
        int sequence;
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
        // The real tutorial seawall solver emits spray on contact, and still no sound.
        Game wall=new Game();wall.resetPracticeYard();wall.mode=Game.PLAY;
        wall.tug.x=-1600;wall.tug.y=TrainingChannel.BASIN_TOP+wall.tug.half+wall.tug.radius-.5f;
        wall.tug.angle=-(float)Math.PI/2;wall.tug.vx=0;wall.tug.vy=-5;
        wall.step();wall.step();
        check(wall.splashes.emitted>0 && SoundEffects.sequence(wall)==0,"seawall contact through the real solver is silent");
        Game launch=new Game();launch.mode=Game.PLAY;
        launch.sounds.firework();check(SoundEffects.cue(launch)==SoundEffects.FIREWORK_LAUNCH && SoundEffects.audible(launch),"launch cue is immediate and shared");
        check(SoundEffects.engineGain(launch)<.3f,"engine ducks for the launch transient");
        for(int i=0;i<6;i++)launch.sounds.step();check(!SoundEffects.audible(launch),"late launch cue is dropped rather than played out of sync");
        launch.radio.transmit(RadioAudio.TEACHER);int launchSequence=SoundEffects.sequence(launch);launch.sounds.firework();
        check(SoundEffects.sequence(launch)==launchSequence,"rocket cannot cut off instructor radio");
        for(int i=0;i<54;i++)launch.sounds.step();launch.sounds.firework();
        check(SoundEffects.cue(launch)==SoundEffects.FIREWORK_LAUNCH,"new rockets sound after radio grace period");
        System.out.println("Silent collisions, splash/completion priority, cooldown and stale-event checks passed");
    }
}
