package com.bargeboss.game;

/** Shared tap acknowledgement. Keep the current menu visible before committing its action. */
final class MenuFeedback {
    private Layout.Box box;
    private int owner=-1,releaseId;
    private float releaseX,releaseY,releaseAlpha;
    private double age,wait,accumulator;
    private boolean pending,dispatching;
    private float[] outline;
    int revision;
    boolean busy(){return pending;}
    void cancel(){owner=-1;pending=false;box=null;age=accumulator=0;revision++;}
    void cancel(int id){if(id==owner || pending && id==releaseId)cancel();}
    void down(Game g,int id,float x,float y){
        if(owner!=-1 || pending)return;
        Layout.Box target=target(g,x,y);
        if(target==null)return;
        box=target;owner=id;age=0;outline=shape(g,target,x,y);revision++;
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
        float strength=alpha()/.32f;
        // Saturated accent at the edge, with enough transparent fill to preserve labels.
        int fill=((int)(strength*100)<<24)|(PilotGlass.AMBER&0xffffff);
        int edge=((int)(strength*255)<<24)|(PilotGlass.AMBER&0xffffff);
        PilotGlass.plate(p,outline,fill,edge,2);
    }
    private static float[] shape(Game g,Layout.Box b,float x,float y){
        if(g.developerPanel.feedbackBox(g,x,y)!=null)return PilotGlass.buttonShape(b.x,b.y,b.w,b.h,g.developerPanel.feedbackPrimary(g,x,y));
        if(g.waveControls.feedbackBox(g,x,y)!=null)return rectangle(b);
        if(g.dialogue.ready()){int choice=g.dialogue.hit(g,x,y);if(choice>=0)return PilotGlass.replyShape(b,choice%2==0);}
        if(g.mode==Game.CHALLENGES)for(Layout.Box card:Layout.CHALLENGE_CARDS)
            if(g.controlBox(card).contains(x,y))return PilotGlass.panelShape(b.x,b.y,b.w,b.h);
        if(g.mode==Game.SETTINGS && !g.controlBox(Layout.SETTINGS_BACK).contains(x,y))return rectangle(b);
        boolean primary=false;
        switch(g.mode){
            case Game.TITLE:case Game.HELP:primary=g.controlBox(Layout.START).contains(x,y);break;
            case Game.CHALLENGES:primary=g.controlBox(Layout.CHALLENGE_START).contains(x,y)
                    || g.trainingSelected && g.controlBox(Layout.TRAINING_HARBOR).contains(x,y);break;
            case Game.SETTINGS:primary=true;break;
            case Game.PAUSE:primary=g.controlBox(Layout.RESUME).contains(x,y);break;
            case Game.FINISH:
                boolean next=Challenges.nextStage(g.challengeId)>=0 && g.career.unlocked(Challenges.nextStage(g.challengeId));
                primary=g.controlBox(Layout.NEXT_CHALLENGE).contains(x,y) || !next && g.controlBox(Layout.AGAIN).contains(x,y);break;
            default:primary=g.tutorial!=null && g.tutorial.lesson==Tutorial.DONE;
        }
        return PilotGlass.buttonShape(b.x,b.y,b.w,b.h,primary);
    }
    private static float[] rectangle(Layout.Box b){return new float[]{b.x,b.y,b.x+b.w,b.y,b.x+b.w,b.y+b.h,b.x,b.y+b.h};}
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
            default:choices=g.tutorial!=null && g.tutorial.lesson==Tutorial.DONE?new Layout.Box[]{Layout.START}:g.mode==Game.PLAY && !g.conversation() && g.tutorial!=null?new Layout.Box[]{Layout.HELP,Layout.PRACTICE}:new Layout.Box[0];
        }
        for(Layout.Box candidate:choices){Layout.Box b=g.controlBox(candidate);if(b.contains(x,y))return b;}
        return null;
    }
}
