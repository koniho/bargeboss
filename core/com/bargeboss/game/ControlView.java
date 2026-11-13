package com.bargeboss.game;

/** Feedback for the actual controller state, shared by all native renderers. */
public final class ControlView {
    public static final int BLUE=0xff86d4ed, GREEN=0xffa5deb5, AMBER=0xffefb651, PURPLE=0xffd2b2f3;
    private static final int PAPER=0xffeee7d4, INK=0xff101e25;
    private static final String[] LABELS={"READY","MOVING TO TARGET","KEEP HOLDING","LINING UP","PUSHING",
            "MANUAL STEERING","DOCKING","COASTING","BRAKING"};
    private ControlView() {}
    public static int color(Game g) {
        if(g.control==Game.MOVING) return BLUE;
        if(g.control==Game.STEERING) return PURPLE;
        if(g.control==Game.PUSHING || g.control==Game.DOCKING) return GREEN;
        if(g.control==Game.BRAKING) return Game.COLORS[2];
        return g.control==Game.IDLE || g.control==Game.COASTING?PAPER:AMBER;
    }
    public static String hint(Game g) {
        if(g.helm.enabled) {
            if(g.control==Game.PUSHING) return "PUSHING CARGO / EASE OFF TO DOCK";
            if(g.control==Game.DOCKING) return "SETTLING INTO BERTH / HOLD YOUR COURSE";
            if(g.control==Game.BRAKING) return "THROTTLE NEUTRAL / ENGINES BRAKING";
            return "THROTTLE STAYS SET / RELEASE WHEEL TO CENTER";
        }
        switch(g.control) {
            case Game.MOVING: return "TAP A NEW TARGET / BRAKE TO STOP";
            case Game.HOLDING: return "FILL THE RING TO ENGAGE THE TUG";
            case Game.ALIGNING: return "KEEP YOUR FINGER STILL / TUG APPROACHING";
            case Game.PUSHING: return "HOLD STILL FOR ASSIST / DRAG TO STEER";
            case Game.STEERING: return "MOVE BACK INTO THE RING FOR ASSIST";
            case Game.DOCKING: return "KEEP HOLDING / SLOW AND STEADY";
            case Game.COASTING: return "THRUST OFF / TAP BRAKE TO STOP THE TUG";
            case Game.BRAKING: return "ENGINES REVERSED / SHEDDING SPEED";
            default: return "TAP WATER TO MOVE / HOLD CARGO TO PUSH";
        }
    }
    public static void status(Game g,Painter p) {
        String label=LABELS[g.control];
        if(g.helm.enabled && g.control==Game.MOVING) label=g.helm.throttle<0?"REVERSE THRUST":"FORWARD THRUST";
        if(g.helm.enabled && Math.abs(g.helm.rudder)>.12f) label+=(g.helm.rudder<0?" / LEFT":" / RIGHT");
        Lettering.center(p,label,300,721,14,color(g));
        Lettering.center(p,hint(g),300,741,10,PAPER);
    }
    public static void ring(Painter p,float x,float y,float radius,float fraction,int color,float width) {
        int segments=40;
        for(int i=0;i<segments*fraction;i++) {
            double a=-Math.PI/2+i*Math.PI*2/segments, b=-Math.PI/2+Math.min(i+1,segments*fraction)*Math.PI*2/segments;
            p.line(x+radius*(float)StrictMath.cos(a),y+radius*(float)StrictMath.sin(a),
                    x+radius*(float)StrictMath.cos(b),y+radius*(float)StrictMath.sin(b),width,color);
        }
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        float dx=xx-x,dy=yy-y,length=Physics.length(dx,dy); if(length<3) return;
        dx/=length; dy/=length;
        p.line(x,y,xx,yy,3,color);
        p.poly(new float[]{xx,yy,xx-dx*12-dy*7,yy-dy*12+dx*7,xx-dx*12+dy*7,yy-dy*12-dx*7},color);
    }
    public static void path(Painter p,float x,float y,float xx,float yy,int color) {
        float distance=Physics.length(xx-x,yy-y); int count=Math.max(1,(int)(distance/12));
        for(int i=1;i<count;i++) { float t=(float)i/count; p.circle(x+(xx-x)*t,y+(yy-y)*t,2,color); }
    }
    public static void draw(Game g,Painter p) {
        int color=color(g);
        if(g.navigating) {
            ring(p,g.targetX,g.targetY,17,1,BLUE,2);
            path(p,g.tug.x,g.tug.y,g.targetX,g.targetY,BLUE);
            label(p,"TARGET",g.targetX,g.targetY-32,BLUE);
        }
        Body b=g.workingCargo();
        if(b!=null) {
            ring(p,b.x,b.y,39,1,0x668eaaa9,2);
            if(!g.pushing && !g.helm.enabled) ring(p,b.x,b.y,39,g.holdProgress(),AMBER,5);
            else {
                ring(p,b.x,b.y,39,1,color,2);
                float bx=Game.DOCK_X[b.berth],by=Game.DOCK_Y[b.berth];
                ring(p,bx,by,48,1,Game.COLORS[b.berth],2);
                if(g.control==Game.ALIGNING || (g.control==Game.STEERING && !Physics.touching(g.tug,b,3))) {
                    path(p,g.tug.x,g.tug.y,g.guideX,g.guideY,AMBER);
                    ring(p,g.guideX,g.guideY,15,1,AMBER,2);
                    label(p,"APPROACH",g.guideX,g.guideY+26,AMBER);
                }
                if(g.control!=Game.DOCKING) arrow(p,b.x+g.pushX*40,b.y+g.pushY*40,
                        Physics.clamp(b.x+g.pushX*95,115,485),Physics.clamp(b.y+g.pushY*95,220,809),color);
                // The origin stays where the finger first landed: moving cargo need not be chased.
                if(!g.helm.enabled) {
                ring(p,g.downX,g.downY,42,1,0x77eee7d4,1.5f);
                p.circle(g.downX,g.downY,3,PAPER);
                float dx=g.pointerX-g.downX,dy=g.pointerY-g.downY,len=Physics.length(dx,dy);
                float scale=Math.min(1,75/Math.max(1,len));
                if(len>8) {
                    p.line(g.downX,g.downY,g.downX+dx*scale,g.downY+dy*scale,2,PURPLE);
                    p.circle(g.downX+dx*scale,g.downY+dy*scale,8,PURPLE);
                }
                }
                if(g.control==Game.DOCKING) {
                    ring(p,b.x,b.y,44,Math.min(1,b.dockTime/1.2f),GREEN,5);
                    label(p,"MOORING",b.x,b.y-67,GREEN);
                }
            }
        }
        ring(p,g.tug.x,g.tug.y,27,1,color,2);
        if(g.control==Game.COASTING || g.control==Game.BRAKING) {
            float speed=g.tug.speed();
            if(speed>5) arrow(p,g.tug.x,g.tug.y,g.tug.x+g.tug.vx*.65f,g.tug.y+g.tug.vy*.65f,color);
        }
        if(g.noticeTicks>0 && (g.notice.startsWith("DELIVERED") || g.notice.startsWith("HARD IMPACT")
                || g.notice.startsWith("CONTRACT"))) {
            p.rect(117,216,366,29,0xee101e25);
            Lettering.center(p,g.notice,300,224,11,PAPER);
        }
    }
    private static void label(Painter p,String text,float x,float y,int color) {
        float w=Lettering.width(text,10)+10;
        x=Physics.clamp(x,115+w/2,485-w/2); y=Physics.clamp(y,218,799);
        p.rect(x-w/2,y-3,w,17,0xdd101e25); Lettering.center(p,text,x,y,10,color);
    }
}
