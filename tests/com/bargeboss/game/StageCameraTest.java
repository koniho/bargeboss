package com.bargeboss.game;
/** Geographic stage camera, gesture stability and lifecycle. */
public final class StageCameraTest {
 private static int checks;
 private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 private static void steps(Game g,int n){for(int i=0;i<n;i++)g.step();}
 public static void run(){
  for(int id:new int[]{0,Challenges.MONTROSE,Challenges.INDIANA}){
   Game g=new Game();g.challengeId=id;g.reset();g.dialogue.close(g);
   check(g.layout.camera.zoom>=Game.TRANSIT_ZOOM,"lake approach uses readable transit scale");
   g.layout=Layout.focused(1333,600,g.tug.x,g.tug.y,2.4f,666,312);
   float x=Joystick.centerX(g),y=Joystick.centerY(g);g.pointerDown(9,x,y);g.pointerMove(9,x+30,y-18);
   float zoom=g.layout.camera.zoom;steps(g,90);
   check(g.layout.camera.zoom>=zoom-.001f,"steering holds camera zoom");g.pointerUp(9,x+30,y-18);
   steps(g,30);check(g.layout.camera.zoom>=zoom-.001f,"release holds view briefly");
   int hold=g.cameraZoomHoldTicks,tick=g.ticks;RiverCamera paused=g.layout.camera;
   g.background();steps(g,60);check(g.ticks==tick && g.layout.camera==paused && g.cameraZoomHoldTicks==hold,"pause freezes view and hold timer");g.pause();steps(g,300);
   RiverCamera c=g.layout.camera;
   for(float dx:new float[]{-150,0,150})for(float dy:new float[]{-150,0,150}){
    float wx=g.tug.x+dx,wy=g.tug.y+dy,sx=c.screenX(wx,wy),sy=c.screenY(wx,wy);
    check(Math.abs(c.worldX(sx,sy)-wx)<.01f && Math.abs(c.worldY(sx,sy)-wy)<.01f,"large geographic coordinates preserve pointer inverse");
   }
   int frame=Parity.frame(g);check(frame==Parity.frame(g) && g.layout.camera==c,"draw is camera-pure");
   g.reset();g.dialogue.close(g);check(g.cameraZoomHoldTicks==0 && g.ticks==0,"location restart clears camera hold");
  }
  // Cast off beside the travel lift: the old generic quay bollard is far away.
  Game marina=ReplayInput.montroseJob(Challenges.HAULOUT);Body boat=marina.cargo[0];
  marina.dialogue.close(marina);
  boat.awaitingPickup=false;boat.x=MontroseHaulout.X;boat.y=MontroseHaulout.APPROACH_Y+25;
  marina.tug.x=boat.x;marina.tug.y=boat.y+100;marina.tug.angle=boat.angle;
  marina.layout=Layout.focused(1333,600,boat.x,boat.y+50,2.4f,666,312);
  marina.tow.attach(marina,boat);ReplayInput.releaseTow(marina,boat);
  check(marina.tow.incoming(boat)==null,"public towline tap releases disabled cruiser");
  steps(marina,360);
  check(marina.layout.camera.zoom>=2.4f,"castoff near lift must not frame nonexistent distant bollard");
  check(marina.layout.camera.screenX(marina.tug.x,marina.tug.y)>120 && marina.layout.camera.screenX(marina.tug.x,marina.tug.y)<1213,"released tow camera remains centered on local work");
  System.out.println(checks+" geographic camera, driving hold, pause and inverse assertions passed");
 }
 public static void main(String[] args){run();}
}
