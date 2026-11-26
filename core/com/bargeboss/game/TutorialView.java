package com.bargeboss.game;

/** Compact field-guide cards and upright cues on the rotated landscape board. */
public final class TutorialView {
    private static final int PAPER=PilotGlass.TEXT,MUTED=PilotGlass.MUTED;
    private TutorialView() {}
    static InstructionPanel.Content instructionContent(Game g) {
        Tutorial t=g.tutorial;
        if((t.lesson>=Tutorial.SHOW && t.lesson<=Tutorial.EXTINGUISH) && t.towBridges.current(g)==0 && !t.towBridges.busy())return t.fireworks.content(g);
        InstructionPanel.Content coaching=t.feedback.content();if(coaching!=null)return coaching;
        InstructionPanel.Content radio=t.instructorRadio.content();if(radio!=null)return radio;
        if(t.lesson==Tutorial.CHALLENGE)return t.challenge.content(g);
        InstructionPanel.Content towRadio=t.towBridges.content(g);if(towRadio!=null)return towRadio;
        if(t.lesson==Tutorial.CHANNEL && !t.entering)return t.canal.content(g);
        if(t.arrival!=null && t.arrival.warningTicks>0)return t.arrival.warning();
        if(t.lesson==Tutorial.DONE)return null;
        if(t.entering)return t.arrival.content();
        if(t.lesson==Tutorial.SLIP || t.lesson==Tutorial.MAP)return t.navigation.content(g);
        if(t.lesson==Tutorial.CHANNEL){
            boolean entering=g.tug.x<TrainingChannel.X[0];
            return new InstructionPanel.Content("channel-"+entering,entering?"ENTER THE CANAL":"NAVIGATE THE CANAL",
                    entering?"STEER THROUGH THE OPENING AHEAD.":"STEER UPSTREAM THROUGH EACH GATE.",
                    "SLOW AT CORNERS. LIFT TO COAST.",t.lesson,(t.channelGate-1)/(TrainingChannel.X.length-1f));
        }
        Body target=t.lesson==Tutorial.LINK || t.lesson==Tutorial.DOUBLE || t.lesson==Tutorial.EXTINGUISH?g.cargo[2]:g.cargo[0];
        boolean secured=t.lesson==Tutorial.TIE && target!=null && target.lineAttached;
        boolean unloading=secured && target.unloadingTicks>0;
        String title=secured?(unloading?"CREW AT WORK":"LINE SECURED"):t.title();
        boolean approach=target!=null && (t.lesson==Tutorial.HOLD || t.lesson==Tutorial.CONNECT)
                && (Physics.length(target.x-g.tug.x,target.y-g.tug.y)>165 || t.lesson==Tutorial.HOLD && g.tug.x<210);
        boolean wrongTug=t.lesson==Tutorial.HOLD && g.holdingTug() && !approach;
        String first=secured?(unloading?"UNLOADING: "+Math.max(1,(Game.UNLOAD_TICKS-target.unloadingTicks+59)/60)+" SECONDS.":"WINCHING THE BARGE ALONGSIDE."):
                approach?(t.lesson==Tutorial.HOLD?"DRIVE INTO THE HARBOR.":"APPROACH THE FAR-SIDE BARGE."):
                wrongTug?"THAT IS THE TUG. LIFT YOUR FINGER.":t.firstLine();
        String second=secured?(unloading?"THE NEXT BARGE IS WAITING UPSTREAM.":"THE CREW WILL UNLOAD HER NEXT."):
                approach?"APPROACH THE GLOWING BARGE.":wrongTug?"PRESS THE CARGO, THEN DRAG TO PUSH.":t.secondLine();
        if(t.lesson>=Tutorial.HOLD && t.lesson<=Tutorial.DOCK && g.tow.incoming(g.cargo[0])!=null)
            return new InstructionPanel.Content("push-release-recovery","RELEASE BEFORE PUSHING",
                    "TAP THE TOWLINE TO LET GO.","THEN PRESS, HOLD AND DRAG THE BARGE.",-1,0);
        if(t.lesson==Tutorial.LINK && (g.tow.incoming(g.cargo[2])==null || g.tow.incoming(g.cargo[0])==null)) {
            first="DRAG THE FREE BARGE TO THE TOWED ONE.";second="MAKE A SINGLE-FILE TRAIN TO FIT THE PASSAGE.";
        }
        if(t.lesson==Tutorial.LINK && !g.tow.connected(g.cargo[0],g.tug) && !g.tow.connected(g.cargo[2],g.tug)) {
            first="RECONNECT A BARGE TO THE TUG.";second="WE NEED SINGLE FILE FOR THE NARROW PASSAGE.";
        }
        if(t.lesson==Tutorial.MOVE)return new InstructionPanel.Content("helm-"+t.helm.phase+":"+t.helm.pace,t.helm.title(),t.helm.first(),t.helm.second(),t.lesson,t.helm.phase/5f);
        if(t.lesson==Tutorial.FIGURE_EIGHT || t.lesson==Tutorial.REVERSE)
            second="BUOY "+(Math.min(t.driveMarker,1)+1)+" / 2 - "+(int)(100*t.orbitProgress())+"% / CROSS THROUGH THE MIDDLE";
        if(t.lesson>=Tutorial.SHOW && g.abilities.selected!=null && g.abilities.visibleTicks>0){
            Body equipment=g.abilities.selected;
            return new InstructionPanel.Content("ability-"+equipment.ability,"BARGE EQUIPMENT",equipment.ability==BargeAbilities.FIREWORKS?"TAP LAUNCH IN THE EQUIPMENT PANEL.":"HOLD THE AIM PAD. DRAG TO AIM AND SET REACH.","TAP THE TOWLINE ITSELF TO DISCONNECT.",t.lesson,0);
        }
        String key=t.lesson+":"+title+":"+approach+":"+wrongTug+":"+t.reverseArmed;
        if(t.lesson==Tutorial.LINK)key+=":"+first;
        return new InstructionPanel.Content(key,title,first,second,t.lesson,0);
    }
    static void buoyFeedback(Game g,Painter p){
        Painter river=new RiverPainter(p,g.layout.camera);
        for(int i=0;i<Tutorial.DRIVE_X.length;i++){
            int remaining=g.tutorial.buoyClearTicks[i];if(remaining<=0)continue;
            float age=Tutorial.BUOY_CLEAR_LIFE-remaining,grow=Physics.clamp(age/8,0,1);
            float t=Physics.clamp((age-28)/(Tutorial.BUOY_CLEAR_LIFE-28f),0,1),fade=1-t*t*(3-2*t);
            float pulse=.5f+.5f*(float)StrictMath.cos(age*.22f);
            float radius=Tutorial.DRIVE_RADIUS+grow*(1+3*pulse),width=2+grow*(3+2*pulse);
            SoftCue.target(g,river,Tutorial.DRIVE_X[i],Tutorial.DRIVE_Y[i],radius,1,SoftCue.alpha(0xffffd329,fade),width);
        }
    }
    public static void draw(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.entering){if(!g.dialogue.active)t.instructionPanel.draw(g,p);return;}
        gateFeedback(g,p);
        buoyFeedback(g,p);
        float burst=t.feedback.burstProgress();
        if(burst<1){
            Painter river=new RiverPainter(p,g.layout.camera);
            SoftCue.burst(river,g.tug.x,g.tug.y,40,burst,SoftCue.YELLOW);
            SoftCue.ring(river,g.tug.x,g.tug.y,40+36*burst,1,SoftCue.alpha(SoftCue.YELLOW,1-burst),3);
        }
        if(t.lesson==Tutorial.DONE) {
            if(!t.instructionPanel.hidden()){t.instructionPanel.draw(g,p);return;}
            p=ScreenView.menu(g,p);
            ScreenView.overlay(p,104,412);
            Lettering.center(p,"PRACTICE COMPLETE",480,141,27,PAPER);
            ScreenView.star(p,302,254+2*(float)StrictMath.sin(g.visualTicks*.06),42,VisualStyle.GOLD);
            Lettering.center(p,"PUSH. TOW. DELIVER.",302,323,20,PAPER);
            Lettering.center(p,"READY FOR WORK.",302,355,20,ControlView.GREEN);
            Lettering.center(p,"THROTTLE. DRAG CARGO.",770,241,12,PAPER);
            Lettering.center(p,"THROW LINE. TOW. LET GO.",770,272,12,MUTED);
            ScreenView.button(g,p,Layout.START,t.resumeShift?"BACK TO LEVEL":"START LEVEL",true);
            return;
        }
        if(t.lesson==Tutorial.CHALLENGE){
            if(!t.instructorRadio.teach && t.instructorRadio.phase==0)ChallengeView.guidance(g,p);
            t.instructionPanel.draw(g,p);
            ChallengeView.scoreboard(g,p);
            return;
        }
        drawLessonGuidance(g,p);
        // Screen-space instructions always composite above world guidance.
        t.instructionPanel.draw(g,p);
    }
    private static void drawLessonGuidance(Game g,Painter p) {
        Tutorial t=g.tutorial;
        t.instructorRadio.cue(g,p);
        if(t.instructorRadio.teach || t.instructorRadio.phase!=0)return;
        if(t.lesson==Tutorial.CHANNEL) {
            if(!t.canal.authorized || !g.passages.passable(0))return;
            if(g.tug.x<TrainingChannel.X[0]) {
                float[] entry=new float[2];TrainingChannel.guide(g.tug.x,g.tug.y,t.channelGate,entry);
                driveArrow(g,new RiverPainter(p,g.layout.camera),entry[0],entry[1],"CANAL ENTRANCE");
            }
            return;
        }
        Body target=t.lesson==Tutorial.LINK || t.lesson==Tutorial.DOUBLE || t.lesson==Tutorial.EXTINGUISH?g.cargo[2]:g.cargo[0];
        boolean approach=target!=null && (t.lesson==Tutorial.HOLD || t.lesson==Tutorial.CONNECT)
                && (Physics.length(target.x-g.tug.x,target.y-g.tug.y)>165 || t.lesson==Tutorial.HOLD && g.tug.x<210);
        Painter river=new RiverPainter(p,g.layout.camera);
        float pulse=.8f*(float)StrictMath.sin(g.visualTicks*.045);
        if(g.towAiming || g.ropeDragging)return; // The live trajectory replaces the demonstration.
        if(approach) { if(t.lesson==Tutorial.CONNECT)driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),null);else approach(g,p,river,target,pulse);return; }
        if(t.lesson>=Tutorial.SHOW && t.lesson<=Tutorial.EXTINGUISH){
            if(t.towBridges.current(g)>0)return;
            Body equipment=t.lesson==Tutorial.SHOW?g.cargo[0]:g.cargo[3];
            if(Physics.length(g.tug.x-equipment.x,g.tug.y-equipment.y)>160){
                if(t.lesson==Tutorial.FIRE_TOW)driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),null);
                else approach(g,p,river,equipment,pulse);return;
            }
            ControlView.ring(g,river,equipment.x,equipment.y,46+pulse,1,VisualStyle.GOLD,3);
            if(t.lesson==Tutorial.FIRE_TOW){
                if(Tutorial.direct(g,equipment)){
                    driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),null);
                    ControlView.ring(g,river,FireworksLesson.TOW_X,FireworksLesson.TOW_Y,50+pulse,1,ControlView.GREEN,3);
                }else ControlView.arrow(g,river,equipment.x,equipment.y,g.tug.x,g.tug.y,ControlView.BLUE);
            }
            if(t.lesson==Tutorial.EXTINGUISH && t.fireworks.heat>0 && !t.fireworks.onStation(equipment)){
                // Next quay group is out of monitor reach: lead the tow to its station.
                int group=t.fireworks.targetGroup();
                driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),null);
                ControlView.ring(g,river,FireworksLesson.STATION_X[group],FireworksLesson.STATION_Y[group],50+pulse,1,ControlView.GREEN,3);
            }
            if(t.lesson==Tutorial.EXTINGUISH)for(int pile=0;pile<FireworksLesson.PILES;pile++){
                if(t.fireworks.heat(pile)==0)continue;
                float x=g.layout.camera.screenX(FireworksLesson.x(pile),FireworksLesson.y(pile)),y=g.layout.camera.screenY(FireworksLesson.x(pile),FireworksLesson.y(pile))-GroundArt.LAND_RISE*g.layout.camera.scale;
                SoftCue.ring(p,x,y,34,1-t.fireworks.heat(pile)/(float)FireworksLesson.PILE_HEAT,SoftCue.CYAN,3);
            }
            return;
        }
        if(t.lesson==Tutorial.SLIP || t.lesson==Tutorial.MAP){
            NavigationPractice n=t.navigation;
            if(t.lesson==Tutorial.MAP && n.mapPhase>=2){
                Layout.Box b=OverviewMap.sliderBounds(g);
                SoftCue.line(p,b.x+24,b.centerY(),b.x+b.w-24,b.centerY(),2,SoftCue.animated(g,0xdd28e7ff));
                if(n.mapPhase==3 && Physics.length(g.tug.x-NavigationPractice.TARGET_X,g.tug.y-NavigationPractice.TARGET_Y)<150)
                    SoftCue.target(g,river,NavigationPractice.TARGET_X,NavigationPractice.TARGET_Y,40+pulse,1,0xdd28e7ff,2);
            }else{
                if(t.lesson==Tutorial.SLIP && n.slipPhase==2 && !n.retry){
                    float l=TrainingChannel.SLIP_LEFT+5,r=TrainingChannel.BASIN_LEFT-2,top=TrainingChannel.SLIP_TOP+5,bottom=TrainingChannel.SLIP_BOTTOM-5;
                    int ink=SoftCue.animated(g,n.retry?SoftCue.RED:SoftCue.CYAN);
                    SoftCue.line(river,l,top,r,top,1.5f,ink);SoftCue.line(river,r,top,r,bottom,1.5f,ink);
                    SoftCue.line(river,r,bottom,l,bottom,1.5f,ink);SoftCue.line(river,l,bottom,l,top,1.5f,ink);
                }else SoftCue.target(g,river,n.targetX(g),n.targetY(g),30+pulse,1,n.retry?SoftCue.RED:SoftCue.CYAN,2);
                driveArrow(g,river,n.targetX(g),n.targetY(g),null);
            }
        } else if(t.lesson==Tutorial.MOVE){
            if(t.helm.phase==HelmPractice.RENDEZVOUS){
                SoftCue.target(g,river,HelmPractice.MEET_X,HelmPractice.MEET_Y,HelmPractice.MEET_RADIUS+pulse,1,0xdd28e7ff,2);
                driveArrow(g,river,HelmPractice.MEET_X,HelmPractice.MEET_Y,null);
            }
        } else if(t.lesson==Tutorial.FIGURE_EIGHT || t.lesson==Tutorial.REVERSE) {
            for(int i=0;i<2;i++)SoftCue.target(g,river,Tutorial.DRIVE_X[i],Tutorial.DRIVE_Y[i],Tutorial.DRIVE_RADIUS,1,0x3395d7d1,1);
            int buoy=Math.min(t.driveMarker,Tutorial.DRIVE_X.length-1);
            float bx=Tutorial.DRIVE_X[buoy],by=Tutorial.DRIVE_Y[buoy];
            // The course is a soft projected circle; tall lit buoys live in world depth.
            SoftCue.target(g,river,bx,by,Tutorial.DRIVE_RADIUS,1,0x5595d7d1,1);
            SoftCue.target(g,river,bx,by,Tutorial.DRIVE_RADIUS,t.orbitProgress(),0xff28e7ff,2);
            float a=t.orbitArmed?t.orbitAngle:(float)StrictMath.PI,d=t.orbitDirection();
            for(int k=1;k<=3;k++) {
                float angle=a+d*(.42f+k*.36f),next=angle+d*.14f;
                SoftCue.arrow(river,bx+Tutorial.DRIVE_RADIUS*(float)StrictMath.cos(angle),by+Tutorial.DRIVE_RADIUS*(float)StrictMath.sin(angle),
                        bx+Tutorial.DRIVE_RADIUS*(float)StrictMath.cos(next),by+Tutorial.DRIVE_RADIUS*(float)StrictMath.sin(next),0x99ffe6a1);
            }
            if(!t.orbitArmed)driveArrow(g,river,t.driveTargetX(),t.driveTargetY(),null);
        } else if(t.lesson==Tutorial.RELEASE) {
            Body b=g.tow.primary();if(b==null)return;RiverCamera c=g.layout.camera;
            ControlView.ring(g,river,b.x,b.y,44+pulse,1,ControlView.GREEN,3);
        } else if(t.lesson==Tutorial.DOUBLE) {
            if(t.towBridges.current(g)>0)return;
            Body b=!Tutorial.direct(g,g.cargo[0])?g.cargo[0]:g.cargo[2];
            if(Physics.length(b.x-g.tug.x,b.y-g.tug.y)>150){approach(g,p,river,b,pulse);return;}
            ControlView.ring(g,river,b.x,b.y,44+pulse,1,VisualStyle.GOLD,2);
            ControlView.arrow(g,river,b.x,b.y,g.tug.x,g.tug.y,ControlView.GREEN);
            SoftCue.caption(river,g.tow.incoming(b)!=null?"TAP TO UNLINK":"DRAG TO TUG",b.x,b.y+38,9,VisualStyle.GOLD);
        } else if(t.lesson==Tutorial.LINK) {
            if(!g.tow.connected(g.cargo[0],g.tug) && !g.tow.connected(g.cargo[2],g.tug)) {
                Body head=g.tow.incoming(g.cargo[0])==null?g.cargo[0]:g.cargo[2];
                if(Physics.length(head.x-g.tug.x,head.y-g.tug.y)>150){approach(g,p,river,head,pulse);return;}
                ControlView.ring(g,river,head.x,head.y,44+pulse,1,VisualStyle.GOLD,2);
                ControlView.arrow(g,river,head.x,head.y,g.tug.x,g.tug.y,ControlView.GREEN);return;
            }
            Body spare=g.cargo[2],parent=g.cargo[0];
            if(g.tow.incoming(parent)==null){spare=g.cargo[0];parent=g.cargo[2];}
            if(g.tow.incoming(spare)!=null) {
                ControlView.ring(g,river,spare.x,spare.y,44+pulse,1,VisualStyle.GOLD,2);
                SoftCue.caption(river,"TAP TO UNLINK",spare.x,spare.y+35,9,VisualStyle.GOLD);return;
            }
            ControlView.ring(g,river,spare.x,spare.y,44+pulse,1,VisualStyle.GOLD,2);
            ControlView.ring(g,river,parent.x,parent.y,44+pulse,1,ControlView.GREEN,2);
            ControlView.arrow(g,river,spare.x,spare.y-30,parent.x,parent.y+30,ControlView.GREEN);
            SoftCue.caption(river,"DRAG THIS BARGE",spare.x,spare.y+35,9,VisualStyle.GOLD);
        } else if(t.lesson==Tutorial.CASTOFF) {
            Body b=g.cargo[0];ControlView.ring(g,river,b.x,b.y,46+pulse,1,VisualStyle.GOLD,3);
            driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),null);
        } else if(t.lesson==Tutorial.BRIDGE || t.lesson==Tutorial.TRAIN) {
            driveArrow(g,river,t.towTargetX(g),t.towTargetY(g),t.lesson==Tutorial.BRIDGE && !g.passages.passable(TutorialTowBridges.EXIT)?"WAIT FOR GREEN":null);
            {
                float y=t.lesson==Tutorial.BRIDGE?Tutorial.BRIDGE_CLEAR_Y:Tutorial.TRAIN_CLEAR_Y;
                int mask=t.lesson==Tutorial.BRIDGE?t.bridgeCrossed:t.trainCrossed;
                SoftCue.line(river,t.lesson==Tutorial.TRAIN?270:200,y,t.lesson==Tutorial.TRAIN?330:400,y,2,SoftCue.animated(g,ControlView.GREEN));
                SoftCue.caption(river,"CLEAR BOTH LOADS: "+Integer.bitCount(mask)+" / 2",300,y-20,9,ControlView.GREEN);
            }
        } else if(g.cargo[0]!=null && g.cargo[0].active) {
            Body b=g.cargo[0];
            if(!b.lineAttached && g.heldCargo()!=b && !g.canTie(b))ControlView.ring(g,river,b.x,b.y,46+pulse,1,VisualStyle.GOLD,2);
            if(t.lesson==Tutorial.CONNECT && !g.towAiming) {
                ControlView.arrow(g,river,b.x,b.y-38,g.tug.x,g.tug.y+30,ControlView.GREEN);
                ControlView.ring(g,river,g.tug.x,g.tug.y,38+pulse,1,ControlView.GREEN,3);
            } else if(t.lesson==Tutorial.HOLD || t.lesson==Tutorial.STEER) {
                pushGesture(g,p,river,b,t.lesson==Tutorial.STEER);
            } else if(t.lesson==Tutorial.TIE) {
                if(g.canTie(b) && !g.ropeDragging) {
                    ControlView.arrow(g,river,b.x-25,b.y,g.bollardX(b.berth)+18,g.bollardY(b.berth),ControlView.GREEN);
                    ControlView.ring(g,river,g.bollardX(b.berth),g.bollardY(b.berth),20+pulse,1,PAPER,2);
                }
            }
        }
    }
    private static void driveArrow(Game g,Painter river,float x,float y,String label) {
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Math.max(1,Physics.length(dx,dy)),reach=Math.min(90,len);
        ControlView.arrow(g,river,g.tug.x+dx/len*30,g.tug.y+dy/len*30,g.tug.x+dx/len*reach,g.tug.y+dy/len*reach,VisualStyle.GOLD);
        if(label!=null) SoftCue.caption(river,label,g.tug.x+dx/len*reach,g.tug.y+dy/len*reach-18,9,PAPER);
    }
    /** A navigation cue precedes the cargo gesture when the next job is farther away. */
    private static void approach(Game g,Painter canvas,Painter river,Body b,float pulse) {
        ControlView.ring(g,river,b.x,b.y,46+pulse,1,VisualStyle.GOLD,3);
        float x=b.x,y=b.y;
        if(g.tug.x<210) {x=230;y=TrainingChannel.Y[TrainingChannel.Y.length-1];}
        float dx=x-g.tug.x,dy=y-g.tug.y,length=Math.max(1,Physics.length(dx,dy));
        float reach=Math.min(95,length);
        ControlView.arrow(g,river,g.tug.x+dx/length*30,g.tug.y+dy/length*30,
                g.tug.x+dx/length*reach,g.tug.y+dy/length*reach,VisualStyle.GOLD);
        SoftCue.caption(river,"DRIVE THIS WAY",g.tug.x+dx/length*reach,g.tug.y+dy/length*reach-18,9,PAPER);

    }
    private static void gateFeedback(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.gateClearTicks<=0 || t.clearedGate<1) return;
        boolean complete=t.clearedGate==TrainingChannel.X.length-1;
        float age=1-t.gateClearTicks/90f,alpha=Math.min(1,t.gateClearTicks/24f);
        float x=g.layout.width*.5f,y=217;
        p=new OpacityPainter(p,alpha);
        int cream=0xffffefcf,green=0xffb9df9c;
        PilotGlass.panel(p,x-117,y-9,234,43,0xe1101c30);
        float pop=1+2*(float)StrictMath.sin(Math.min(1,age*5)*Math.PI);
        Lettering.center(p,complete?"CANAL CLEARED!":"GATE "+t.clearedGate+" CLEARED!",x,y,13+pop,cream);
        Lettering.center(p,complete?"READY TO TOW":"KEEP GOING, CAPTAIN",x,y+21,9,green);
    }
    private static void pushGesture(Game g,Painter canvas,Painter river,Body b,boolean assist) {
        boolean held=g.heldCargo()==b;
        if(held)return; // Live drag feedback replaces the demonstration while the player acts.
        float x=held?g.downX:b.x,y=held?g.downY:b.y;
        float phase=(g.visualTicks%150)/150f;
        float travel=Physics.clamp((phase-.2f)/.5f,0,1);
        float from=assist?1:0,to=assist?0:1,position=from+(to-from)*travel;
        float dx=g.dockX(b)-b.x,dy=g.dockY(b)-b.y,len=Math.max(1,Physics.length(dx,dy));
        dx=assist?-65:dx/len*90;dy=assist?-65:dy/len*90;
        float sx=x+dx*from,sy=y+dy*from,tx=x+dx*to,ty=y+dy*to;
        ControlView.path(river,sx,sy,tx,ty,ControlView.BLUE);
        ControlView.arrow(g,river,sx-8*(assist?-1:1),sy-8*(assist?-1:1),tx,ty,VisualStyle.GOLD);
        RiverCamera c=g.layout.camera;
        float fingerX=c.screenX(x+dx*position,y+dy*position),fingerY=c.screenY(x+dx*position,y+dy*position);
        float pulse=phase<.2f?2*(float)StrictMath.sin(phase*5*Math.PI):0;
        SoftCue.dot(canvas,fingerX,fingerY,7+pulse,SoftCue.animated(g,VisualStyle.GOLD));


    }
}
