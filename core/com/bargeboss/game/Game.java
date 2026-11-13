package com.bargeboss.game;

/** All gameplay, timers, gestures and lifecycle behavior live here, never in native hosts. */
public final class Game {
    public static final int TITLE=0, PLAY=1, PAUSE=2, FINISH=3, HELP=4;
    public static final int SHIFT_TICKS=180*60, QUOTA=6;
    public static final float[] DOCK_X={178,422,178}, DOCK_Y={290,488,700};
    public static final int[] COLORS={0xffefa94a,0xff62c8bd,0xffde7965};
    public static final String[] CARGO_NAMES={"STEEL", "FREIGHT", "BULK"};
    public final Body[] cargo=new Body[9];
    public Body tug;
    public int mode=TITLE, ticks, score, delivered, lost, hull=100, selected=-1, combo;
    public int noticeTicks, damageCooldown, nextId=1;
    public String notice="WELCOME TO THE RIVER";
    public float targetX, targetY, pointerX, pointerY, downX, downY;
    public boolean pointer, pushing, navigating;
    private int pressTicks, held=-1, spawnIndex;
    private double accumulator;

    public Game() { reset(); mode=TITLE; }
    public void render(Painter painter) { Renderer.draw(this,painter); }
    public String status() {
        String state=mode==PLAY?"Working":mode==PAUSE?"Paused":mode==HELP?"Help":mode==TITLE?"Ready":"Finished";
        return state+"; delivered "+delivered+"; score "+score;
    }
    public void reset() {
        ticks=score=delivered=lost=combo=spawnIndex=0; hull=100; nextId=1;
        damageCooldown=0; accumulator=0; selected=-1;
        tug=new Body(0,-1,376,739,(float)Math.PI);
        for(int i=0;i<cargo.length;i++) cargo[i]=null;
        addCargo(2,292,700); addCargo(1,320,480);
        targetX=tug.x; targetY=tug.y; navigating=false; cancel();
        mode=PLAY; tell("HOLD BULK 01 TO PUSH INTO BERTH C");
    }
    void addCargo(int berth, float x, float y) {
        for(int i=0;i<cargo.length;i++) if(cargo[i]==null || !cargo[i].active) {
            cargo[i]=new Body(nextId++,berth,x,y,berth==1?0:(float)Math.PI); return;
        }
    }
    public void tell(String message) { notice=message; noticeTicks=240; }
    public void cancel() { pointer=false; pushing=false; held=-1; pressTicks=0; }
    public void background() { cancel(); accumulator=0; if(mode==PLAY) mode=PAUSE; }
    public void pause() { cancel(); accumulator=0; if(mode==PLAY) mode=PAUSE; else if(mode==PAUSE) mode=PLAY; }
    public void down(float x,float y) {
        if(pointer || x<0 || x>600 || y<0 || y>960) return;
        pointer=true; pointerX=downX=x; pointerY=downY=y; pressTicks=0; held=-1;
        if(mode!=PLAY) return;
        for(int i=0;i<cargo.length;i++) if(cargo[i]!=null && cargo[i].active && cargo[i].hit(x,y,14)) {
            held=selected=i; return;
        }
        if(tug.hit(x,y,16)) held=-2;
    }
    public void move(float x,float y) { if(pointer) { pointerX=x; pointerY=y; } }
    public void up(float x,float y) {
        if(!pointer) return;
        boolean tap=!pushing && Physics.length(x-downX,y-downY)<22;
        if(mode==HELP) {
            if(tap && x>=100 && x<=500 && y>=650 && y<=726) mode=PLAY;
        } else if(mode==TITLE || mode==FINISH) {
            if(tap && x>=100 && x<=500 && y>=650 && y<=726) reset();
        } else if(mode==PAUSE) {
            if(tap && x>=100 && x<=500 && y>=540 && y<=612) pause();
            else if(tap && x>=100 && x<=500 && y>=632 && y<=704) reset();
        } else if(tap) {
            if(x>=508 && y<=100) pause();
            else if(y>=872 && y<=940 && x>=30 && x<=198) {
                navigating=false; targetX=tug.x; targetY=tug.y; tell("ENGINES REVERSE - BRAKING");
            } else if(y>=872 && y<=940 && x>=220 && x<=388) {
                background();
            } else if(y>=872 && y<=940 && x>=410) { mode=HELP; cancel(); }
            else if(held==-1 && y>=215 && y<=817 && x>=110 && x<=490) {
                targetX=Physics.clamp(x,140,460); targetY=Physics.clamp(y,245,787); navigating=true;
            } else if(held>=0) tell("HOLD THE CARGO TO LINE UP AND PUSH");
        }
        cancel();
    }
    /** Bounded catch-up protects against a stalled host. Hosts must cancel on background. */
    public void advance(double seconds) {
        if(mode!=PLAY) { accumulator=0; return; }
        if(Double.isNaN(seconds) || seconds<=0) return;
        accumulator+=Math.min(.25,seconds);
        while(accumulator+1e-10>=1.0/60.0) { accumulator-=1.0/60.0; step(); if(mode!=PLAY) { accumulator=0; break; } }
    }
    public void step() {
        if(mode!=PLAY) return;
        ticks++; if(noticeTicks>0) noticeTicks--; if(damageCooldown>0) damageCooldown--;
        if(pointer) { pressTicks++; if(pressTicks>=12 && held!=-1) pushing=true; }
        if(pushing && held>=0 && cargo[held]!=null && cargo[held].active) pushCargo(cargo[held]);
        else if(navigating) navigate(targetX,targetY,pushing?125:88);
        else { tug.vx*=.94f; tug.vy*=.94f; tug.spin*=.94f; }
        Physics.integrate(tug); damage(Physics.banks(tug));
        for(Body b:cargo) if(b!=null && b.active) { b.age+=Physics.DT; Physics.integrate(b); damage(Physics.banks(b)); }
        for(int iteration=0;iteration<3;iteration++) {
            for(int i=0;i<cargo.length;i++) {
                Body a=cargo[i]; if(a==null || !a.active) continue;
                damage(Physics.collide(tug,a));
                for(int j=i+1;j<cargo.length;j++) if(cargo[j]!=null) damage(Physics.collide(a,cargo[j]));
            }
        }
        traffic();
        for(int i=0;i<cargo.length;i++) {
            Body b=cargo[i]; if(b==null || !b.active) continue;
            if(inDock(b) && b.speed()<24) b.dockTime+=Physics.DT; else b.dockTime=0;
            if(b.dockTime>=1.2f) {
                b.active=false; delivered++; combo++; int points=300+Math.max(0,180-(int)b.age)*2+Math.min(4,combo)*50;
                score+=points; tell("DELIVERED +"+points+" / "+delivered+" OF "+QUOTA);
                if(selected==i) selected=-1;
                if(held==i) { cancel(); navigating=false; }
            } else if(b.age>68) {
                b.active=false; lost++; combo=0; score=Math.max(0,score-100); tell("CONTRACT EXPIRED -100");
                if(held==i) { cancel(); navigating=false; } if(selected==i) selected=-1;
            }
        }
        if(ticks%1080==0 && ticks<SHIFT_TICKS-1800) spawn();
        if(ticks>=SHIFT_TICKS || hull<=0) { mode=FINISH; cancel(); }
    }
    public boolean inDock(Body b) {
        for(int i=0;i<3;i++) if(Math.abs(b.discX(i)-DOCK_X[b.berth])+b.radius>55
                || Math.abs(b.discY(i)-DOCK_Y[b.berth])+b.radius>53) return false;
        return true;
    }
    private void spawn() {
        float[] ys={270,748,570,340,640,450};
        float y=ys[spawnIndex%ys.length];
        boolean clear=!tug.hit(302,y,55);
        for(Body b:cargo) if(b!=null && b.active && b.hit(302,y,55)) clear=false;
        if(clear) { addCargo(spawnIndex%3,302,y); spawnIndex++; tell("INBOUND CARGO - NEW CONTRACT"); }
        else tell("ENTRY BLOCKED - CLEAR THE CENTER CHANNEL");
    }
    private void navigate(float x,float y,float topSpeed) {
        float dx=x-tug.x, dy=y-tug.y, distance=Physics.length(dx,dy);
        if(distance<1) { tug.vx*=.93f; tug.vy*=.93f; return; }
        float desired=(float)StrictMath.atan2(dy,dx), error=Physics.angle(desired-tug.angle);
        tug.spin+=Physics.clamp(error*6-tug.spin*3,-7,7)*Physics.DT;
        float speed=Math.min(topSpeed,(float)StrictMath.sqrt(distance*100));
        speed*=Math.max(.1f,1-Math.abs(error)*.45f);
        float dvx=dx/distance*speed-tug.vx, dvy=dy/distance*speed-tug.vy;
        float force=Physics.length(dvx,dvy), maxChange=82*Physics.DT;
        if(force>maxChange) { dvx*=maxChange/force; dvy*=maxChange/force; }
        tug.vx+=dvx; tug.vy+=dvy;
    }
    private void pushCargo(Body b) {
        float gx=DOCK_X[b.berth], gy=DOCK_Y[b.berth];
        // Drag from the original contact point to override the assisted berth heading.
        float dragX=pointerX-downX, dragY=pointerY-downY;
        boolean steering=Physics.length(dragX,dragY)>38;
        if(steering) { gx=b.x+dragX*2; gy=b.y+dragY*2; }
        float dx=gx-b.x, dy=gy-b.y, distance=Physics.length(dx,dy);
        if(distance<8 && !steering) {
            // Mooring assist only inside the correct berth; it never transports cargo.
            b.vx*=.90f; b.vy*=.90f; b.spin*=.9f;
            tug.vx*=.90f; tug.vy*=.90f; return;
        }
        float nx=dx/Math.max(1,distance), ny=dy/Math.max(1,distance);
        float along=(tug.x-b.x)*nx+(tug.y-b.y)*ny;
        float side=(tug.x-b.x)*(-ny)+(tug.y-b.y)*nx;
        float reach=b.half+b.radius+tug.half+tug.radius-2;
        if(along>-reach*.6f || Math.abs(side)>24) {
            // Route around a hull before approaching its stern; never push from the wrong side.
            float sideSign=side>=0?1:-1;
            float sx=b.x-nx*(reach+14), sy=b.y-ny*(reach+14);
            if(along>-20) { sx-=ny*sideSign*78; sy+=nx*sideSign*78; }
            navigate(Physics.clamp(sx,138,462),Physics.clamp(sy,245,787),95);
        } else {
            navigate(b.x+nx*26,b.y+ny*26,Math.min(76,18+distance*.85f));
            // A broad fender steadies the contact, while collisions still supply linear momentum.
            float heading=(float)StrictMath.atan2(ny,nx);
            b.spin+=Physics.clamp(Physics.angle(heading-b.angle)*2.5f-b.spin*2,-2,2)*Physics.DT;
            if(distance<43 && !steering) { b.vx*=.96f; b.vy*=.96f; }
        }
    }
    private void damage(float speed) {
        if(speed<78 || damageCooldown>0) return;
        hull=Math.max(0,hull-9); combo=0; damageCooldown=75; tell("HARD IMPACT - HULL -9");
    }
    public int trafficPhase() { return ticks%(28*60); }
    public boolean trafficWarning() { return ticks>=600 && trafficPhase()>=900 && trafficPhase()<1080; }
    public boolean trafficActive() { return ticks>=600 && trafficPhase()>=1080 && trafficPhase()<1560; }
    public float trafficX() { return 75+(trafficPhase()-1080)*.95f; }
    private void traffic() {
        if(!trafficActive()) return;
        Body ship=new Body(-1,-1,trafficX(),395,0); ship.vx=57;
        float beforeX=ship.x, beforeY=ship.y;
        float hit=Physics.collide(ship,tug);
        for(Body b:cargo) if(b!=null && b.active) hit=Math.max(hit,Physics.collide(ship,b));
        if(hit>25 || Math.abs(ship.x-beforeX)+Math.abs(ship.y-beforeY)>2) damage(90);
    }
}
