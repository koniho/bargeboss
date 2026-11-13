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
    public static final Layout DEFAULT=new Layout(WIDTH,HEIGHT);
    public final float width,height,offsetX,offsetY;
    public final boolean rails;
    public final RiverCamera camera;
    public Layout(float width,float height) {
        this.width=Math.max(WIDTH,width); this.height=Math.max(HEIGHT,height);
        offsetX=(this.width-WIDTH)/2; offsetY=(this.height-HEIGHT)/2;
        rails=this.width/this.height>=2.1f;
        if(this.width==WIDTH && this.height==HEIGHT) camera=RiverCamera.DEFAULT;
        else {
            float availableWidth=this.width-(rails?324:32),availableHeight=this.height-(rails?32:168);
            float scale=Math.min(availableWidth/688,availableHeight/320);
            camera=new RiverCamera((this.width-688*scale)/2,(this.height-320*scale)/2,scale);
        }
    }
    /** Menu rectangles remain centered; live controls reflow into side rails on wide screens. */
    public Box box(Box original) {
        if(rails) {
            if(original==HAPTICS) return new Box(12,height-82,138,64);
            if(original==PAUSE) return new Box(width-150,18,138,60);
            if(original==BRAKE) return new Box(width-150,height-210,138,60);
            if(original==HELP) return new Box(width-150,height-138,138,54);
            if(original==PRACTICE) return new Box(width-150,height-72,138,54);
        } else {
            if(original==HAPTICS || original==PAUSE) return new Box(original.x+offsetX,original.y,original.w,original.h);
            if(original==BRAKE || original==HELP || original==PRACTICE)
                return new Box(original.x+offsetX,original.y+height-HEIGHT,original.w,original.h);
        }
        return new Box(original.x+offsetX,original.y+offsetY,original.w,original.h);
    }
    public float statusX() { return rails?width-81:STATUS_CENTER_X+offsetX; }
    public float statusY() { return rails?106:STATUS_LABEL_Y+height-HEIGHT; }
    public float statusHintY() { return rails?140:STATUS_HINT_Y+height-HEIGHT; }

    public static final class Box {
        public final float x,y,w,h;
        public Box(float x,float y,float w,float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        public boolean contains(float px,float py) { return px>=x && px<=x+w && py>=y && py<=y+h; }
        public float centerX() { return x+w*.5f; }
        public float centerY() { return y+h*.5f; }
    }
}
