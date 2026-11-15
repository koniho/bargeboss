package com.bargeboss.game;

/** Independent debug-tool checks; not shipped by the normal build. */
public final class ProfileScenarioTest {
    public static void main(String[] args) {
        int checks=0;
        for(String scene:ProfileScenario.NAMES){
            Game a=new Game(),b=new Game();
            ProfileScenario.prepare(a,scene,1320,600);ProfileScenario.prepare(b,scene,1320,600);
            Game x=a.activeGame(),y=b.activeGame();
            if(x.tutorial==null || x.tutorial.lesson!=y.tutorial.lesson || x.visualTicks!=y.visualTicks
                    || x.tug.x!=y.tug.x || x.tug.y!=y.tug.y)throw new AssertionError("Non-repeatable fixture "+scene);
            checks++;
            int tick=x.visualTicks;ProfileScenario.replayTick(a);
            if(x.visualTicks!=tick+1)throw new AssertionError("Replay step must be one shared tick");checks++;
            System.out.println(scene+" lesson="+y.tutorial.lesson+" tick="+y.visualTicks);
        }
        try{ProfileScenario.prepare(new Game(),"invalid",960,600);throw new AssertionError("Unknown fixture accepted");}
        catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" developer fixture checks passed");
    }
}
