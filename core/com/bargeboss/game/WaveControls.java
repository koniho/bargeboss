package com.bargeboss.game;

/** Session-only wave experiment, drawn and routed identically by every host. */
public final class WaveControls {
    private boolean open;
    private int pointer=-1,pressed=-1;
    boolean visible(Game g){return g.settings.developerWaves && g.montroseStage() && g.mode==Game.PLAY;}
    private int hit(float x,float y){
        if(x<12 || x>282 || y<76 || y>(open?258:112))return -1;
        if(y<112)return 0;
        if(y>=122 && y<=158)return x<65?1:x>229?2:6;
        if(y>=168 && y<=204)return x<65?3:x>229?4:6;
        return y>=216?5:6;
    }
    boolean down(Game g,int id,float x,float y){if(!visible(g)||hit(x,y)<0)return false;if(pointer<0){pointer=id;pressed=hit(x,y);}return true;}
    boolean owns(int id){return pointer==id;}
    boolean up(Game g,int id,float x,float y){if(!owns(id))return false;
        int action=hit(x,y);if(action==pressed && visible(g)){
            if(action==0)open=!open;
            if(action==1 || action==2)g.settings.adjustWaves(action==1?-1:1);
            if(action==3 || action==4)g.settings.turnWaves(action==3?-1:1);
            if(action==5)g.settings.resetWaves();
        }cancel();return true;
    }
    void cancel(){pointer=pressed=-1;}
    void draw(Game g,Painter p){if(!visible(g))return;
        p.rect(12,76,270,open?182:36,0xee101e29);
        Lettering.draw(p,open?"WAVE LAB  /  CLOSE":"WAVES",24,100,13,0xffc1d9dc);
        if(!open)return;
        Lettering.draw(p,"-",30,148,20,0xffc1d9dc);Lettering.draw(p,"+",245,148,20,0xffc1d9dc);
        Lettering.draw(p,"POWER "+g.settings.waveIntensity()+"X",78,145,12,0xffc1d9dc);
        Lettering.draw(p,"-",30,194,20,0xffc1d9dc);Lettering.draw(p,"+",245,194,20,0xffc1d9dc);
        Lettering.draw(p,"TOWARD "+g.settings.waveBearing()+" DEG",70,191,11,0xffc1d9dc);
        Lettering.draw(p,"RESET / 0 DEG = NORTH",24,241,11,0xff92aeb9);
    }
}
