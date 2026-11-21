package com.bargeboss.game;

/** Feedback for the actual controller state, shared by all native renderers. */
public final class ControlView {
    public static final int BLUE=0xff83c8d5, GREEN=0xffb9df9c, AMBER=0xffffc52f, PURPLE=0xffb7a2df;
    private static final int PAPER=VisualStyle.CREAM, INK=VisualStyle.NAVY;
    private static final String[] LABELS={"READY","DRIVING","KEEP HOLDING","LINING UP","PUSHING",
            "MANUAL STEERING","DOCKING","COASTING","REVERSE THRUST","WAITING FOR BRIDGE"};
    private ControlView() {}
    public static int color(Game g) {
        if(g.control==Game.MOVING) return BLUE;
        if(g.control==Game.STEERING) return PURPLE;
        if(g.control==Game.PUSHING || g.control==Game.DOCKING) return GREEN;
        if(g.control==Game.REVERSING) return Game.COLORS[2];
        return g.control==Game.IDLE || g.control==Game.COASTING?PAPER:AMBER;
    }
    /** An unloaded hull remains a real obstacle until it is towed away. */
    private static Body emptyToMove(Game g) {
        if(g.tutorial!=null)return null;
        Body held=g.heldCargo();
        if(held!=null)return held.active && held.deliveryRecorded?held:null;
        if(g.selected>=0){Body b=g.cargo[g.selected];if(b!=null && b.active && b.deliveryRecorded && g.tow.incoming(b)==null)return b;}
        if(g.sharedBerthStage()){Body b=g.berthBlocker(2,null);if(b!=null && b.deliveryRecorded)return b;}
        if(g.control==Game.IDLE)for(Body b:g.cargo)if(b!=null && b.active && b.deliveryRecorded && b.lineAttached)return b;
        return null;
    }
    public static String hint(Game g) {
        if(g.tutorial!=null && g.tutorial.entering)return "TAKE THE HELM ONCE INSIDE";
        if(MontroseRescue.active(g))return g.rescue.status(g);
        if(g.montroseStage())return g.tow.count()>0?"USE THROTTLE TO TOW / TAP YACHT TO UNCOUPLE":"DRAG YACHT TO TUG / RELEASE TO LINK";
        if(g.towAiming) return "RELEASE OVER THE HIGHLIGHTED BOAT TO LINK";
        if(g.ropeToTug && g.ropePhase==1) return "TOWLINE AWAY / WATCH THE TARGET";
        if(g.tow.count()>0) {
            if(g.heldCargo()!=null)return g.tow.incoming(g.heldCargo())!=null?"TAP TO UNCOUPLE / DRAG TO BOLLARD":"DRAG TO TUG OR BARGE / RELEASE TO LINK";
            return g.tow.loadCount(g.tug)>0?"USE THROTTLE TO PULL / TAP LINKED BARGE TO UNCOUPLE":"DRAG THE TRAIN HEAD TO THE TUG TO RECONNECT";
        }
        if(g.ropeDragging) return "AIM THE ARC / RELEASE TO THROW THE LASSO";
        if(g.ropePhase==1) return "LINE AWAY / WATCH THE BOLLARD";
        if(g.ropePhase==2) return "MISSED / REELING BACK FOR ANOTHER THROW";
        if(g.tug.lineAttached)return "TETHERED / THROTTLE TO MOVE / TAP BOLLARD TO RELEASE";
        if(g.ropePhase==3) return "BOLLARD CAUGHT / LINE TAUT / WINCHING";
        for(Body b:g.cargo) if(b!=null && b.active && !b.deliveryRecorded && b.lineSnapTicks>0) return "LINE TAUT / CREW TAKING OVER";
        for(Body b:g.cargo) if(b!=null && b.active && Mooring.aligning(b)) return "WINCHING ALONGSIDE / LINING UP WITH SHORE";
        for(Body b:g.cargo) if(b!=null && b.active && b.lineAttached && !b.deliveryRecorded) return b.unloadingTicks>0?"CREW UNLOADING / NEXT LOAD IS YOURS":"LINE ATTACHED / SECURING THE LOAD";
        for(Body b:g.cargo) if(g.canTie(b)) return "LIFT, DRAG CARGO TO AIM, RELEASE TO THROW";
        if(g.bridgeStage() && !g.bridge.passable() && Math.abs(g.tug.y-Bridge.Y)<220) return "BRIDGE / "+g.bridge.signal()+" / HOLD FOR ASSIST";
        if(g.noticeTicks>0 && g.notice.startsWith("TOWLINE RELEASED")) return "LINE BLOCKED OR TOO LONG / RECONNECT NEARBY";
        if(g.lockStage() && g.lock.phase==Lock.FILLING)return "CROSS CURRENT / STEER TO HOLD THE CONVOY";
        if(emptyToMove(g)!=null) return "DRAG EMPTY TO TUG / PULL CLEAR OF BERTH";
        switch(g.control) {
            case Game.MOVING: return "DRAG TO STEER / RELEASE TO COAST";
            case Game.HOLDING: return "FILL THE RING TO ENGAGE THE TUG";
            case Game.ALIGNING: return "KEEP YOUR FINGER STILL / TUG APPROACHING";
            case Game.PUSHING: return "HOLD STILL FOR ASSIST / DRAG TO STEER";
            case Game.STEERING: return "MOVE BACK INTO THE RING FOR ASSIST";
            case Game.DOCKING: return "KEEP HOLDING / SLOW AND STEADY";
            case Game.COASTING: return "THRUST OFF / DRAG AGAINST MOTION TO SLOW";
            case Game.REVERSING: return "ASTERN POWER / RELEASE TO COAST";
            default: return g.trainingChannel()?"THROTTLE UPSTREAM / PASS EACH GATE":"USE THROTTLE TO DRIVE / DRAG CARGO TO PUSH";
        }
    }
    public static void status(Game g,Painter p) {
        String label=g.towAiming?"AIM AT BOAT":g.montroseStage() && g.tow.count()>0?"TOWING YACHT":g.tow.count()>0?(g.tow.loadCount(g.tug)>0?"TOWING "+g.tow.loadCount(g.tug)+" BARGES":"BARGES LINKED"):g.ropeDragging?"AIM YOUR THROW":g.ropePhase==1?"LASSO AWAY":g.ropePhase==2?"REELING IN":g.ropePhase==3?"BOLLARD HIT":LABELS[g.control];
        if(!g.ropeDragging && !g.towAiming && g.tow.count()==0 && g.ropePhase==0) for(Body b:g.cargo) if(g.canTie(b)) { label="READY TO TIE";break; }
        if(g.control==Game.IDLE || g.control==Game.REVERSING) for(Body b:g.cargo)
            if(b!=null && b.active && b.lineAttached && !b.deliveryRecorded) { label=b.unloadingTicks>0?"CREW UNLOADING":Mooring.aligning(b)?"DRAWING ALONGSIDE":"TIGHTENING LINE"; break; }
        if(g.ropePhase==0 && !g.towAiming && !g.ropeDragging && g.tow.count()==0
                && (g.control==Game.IDLE || g.heldCargo()!=null) && emptyToMove(g)!=null)label="EMPTY / TOW CLEAR";
        if(g.tutorial!=null && g.tutorial.entering)label="ARRIVING";
        Layout layout=g.layout;
        Lettering.center(p,label,layout.statusX(),layout.statusY(),14,label.equals("READY TO TIE")?GREEN:color(g));
        Lettering.center(p,hint(g),layout.statusX(),layout.statusHintY(),10,PAPER);
    }

    public static void ring(Painter p,float x,float y,float radius,float fraction,int color,float width) {
        SoftCue.ring(p,x,y,radius,fraction,color,width);
    }
    public static void ring(Game g,Painter p,float x,float y,float radius,float fraction,int color,float width) {
        ring(p,x,y,radius,fraction,SoftCue.animated(g,color),width);
    }
    public static void arrow(Painter p,float x,float y,float xx,float yy,int color) {
        SoftCue.arrow(p,x,y,xx,yy,color);
    }
    public static void arrow(Game g,Painter p,float x,float y,float xx,float yy,int color) {
        if(!assistPush(g))arrow(p,x,y,xx,yy,SoftCue.animated(g,color));
    }
    public static void path(Painter p,float x,float y,float xx,float yy,int color) {
        float distance=Physics.length(xx-x,yy-y); int count=Math.max(1,(int)(distance/12));
        for(int i=1;i<count;i++) { float t=(float)i/count; SoftCue.dot(p,x+(xx-x)*t,y+(yy-y)*t,1.5f,color); }
    }
    static boolean assistPush(Game g){return g.pushing && !g.manualSteering && g.workingCargo()!=null && !g.ropeDragging && !g.towAiming && g.tow.count()==0;}
    public static void draw(Game g,Painter p) {
        int color=color(g);
        Body b=g.workingCargo();
        if(b!=null && !g.ropeDragging && !g.towAiming && g.tow.count()==0 && !g.canTie(b)) {
            if(!g.pushing) ring(g,p,b.x,b.y,46,g.holdProgress(),AMBER,5);
            else if(assistPush(g)) {
                if(g.control!=Game.WAITING_BRIDGE && g.control!=Game.DOCKING)
                    arrow(p,b.x+g.pushX*46,b.y+g.pushY*46,b.x+g.pushX*108,b.y+g.pushY*108,SoftCue.animated(g,color));
            } else {
                float bx=g.dockX(b),by=g.dockY(b);
                if(!b.deliveryRecorded && g.berthEnabled(b.berth))ring(g,p,bx,by,48,1,Game.COLORS[b.berth],2);
                if(g.control==Game.ALIGNING || (g.control==Game.STEERING && !Physics.touching(g.tug,b,3))) {
                    path(p,g.tug.x,g.tug.y,g.guideX,g.guideY,AMBER);
                    ring(g,p,g.guideX,g.guideY,15,1,AMBER,2);
                    label(p,"APPROACH",g.guideX,g.guideY+26,AMBER);
                }
                if(g.control!=Game.DOCKING && g.control!=Game.WAITING_BRIDGE) arrow(g,p,b.x+g.pushX*46,b.y+g.pushY*46,
                        Physics.clamp(b.x+g.pushX*108,Physics.LEFT+5,Physics.RIGHT-5),Physics.clamp(b.y+g.pushY*108,g.channelTop()+5,Physics.BOTTOM-5),color);
                dragRing(g,p,PURPLE);
                if(g.control==Game.WAITING_BRIDGE) label(p,"WAIT FOR GREEN",b.x,b.y-67,AMBER);
                if(g.control==Game.DOCKING) {
                    ring(g,p,b.x,b.y,51,Math.min(1,b.dockTime/1.2f),GREEN,5);
                    label(p,"SETTLING",b.x,b.y-67,GREEN);
                }
            }
        }
        // Hull bloom identifies the tug; avoid a second permanent circle over it.

        if(g.control==Game.COASTING && !g.reversing && g.driveDirection()>=0) {
            float speed=g.tug.speed();
            if(speed>5) arrow(g,p,g.tug.x,g.tug.y,g.tug.x+g.tug.vx*.65f,g.tug.y+g.tug.vy*.65f,color);
        }
        if(g.noticeTicks>0 && (g.notice.startsWith("DELIVERED") || g.notice.startsWith("HARD IMPACT")
                || g.notice.startsWith("CONTRACT"))) {
            RiverCamera c=g.layout.camera;
            Painter canvas=p instanceof RiverPainter?((RiverPainter)p).canvas():p;
            Lettering.center(canvas,g.notice,g.layout.width/2,88,10,PAPER);
        }
        TowView.draw(g,p);
        if(!g.trainingChannel()) MooringView.draw(g,p);
    }
    private static void dragRing(Game g,Painter p,int color) {
        ring(g,p,g.downX,g.downY,42,1,0x99eee7d4,1.5f);
        SoftCue.dot(p,g.downX,g.downY,3,SoftCue.animated(g,PAPER));
        float dx=g.pointerX-g.downX,dy=g.pointerY-g.downY,len=Physics.length(dx,dy);
        float scale=Math.min(1,90/Math.max(1,len));
        if(len>8) {
            SoftCue.line(p,g.downX,g.downY,g.downX+dx*scale,g.downY+dy*scale,2,SoftCue.animated(g,color));
            SoftCue.dot(p,g.downX+dx*scale,g.downY+dy*scale,5,SoftCue.animated(g,color));
        }
    }
    private static void label(Painter p,String text,float x,float y,int color) {
        if(!(p instanceof RiverPainter))Lettering.center(p,text,x,y,10,color);
    }
}
