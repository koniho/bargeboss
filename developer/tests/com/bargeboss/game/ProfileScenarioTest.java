package com.bargeboss.game;

/** Independent debug-tool checks; not shipped by the normal build. */
public final class ProfileScenarioTest {
    public static void main(String[] args) {
        int checks=0;
        for(String scene:ProfileScenario.NAMES){
            Game a=new Game(),b=new Game();
            ProfileScenario.prepare(a,scene,1320,600);ProfileScenario.prepare(b,scene,1320,600);
            Game x=a.activeGame(),y=b.activeGame();
            if((x.tutorial==null)!=(y.tutorial==null) || (x.tutorial!=null && x.tutorial.lesson!=y.tutorial.lesson) || x.visualTicks!=y.visualTicks
                    || x.tug.x!=y.tug.x || x.tug.y!=y.tug.y)throw new AssertionError("Non-repeatable fixture "+scene);
            checks++;
            int tick=x.visualTicks;ProfileScenario.replayTick(a);
            if(x.visualTicks!=tick+1)throw new AssertionError("Replay step must be one shared tick");checks++;
            System.out.println(scene+" lesson="+(y.tutorial==null?-1:y.tutorial.lesson)+" tick="+y.visualTicks);
        }
        circle();
        try{ProfileScenario.prepare(new Game(),"invalid",960,600);throw new AssertionError("Unknown fixture accepted");}
        catch(IllegalArgumentException expected){checks++;}
        System.out.println(checks+" developer fixture checks passed");
    }
    private static void circle(){
        Game a=new Game(),b=new Game();
        ProfileScenario.prepare(a,"tow-tight-circle",1320,600);
        ProfileScenario.prepare(b,"tow-tight-circle",1320,600);
        double sweep=0,previous=StrictMath.atan2(a.tug.y+400,a.tug.x-600);
        for(int i=0;i<6000;i++){
            ProfileScenario.replayTick(a);ProfileScenario.replayTick(b);
            if(a.tug.x!=b.tug.x || a.tug.y!=b.tug.y || a.cargo[0].angle!=b.cargo[0].angle)throw new AssertionError("Circle replay diverged");
            if(a.tow.count()!=1 || !a.tow.connected(a.cargo[0],a.tug))throw new AssertionError("Circle tow detached at "+i);
            if(!a.inWater(a.tug.x,a.tug.y) || !a.inWater(a.cargo[0].x,a.cargo[0].y))throw new AssertionError("Circle left water");
            double angle=StrictMath.atan2(a.tug.y+400,a.tug.x-600),delta=angle-previous;
            while(delta>Math.PI)delta-=2*Math.PI;while(delta< -Math.PI)delta+=2*Math.PI;
            sweep+=delta;previous=angle;
        }
        if(sweep<4*Math.PI)throw new AssertionError("Circle did not complete two laps: "+sweep);
        System.out.println("Single-barge circle: "+(sweep/(2*Math.PI))+" laps over 6000 deterministic physical ticks");
    }
}
