package com.bargeboss.game;

/** Session-only wave and weather experiment, drawn and routed identically by every host. */
public final class WaveControls {
    private static final String[] RAIN={"OFF","LIGHT","STEADY","HEAVY"},LIGHTNING={"OFF","LOW","HIGH"},FOG={"OFF","LIGHT","MEDIUM","DENSE"};
    private boolean open;
    private int pointer=-1,pressed=-1;
    boolean visible(Game g){return g.settings.developerWaves && g.montroseStage() && g.mode==Game.PLAY;}
    private int hit(float x,float y){
        if(x<12 || x>282 || y<76 || y>(open?354:112))return -1;
        if(y<112)return 0;
        if(y>=118 && y<=150)return x<65?1:x>229?2:6;
        if(y>=156 && y<=188)return x<65?3:x>229?4:6;
        if(y>=194 && y<=226)return x<65?7:x>229?8:6;
        if(y>=232 && y<=264)return x<65?9:x>229?10:6;
        if(y>=270 && y<=302)return x<65?12:x>229?13:6;
        return y>=310?(x<147?11:5):6;
    }
    boolean down(Game g,int id,float x,float y){if(!visible(g)||hit(x,y)<0)return false;if(pointer<0){pointer=id;pressed=hit(x,y);}return true;}
    boolean owns(int id){return pointer==id;}
    boolean up(Game g,int id,float x,float y){if(!owns(id))return false;
        int action=hit(x,y);if(action==pressed && visible(g)){
            if(action==0)open=!open;
            if(action==1 || action==2)g.settings.adjustWaves(action==1?-1:1);
            if(action==3 || action==4)g.settings.turnWaves(action==3?-1:1);
            if(action==5){g.settings.resetWaves();g.settings.resetWeather();}
            if(action==7 || action==8)g.settings.adjustRain(action==7?-1:1);
            if(action==9 || action==10)g.settings.adjustLightning(action==9?-1:1);
            if(action==12 || action==13)g.settings.adjustFog(action==12?-1:1);
            if(action==11)g.settings.weatherFlashTick=g.visualTicks;
        }cancel();return true;
    }
    void cancel(){pointer=pressed=-1;}
    void draw(Game g,Painter p){if(!visible(g))return;
        p.rect(12,76,270,open?278:36,0xee101e29);
        Lettering.draw(p,open?"WEATHER LAB / CLOSE":"WAVES + WEATHER",24,100,13,0xffc1d9dc);
        if(!open)return;
        Lettering.draw(p,"-",30,142,20,0xffc1d9dc);Lettering.draw(p,"+",245,142,20,0xffc1d9dc);
        Lettering.draw(p,"WAVES "+g.settings.waveIntensity()+"X",78,139,12,0xffc1d9dc);
        Lettering.draw(p,"-",30,180,20,0xffc1d9dc);Lettering.draw(p,"+",245,180,20,0xffc1d9dc);
        Lettering.draw(p,"TOWARD "+g.settings.waveBearing()+" DEG",70,177,11,0xffc1d9dc);
        Lettering.draw(p,"-",30,218,20,0xffc1d9dc);Lettering.draw(p,"+",245,218,20,0xffc1d9dc);
        Lettering.draw(p,"RAIN "+RAIN[g.settings.rainLevel()],78,215,11,0xffc1d9dc);
        Lettering.draw(p,"-",30,256,20,0xffc1d9dc);Lettering.draw(p,"+",245,256,20,0xffc1d9dc);
        Lettering.draw(p,"LIGHTNING "+LIGHTNING[g.settings.lightningLevel()],66,253,10,0xffc1d9dc);
        Lettering.draw(p,"-",30,294,20,0xffc1d9dc);Lettering.draw(p,"+",245,294,20,0xffc1d9dc);
        Lettering.draw(p,"FOG "+FOG[g.settings.fogLevel()],78,291,11,0xffc1d9dc);
        Lettering.draw(p,"TEST FLASH",24,335,10,0xff92aeb9);
        Lettering.draw(p,"RESET ALL",166,335,10,0xff92aeb9);
    }
}
