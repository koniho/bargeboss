package com.bargeboss.game;

/** Compact landscape controls leave the whole river visible. */
public final class AssistView {
    private static final int INK=VisualStyle.OUTLINE,PAPER=VisualStyle.CREAM;
    private AssistView() {}
    public static void draw(Game g,Painter p) {
        Layout layout=g.layout;
        VisualStyle.panel(p,192,layout.height-48,418,40,0xde101c30);
        ControlView.status(g,p);
        button(p,layout.box(Layout.HELP),g.tutorial==null?"HELP":"RESET",0xff2b4156);
        button(p,layout.box(Layout.PRACTICE),g.tutorial==null?"PRACTICE":"EXIT",0xff2b4156);
    }
    private static void button(Painter p,Layout.Box b,String label,int fill) {
        p.rect(b.x+2,b.y+4,b.w,b.h,INK); VisualStyle.panel(p,b.x,b.y,b.w,b.h,fill);
        p.rect(b.x+7,b.y+5,b.w-14,2,0x667b9fb1);
        Lettering.center(p,label,b.centerX(),b.y+14,label.length()>7?11:14,PAPER);
    }
}
