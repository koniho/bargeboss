package com.bargeboss.game;
final class DockLineCrewTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void run(){
  Game g=TravelLiftTest.scene();
  for(int id=0;id<4;id++){DockLineCrew c=DockLineCrew.crew(g,id);check(!MontroseHarbor.contains(c.x(id%2),c.y(id%2)),"Dockhand feet stay on real dry quay/dock");}
  Body b=g.cargo[0];DockLineCrew c=g.haulout.lineCrew;b.x=MontroseHaulout.X;b.y=MontroseHaulout.Y+25;c.start(b);
  check(!c.caught() && new TowView.Segments(g).count==0,"Windup before line flight");
  for(int i=0;i<40;i++)c.step();check(!c.caught() && new TowView.Segments(g).count==48,"Two cast lines in depth stream");
  for(int i=40;i<DockLineCrew.CATCH;i++)c.step();check(c.caught(),"Crews pull only after lines arrive");
  c.finish();check(new TowView.Segments(g).count==0 && c.cheer>0,"Lines come off before lift departs");
  g=ReplayInput.montroseJob(Challenges.MONTROSE);b=g.cargo[0];g.mission.wasTowed=g.mission.entered=true;
  b.x=MontroseHarbor.SLIP_X+8;b.y=MontroseHarbor.SLIP_Y+65;b.angle=MontroseHarbor.SLIP_ANGLE+.1f;b.vx=b.vy=0;
  g.tug.x=b.x+180;g.tug.y=b.y+100;g.mission.step(g);check(g.mission.warping && !g.mission.lineCrew.caught(),"Yacht crew cast before hauling");
  for(int i=0;i<1200 && !b.deliveryRecorded;i++)g.step();check(b.deliveryRecorded,"Crew haul large yacht into final slip and complete delivery");
  System.out.println("Dry dockhand anchors, timed throw/catch, depth ropes and yacht crew completion passed");
 }
}
