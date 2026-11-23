package com.bargeboss.game;
public final class RacingBoatTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void run(){
  Game g=ReplayInput.montroseJob(Challenges.HAULOUT);Body b=g.cargo[0];MooredBoats.Berth berth=MontroseHaulout.pickupBerth();
  check(b.racing && b.half==18 && b.radius==7 && berth.star==2 && berth.arm==3,"Large racer uses farthest star dock S");
  float far=Physics.length(MontroseHarbor.STAR_CENTERS[berth.star][0]-MontroseHaulout.X,MontroseHarbor.STAR_CENTERS[berth.star][1]-MontroseHaulout.Y);
  for(float[] star:MontroseHarbor.STAR_CENTERS)check(Physics.length(star[0]-MontroseHaulout.X,star[1]-MontroseHaulout.Y)<=far,"Farthest star selected geometrically");
  for(int d=0;d<3;d++)for(int i=0;i<32;i++){double a=i*Math.PI/16;check(MontroseHarbor.contains(b.discX(d)+b.radius*(float)Math.cos(a),b.discY(d)+b.radius*(float)Math.sin(a)),"Racing hull clears star fingers");}
  for(MooredFleet.FloatBoat npc:g.mooredFleet.boats)check(npc.berth!=berth,"Occupied mission slip has no duplicate NPC");
  g.tug.x=b.x+80*(float)Math.cos(b.angle);g.tug.y=b.y+80*(float)Math.sin(b.angle);g.tug.angle=b.angle;
  g.layout=Layout.focused(g.layout.width,g.layout.height,(g.tug.x+b.x)*.5f,(g.tug.y+b.y)*.5f,2.4f,g.layout.width*.5f,g.layout.height*.5f);
  ReplayInput.down(g,b.x,b.y);ReplayInput.move(g,g.tug.x,g.tug.y);ReplayInput.up(g,g.tug.x,g.tug.y);for(int i=0;i<45;i++)g.step();
  check(g.tow.connected(b,g.tug),"Public drag/release attaches racer at star dock");
  b.x=MontroseHaulout.X;b.y=MontroseHaulout.Y;b.angle=MontroseHaulout.ANGLE;check(MontroseHaulout.fits(b),"Large racer fits reduced lift");
  check(RacingBoatArt.LIGHTS.length==4 && RacingBoatArt.SMOKESTACKS.length==0,"Explicit racing light and empty stack inventory");
  System.out.println("Racing hull, farthest star dock, physical clearance, public pickup and lift fit passed");
 }
 public static void main(String[] args){run();}
}
