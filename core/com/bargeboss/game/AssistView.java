package com.bargeboss.game;

/** Compact landscape controls leave the whole river visible. */
public final class AssistView {
    private static final int PAPER=PilotGlass.TEXT;
    private AssistView() {}
    public static void draw(Game g,Painter p) {
        Layout layout=g.layout;
        button(p,layout.box(Layout.HELP),g.tutorial==null?"HELP":"RESET");
        button(p,layout.box(Layout.PRACTICE),g.tutorial==null?"PRACTICE":"EXIT");
    }
    private static void button(Painter p,Layout.Box b,String label) {
        PilotGlass.button(p,b.x,b.y,b.w,b.h,false);
        Lettering.center(p,label,b.centerX(),b.y+14,label.length()>7?10:12,PAPER);
    }
}
