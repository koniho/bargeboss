package com.bargeboss.game;
final class HauloutCaptureTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void run(){
  Game g=TravelLiftTest.scene();Body b=g.cargo[0];
  g.haulout.requested=true;g.haulout.liftTicks=MontroseHaulout.LOWER_TICKS;g.haulout.rig.update(1);g.haulout.towed=true;
  g.tug.x=MontroseHaulout.X+100;g.tug.y=MontroseHaulout.Y+150;
  b.x=MontroseHaulout.X+5;b.y=MontroseHaulout.Y+42;b.angle=MontroseHaulout.ANGLE+.22f;b.vy=-12;
  check(!MontroseHaulout.fits(b) && MontroseHaulout.captureArea(b),"Crew capture accepts approach before exact hoist fit");
  b.y=MontroseHaulout.Y+65;check(!MontroseHaulout.captureArea(b),"Boat must pass the mouth");b.y=MontroseHaulout.Y+42;
  b.angle=0;check(!MontroseHaulout.captureArea(b),"Sideways boat cannot fit");b.angle=MontroseHaulout.ANGLE+.22f;
  b.x=MontroseHaulout.X+30;check(!MontroseHaulout.captureArea(b),"Hull across quay cannot trigger");b.x=MontroseHaulout.X+5;
  g.haulout.liftTicks=100;g.haulout.step(g);check(!g.haulout.warping,"Raised straps cannot capture");g.haulout.liftTicks=MontroseHaulout.LOWER_TICKS;
  b.vy=-40;g.haulout.step(g);check(!g.haulout.warping,"Fast collision is not crew clearance");b.vy=-12;
  g.tow.attach(g,b,g.tug);g.haulout.step(g);check(!g.haulout.warping,"Release tow before handoff");g.tow.detach(g,b);
  g.haulout.step(g);check(g.haulout.warping && !g.haulout.pushed,"Released moderate-speed arrival does not need a push flag");
  for(int i=0;i<1800 && !b.deliveryRecorded;i++)g.step();
  check(b.deliveryRecorded && g.challengeId==Challenges.RAFT_RESCUE,"Crew aligns, lifts and completes from relaxed entry");
  System.out.println("Haulout generous capture, realignment, speed/strap/tow safeguards and real completion passed");
 }
}
