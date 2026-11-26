package com.bargeboss.game;

/** Shared tap acknowledgement. Keep the current menu visible before committing its action. */
final class MenuFeedback {
    private Layout.Box box;
    private int owner=-1,releaseId;
    private float releaseX,releaseY,releaseAlpha;
    private double age,wait,accumulator;
    private boolean pending,dispatching,chevron;
    int revision;
    boolean busy(){return pending;}
    void cancel(){owner=-1;pending=false;box=null;age=accumulator=0;revision++;}
    void cancel(int id){if(id==owner || pending && id==releaseId)cancel();}
    void down(Game g,int id,float x,float y){
        if(owner!=-1 || pending)return;
        Layout.Box target=target(g,x,y);
        if(target==null)return;
        box=target;owner=id;age=0;chevron=g.dialogue.active;revision++;
    }
    void move(int id,float x,float y){if(id==owner && box!=null && !box.contains(x,y))cancel();}
    boolean release(int id,float x,float y){
        if(pending && !dispatching)return true;
        if(dispatching || id!=owner || box==null)return false;
        owner=-1;
        if(!box.contains(x,y)){cancel();return false;}
        releaseId=id;releaseX=x;releaseY=y;releaseAlpha=alpha();
        pending=true;wait=0;accumulator=0;revision++;return true;
    }
    void advance(Game g,double seconds){
        if(box==null || Double.isNaN(seconds) || seconds<=0)return;
        // A stalled callback cannot consume the complete acknowledgement in one update.
        accumulator+=Math.min(seconds,1.0/30);
        while(box!=null && accumulator+1e-10>=1.0/60){
            accumulator-=1.0/60;tick(g);
        }
    }
    private void tick(Game g){
        double dt=1.0/60;age+=dt;revision++;
        if(pending && (wait+=dt)+1e-10>=7.0/60){
            pending=false;dispatching=true;
            try{g.pointerUp(releaseId,releaseX,releaseY);}finally{dispatching=false;box=null;revision++;}
        }else if(!pending && owner<0 && age>.24){box=null;}
    }
    float alpha(){
        return pending?releaseAlpha*(float)Math.max(0,1-wait/(7.0/60))
                :(float)(.12+.20*StrictMath.exp(-age*13));
    }
    void draw(Painter p){
        if(box==null)return;
        Layout.Box b=box;float a=alpha();
        int color=((int)(a*255)<<24)|(PilotGlass.AMBER&0xffffff);
        float cut=chevron?14:6;
        p.poly(new float[]{b.x+cut,b.y,b.x+b.w-cut,b.y,b.x+b.w,b.y+b.h/2,
                b.x+b.w-cut,b.y+b.h,b.x+cut,b.y+b.h,b.x,b.y+b.h/2},color);
    }
    private static Layout.Box target(Game g,float x,float y){
        Layout.Box dev=g.developerPanel.feedbackBox(g,x,y);if(dev!=null)return dev;
        Layout.Box weather=g.waveControls.feedbackBox(g,x,y);if(weather!=null)return weather;
        if(g.dialogue.ready()){
            int choice=g.dialogue.hit(g,x,y);return choice<0?null:g.dialogue.choiceBox(g,choice);
        }
        Layout.Box[] choices;
        switch(g.mode){
            case Game.TITLE:choices=new Layout.Box[]{Layout.START,Layout.CHALLENGES,Layout.SETTINGS,Layout.CAPTAIN};break;
            case Game.CAPTAINS:choices=new Layout.Box[]{Layout.BACK,Layout.CAPTAIN_CONFIRM,Layout.CAPTAIN_CARDS[0],Layout.CAPTAIN_CARDS[1],Layout.CAPTAIN_CARDS[2]};break;
            case Game.CHALLENGES:choices=new Layout.Box[]{Layout.TRAINING_HARBOR,Layout.BACK,Layout.CHALLENGE_START,Layout.CHALLENGE_PREV_PAGE,Layout.CHALLENGE_NEXT_PAGE,Layout.CHALLENGE_CARDS[0],Layout.CHALLENGE_CARDS[1],Layout.CHALLENGE_CARDS[2]};break;
            case Game.SETTINGS:choices=new Layout.Box[]{Layout.JOYSTICK_LEFT,Layout.JOYSTICK_RIGHT,Layout.HAPTICS,Layout.SOUND_MUTE,Layout.MUSIC_MUTE,Layout.SETTINGS_BACK};break;
            case Game.PAUSE:choices=new Layout.Box[]{Layout.RESUME,Layout.RESTART,Layout.TITLE_SCREEN};break;
            case Game.HELP:choices=new Layout.Box[]{Layout.LEARN,Layout.START};break;
            case Game.FINISH:choices=new Layout.Box[]{Layout.AGAIN,Layout.FINISH_CHALLENGES,Layout.NEXT_CHALLENGE};break;
            default:choices=g.tutorial!=null && g.tutorial.lesson==Tutorial.DONE?new Layout.Box[]{Layout.START}:g.mode==Game.PLAY && !g.conversation()?new Layout.Box[]{Layout.HELP,Layout.PRACTICE}:new Layout.Box[0];
        }
        for(Layout.Box candidate:choices){Layout.Box b=g.controlBox(candidate);if(b.contains(x,y))return b;}
        return null;
    }
}
