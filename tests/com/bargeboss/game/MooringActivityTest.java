package com.bargeboss.game;
public final class MooringActivityTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void run(){Game g=ReplayInput.montroseJob(Challenges.HAULOUT);int sails=0;MooredFleet.FloatBoat target=null;
  for(MooredFleet.FloatBoat b:g.mooredFleet.boats)if(b.berth.mooring>=0){sails++;target=b;check(b.berth.kind>=3 && b.clear(),"mooring sailboat is clear of shore");check(g.buoys.get(g,b.berth.mooring+2)!=null,"sailboat has a physical mooring ball");}
  check(sails==3,"three sailboats attached to mooring balls");
  Buoys.Buoy ball=g.buoys.get(g,target.berth.mooring+2);g.tug.x=ball.x;g.tug.y=ball.y;g.tug.angle=.3f;g.tug.vx=40;g.tug.vy=10;g.tug.spin=.2f;
  float x=g.tug.x,y=g.tug.y,vx=g.tug.vx,vy=g.tug.vy,spin=g.tug.spin;
  Buoys.contact(g,ball,g.tug,false);
  check(g.tug.x==x && g.tug.y==y && g.tug.vx==vx && g.tug.vy==vy && g.tug.spin==spin,"mooring ball never alters tug position velocity or turn");
  check(Physics.length(ball.x-ball.anchorX,ball.y-ball.anchorY)>1 && ball.contactBob>0,"ball gives way and bobs on contact");
  g.tug.x+=1000;g.tug.vx=g.tug.vy=0;for(int i=0;i<600;i++){g.visualTicks++;g.buoys.step(g);}
  check(Physics.length(ball.x-ball.anchorX,ball.y-ball.anchorY)<1,"ball settles back at its anchor");
  g=ReplayInput.montroseJob(Challenges.HAULOUT);for(MooredFleet.FloatBoat b:g.mooredFleet.boats)if(b.berth.mooring==9)target=b;
  g.tug.x=target.x+60;g.tug.y=target.y;g.tug.angle=0;g.tug.vx=60;g.tug.wakeTrailTicks=7;g.water.step(g);g.tug.x+=500;g.tug.vx=0;
  float peak=0,moved=0;for(int i=0;i<240;i++){g.visualTicks++;g.water.step(g);g.mooredFleet.step(g);peak=Math.max(peak,Math.abs(target.wakeHeave));moved=Math.max(moved,Physics.length(target.x-target.berth.x,target.y-target.berth.y));}
  check(peak>.1f && moved>.1f,"passing wake visibly bobs and displaces moored sailboat");
  check(target.clear() && target.bowDistance()<=20.001f,"stronger wake stays tethered and clear");
  target.spin=.65f;for(int i=0;i<90;i++)g.mooredFleet.step(g);
  check(Math.abs(target.yaw)>.1f && target.bowDistance()<=20.001f && target.clear(),"single bow mooring allows broad rotation while staying tethered");
  System.out.println("Three mooring sailboats, one-way collision, anchor recovery and amplified wake response passed");
 }
 public static void main(String[] args){run();}
}
