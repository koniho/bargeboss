package com.bargeboss.game;

/** Tap-to-call instructor, with an explicit reply before skipping a complete training phase. */
final class InstructorRadio {
    boolean taught,teach;
    int phase,ticks;
    private int pointer=-1;
    private boolean pressed;
    void reset(){phase=ticks=0;teach=false;cancelTouch();}
    void introduce(Game g){if(!taught){teach=true;g.cancelInputs();g.radio.transmit(RadioAudio.TEACHER);}}
    boolean available(Game g){return g.mode==Game.PLAY && !g.tutorial.entering && g.tutorial.lesson<Tutorial.DONE
            && phase==0 && !g.dialogue.active && g.teacherBoat()!=null
            && !g.tutorial.towBridges.busy() && g.tutorial.canal.phase!=TutorialCanalBridge.CALLING && g.tutorial.canal.phase!=TutorialCanalBridge.QUESTION;}
    float x(Game g){Body b=g.teacherBoat();return g.layout.camera.screenX(b.x,b.y);}
    float y(Game g){Body b=g.teacherBoat();return g.layout.camera.screenY(b.x,b.y)-12*g.layout.camera.scale;}
    private boolean hit(Game g,float x,float y){
        if(g.teacherBoat()==null || x<0 || x>g.layout.width || y<64 || y>=g.layout.height-64
                || OverviewMap.visible(g) && OverviewMap.bounds(g).contains(x,y) || Joystick.hit(g,x,y)
                || g.layout.box(Layout.HELP).contains(x,y) || g.layout.box(Layout.PRACTICE).contains(x,y)
                || (x>g.layout.width-562 && y<192))return false;
        float dx=(x-x(g))/Math.max(30,37*g.layout.camera.scale),dy=(y-y(g))/Math.max(24,28*g.layout.camera.scale);
        return dx*dx+dy*dy<=1;
    }
    boolean down(Game g,int id,float x,float y){
        if(pointer>=0)return true;
        if(!available(g) || g.pointer || g.joystick.active || g.pinchZoom.active() || g.ropePhase!=0 || !hit(g,x,y))return false;
        pointer=id;pressed=true;return true;
    }
    boolean move(Game g,int id,float x,float y){if(pointer<0)return false;if(id==pointer && !hit(g,x,y))pressed=false;return true;}
    boolean up(Game g,int id,float x,float y){
        if(pointer<0)return false;
        if(id==pointer){boolean call=pressed && available(g) && hit(g,x,y);cancelTouch();if(call){
            g.cancelInputs();taught=true;teach=false;phase=1;ticks=0;g.radio.transmit(RadioAudio.REPLY);
        }}return true;
    }
    void cancel(int id){if(id==pointer)cancelTouch();}
    void cancelTouch(){pointer=-1;pressed=false;}
    boolean step(Game g){
        if(phase==0)return teach;
        ticks++;
        if(phase==1 && ticks>=45){
            phase=2;ticks=0;g.dialogue.show(g,"INSTRUCTOR / GREEN TUG","SKIP THE "+g.tutorial.phaseName()+" PHASE?",
                    g.tutorial.skipDetail(),"KEEP PRACTICING.","SKIP ALL "+g.tutorial.phaseName()+".");
        }else if(phase==2){
            if(g.dialogue.result()<0){ticks=0;return true;}
            if(ticks>=24){
                boolean skip=g.dialogue.result()==1;g.dialogue.close(g);phase=ticks=0;
                if(skip)g.tutorial.skip(g);
                g.radio.transmit(RadioAudio.TEACHER);
            }
        }
        return true;
    }
    InstructionPanel.Content content(){
        if(phase==1)return new InstructionPanel.Content("instructor-call","CALLING THE INSTRUCTOR","INSTRUCTOR, PRACTICE TUG CALLING.",
                "WAITING FOR YOUR REPLY...",-1,0,"YOU / CHANNEL 08","RADIO / TX",true);
        if(teach)return new InstructionPanel.Content("learn-instructor-radio","TAP MY GREEN TUG","TAP THE INSTRUCTOR'S BOAT TO RADIO ME.",
                "ASK TO SKIP A WHOLE TRAINING PHASE.",-1,0,"INSTRUCTOR / GREEN TUG","RADIO / CH 08",true);
        return null;
    }
    void cue(Game g,Painter p){
        if(teach && available(g)){SoftCue.ring(p,x(g),y(g),Math.max(32,40*g.layout.camera.scale),1,SoftCue.animated(g,0xdd28e7ff),1);}
    }
}
