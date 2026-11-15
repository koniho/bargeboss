package com.bargeboss.game;

/** A tension-only spring between deck fittings, advanced at the shared motion rate. */
public final class Towline {
    public static final int CAPACITY=9;
    private final Towline[] additional=new Towline[CAPACITY-1];
    public Body cargo,leader;
    public float leaderEnd;
    public Towline link(int index){return index==0?this:additional[index-1];}
    public Towline incoming(Body b){if(b==null)return null;for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo==b)return l;}return null;}
    public Body primary(){for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null)return l.cargo;}return null;}
    public int count(){int n=0;for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null)n++;}return n;}
    public boolean connected(Body b,Body tug){for(int i=0;i<CAPACITY;i++){if(b==tug)return true;Towline l=incoming(b);if(l==null)return false;b=l.leader;}return b==tug;}
    public int loadCount(Body tug){int n=0;for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null && connected(l.cargo,tug))n++;}return n;}
    public float loadMass(Body tug){float m=0;for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null && connected(l.cargo,tug))m+=l.cargo.mass;}return m;}
    public float speedFactor(Body tug){return 1/(1+.075f*loadMass(tug));}
    public float pullLoad(Body tug){float sum=0;for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null && l.leader==tug)sum+=l.tension;}return sum;}
    public boolean canAttach(Game g,Body b,Body parent) {
        if(b==null || parent==null || b==parent || b.id<=0 || !b.active || b.lineAttached && !b.deliveryRecorded || incoming(b)!=null || count()>=CAPACITY)return false;
        if(parent!=g.tug && (parent.id<=0 || !parent.active || parent.lineAttached))return false;
        for(Body cursor=parent;cursor!=null;) {if(cursor==b)return false;Towline l=incoming(cursor);cursor=l==null?null:l.leader;}
        return true;
    }
    public void detach(Game g,Body b){Towline l=incoming(b);if(l!=null)l.releaseOne(g);}
    public void detachBody(Game g,Body b){for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null && (l.cargo==b || l.leader==b))l.releaseOne(g);}}
    public void prune(Game g){for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null && l.cargo!=null && (!l.cargo.active || l.cargo.lineAttached || l.leader==null || !l.leader.active || l.leader.lineAttached))l.releaseOne(g);}}

    public float length,tension,cargoEnd;
    public int catchTicks,releaseTicks;
    public float releaseX,releaseY,releaseToX,releaseToY;
    public float tugX(Body tug) { Body t=leader==null?tug:leader;return t.x+(leader==null?-(t.half+7):leaderEnd)*(float)StrictMath.cos(t.angle); }
    public float tugY(Body tug) { Body t=leader==null?tug:leader;return t.y+(leader==null?-(t.half+7):leaderEnd)*(float)StrictMath.sin(t.angle); }
    public float cargoX() { return cargo.x+cargoEnd*(float)StrictMath.cos(cargo.angle); }
    public float cargoY() { return cargo.y+cargoEnd*(float)StrictMath.sin(cargo.angle); }
    public static boolean clear(Game g,float x,float y,float xx,float yy) {
        if(g.hasBridge() && !g.bridge.passable() && g.bridge.crosses(x,y,xx,yy))return false;
        if(g.tutorial!=null) {
            int steps=Math.max(1,(int)(Physics.length(xx-x,yy-y)/4));
            for(int i=0;i<=steps;i++) if(!TrainingChannel.contains(x+(xx-x)*i/steps,y+(yy-y)*i/steps)) return false;
            return true;
        }
        return !(g.lockStage() && g.lock.blocksLine(x,y,xx,yy))
                && (!g.orderStage() || PhysicalBerths.clear(g.challengeId,x,y,xx,yy,3));
    }
    public void attach(Game g,Body b) {attach(g,b,g.tug);}
    public boolean attach(Game g,Body b,Body parent) {
        if(!canAttach(g,b,parent))return false;
        Towline slot=null;
        for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l==null){additional[i-1]=l=new Towline();}if(l.cargo==null){slot=l;break;}}
        if(b.deliveryRecorded){b.lineAttached=false;b.dockTime=0;b.lineSnapTicks=0;}
        slot.cargo=b;slot.leader=parent;b.awaitingPickup=false;parent.awaitingPickup=false;
        float dot=(parent.x-b.x)*(float)StrictMath.cos(b.angle)+(parent.y-b.y)*(float)StrictMath.sin(b.angle);
        slot.cargoEnd=(dot>=0?1:-1)*(b.half+7);
        Towline parentLine=incoming(parent);
        float parentDot=(b.x-parent.x)*(float)StrictMath.cos(parent.angle)+(b.y-parent.y)*(float)StrictMath.sin(parent.angle);
        slot.leaderEnd=parent==g.tug?-(parent.half+7):parentLine!=null?-parentLine.cargoEnd:(parentDot>=0?1:-1)*(parent.half+7);
        slot.length=Physics.clamp(Physics.length(slot.tugX(g.tug)-slot.cargoX(),slot.tugY(g.tug)-slot.cargoY()),32,85);
        slot.tension=0;slot.catchTicks=24;slot.releaseTicks=0;
        g.tell(parent==g.tug?"TOWLINE CAUGHT / DRAG TUG TO PULL":"BARGES LINKED / DRAG TUG TO PULL THE TRAIN");return true;
    }
    public void release(Game g) {for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null)l.releaseOne(g);}}
    private void releaseOne(Game g) {
        if(cargo==null) return;
        releaseX=cargoX();releaseY=cargoY();releaseToX=tugX(g.tug);releaseToY=tugY(g.tug);
        releaseTicks=18;cargo=null;tension=0;catchTicks=0;
        g.haptic(Game.HAPTIC_TICK);
    }
    public void tick(Game g) {
        for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null){if(l.catchTicks>0)l.catchTicks--;if(l.releaseTicks>0)l.releaseTicks--;}}
        prune(g);
    }
    public void step(Game g) {for(int i=0;i<CAPACITY;i++){Towline l=link(i);if(l!=null)l.stepOne(g);}}
    private void stepOne(Game g) {
        if(cargo==null)return;
        Body b=cargo,t=leader;
        if(!b.active || b.lineAttached || b.awaitingPickup || t==null || !t.active || t.lineAttached || t.awaitingPickup){releaseOne(g);return;}
        float ax=cargoX(),ay=cargoY(),bx=tugX(t),by=tugY(t);
        float dx=bx-ax,dy=by-ay,distance=Physics.length(dx,dy);
        if(distance>length+100 || !clear(g,ax,ay,bx,by)) {
            releaseOne(g);g.tell("TOWLINE RELEASED / GET CLOSER AND RECONNECT");return;
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
