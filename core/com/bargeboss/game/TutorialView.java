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
            Lettering.center(p,"THROTTLE SETS YOUR THRUST.",300,527,14,PAPER);
            Lettering.center(p,"WHEEL STEERS. BRAKE STOPS.",300,558,14,PAPER);
            p.rect(100,650,400,76,Game.COLORS[0]);
            Lettering.center(p,t.resumeShift?"BACK TO YOUR SHIFT":"START SHIFT",300,679,18,INK);
            return;
        }
        p.rect(100,205,400,147,0xf5101e25); p.rect(100,205,400,3,ControlView.BLUE);
        Lettering.draw(p,"PRACTICE "+(t.lesson+1)+" / 5",118,222,11,ControlView.BLUE);
        Lettering.draw(p,t.title(),118,247,19,PAPER);
        Lettering.draw(p,t.firstLine(),118,285,13,PAPER);
        Lettering.draw(p,t.secondLine(),118,307,12,MUTED);
        for(int i=0;i<5;i++) p.rect(118+i*74,334,60,4,i<=t.lesson?ControlView.BLUE:0xff30464b);
        if(t.lesson==Tutorial.THROTTLE || (t.lesson==Tutorial.REVERSE && !t.reversed)) {
            float end=t.lesson==Tutorial.THROTTLE?805:918;
            ControlView.arrow(p,55,864,55,end,ControlView.BLUE);
            ControlView.ring(p,110,end,28,1,ControlView.BLUE,3);
        } else if(t.lesson==Tutorial.WHEEL) {
            ControlView.ring(p,505,865,24,1,ControlView.BLUE,3);
            Lettering.center(p,"HOLD HERE",455,787,11,ControlView.BLUE);
        } else if(t.lesson==Tutorial.STOP || (t.lesson==Tutorial.REVERSE && t.reversed)) {
            p.rect(220,880,140,4,ControlView.GREEN); p.rect(220,932,140,4,ControlView.GREEN);
        } else if(t.lesson==Tutorial.DOCK && g.cargo[0]!=null && g.cargo[0].active) {
            Painter river=new RiverPainter(p); Body b=g.cargo[0];
            ControlView.ring(river,b.x,b.y,43,1,Game.COLORS[0],3);
            ControlView.arrow(river,b.x-45,b.y,Game.DOCK_X[2]+25,Game.DOCK_Y[2],ControlView.GREEN);
            Lettering.center(river,"PUSH INTO C",b.x,b.y-67,13,PAPER);
        }
    }
}
