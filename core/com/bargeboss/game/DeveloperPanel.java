package com.bargeboss.game;

/** Opt-in host capability; shared layout, routing and isolated mission initialization. */
public final class DeveloperPanel {
    boolean open;
    /** Job buttons use action codes from here up, clear of the weather controls (30..51) however many jobs a location has. */
    static final int JOBS=100;
    /** Closed DEV PANEL button sits below the always-visible captain portrait. */
    static final float BUTTON_Y=PlayerPortrait.BOTTOM+8;
    private boolean environment;
    /** Weather/time rows: waves, direction, rain, lightning, fog, blizzard, time; they end above the 444 buttons. */
    private static final int ENV_ROWS=7,ENV_ROW=32;
    private static final String[] BLIZZARD={"OFF","LIGHT","HEAVY","WHITEOUT"};
    private int pointer=-1,pressed=-1;
    private boolean enabled(Game g){return g.settings.developerTools && (g.activeGame().mode==Game.PLAY || g.activeGame().mode==Game.PAUSE);}
    private int hit(Game g,float x,float y){
        if(!open)return x>=12 && x<=172 && y>=BUTTON_Y && y<=BUTTON_Y+36?0:-1;
        float left=(g.layout.width-700)/2;
        if(x<left || x>left+700 || y<100 || y>500)return -1;
        if(y<150)return x>left+600?0:-1;
        if(y>=158 && y<=202)return x<left+350?2:3;
        if(environment){
            if(y>=218 && y<218+ENV_ROWS*ENV_ROW)return 30+(int)((y-218)/ENV_ROW)*2+(x>left+350?1:0);
            return y>=444?(x<left+350?50:51):-1;
        }
        int count=jobs(g).length,cols=columns(count);float rowH=rowHeight(count);
        if(y>=218 && y<218+rows(count)*rowH){int index=(int)((y-218)/rowH)*cols+(cols==2 && x>left+350?1:0);return index<count?JOBS+index:-1;}
        return y>=444 && g.activeGame().tutorial!=null?1:-1;
    }
    boolean feedbackPrimary(Game g,float x,float y){int action=hit(g,x,y);return open && (action==2 && !environment || action==3 && environment);}
    Layout.Box feedbackBox(Game g,float x,float y){
        if(!enabled(g) || hit(g,x,y)<0)return null;
        if(!open)return new Layout.Box(12,BUTTON_Y,160,36);
        float left=(g.layout.width-700)/2;int action=hit(g,x,y);
        if(action==0)return new Layout.Box(left+600,108,88,36);
        if(action==2 || action==3)return new Layout.Box(left+(action==2?16:356),158,328,44);
        if(action>=JOBS)return jobBox(left,action-JOBS,jobs(g).length);
        if(action>=30 && action<30+ENV_ROWS*2)return new Layout.Box(left+((action%2)==0?16:630),218+((action-30)/2)*ENV_ROW,54,ENV_ROW-4);
        if(action>=50)return new Layout.Box(left+(action==50?16:356),448,328,40);
        return new Layout.Box(left+16,448,668,40);
    }
    /** Long job lists (Montrose) use two columns of shorter rows so every job fits the panel. */
    private static int columns(int count){return count>4?2:1;}
    private static float rowHeight(int count){return count>8?36:count>4?41:52;}
    private static int rows(int count){return (count+columns(count)-1)/columns(count);}
    private static Layout.Box jobBox(float left,int index,int count){
        int cols=columns(count);float rowH=rowHeight(count),w=cols==2?328:668;
        return new Layout.Box(left+16+(index%cols)*340,218+(index/cols)*rowH,w,rowH-6);
    }
    /** Screen centre of job button `index` (tooling and tests tap through it). */
    static float[] jobCenter(Game g,int index){Layout.Box b=jobBox((g.layout.width-700)/2,index,new DeveloperPanel().jobs(g).length);return new float[]{b.centerX(),b.centerY()};}
    private int[] jobs(Game g){return g.activeGame().tutorial!=null?new int[0]:Challenges.locationJobs(g.activeGame().challengeId);}
    boolean down(Game g,int id,float x,float y){
        if(!enabled(g))return false;int action=hit(g,x,y);
        if(!open && action<0)return false;
        if(pointer<0){g.cancelInputs();pointer=id;pressed=action;}return true;
    }
    boolean move(Game g,int id){return enabled(g) && (open || pointer==id);}
    boolean up(Game g,int id,float x,float y){
        if(!enabled(g) || (!open && pointer!=id))return false;
        if(pointer!=id)return true;
        int action=hit(g,x,y),old=pressed;cancel();
        if(action!=old)return true;
        if(action==0)open=!open;
        else if(action==2 || action==3)environment=action==3;
        else if(action>=JOBS){int mission=jobs(g)[action-JOBS];open=false;g.startDeveloperMission(mission);}
        else if(action>=30){
            Game active=g.activeGame();
            int[] actions={1,2,3,4,7,8,9,10,12,13,16,17,14,15};
            WaveControls.adjust(active,action==50?11:action==51?5:actions[action-30]);
        }
        else if(action==1){open=false;g.startTutorial();}
        return true;
    }
    void cancel(){pointer=pressed=-1;}
    boolean back(){if(!open)return false;open=false;cancel();return true;}
    void draw(Game g,Painter p){
        if(!enabled(g))return;
        if(!open){button(p,12,BUTTON_Y,160,36,"DEV PANEL",false);return;}
        p.rect(0,0,g.layout.width,g.layout.height,0xb8000814);
        float x=(g.layout.width-700)/2;
        PilotGlass.button(p,x,100,700,400,false);
        Lettering.draw(p,"DEVELOPER / "+(g.activeGame().tutorial!=null?"TRAINING":Challenges.locationTitle(g.activeGame().challengeId)),x+20,120,18,0xfffff0ce);
        button(p,x+600,108,88,36,"CLOSE",false);
        button(p,x+16,158,328,44,"MISSIONS",!environment);
        button(p,x+356,158,328,44,"WEATHER / TIME",environment);
        if(environment){
            Game a=g.activeGame();
            String[] labels={"WAVES "+a.settings.waveIntensity()+"X","DIRECTION "+a.settings.waveBearing()+" DEG","RAIN "+a.settings.rainLevel(),"LIGHTNING "+a.settings.lightningLevel(),"FOG "+a.settings.fogLevel(),"BLIZZARD "+BLIZZARD[a.settings.blizzardLevel()],"TIME "+a.lighting().name};
            for(int i=0;i<labels.length;i++){
                float y=218+i*ENV_ROW;button(p,x+16,y,54,ENV_ROW-4,"-",false);
                Lettering.center(p,labels[i],x+350,y+8,13,PilotGlass.TEXT);
                button(p,x+630,y,54,ENV_ROW-4,"+",false);
            }
            button(p,x+16,448,328,40,"TEST FLASH",false);button(p,x+356,448,328,40,"RESET WEATHER / TIME",false);return;
        }
        int[] jobs=jobs(g);
        for(int i=0;i<jobs.length;i++){Layout.Box b=jobBox(x,i,jobs.length);button(p,b.x,b.y,b.w,b.h,title(jobs[i]),false);}
        if(g.activeGame().tutorial!=null)button(p,x+16,448,668,40,"RESTART TUTORIAL",false);
    }
    static String title(int id){
        if(Challenges.calumet(id))return new String[]{"TWO BANKS","BERTH EXCHANGE","BRIDGE CONVOY","BASIN FIVE"}[id];
        return Challenges.title(id);
    }
    private static void button(Painter p,float x,float y,float w,float h,String text,boolean selected){
        PilotGlass.button(p,x,y,w,h,selected);Lettering.draw(p,text,x+12,y+12,14,PilotGlass.buttonText(selected));
    }
}
