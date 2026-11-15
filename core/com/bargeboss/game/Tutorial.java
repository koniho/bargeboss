package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, COAST=1, CHANNEL=2, HOLD=3, STEER=4, DOCK=5, TIE=6, CONNECT=7, PULL=8, DOUBLE=9, BRIDGE=10, LINK=11, TRAIN=12, RELEASE=13, DONE=14;
    public static final float TOW_BERTH_X=245,TOW_BERTH_Y=310,TOW_BERTH_RADIUS=58;
    public static final float BRIDGE_CLEAR_Y=Bridge.Y-35,TRAIN_CLEAR_Y=TrainingChannel.NARROW_EXIT-15;
    public int bridgeCrossed,trainCrossed;
    private final float[] previousTowY=new float[2];
    private int reconfigureRelease;
    public static final float PUSH_X=240,PUSH_Y=600,PUSH_RADIUS=24;
    public static final float START_X=-1740,START_Y=650,DRIVE_RADIUS=90;
    public static final float ORBIT_INNER=50,ORBIT_OUTER=150,ORBIT_TURN=(float)(2*StrictMath.PI);
    public boolean orbitArmed;
    public float orbitSweep,orbitAngle;
    private float orbitPreviousX,orbitPreviousY;
    public static final float[] DRIVE_X={-1570,-1450},DRIVE_Y={650,440};
    public int lesson=MOVE, actionTicks,channelGate=1;
    public int driveMarker;
    public int clearedGate,gateClearTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY,channelPreviousX,channelPreviousY;
    /** Explicit start/reset only. Lesson progression never rebuilds the scene. */
    public void setup(Game g) {
        g.buoys.reset(g);g.splashes=new CollisionSplashes();
        clearedGate=gateClearTicks=driveMarker=bridgeCrossed=trainCrossed=0;channelGate=1;
        orbitArmed=false;orbitSweep=0;orbitAngle=(float)StrictMath.PI;
        g.cancelInputs();g.reversing=false;
        g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=g.towAimBody=null;g.ropeToTug=false;g.tow=new Towline();
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0;g.nextId=1;g.selected=-1;actionTicks=0;
        g.addCargo(2,292,700);park(g.cargo[0]);
        g.addCargo(2,350,490);g.cargo[1].angle=-(float)Math.PI/2;park(g.cargo[1]);
        g.addCargo(2,390,430);g.cargo[2].angle=-(float)Math.PI/2;park(g.cargo[2]);
        g.tug=new Body(0,-1,START_X,START_Y,0);
        if(lesson==COAST) {
            driveMarker=DRIVE_X.length;g.tug.x=DRIVE_X[driveMarker-1]-DRIVE_RADIUS;g.tug.y=DRIVE_Y[driveMarker-1];g.tug.vx=25;g.tug.vy=-25;
        }
        if(lesson==CHANNEL) g.tug=new Body(0,-1,TrainingChannel.X[0],TrainingChannel.Y[0],0);
        // Reset can restart the current exercise; normal progression preserves
        // the original bodies, positions, momentum, water and camera instead.
        if(lesson>=HOLD) g.tug=new Body(0,-1,376,700,(float)Math.PI);
        if(lesson==TIE) {g.cargo[0].x=Game.DOCK_X[2];park(g.cargo[0]);}
        if(lesson>=CONNECT) {
            Body finished=g.cargo[0],towPractice=g.cargo[1];
            finished.x=Physics.LEFT+finished.radius+10;finished.y=g.dockY(2);finished.angle=(float)Math.PI/2;
            park(finished);finished.lineAttached=true;finished.deliveryRecorded=true;
            finished.unloadingTicks=Game.UNLOAD_TICKS;finished.mooringTicks=Mooring.ALIGN_TICKS;
            finished.dockTime=1.2f;
            g.cargo[0]=towPractice;g.cargo[1]=finished;g.delivered=1;
            towPractice.x=300;towPractice.y=690;park(towPractice);
            g.tug=new Body(0,-1,300,570,-(float)Math.PI/2);
            g.cargo[2].x=350;g.cargo[2].y=450;park(g.cargo[2]);
            if(lesson>=PULL) g.tow.attach(g,towPractice);
            if(lesson>=DOUBLE) {
                g.tug.x=300;g.tug.y=230;towPractice.x=275;towPractice.y=345;park(towPractice);towPractice.awaitingPickup=false;
                g.cargo[2].x=365;g.cargo[2].y=360;park(g.cargo[2]);
                g.tow.release(g);g.tow.attach(g,towPractice);
            }
            if(lesson>=BRIDGE)g.tow.attach(g,g.cargo[2]);
            if(lesson>=LINK) {
                g.tug.x=300;g.tug.y=-70;towPractice.x=275;towPractice.y=55;g.cargo[2].x=365;g.cargo[2].y=60;
                bridgeCrossed=3;g.bridge.phase=Bridge.OPEN;
            }
            if(lesson>=TRAIN) {
                towPractice.x=300;towPractice.y=30;g.cargo[2].x=300;g.cargo[2].y=140;
                g.tow.detach(g,g.cargo[2]);g.tow.attach(g,g.cargo[2],towPractice);
            }
            if(lesson>=RELEASE) {
                g.tug.y=-850;towPractice.y=-750;g.cargo[2].y=-640;trainCrossed=3;
            }
            reconfigureRelease=g.towReleases;rememberTow(g);
            actionTicks=g.towReleases;
        }
        startX=lesson==TRAIN?g.cargo[2].x:lesson>=HOLD?g.cargo[0].x:g.tug.x;
        startY=lesson==TRAIN?g.cargo[2].y:lesson>=HOLD?g.cargo[0].y:g.tug.y;
        channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        g.fitTutorialCamera();
    }
    private static void park(Body b) {
        b.vx=b.vy=b.spin=0;b.awaitingPickup=true;
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
    }
    public void update(Game g) {
        if(gateClearTicks>0) gateClearTicks--;
        // A player may keep driving before following the coast instruction.
        // Remember genuine crossings so lifting late never strands progression.
        if(lesson<=CHANNEL) {
            if(channelGate<TrainingChannel.X.length && TrainingChannel.reachedGate(channelGate,
                    channelPreviousX,channelPreviousY,g.tug.x,g.tug.y)) {
                clearedGate=channelGate++;gateClearTicks=90;g.haptic(Game.HAPTIC_CONTACT);
            }
            channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        }
        if(lesson==MOVE) {
            updateOrbit(g);
        } else if(lesson==COAST) {
            actionTicks=!g.pointer && !g.navigating?actionTicks+1:0;
            if(actionTicks>=60) {
                lesson=CHANNEL;actionTicks=0;
                channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
                g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==CHANNEL) {
            if(channelGate==TrainingChannel.X.length) {
                lesson=HOLD;actionTicks=0;g.haptic(Game.HAPTIC_SUCCESS);
            }
        } else if(lesson==HOLD) {
            Body b=g.cargo[0];
            if(g.pushing && g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++;
            if(actionTicks>=6 && Physics.length(b.x-PUSH_X,b.y-PUSH_Y)<=PUSH_RADIUS) {
                lesson=STEER;actionTicks=0;startX=b.x;startY=b.y;
                g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
            }
        } else if(lesson==STEER) {
            Body b=g.cargo[0];
            if(g.pushing && !g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++; else actionTicks=0;
            if(actionTicks>=12 && Physics.length(b.x-startX,b.y-startY)>3) {
                lesson=DOCK;actionTicks=0;g.haptic(Game.HAPTIC_TICK);
            }
        } else if(lesson==DOCK && g.inTieArea(g.cargo[0])) {
            lesson=TIE;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==TIE && g.delivered>0) {
            // The unloaded barge stays alongside. The next job uses the second
            // hull that has been waiting in the same harbor since the start.
            Body finished=g.cargo[0];g.cargo[0]=g.cargo[1];g.cargo[1]=finished;
            lesson=CONNECT;actionTicks=0;g.selected=-1;g.cancelInputs();
            g.haptic(Game.HAPTIC_SUCCESS);
        }
        else if(lesson==CONNECT && direct(g,g.cargo[0])) {
            lesson=PULL;startX=g.cargo[0].x;startY=g.cargo[0].y;g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==PULL) {
            Body b=g.cargo[0];if(g.navigating)actionTicks++;
            if(!g.tow.connected(b,g.tug) && g.ropePhase==0){lesson=CONNECT;g.cancelInputs();}
            else if(actionTicks>=6 && Physics.length(b.x-TOW_BERTH_X,b.y-TOW_BERTH_Y)<=TOW_BERTH_RADIUS) {
                lesson=DOUBLE;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
            }
        } else if(lesson==DOUBLE && doubleTow(g)) {
            lesson=BRIDGE;rememberTow(g);g.cancelInputs();g.haptic(Game.HAPTIC_TICK);
        } else if(lesson==BRIDGE) {
            if(doubleTow(g)) {
                bridgeCrossed=crossed(g,BRIDGE_CLEAR_Y,bridgeCrossed);
                if(bridgeCrossed==3 && allClear(g,BRIDGE_CLEAR_Y)) {
                    lesson=LINK;reconfigureRelease=g.towReleases;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
                }
            } else if(g.ropePhase==0){lesson=DOUBLE;g.cancelInputs();}
        } else if(lesson==LINK && chain(g) && g.towReleases>reconfigureRelease) {
            lesson=TRAIN;rememberTow(g);g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
        } else if(lesson==TRAIN) {
            if(chain(g)) {
                trainCrossed=crossed(g,TRAIN_CLEAR_Y,trainCrossed);
                if(trainCrossed==3 && allClear(g,TRAIN_CLEAR_Y)) {
                    lesson=RELEASE;actionTicks=g.towReleases;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);
                }
            } else if(g.ropePhase==0){lesson=LINK;g.cancelInputs();}
        } else if(lesson==RELEASE && g.tow.count()==0 && g.towReleases>actionTicks) {lesson=DONE;g.cancelInputs();g.haptic(Game.HAPTIC_SUCCESS);}
        else if(lesson==RELEASE && g.tow.count()==0 && g.ropePhase==0){lesson=LINK;g.cancelInputs();}
    }
    public static boolean direct(Game g,Body b){Towline link=g.tow.incoming(b);return link!=null && link.leader==g.tug;}
    public static boolean doubleTow(Game g){return direct(g,g.cargo[0]) && direct(g,g.cargo[2]);}
    public static boolean chain(Game g){
        Towline a=g.tow.incoming(g.cargo[0]),b=g.tow.incoming(g.cargo[2]);
        return a!=null && b!=null && (a.leader==g.tug && b.leader==g.cargo[0] || b.leader==g.tug && a.leader==g.cargo[2]);
    }
    public static float sternY(Body b){return Math.max(b.discY(0),b.discY(2))+b.radius;}
    public static boolean allClear(Game g,float y){return sternY(g.tug)<y && sternY(g.cargo[0])<y && sternY(g.cargo[2])<y;}
    private void rememberTow(Game g){previousTowY[0]=sternY(g.cargo[0]);previousTowY[1]=sternY(g.cargo[2]);}
    private int crossed(Game g,float y,int mask){
        for(int i=0;i<2;i++){
            float now=sternY(g.cargo[i==0?0:2]);
            if(previousTowY[i]>=y && now<y)mask|=1<<i;
            previousTowY[i]=now;
        }
        return mask;
    }
    /** One route target for the actual tow; cues and public-input replays share it. */
    public float towTargetX(){return lesson==PULL?TOW_BERTH_X:300;}
    public float towTargetY(Game g){
        if(lesson==PULL)return 230;
        if(lesson==BRIDGE)return g.bridge.passable()?-95:Bridge.Y+95;
        return TrainingChannel.TOW_END+80;
    }

    /** Credit one continuous winding around each buoy, never proximity or elapsed time. */
    private void updateOrbit(Game g) {
        float dx=g.tug.x-DRIVE_X[driveMarker],dy=g.tug.y-DRIVE_Y[driveMarker];
        float radius=Physics.length(dx,dy),angle=(float)StrictMath.atan2(dy,dx);
        if(radius<ORBIT_INNER || radius>ORBIT_OUTER) {orbitArmed=false;orbitSweep=0;return;}
        if(!orbitArmed) {
            if(!g.navigating)return;
            orbitArmed=true;orbitSweep=0;orbitAngle=angle;
        } else {
            float delta=Physics.angle(angle-orbitAngle);
            // Physical fixed-step movement is small; relocation cannot award an arc.
            if(Math.abs(delta)>.35f || Physics.length(g.tug.x-orbitPreviousX,g.tug.y-orbitPreviousY)>12)orbitSweep=0;
            else orbitSweep+=delta; // Reversing retraces and cancels the same arc.
            orbitAngle=angle;
        }
        orbitPreviousX=g.tug.x;orbitPreviousY=g.tug.y;
        if(Math.abs(orbitSweep)>=ORBIT_TURN) {
            driveMarker++;actionTicks=0;orbitArmed=false;orbitSweep=0;
            g.haptic(Game.HAPTIC_SUCCESS);
            if(driveMarker==DRIVE_X.length)lesson=COAST;
        }
    }
    public float orbitProgress(){return Physics.clamp(Math.abs(orbitSweep)/ORBIT_TURN,0,1);}
    public float orbitDirection(){return orbitSweep<-.04f?-1:1;}
    private float guideAngle(){return orbitArmed?orbitAngle+orbitDirection()*.65f:(float)StrictMath.PI;}
    public float driveTargetX(){return DRIVE_X[Math.min(driveMarker,DRIVE_X.length-1)]+DRIVE_RADIUS*(float)StrictMath.cos(guideAngle());}
    public float driveTargetY(){return DRIVE_Y[Math.min(driveMarker,DRIVE_Y.length-1)]+DRIVE_RADIUS*(float)StrictMath.sin(guideAngle());}
    public String title() {
        return new String[]{"CIRCLE TWO BUOYS","RELEASE TO COAST","NAVIGATE THE CANAL","DRAG TO PUSH","HOLD FOR ASSIST","DOCK YOUR FIRST LOAD","THROW YOUR LINE","CONNECT A TOWLINE","TOW TO THE BERTH","CONNECT BOTH TO TUG","TOW UNDER THE BRIDGE","RECONFIGURE YOUR TOW","THREAD THE CHANNEL","UNCOUPLE YOUR TRAIN","READY FOR YOUR LEVEL"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case MOVE: return "DRAG TUG OR USE JOYSTICK TO CIRCLE.";
            case COAST: return "LIFT YOUR FINGER TO CUT THRUST.";
            case CHANNEL: return "STEER UPSTREAM THROUGH EVERY GATE.";
            case HOLD: return "PRESS AND DRAG THE BARGE TO PUSH HERE.";
            case STEER: return "LIFT, THEN HOLD THE BARGE STILL.";
            case DOCK: return "LIFT, THEN HOLD THE BARGE AGAIN.";
            case TIE: return "DRAG TO AIM AT THE GLOWING BOLLARD.";
            case CONNECT: return "APPROACH THE SECOND BARGE. DRAG TO TUG.";
            case PULL: return "TOW YOUR BARGE TO THE MARKED BERTH.";
            case DOUBLE: return "LINK BOTH BARGES DIRECTLY TO THE TUG.";
            case BRIDGE: return "PULL BOTH BARGES UNDER THE BRIDGE.";
            case LINK: return "TAP ONE BARGE TO RELEASE ITS TUG LINE.";
            case TRAIN: return "PULL YOUR TRAIN THROUGH THE CHANNEL.";
            case RELEASE: return "TAP EACH LINKED BARGE TO UNCOUPLE.";
            default: return "YOU PUSHED, TIED UP AND TOWED A TRAIN.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case MOVE: return "STAY NEAR THE RING. EITHER WAY ROUND.";
            case COAST: return "BOATS COAST. DRAG BACK TO SLOW DOWN.";
            case CHANNEL: return "SLOW FOR SQUARE TURNS. CAMERA FOLLOWS.";
            case HOLD: return "REACH THE MARKED ZONE TO CONTINUE.";
            case STEER: return "HOLD STILL FOR AN ASSISTED PUSH.";
            case DOCK: return "HOLD STILL TO ENTER THE BERTH.";
            case TIE: return "RELEASE TO THROW. MISSES REEL BACK.";
            case CONNECT: return "RELEASE OVER THE TUG TO THROW.";
            case PULL: return "KEEP THE LINE ATTACHED FOR THE NEXT JOB.";
            case DOUBLE: return "DRAG EACH UNLINKED BARGE TO THE TUG.";
            case BRIDGE: return "WAIT FOR GREEN. BOTH LOADS MUST CLEAR.";
            case LINK: return "THEN DRAG IT TO THE OTHER BARGE.";
            case TRAIN: return "SINGLE FILE. CLEAR THE EXIT WITH BOTH.";
            case RELEASE: return "EACH TAP RELEASES ITS INCOMING LINE.";
            default: return "THE SAME CONTROLS WORK IN A SHIFT.";
        }
    }
}
