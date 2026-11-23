package com.bargeboss.game;
public final class ShoreReactionTest {
 private static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void run(){
  Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();g.tutorial.lesson=Tutorial.EXTINGUISH;g.tutorial.setup(g);
  FireworksLesson f=g.tutorial.fireworks;
  check(!f.waterHit(0,0) && f.contactSteam.emitted==0,"missed hose makes no steam");
  check(f.waterHit(FireworksLesson.x(0),FireworksLesson.y(0)) && f.contactSteam.emitted==1 && f.heat(0)>0,"water makes immediate contact steam while fire still burns");
  f.waterHit(FireworksLesson.x(0),FireworksLesson.y(0));check(f.contactSteam.emitted==1,"contact steam emission bounded per source");
  f.waterHit(FireworksLesson.x(1),FireworksLesson.y(1));check(f.contactSteam.emitted==2,"second pile has independent steam");
  for(int spectator=TrainingYardCrew.count()-TrainingYardCrew.spectators();spectator<TrainingYardCrew.count();spectator++){float[] pos=new float[2];TrainingYardCrew.position(spectator,pos);check(!g.inWater(pos[0]-7,pos[1]) && !g.inWater(pos[0]+7,pos[1]) && !g.inWater(pos[0],pos[1]-7) && !g.inWater(pos[0],pos[1]+7),"all spectators stand on dry ground around the basin");}
  int id=6+TrainingYardCrew.count()-2;float[] at=new float[2];DockWorkers.position(g,id,at);
  check(!g.inWater(at[0],at[1]),"finale worker stands on dry ground");
  check(!g.crew.hit(g,0,0,1,0),"miss does not start a recoil");
  check(g.crew.hit(g,at[0],at[1],70,0) && g.crew.ticks[id]==150,"water contact starts recoil");
  float far=0;for(int i=0;i<160;i++){g.crew.step(g);DockWorkers.position(g,id,at);check(!g.inWater(at[0],at[1]),"retreat never runs into water");far=Math.max(far,Physics.length(g.crew.dx[id],g.crew.dy[id]));}
  check(far>8 && g.crew.ticks[id]==0 && g.crew.dx[id]==0 && g.crew.dy[id]==0,"worker retreats then returns to their place");
  f.reset();check(f.contactSteam.emitted==0,"reset clears steam pool");g.crew.reset();check(g.crew.ticks[id]==0,"reset clears reactions");
  System.out.println("Immediate contact steam, per-pile budget, worker recoil and dry retreat passed");
 }
 public static void main(String[]a){run();}
}
