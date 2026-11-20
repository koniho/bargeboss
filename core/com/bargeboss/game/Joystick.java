package com.bargeboss.game;

/** Screen-space thumb input. Projection and boat physics remain in shared Game. */
public final class Joystick {
    public static final float RADIUS=81,TRAVEL=63,DEAD_ZONE=7,INTENT_DISTANCE=14;
    public static final int FORWARD_COLOR=0xba75e68c,REVERSE_COLOR=0xbaffb875;
    public boolean active;
    public float x,y;
    private float touchX,touchY;
    private boolean directionReady;
    private float guideAlpha=1,throttleAlpha,throttleAngle;
    private int throttleRgb;
    private static final ThreadLocal<float[]> SECTOR=new ThreadLocal<float[]>(){
        protected float[] initialValue(){return new float[8];}
    };
    public float guideAlpha(){return guideAlpha;}
    public float throttleAlpha(){return throttleAlpha;}
    public void resetGuide(){guideAlpha=1;throttleAlpha=0;throttleAngle=0;throttleRgb=0;}
    /** Shared fixed-step fade; releasing mid-fade reverses from the current opacity. */
    public void step(Game g){
        guideAlpha=ease(guideAlpha,g.pointer?0:1);
        boolean powered=appliedThrottle(g)>0;
        throttleAlpha=ease(throttleAlpha,powered?1:0);
        if(powered){throttleAngle=thrustAngle(g);throttleRgb=(g.driveDirection()<0?REVERSE_COLOR:FORWARD_COLOR)&0xffffff;}
    }
    private static float ease(float value,float target){value+=(target-value)*.18f;return Math.abs(target-value)<.001f?target:value;}
    private static float appliedThrottle(Game g){
        if(g.driveDirection()==0)return 0;
        return g.joystick.active?g.joystick.throttle():g.holdingTug() && g.pushing && g.manualSteering?
                Physics.clamp((Physics.length(g.pointerX-g.downX,g.pointerY-g.downY)-26)/60,0,1):0;
    }
    private static float thrustAngle(Game g){
        Joystick j=g.joystick;float dx=j.active?j.x:0,dy=j.active?j.y:0;
        if(!j.active && g.holdingTug() && g.manualSteering){
            float wx=g.pointerX-g.downX,wy=g.pointerY-g.downY;
            dx=RiverCamera.Q*(wx+wy);dy=RiverCamera.Q*RiverCamera.DEPTH*(wy-wx);
        }
        return Physics.length(dx,dy)>.001f?(float)StrictMath.atan2(dy,dx):
                projectedAngle(g.tug.angle+(g.driveDirection()<0?(float)StrictMath.PI:0));
    }
    public static boolean reverseWorld(float heading,float dx,float dy){
        return dx*(float)StrictMath.cos(heading)+dy*(float)StrictMath.sin(heading)<-.5f*Physics.length(dx,dy);
    }
    public static boolean reverseScreen(float heading,float x,float y){
        return reverseWorld(heading,(x-y/RiverCamera.DEPTH)/(2*RiverCamera.Q),(x+y/RiverCamera.DEPTH)/(2*RiverCamera.Q));
    }
    /** Project the actual 120/240-degree thrust thresholds onto the screen-space pad. */
    public static float reverseStart(float heading){return projectedAngle(heading+(float)(StrictMath.PI*2/3));}
    public static float reverseSweep(float heading){float a=reverseStart(heading),b=projectedAngle(heading+(float)(StrictMath.PI*4/3));while(b<=a)b+=(float)(2*StrictMath.PI);return b-a;}
    public static float forwardStart(float heading){return reverseStart(heading)+reverseSweep(heading);}
    public static float forwardSweep(float heading){return (float)(2*StrictMath.PI)-reverseSweep(heading);}
    private static float projectedAngle(float angle){float x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);return (float)StrictMath.atan2(RiverCamera.DEPTH*(y-x),x+y);}
    private static void band(Painter p,float cx,float cy,float a,float b,float inner,float outer,int color){
        float ax=(float)StrictMath.cos(a),ay=(float)StrictMath.sin(a),bx=(float)StrictMath.cos(b),by=(float)StrictMath.sin(b);float[] q=SECTOR.get();
        q[0]=cx+ax*inner;q[1]=cy+ay*inner;q[2]=cx+ax*outer;q[3]=cy+ay*outer;
        q[4]=cx+bx*outer;q[5]=cy+by*outer;q[6]=cx+bx*inner;q[7]=cy+by*inner;p.poly(q,color);
    }
    private static void guideSector(Painter p,float cx,float cy,float a,float sweep,int segments,int color){
        float step=sweep/segments;
        for(int i=0;i<segments;i++){float lo=a+(i+.075f)*step,hi=a+(i+.925f)*step;
            band(p,cx,cy,lo,hi,25,52,SoftCue.alpha(color,.10f));
            band(p,cx,cy,lo,hi,28,49,SoftCue.alpha(color,.28f));
            band(p,cx,cy,lo,hi,44,49,SoftCue.alpha(color,.66f));
        }
    }
    private static void driveGuide(Game g,Painter p,float cx,float cy){
        float alpha=g.joystick.guideAlpha;if(alpha<=0)return;
        float a=reverseStart(g.tug.angle),sweep=reverseSweep(g.tug.angle);
        float power=alpha*SoftCue.breath(g.visualTicks);
        int reverse=SoftCue.alpha(REVERSE_COLOR,power);
        guideSector(p,cx,cy,a,sweep,8,reverse);
        guideSector(p,cx,cy,forwardStart(g.tug.angle),forwardSweep(g.tug.angle),16,SoftCue.alpha(FORWARD_COLOR,power));
        for(int i=0;i<2;i++){float angle=a+i*sweep,x=(float)StrictMath.cos(angle),y=(float)StrictMath.sin(angle);
            SoftCue.line(p,cx+x*25,cy+y*25,cx+x*51,cy+y*51,1,SoftCue.alpha(reverse,.75f));}
    }
    public static float centerX(Game g){return g.settings.joystickSide()==GameSettings.RIGHT?g.layout.width-(RADIUS+32):RADIUS+32;}
    public static float centerY(Game g){return g.layout.height-(RADIUS+90);}
    public static boolean visible(Game g){return g.mode==Game.PLAY && (g.settings.joystickSide()!=GameSettings.OFF || g.tutorial!=null && g.tutorial.lesson==Tutorial.MOVE) && (g.tutorial==null || g.tutorial.lesson!=Tutorial.DONE && !g.tutorial.entering);}
    public static boolean hit(Game g,float x,float y){return visible(g) && Physics.length(x-centerX(g),y-centerY(g))<=RADIUS+8;}
    public void begin(Game g,float px,float py){
        active=true;touchX=px;touchY=py;x=y=0;directionReady=false;
    }
    public void move(Game g,float px,float py){
        // Measure intent from where the thumb actually landed, not an assumed
        // perfect hit on the pad center. Touch-down alone must never select gear.
        float dx=px-touchX,dy=py-touchY,d=Physics.length(dx,dy),scale=Math.max(TRAVEL,d);
        x=dx/scale;y=dy/scale;
        if(!directionReady && d>=INTENT_DISTANCE)directionReady=true;
        if(directionReady && throttle()>0)g.selectDriveDirection((x-y/RiverCamera.DEPTH)/(2*RiverCamera.Q),(x+y/RiverCamera.DEPTH)/(2*RiverCamera.Q));
    }
    public float throttle(){return directionReady?Physics.clamp((Physics.length(x,y)-DEAD_ZONE/TRAVEL)/(1-DEAD_ZONE/TRAVEL),0,1):0;}
    public void cancel(){active=false;directionReady=false;x=y=0;}
    public static void draw(Game g,Painter p) {
        if(!visible(g))return;
        Joystick j=g.joystick;float cx=centerX(g),cy=centerY(g),tx=cx+j.x*TRAVEL,ty=cy+j.y*TRAVEL;
        boolean engaged=j.active || g.driveDirection()!=0;
        int rgb=(g.driveDirection()<0?REVERSE_COLOR:g.driveDirection()>0?FORWARD_COLOR:0x997ca5ac)&0xffffff;
        p.circle(cx,cy,RADIUS,0x44101c30);
        driveGuide(g,ScreenView.scaled(p,cx,cy,1.5f),0,0);
        SoftCue.ring(p,cx,cy,RADIUS,1,engaged?(0x79000000|rgb):0x467a9fa9,1.3f);
        SoftCue.ring(p,cx,cy,DEAD_ZONE,1,engaged?(0x32000000|rgb):0x327a9fa9,.8f);
        if(j.active)SoftCue.line(p,cx,cy,tx,ty,2,0x50000000|rgb);
        p.circle(tx,ty,25.5f,engaged?(0x98000000|rgb):0x507a9fa9);
        SoftCue.ring(p,tx,ty,27,1,engaged?(0xb8000000|rgb):0x667a9fa9,1);
        if(j.throttleAlpha>0)throttleArc(p,cx,cy,j.throttleAngle,appliedThrottle(g),j.throttleRgb,j.throttleAlpha);
    }

    /** Fixed angular span; radial fill measures thrust against a full-throttle outline. */
    private static void throttleArc(Painter p,float x,float y,float angle,float throttle,int rgb,float alpha){
        float inner=RADIUS+10,outer=RADIUS+26,sweep=(float)(StrictMath.PI*4/9);
        arcBand(p,x,y,angle,sweep,inner,outer,SoftCue.alpha(0x18000000|rgb,alpha));
        if(throttle>0)arcBand(p,x,y,angle,sweep,inner,inner+(outer-inner)*throttle,SoftCue.alpha(0xc8000000|rgb,alpha));
        arcBand(p,x,y,angle,sweep,inner-.6f,inner+.6f,SoftCue.alpha(0x88000000|rgb,alpha));
        arcBand(p,x,y,angle,sweep,outer-.6f,outer+.6f,SoftCue.alpha(0x88000000|rgb,alpha));
        for(int end=-1;end<=1;end+=2){float a=angle+end*sweep*.5f,c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a);
            p.line(x+c*inner,y+s*inner,x+c*outer,y+s*outer,1.2f,SoftCue.alpha(0x88000000|rgb,alpha));
        }
    }
    private static void arcBand(Painter p,float x,float y,float angle,float sweep,float inner,float outer,int color){
        float diameter=2*outer/.98f,rotation=angle-sweep*.5f+(float)StrictMath.PI/2;
        float c=(float)StrictMath.cos(rotation)*diameter,s=(float)StrictMath.sin(rotation)*diameter;
        p.material(x,y,c,s,-s,c,EffectMaterial.RING,(outer-inner)/diameter,sweep/(float)(2*StrictMath.PI),color);
    }
}
