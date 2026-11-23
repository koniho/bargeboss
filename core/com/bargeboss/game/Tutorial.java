package com.bargeboss.game;

/** Drag-control practice with action gates and no clock, damage or traffic. */
public final class Tutorial {
    public static final int MOVE=0, FIGURE_EIGHT=1, REVERSE=2, SLIP=3, MAP=4, CHANNEL=5, CONNECT=6, PULL=7, CASTOFF=8, HOLD=9, STEER=10, DOCK=11, TIE=12, DOUBLE=13, BRIDGE=14, LINK=15, TRAIN=16, RELEASE=17, SHOW=18, FIRE_TOW=19, EXTINGUISH=20, DONE=21;
    // PULL (7) is a retired checkpoint ID; explicit resets redirect to CASTOFF.
    public static final float BRIDGE_CLEAR_Y=TutorialTowBridges.EXIT_Y-35,TRAIN_CLEAR_Y=TrainingChannel.NARROW_EXIT-15;
    final InstructorRadio instructorRadio=new InstructorRadio();
    public final HelmPractice helm=new HelmPractice();
    public final NavigationPractice navigation=new NavigationPractice();
    public final TutorialCanalBridge canal=new TutorialCanalBridge();
    final TutorialTowBridges towBridges=new TutorialTowBridges();
    final FireworksLesson fireworks=new FireworksLesson();
    public boolean firstTowParked;
    private int firstOrbitDirection,outsideTicks;
    private boolean skippedPhase;
    public boolean reverseArmed;
    public float reverseTargetX,reverseTargetY;
    public int bridgeCrossed,trainCrossed;
    private final float[] previousTowY=new float[2];
    private int reconfigureRelease;
    public static final float START_X=-1600,START_Y=810,DRIVE_RADIUS=121;
    public static final float ORBIT_INNER=38,ORBIT_OUTER=185,ORBIT_TURN=(float)(2*StrictMath.PI);
    public boolean orbitArmed;
    public float orbitSweep,orbitAngle;
    private float orbitPreviousX,orbitPreviousY;
    public static final float[] DRIVE_X={-1570,-1450},DRIVE_Y={650,440};
    public int lesson=MOVE, actionTicks,channelGate=1;
    public int driveMarker;
    TutorialArrival arrival;
    public boolean entering;
    public int entranceTicks;
    final InstructionPanel instructionPanel=new InstructionPanel();
    final LessonFeedback feedback=new LessonFeedback();
    public void enter(Game g){
        if(lesson!=MOVE)return;
        entering=true;entranceTicks=0;g.cancelInputs();
        arrival=new TutorialArrival();arrival.start(g);
        g.fitTutorialCamera();
    }
    public static final int BUOY_CLEAR_LIFE=96;
    public final int[] buoyClearTicks=new int[DRIVE_X.length];
    public int clearedGate,gateClearTicks;
    public boolean exitRequested, resumeShift;
    private float startX,startY,channelPreviousX,channelPreviousY;
    /** Explicit start/reset only. Lesson progression never rebuilds the scene. */
    public void setup(Game g) {
        if(lesson==PULL)lesson=CASTOFF;
        FirewoodPile.prepare();
        boolean hadTeacher=arrival!=null;instructorRadio.reset();feedback.reset();
        entering=false;entranceTicks=0;arrival=null;g.dialogue.close(g);instructionPanel.reset();
        g.buoys.reset(g);g.splashes=new CollisionSplashes();
        g.crew.reset();towBridges.reset();fireworks.reset();g.abilities.reset();firstTowParked=false;
        canal.reset(lesson>CHANNEL);g.passages=new BridgePassage(g);LocationBridges.prepare(g.passages.bridges);
        if(lesson>CHANNEL)g.passages.position[0]=BridgePassage.OPEN_TICKS;
        for(int i=0;i<buoyClearTicks.length;i++)buoyClearTicks[i]=0;
        clearedGate=gateClearTicks=driveMarker=bridgeCrossed=trainCrossed=0;channelGate=1;
        orbitArmed=false;orbitSweep=0;orbitAngle=(float)StrictMath.PI;firstOrbitDirection=outsideTicks=0;reverseArmed=lesson==REVERSE;
        g.cancelInputs();g.reversing=false;
        g.ropePhase=g.ropeTicks=0;g.ropeBody=null;g.ropeTowTarget=g.towAimBody=null;g.ropeToTug=false;g.tow=new Towline();
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.delivered=g.score=0;g.nextId=1;g.selected=-1;actionTicks=0;
        g.addCargo(2,599,300);g.cargo[0].angle=(float)Math.PI/2;park(g.cargo[0]);
        g.addCargo(2,245,0);g.cargo[1].ability=BargeAbilities.FIREWORKS;g.cargo[1].angle=-(float)Math.PI/2;park(g.cargo[1]);
        g.addCargo(2,365,0);g.cargo[2].ability=BargeAbilities.FIREWORKS;g.cargo[2].angle=-(float)Math.PI/2;park(g.cargo[2]);
        g.addCargo(2,FireworksLesson.BERTH_X,FireworksLesson.BERTH_Y);g.cargo[3].ability=BargeAbilities.FIRE;g.cargo[3].angle=0;park(g.cargo[3]);
        g.tug=new Body(0,-1,START_X,START_Y,0);
        if(lesson==FIGURE_EIGHT || lesson==REVERSE){
            g.tug.x=HelmPractice.MEET_X;g.tug.y=HelmPractice.MEET_Y;
            g.tug.angle=.52f;
        }
        if(lesson==SLIP || lesson==MAP)g.tug=new Body(0,-1,NavigationPractice.APPROACH_X,NavigationPractice.SLIP_Y,0);
        if(lesson==CHANNEL){
            g.tug=new Body(0,-1,TrainingChannel.MIN-160,TrainingChannel.Y[0],0);
            arrival=new TutorialArrival();arrival.observe();
        }
        // Reset can restart the current exercise; normal progression preserves
        // the original bodies, positions, momentum, water and camera instead.
        if(lesson>=CONNECT) g.tug=new Body(0,-1,455,600,0);
        if(lesson>=HOLD){g.cargo[0].x=292;g.cargo[0].y=700;g.cargo[0].angle=(float)Math.PI;park(g.cargo[0]);g.tug=new Body(0,-1,376,700,(float)Math.PI);}
        if(lesson==CASTOFF){
            g.cargo[0].x=320;g.cargo[0].y=550;park(g.cargo[0]);g.tug.x=320;g.tug.y=435;
            g.tow.attach(g,g.cargo[0]);actionTicks=g.towReleases;
        }
        if(lesson==TIE) {g.cargo[0].x=Game.DOCK_X[2];park(g.cargo[0]);}
        if(lesson>=DOUBLE) {
            Body finished=g.cargo[0];
            finished.x=Physics.LEFT+finished.radius+10;finished.y=g.dockY(2);finished.angle=(float)Math.PI/2;
            park(finished);finished.lineAttached=true;finished.deliveryRecorded=true;
            finished.unloadingTicks=Game.UNLOAD_TICKS;finished.mooringTicks=Mooring.ALIGN_TICKS;finished.dockTime=1.2f;g.delivered=1;
            g.tug=new Body(0,-1,260,590,-(float)Math.PI/2);
            g.cargo[0]=g.cargo[1];g.cargo[1]=finished;firstTowParked=true;
            g.tug.x=300;g.tug.y=480;
            Body towPractice=g.cargo[0];
            if(lesson>=BRIDGE){
                towBridges.authorized[TutorialTowBridges.ENTRY]=true;g.passages.position[TutorialTowBridges.ENTRY]=BridgePassage.OPEN_TICKS;
                g.tug.x=300;g.tug.y=-255;towPractice.y=-190;g.cargo[2].y=-190;g.tow.attach(g,towPractice);g.tow.attach(g,g.cargo[2]);
            }
            if(lesson>=LINK){
                towBridges.authorized[TutorialTowBridges.EXIT]=true;g.passages.position[TutorialTowBridges.EXIT]=BridgePassage.OPEN_TICKS;
                g.tug.x=300;g.tug.y=-570;towPractice.x=275;towPractice.y=-445;g.cargo[2].x=365;g.cargo[2].y=-440;bridgeCrossed=3;
            }
            if(lesson>=TRAIN){
                towPractice.x=300;towPractice.y=-470;g.cargo[2].x=300;g.cargo[2].y=-360;
                g.tow.detach(g,g.cargo[2]);g.tow.attach(g,g.cargo[2],towPractice);
            }
            if(lesson>=RELEASE){g.tug.y=-1350;towPractice.y=-1250;g.cargo[2].y=-1140;trainCrossed=3;}
            if(lesson>=SHOW){g.tow.release(g);fireworks.reset();}
            if(lesson>=FIRE_TOW){fireworks.launched=true;fireworks.ignite();}
            if(lesson==EXTINGUISH){g.cargo[3].x=FireworksLesson.TOW_X;g.cargo[3].y=FireworksLesson.TOW_Y;park(g.cargo[3]);g.tug.x=FireworksLesson.TOW_X;g.tug.y=FireworksLesson.TOW_Y+110;g.tow.attach(g,g.cargo[3]);}
            reconfigureRelease=g.towReleases;rememberTow(g);
            actionTicks=g.towReleases;
        }
        startX=lesson==TRAIN?g.cargo[2].x:lesson>=HOLD?g.cargo[0].x:g.tug.x;
        startY=lesson==TRAIN?g.cargo[2].y:lesson>=HOLD?g.cargo[0].y:g.tug.y;
        channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        helm.reset(g);navigation.reset(g);
        if(hadTeacher && arrival==null){arrival=new TutorialArrival();arrival.observe(g);}
        g.fitTutorialCamera();
    }
    int nextPhase(){return lesson<CONNECT?CONNECT:lesson<SHOW?SHOW:DONE;}
    String phaseName(){return lesson<CONNECT?"DRIVING":lesson<SHOW?"TOWING":"BARGE ABILITIES";}
    String skipDetail(){return lesson<CONNECT?"NEXT: CONNECT AND HANDLE A TOW.":lesson<SHOW?"NEXT: FIREWORKS AND FIREFIGHTING.":"THIS WILL FINISH CAPTAIN TRAINING.";}
    /** Explicit phase skip prepares the next phase's complete prerequisite scene. */
    void skip(Game g){
        if(lesson>=DONE)return;
        g.cancelInputs();skippedPhase=true;lesson=nextPhase();
        if(lesson<DONE)setup(g);
        else {
            // Do not leave an active fire/audio loop or reopen the relief dialogue on DONE.
            g.dialogue.close(g);instructorRadio.reset();feedback.reset();
            fireworks.reset();g.abilities.reset();g.selected=-1;
        }
        g.haptic(Game.HAPTIC_SUCCESS);
    }
    private static void park(Body b) {
        b.vx=b.vy=b.spin=0;b.awaitingPickup=true;
        b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
    }
    private static void success(Game g){g.sounds.complete();g.haptic(Game.HAPTIC_SUCCESS);}
    public void update(Game g) {
        skippedPhase=false;feedback.sample(g);int before=lesson;
        updateLesson(g);if(!skippedPhase)feedback.completed(g,before,lesson);
    }
    private void updateLesson(Game g) {
        if(instructorRadio.step(g)){
            // Radio pauses lesson progression, not the observer hull's motion state.
            if(arrival!=null)arrival.step(g);
            return;
        }
        canal.step(g);towBridges.step(g);
        if(arrival!=null)arrival.step(g);
        if(entering)return;
        if(gateClearTicks>0) gateClearTicks--;
        for(int i=0;i<buoyClearTicks.length;i++)if(buoyClearTicks[i]>0)buoyClearTicks[i]--;
        // Remember genuine early crossings without bypassing the opening exercises.
        if(lesson<=CHANNEL) {
            if(channelGate<TrainingChannel.X.length && TrainingChannel.reachedGate(channelGate,
                    channelPreviousX,channelPreviousY,g.tug.x,g.tug.y)) {
                clearedGate=channelGate++;gateClearTicks=90;success(g);
            }
            channelPreviousX=g.tug.x;channelPreviousY=g.tug.y;
        }
        if(lesson==MOVE) {
            int previous=helm.phase;
            boolean complete=helm.step(g);
            if(previous==HelmPractice.FORWARD && helm.phase==HelmPractice.LEFT)instructorRadio.introduce(g);
            if(complete){lesson=FIGURE_EIGHT;driveMarker=0;orbitArmed=false;orbitSweep=0;firstOrbitDirection=0;g.cancelInputs();success(g);}
        } else if(lesson==FIGURE_EIGHT || lesson==REVERSE) {
            updateOrbit(g);reverseTargetX=driveTargetX();reverseTargetY=driveTargetY();
        } else if(lesson==SLIP) {
            if(navigation.slip(g)){lesson=MAP;g.cancelInputs();success(g);}
        } else if(lesson==MAP) {
            if(navigation.map(g)){lesson=CHANNEL;g.cancelInputs();success(g);}
        } else if(lesson==CHANNEL) {
            if(channelGate==TrainingChannel.X.length && canal.authorized && g.passages.passable(0)) {
                lesson=CONNECT;actionTicks=0;success(g);
            }
        } else if(lesson==HOLD) {
            Body b=g.cargo[0];
            if(g.pushing && g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++;
            if(actionTicks>=6 && Physics.length(b.x-startX,b.y-startY)>6) {
                lesson=STEER;actionTicks=0;startX=b.x;startY=b.y;
                g.cancelInputs();success(g);
            }
        } else if(lesson==STEER) {
            Body b=g.cargo[0];
            if(g.pushing && !g.manualSteering && g.heldCargo()==b && Physics.touching(g.tug,b,5)) actionTicks++; else actionTicks=0;
            if(actionTicks>=12 && Physics.length(b.x-startX,b.y-startY)>3) {
                lesson=DOCK;actionTicks=0;success(g);
            }
        } else if(lesson==DOCK && g.inTieArea(g.cargo[0])) {
            lesson=TIE;g.cancelInputs();success(g);
        } else if(lesson==TIE && g.delivered>0) {
            // Keep the delivered hull alongside while the next pair waits beyond the bridge.
            Body finished=g.cargo[0];
            g.cargo[0]=g.cargo[1];g.cargo[1]=finished;firstTowParked=true;
            lesson=DOUBLE;actionTicks=0;g.selected=-1;g.cancelInputs();success(g);
        } else if(lesson==CONNECT && direct(g,g.cargo[0])) {
            lesson=CASTOFF;actionTicks=g.towReleases;g.cancelInputs();success(g);
        } else if(lesson==CASTOFF && g.tow.incoming(g.cargo[0])==null && g.ropePhase==0) {
            if(g.towReleases>actionTicks){lesson=HOLD;actionTicks=0;startX=g.cargo[0].x;startY=g.cargo[0].y;g.cancelInputs();success(g);}
            else {lesson=CONNECT;g.cancelInputs();}
        } else if(lesson==SHOW && fireworks.ignited) {
            lesson=FIRE_TOW;actionTicks=0;g.cancelInputs();g.haptic(Game.HAPTIC_CONTACT);
        } else if(lesson==FIRE_TOW){
            Body fire=g.cargo[3];if(direct(g,fire) && g.navigating)actionTicks++;
            if(direct(g,fire) && actionTicks>=6 && Physics.length(fire.x-FireworksLesson.TOW_X,fire.y-FireworksLesson.TOW_Y)<=50){lesson=EXTINGUISH;g.cancelInputs();success(g);}
        } else if(lesson==EXTINGUISH && fireworks.done && fireworks.rescueRadio.complete()) {
            lesson=DONE;g.cancelInputs();success(g);
        } else if(lesson==DOUBLE && g.passages.passable(TutorialTowBridges.ENTRY) && doubleTow(g)) {
            lesson=BRIDGE;rememberTow(g);g.cancelInputs();success(g);
        } else if(lesson==BRIDGE) {
            if(doubleTow(g) && g.passages.passable(TutorialTowBridges.EXIT)) {
                bridgeCrossed=crossed(g,BRIDGE_CLEAR_Y,bridgeCrossed);
                if(bridgeCrossed==3 && allClear(g,BRIDGE_CLEAR_Y)) {
                    lesson=LINK;reconfigureRelease=g.towReleases;g.cancelInputs();success(g);
                }
            } else if(!doubleTow(g) && g.ropePhase==0){lesson=DOUBLE;g.cancelInputs();}
        } else if(lesson==LINK && chain(g) && g.towReleases>reconfigureRelease) {
            lesson=TRAIN;rememberTow(g);g.cancelInputs();success(g);
        } else if(lesson==TRAIN) {
            if(chain(g)) {
                trainCrossed=crossed(g,TRAIN_CLEAR_Y,trainCrossed);
                if(trainCrossed==3 && allClear(g,TRAIN_CLEAR_Y)) {
                    lesson=RELEASE;actionTicks=g.towReleases;g.cancelInputs();success(g);
                }
            } else if(g.ropePhase==0){lesson=LINK;g.cancelInputs();}
        } else if(lesson==RELEASE && g.tow.count()==0 && g.towReleases>actionTicks) {lesson=SHOW;fireworks.reset();g.cancelInputs();success(g);}
        else if(lesson==RELEASE && g.tow.count()==0 && g.ropePhase==0){lesson=LINK;g.cancelInputs();}
    }
    public float reverseProgress(){return Physics.clamp((driveMarker+orbitProgress())/2,0,1);}
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
    private static final float[] FIRST_TOW_X={300,320,599,599},FIRST_TOW_Y={650,700,700,410};
    float firstTowTarget(Game g,boolean x){
        return TowRouteGuide.target(g.tug.x,g.tug.y,FIRST_TOW_X,FIRST_TOW_Y,lesson==CASTOFF,x);
    }
    /** One route target for the actual tow; cues and public-input replays share it. */
    public float towTargetX(Game g){if(lesson==FIRE_TOW)return fireworks.target(g,true);return lesson==CASTOFF || lesson==CONNECT?firstTowTarget(g,true):300;}
    public float towTargetY(Game g){
        if(lesson==FIRE_TOW)return fireworks.target(g,false);
        if(lesson==CASTOFF || lesson==CONNECT)return firstTowTarget(g,false);
        if(lesson==BRIDGE)return g.passages.passable(TutorialTowBridges.EXIT)?-595:TutorialTowBridges.EXIT_Y+95;
        return TrainingChannel.TOW_END+80;
    }

    /** Opposite signed windings form a figure eight; the reverse pass requires astern thrust. */
    private void updateOrbit(Game g) {
        if(driveMarker>=2)return;
        float dx=g.tug.x-DRIVE_X[driveMarker],dy=g.tug.y-DRIVE_Y[driveMarker];
        float radius=Physics.length(dx,dy),angle=(float)StrictMath.atan2(dy,dx);
        float mx=g.tug.x-orbitPreviousX,my=g.tug.y-orbitPreviousY;
        float delta=Physics.angle(angle-orbitAngle);
        orbitPreviousX=g.tug.x;orbitPreviousY=g.tug.y;orbitAngle=angle;
        if(radius<ORBIT_INNER || radius>ORBIT_OUTER){
            if(++outsideTicks>30){orbitArmed=false;orbitSweep=0;}return;
        }
        outsideTicks=0;
        boolean reverse=lesson==REVERSE;
        float along=mx*(float)StrictMath.cos(g.tug.angle)+my*(float)StrictMath.sin(g.tug.angle);
        boolean powered=g.navigating && g.reversing==reverse && (reverse?along<-.001f:along>.001f);
        if(!orbitArmed){if(powered){orbitArmed=true;orbitSweep=0;}return;}
        if(Math.abs(delta)>.35f || Physics.length(mx,my)>12){orbitSweep=0;return;}
        if(powered)orbitSweep+=delta;
        int required=driveMarker==0?0:-firstOrbitDirection;
        float winding=required==0?Math.abs(orbitSweep):orbitSweep*required;
        if(winding>=ORBIT_TURN){
            if(driveMarker==0)firstOrbitDirection=orbitSweep<0?-1:1;
            buoyClearTicks[driveMarker]=BUOY_CLEAR_LIFE;driveMarker++;orbitArmed=false;orbitSweep=0;
            success(g);
            if(driveMarker==2){
                if(lesson==FIGURE_EIGHT){lesson=REVERSE;driveMarker=0;firstOrbitDirection=0;reverseArmed=true;}
                else {lesson=SLIP;reverseArmed=false;navigation.reset(g);}
                actionTicks=0;g.cancelInputs();success(g);
            }
        }
    }
    public float orbitProgress(){float sweep=driveMarker==0?Math.abs(orbitSweep):Math.max(0,orbitSweep*-firstOrbitDirection);return Physics.clamp(sweep/ORBIT_TURN,0,1);}
    public float orbitDirection(){return driveMarker>0?-firstOrbitDirection:orbitSweep<-.04f?-1:orbitSweep>.04f?1:lesson==REVERSE?-1:1;}
    private float guideAngle(){
        if(orbitArmed)return orbitAngle+orbitDirection()*.55f;
        int i=Math.min(driveMarker,1);return (float)StrictMath.atan2(HelmPractice.MEET_Y-DRIVE_Y[i],HelmPractice.MEET_X-DRIVE_X[i])+orbitDirection()*.25f;
    }
    public float driveTargetX(){return lesson==MOVE?helm.targetX:DRIVE_X[Math.min(driveMarker,1)]+DRIVE_RADIUS*(float)StrictMath.cos(guideAngle());}
    public float driveTargetY(){return lesson==MOVE?helm.targetY:DRIVE_Y[Math.min(driveMarker,1)]+DRIVE_RADIUS*(float)StrictMath.sin(guideAngle());}
    public String title() {
        return new String[]{"LEAVE THE DOCK","FORWARD FIGURE EIGHT","REVERSE FIGURE EIGHT","BACK INTO THE SLIP","USE THE OVERVIEW MAP","NAVIGATE THE CANAL","CONNECT A TOWLINE","RETIRED CHECKPOINT","TOW THEN RELEASE","HOLD AND DRAG TO PUSH","HOLD FOR BERTH ASSIST","DOCK YOUR FIRST LOAD","THROW YOUR LINE","CONNECT BOTH TO TUG","TOW UNDER THE BRIDGE","NARROW PASSAGE AHEAD","THREAD THE CHANNEL","UNCOUPLE YOUR TRAIN","VICTORY FIREWORKS","FETCH THE FIRE BARGE","ONE LAST LITTLE FIRE","READY FOR YOUR LEVEL"}[lesson];
    }
    public String firstLine() {
        switch(lesson) {
            case MOVE: return helm.first();
            case FIGURE_EIGHT: return "CIRCLE BOTH BUOYS IN OPPOSITE DIRECTIONS.";
            case REVERSE: return "RELEASE. START IN AMBER. TRACE THE 8.";
            case CHANNEL: return "STEER UPSTREAM THROUGH EVERY GATE.";
            case HOLD: return "PRESS THE BARGE. KEEP HOLDING, THEN DRAG.";
            case STEER: return "LIFT. PRESS AND HOLD THE BARGE AGAIN.";
            case DOCK: return "KEEP HOLDING TO PUSH INTO THE BERTH.";
            case TIE: return "DRAG TO AIM AT THE GLOWING BOLLARD.";
            case CONNECT: return "DRAG THE FAR-SIDE BARGE TO YOUR TUG.";
            case CASTOFF: return "TOW CLEAR OF THE ARM, TOWARD THE BERTH.";
            case DOUBLE: return "LINK BOTH BARGES DIRECTLY TO THE TUG.";
            case BRIDGE: return "PULL BOTH BARGES UNDER THE BRIDGE.";
            case LINK: return "TWO ABREAST WON'T FIT. TAP ONE TOWLINE.";
            case TRAIN: return "PULL YOUR TRAIN THROUGH THE CHANNEL.";
            case RELEASE: return "TAP EACH TOWLINE TO LET GO.";
            default: return "YOU PUSHED, TIED UP AND TOWED A TRAIN.";
        }
    }
    public String secondLine() {
        switch(lesson) {
            case MOVE: return helm.second();
            case FIGURE_EIGHT: return "CROSS THROUGH THE MIDDLE TO MAKE AN 8.";
            case REVERSE: return "KEEP THE STERN LEADING AROUND BOTH BUOYS.";
            case CHANNEL: return "SLOW FOR SQUARE TURNS. CAMERA FOLLOWS.";
            case HOLD: return "TUG ALIGNS BEHIND IT. DRAG TOWARD THE BERTH.";
            case STEER: return "DON'T DRAG: ASSIST STEERS TO THE BERTH.";
            case DOCK: return "DRAG TO STEER. LIFT TO STOP PUSHING.";
            case TIE: return "RELEASE TO THROW. MISSES REEL BACK.";
            case CONNECT: return "RELEASE OVER THE TUG TO THROW.";
            case CASTOFF: return "READY TO PUSH? TAP THE TOWLINE TO LET GO.";
            case DOUBLE: return "DRAG EACH UNLINKED BARGE TO THE TUG.";
            case BRIDGE: return "WAIT FOR GREEN. BOTH LOADS MUST CLEAR.";
            case LINK: return "UNCOUPLE, THEN DRAG BARGE TO BARGE.";
            case TRAIN: return "GO SLOW. BOTH BARGES MUST CLEAR THE EXIT.";
            case RELEASE: return "LEAVE BOTH BARGES READY FOR CELEBRATION.";
            default: return "THE SAME CONTROLS WORK IN A SHIFT.";
        }
    }
}
