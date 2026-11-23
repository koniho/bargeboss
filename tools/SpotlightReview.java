package com.bargeboss.game;
/** Shared renderer checks for beam direction, scenery receivers and airborne rings. */
public final class SpotlightReview {
 public static void main(String[] args)throws Exception{
  Game g=RescueBargeTest.scene();Body b=g.rescue.barge.boat;
  g.tug.x=b.x-130;g.tug.y=b.y;g.tug.angle=0;g.rescue.found[0]=true;
  g.layout=Layout.focused(1333,750,b.x,b.y-20,3,620,420);
  DialogueReview.capture(g,"spotlight-vessels-off");g.spotlight.aim(g,0);DialogueReview.capture(g,"spotlight-vessels-on");
  g=new Game();g.startTutorial();g=g.activeGame();g.dialogue.close(g);g.configureCanvas(1333,750);
  float x=TrainingChannelView.factoryX(0),y=TrainingChannelView.factoryY(0);
  g.tug.x=x-160;g.tug.y=y;g.tug.angle=0;g.layout=Layout.focused(1333,750,x,y,2.4f,650,440);
  DialogueReview.capture(g,"spotlight-factory-off");g.spotlight.aim(g,0);DialogueReview.capture(g,"spotlight-factory-on");
 }
}
