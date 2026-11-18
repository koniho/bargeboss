package com.bargeboss.game;
/** Geographic stage entry and model contract; route/mooring exercised by SurveyedLocationTest. */
public final class IndianaHarborTest {
 private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void run(){Game g=new Game();g.challengeId=Challenges.INDIANA;g.reset();
  check(g.indianaStage() && !g.montroseStage() && !g.hasBridge(),"Indiana uses geographic rigid bridge scenery");
  check(g.feeders.length==0 && g.cargo[0].berth==0,"steel tow starts in lake approach");
  check(IndianaHarbor.COAST.length>600 && IndianaHarbor.ROUTE.length>15,"survey retains shoreline and connected canal branches");
  check(g.inWater(g.tug.x,g.tug.y) && g.inWater(g.cargo[0].x,g.cargo[0].y),"both starting hulls lie in water");
  check(!IndianaHarbor.contains(IndianaHarbor.north(41.664),IndianaHarbor.east(-87.459)),"steelworks peninsula remains solid land");
  check(IndianaSurvey.BRIDGES.length>=3,"mapped canal crossings represented");
  System.out.println("Indiana Harbor survey and stage entry pass");
 }
 public static void main(String[] args){run();}
}
