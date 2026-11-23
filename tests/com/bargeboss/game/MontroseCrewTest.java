package com.bargeboss.game;
/** Position-triggered rescue call and all reply branches use the public dialogue controls. */
public final class MontroseCrewTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void answer(Game g,int n){for(int i=0;i<30;i++)g.step();Layout.Box b=g.dialogue.choiceBox(g,n);ReplayInput.tap(g,b.centerX(),b.centerY());for(int i=0;i<36;i++)g.step();}
 public static void run(){
  for(int choice=0;choice<2;choice++){
   Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.rescue.touring=true;
   g.tug.x=MontroseHaulout.X;g.tug.y=MontroseHaulout.APPROACH_Y;MontroseDockmaster.dispatch(g);
   check(g.dialogue.content().title.contains("KNOW THE AREA"),"Gavin offers beach exploration after haulout");answer(g,0);
   for(int i=0;i<120;i++)g.step();check(!g.dialogue.active && g.rescue.touring,"Idle at lift cannot trigger distress");
   g.tug.x=MontroseHarbor.mapX(2370);g.tug.y=MontroseHarbor.mapY(500);g.step();
   check(g.dialogue.crewConversation() && !g.rescue.touring,"Chas interrupts the trip outside the headland");
   check(g.dialogue.content().title.contains("THREE KIDS") && g.radio.transmitting(g,g.cargo[1]),"Children aboard and source boat radio animation");
   check(SpeakerPortrait.atlas(g,g.dialogue.content().header)==SpriteAtlas.CHAS_PORTRAIT,"Chas uses his own portrait");
   answer(g,choice);check(g.dialogue.content().title.contains("BATTERIES") && g.dialogue.content().first.contains("MUSIC"),"Both batteries drained by music");
   answer(g,choice);check(g.dialogue.content().first.contains("HORN"),"Crew gives lighthouse location");answer(g,0);
   check(!g.dialogue.active && g.rescue.rafted && g.delivered==0,"Replies leave rescue playable without granting completion");
   for(int i=0;i<2;i++){Body boat=g.cargo[i];for(int disc=0;disc<3;disc++)check(MontroseHarbor.contains(boat.discX(disc),boat.discY(disc)),"Rafted hull clear of lighthouse shore");}
  }
  System.out.println("Beach cruise trigger, Chas portrait, crew radio and both dialogue branches passed");
 }
 public static void main(String[] args){run();}
}
