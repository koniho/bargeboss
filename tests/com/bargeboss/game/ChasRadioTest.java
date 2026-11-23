package com.bargeboss.game;
public final class ChasRadioTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void ticks(Game g,int n){for(int i=0;i<n;i++){g.radio.step();g.rescue.radioStep(g);}}
 static final class Cues implements Painter {
  public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
  public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
  public void circle(float x,float y,float r,int c){}
  public void rect(float x,float y,float w,float h,int c){}
  public void poly(float[] xy,int c){}
  int slips;
  public void line(float x,float y,float xx,float yy,float w,int c){for(int i=0;i<2;i++){MooredBoats.Berth b=MontroseRescue.berth(i);if(Physics.length(x-b.x,y-b.y)<60)slips++;}}
 }
 public static void run(){
  Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.dialogue.close(g);
  Cues hidden=new Cues();MontroseRescueView.water(g,hidden);check(hidden.slips==0,"No destination highlights before raft release");
  g.tug.x+=500;check(g.rescue.tapRaft(g,g.cargo[0].x,g.cargo[0].y) && g.rescue.rafted,"Remote tap does not release raft");
  ticks(g,1500);check(g.rescue.radio.content()==null && !g.rescue.slipsRevealed(),"No chatter or slip reveal before arrival and release");
  g.tug.x=g.cargo[0].x;g.tug.y=g.cargo[0].y-80;
  MontroseRescueTest.tapWorld(g,g.cargo[0].x+11,g.cargo[0].y);ticks(g,1);
  check(!g.rescue.rafted && g.rescue.slipsRevealed(),"Public raft cut reveals destination slips");
  InstructionPanel.Content c=g.rescue.radio.content();
  check(c.first.contains("RUMRUNNER 2") && c.first.contains("C-38") && c.second.contains("NEVER WETTER") && c.second.contains("C-22"),"Chas assigns both boats to specific slips");
  check(SpeakerPortrait.atlas(g,c.header)==SpriteAtlas.CHAS_PORTRAIT && g.radio.transmitting(g,g.cargo[1]),"Nonmodal Chas portrait and source animation");
  Cues shown=new Cues();MontroseRescueView.water(g,shown);check(shown.slips>0,"Slips become visibly highlighted after release");
  ticks(g,ChasRadio.SHOW_TICKS+1500);check(g.rescue.radio.content()==null,"No casual chatter when untethered");
  check(g.tow.attach(g,g.cargo[0],g.tug),"Real tow starts chatter eligibility");g.tug.vx=20;
  ReplayInput.throttle(g,70,0);ticks(g,ChasRadio.FIRST_TOW_TICKS);
  check(g.rescue.radio.content()!=null && g.rescue.radio.content().title.contains("COMMITTEE"),"First moving-tow absurdist call");
  check(g.joystick.active && !g.conversation(),"Chatter leaves throttle and navigation available");
  ticks(g,ChasRadio.SHOW_TICKS);check(g.rescue.radio.content()==null,"Transmission expires");
  ticks(g,120);check(g.rescue.radio.content().title.contains("COOLER") && SpeakerPortrait.atlas(g,g.rescue.radio.content().header)==SpriteAtlas.ALEX_PORTRAIT && g.radio.transmitting(g,g.cargo[0]),"Calls advance after spaced moving-tow time");
  g.rescue.radio.delivered(1);g.rescue.radio.delivered(0);ticks(g,60);
  check(g.rescue.radio.content().title.contains("RUMRUNNER 2") && g.rescue.radio.content().first.contains("ENERGY DRINKS"),"First delivery thank-you takes priority");
  ticks(g,ChasRadio.SHOW_TICKS);
  check(g.rescue.radio.content().title.contains("NEVER WETTER") && g.rescue.radio.content().first.contains("FIREBALL"),"Second delivery has distinct grateful offer");
  check(g.radio.transmitting(g,g.cargo[1]),"Second boat owns its transmission animation");
  ticks(g,ChasRadio.SHOW_TICKS);check(!g.rescue.radio.pendingThanks(),"Both delivery acknowledgements drain");
  g.rescue.radio.delivered(0);g.rescue.radio.delivered(1);check(!g.rescue.radio.pendingThanks(),"Delivery thanks are deduplicated");
  g.reset();check(g.rescue.radio.content()==null && !g.rescue.radio.pendingThanks(),"Restart clears the radio queue");
  g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.dialogue.close(g);g.rescue.rafted=false;
  g.tug.x=300;g.tug.y=-400;
  for(int i=0;i<2;i++){Body boat=g.cargo[i];MooredBoats.Berth berth=MontroseRescue.berth(i);
   boat.x=boat.tiedX=berth.x;boat.y=boat.tiedY=berth.y;boat.angle=boat.tiedAngle=berth.angle;
   boat.awaitingPickup=true;g.rescue.towed[i]=true;g.rescue.settled[i]=59;}
  g.step();check(g.delivered==2 && g.challengeId==Challenges.RAFT_RESCUE,"Simultaneous delivery waits for both thank-yous");
  for(int i=0;i<2*ChasRadio.SHOW_TICKS+5;i++)g.step();
  check(g.challengeId==Challenges.MONTROSE && g.dialogue.dockmasterConversation(),"Gavin dispatches next job only after both thanks");
  System.out.println("Chas nonblocking tow chatter, slip reveal/assignment, per-boat thanks, source and reset passed");
 }
 public static void main(String[] args){run();}
}
