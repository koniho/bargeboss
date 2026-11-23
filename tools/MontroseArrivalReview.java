package com.bargeboss.game;
/** Captures the actual shared arrival camera, welcome panel, and free mooring fleet. */
public final class MontroseArrivalReview {
 public static void main(String[] args)throws Exception{
  Game g=new Game();g.configureCanvas(1333,600);g.selectChallenge(Challenges.MONTROSE);g.reset();
  DialogueReview.capture(g,"montrose-arrival-south");for(int i=0;i<180;i++)g.step();DialogueReview.capture(g,"montrose-arrival-northbound");
  for(int i=0;i<200;i++)g.step();DialogueReview.capture(g,"montrose-gavin-welcome");
  g.montroseArrival.clear();g.dialogue.close(g);g.layout=Layout.focused(1333,600,MontroseHarbor.mapX(250),MontroseHarbor.mapY(640),1.8f,666,340);
  DialogueReview.capture(g,"montrose-three-mooring-sails");
 }
}
