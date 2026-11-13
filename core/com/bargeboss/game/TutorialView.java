package com.bargeboss.game;

public final class TutorialView {
    private static final int INK=0xff101e25, PAPER=0xffeee7d4, MUTED=0xff8eaaa9;
    private TutorialView() {}
    public static void draw(Game g,Painter p) {
        Tutorial t=g.tutorial;
        if(t.lesson==Tutorial.DONE) {
            p.rect(40,385,520,364,0xf5101e25); p.rect(40,385,520,4,ControlView.GREEN);
            Lettering.center(p,"PRACTICE COMPLETE",300,416,24,PAPER);
            Lettering.center(p,"YOU DELIVERED YOUR FIRST LOAD.",300,471,14,ControlView.GREEN);
            Lettering.center(p,"DRAG TUG. HOLD CARGO.",300,527,14,PAPER);
            Lettering.center(p,"LASSO THE BOLLARD TO SCORE.",300,558,14,PAPER);
            p.rect(100,650,400,76,Game.COLORS[0]);
            Lettering.center(p,t.resumeShift?"BACK TO YOUR SHIFT":"START SHIFT",300,679,18,INK);
            return;
        }
        p.rect(100,205,400,147,0xf5101e25); p.rect(100,205,400,3,ControlView.BLUE);
        Lettering.draw(p,"PRACTICE "+(t.lesson+1)+" / 6",118,222,11,ControlView.BLUE);
        Lettering.draw(p,t.title(),118,247,19,PAPER);
        Lettering.draw(p,t.firstLine(),118,285,13,PAPER);
        Lettering.draw(p,t.secondLine(),118,307,12,MUTED);
        for(int i=0;i<6;i++) p.rect(118+i*61,334,49,4,i<=t.lesson?ControlView.BLUE:0xff30464b);
        Painter river=new RiverPainter(p);
        if(t.lesson==Tutorial.MOVE) {
            ControlView.ring(river,g.tug.x,g.tug.y,30,1,ControlView.BLUE,3);
            float x=g.holdingTug()?g.downX:g.tug.x,y=g.holdingTug()?g.downY:g.tug.y;
            ControlView.arrow(river,x,y,x,y-100,ControlView.BLUE);
        } else if(t.lesson==Tutorial.STOP) {
            p.rect(30,880,168,4,ControlView.GREEN); p.rect(30,936,168,4,ControlView.GREEN);
        } else if(g.cargo[0]!=null && g.cargo[0].active) {
            Body b=g.cargo[0];
            ControlView.ring(river,b.x,b.y,43,1,Game.COLORS[0],3);
            if(t.lesson==Tutorial.STEER) {
                float x=g.pointer?g.downX:b.x,y=g.pointer?g.downY:b.y;
                ControlView.arrow(river,x,y,x-65,y-65,ControlView.PURPLE);
            } else if(t.lesson==Tutorial.TIE) {
                ControlView.arrow(river,b.x,b.y,Game.TIE_X[b.berth]+18,b.y,ControlView.GREEN);
            } else Lettering.center(river,"HOLD HERE",b.x,b.y-67,13,Game.COLORS[0]);
        }
    }
}
