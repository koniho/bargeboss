package com.bargeboss.game;

/** Drag controls and reminders, identical on every platform. */
public final class AssistView {
    private static final int INK=0xff101e25,PAPER=0xffeee7d4,MUTED=0xff8eaaa9;
    private AssistView() {}
    public static void draw(Game g,Painter p) {
        p.rect(0,714,600,246,INK); p.rect(24,714,552,1,0xff476064);
        ControlView.status(g,p);
        Lettering.center(p,"DRAG CONTROLS / 2X PACE",300,774,16,ControlView.BLUE);
        Lettering.center(p,"DRAG TUG TO DRIVE / RELEASE TO COAST",300,806,13,PAPER);
        Lettering.center(p,"HOLD CARGO TO PUSH / LASSO TO TIE UP",300,833,12,MUTED);
        button(p,30,"BRAKE",g.braking?Game.COLORS[2]:0xff30464b,g.braking?INK:PAPER);
        button(p,216,g.tutorial==null?"HELP":"RESET LESSON",0xff30464b,PAPER);
        button(p,402,g.tutorial==null?"PRACTICE":"EXIT PRACTICE",0xff30464b,PAPER);
    }
    private static void button(Painter p,float x,String label,int bg,int fg) {
        p.rect(x,880,168,60,bg);
        Lettering.center(p,label,x+84,902,label.length()>10?11:15,fg);
    }
}
