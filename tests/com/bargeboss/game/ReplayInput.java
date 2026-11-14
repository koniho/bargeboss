package com.bargeboss.game;

/** Deterministic user gestures shared by tests and real-renderer captures. */
public final class ReplayInput {
    private ReplayInput() {}
    /** Stable local cargo fixture for isolated control/art tests, separate from feeder replays. */
    public static void controlFixture(Game g) {
        g.challengeId=2;g.reset(); g.tug=new Body(0,-1,376,700,(float)Math.PI); for(Feeder f:g.feeders) { f.phase=Feeder.IDLE; f.load=null; }
        g.addCargo(2,292,700); g.addCargo(1,320,480);
    }
    public static void tap(Game g,float x,float y) { g.pointerDown(0,x,y); g.pointerUp(0,x,y); }
    public static void tap(Game g,Layout.Box box) { box=g.activeGame().controlBox(box); tap(g,box.centerX(),box.centerY()); }
    public static void down(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerDown(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void move(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerMove(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void up(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerUp(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void releaseTow(Game root) {
        Body b=root.activeGame().tow.cargo;
        if(b!=null) { down(root,b.x,b.y);up(root,b.x,b.y); }
    }
    public static void lasso(Game root,Body b) {
        Game g=root.activeGame();
        down(root,b.x,b.y); move(root,g.bollardX(b.berth),g.bollardY(b.berth));
        up(root,g.bollardX(b.berth),g.bollardY(b.berth));
    }
    public static void work(Game g) {
        if(g.ropePhase!=0) return;
        // A covered or drifting hull can make a press miss; lift before retrying.
        if(g.pointer && g.heldCargo()==null && !g.holdingTug()) up(g,g.pointerX,g.pointerY);
        Body target=null;
        for(Body b:g.cargo) if(b!=null && b.active && !b.lineAttached) { target=b; break; }
        if(target!=null && (!g.pointer || g.holdingTug()) && !g.layout.camera.contains(g.layout.camera.screenX(target.x,target.y),g.layout.camera.screenY(target.x,target.y))) {
            drive(g,300,target.y);return;
        }
        if(target!=null && g.holdingTug() && Math.abs(target.y-g.tug.y)>220) { drive(g,300,target.y);return; }
        if(target!=null) {
            if(g.holdingTug()) up(g,g.pointerX,g.pointerY);
            if(g.pointer && g.canTie(g.heldCargo())) up(g,g.pointerX,g.pointerY);
            if(!g.pointer) { if(g.canTie(target)) lasso(g,target); else down(g,target.x,target.y); }
        } else {
            // Clear the arrival channel while the crew works, using the same drag gesture.
            float dx=300-g.tug.x,dy=610-g.tug.y,len=Physics.length(dx,dy);
            if(len<22) {
                if(g.pointer) up(g,g.pointerX,g.pointerY);
            } else {
                if(!g.pointer) down(g,g.tug.x,g.tug.y);
                move(g,g.downX+dx/len*90,g.downY+dy/len*90);
            }
        }
    }
    private static void drive(Game g,float x,float y) {
        if(g.bridgeStage() && !g.bridge.passable() && g.bridge.separates(g.tug.y,y)) y=Bridge.Y+(g.tug.y>Bridge.Y?90:-90);
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Physics.length(dx,dy);
        if(!g.pointer) down(g,g.tug.x,g.tug.y);
        if(len<25) move(g,g.downX,g.downY);
        else move(g,g.downX+dx/len*90,g.downY+dy/len*90);
    }
    public static void channel(Game root) {
        Game g=root.activeGame();
        float[] target=new float[2];TrainingChannel.guide(g.tug.x,g.tug.y,g.tutorial.channelGate,target);
        float dx=target[0]-g.tug.x,dy=target[1]-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
        if(!g.pointer) down(root,g.tug.x,g.tug.y);
        move(root,g.downX+dx/len*55,g.downY+dy/len*55);
    }
    public static void pushPractice(Game root) {
        Game g=root.activeGame();Body b=g.cargo[0];
        // The canal now opens into this same harbor. Drive through its mouth
        // before touching the load, instead of selecting an off-screen hull.
        if(g.tug.x<215) { drive(g,240,500);return; }
        if(Physics.length(g.tug.x-b.x,g.tug.y-b.y)>190) { drive(g,360,620);return; }
        if(g.holdingTug()) up(root,g.pointerX,g.pointerY);
        if(!g.pointer) down(root,b.x,b.y);
        float dx=Tutorial.PUSH_X-b.x,dy=Tutorial.PUSH_Y-b.y,len=Math.max(1,Physics.length(dx,dy));
        move(root,g.downX+90*dx/len,g.downY+90*dy/len);
    }
    public static void tutorial(Game root) {
        Game g=root.activeGame();
        switch(g.tutorial.lesson) {
            case Tutorial.MOVE:
                if(!g.pointer) down(root,g.tug.x,g.tug.y);
                move(root,g.downX+100,g.downY); break;
            case Tutorial.COAST:
                if(g.pointer) up(root,g.pointerX,g.pointerY);
                break;
            case Tutorial.CHANNEL:
                channel(root);break;
            case Tutorial.HOLD:
                pushPractice(root);break;
            case Tutorial.STEER:
                if(!g.pointer) down(root,g.cargo[0].x,g.cargo[0].y);
                move(root,g.downX,g.downY); break;
            case Tutorial.DOCK:
                if(!g.pointer) down(root,g.cargo[0].x,g.cargo[0].y); break;
            case Tutorial.TIE:
                if(!g.cargo[0].lineAttached && g.ropePhase==0) lasso(root,g.cargo[0]); break;
            case Tutorial.CONNECT:
                if(g.ropePhase==0 && g.tow.cargo==null) {
                    Body b=g.cargo[0];
                    // Circle to the upstream side of the waiting barge. The
                    // subsequent northbound drag should pull, not ram its bow.
                    if(Physics.length(g.tug.x-b.x,g.tug.y-b.y)>150 || g.tug.y>b.y-55) {
                        drive(g,b.x-95,b.y-85);break;
                    }
                    if(g.pointer) up(root,g.pointerX,g.pointerY);
                    down(root,g.cargo[0].x,g.cargo[0].y);move(root,g.tug.x,g.tug.y);up(root,g.tug.x,g.tug.y);
                } break;
            case Tutorial.PULL:
                if(!g.pointer) down(root,g.tug.x,g.tug.y);
                move(root,g.downX,g.downY-100);break;
            case Tutorial.RELEASE:
                if(g.pointer) up(root,g.pointerX,g.pointerY);
                releaseTow(root);break;
            default: break;
        }
    }
}
