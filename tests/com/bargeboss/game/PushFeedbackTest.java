package com.bargeboss.game;
final class PushFeedbackTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void run(){
  Game g=new Game();ReplayInput.controlFixture(g);Body b=g.cargo[0];
  b.x=300;b.y=600;b.angle=0;g.pushX=1;g.pushY=0;g.control=Game.PUSHING;
  for(int i=1;i<g.cargo.length;i++)if(g.cargo[i]!=null)g.cargo[i].active=false;
  check(!PushFeedback.blocked(g,b),"Open water push stays quiet");
  g.cargo[1]=new Body(2,1,b.x+75,b.y,0);
  check(PushFeedback.blocked(g,b),"Load ahead blocks requested push");
  g.pushX=-1;check(!PushFeedback.blocked(g,b),"Pushing away from obstacle is allowed");
  g.control=Game.WAITING_BRIDGE;check(PushFeedback.blocked(g,b),"Closed bridge wait is marked");
  g.control=Game.DOCKING;check(!PushFeedback.blocked(g,b),"Normal docking is not a failure");
  g.control=Game.ALIGNING;check(!PushFeedback.blocked(g,b),"Normal tug approach is not a failure");
  g.cargo[1].active=false;g.pushX=1;
  // Walk the fixture hull toward the east bank and require a warning before its center reaches land.
  boolean warned=false;for(int x=330;x<700 && TrainingChannel.contains(x,600);x+=4){b.x=x;if(PushFeedback.blocked(g,b)){warned=true;break;}}
  check(warned,"Shore blocks a forward hull edge before center goes ashore");
  System.out.println("Push clearance: open water, other hull, retreat, bridge, docking and shore passed");
 }
}
