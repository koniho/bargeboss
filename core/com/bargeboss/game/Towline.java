package com.bargeboss.game;

/** A tension-only spring between deck fittings, advanced at the shared motion rate. */
public final class Towline {
    public Body cargo;
    public float length,tension,cargoEnd;
    public int catchTicks,releaseTicks;
    public float releaseX,releaseY,releaseToX,releaseToY;
    public float tugX(Body tug) { return tug.x-(tug.half+7)*(float)StrictMath.cos(tug.angle); }
    public float tugY(Body tug) { return tug.y-(tug.half+7)*(float)StrictMath.sin(tug.angle); }
    public float cargoX() { return cargo.x+cargoEnd*(float)StrictMath.cos(cargo.angle); }
    public float cargoY() { return cargo.y+cargoEnd*(float)StrictMath.sin(cargo.angle); }
    public static boolean clear(Game g,float x,float y,float xx,float yy) {
        if(g.tutorial!=null) {
            int steps=Math.max(1,(int)(Physics.length(xx-x,yy-y)/4));
            for(int i=0;i<=steps;i++) if(!TrainingChannel.contains(x+(xx-x)*i/steps,y+(yy-y)*i/steps)) return false;
            return true;
        }
        return !(g.bridgeStage() && !g.bridge.passable() && g.bridge.separates(y,yy))
                && (g.challengeId!=2 || HarborGeometry.clear(x,y,xx,yy,3));
    }
    public void attach(Game g,Body b) {
        cargo=b;b.awaitingPickup=false;
        float dot=(g.tug.x-b.x)*(float)StrictMath.cos(b.angle)+(g.tug.y-b.y)*(float)StrictMath.sin(b.angle);
        cargoEnd=(dot>=0?1:-1)*(b.half+7);
        length=Physics.clamp(Physics.length(tugX(g.tug)-cargoX(),tugY(g.tug)-cargoY()),32,85);
        tension=0;catchTicks=24;releaseTicks=0;
        g.tell("TOWLINE CAUGHT / DRAG TUG TO PULL");
    }
    public void release(Game g) {
        if(cargo==null) return;
        releaseX=cargoX();releaseY=cargoY();releaseToX=tugX(g.tug);releaseToY=tugY(g.tug);
        releaseTicks=18;cargo=null;tension=0;catchTicks=0;
        g.haptic(Game.HAPTIC_TICK);
    }
    public void tick(Game g) {
        if(catchTicks>0) catchTicks--;
        if(releaseTicks>0) releaseTicks--;
        if(cargo!=null && (!cargo.active || cargo.lineAttached)) release(g);
    }
    public void step(Game g) {
        if(cargo==null) return;
        Body b=cargo,t=g.tug;
        if(!b.active || b.lineAttached || b.awaitingPickup) { release(g);return; }
        float ax=cargoX(),ay=cargoY(),bx=tugX(t),by=tugY(t);
        float dx=bx-ax,dy=by-ay,distance=Physics.length(dx,dy);
        if(distance>length+100 || !clear(g,ax,ay,bx,by)) {
            release(g);g.tell("TOWLINE RELEASED / GET CLOSER AND RECONNECT");return;
        }
        tension=0;
        if(distance<=length || distance<1) return; // Slack can never push a hull.
        dx/=distance;dy/=distance;
        float arx=ax-b.x,ary=ay-b.y,brx=bx-t.x,bry=by-t.y;
        float relative=(t.vx-t.spin*bry-b.vx+b.spin*ary)*dx
                +(t.vy+t.spin*brx-b.vy-b.spin*arx)*dy;
        tension=Physics.clamp((distance-length)*14+relative*8,0,240);
        float ix=dx*tension*Physics.DT,iy=dy*tension*Physics.DT;
        b.vx+=ix/b.mass;b.vy+=iy/b.mass;b.spin+=(arx*iy-ary*ix)/b.inertia;
        t.vx-=ix/t.mass;t.vy-=iy/t.mass;t.spin-=(brx*iy-bry*ix)/t.inertia;
    }
}
