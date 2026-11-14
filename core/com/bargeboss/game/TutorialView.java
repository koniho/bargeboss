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
            VisualStyle.panel(p,18,68,286,114,VisualStyle.NAVY);
            Lettering.draw(p,"CAPTAIN'S PRACTICE / 3 OF 10",30,79,9,VisualStyle.GOLD);
            Lettering.draw(p,"NAVIGATE THE CANAL",30,100,16,PAPER);
            Lettering.draw(p,"STEER UPSTREAM THROUGH EACH GATE.",30,127,9,PAPER);
            Lettering.draw(p,"SLOW AT CORNERS. LIFT TO COAST.",30,145,9,MUTED);
            p.rect(30,167,260,4,0xff35475a);
            p.rect(30,167,260*(t.channelGate-1)/(TrainingChannel.X.length-1f),4,ControlView.GREEN);
            return;
        }
        Painter coach=ScreenView.offset(p,-18,-30);
        VisualStyle.panel(coach,36,98,432,124,VisualStyle.NAVY);
        coach.rect(44,105,416,3,VisualStyle.GOLD);
        Lettering.draw(coach,"CAPTAIN'S PRACTICE",54,116,10,VisualStyle.GOLD);
        Lettering.draw(coach,(t.lesson+1)+" / "+Tutorial.DONE,403,116,10,MUTED);
        Lettering.draw(coach,t.title(),54,137,19,PAPER);
        boolean wrongTug=t.lesson==Tutorial.HOLD && g.holdingTug();
        Lettering.draw(coach,wrongTug?"THAT IS THE TUG. LIFT YOUR FINGER.":t.firstLine(),54,168,12,PAPER);
        Lettering.draw(coach,wrongTug?"PRESS THE CARGO, THEN DRAG TO PUSH.":t.secondLine(),54,187,11,MUTED);
        for(int i=0;i<Tutorial.DONE;i++) {
            coach.rect(54+i*40,210,32,4,i<t.lesson?ControlView.GREEN:i==t.lesson?VisualStyle.GOLD:0xff35475a);
            if(i==t.lesson) coach.rect(54+i*40+(g.visualTicks%60)/60f*25,208,7,2,PAPER);
        }
        Painter river=new RiverPainter(p,g.layout.camera);
        float pulse=2*(float)StrictMath.sin(g.visualTicks*.10);
        if(t.lesson==Tutorial.HOLD) {
            river.circle(Tutorial.PUSH_X,Tutorial.PUSH_Y,58,0x2635c8bd);
            ControlView.ring(river,Tutorial.PUSH_X,Tutorial.PUSH_Y,58+pulse,1,ControlView.GREEN,3);
            ControlView.ring(river,Tutorial.PUSH_X,Tutorial.PUSH_Y,Tutorial.PUSH_RADIUS,1,0x99b9df9c,1);
            RiverPainter.center(river,"PUSH HERE",Tutorial.PUSH_X,Tutorial.PUSH_Y,11,ControlView.GREEN);
        }
        if(t.lesson==Tutorial.MOVE) {
            ControlView.ring(river,g.tug.x,g.tug.y,36+pulse,1,VisualStyle.GOLD,3);
            float x=g.holdingTug()?g.downX:g.tug.x,y=g.holdingTug()?g.downY:g.tug.y;
            ControlView.arrow(river,x,y-25,x,y-100-pulse,VisualStyle.GOLD);
            RiverPainter.center(river,"DRAG THIS WAY",x+22,y-72,10,PAPER);
        } else if(t.lesson==Tutorial.COAST) {
            ControlView.ring(river,g.tug.x,g.tug.y,36+pulse,1,ControlView.BLUE,3);
            RiverPainter.center(river,"LIFT TO COAST",g.tug.x-45,g.tug.y,10,PAPER);
        } else if(t.lesson==Tutorial.RELEASE) {
            Body b=g.cargo[0];RiverCamera c=g.layout.camera;
            ControlView.ring(river,b.x,b.y,44+pulse,1,ControlView.GREEN,3);
            float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y);
            VisualStyle.panel(p,x-54,y-12,108,24,0xee101c30);
            Lettering.center(p,"TAP TO LET GO",x,y-4,10,ControlView.GREEN);
        } else if(t.lesson==Tutorial.PULL) {
            ControlView.ring(river,g.tug.x,g.tug.y,36+pulse,1,VisualStyle.GOLD,3);
            ControlView.arrow(river,g.tug.x,g.tug.y-25,g.tug.x,g.tug.y-100,VisualStyle.GOLD);
        } else if(g.cargo[0]!=null && g.cargo[0].active) {
            Body b=g.cargo[0];
            if(!b.lineAttached) ControlView.ring(river,b.x,b.y,46+pulse,1,VisualStyle.GOLD,3);
            if(t.lesson==Tutorial.CONNECT) {
                ControlView.arrow(river,b.x,b.y-38,g.tug.x,g.tug.y+30,ControlView.GREEN);
                ControlView.ring(river,g.tug.x,g.tug.y,38+pulse,1,ControlView.GREEN,3);
            } else if(t.lesson==Tutorial.HOLD || t.lesson==Tutorial.STEER) {
                pushGesture(g,p,river,b,t.lesson==Tutorial.STEER);
            } else if(t.lesson==Tutorial.TIE) {
                if(g.canTie(b) && !g.ropeDragging) {
                    ControlView.arrow(river,b.x-25,b.y,Game.TIE_X[b.berth]+18,b.y,ControlView.GREEN);
                    ControlView.ring(river,Game.TIE_X[b.berth],b.y,20+pulse,1,PAPER,2);
                }
            } else {
                // Put the instruction on the cargo's actual touch target. The old
                // offset sat over the tug and taught players to press the wrong boat.
                RiverCamera c=g.layout.camera;
                float x=c.screenX(b.x,b.y),y=c.screenY(b.x,b.y);
                VisualStyle.panel(p,x-43,y-12,86,24,0xee101c30);
                Lettering.center(p,"HOLD HERE",x,y-4,10,VisualStyle.GOLD);
            }
        }
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
        float x=held?g.downX:b.x,y=held?g.downY:b.y;
        float phase=(g.visualTicks%150)/150f;
        float travel=Physics.clamp((phase-.2f)/.5f,0,1);
        float from=assist?1:0,to=assist?0:1,position=from+(to-from)*travel;
        float dx=Tutorial.PUSH_X-b.x,dy=Tutorial.PUSH_Y-b.y,len=Math.max(1,Physics.length(dx,dy));
        dx=assist?-65:dx/len*90;dy=assist?-65:dy/len*90;
        float sx=x+dx*from,sy=y+dy*from,tx=x+dx*to,ty=y+dy*to;
        ControlView.path(river,sx,sy,tx,ty,ControlView.BLUE);
        ControlView.arrow(river,sx-8*(assist?-1:1),sy-8*(assist?-1:1),tx,ty,VisualStyle.GOLD);
        RiverCamera c=g.layout.camera;
        float fingerX=c.screenX(x+dx*position,y+dy*position),fingerY=c.screenY(x+dx*position,y+dy*position);
        float pulse=phase<.2f?2*(float)StrictMath.sin(phase*5*Math.PI):0;
        canvas.circle(fingerX,fingerY,12+pulse,0x99101c30);
        canvas.circle(fingerX,fingerY,8+pulse,VisualStyle.CREAM);
        canvas.circle(fingerX,fingerY,4,VisualStyle.GOLD);
        if(!held) {
            float bx=c.screenX(b.x,b.y),by=c.screenY(b.x,b.y);
            VisualStyle.panel(canvas,bx-51,by-12,102,24,0xee101c30);
            Lettering.center(canvas,assist?"HOLD STILL":"PRESS + DRAG",bx,by-4,10,VisualStyle.GOLD);
        }
        Lettering.center(canvas,assist?"BACK TO THE RING":phase<.2f?"PRESS":phase<.7f?"DRAG":"KEEP HOLDING",fingerX,fingerY+20,10,PAPER);
    }
}
