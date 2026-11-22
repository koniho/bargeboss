package com.bargeboss.game;

/** Tap-to-call and explicit operator clearance; unrelated radio replies cannot authorize motion. */
final class TravelLiftRadio {
    private static final int IDLE=0,CALLING=1,QUESTION=2;
    private int phase,ticks,pointer=-1;
    private boolean pressed,offered;
    private final InstructionPanel panel=new InstructionPanel();
    static float x(Game g){return Physics.clamp(g.layout.camera.screenX(MontroseHaulout.X-49,MontroseHaulout.Y+4),38,g.layout.width-38);}
    static float y(Game g){return Physics.clamp(g.layout.camera.screenY(MontroseHaulout.X-49,MontroseHaulout.Y+4)-50*g.layout.camera.scale,215,g.layout.height-165);}
    private boolean available(Game g){return g.mode==Game.PLAY && !g.haulout.requested && phase==IDLE && Physics.length(g.tug.x-MontroseHaulout.X,g.tug.y-MontroseHaulout.Y)<480;}
    private boolean hit(Game g,float x,float y){return Physics.length(x-x(g),y-y(g))<32;}
    boolean down(Game g,int id,float x,float y){
        if(!available(g) || !hit(g,x,y) || g.pointer || g.pinchZoom.active() || g.ropePhase!=0)return false;
        if(pointer>=0)return true;pointer=id;pressed=true;return true;
    }
    boolean move(Game g,int id,float x,float y){if(pointer<0)return false;if(id==pointer && !hit(g,x,y))pressed=false;return true;}
    boolean up(Game g,int id,float x,float y){
        if(pointer<0)return false;
        if(id==pointer){boolean call=pressed && hit(g,x,y) && available(g);cancelTouch();if(call){g.cancelInputs();phase=CALLING;ticks=0;panel.reset();g.radio.transmit(RadioAudio.REPLY);}}
        return true;
    }
    void cancel(int id){if(pointer==id)cancelTouch();}
    void cancelTouch(){pointer=-1;pressed=false;}
    boolean waiting(){return phase==CALLING;}
    void step(Game g){
        if(phase==CALLING){
            ticks++;panel.step(content());
            if(ticks==36){
                offered=MontroseHaulout.clear(g);phase=QUESTION;
                if(offered)g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"TRAVEL LIFT / MONTROSE","WELL CLEAR. READY TO LOWER?","KEEP ALL BOATS OUT UNTIL THE STRAPS ARE DOWN.","LOWER THE STRAPS.","STAND BY.");
                else g.dialogue.showFrom(g,RadioAudio.DOCKMASTER,"TRAVEL LIFT / MONTROSE","HOLD. AREA NOT CLEAR.","MOVE ALL BOATS OUT OF THE WELL AND CALL AGAIN.","COPY. CLEARING THE AREA.");
            }
        }else if(phase==QUESTION && g.dialogue.dockmasterConversation() && g.dialogue.result()>=0){
            boolean lower=offered && g.dialogue.result()==0 && MontroseHaulout.clear(g);
            g.dialogue.close(g);phase=IDLE;ticks=0;
            if(lower){g.haulout.requested=true;g.tell("TRAVEL LIFT: LOWERING STRAPS. STAY CLEAR.");g.radio.receiveDockmaster();}
            else if(offered){g.tell("TRAVEL LIFT: HOLDING. CLEAR THE AREA AND CALL AGAIN.");g.radio.receiveDockmaster();}
        }
    }
    private InstructionPanel.Content content(){return new InstructionPanel.Content("travelift-calling","CALLING TRAVEL LIFT","TRAVEL LIFT, REQUESTING THE STRAPS DOWN.","WAITING FOR THE OPERATOR...",-1,0,"YOU / MONTROSE","RADIO / TX",true);}
    void draw(Game g,Painter p){
        if(waiting()){panel.draw(g,p);return;}
        if(g.dialogue.active || !available(g))return;
        float x=x(g),y=y(g);SoftCue.ring(p,x,y,23,1,SoftCue.animated(g,0xccffd329),1);
        ArcadeIcons.draw(p,ArcadeIcons.RADIO,x,y,.85f,0xffeeddb3);SoftCue.caption(p,"RADIO LIFT",x,y+35,10,0xffeeddb3);
    }
}
