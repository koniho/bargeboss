package com.bargeboss.game;

/** Radio authorization for the mouth bridge; scenery, input and timing stay shared. */
public final class TutorialCanalBridge {
    static final int IDLE=0,CALLING=1,QUESTION=2,ACKNOWLEDGE=3,STANDBY=4;
    // Place the footing width entirely on the canal banks at the mouth.
    static final float X=TrainingChannel.MIN+24,Y=TrainingChannel.Y[0];
    private static final class Geometry {
        static final LandmarkBridge[] BRIDGES={new LandmarkBridge("practice-mouth","CANAL BRIDGE",X,Y,
                (float)StrictMath.PI/2,LandmarkBridge.BASCULE,1,24,TrainingChannel.HALF_WIDTH,TrainingChannel.HALF_WIDTH),
                new LandmarkBridge("practice-tow-entry","TOW BASIN ENTRY",300,TutorialTowBridges.ENTRY_Y,0,LandmarkBridge.BASCULE,1,24,150,150),
                new LandmarkBridge("practice-tow-exit","CONVOY BRIDGE",300,TutorialTowBridges.EXIT_Y,0,LandmarkBridge.BASCULE,1,24,150,150)};
    }
    static LandmarkBridge[] bridges(){return Geometry.BRIDGES;}
    final CanalEscort escort=new CanalEscort();
    boolean authorized;
    int phase,ticks;
    private int pointer=-1;
    private boolean pressed;
    void reset(boolean complete){authorized=complete;phase=IDLE;ticks=0;cancelTouch();escort.reset();}
    boolean contactable(Game g){return g.mode==Game.PLAY && g.tutorial.lesson==Tutorial.CHANNEL && !authorized
            && phase==IDLE && !g.dialogue.active && Physics.length(g.tug.x-X,g.tug.y-Y)<330 && houseVisible(g);}
    private boolean houseVisible(Game g){
        float x=houseX(g),y=houseY(g);
        return x>=28 && x<=g.layout.width-28 && y>=80 && y<=g.layout.height-80
                && !(x>=g.layout.width-450 && y<=192) && !OverviewMap.bounds(g).contains(x,y);
    }
    float houseX(Game g){float[] p=bridges()[0].radioPoint;return g.layout.camera.screenX(p[0],p[1]);}
    float houseY(Game g){float[] p=bridges()[0].radioPoint;return g.layout.camera.screenY(p[0],p[1])-p[2]*ArticulatedModel.Z_SCALE*g.layout.camera.scale;}
    boolean hit(Game g,float x,float y){
        return Math.abs(x-houseX(g))<=Math.max(24,22*g.layout.camera.scale)
                && Math.abs(y-houseY(g))<=Math.max(28,27*g.layout.camera.scale);
    }
    boolean down(Game g,int id,float x,float y){
        if(pointer!=-1)return true;
        if(!contactable(g) || !hit(g,x,y) || g.pointer || g.joystick.active || g.pinchZoom.active() || g.ropePhase!=0)return false;
        pointer=id;pressed=true;return true;
    }
    boolean move(Game g,int id,float x,float y){if(pointer<0)return false;if(id==pointer && !hit(g,x,y))pressed=false;return true;}
    boolean up(Game g,int id,float x,float y){
        if(pointer<0)return false;
        if(id==pointer){boolean send=pressed && hit(g,x,y) && contactable(g);cancelTouch();if(send){
            g.cancelInputs();phase=CALLING;ticks=0;g.radio.transmit(RadioAudio.REPLY);g.haptic(Game.HAPTIC_TICK);
        }}return true;
    }
    void cancel(int id){if(id==pointer)cancelTouch();}
    void cancelTouch(){pointer=-1;pressed=false;}
    void step(Game g){
        if(g.tutorial.lesson!=Tutorial.CHANNEL)return;
        ticks++;
        if(phase==CALLING && ticks>=60){
            phase=QUESTION;ticks=0;
            g.dialogue.showFrom(g,RadioAudio.BRIDGE,"CANAL BRIDGE / CHANNEL 08","CANAL BRIDGE HERE, CAPTAIN.",
                    "READY FOR US TO RAISE THE BRIDGE?","YES. REQUESTING PASSAGE.","STAND BY. NOT READY.");
        }else if(phase==QUESTION){
            if(g.dialogue.result()<0){ticks=0;return;}
            if(ticks>=24){
                authorized=g.dialogue.result()==0;g.dialogue.close(g);g.radio.receiveBridge();
                phase=authorized?ACKNOWLEDGE:STANDBY;ticks=0;
            }
        }else if(phase==STANDBY && ticks>=120){phase=IDLE;ticks=0;}
    }
    InstructionPanel.Content content(Game g){
        if(g.tutorial.lesson!=Tutorial.CHANNEL)return null;
        String key="canal-radio-"+phase,title,first,second,from="INSTRUCTOR / CHANNEL 08";boolean waiting=false;
        if(phase==CALLING){title="CALLING CANAL BRIDGE";first="CANAL BRIDGE, PRACTICE TUG CALLING.";second="WAITING FOR THE BRIDGE OPERATOR...";from="YOU / CHANNEL 08";waiting=true;}
        else if(phase==STANDBY){title="ROGER. CALL WHEN READY.";first="THE BRIDGE WILL REMAIN CLOSED.";second="TAP THE BRIDGE HOUSE TO CALL AGAIN.";from="CANAL BRIDGE / CHANNEL 08";}
        else if(authorized && !g.passages.passable(0)){title="RAISING THE BRIDGE";first="HOLD SHORT UNTIL THE LIGHT IS GREEN.";second="LET YOUR INSTRUCTOR ENTER FIRST.";from="CANAL BRIDGE / CHANNEL 08";}
        else if(authorized)return escort.content(g);
        else if(contactable(g)){title="CONTACT THE BRIDGE HOUSE";first="THE BRIDGE BLOCKS THE CANAL ENTRANCE.";second="TAP THE BRIDGE HOUSE TO USE YOUR RADIO.";}
        else {title="APPROACH THE CANAL BRIDGE";first="FOLLOW THE MARKER TO THE CANAL MOUTH.";second="WE'LL RADIO THE BRIDGE HOUSE THERE.";}
        return new InstructionPanel.Content(key,title,first,second,Tutorial.CHANNEL,
                (g.tutorial.channelGate-1)/(TrainingChannel.X.length-1f),from,"RADIO / CH 08",waiting);
    }
    int guideGate(Game g){return escort.leading?escort.gate:g.tutorial.channelGate;}
    void cue(Game g,Painter p){
        if(!contactable(g))return;
        float x=houseX(g),y=houseY(g),r=Math.max(22,23*g.layout.camera.scale);
        SoftCue.ring(p,x,y,r,1,SoftCue.animated(g,0x9995d7d1),1);
        SoftCue.caption(p,"RADIO",x,y-r-13,9,0xbbe7bc73);
    }
}
