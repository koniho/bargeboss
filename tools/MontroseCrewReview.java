package com.bargeboss.game;
public final class MontroseCrewReview {
 public static void main(String[] args)throws Exception{
  Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.configureCanvas(1333,750);
  g.layout=Layout.focused(1333,750,MontroseRescue.RAFT_X-100,MontroseRescue.RAFT_Y+30,2.4f,666,400);
  g.rescue.distress(g);for(int i=0;i<24;i++)g.step();DialogueReview.capture(g,"montrose-chas-distress");
  MontroseCrewTest.answer(g,0);for(int i=0;i<24;i++)g.step();DialogueReview.capture(g,"montrose-chas-batteries");
  g.dialogue.close(g);g.layout=Layout.focused(1333,750,MontroseBeaconArt.x(MontroseBeaconArt.HORN),MontroseBeaconArt.y(MontroseBeaconArt.HORN),1.6f,666,410);
  DialogueReview.capture(g,"montrose-rafted-lighthouse");
 }
}
