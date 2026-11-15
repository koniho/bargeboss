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
            ScreenView.button(g,p,Layout.START,t.resumeShift?"BACK TO SHIFT":"START SHIFT",true);
            return;
        }
        if(t.lesson==Tutorial.CHANNEL) {
            boolean entering=g.tug.x<TrainingChannel.X[0];
            VisualStyle.panel(p,18,68,286,114,VisualStyle.NAVY);
            Lettering.draw(p,"CAPTAIN'S PRACTICE / 3 OF "+Tutorial.DONE,30,79,9,VisualStyle.GOLD);
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
        boolean markerOnLeft=t.lesson==Tutorial.MOVE && g.layout.camera.screenX(t.driveTargetX(),t.driveTargetY())<g.layout.width*.5f;
        Painter coach=ScreenView.offset(p,t.lesson>=Tutorial.PULL || markerOnLeft?g.layout.width-486:-18,-30);
        VisualStyle.panel(coach,36,98,432,124,VisualStyle.NAVY);
        coach.rect(44,105,416,3,VisualStyle.GOLD);
        Lettering.draw(coach,"CAPTAIN'S PRACTICE",54,116,10,VisualStyle.GOLD);
        Lettering.draw(coach,(t.lesson+1)+" / "+Tutorial.DONE,403,116,10,MUTED);
        Body target=t.lesson==Tutorial.LINK?g.cargo[2]:g.cargo[0];
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
            for(int i=0;i<Tutorial.DRIVE_X.length;i++) {
                float x=Tutorial.DRIVE_X[i],y=Tutorial.DRIVE_Y[i];int color=i<t.driveMarker?ControlView.GREEN:VisualStyle.GOLD;
                if(i>t.driveMarker)continue;
                if(i==t.driveMarker)river.circle(x,y,Tutorial.DRIVE_RADIUS,0x0a35c8bd);
                if(i==t.driveMarker)ControlView.ring(g,river,x,y,Tutorial.DRIVE_RADIUS+pulse,1,color,1.2f);
                SoftCue.dot(river,x,y,4,SoftCue.animated(g,color));
            }
            if(!g.holdingTug())driveArrow(g,river,t.driveTargetX(),t.driveTargetY(),null);
            RiverCamera c=g.layout.camera;float sx=c.screenX(t.driveTargetX(),t.driveTargetY()),sy=c.screenY(t.driveTargetX(),t.driveTargetY());
            sx=Physics.clamp(sx,75,g.layout.width-75);sy=Physics.clamp(sy+48,216,g.layout.height-90);
            p.rect(sx-47,sy-12,94,24,0x40101c30);
            Lettering.center(p,"BUOY "+(t.driveMarker+1)+" / 2",sx,sy-4,10,VisualStyle.GOLD);
        } else if(t.lesson==Tutorial.COAST) {
            SoftCue.caption(river,"LIFT TO COAST",g.tug.x-45,g.tug.y,10,PAPER);
        } else if(t.lesson==Tutorial.RELEASE) {
            Body b=g.tow.primary();if(b==null)return;RiverCamera c=g.layout.camera;
            ControlView.ring(g,river,b.x,b.y,44+pulse,1,ControlView.GREEN,3);
            float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y);
            p.rect(x-54,y-12,108,24,0x40101c30);
            Lettering.center(p,"TAP TO LET GO",x,y-4,10,ControlView.GREEN);
        } else if(t.lesson==Tutorial.LINK) {
            Body spare=g.cargo[2],parent=g.cargo[0];
            ControlView.ring(g,river,spare.x,spare.y,44+pulse,1,VisualStyle.GOLD,2);
            ControlView.ring(g,river,parent.x,parent.y,44+pulse,1,ControlView.GREEN,2);
            ControlView.arrow(g,river,spare.x,spare.y-30,parent.x,parent.y+30,ControlView.GREEN);
            SoftCue.caption(river,"DRAG THIS BARGE",spare.x,spare.y+35,9,VisualStyle.GOLD);
        } else if(t.lesson==Tutorial.PULL || t.lesson==Tutorial.TRAIN) {
            ControlView.arrow(g,river,g.tug.x,g.tug.y-25,g.tug.x,g.tug.y-100,VisualStyle.GOLD);
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
            } else {
                // Put the instruction on the cargo's actual touch target. The old
                // offset sat over the tug and taught players to press the wrong boat.
                RiverCamera c=g.layout.camera;
                float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y);
                p.rect(x-43,y-12,86,24,0x40101c30);
                Lettering.center(p,"HOLD HERE",x,y-4,10,VisualStyle.GOLD);
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
        RiverCamera c=g.layout.camera;
        float sx=c.screenX(b.x,b.y),sy=c.screenY(b.x,b.y);
        if(sx<55 || sx>g.layout.width-55 || sy<210 || sy>g.layout.height-85) {
            float cx=g.layout.width*.5f,cy=g.layout.height*.55f,ex=sx-cx,ey=sy-cy;
            float fraction=Math.min((g.layout.width*.5f-65)/Math.max(1,Math.abs(ex)),
                    (g.layout.height*.32f)/Math.max(1,Math.abs(ey)));
            sx=cx+ex*fraction;sy=cy+ey*fraction;
        }
        canvas.rect(sx-51,sy+28,102,23,0x40101c30);
        Lettering.center(canvas,"NEXT BARGE",sx,sy+33,10,VisualStyle.GOLD);
    }
    private static void gateFeedback(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.gateClearTicks<=0 || t.clearedGate<1) return;
        boolean complete=t.clearedGate==TrainingChannel.X.length-1;
        float age=1-t.gateClearTicks/90f,alpha=Math.min(1,t.gateClearTicks/24f);
        float x=g.layout.width*.5f,y=217;
        if(g.trainingChannel()) {
            RiverCamera c=g.layout.camera;
            x=Physics.clamp(c.screenX(TrainingChannel.X[t.clearedGate],TrainingChannel.Y[t.clearedGate]),130,g.layout.width-130);
            y=Physics.clamp(c.screenY(TrainingChannel.X[t.clearedGate],TrainingChannel.Y[t.clearedGate])-54-18*age,210,g.layout.height-100);
        }
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
        if(!held) {
            float bx=c.screenX(b.x,b.y),by=c.screenY(b.x,b.y);
            canvas.rect(bx-51,by+26,102,24,0x40101c30);
            Lettering.center(canvas,assist?"HOLD STILL":"PRESS + DRAG",bx,by+34,10,SoftCue.alpha(VisualStyle.GOLD,.8f));
        }

    }
}
