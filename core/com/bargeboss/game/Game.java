package com.bargeboss.game;

/** All gameplay, timers, gestures and lifecycle behavior live here, never in native hosts. */
public final class Game {
    public static final int TITLE=0, PLAY=1, PAUSE=2, FINISH=3, HELP=4;
    public static final int SHIFT_TICKS=180*60, QUOTA=6;
    // Two original-size motion steps per real tick: 2x travel/turning with the
    // same collision resolution, stopping distances and impact thresholds.
    // Gesture, contract and shift clocks stay at 60 real ticks per second.
    public static final int MOTION_STEPS=2, UNLOAD_TICKS=3*60;
    public static final float[] DOCK_X={230,370,230}, DOCK_Y={290,488,700};
    public static final float[] TIE_X={176,424,176};
    public static final float TIE_RADIUS=30;
    public static final int ROPE_FLIGHT_TICKS=18, ROPE_RETURN_TICKS=18, ROPE_HIT_TICKS=24;
    public int ropePhase,ropeTicks;
    public Body ropeBody;
    public float ropeStartX,ropeStartY,ropeEndX,ropeEndY;
    private boolean ropeTargeted;
    public static final int[] COLORS={0xffefa94a,0xff62c8bd,0xffde7965};
    public static final String[] CARGO_NAMES={"STEEL", "FREIGHT", "BULK"};
    public final Body[] cargo=new Body[9];
    public Body tug;
    public int mode=TITLE, ticks, score, delivered, lost, hull=100, selected=-1, combo;
    public int visualTicks;
    public int celebrationTicks, celebrationPoints, impactTicks;
    public float celebrationX,celebrationY,impactX,impactY;
    public int noticeTicks, damageCooldown, nextId=1;
    public String notice="WELCOME TO THE RIVER";
    public float targetX, targetY, pointerX, pointerY, downX, downY;
    public boolean pointer, pushing, navigating, ropeDragging;
    public static final int IDLE=0, MOVING=1, HOLDING=2, ALIGNING=3, PUSHING=4,
            STEERING=5, DOCKING=6, COASTING=7, BRAKING=8;
    public int control=IDLE;
    public boolean braking, manualSteering;
    public float guideX, guideY, pushX=-1, pushY;
    public Tutorial tutorial;
    public static final int HAPTIC_TICK=1, HAPTIC_CONTACT=2, HAPTIC_IMPACT=3, HAPTIC_SUCCESS=4;
    public boolean hapticsEnabled=true;
    private int hapticPending, hapticCooldown, contactCooldown, contactId=-1, mapPointer=-1;
    private boolean mapInRiver;
    private Game practice;
    private boolean returnToShift;
    private int pressTicks, held=-1, spawnIndex;
    private double accumulator, visualAccumulator;

    public Game() { reset(); mode=TITLE; }
    public Game activeGame() { return practice==null?this:practice; }
    public void startTutorial() {
        cancelInputs(); accumulator=0; visualAccumulator=0;
        returnToShift=mode!=TITLE && mode!=FINISH;
        practice=new Game(); practice.reset(); practice.tutorial=new Tutorial();
        practice.hapticsEnabled=hapticsEnabled;
        practice.tutorial.setup(practice);
        practice.tutorial.resumeShift=returnToShift;
    }
    public void render(Painter painter) { Renderer.draw(this,painter); }
    public String status() {
        if(practice!=null) return practice.status();
        String state=mode==PLAY?"Working":mode==PAUSE?"Paused":mode==HELP?"Help":mode==TITLE?"Ready":"Finished";
        String activity=ropePhase==1?"; rope flying":ropePhase==2?"; rope retracting":ropePhase==3?"; bollard hit":"";
        for(Body b:cargo) if(b!=null && b.active && b.unloadingTicks>0) activity="; unloading";
        for(Body b:cargo) if(canTie(b)) { activity="; ready to tie "+b.berth; break; }
        return state+"; delivered "+delivered+"; score "+score+(tutorial==null?"":"; lesson "+tutorial.lesson)
                +"; drag controls"+activity;
    }
    public void reset() {
        practice=null; tutorial=null; pushing=false; ropeDragging=false; braking=false; manualSteering=false; control=IDLE;
        mapPointer=-1; hapticPending=hapticCooldown=contactCooldown=0; contactId=-1;
        ticks=score=delivered=lost=combo=spawnIndex=0; hull=100; nextId=1;
        visualTicks=0; visualAccumulator=0; ropePhase=ropeTicks=0; ropeBody=null; ropeTargeted=false;
        celebrationTicks=impactTicks=0;
        damageCooldown=0; accumulator=0; selected=-1;
        tug=new Body(0,-1,376,700,(float)Math.PI);
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
    public void haptic(int event) {
        if(!hapticsEnabled) return;
        if(event>=HAPTIC_IMPACT || hapticCooldown==0) {
            hapticPending=Math.max(event,hapticPending); hapticCooldown=6;
        }
    }
    public int pollHaptic() {
        if(practice!=null) return practice.pollHaptic();
        int event=hapticPending; hapticPending=0; return event;
    }
    public void cancelInputs() {
        if(practice!=null) { practice.cancelInputs(); return; }
        cancel(); mapPointer=-1; hapticPending=0; braking=true; navigating=false;
    }
    /** Native input is always in landscape canvas coordinates. */
    public void pointerDown(int id,float x,float y) {
        if(practice!=null) { practice.pointerDown(id,x,y); finishPractice(); return; }
        if(mapPointer!=-1 || pointer || x<0 || x>Layout.WIDTH || y<0 || y>Layout.HEIGHT) return;
        boolean river=liveRiverInput() && RiverPainter.inBoard(x,y);
        if(river) beginDown(RiverPainter.riverX(x,y),RiverPainter.riverY(x,y),true);
        else beginDown(x,y,false);
        if(pointer) mapPointer=id;
    }
    public void pointerMove(int id,float x,float y) {
        if(practice!=null) { practice.pointerMove(id,x,y); return; }
        if(id!=mapPointer) return;
        if(mapInRiver) move(RiverPainter.riverX(x,y),RiverPainter.riverY(x,y)); else move(x,y);
    }
    public void pointerUp(int id,float x,float y) {
        if(practice!=null) { practice.pointerUp(id,x,y); finishPractice(); return; }
        if(id!=mapPointer) return;
        mapPointer=-1;
        if(mapInRiver) up(RiverPainter.riverX(x,y),RiverPainter.riverY(x,y)); else up(x,y);
    }
    private boolean liveRiverInput() { return mode==PLAY && (tutorial==null || tutorial.lesson!=Tutorial.DONE); }
    public void pointerCancel(int id) {
        if(practice!=null) { practice.pointerCancel(id); return; }
        if(id==mapPointer) { cancel(); mapPointer=-1; }
    }
    private void finishPractice() {
        if(practice==null || !practice.tutorial.exitRequested) return;
        boolean finished=practice.tutorial.lesson==Tutorial.DONE;
        hapticsEnabled=practice.hapticsEnabled; practice=null; accumulator=0;
        if(returnToShift) mode=PLAY; else if(finished) reset();
    }
    public void cancel() {
        if(practice!=null) { practice.cancel(); return; }
        if(pushing) { navigating=false; braking=false; control=COASTING; }
        pointer=false; mapPointer=-1; mapInRiver=false; pushing=false; ropeDragging=false; navigating=false; held=-1; pressTicks=0; manualSteering=false;
    }
    public boolean holdingTug() { return pointer && held==-2; }
    public boolean canTie(Body b) { return ropePhase==0 && tieEligible(b); }
    private boolean tieEligible(Body b) {
        return b!=null && b.active && !b.lineAttached && inDock(b) && b.speed()<24
                && (tutorial==null || tutorial.lesson>=Tutorial.TIE);
    }
    public boolean overTiePoint(Body b,float x,float y) {
        return Physics.length(x-TIE_X[b.berth],y-DOCK_Y[b.berth])<=TIE_RADIUS;
    }
    public float aimX(Body b,float x,float y) {
        return overTiePoint(b,x,y)?TIE_X[b.berth]:Physics.clamp(x,144,456);
    }
    public float aimY(Body b,float x,float y) {
        return overTiePoint(b,x,y)?DOCK_Y[b.berth]:Physics.clamp(y,168,816);
    }
    public Body heldCargo() { return held>=0 && cargo[held]!=null && cargo[held].active?cargo[held]:null; }
    public Body workingCargo() { return heldCargo(); }
    public float holdProgress() { return pointer && held!=-1?Math.min(1,pressTicks/12f):0; }
    public void background() { if(practice!=null) { practice.background(); return; } cancelInputs(); accumulator=0; visualAccumulator=0; if(mode==PLAY) mode=PAUSE; }
    public void pause() { if(practice!=null) { practice.pause(); return; } cancelInputs(); accumulator=0; visualAccumulator=0; if(mode==PLAY) mode=PAUSE; else if(mode==PAUSE) mode=PLAY; }
    /** Direct/replay input uses world coordinates on the board, canvas coordinates in menus. */
    public void down(float x,float y) {
        if(practice!=null) { practice.down(x,y); return; }
        boolean river=liveRiverInput() && x>=RiverPainter.WORLD_LEFT && x<=RiverPainter.WORLD_RIGHT
                && y>=RiverPainter.WORLD_TOP && y<=RiverPainter.WORLD_BOTTOM;
        beginDown(x,y,river);
    }
    private void beginDown(float x,float y,boolean river) {
        if(pointer) return;
        if(river) {
            if(x<RiverPainter.WORLD_LEFT || x>RiverPainter.WORLD_RIGHT || y<RiverPainter.WORLD_TOP || y>RiverPainter.WORLD_BOTTOM) return;
        } else if(x<0 || x>Layout.WIDTH || y<0 || y>Layout.HEIGHT) return;
        pointer=true; mapInRiver=river; pointerX=downX=x; pointerY=downY=y; pressTicks=0; held=-1;
        if(!river) return;
        if(tug.hit(x,y,8)) {
            held=-2; braking=true; control=HOLDING; haptic(HAPTIC_TICK); return;
        }
        for(int i=0;i<cargo.length;i++) if(cargo[i]!=null && cargo[i].active && cargo[i].hit(x,y,22)) {
            if(cargo[i].lineAttached || (ropePhase!=0 && cargo[i]==ropeBody)) { selected=i; return; }
            held=selected=i; navigating=false; braking=true; control=HOLDING;
            ropeDragging=canTie(cargo[i]); haptic(HAPTIC_TICK); return;
        }
    }
    public void move(float x,float y) {
        if(practice!=null) { practice.move(x,y); return; }
        if(pointer) {
            pointerX=x; pointerY=y;
            if(!mapInRiver) return;
            float distance=Physics.length(x-downX,y-downY);
            boolean wasSteering=manualSteering;
            if(distance>42) manualSteering=true;
            else if(distance<26) manualSteering=false;
            if(manualSteering!=wasSteering) haptic(HAPTIC_TICK);
        }
    }
    public void up(float x,float y) {
        if(practice!=null) {
            practice.up(x,y);
            finishPractice();
            return;
        }
        if(!pointer) return;
        if(ropeDragging) {
            Body b=heldCargo();
            if(b!=null && ropePhase==0) {
                ropeBody=b; ropePhase=1; ropeTicks=0;
                ropeStartX=b.x; ropeStartY=b.y; ropeTargeted=overTiePoint(b,x,y);
                ropeEndX=aimX(b,x,y); ropeEndY=aimY(b,x,y);
                haptic(HAPTIC_TICK);
            }
            cancel(); return;
        }
        // A world gesture never activates a screen-space button, even if its
        // rotated endpoint happens to share a button's numerical coordinates.
        if(mapInRiver) {
            if(!pushing && held>=0) tell("HOLD THE CARGO TO LINE UP AND PUSH");
            cancel(); return;
        }
        boolean tap=Physics.length(x-downX,y-downY)<22;
        if(tap && tapped(Layout.HAPTICS,x,y)) {
            hapticsEnabled=!hapticsEnabled; hapticPending=0; if(hapticsEnabled) haptic(HAPTIC_TICK);
        } else if(tutorial!=null && mode==PLAY && tutorial.lesson==Tutorial.DONE) {
            if(tap && tapped(Layout.START,x,y)) tutorial.exitRequested=true;
        } else if(mode==HELP) {
            if(tap && tapped(Layout.LEARN,x,y)) { startTutorial(); return; }
            if(tap && tapped(Layout.START,x,y)) mode=PLAY;
        } else if(mode==TITLE) {
            if(tap && tapped(Layout.LEARN,x,y)) { startTutorial(); return; }
            if(tap && tapped(Layout.START,x,y)) reset();
        } else if(mode==FINISH) {
            if(tap && tapped(Layout.AGAIN,x,y)) reset();
        } else if(mode==PAUSE) {
            if(tap && tapped(Layout.RESUME,x,y)) pause();
            else if(tap && tapped(Layout.RESTART,x,y)) {
                boolean training=tutorial!=null, resume=training && tutorial.resumeShift;
                reset();
                if(training) { tutorial=new Tutorial(); tutorial.resumeShift=resume; tutorial.setup(this); }
            }
        } else if(tap) {
            if(tapped(Layout.PAUSE,x,y)) pause();
            else if(tapped(Layout.BRAKE,x,y)) {
                cancelInputs(); control=BRAKING; targetX=tug.x; targetY=tug.y; haptic(HAPTIC_TICK);
            } else if(tapped(Layout.HELP,x,y)) {
                cancelInputs();
                if(tutorial!=null) tutorial.setup(this); else mode=HELP;
            } else if(tapped(Layout.PRACTICE,x,y)) {
                cancelInputs();
                if(tutorial!=null) tutorial.exitRequested=true;
                else { startTutorial(); return; }
            }
        }
        cancel();
    }
    private boolean tapped(Layout.Box box,float x,float y) {
        return box.contains(downX,downY) && box.contains(x,y);
    }
    /** Bounded catch-up protects against a stalled host. Hosts must cancel on background. */
    public void advance(double seconds) {
        if(practice!=null) { practice.advance(seconds); return; }
        if(Double.isNaN(seconds) || seconds<=0) return;
        if(mode!=PLAY) {
            accumulator=0;
            if(mode==TITLE || mode==FINISH) {
                visualAccumulator+=Math.min(.25,seconds);
                while(visualAccumulator+1e-10>=1.0/60.0) {
                    visualAccumulator-=1.0/60.0; visualTicks++;
                }
            } else visualAccumulator=0;
            return;
        }
        visualAccumulator=0;
        accumulator+=Math.min(.25,seconds);
        while(accumulator+1e-10>=1.0/60.0) { accumulator-=1.0/60.0; step(); if(mode!=PLAY) { accumulator=0; break; } }
    }
    public void step() {
        if(practice!=null) { practice.step(); return; }
        if(mode!=PLAY) return;
        visualTicks++;
        if(tutorial!=null && tutorial.lesson==Tutorial.DONE) {
            if(celebrationTicks>0) celebrationTicks--;
            if(impactTicks>0) impactTicks--;
            return;
        }
        ticks++; if(celebrationTicks>0) celebrationTicks--; if(impactTicks>0) impactTicks--;
        if(noticeTicks>0) noticeTicks--; if(damageCooldown>0) damageCooldown--;
        if(hapticCooldown>0) hapticCooldown--; if(contactCooldown>0) contactCooldown--;
        if(pointer) { pressTicks++; if(pressTicks>=12 && held!=-1 && !ropeDragging) { if(!pushing) haptic(HAPTIC_TICK); pushing=true; braking=false; } }
        for(int i=0;i<MOTION_STEPS;i++) motionStep();
        updateRope();
        for(int i=0;i<cargo.length;i++) {
            Body b=cargo[i]; if(b==null || !b.active) continue;
            if(b.lineSnapTicks>0) b.lineSnapTicks--;
            if(tutorial==null && !b.lineAttached) b.age+=Physics.DT;
            if(b.lineAttached && inDock(b) && b.speed()<24) b.dockTime+=Physics.DT;
            else { b.dockTime=0; b.lineAttached=false; }
            if(b.dockTime>=1.2f) {
                if(b.unloadingTicks==0) { b.lineSnapTicks=18; haptic(HAPTIC_CONTACT); }
                b.unloadingTicks++;
            }
            if(b.unloadingTicks>=UNLOAD_TICKS) {
                b.active=false; delivered++; combo++; int points=300+Math.max(0,180-(int)b.age)*2+Math.min(4,combo)*50;
                celebrationTicks=90; celebrationPoints=points; celebrationX=b.x; celebrationY=b.y;
                score+=points; tell("DELIVERED +"+points+" / "+delivered+" OF "+QUOTA);
                haptic(HAPTIC_SUCCESS);
                if(selected==i) selected=-1;
                if(held==i) { cancel(); navigating=false; }
            } else if(!b.lineAttached && b.age>68) {
                b.active=false; lost++; combo=0; score=Math.max(0,score-100); tell("CONTRACT EXPIRED -100");
                if(held==i) { cancel(); navigating=false; } if(selected==i) selected=-1;
            }
        }
        if(tutorial!=null) tutorial.update(this);
        else {
            if(ticks%1080==0 && ticks<SHIFT_TICKS-1800) spawn();
            if(ticks>=SHIFT_TICKS || hull<=0) { mode=FINISH; cancel(); }
        }
    }
    /** Rope flight, catch and recoil are real-time events, independent of 2x boat motion. */
    private void updateRope() {
        if(ropePhase==0 || ropeBody==null) return;
        ropeTicks++;
        if(ropePhase==1 && ropeTicks>=ROPE_FLIGHT_TICKS) {
            if(ropeTargeted && tieEligible(ropeBody)) {
                Body b=ropeBody;
                b.lineAttached=true; b.dockTime=0; b.tiedX=b.x; b.tiedY=b.y; b.tiedAngle=b.angle;
                b.vx=b.vy=b.spin=0; ropePhase=3; ropeTicks=0; haptic(HAPTIC_CONTACT);
            } else { ropePhase=2; ropeTicks=0; }
        } else if((ropePhase==2 && ropeTicks>=ROPE_RETURN_TICKS)
                || (ropePhase==3 && ropeTicks>=ROPE_HIT_TICKS)) {
            ropePhase=ropeTicks=0; ropeBody=null;
        }
    }
    private void motionStep() {
        if(pushing && held>=0 && cargo[held]!=null && cargo[held].active) pushCargo(cargo[held]);
        else if(pushing && held==-2) {
            float dx=pointerX-downX,dy=pointerY-downY,len=Physics.length(dx,dy);
            if(manualSteering) {
                navigating=true; braking=false; control=MOVING;
                targetX=tug.x+dx/len*200; targetY=tug.y+dy/len*200;
                navigate(targetX,targetY,88*Physics.clamp((len-26)/60,0,1));
            } else {
                navigating=false; braking=true; control=BRAKING;
                tug.vx*=.90f; tug.vy*=.90f; tug.spin*=.90f;
            }
        } else {
            float drag=braking?.90f:.985f;
            tug.vx*=drag; tug.vy*=drag; tug.spin*=drag;
            control=pointer && held!=-1?HOLDING:tug.speed()>3?(braking?BRAKING:COASTING):IDLE;
        }
        Physics.integrate(tug); damage(Physics.banks(tug));
        for(Body b:cargo) if(b!=null && b.active && !b.lineAttached) { Physics.integrate(b); damage(Physics.banks(b)); }
        for(int iteration=0;iteration<3;iteration++) {
            for(int i=0;i<cargo.length;i++) {
                Body a=cargo[i]; if(a==null || !a.active) continue;
                damage(Physics.collide(tug,a));
                for(int j=i+1;j<cargo.length;j++) if(cargo[j]!=null) damage(Physics.collide(a,cargo[j]));
            }
        }
        traffic();
        Body contact=null;
        for(Body b:cargo) if(b!=null && b.active && Physics.touching(tug,b,3)) { contact=b; break; }
        int currentContact=contact==null?-1:contact.id;
        if(currentContact!=-1 && currentContact!=contactId && contactCooldown==0) {
            haptic(HAPTIC_CONTACT); contactCooldown=45;
            impactX=(tug.x+contact.x)/2; impactY=(tug.y+contact.y)/2; impactTicks=30;
        }
        contactId=currentContact;
        for(Body b:cargo) if(b!=null && b.active && b.lineAttached) {
            b.x=b.tiedX; b.y=b.tiedY; b.angle=b.tiedAngle; b.vx=b.vy=b.spin=0;
        }
        // Fender damping follows motion time; mooring confirmation uses real time.
        for(Body b:cargo) if(b!=null && b.active) {
            if(inDock(b) && b.speed()<65) { b.vx*=.93f; b.vy*=.93f; b.spin*=.93f; }
        }
    }
    public boolean inDock(Body b) {
        for(int i=0;i<3;i++) if(Math.abs(b.discX(i)-DOCK_X[b.berth])+b.radius>55
                || Math.abs(b.discY(i)-DOCK_Y[b.berth])+b.radius>53) return false;
        return true;
    }
    private void spawn() {
        float[] ys={270,710,570,340,640,450};
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
        boolean steering=manualSteering;
        if(steering) { gx=b.x+dragX*2; gy=b.y+dragY*2; }
        float dx=gx-b.x, dy=gy-b.y, distance=Physics.length(dx,dy);
        pushX=dx/Math.max(1,distance); pushY=dy/Math.max(1,distance);
        if(distance<8 && !steering) {
            control=DOCKING;
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
            guideX=Physics.clamp(sx,198,402); guideY=Physics.clamp(sy,245,787);
            control=steering?STEERING:ALIGNING;
            navigate(guideX,guideY,95);
        } else {
            guideX=b.x-nx*reach; guideY=b.y-ny*reach;
            control=steering?STEERING:!Physics.touching(tug,b,3)?ALIGNING:inDock(b)?DOCKING:PUSHING;
            navigate(b.x+nx*26,b.y+ny*26,Math.min(76,18+distance*.85f));
            // A broad fender steadies the contact, while collisions still supply linear momentum.
            float heading=(float)StrictMath.atan2(ny,nx);
            b.spin+=Physics.clamp(Physics.angle(heading-b.angle)*2.5f-b.spin*2,-2,2)*Physics.DT;
            if(distance<43 && !steering) { b.vx*=.96f; b.vy*=.96f; }
        }
    }
    private void damage(float speed) {
        if(tutorial!=null) return;
        if(speed<78 || damageCooldown>0) return;
        hull=Math.max(0,hull-9); combo=0; damageCooldown=75; tell("HARD IMPACT - HULL -9");
        impactX=tug.x; impactY=tug.y; impactTicks=45;
        haptic(HAPTIC_IMPACT);
    }
    public int trafficPhase() { return ticks%(28*60); }
    public boolean trafficWarning() { return tutorial==null && ticks>=600 && trafficPhase()>=900 && trafficPhase()<1080; }
    public boolean trafficActive() { return tutorial==null && ticks>=600 && trafficPhase()>=1080 && trafficPhase()<1080+400/MOTION_STEPS; }
    public float trafficX() { return 110+(trafficPhase()-1080)*.95f*MOTION_STEPS; }
    private void traffic() {
        if(!trafficActive()) return;
        Body ship=new Body(-1,-1,trafficX(),395,0); ship.vx=57;
        float beforeX=ship.x, beforeY=ship.y;
        float hit=Physics.collide(ship,tug);
        for(Body b:cargo) if(b!=null && b.active) hit=Math.max(hit,Physics.collide(ship,b));
        if(hit>25 || Math.abs(ship.x-beforeX)+Math.abs(ship.y-beforeY)>2) damage(90);
    }
}
