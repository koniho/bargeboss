package com.bargeboss.game;
public final class StageWeatherTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 public static void run(){
  Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.selectLighting(1);g.reset();
  check(g.lighting()==LightingProfile.DUSK,"Every Montrose arrival begins at dusk");
  g.montroseArrival.clear();g.dialogue.close(g);g.weather.evening=true;
  LightingProfile stable=g.lighting();float before=0;
  for(int i=0;i<1800;i++){g.weather.step(g);float now=g.weather.night();check(now>=before && now-before<.001f,"Continuous bounded evening ramp");check(g.lighting()==stable,"No lighting snapshot/cache churn");before=now;}
  check(before==1,"Nightfall reaches its endpoint");
  g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);g.rescue.distress(g);g.dialogue.close(g);g.rescue.rafted=false;
  for(int i=0;i<600;i++)g.weather.step(g);
  check(g.weather.fog(g)==1 && g.weather.waves(g)==1 && g.weather.rain(g)==.35f,"Initial storm haze and light rain before tow");
  check(g.tow.attach(g,g.cargo[0],g.tug),"Fixture starts real tow");g.tug.vy=-12;
  before=0;for(int i=0;i<3600;i++){g.weather.step(g);float now=g.weather.returnStorm();check(now>=before && now-before<.001f,"No return-storm jump");before=now;}
  check(g.weather.fog(g)==2 && g.weather.waves(g)==2 && g.weather.rain(g)==3,"Return tow reaches double waves/fog and heavy rain");
  int phase=g.weather.towTicks;g.mode=Game.PAUSE;for(int i=0;i<60;i++)g.advance(1f/60);check(g.weather.towTicks==phase,"Pause freezes weather progression");
  WaveControls.adjust(g,5);check(g.weather.fog(g)==0 && g.weather.waves(g)==1 && g.weather.night()==0,"Developer reset overrides scripted weather");
  System.out.println("Dusk entry, continuous evening and tow-driven double waves/fog, bounded rain and pause tests passed");
 }
 public static void main(String[] args){run();}
}
