package com.bargeboss.game;

/** Deterministic user gestures shared by tests and real-renderer captures. */
public final class ReplayInput {
    private ReplayInput() {}
    public static void tap(Game g,float x,float y) { g.pointerDown(0,x,y); g.pointerUp(0,x,y); }
    public static void tap(Game g,Layout.Box box) { tap(g,box.centerX(),box.centerY()); }
    public static void down(Game g,float x,float y) { g.pointerDown(0,RiverPainter.screenX(x,y),RiverPainter.screenY(x,y)); }
    public static void move(Game g,float x,float y) { g.pointerMove(0,RiverPainter.screenX(x,y),RiverPainter.screenY(x,y)); }
    public static void up(Game g,float x,float y) { g.pointerUp(0,RiverPainter.screenX(x,y),RiverPainter.screenY(x,y)); }
    public static void lasso(Game root,Body b) {
        down(root,b.x,b.y); move(root,Game.TIE_X[b.berth],Game.DOCK_Y[b.berth]);
        up(root,Game.TIE_X[b.berth],Game.DOCK_Y[b.berth]);
    }
    public static void work(Game g) {
        if(g.ropePhase!=0) return;
        Body target=null;
        for(Body b:g.cargo) if(b!=null && b.active && !b.lineAttached) { target=b; break; }
        if(target!=null) {
            if(g.holdingTug()) up(g,g.pointerX,g.pointerY);
            if(g.pointer && g.canTie(g.heldCargo())) up(g,g.pointerX,g.pointerY);
            if(!g.pointer) { if(g.canTie(target)) lasso(g,target); else down(g,target.x,target.y); }
        } else {
            // Clear the arrival channel while the crew works, using the same drag gesture.
            float dx=398-g.tug.x,dy=610-g.tug.y,len=Physics.length(dx,dy);
            if(len<22) {
                if(g.pointer) up(g,g.pointerX,g.pointerY);
                if(!g.braking) tap(g,Layout.BRAKE);
            } else {
                if(!g.pointer) down(g,g.tug.x,g.tug.y);
                move(g,g.downX+dx/len*90,g.downY+dy/len*90);
            }
        }
    }
    public static void tutorial(Game root) {
        Game g=root.activeGame();
        switch(g.tutorial.lesson) {
            case Tutorial.MOVE:
                if(!g.pointer) down(root,g.tug.x,g.tug.y);
                move(root,g.downX,g.downY-100); break;
            case Tutorial.STOP:
                if(g.pointer) up(root,g.pointerX,g.pointerY);
                if(!g.braking) tap(root,Layout.BRAKE); break;
            case Tutorial.HOLD:
                if(!g.pointer) down(root,g.cargo[0].x,g.cargo[0].y); break;
            case Tutorial.STEER:
                if(!g.pointer) down(root,g.cargo[0].x,g.cargo[0].y);
                move(root,g.downX-65,g.downY-65); break;
            case Tutorial.DOCK:
                if(!g.pointer) down(root,g.cargo[0].x,g.cargo[0].y); break;
            case Tutorial.TIE:
                if(!g.cargo[0].lineAttached && g.ropePhase==0) lasso(root,g.cargo[0]); break;
            default: break;
        }
    }
}
