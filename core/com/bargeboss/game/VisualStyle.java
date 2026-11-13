package com.bargeboss.game;

/** Shared palette and chamfered pixel-art surfaces drawn by all native hosts. */
public final class VisualStyle {
    public static final int OUTLINE=0xff0b1424, NAVY=0xff101c30, PANEL=0xff1d3047,
        CREAM=0xfffff0ce, GOLD=0xffffc52f, RED=0xffd8472f, TEAL=0xff10495d,
        AQUA=0xff57b8be, CONCRETE=0xff8b8795, RUST=0xff8e4e31, MUTED=0xff99adbc;
    private VisualStyle() {}
    public static void panel(Painter p,float x,float y,float w,float h,int fill) {
        p.poly(new float[]{x+6,y,x+w-6,y,x+w,y+6,x+w,y+h-6,x+w-6,y+h,x+6,y+h,x,y+h-6,x,y+6},OUTLINE);
        p.rect(x+3,y+6,w-6,h-12,fill); p.rect(x+6,y+3,w-12,h-6,fill);
        p.line(x+7,y+4,x+w-7,y+4,2,0x446fa4ba);
    }
    public static void bolt(Painter p,float x,float y) {
        p.rect(x-2,y-2,4,4,MUTED); p.rect(x-1,y-1,2,2,OUTLINE);
    }
}
