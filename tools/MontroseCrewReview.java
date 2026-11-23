package com.bargeboss.game;
public final class MontroseCrewReview {
 public static void main(String[] args)throws Exception{
  Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.configureCanvas(1333,750);
  g.layout=Layout.focused(1333,750,MontroseRescue.RAFT_X-100,MontroseRescue.RAFT_Y+30,2.4f,666,400);
  g.rescue.distress(g);for(int i=0;i<24;i++)g.step();DialogueReview.capture(g,"montrose-chas-distress");
  MontroseCrewTest.answer(g,0);for(int i=0;i<24;i++)g.step();DialogueReview.capture(g,"montrose-chas-batteries");
  g.dialogue.close(g);g.layout=Layout.focused(1333,750,MontroseBeaconArt.x(MontroseBeaconArt.HORN),MontroseBeaconArt.y(MontroseBeaconArt.HORN),1.6f,666,410);
  DialogueReview.capture(g,"montrose-rafted-lighthouse");
  g.tug.x=g.cargo[0].x;g.tug.y=g.cargo[0].y-80;
  g.rescue.tapRaft(g,g.cargo[0].x+11,g.cargo[0].y);ChasRadioTest.ticks(g,90);
  DialogueReview.capture(g,"montrose-chas-slip-assignments");
  ChasRadioTest.ticks(g,ChasRadio.SHOW_TICKS);g.tow.attach(g,g.cargo[0],g.tug);g.tug.vx=20;
  ChasRadioTest.ticks(g,ChasRadio.FIRST_TOW_TICKS+24);DialogueReview.capture(g,"montrose-chas-tow-commentary");
  g.rescue.radio.delivered(0);ChasRadioTest.ticks(g,180);DialogueReview.capture(g,"montrose-alex-energy-rumrunner");
  g.rescue.radio.delivered(1);ChasRadioTest.ticks(g,ChasRadio.SHOW_TICKS+180);DialogueReview.capture(g,"montrose-chas-fireball-never-wetter");
 }
}
