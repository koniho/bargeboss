package com.bargeboss.game;
public final class TravelLiftCycleTest {
 private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static Game scene(){
  Game g=TravelLiftTest.scene();g.haulout.requested=true;g.haulout.liftTicks=MontroseHaulout.LOWER_TICKS;g.haulout.rig.update(1);
  Body b=g.cargo[0];b.x=b.tiedX=MontroseHaulout.X;b.y=b.tiedY=MontroseHaulout.Y;b.angle=b.tiedAngle=MontroseHaulout.ANGLE;b.vx=b.vy=b.spin=0;
  g.haulout.towed=g.haulout.pushed=g.haulout.warping=true;g.haulout.settled=59;g.haulout.lineCrew.start(b);g.haulout.lineCrew.age=DockLineCrew.CATCH;g.haulout.step(g);
  for(int i=0;i<MontroseHaulout.HOIST_TICKS;i++)g.haulout.step(g);
  return g;
 }
 public static void run(){
  Game g=scene();Body b=g.cargo[0];MontroseTravelLift.Rig rig=g.haulout.rig;
  check(b.deliveryRecorded && g.haulout.hoistTicks==MontroseHaulout.HOIST_TICKS,"Hoist finishes before driving");
  ArticulatedModel.Pose frame=rig.farPose,slings=rig.pose;int topology=rig.motion.fragmentCount();
  float prior=0;
  for(int i=0;i<MontroseHaulout.TRAVEL_TICKS;i++){
   g.haulout.step(g);check(rig.offsetY<prior,"Every outbound tick advances continuously");prior=rig.offsetY;
   check(Math.abs(b.y-(MontroseHaulout.Y+rig.offsetY))<.001f,"Boat follows sling translation");
   float[] a=rig.anchor(26,24,4);check(Math.abs(a[1]-(MontroseHaulout.Y+24*MontroseTravelLift.SCALE+rig.offsetY))<.001f,"Cables share carrier translation");
   check(rig.farPose==frame && rig.pose==slings && topology==rig.motion.fragmentCount(),"No pose table or per-tick topology rebuild");
  }
  for(int i=0;i<MontroseHaulout.YARD_TICKS;i++)g.haulout.step(g);
  check(g.haulout.returning && b.active && g.haulout.inLift(b),"Yard retains delivered boat while empty lift returns");
  float parked=b.y;
  for(int i=0;i<MontroseHaulout.RETURN_TICKS;i++){g.haulout.step(g);check(rig.offsetY>prior,"Every return tick advances continuously");prior=rig.offsetY;check(b.y==parked,"Boat stays on land during return");}
  check(g.haulout.readyAgain && rig.offsetY==0 && rig.amount==0 && !g.haulout.requested && !g.haulout.lowered(),"Lift back at well, empty and raised, ready for another call");
  for(int i=0;i<120;i++)g.haulout.step(g);check(g.score==1200 && b.y==parked,"Idle is stable and reward is not repeated");
  g=scene();g.step();check(g.challengeId==Challenges.RAFT_RESCUE,"Normal mission handoff still occurs");
  prior=g.haulout.travelY();g.step();check(g.haulout.travelY()<prior,"Carrier continues during next job");
  prior=g.haulout.travelY();g.background();for(int i=0;i<90;i++)g.step();check(g.haulout.travelY()==prior,"Background pauses carrier");
  System.out.println("Lift carry-away, retained yard boat, continuous return, reuse and mission/lifecycle checks passed");
 }
 public static void main(String[] args){run();}
}
