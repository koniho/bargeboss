package com.bargeboss.game;
/** Shared weather for every stage (rain, fog, lightning, evening, storm waves), advanced at 60 Hz without
 * replacing native lighting/cache snapshots. Each stage's draw pipeline calls the WeatherView layers, so a
 * new level gets weather by using SurveyLevelView (or calling WeatherView.scenery / Renderer as they do).
 * Mission scripts (e.g. Montrose's rescue storm) only set the evening/storm flags. */
final class StageWeather {
 static final int EVENING_TICKS=1800,STORM_TICKS=600,TOW_TICKS=3600;
 boolean evening,storm,manualLighting,manualWeather;
 int eveningTicks,stormTicks,towTicks;
 void reset(){evening=storm=manualLighting=manualWeather=false;eveningTicks=stormTicks=towTicks=0;}
 void step(Game g){
  if(evening && !manualLighting && eveningTicks<EVENING_TICKS)eveningTicks++;
  if(storm && !manualWeather && stormTicks<STORM_TICKS)stormTicks++;
  if(storm && !manualWeather && g.montroseStage() && MontroseRescue.cruisers(g) && !g.conversation() && g.tug.speed()>1){
   boolean towing=false;for(int i=0;i<2;i++)if(g.cargo[i]!=null && !g.cargo[i].deliveryRecorded && g.tow.connected(g.cargo[i],g.tug))towing=true;
   if(towing && towTicks<TOW_TICKS)towTicks++;
  }
 }
 float night(){return manualLighting?0:SeaWaves.smooth(0,EVENING_TICKS,eveningTicks);}
 float returnStorm(){return manualWeather?0:SeaWaves.smooth(0,TOW_TICKS,towTicks);}
 float onset(){return manualWeather?0:SeaWaves.smooth(0,STORM_TICKS,stormTicks);}
 float rain(Game g){return Math.max(g.settings.rainLevel(),onset()*(.35f+2.65f*returnStorm()));}
 float fog(Game g){return Math.max(g.settings.fogLevel(),onset()*(1+returnStorm()));}
 float waves(Game g){return g.settings.waveIntensity()*(1+returnStorm());}
 /** Only background/terrain is darkened. Vessel pixels, lamps and UI draw afterward.
  * Keeps the existing dusk bitmap cache stable throughout the continuous transition. */
 void background(Game g,Painter p){int alpha=Math.round(night()*92);if(alpha>0)p.rect(0,0,g.layout.width,g.layout.height,(alpha<<24)|0x020915);}
}
