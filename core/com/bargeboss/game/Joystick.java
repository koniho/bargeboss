package com.bargeboss.game;

/** Screen-space thumb input. Projection and boat physics remain in shared Game. */
public final class Joystick {
    public static final float RADIUS=54,TRAVEL=42,DEAD_ZONE=7;
    public boolean active;
    public float x,y;
    private float guideAlpha=1;
    private static final ThreadLocal<float[]> SECTOR=new ThreadLocal<float[]>(){
        protected float[] initialValue(){return new float[8];}
    };
    public float guideAlpha(){return guideAlpha;}
    public void resetGuide(){guideAlpha=1;}
    /** Shared fixed-step fade; releasing mid-fade reverses from the current opacity. */
    public void step(Game g){float target=g.pointer?0:1;guideAlpha+=(target-guideAlpha)*.18f;if(Math.abs(target-guideAlpha)<.001f)guideAlpha=target;}
    public static boolean reverseWorld(float heading,float dx,float dy){
        return dx*(float)StrictMath.cos(heading)+dy*(float)StrictMath.sin(heading)<-.5f*Physics.length(dx,dy);
    }
    public static boolean reverseScreen(float heading,float x,float y){
        return reverseWorld(heading,(x-y/RiverCamera.DEPTH)/(2*RiverCamera.Q),(x+y/RiverCamera.DEPTH)/(2*RiverCamera.Q));
    }
    /** Project the actual 120/240-degree thrust thresholds onto the screen-space pad. */
    public static float reverseStart(float heading){return projectedAngle(heading+(float)(StrictMath.PI*2/3));}
    public static float reverseSweep(float heading){float a=reverseStart(heading),b=projectedAngle(heading+(float)(StrictMath.PI*4/3));while(b<=a)b+=(float)(2*StrictMath.PI);return b-a;}
    private static float projectedAngle(float angle){float x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);return (float)StrictMath.atan2(RiverCamera.DEPTH*(y-x),x+y);}
    private static void band(Painter p,float cx,float cy,float a,float b,float inner,float outer,int color){
        float ax=(float)StrictMath.cos(a),ay=(float)StrictMath.sin(a),bx=(float)StrictMath.cos(b),by=(float)StrictMath.sin(b);float[] q=SECTOR.get();
        q[0]=cx+ax*inner;q[1]=cy+ay*inner;q[2]=cx+ax*outer;q[3]=cy+ay*outer;
        q[4]=cx+bx*outer;q[5]=cy+by*outer;q[6]=cx+bx*inner;q[7]=cy+by*inner;p.poly(q,color);
    }
    private static void reverseGuide(Game g,Painter p,float cx,float cy){
        float alpha=g.joystick.guideAlpha;if(alpha<=0)return;
        float a=reverseStart(g.tug.angle),sweep=reverseSweep(g.tug.angle),step=sweep/8;
        int color=SoftCue.alpha(0xbaffb875,alpha*SoftCue.breath(g.visualTicks));
        for(int i=0;i<8;i++){float lo=a+(i+.075f)*step,hi=a+(i+.925f)*step;
            band(p,cx,cy,lo,hi,25,52,SoftCue.alpha(color,.10f));
            band(p,cx,cy,lo,hi,28,49,SoftCue.alpha(color,.28f));
            band(p,cx,cy,lo,hi,44,49,SoftCue.alpha(color,.66f));
        }
        for(int i=0;i<2;i++){float angle=a+i*sweep,x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);
            SoftCue.line(p,cx+x*25,cy+y*25,cx+x*51,cy+y*51,1,SoftCue.alpha(color,.75f));}
    }
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
        reverseGuide(g,p,cx,cy);
        SoftCue.ring(p,cx,cy,RADIUS,1,j.active?0x7983c8d5:0x467a9fa9,1.3f);
        SoftCue.ring(p,cx,cy,DEAD_ZONE,1,0x327a9fa9,.8f);
        if(j.active)SoftCue.line(p,cx,cy,tx,ty,2,0x5083c8d5);
        p.circle(tx,ty,17,j.active?0x987cc7d2:0x507a9fa9);
        SoftCue.ring(p,tx,ty,18,1,j.active?0xb8cee6df:0x667a9fa9,1);
        Lettering.center(p,"DRIVE",cx,cy+RADIUS+6,8,0x997ca5ac);
    }
}
