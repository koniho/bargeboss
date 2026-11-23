package com.bargeboss.game;
/** Actual shared-renderer storm rescue, flight, boarding and injury checkpoints. */
public final class StormRescueReview {
 public static void main(String[] args)throws Exception{
  Game g=RescueBargeTest.scene();MontroseRescue r=g.rescue;Body b=r.barge.boat;r.barge.towed=true;
  g.tug.x=b.x-75;g.tug.y=b.y+100;g.spotlight.aim(g,(float)StrictMath.atan2(r.y[0]-g.tug.y,r.x[0]-g.tug.x));g.abilities.reveal(g,g.tug);
  for(int i=0;i<15;i++)g.abilities.step(g);DialogueReview.capture(g,"storm-rescue-spotlight");
  RescueBargeTest.fling(g,r.x[0]+15,r.y[0]);RescueBargeTest.ticks(g,20);DialogueReview.capture(g,"storm-rescue-ring-flight");
  boolean captured=false;for(int i=0;i<1600 && r.state[0]!=4;i++){RescueBargeTest.ticks(g,1);if(r.state[0]==3 && r.barge.timer[0]==38){DialogueReview.capture(g,"storm-rescue-climbing");captured=true;}}
  if(!captured)throw new AssertionError("Missing boarding checkpoint");DialogueReview.capture(g,"storm-rescue-aboard");
  g=RescueBargeTest.scene();r=g.rescue;g.tug.x=r.x[0];g.tug.y=r.y[0];RescueBargeTest.ticks(g,12);DialogueReview.capture(g,"storm-rescue-hurt");
  System.out.println("Montrose rotation preparation frames: "+LevelAssets.challenge(Challenges.MONTROSE).rotationFrames());
 }
}
