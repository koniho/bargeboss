package com.bargeboss.game;
public final class MontroseArrivalTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void run(){
  Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();
  check(g.montroseArrival.entering && !g.dialogue.active,"arrival precedes first transmission");
  float prior=g.tug.y,focus=g.layout.camera.worldY(g.layout.width*.5f,g.layout.height*.52f);
  for(int tick=0;tick<MontroseArrival.PAN_TICKS;tick++){
   if(tick==120){int before=g.montroseArrival.ticks;g.mode=Game.TITLE;g.step();check(g.montroseArrival.ticks==before,"arrival pauses with game");g.mode=Game.PLAY;}
   if(tick==180)g.configureCanvas(1333,600);
   ReplayInput.throttleDown(g);g.step();
   check(g.tug.y<=prior && g.tug.x==MontroseArrival.X,"tug approaches north without sideways teleport");prior=g.tug.y;
   float y=g.layout.camera.worldY(g.layout.width*.5f,g.layout.height*.52f);check(y<=focus+.01f,"camera pans north");focus=y;
   for(int disc=0;disc<3;disc++)for(int a=0;a<8;a++){double angle=a*Math.PI/4;check(MontroseHarbor.openWater(g.tug.discX(disc)+g.tug.radius*(float)Math.cos(angle),g.tug.discY(disc)+g.tug.radius*(float)Math.sin(angle)),"arrival hull remains in open water");}
   check(!g.joystick.active && g.score==0 && g.ticks==0,"arrival does not accept drive input or advance objectives");
  }
  check(g.dialogue.content().title.equals("WELCOME TO MONTROSE HARBOR, CAPTAIN!"),"first radio is welcome");
  check(g.radio.transmitter()==RadioAudio.DOCKMASTER && SpeakerPortrait.atlas(g,g.dialogue.content().header)==SpriteAtlas.GAVIN_PORTRAIT,"Gavin portrait and radio reception");
  check(g.tug.speed()==0 && g.tug.engineLoad==0,"radio hold does not leave stale wake velocity");
  for(int i=0;i<40;i++)g.step();ReplayInput.tap(g,g.dialogue.choiceBox(g,0).centerX(),g.dialogue.choiceBox(g,0).centerY());g.step();
  check(!g.montroseArrival.active() && g.dialogue.content().title.equals("AIN'T BROKE HAS BROKEN DOWN"),"welcome hands off to first job");
  g.montroseArrival.start(g);g.challengeId=0;g.reset();check(!g.montroseArrival.active(),"other levels clear arrival state");
  System.out.println("Montrose northbound arrival, hull clearance, resize/pause/input, welcome and Gavin dispatch checks passed");
 }
 public static void main(String[] args){run();}
}
