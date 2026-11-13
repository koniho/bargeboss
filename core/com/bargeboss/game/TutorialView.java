package com.bargeboss.game;

/** Compact field-guide cards and upright cues on the rotated landscape board. */
public final class TutorialView {
    private static final int PAPER=VisualStyle.CREAM,MUTED=VisualStyle.MUTED;
    private TutorialView() {}
    public static void draw(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.lesson==Tutorial.DONE) {
            p=ScreenView.menu(g,p);
            ScreenView.overlay(p,104,412);
            Lettering.center(p,"PRACTICE COMPLETE",480,141,27,PAPER);
            ScreenView.star(p,302,254+2*(float)StrictMath.sin(g.visualTicks*.06),42,VisualStyle.GOLD);
            Lettering.center(p,"YOUR FIRST LOAD",302,323,20,PAPER);
            Lettering.center(p,"IS UNLOADED.",302,355,20,ControlView.GREEN);
            Lettering.center(p,"DRAG TUG. HOLD CARGO.",770,241,12,PAPER);
            Lettering.center(p,"AIM. RELEASE. UNLOAD.",770,272,12,MUTED);
            ScreenView.button(g,p,Layout.START,t.resumeShift?"BACK TO SHIFT":"START SHIFT",true);
            return;
        }
        Painter coach=ScreenView.offset(p,g.layout.camera.left-16,g.layout.camera.top-84);
        VisualStyle.panel(coach,36,98,432,124,VisualStyle.NAVY);
        coach.rect(44,105,416,3,VisualStyle.GOLD);
        Lettering.draw(coach,"CAPTAIN'S PRACTICE",54,116,10,VisualStyle.GOLD);
        Lettering.draw(coach,(t.lesson+1)+" / 6",403,116,10,MUTED);
        Lettering.draw(coach,t.title(),54,137,19,PAPER);
        Lettering.draw(coach,t.firstLine(),54,168,12,PAPER);
        Lettering.draw(coach,t.secondLine(),54,187,11,MUTED);
        for(int i=0;i<6;i++) {
            coach.rect(54+i*66,210,55,4,i<t.lesson?ControlView.GREEN:i==t.lesson?VisualStyle.GOLD:0xff35475a);
            if(i==t.lesson) coach.rect(54+i*66+(g.visualTicks%60)/60f*48,208,7,2,PAPER);
        }
        Painter river=new RiverPainter(p,g.layout.camera);
        float pulse=2*(float)StrictMath.sin(g.visualTicks*.10);
        if(t.lesson==Tutorial.MOVE) {
            ControlView.ring(river,g.tug.x,g.tug.y,36+pulse,1,VisualStyle.GOLD,3);
            float x=g.holdingTug()?g.downX:g.tug.x,y=g.holdingTug()?g.downY:g.tug.y;
            ControlView.arrow(river,x,y-25,x,y-100-pulse,VisualStyle.GOLD);
            RiverPainter.center(river,"DRAG LEFT",x+22,y-72,10,PAPER);
        } else if(t.lesson==Tutorial.STOP) {
            Layout.Box b=g.layout.box(Layout.BRAKE);
            p.rect(b.x,b.y,b.w,3,VisualStyle.GOLD); p.rect(b.x,b.y+b.h-3,b.w,3,VisualStyle.GOLD);
            p.poly(new float[]{b.centerX()-10,b.y-10-pulse,b.centerX()+10,b.y-10-pulse,b.centerX(),b.y-2-pulse},VisualStyle.GOLD);
        } else if(g.cargo[0]!=null && g.cargo[0].active) {
            Body b=g.cargo[0];
            if(!b.lineAttached) ControlView.ring(river,b.x,b.y,46+pulse,1,VisualStyle.GOLD,3);
            if(t.lesson==Tutorial.STEER) {
                float x=g.pointer?g.downX:b.x,y=g.pointer?g.downY:b.y;
                ControlView.arrow(river,x-15,y-15,x-65-pulse,y-65-pulse,VisualStyle.GOLD);
            } else if(t.lesson==Tutorial.TIE) {
                if(g.canTie(b) && !g.ropeDragging) {
                    ControlView.arrow(river,b.x-25,b.y,Game.TIE_X[b.berth]+18,b.y,ControlView.GREEN);
                    ControlView.ring(river,Game.TIE_X[b.berth],b.y,20+pulse,1,PAPER,2);
                }
            } else {
                RiverPainter.center(river,"HOLD HERE",b.x+55,b.y,11,VisualStyle.GOLD);
                p.circle(g.layout.camera.screenX(b.x+43,b.y),g.layout.camera.screenY(b.x+43,b.y),2+pulse*.3f,VisualStyle.GOLD);
            }
        }
    }
}
