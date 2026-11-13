package com.bargeboss.game;

/** The shared landscape canvas and every menu/control hit target. */
public final class Layout {
    public static final int WIDTH=960, HEIGHT=600, HEADER_HEIGHT=80, FOOTER_TOP=520;
    public static final float STATUS_CENTER_X=300, STATUS_LABEL_Y=530, STATUS_HINT_Y=552;
    public static final Box HAPTICS=new Box(786,10,94,60), PAUSE=new Box(890,10,54,60);
    public static final Box BRAKE=new Box(610,534,102,50), HELP=new Box(722,534,102,50),
            PRACTICE=new Box(834,534,110,50);
    public static final Box LEARN=new Box(632,330,276,64), START=new Box(632,410,276,72);
    public static final Box RESUME=new Box(340,300,280,64), RESTART=new Box(340,382,280,64),
            AGAIN=new Box(340,450,280,64);
    private Layout() {}
    public static final class Box {
        public final float x,y,w,h;
        public Box(float x,float y,float w,float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        public boolean contains(float px,float py) { return px>=x && px<=x+w && py>=y && py<=y+h; }
        public float centerX() { return x+w*.5f; }
        public float centerY() { return y+h*.5f; }
    }
}
