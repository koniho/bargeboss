package com.bargeboss.game;

/** Feedback for the actual controller state, shared by all native renderers. */
public final class ControlView {
    public static final int BLUE=0xff83c8d5, GREEN=0xffb9df9c, AMBER=0xffffc52f, PURPLE=0xffb7a2df;
    private static final int PAPER=VisualStyle.CREAM, INK=VisualStyle.NAVY;
    private static final String[] LABELS={"READY","DRIVING","KEEP HOLDING","LINING UP","PUSHING",
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
        if(g.ropeDragging) return "AIM THE ARC / RELEASE TO THROW THE LASSO";
        if(g.ropePhase==1) return "LINE AWAY / WATCH THE BOLLARD";
        if(g.ropePhase==2) return "MISSED / REELING BACK FOR ANOTHER THROW";
        if(g.ropePhase==3) return "BOLLARD CAUGHT / PULLING THE LINE TIGHT";
        for(Body b:g.cargo) if(b!=null && b.active && b.lineSnapTicks>0) return "LINE TAUT / CREW TAKING OVER";
        for(Body b:g.cargo) if(b!=null && b.active && Mooring.aligning(b)) return "WINCHING ALONGSIDE / LINING UP WITH SHORE";
        for(Body b:g.cargo) if(b!=null && b.active && b.lineAttached) return b.unloadingTicks>0?"CREW UNLOADING / NEXT LOAD IS YOURS":"LINE ATTACHED / SECURING THE LOAD";
        for(Body b:g.cargo) if(g.canTie(b)) return "LIFT, DRAG CARGO TO AIM, RELEASE TO THROW";
        if(g.holdingTug()) return "DRAG FARTHER FOR THRUST / CENTER TO BRAKE";
        switch(g.control) {
            case Game.MOVING: return "DRAG TO STEER / RELEASE TO COAST";
            case Game.HOLDING: return "FILL THE RING TO ENGAGE THE TUG";
            case Game.ALIGNING: return "KEEP YOUR FINGER STILL / TUG APPROACHING";
            case Game.PUSHING: return "HOLD STILL FOR ASSIST / DRAG TO STEER";
            case Game.STEERING: return "MOVE BACK INTO THE RING FOR ASSIST";
            case Game.DOCKING: return "KEEP HOLDING / SLOW AND STEADY";
            case Game.COASTING: return "THRUST OFF / TAP BRAKE TO STOP THE TUG";
            case Game.BRAKING: return "ENGINES REVERSED / SHEDDING SPEED";
            default: return "DRAG TUG TO DRIVE / HOLD CARGO TO PUSH";
        }
    }
    public static void status(Game g,Painter p) {
        String label=g.ropeDragging?"AIM YOUR THROW":g.ropePhase==1?"LASSO AWAY":g.ropePhase==2?"REELING IN":g.ropePhase==3?"BOLLARD HIT":LABELS[g.control];
        if(g.control==Game.IDLE || g.control==Game.BRAKING) for(Body b:g.cargo)
            if(b!=null && b.active && b.lineAttached) { label=b.unloadingTicks>0?"CREW UNLOADING":Mooring.aligning(b)?"DRAWING ALONGSIDE":"TIGHTENING LINE"; break; }
        Layout layout=g.layout;
        Lettering.center(p,label,layout.statusX(),layout.statusY(),14,color(g));
        Lettering.center(p,hint(g),layout.statusX(),layout.statusHintY(),10,PAPER);
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
        if(g.holdingTug()) {
            dragRing(g,p,BLUE);
            if(!g.pushing) ring(p,g.tug.x,g.tug.y,36,g.holdProgress(),AMBER,4);
            else if(g.manualSteering) {
                float dx=g.pointerX-g.downX,dy=g.pointerY-g.downY,len=Physics.length(dx,dy);
                arrow(p,g.tug.x,g.tug.y,g.tug.x+dx/len*70,g.tug.y+dy/len*70,BLUE);
            }
        }
        Body b=g.workingCargo();
        if(b!=null && !g.ropeDragging) {
            ring(p,b.x,b.y,46,1,0x668eaaa9,2);
            if(!g.pushing) ring(p,b.x,b.y,46,g.holdProgress(),AMBER,5);
            else {
                ring(p,b.x,b.y,46,1,color,2);
                float bx=Game.DOCK_X[b.berth],by=Game.DOCK_Y[b.berth];
                ring(p,bx,by,48,1,Game.COLORS[b.berth],2);
                if(g.control==Game.ALIGNING || (g.control==Game.STEERING && !Physics.touching(g.tug,b,3))) {
                    path(p,g.tug.x,g.tug.y,g.guideX,g.guideY,AMBER);
                    ring(p,g.guideX,g.guideY,15,1,AMBER,2);
                    label(p,"APPROACH",g.guideX,g.guideY+26,AMBER);
                }
                if(g.control!=Game.DOCKING) arrow(p,b.x+g.pushX*46,b.y+g.pushY*46,
                        Physics.clamp(b.x+g.pushX*108,Physics.LEFT+5,Physics.RIGHT-5),Physics.clamp(b.y+g.pushY*108,Physics.TOP+5,Physics.BOTTOM-5),color);
                dragRing(g,p,PURPLE);
                if(g.control==Game.DOCKING) {
                    ring(p,b.x,b.y,51,Math.min(1,b.dockTime/1.2f),GREEN,5);
                    label(p,"SETTLING",b.x,b.y-67,GREEN);
                }
            }
        }
        MooringView.draw(g,p);
        ring(p,g.tug.x,g.tug.y,33,1,color,2);
        if(g.control==Game.COASTING || g.control==Game.BRAKING) {
            float speed=g.tug.speed();
            if(speed>5) arrow(p,g.tug.x,g.tug.y,g.tug.x+g.tug.vx*.65f,g.tug.y+g.tug.vy*.65f,color);
        }
        if(g.noticeTicks>0 && (g.notice.startsWith("DELIVERED") || g.notice.startsWith("HARD IMPACT")
                || g.notice.startsWith("CONTRACT"))) {
            label(p,g.notice,454,(RiverPainter.WORLD_TOP+RiverPainter.WORLD_BOTTOM)/2,PAPER);
        }
    }
    private static void dragRing(Game g,Painter p,int color) {
        ring(p,g.downX,g.downY,42,1,0x99eee7d4,1.5f);
        p.circle(g.downX,g.downY,3,PAPER);
        float dx=g.pointerX-g.downX,dy=g.pointerY-g.downY,len=Physics.length(dx,dy);
        float scale=Math.min(1,90/Math.max(1,len));
        if(len>8) {
            p.line(g.downX,g.downY,g.downX+dx*scale,g.downY+dy*scale,2,color);
            p.circle(g.downX+dx*scale,g.downY+dy*scale,8,color);
        }
    }
    private static void label(Painter p,String text,float x,float y,int color) {
        float size=10,w=Lettering.width(text,size)+10;
        if(p instanceof RiverPainter) {
            // The board rotates, but the label and its backing remain screen-upright.
            x=Physics.clamp(x,RiverPainter.WORLD_LEFT+size+4,RiverPainter.WORLD_RIGHT-3);
            y=Physics.clamp(y,RiverPainter.WORLD_TOP+w/2+3,RiverPainter.WORLD_BOTTOM-w/2-3);
            p.rect(x-size-3,y-w/2,size+6,w,0xee101e25);
        } else {
            x=Physics.clamp(x,w/2+4,Layout.WIDTH-w/2-4);
            y=Physics.clamp(y,4,Layout.HEIGHT-size-4);
            p.rect(x-w/2,y-3,w,size+6,0xee101e25);
        }
        RiverPainter.center(p,text,x,y,size,color);
    }
}
