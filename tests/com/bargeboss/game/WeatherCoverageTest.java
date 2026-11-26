package com.bargeboss.game;
/** Every stage renders every kind of weather: rain, fog, lightning, evening darkening and storm waves.
 * New stages are covered by adding them to STAGES (survey levels inherit it through SurveyLevelView). */
public final class WeatherCoverageTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    /** -2 practice tutorial, -1 practice yard, then every challenge id (Calumet, lock, Indiana, Montrose, Chicago...). */
    private static Game stage(int id){
        Game root=new Game();root.configureCanvas(1243,600);
        if(id==-2){root.startTutorial();Game g=root.activeGame();g.tutorial.setup(g);g.mode=Game.PLAY;return g;}
        if(id==-1){root.resetPracticeYard();root.mode=Game.PLAY;return root;}
        root.challengeId=id;root.reset();root.mode=Game.PLAY;return root;
    }
    private static String name(int id){return id==-2?"practice tutorial":id==-1?"practice yard":Challenges.title(id);}
    public static void run(){
        for(int id=-2;id<Challenges.COUNT;id++){
            Game g=stage(id);for(int i=0;i<30;i++)g.step();
            int calm=Parity.frame(g);
            check(Parity.frame(g)==calm,"frame hash is stable: "+name(id));
            g.settings.adjustRain(3);check(Parity.frame(g)!=calm,"rain draws in "+name(id));g.settings.adjustRain(-3);
            g.settings.adjustFog(3);check(Parity.frame(g)!=calm,"fog draws in "+name(id));g.settings.adjustFog(-3);
            g.settings.weatherFlashTick=g.visualTicks-4;check(Parity.frame(g)!=calm,"lightning flashes in "+name(id));g.settings.weatherFlashTick=-1000;
            g.weather.evening=true;g.weather.eveningTicks=StageWeather.EVENING_TICKS;check(Parity.frame(g)!=calm,"evening darkens "+name(id));
            g.weather.evening=false;g.weather.eveningTicks=0;
            check(Parity.frame(g)==calm,"weather fully clears in "+name(id));
            float sea=g.water.height(g,g.tug.x,g.tug.y,g.visualTicks,null);
            g.settings.adjustWaves(8);float storm=g.water.height(g,g.tug.x,g.tug.y,g.visualTicks,null);
            check(g.settings.waveIntensity()>1 && (Math.abs(storm)>Math.abs(sea)+1e-4f || Math.abs(sea)<1e-4f && storm!=sea || Parity.frame(g)!=calm),"storm waves raise the sea in "+name(id));
            g.settings.resetWaves();
        }
        // Every level maps where waves build (open water) and where they stay calmer.
        for(SurveyLevel level:new SurveyLevel[]{SurveyLevel.calumet(),SurveyLevel.indiana(),SurveyLevel.chicago()}){
            float[][] zones=level.waveZones();check(zones.length>0,level.key()+" defines wave zones");
            float[] z=zones[0];
            float open=level.waveExposure(z[0]+z[2]*z[5]*1.5f,z[1]+z[3]*z[5]*1.5f),sheltered=level.waveExposure(z[0]-z[2]*z[4]*4.5f,z[1]-z[3]*z[4]*4.5f);
            check(open>.8f && sheltered<.3f,level.key()+" has rough open water and calm sheltered reaches: "+open+" / "+sheltered);
        }
        check(WaveZones.training(300,-2700)>.9f && WaveZones.training(-600,500)<.2f,"training: big basin rough, canal calm");
        check(SeaWaves.exposure(MontroseHarbor.mapX(1350),MontroseHarbor.mapY(-1530))>SeaWaves.exposure(MontroseHarbor.SLIP_X,MontroseHarbor.SLIP_Y),"Montrose: lake rougher than the yacht slips");
        Game yard=stage(-2);check(SeaWaves.sheltered(yard,300,-2700)==1,"default sea is unchanged everywhere");
        yard.settings.adjustWaves(8);
        check(SeaWaves.sheltered(yard,300,-2700)>SeaWaves.sheltered(yard,-600,500)+1,"storms build far bigger waves in open water than in the canal");
        System.out.println(checks+" weather coverage assertions passed across every stage");
    }
    public static void main(String[] a){run();}
}
