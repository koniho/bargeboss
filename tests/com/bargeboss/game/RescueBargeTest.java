package com.bargeboss.game;
public final class RescueBargeTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static Game scene(){
  Game g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);MontroseRescue r=g.rescue;Body b=r.barge.boat;
  b.x=MontroseHarbor.mapX(1300);b.y=MontroseHarbor.mapY(-1600);b.angle=0;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.awaitingPickup=false;
  g.tug.x=b.x+100;g.tug.y=b.y+120;g.configureCanvas(1333,750);g.layout=Layout.focused(1333,750,b.x,b.y-30,2.8f,600,440);
  for(int i=0;i<20;i++){r.x[i]=b.x+2000+i*60;r.y[i]=b.y-500;}
  r.x[0]=b.x;r.y[0]=b.y-100;r.found[0]=true;
  return g;
 }
 static void fling(Game g,float x,float y){
  Body b=g.rescue.barge.boat;g.abilities.reveal(g,b);if(g.abilities.selected!=b)g.abilities.reveal(g,b);
  float cx=g.abilities.buttonX(g,0),cy=g.abilities.buttonY(g),dx=x-b.x,dy=y-b.y;
  RiverCamera c=g.layout.camera;float sx=c.screenX(dx,dy)-c.screenX(0,0),sy=c.screenY(dx,dy)-c.screenY(0,0),len=Physics.length(sx,sy),drag=Physics.length(dx,dy)/210*60;
  g.pointerDown(8,cx,cy);g.pointerMove(8,cx+sx/len*drag,cy+sy/len*drag);g.pointerUp(8,cx+sx/len*drag,cy+sy/len*drag);
 }
 static void ticks(Game g,int count){for(int i=0;i<count;i++){g.visualTicks++;g.rescue.step(g);}}
 public static void run(){
  Game g=scene();MontroseRescue r=g.rescue;Body b=r.barge.boat;
  check(b.ability==BargeAbilities.RESCUE && g.weather.rain(g)>=3 && g.weather.fog(g)>=2,"Storm mission supplies a rescue barge");
  fling(g,r.x[0]+15,r.y[0]);check(r.barge.throwsMade==0,"Player must tow barge before operating rescue");
  check(g.tow.attach(g,b,g.tug),"Rescue barge supports normal tow graph");r.barge.step(g);g.tow.detach(g,b);
  fling(g,r.x[0]+15,r.y[0]);check(r.barge.throwsMade==1 && r.barge.phase[0]==1,"Public equipment fling launches free-aim ring");
  ticks(g,RescueBarge.FLIGHT+1);check(r.state[0]==1 && g.delivered==0,"Nearby swimmer swims to landed ring; throw earns no rescue");
  float before=r.x[0];ticks(g,30);check(r.x[0]>before,"Swimmer approaches off-center ring");
  boolean floating=false,climbing=false;for(int i=0;i<1600 && r.state[0]!=4;i++){ticks(g,1);floating|=r.state[0]==2;climbing|=r.state[0]==3;}
  check(floating && climbing && r.state[0]==4 && g.delivered==1,"Ring flotation, return, ladder climb and aboard credit occur in sequence");
  ticks(g,150);check(g.delivered==1 && g.score==100,"Aboard reward happens once");
  for(int i=1;i<20;i++){r.x[i]=b.x;r.y[i]=b.y-100;r.found[i]=true;fling(g,r.x[i]+12,r.y[i]);for(int t=0;t<1600 && r.state[i]!=4;t++)ticks(g,1);check(r.state[i]==4,"Every survivor can board");}
  check(g.delivered==20,"all twenty aboard");g.step();check(g.challengeId==Challenges.WATER_TAXI && g.career.bestStars(Challenges.SWIMMER_RESCUE)>0,"Only all twenty aboard completes rescue and starts the next job");
  g=scene();r=g.rescue;r.barge.towed=true;fling(g,b.x+190,b.y+40);ticks(g,RescueBarge.FLIGHT+10);check(r.state[0]==0,"Miss does not home in or rescue distant swimmer");
  g=scene();r=g.rescue;g.tug.x=r.x[0];g.tug.y=r.y[0];g.rescue.step(g);
  check(r.state[0]==5 && !g.goalFailed() && g.lost==1,"Impact starts visible injury instead of instant failure");
  ticks(g,RescueBarge.HURT-1);check(r.state[0]==5,"Hurt swimmer remains during flailing interval");ticks(g,1);
  check(r.state[0]==6 && !r.failed && !g.goalFailed(),"Hurt swimmer vanishes and the rescue carries on");
  check(g.quota()==19,"a lost swimmer leaves the quota");
  ticks(g,RescueBarge.VANISH+5);check(r.barge.timer[0]==RescueBarge.HURT+RescueBarge.VANISH,"vanish effect clock stops");
  g=scene();r=g.rescue;g.tug.x=r.x[0];g.tug.y=r.y[0];g.rescue.step(g);check(SoundEffects.cue(g)==SoundEffects.SWIMMER_YELL,"a struck swimmer yells");
  System.out.println("Rescue barge tow gate, public free fling, misses, swim/ring/return/climb,20 aboard and delayed casualty passed");
 }
 public static void main(String[] args){run();}
}
