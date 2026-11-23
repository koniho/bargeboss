package com.bargeboss.game;
/** Shared-renderer checkpoints for the continuous carrier/yard/return cycle. */
public final class TravelLiftCycleReview {
 public static void main(String[] args)throws Exception{
  Game g=TravelLiftTest.scene();g.configureCanvas(1333,750);
  g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHaulout.Y,3,666,430);
  Body boat=g.cargo[0];boat.x=MontroseHaulout.X;boat.y=MontroseHaulout.Y+25;g.haulout.lineCrew.start(boat);
  for(int i=0;i<40;i++)g.haulout.lineCrew.step();DialogueReview.capture(g,"lift-crew-casting");
  for(int i=40;i<85;i++)g.haulout.lineCrew.step();DialogueReview.capture(g,"lift-crew-hauling");
  g=TravelLiftCycleTest.scene();g.configureCanvas(1333,750);
  g.layout=Layout.focused(1333,750,MontroseHaulout.X,MontroseHaulout.Y-50,2.4f,666,430);
  DialogueReview.capture(g,"lift-cycle-hoisted");
  for(int i=0;i<180;i++)g.haulout.step(g);DialogueReview.capture(g,"lift-cycle-carrying");
  for(int i=180;i<MontroseHaulout.TRAVEL_TICKS+MontroseHaulout.YARD_TICKS;i++)g.haulout.step(g);DialogueReview.capture(g,"lift-cycle-yard");
  for(int i=0;i<180;i++)g.haulout.step(g);DialogueReview.capture(g,"lift-cycle-returning");
  for(int i=180;i<MontroseHaulout.RETURN_TICKS;i++)g.haulout.step(g);DialogueReview.capture(g,"lift-cycle-ready");
 }
}
