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
        EngineHaptics pulses=g.engineHaptics;int idleCount=0,workCount=0;
        g.tug.engineLoad=0;
        for(int tick=0;tick<240;tick++){pulses.step(g);int pulse=g.pollEngineHaptic();if(pulse>0){idleCount++;check(pulse==1,"idle pulse is light");}check(g.pollEngineHaptic()==0,"engine pulse drains once");}
        pulses.reset();g.tug.engineLoad=1;
        for(int tick=0;tick<240;tick++){pulses.step(g);int pulse=g.pollEngineHaptic();if(pulse>0){workCount++;check(pulse==2,"loaded engine pulse is stronger");}}
        check(idleCount==5 && workCount==13,"engine pulses are load-dependent and bounded");
        g.haptic(Game.HAPTIC_IMPACT);
        for(int tick=0;tick<24;tick++){pulses.step(g);check(g.pollEngineHaptic()==0,"contact feedback gets priority over engine rumble");}
        check(g.pollHaptic()==Game.HAPTIC_IMPACT,"engine never replaces impact event");
        g.hapticsEnabled=false;
        for(int tick=0;tick<60;tick++){pulses.step(g);check(g.pollEngineHaptic()==0,"haptics preference silences engine");}
        g.hapticsEnabled=true;pulses.reset();
        for(int tick=0;tick<18;tick++)pulses.step(g);
        g.background();check(g.pollEngineHaptic()==0,"pause discards engine pulse");
        System.out.println("Engine audio idle/load, headroom and lifecycle checks passed");
    }
}
