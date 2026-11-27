package com.bargeboss.game;

/** Study cameras remain reproducible and the lift fixture exercises the real gate simulation. */
public final class SceneryProfileTest {
 public static void main(String[] args){
  for(String scene:new String[]{"montrose-powerup-review","montrose-pavilion-study","montrose-concession-study","islands-cottage-study","islands-channel-study","islands-lift-study","montrose-markers-review","montrose-boost-review","damage-sink-review","damage-impact-review"}){
   Game a=new Game(),b=new Game();ProfileScenario.prepare(a,scene,1320,600);ProfileScenario.prepare(b,scene,1320,600);
   if(a.tug.x!=b.tug.x || a.tug.y!=b.tug.y || a.layout.camera.scale!=b.layout.camera.scale)throw new AssertionError("Non-repeatable study: "+scene);
   int tick=a.visualTicks;ProfileScenario.replayTick(a);if(a.visualTicks!=tick+1)throw new AssertionError("Study replay skips shared ticks");
   if(scene.equals("montrose-powerup-review") && (a.transit.spent[3]!=0 || a.transit.boostTicks!=0))throw new AssertionError("Ready powerup fixture starts uncollected");
   if(scene.equals("montrose-boost-review")){for(int k=0;k<35;k++)ProfileScenario.replayTick(a);if(a.developerPanel.open || a.transit.boostTicks<=0)throw new AssertionError("Boost replay must remain in gameplay");}
   if(scene.equals("islands-lift-study")){
    float old=a.arena.footbridges[1].spanPoint(0)[2];
    for(int k=1;k<60;k++){ProfileScenario.replayTick(a);float z=a.arena.footbridges[1].spanPoint(0)[2];if(z<old || a.arena.open[1]<1 && z==old)throw new AssertionError("Fixture fails to exercise continuous lift tick "+k);old=z;}
    if(!a.arena.passable(1))throw new AssertionError("Bridge never opened");
   }
  }
  System.out.println("Ten scenery/effects cameras/replays and every active opening lift update passed");
 }
}
