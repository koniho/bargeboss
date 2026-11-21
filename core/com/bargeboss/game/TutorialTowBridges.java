package com.bargeboss.game;

/** Two explicit radio gates separate single-load work from convoy practice. */
final class TutorialTowBridges {
    static final int ENTRY=1,EXIT=2;
    static final float ENTRY_Y=400,EXIT_Y=-300;
    final boolean[] authorized=new boolean[3];
    int phase,ticks,calling;
    private int pointer=-1;
    private boolean pressed;
    void reset(){for(int i=0;i<authorized.length;i++)authorized[i]=false;phase=ticks=calling=0;cancelTouch();}
    boolean busy(){return phase==1 || phase==2;}
    int current(Game g){
        Tutorial t=g.tutorial;
        if(t.lesson==Tutorial.DOUBLE && t.firstTowParked && !g.passages.passable(ENTRY))return ENTRY;
        if(t.lesson==Tutorial.BRIDGE && !g.passages.passable(EXIT))return EXIT;
        return 0;
    }
    boolean eligible(Game g,int index){
        if(index==ENTRY)return g.tutorial.firstTowParked && g.tutorial.lesson>=Tutorial.DOUBLE;
        return index==EXIT && g.tutorial.firstTowParked && Tutorial.doubleTow(g);
    }
    float houseX(Game g,int i){float[] p=g.passages.bridges[i].radioPoint;return g.layout.camera.screenX(p[0],p[1]);}
    float houseY(Game g,int i){float[] p=g.passages.bridges[i].radioPoint;float y=Math.max(100,g.layout.camera.screenY(p[0],p[1])-p[2]*ArticulatedModel.Z_SCALE*g.layout.camera.scale);return InstructionPanel.radioY(g,houseX(g,i),y);}
    boolean contactable(Game g){
        int i=current(g);if(i==0 || authorized[i] || phase!=0 || !eligible(g,i) || g.dialogue.active || g.mode!=Game.PLAY)return false;
        LandmarkBridge b=g.passages.bridges[i];float x=houseX(g,i),y=houseY(g,i);
        return Physics.length(g.tug.x-b.x,g.tug.y-b.y)<330 && x>=28 && x<=g.layout.width-28 && y>=80 && y<=g.layout.height-80
                && !(x>=g.layout.width-562 && y<=192) && !OverviewMap.bounds(g).contains(x,y) && !Joystick.hit(g,x,y);
    }
    private boolean hit(Game g,int i,float x,float y){return i>0 && Math.abs(x-houseX(g,i))<=Math.max(24,22*g.layout.camera.scale) && Math.abs(y-houseY(g,i))<=Math.max(28,27*g.layout.camera.scale);}
    boolean down(Game g,int id,float x,float y){
        if(pointer!=-1)return true;
        if(!contactable(g) || !hit(g,current(g),x,y) || g.pointer || g.pinchZoom.active() || g.ropePhase!=0)return false;
        pointer=id;pressed=true;return true;
    }
    boolean move(Game g,int id,float x,float y){if(pointer<0)return false;if(id==pointer && !hit(g,current(g),x,y))pressed=false;return true;}
    boolean up(Game g,int id,float x,float y){
        if(pointer<0)return false;
        if(id==pointer){int i=current(g);boolean send=pressed && contactable(g) && hit(g,i,x,y);cancelTouch();if(send){g.cancelInputs();calling=i;phase=1;ticks=0;g.radio.transmit(RadioAudio.REPLY);g.haptic(Game.HAPTIC_TICK);}}return true;
    }
    void cancel(int id){if(id==pointer)cancelTouch();}
    void cancelTouch(){pointer=-1;pressed=false;}
    void step(Game g){
        if(phase==0)return;
        ticks++;
        if(phase==1 && ticks>=60){phase=2;ticks=0;g.dialogue.showFrom(g,RadioAudio.BRIDGE,"TOW BRIDGE / CHANNEL 08","BRIDGE CONTROL HERE, CAPTAIN.",
                calling==ENTRY?"READY TO ENTER THE TWO-BARGE BASIN?":"TWO LINES SECURED. REQUEST PASSAGE?","YES. PLEASE OPEN THE BRIDGE.","STAND BY. NOT READY.");}
        else if(phase==2){
            if(g.dialogue.result()<0){ticks=0;return;}
            if(ticks>=24){authorized[calling]=g.dialogue.result()==0 && eligible(g,calling);g.dialogue.close(g);g.radio.receiveBridge();phase=authorized[calling]?3:4;ticks=0;}
        }else if((phase==3 || phase==4) && ticks>=120){phase=0;ticks=0;}
    }
    InstructionPanel.Content content(Game g){
        int i=current(g);if(i==0 && phase==0)return null;
        if(i==0)i=calling;
        String title,first,second;
        if(phase==1){title="CALLING TOW BRIDGE";first="BRIDGE CONTROL, PRACTICE TUG CALLING.";second="WAITING FOR THE BRIDGE OPERATOR...";}
        else if(phase==4){title="ROGER. CALL WHEN READY.";first="THE BRIDGE WILL REMAIN CLOSED.";second="TAP THE BRIDGE HOUSE TO CALL AGAIN.";}
        else if(authorized[i]){title="RAISING THE BRIDGE";first="HOLD SHORT UNTIL THE LIGHT IS GREEN.";second=i==ENTRY?"TWO BARGES ARE WAITING IN THE NEXT BASIN.":"KEEP BOTH BARGES CONNECTED THROUGH THE SPAN.";}
        else {title=i==ENTRY?"CALL INTO THE NEXT BASIN":"CALL FOR CONVOY PASSAGE";first=i==ENTRY?"YOUR FIRST BARGE STAYS HERE.":"BOTH BARGES ARE NOW UNDER TOW.";second="TAP THE GLOWING BRIDGE HOUSE TO CALL.";}
        return new InstructionPanel.Content("tow-radio-"+i+":"+phase+":"+authorized[i],title,first,second,g.tutorial.lesson,0,
                phase==1?"YOU / CHANNEL 08":"BRIDGE CONTROL / CHANNEL 08","RADIO / CH 08",phase==1);
    }
    void cue(Game g,Painter p){if(!contactable(g))return;int i=current(g);float x=houseX(g,i),y=houseY(g,i),r=Math.max(22,23*g.layout.camera.scale);
        ArcadeIcons.draw(p,ArcadeIcons.RADIO,x,y,.85f,PilotGlass.AMBER);SoftCue.ring(p,x,y,r,1,SoftCue.animated(g,0x9995d7d1),1);SoftCue.caption(p,"RADIO",x,y-r-13,9,0xbbe7bc73);}
}
