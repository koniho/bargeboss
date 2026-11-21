package com.bargeboss.game;

public final class EngineAudioTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void run(){
        Game g=new Game();g.reset();g.mode=Game.PLAY;g.tug.engineLoad=0;
        check(EngineAudio.gain(g,0)>0 && EngineAudio.gain(g,1)==0,"Marine idle without truck");
        float previous=-1;
        for(int i=0;i<=100;i++){
            g.tug.engineLoad=i/100f;
            float working=EngineAudio.gain(g,1);
            check(working>=previous,"Working mix must rise continuously");previous=working;
            for(int layer=0;layer<2;layer++)check(EngineAudio.rate(g,layer)>=.5f && EngineAudio.rate(g,layer)<=2,"Native rate bounds");
            check(EngineAudio.gain(g,0)+working<1,"Mix headroom");
        }
        g.tug.vx=g.tug.vy=0;float stalled=EngineAudio.gain(g,1);g.tug.vx=80;
        check(stalled==EngineAudio.gain(g,1) && stalled>.4f,"Loaded stationary tug must roar");
        for(int mode:new int[]{Game.TITLE,Game.PAUSE,Game.HELP,Game.SETTINGS,Game.FINISH,Game.CHALLENGES}){
            g.mode=mode;check(!EngineAudio.audible(g) && EngineAudio.gain(g,0)==0 && EngineAudio.gain(g,1)==0,"Non-play silence");
        }
        g.startTutorial();check(EngineAudio.audible(g),"Practice routes to active game");
        g.activeGame().mode=Game.PAUSE;check(!EngineAudio.audible(g),"Paused practice silence");
        g=new Game();g.resetPracticeYard();g.mode=Game.PLAY;g.tug.engineLoad=1;
        float idle=EngineAudio.gain(g,0),working=EngineAudio.gain(g,1),rate=EngineAudio.rate(g,1);
        g.sounds.impact(90);
        check(Math.abs(EngineAudio.gain(g,1)-working*.28f)<.00001f,"impact makes room in engine mix immediately");
        check(EngineAudio.rate(g,1)==rate,"ducking does not change engine speed/pitch");
        previous=0;
        for(int tick=0;tick<24;tick++){
            g.sounds.step();float gain=EngineAudio.gain(g,1);
            check(gain>=previous && gain<=working,"engine restores smoothly without overshoot");previous=gain;
        }
        check(EngineAudio.gain(g,0)==idle && EngineAudio.gain(g,1)==working,"full engine mix restored after 400ms");
        EngineHaptics rumble=g.engineHaptics;g.tug.engineLoad=0;
        for(int tick=0;tick<120;tick++)rumble.step(g);
        float idleRumble=g.engineRumble();check(idleRumble>.09f && idleRumble<.11f,"idle is a sustained low rumble");
        check(g.engineRumble()==idleRumble,"reading rumble does not consume a pulse");
        g.tug.engineLoad=1;previous=idleRumble;
        for(int tick=0;tick<120;tick++){rumble.step(g);float strength=g.engineRumble();check(strength>=previous && strength-previous<.03f,"load ramps continuously without taps");previous=strength;}
        check(g.engineRumble()>.27f && g.engineRumble()<=.28f,"loaded rumble remains bounded");
        g.haptic(Game.HAPTIC_IMPACT);
        for(int tick=0;tick<24;tick++){rumble.step(g);check(g.engineRumble()==0,"contact feedback gets priority over engine rumble");}
        check(g.pollHaptic()==Game.HAPTIC_IMPACT,"engine never replaces impact event");
        rumble.step(g);check(g.engineRumble()>0 && g.engineRumble()<.04f,"rumble fades back after contact");
        g.hapticsEnabled=false;check(g.engineRumble()==0,"setting silences engine immediately");
        g.hapticsEnabled=true;g.background();check(g.engineRumble()==0,"pause discards engine rumble");
        System.out.println("Engine audio idle/load, headroom and lifecycle checks passed");
    }
}
