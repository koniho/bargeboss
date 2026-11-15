package com.bargeboss.game;

/** Screen-space thumb input. Projection and boat physics remain in shared Game. */
public final class Joystick {
    public static final float RADIUS=54,TRAVEL=42,DEAD_ZONE=7;
    public boolean active;
    public float x,y;
    public static float centerX(Game g){return g.settings.joystickSide()==GameSettings.RIGHT?g.layout.width-86:86;}
    public static float centerY(Game g){return g.layout.height-118;}
    public static boolean visible(Game g){return g.mode==Game.PLAY && g.settings.joystickSide()!=GameSettings.OFF && (g.tutorial==null || g.tutorial.lesson!=Tutorial.DONE);}
    public static boolean hit(Game g,float x,float y){return visible(g) && Physics.length(x-centerX(g),y-centerY(g))<=RADIUS+8;}
    public void begin(Game g,float px,float py){active=true;move(g,px,py);}
    public void move(Game g,float px,float py){float dx=px-centerX(g),dy=py-centerY(g),d=Physics.length(dx,dy);float scale=Math.max(TRAVEL,d);x=dx/scale;y=dy/scale;}
    public float throttle(){return Physics.clamp((Physics.length(x,y)-DEAD_ZONE/TRAVEL)/(1-DEAD_ZONE/TRAVEL),0,1);}
    public void cancel(){active=false;x=y=0;}
    public static void draw(Game g,Painter p) {
        if(!visible(g))return;
        Joystick j=g.joystick;float cx=centerX(g),cy=centerY(g),tx=cx+j.x*TRAVEL,ty=cy+j.y*TRAVEL;
        p.circle(cx,cy,RADIUS,0x44101c30);
        SoftCue.ring(p,cx,cy,RADIUS,1,j.active?0x7983c8d5:0x467a9fa9,1.3f);
        SoftCue.ring(p,cx,cy,DEAD_ZONE,1,0x327a9fa9,.8f);
        if(j.active)SoftCue.line(p,cx,cy,tx,ty,2,0x5083c8d5);
        p.circle(tx,ty,17,j.active?0x987cc7d2:0x507a9fa9);
        SoftCue.ring(p,tx,ty,18,1,j.active?0xb8cee6df:0x667a9fa9,1);
        Lettering.center(p,"DRIVE",cx,cy+RADIUS+6,8,0x997ca5ac);
    }
}
