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
        if(y>=118 && y<=146)return x<65?1:x>229?2:6;
        if(y>=150 && y<=178)return x<65?3:x>229?4:6;
        if(y>=182 && y<=210)return x<65?7:x>229?8:6;
        if(y>=214 && y<=242)return x<65?9:x>229?10:6;
        if(y>=246 && y<=274)return x<65?12:x>229?13:6;
        if(y>=278 && y<=306)return x<65?14:x>229?15:6;
        return y>=310?(x<147?11:5):6;
    }
    boolean down(Game g,int id,float x,float y){if(!visible(g)||hit(x,y)<0)return false;if(pointer<0){pointer=id;pressed=hit(x,y);}return true;}
    boolean owns(int id){return pointer==id;}
    boolean up(Game g,int id,float x,float y){if(!owns(id))return false;
        int action=hit(x,y);if(action==pressed && visible(g)){
            if(action==0)open=!open;
            if(action==1 || action==2)g.settings.adjustWaves(action==1?-1:1);
            if(action==3 || action==4)g.settings.turnWaves(action==3?-1:1);
            if(action==5){g.settings.resetWaves();g.settings.resetWeather();g.changeLighting(LightingProfile.DUSK.id);}
            if(action==7 || action==8)g.settings.adjustRain(action==7?-1:1);
            if(action==9 || action==10)g.settings.adjustLightning(action==9?-1:1);
            if(action==12 || action==13)g.settings.adjustFog(action==12?-1:1);
            if(action==14 || action==15)g.changeLighting(Math.floorMod(g.lighting().id+(action==14?-1:1),4));
            if(action==11)g.settings.weatherFlashTick=g.visualTicks;
        }cancel();return true;
    }
    void cancel(){pointer=pressed=-1;}
    void draw(Game g,Painter p){if(!visible(g))return;
        p.rect(12,76,270,open?278:36,0xee101e29);
        Lettering.draw(p,open?"WEATHER + TIME / CLOSE":"WAVES / WEATHER / TIME",24,100,13,0xffc1d9dc);
        if(!open)return;
        row(p,118,"WAVES "+g.settings.waveIntensity()+"X",78,12);
        row(p,150,"TOWARD "+g.settings.waveBearing()+" DEG",70,11);
        row(p,182,"RAIN "+RAIN[g.settings.rainLevel()],78,11);
        row(p,214,"LIGHTNING "+LIGHTNING[g.settings.lightningLevel()],66,10);
        row(p,246,"FOG "+FOG[g.settings.fogLevel()],78,11);
        row(p,278,"TIME "+g.lighting().name,78,11);
        Lettering.draw(p,"TEST FLASH",24,335,10,0xff92aeb9);
        Lettering.draw(p,"RESET ALL",166,335,10,0xff92aeb9);
    }
    private static void row(Painter p,float y,String label,float x,float size){
        Lettering.draw(p,"-",30,y+6,20,0xffc1d9dc);Lettering.draw(p,"+",245,y+6,20,0xffc1d9dc);
        Lettering.draw(p,label,x,y+8,size,0xffc1d9dc);
    }
}
