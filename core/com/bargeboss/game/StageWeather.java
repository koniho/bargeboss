package com.bargeboss.game;
/** Shared weather for every stage (rain, fog, lightning, evening, storm waves), advanced at 60 Hz without
 * replacing native lighting/cache snapshots. Each stage's draw pipeline calls the WeatherView layers, so a
 * new level gets weather by using SurveyLevelView (or calling WeatherView.scenery / Renderer as they do).
 * Mission scripts (e.g. Montrose's rescue storm) only set the evening/storm flags. */
final class StageWeather {
 static final int EVENING_TICKS=1800,STORM_TICKS=600,TOW_TICKS=3600;
 boolean evening,storm,manualLighting,manualWeather;
 int eveningTicks,stormTicks,towTicks;
 /** Blizzard strength (0..1, snow, whiteout, gusts) and deep-freeze progress (0..1, ice and slush), set by the
  * mission that brings the blizzard; both read 0 everywhere else. */
 float blizzard,freeze;
 void reset(){evening=storm=manualLighting=manualWeather=false;eveningTicks=stormTicks=towTicks=0;blizzard=freeze=0;}
 /** Gust strength 0..1 on top of the blizzard: slow swells with sharp peaks. */
 float gust(int tick){if(blizzard<=0)return 0;float a=.5f+.5f*(float)StrictMath.sin(tick*.013f),b=.5f+.5f*(float)StrictMath.sin(tick*.041f+1.3f);return blizzard*(.35f+.65f*a*a*(.6f+.4f*b));}
 /** Wind direction (radians): a slowly veering north-westerly. */
 float windAngle(int tick){return 2.4f+.5f*(float)StrictMath.sin(tick*.0021f);}
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
 float fog(Game g){return Math.max(Math.max(g.settings.fogLevel(),onset()*(1+returnStorm())),blizzard*1.6f);}
 float waves(Game g){return g.settings.waveIntensity()*(1+returnStorm())*(1+blizzard*.8f);}
 /** Only background/terrain is darkened. Vessel pixels, lamps and UI draw afterward.
  * Keeps the existing dusk bitmap cache stable throughout the continuous transition. */
 void background(Game g,Painter p){int alpha=Math.round(night()*92);if(alpha>0)p.rect(0,0,g.layout.width,g.layout.height,(alpha<<24)|0x020915);}
}
