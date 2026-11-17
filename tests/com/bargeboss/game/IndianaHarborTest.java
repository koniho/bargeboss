package com.bargeboss.game;
public final class IndianaHarborTest {
 private static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 public static void run(){Game g=new Game();g.challengeId=Challenges.INDIANA;g.reset();
  check(g.indianaStage() && !g.montroseStage() && !g.hasBridge(),"independent harbor uses its own geography and raised rail scenery");
  check(g.feeders.length==0 && g.cargo[0].berth==0,"steel tow starts in outer harbor without Calumet feeder traffic");
  for(float[] p:new float[][]{{600,-1000},{600,-500},{375,450},{-300,950},{375,1500}})check(g.inWater(p[0],p[1]),"lake, basin, canal and both forks remain connected water");
  check(!g.inWater(590,420) && !g.inWater(835,-900),"Hick Tower and breakwater light stand on solid land");
  for(float[] p:new float[][]{{600,-680},{375,-180},{375,400},{375,950},{325,1500}}){Body b=new Body(9,0,p[0],p[1],(float)Math.PI/2);float x=b.x,y=b.y;IndianaHarbor.collide(b,null);check(b.x==x && b.y==y,"full loaded hull fits route");}
  check(!IndianaHarbor.clear(600,0,600,400),"towlines cannot cross mill quay");
  Body b=g.cargo[0];b.x=IndianaHarbor.DOCK_X;b.y=IndianaHarbor.DOCK_Y;b.angle=(float)Math.PI/2;
  check(g.canTie(b),"south mill accepts actual lasso");Mooring.attach(b);
  for(int i=0;i<Game.UNLOAD_TICKS+Mooring.ALIGN_TICKS+20;i++)g.step();
  check(g.delivered==1 && b.deliveryRecorded && g.inWater(b.x,b.y),"south mill completes delivery without winching through the bank");
  System.out.println("Indiana Harbor geography, landmark anchors, route fit and south-mill delivery pass");
 }
 public static void main(String[] args){run();}
}
