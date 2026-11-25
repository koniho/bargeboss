package com.bargeboss.game;

/** The shared landscape canvas and every menu/control hit target. */
public final class Layout {
    public static final int WIDTH=960, HEIGHT=600, HEADER_HEIGHT=80, FOOTER_TOP=520;
    public static final float STATUS_CENTER_X=300, STATUS_LABEL_Y=530, STATUS_HINT_Y=552;
    public static final Box SETTINGS=new Box(682,10,94,60),
            JOYSTICK_LEFT=new Box(500,136,112,54),JOYSTICK_RIGHT=new Box(624,136,112,54),
            SETTINGS_BACK=new Box(212,450,524,56);
    public static final Box HAPTICS=new Box(212,202,524,56);
    public static final Box SOUND_LEVEL=new Box(420,278,236,48), MUSIC_LEVEL=new Box(420,358,236,48),
            SOUND_MUTE=new Box(668,278,80,48), MUSIC_MUTE=new Box(668,358,80,48);
    public static final Box HELP=new Box(722,534,102,50),
            PRACTICE=new Box(834,534,110,50);
    public static final Box TRAINING_HARBOR=new Box(60,444,840,54);
    public static final Box LEARN=new Box(632,330,276,64), START=new Box(632,410,276,72);
    public static final Box RESUME=new Box(244,256,472,68), RESTART=new Box(244,342,224,56),
            TITLE_SCREEN=new Box(492,342,224,56), AGAIN=new Box(340,450,280,64);
    public static final Box CHALLENGES=new Box(632,492,276,42),
            BACK=new Box(40,516,220,56), CHALLENGE_START=new Box(660,516,260,56),
            FINISH_CHALLENGES=new Box(60,450,240,64), NEXT_CHALLENGE=new Box(660,450,240,64);
    public static final Box CHALLENGE_PREV_PAGE=new Box(300,516,100,56),
            CHALLENGE_NEXT_PAGE=new Box(530,516,100,56);
    public static final Box[] CHALLENGE_CARDS={new Box(60,138,260,278),new Box(360,138,260,278),new Box(660,138,260,278)};
    public static final Layout DEFAULT=new Layout(WIDTH,HEIGHT);
    public final float width,height,offsetX,offsetY;
    public final RiverCamera camera;
    public Layout(float width,float height) {
        this(width,height,148,460,1);
    }
    public Layout(float width,float height,float worldTop,float worldRight,float zoom) {
        this.width=Math.max(WIDTH,width); this.height=Math.max(HEIGHT,height);
        offsetX=(this.width-WIDTH)/2; offsetY=(this.height-HEIGHT)/2;
        float scale=Math.min(this.width/RiverCamera.BOARD_WIDTH,(this.height-104)/RiverCamera.BOARD_HEIGHT);
        camera=new RiverCamera((this.width-RiverCamera.BOARD_WIDTH*scale)/2,(this.height-RiverCamera.BOARD_HEIGHT*scale)/2,scale*zoom,worldTop,worldRight,zoom);
    }

    /** Small offscreen drawing viewport; never installed on a playable Game. */
    static Layout renderTile(float width,float height,RiverCamera camera) {return new Layout(width,height,camera);}
    private Layout(float width,float height,RiverCamera camera) {
        this.width=width;this.height=height;offsetX=offsetY=0;this.camera=camera;
    }

    /** Place a world focus at a screen anchor using the same inverse as touch input. */
    public static Layout focused(float width,float height,float x,float y,float zoom,float screenX,float screenY) {
        Layout base=new Layout(width,height,148,460,zoom);
        RiverCamera c=base.camera;
        return new Layout(width,height,148+y-c.worldY(screenX,screenY),460+x-c.worldX(screenX,screenY),zoom);
    }

    /** Title keeps voyage setup on the right; other menus use centered targets. */
    public Box titleBox(Box original) {
        if(original==LEARN)return new Box(48,250+offsetY,310,78);
        if(original==START)return new Box(width-332,294+offsetY,284,76);
        if(original==CHALLENGES)return new Box(width-332,228+offsetY,284,44);
        return box(original);
    }
    /** Other menus remain centered; live controls use the outer canvas edges. */
    public Box box(Box original) {
        if(original==SETTINGS)return new Box(width-118,12,100,40);
        if(original==HELP) return new Box(width*.5f-110,height-44,100,40);
        if(original==PRACTICE) return new Box(width*.5f+4,height-44,108,40);
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
