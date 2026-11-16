package com.bargeboss.game;

/** Compact field-guide cards and upright cues on the rotated landscape board. */
public final class TutorialView {
    private static final int PAPER=VisualStyle.CREAM,MUTED=VisualStyle.MUTED;
    private TutorialView() {}
    public static void draw(Game g,Painter p) {
        Tutorial t=g.tutorial;
        gateFeedback(g,p);
        if(t.lesson==Tutorial.DONE) {
            p=ScreenView.menu(g,p);
            ScreenView.overlay(p,104,412);
            Lettering.center(p,"PRACTICE COMPLETE",480,141,27,PAPER);
            ScreenView.star(p,302,254+2*(float)StrictMath.sin(g.visualTicks*.06),42,VisualStyle.GOLD);
            Lettering.center(p,"PUSH. TOW. DELIVER.",302,323,20,PAPER);
            Lettering.center(p,"READY FOR WORK.",302,355,20,ControlView.GREEN);
            Lettering.center(p,"DRAG TUG. DRAG CARGO.",770,241,12,PAPER);
            Lettering.center(p,"THROW LINE. TOW. LET GO.",770,272,12,MUTED);
            ScreenView.button(g,p,Layout.START,t.resumeShift?"BACK TO LEVEL":"START LEVEL",true);
            return;
        }
        if(t.lesson==Tutorial.CHANNEL) {
            boolean entering=g.tug.x<TrainingChannel.X[0];
            VisualStyle.panel(p,18,68,286,114,VisualStyle.NAVY);
            Lettering.draw(p,"CAPTAIN'S PRACTICE / "+(Tutorial.CHANNEL+1)+" OF "+Tutorial.DONE,30,79,9,VisualStyle.GOLD);
            Lettering.draw(p,entering?"ENTER THE CANAL":"NAVIGATE THE CANAL",30,100,16,PAPER);
            Lettering.draw(p,entering?"STEER THROUGH THE OPENING AHEAD.":"STEER UPSTREAM THROUGH EACH GATE.",30,127,9,PAPER);
            Lettering.draw(p,"SLOW AT CORNERS. LIFT TO COAST.",30,145,9,MUTED);
            p.rect(30,167,260,4,0xff35475a);
            p.rect(30,167,260*(t.channelGate-1)/(TrainingChannel.X.length-1f),4,ControlView.GREEN);
            if(entering) {
                float[] entry=new float[2];TrainingChannel.guide(g.tug.x,g.tug.y,t.channelGate,entry);
                driveArrow(g,new RiverPainter(p,g.layout.camera),entry[0],entry[1],"CANAL ENTRANCE");
            }
            return;
        }
        // The tow route runs through the upper-left harbor. Keep its touch
        // targets visible by placing those two cards on the opposite side.
        Painter coach=ScreenView.offset(p,t.lesson>=Tutorial.PULL || (t.lesson==Tutorial.MOVE || t.lesson==Tutorial.REVERSE)?g.layout.width-486:-18,-30);
        VisualStyle.panel(coach,36,98,432,124,VisualStyle.NAVY);
        coach.rect(44,105,416,3,VisualStyle.GOLD);
        Lettering.draw(coach,"CAPTAIN'S PRACTICE",54,116,10,VisualStyle.GOLD);
        Lettering.draw(coach,(t.lesson+1)+" / "+Tutorial.DONE,403,116,10,MUTED);
        Body target=t.lesson==Tutorial.LINK || t.lesson==Tutorial.DOUBLE?g.cargo[2]:g.cargo[0];
        boolean secured=t.lesson==Tutorial.TIE && target!=null && target.lineAttached;
        boolean unloading=secured && target.unloadingTicks>0;
        Lettering.draw(coach,secured?(unloading?"CREW AT WORK":"LINE SECURED"):t.title(),54,137,19,PAPER);
        boolean approach=target!=null && (t.lesson==Tutorial.HOLD || t.lesson==Tutorial.CONNECT)
                && (Physics.length(target.x-g.tug.x,target.y-g.tug.y)>165 || t.lesson==Tutorial.HOLD && g.tug.x<210);
        boolean wrongTug=t.lesson==Tutorial.HOLD && g.holdingTug() && !approach;
        String first=secured?(unloading?"UNLOADING: "+Math.max(1,(Game.UNLOAD_TICKS-target.unloadingTicks+59)/60)+" SECONDS.":"WINCHING THE BARGE ALONGSIDE."):
                approach?(t.lesson==Tutorial.HOLD?"DRIVE INTO THE HARBOR.":"DRIVE TO THE SECOND BARGE."):
                wrongTug?"THAT IS THE TUG. LIFT YOUR FINGER.":t.firstLine();
        String second=secured?(unloading?"THE NEXT BARGE IS WAITING UPSTREAM.":"THE CREW WILL UNLOAD HER NEXT."):
                approach?"APPROACH THE GLOWING BARGE.":wrongTug?"PRESS THE CARGO, THEN DRAG TO PUSH.":t.secondLine();
        if(t.lesson==Tutorial.LINK && (g.tow.incoming(g.cargo[2])==null || g.tow.incoming(g.cargo[0])==null)) {
            first="DRAG THE FREE BARGE TO THE TOWED ONE.";second="RELEASE TO MAKE A SINGLE-FILE TRAIN.";
        }
        if(t.lesson==Tutorial.LINK && !g.tow.connected(g.cargo[0],g.tug) && !g.tow.connected(g.cargo[2],g.tug)) {
            first="RECONNECT A BARGE TO THE TUG.";second="THEN REBUILD YOUR SINGLE-FILE TRAIN.";
        }
        if(t.lesson==Tutorial.MOVE && g.settings.joystickSide()!=GameSettings.OFF)first="CYAN FORWARD / AMBER REVERSE. DRAG TUG.";
        if(t.lesson==Tutorial.MOVE)second="BUOY "+(Math.min(t.driveMarker,1)+1)+" / 2 - "+(int)(100*t.orbitProgress())+"% COMPLETE";
        if(t.lesson==Tutorial.REVERSE && t.reverseArmed){
            boolean joystick=g.settings.joystickSide()!=GameSettings.OFF;
            first=joystick?"USE THE AMBER JOYSTICK REVERSE AREA.":"DRAG TUG BACKWARD. KEEP BOW AHEAD.";
            second="RELEASE TO CHANGE DRIVE / "+(int)(t.reverseProgress()*100)+"% REVERSED";
        }
        Lettering.draw(coach,first,54,168,12,PAPER);
        Lettering.draw(coach,second,54,187,11,MUTED);
        for(int i=0;i<Tutorial.DONE;i++) {
            coach.rect(54+i*(400f/Tutorial.DONE),210,400f/Tutorial.DONE-8,4,i<t.lesson?ControlView.GREEN:i==t.lesson?VisualStyle.GOLD:0xff35475a);
            if(i==t.lesson) coach.rect(54+i*(400f/Tutorial.DONE)+(g.visualTicks%60)/60f*(400f/Tutorial.DONE-15),208,7,2,PAPER);
        }
        Painter river=new RiverPainter(p,g.layout.camera);
        float pulse=.8f*(float)StrictMath.sin(g.visualTicks*.045);
        if(g.towAiming || g.ropeDragging)return; // The live trajectory replaces the demonstration.
        if(approach) { approach(g,p,river,target,pulse);return; }
        if(t.lesson==Tutorial.HOLD) {
            river.circle(Tutorial.PUSH_X,Tutorial.PUSH_Y,58,0x0c35c8bd);
            ControlView.ring(g,river,Tutorial.PUSH_X,Tutorial.PUSH_Y,58+pulse,1,ControlView.GREEN,3);
            SoftCue.caption(river,"PUSH HERE",Tutorial.PUSH_X,Tutorial.PUSH_Y,11,ControlView.GREEN);
        }
        if(t.lesson==Tutorial.MOVE) {
            int buoy=Math.min(t.driveMarker,Tutorial.DRIVE_X.length-1);
            float bx=Tutorial.DRIVE_X[buoy],by=Tutorial.DRIVE_Y[buoy];
            // The course is a soft projected circle; tall lit buoys live in world depth.
            SoftCue.ring(river,bx,by,Tutorial.DRIVE_RADIUS,1,0x5595d7d1,1);
            SoftCue.ring(river,bx,by,Tutorial.DRIVE_RADIUS,t.orbitProgress(),0xbb6fffc2,2);
            float a=t.orbitArmed?t.orbitAngle:(float)StrictMath.PI,d=t.orbitDirection();
            for(int k=1;k<=3;k++) {
                float angle=a+d*(.42f+k*.36f),next=angle+d*.14f;
                SoftCue.arrow(river,bx+Tutorial.DRIVE_RADIUS*(float)StrictMath.cos(angle),by+Tutorial.DRIVE_RADIUS*(float)StrictMath.sin(angle),
                        bx+Tutorial.DRIVE_RADIUS*(float)StrictMath.cos(next),by+Tutorial.DRIVE_RADIUS*(float)StrictMath.sin(next),0x99ffe6a1);
            }
            if(!t.orbitArmed)driveArrow(g,river,t.driveTargetX(),t.driveTargetY(),null);
        } else if(t.lesson==Tutorial.COAST) {
            SoftCue.caption(river,"LIFT TO COAST",g.tug.x-45,g.tug.y,10,PAPER);
        } else if(t.lesson==Tutorial.REVERSE) {
            if(t.reverseArmed){
                SoftCue.ring(river,t.reverseTargetX,t.reverseTargetY,Tutorial.REVERSE_RADIUS+pulse,1,0x7795d7d1,2);
                SoftCue.ring(river,t.reverseTargetX,t.reverseTargetY,Tutorial.REVERSE_RADIUS+4,t.reverseProgress(),0xaa6fffc2,2);
                float dx=t.reverseTargetX-g.tug.x,dy=t.reverseTargetY-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
                SoftCue.arrow(river,g.tug.x+dx/len*28,g.tug.y+dy/len*28,t.reverseTargetX,t.reverseTargetY,SoftCue.animated(g,0x99ffe6a1));
            }
        } else if(t.lesson==Tutorial.RELEASE) {
            Body b=g.tow.primary();if(b==null)return;RiverCamera c=g.layout.camera;
            ControlView.ring(g,river,b.x,b.y,44+pulse,1,ControlView.GREEN,3);
        } else if(t.lesson==Tutorial.DOUBLE) {
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
        } else if(t.lesson==Tutorial.PULL || t.lesson==Tutorial.BRIDGE || t.lesson==Tutorial.TRAIN) {
            driveArrow(g,river,t.towTargetX(),t.towTargetY(g),t.lesson==Tutorial.BRIDGE && !g.bridge.passable()?"WAIT FOR GREEN":null);
            if(t.lesson==Tutorial.PULL) {
                ControlView.ring(g,river,Tutorial.TOW_BERTH_X,Tutorial.TOW_BERTH_Y,Tutorial.TOW_BERTH_RADIUS+pulse,1,ControlView.GREEN,2);
                SoftCue.caption(river,"TOW BERTH",Tutorial.TOW_BERTH_X,Tutorial.TOW_BERTH_Y,10,ControlView.GREEN);
            } else {
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
        VisualStyle.panel(p,x-117,y-9,234,43,0xe1101c30);
        float pop=1+2*(float)StrictMath.sin(Math.min(1,age*5)*Math.PI);
        Lettering.center(p,complete?"CANAL CLEARED!":"GATE "+t.clearedGate+" CLEARED!",x,y,13+pop,cream);
        Lettering.center(p,complete?"READY TO PUSH":"KEEP GOING, CAPTAIN",x,y+21,9,green);
    }
    private static void pushGesture(Game g,Painter canvas,Painter river,Body b,boolean assist) {
        boolean held=g.heldCargo()==b;
        if(held)return; // Live drag feedback replaces the demonstration while the player acts.
        float x=held?g.downX:b.x,y=held?g.downY:b.y;
        float phase=(g.visualTicks%150)/150f;
        float travel=Physics.clamp((phase-.2f)/.5f,0,1);
        float from=assist?1:0,to=assist?0:1,position=from+(to-from)*travel;
        float dx=Tutorial.PUSH_X-b.x,dy=Tutorial.PUSH_Y-b.y,len=Math.max(1,Physics.length(dx,dy));
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
