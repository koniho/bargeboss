package com.bargeboss.game;

/** Field-guide cards and animated cues follow actual lesson progress. */
public final class TutorialView {
    private static final int INK=VisualStyle.OUTLINE,PAPER=VisualStyle.CREAM,MUTED=VisualStyle.MUTED;
    private TutorialView() {}
    public static void draw(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.lesson==Tutorial.DONE) {
            ScreenView.overlay(p,343,406);
            ScreenView.star(p,300,387+2*(float)StrictMath.sin(g.visualTicks*.06),26,VisualStyle.GOLD);
            Lettering.center(p,"PRACTICE COMPLETE",300,431,23,PAPER);
            Lettering.center(p,"YOUR FIRST LOAD IS UNLOADED.",300,479,13,ControlView.GREEN);
            Lettering.center(p,"DRAG TUG. HOLD CARGO.",300,529,14,PAPER);
            Lettering.center(p,"AIM. RELEASE. UNLOAD.",300,559,13,MUTED);
            ScreenView.button(g,p,100,650,400,76,t.resumeShift?"BACK TO YOUR SHIFT":"START SHIFT",true);
            return;
        }
        VisualStyle.panel(p,95,204,410,150,VisualStyle.NAVY);
        p.rect(103,211,394,3,VisualStyle.GOLD);
        Lettering.draw(p,"CAPTAIN'S PRACTICE",113,225,10,VisualStyle.GOLD);
        Lettering.draw(p,(t.lesson+1)+" / 6",438,225,10,MUTED);
        Lettering.draw(p,t.title(),113,247,19,PAPER);
        Lettering.draw(p,t.firstLine(),113,285,12,PAPER);
        Lettering.draw(p,t.secondLine(),113,307,11,MUTED);
        for(int i=0;i<6;i++) {
            p.rect(113+i*63,335,52,5,i<t.lesson?ControlView.GREEN:i==t.lesson?VisualStyle.GOLD:0xff35475a);
            if(i==t.lesson) p.rect(113+i*63+(g.visualTicks%60)/60f*45,333,7,2,PAPER);
        }
        Painter river=new RiverPainter(p);
        float pulse=2*(float)StrictMath.sin(g.visualTicks*.10);
        if(t.lesson==Tutorial.MOVE) {
            ControlView.ring(river,g.tug.x,g.tug.y,31+pulse,1,VisualStyle.GOLD,3);
            float x=g.holdingTug()?g.downX:g.tug.x,y=g.holdingTug()?g.downY:g.tug.y;
            ControlView.arrow(river,x,y-25,x,y-100-pulse,VisualStyle.GOLD);
            Lettering.center(river,"DRAG UP",x,y-121,11,PAPER);
        } else if(t.lesson==Tutorial.STOP) {
            p.rect(30,880,168,4,VisualStyle.GOLD); p.rect(30,936,168,4,VisualStyle.GOLD);
            p.poly(new float[]{102,869-pulse,126,869-pulse,114,878-pulse},VisualStyle.GOLD);
        } else if(g.cargo[0]!=null && g.cargo[0].active) {
            Body b=g.cargo[0];
            if(!b.lineAttached) ControlView.ring(river,b.x,b.y,42+pulse,1,VisualStyle.GOLD,3);
            if(t.lesson==Tutorial.STEER) {
                float x=g.pointer?g.downX:b.x,y=g.pointer?g.downY:b.y;
                ControlView.arrow(river,x-15,y-15,x-65-pulse,y-65-pulse,VisualStyle.GOLD);
            } else if(t.lesson==Tutorial.TIE) {
                if(g.canTie(b) && !g.ropeDragging) {
                    ControlView.arrow(river,b.x-25,b.y,Game.TIE_X[b.berth]+18,b.y,ControlView.GREEN);
                    ControlView.ring(river,Game.TIE_X[b.berth],b.y,20+pulse,1,PAPER,2);
                }
            } else {
                Lettering.center(river,"HOLD HERE",b.x,b.y-67,12,VisualStyle.GOLD);
                p.circle(RiverPainter.screenX(b.x),RiverPainter.screenY(b.y-50),2+pulse*.3f,VisualStyle.GOLD);
            }
        }
    }
}
