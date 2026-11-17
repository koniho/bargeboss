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
        System.out.println("Engine audio idle/load, headroom and lifecycle checks passed");
    }
}
