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
    public static final Box CHALLENGES=new Box(632,492,276,42),
            BACK=new Box(40,516,220,56), CHALLENGE_START=new Box(660,516,260,56),
            FINISH_CHALLENGES=new Box(60,450,240,64), NEXT_CHALLENGE=new Box(660,450,240,64);
    public static final Box[] CHALLENGE_CARDS={new Box(60,184,260,236),new Box(360,184,260,236),new Box(660,184,260,236)};
    public static final Layout DEFAULT=new Layout(WIDTH,HEIGHT);
    public final float width,height,offsetX,offsetY;
    public final RiverCamera camera;
    public Layout(float width,float height) {
        this.width=Math.max(WIDTH,width); this.height=Math.max(HEIGHT,height);
        offsetX=(this.width-WIDTH)/2; offsetY=(this.height-HEIGHT)/2;
        float scale=Math.min(this.width/688,this.height/320);
        camera=new RiverCamera((this.width-688*scale)/2,(this.height-320*RiverCamera.DEPTH*scale)/2,scale);
    }

    /** Menu rectangles remain centered; live controls overlay the outer shore. */
    public Box box(Box original) {
        if(original==HAPTICS) return new Box(width-158,4,94,40);
        if(original==PAUSE) return new Box(width-56,4,48,40);
        if(original==BRAKE) return new Box(width-332,height-44,100,40);
        if(original==HELP) return new Box(width-224,height-44,100,40);
        if(original==PRACTICE) return new Box(width-116,height-44,108,40);
        return new Box(original.x+offsetX,original.y+offsetY,original.w,original.h);
    }
    public float statusX() { return 401; }
    public float statusY() { return height-41; }
    public float statusHintY() { return height-21; }

    public static final class Box {
        public final float x,y,w,h;
        public Box(float x,float y,float w,float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        public boolean contains(float px,float py) { return px>=x && px<=x+w && py>=y && py<=y+h; }
        public float centerX() { return x+w*.5f; }
        public float centerY() { return y+h*.5f; }
    }
}
