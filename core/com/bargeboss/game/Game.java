package com.bargeboss.game;

/** All gameplay, timers, gestures and lifecycle behavior live here, never in native hosts. */
public final class Game {
    public static final int TITLE=0, PLAY=1, PAUSE=2, FINISH=3, HELP=4, CHALLENGES=5, SETTINGS=6;
    public static final int SHIFT_TICKS=180*60, QUOTA=6;
    // Two original-size motion steps per real tick: 2x travel/turning with the
    // same collision resolution, stopping distances and impact thresholds.
    // Gesture, contract and shift clocks stay at 60 real ticks per second.
    public static final int MOTION_STEPS=2, UNLOAD_TICKS=3*60;
    public static final float[] DOCK_X={230,370,230}, DOCK_Y={290,488,700};
    public static final float[] TIE_X={120,480,120};
    public static final float TIE_RADIUS=30;
    public static final int ROPE_FLIGHT_TICKS=18, ROPE_RETURN_TICKS=18, ROPE_HIT_TICKS=24;
    public int ropePhase,ropeTicks;
    public Body ropeBody;
    public float ropeStartX,ropeStartY,ropeEndX,ropeEndY,ropeStartHeight,ropeLocalX,ropeLocalY;
    private boolean ropeTargeted;
    public boolean ropeToTug,towAiming;
    public Body towAimBody,ropeTowTarget;
    public Towline tow=new Towline();
    public int towReleases;
    public static final int[] COLORS={0xffefa94a,0xff62c8bd,0xffde7965};
    public static final String[] CARGO_NAMES={"STEEL", "FREIGHT", "BULK"};
    public final Body[] cargo=new Body[9];
    public Body tug;
    public WaterSurface water=new WaterSurface();
    public VesselSmoke smoke=new VesselSmoke(),titleSmoke=new VesselSmoke();
    public BuildingFades buildingFades=new BuildingFades();
    public Bridge bridge=new Bridge();
    public Body trafficBoat;
    public int mode=TITLE, ticks, score, delivered, lost, hull=100, selected=-1, combo;
    public int visualTicks;
    public int celebrationTicks, celebrationPoints, impactTicks,damageFlashTicks;
    public float celebrationX,celebrationY,impactX,impactY;
    public int noticeTicks, damageCooldown, nextId=1;
    public String notice="WELCOME TO THE RIVER";
    public float targetX, targetY, pointerX, pointerY, downX, downY;
    public boolean pointer, pushing, navigating, ropeDragging;
    public static final int IDLE=0, MOVING=1, HOLDING=2, ALIGNING=3, PUSHING=4,
            STEERING=5, DOCKING=6, COASTING=7, REVERSING=8, WAITING_BRIDGE=9;
    public int control=IDLE;
    public boolean reversing, manualSteering;
    private boolean tapCandidate;
    // A committed cargo push owns this touch until lift/cancel. Moving vessels
    // must not reclassify the same steering finger as a towline throw.
    private boolean cargoPushGesture;
    public float guideX, guideY, pushX=-1, pushY;
    public Tutorial tutorial;
    public static final int HAPTIC_TICK=1, HAPTIC_CONTACT=2, HAPTIC_IMPACT=3, HAPTIC_SUCCESS=4;
    public boolean hapticsEnabled=true;
    private int hapticPending, hapticCooldown, contactCooldown, contactId=-1, mapPointer=-1;
    private boolean mapInRiver;
    private Game practice;
    private boolean returnToShift;
    private int pressTicks, held=-1, spawnIndex;
    public Feeder[] feeders;
    private int nextFeedTick;
    private final float[] route=new float[2];
    private float engineDemand;
    public int cameraOverviewTicks;
    public static final float TRAFFIC_Y=420;
    private double accumulator, visualAccumulator;

    public Layout layout=Layout.DEFAULT;
    public final Career career=new Career();
    public GameSettings settings=new GameSettings();
    public final Joystick joystick=new Joystick();
    private int settingsReturnMode=TITLE;
    public int challengeId,earnedStars;
    public boolean rankPromoted;
    public Game() { reset(); mode=TITLE; }
    public int quota() { return Challenges.quota(challengeId); }
    public int shiftTicks() { return Challenges.shiftTicks(challengeId); }
    public String challengeTitle() { return Challenges.title(challengeId); }
    public boolean hasBridge() { return bridgeStage() || tutorial!=null; }
    public boolean bridgeStage() { return challengeId==1 && tutorial==null; }
    public boolean berthEnabled(int berth) { return tutorial==null && challengeId==2 || berth==2; }
    public char berthLetter(int berth) { return tutorial==null && challengeId==2?(char)('A'+berth):'A'; }
    public float dockY(int berth) { return bridgeStage()?-400:challengeId==0 && tutorial==null && berth==2?290:DOCK_Y[berth]; }
    public float channelTop() { return tutorial!=null?TrainingChannel.TOW_END:bridgeStage()?-520:Physics.TOP; }
    public int contractSeconds() { return bridgeStage()?130:68; }
    public int feedInterval() { return bridgeStage()?30*60:14*60; }
    public int feederDrops() { int count=0;for(Feeder f:feeders) count+=f.drops;return count; }
    public void restoreProgress(String value) {
        if(value==null || value.length()>192)return;
        String progress=value;int side=GameSettings.LEFT;
        if(value.startsWith("BBG1\n")) {
            String[] parts=value.split("\n",-1);
            if(parts.length!=3 || parts[2].length()!=1 || parts[2].charAt(0)<'0' || parts[2].charAt(0)>'2')return;
            progress=parts[1];side=parts[2].charAt(0)-'0';
        }
        if(career.restore(progress)){challengeId=career.selectedChallenge();settings.restore(side);}
    }
    public String saveProgress() { return "BBG1\n"+career.save()+"\n"+settings.joystickSide(); }
    public int progressVersion() { return career.version()+settings.version(); }
    public void openSettings(){if(mode==SETTINGS)return;settingsReturnMode=mode;cancelInputs();accumulator=visualAccumulator=0;mode=SETTINGS;}
    public boolean selectChallenge(int id) {
        if(!career.select(id)) return false;
        challengeId=id; return true;
    }
    /** Hosts supply available logical bounds; resizing never advances physics or clocks. */
    public void configureCanvas(float width,float height) {
        if(Math.abs(layout.width-width)<.01f && Math.abs(layout.height-height)<.01f) return;
        cancel();
        RiverCamera c=layout.camera;
        layout=new Layout(width,height,c.worldTop,c.worldRight,c.zoom);
        if(tutorial!=null) fitTutorialCamera();
        if(practice!=null) practice.configureCanvas(width,height);
    }
    public boolean trainingChannel() { return tutorial!=null && tutorial.lesson==Tutorial.CHANNEL; }
    public boolean inWater(float x,float y) {
        return tutorial!=null?TrainingChannel.contains(x,y):x>=Physics.LEFT && x<=Physics.RIGHT && y>=channelTop() && y<=Physics.BOTTOM;
    }
    public static final float WORK_ZOOM=1.8f,DOCK_ZOOM=2.1f;
    public static final float PRACTICE_DRIVE_ZOOM=2.2f,PRACTICE_CANAL_ZOOM=1.4f;
    /** Travel gets context; close work gets a larger hull without changing the physics. */
    private Layout tutorialCamera() {
        if(tutorial.lesson==Tutorial.MOVE) {
            int buoy=Math.min(tutorial.driveMarker,Tutorial.DRIVE_X.length-1);
            float dx=Tutorial.DRIVE_X[buoy]-tug.x,dy=Tutorial.DRIVE_Y[buoy]-tug.y;
            // Keep the tug touchable while crossing between the two orbits.
            float lead=Math.min(.65f,95/Math.max(1,Physics.length(dx,dy)));
            return Layout.focused(layout.width,layout.height,tug.x+dx*lead,tug.y+dy*lead,
                    PRACTICE_DRIVE_ZOOM,layout.width*.5f,layout.height*.59f);
        }
        if(tutorial.lesson==Tutorial.BRIDGE || tutorial.lesson==Tutorial.TRAIN)return workCamera(1.3f,0,true);
        float entrance=Physics.clamp((tug.x-(TrainingChannel.MIN-160))/160,0,1);
        entrance=entrance*entrance*(3-2*entrance);
        float travelZoom=PRACTICE_DRIVE_ZOOM+(PRACTICE_CANAL_ZOOM-PRACTICE_DRIVE_ZOOM)*entrance;
        float blend=Physics.clamp((tug.x-80)/160,0,1);
        blend=blend*blend*(3-2*blend);
        if(blend==0) return Layout.focused(layout.width,layout.height,tug.x,tug.y,travelZoom,layout.width*.5f,layout.height*.62f);
        return workCamera(travelZoom+(WORK_ZOOM-travelZoom)*blend,blend,true);
    }
    void fitTutorialCamera() { layout=tutorialCamera(); }
    private Body cameraLoad() {
        Body held=workingCargo();
        if(tutorial!=null && (tutorial.lesson==Tutorial.LINK || tutorial.lesson==Tutorial.DOUBLE) && held==null)return cargo[2];
        if(held!=null && !held.deliveryRecorded) return held;
        if(tow.primary()!=null) return tow.primary();
        Body result=null;float nearest=190;
        for(Body b:cargo) if(b!=null && b.active && !b.deliveryRecorded
                && (tutorial==null || b==cargo[0])) {
            float distance=Physics.length(tug.x-b.x,tug.y-b.y);
            if(distance<nearest) {result=b;nearest=distance;}
        }
        return result;
    }
    private Layout workCamera(float zoom,float workBlend,boolean practiceView) {
        Body load=cameraLoad();
        boolean docking=load!=null && !load.awaitingPickup && tow.incoming(load)==null
                && Physics.length(load.x-DOCK_X[load.berth],load.y-dockY(load.berth))<190;
        if(docking && workBlend>0) {
            float distance=Physics.length(load.x-DOCK_X[load.berth],load.y-dockY(load.berth));
            zoom+=(DOCK_ZOOM-WORK_ZOOM)*workBlend*Physics.clamp((190-distance)/130,0,1);
        }
        // Fit the working hulls and, when relevant, their actual elevated tie point.
        // Far-away held loads do not drag the camera away from the player.
        if(load!=null && Physics.length(tug.x-load.x,tug.y-load.y)>230 && !(tutorial!=null && (tutorial.lesson==Tutorial.LINK || tutorial.lesson==Tutorial.DOUBLE))) {load=null;docking=false;}
        RiverCamera base=RiverCamera.DEFAULT;
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        int count=load==null?1:docking?3:2;
        for(int i=0;i<count+cargo.length;i++) {
            Body train=i>=count?cargo[i-count]:null;
            if(i>=count && (train==null || train==load || !tow.connected(train,tug)))continue;
            float wx=i>=count?train.x:i==0?tug.x:i==1?load.x:bollardX(load.berth);
            float wy=i>=count?train.y:i==0?tug.y:i==1?load.y: bollardY(load.berth);
            float x=base.screenX(wx,wy)/base.scale,y=base.screenY(wx,wy)/base.scale;
            minX=Math.min(minX,x-48);maxX=Math.max(maxX,x+48);
            minY=Math.min(minY,y-78);maxY=Math.max(maxY,y+36);
        }
        float availableHeight=layout.height-(practiceView?250:136);
        float baseScale=layout.camera.scale/layout.camera.zoom;
        float fit=Math.min((layout.width-120)/(maxX-minX),availableHeight/(maxY-minY))/baseScale;
        zoom=Math.min(zoom,Math.max(tow.loadCount(tug)>1?.7f:1,fit));
        float focusX=base.worldX((minX+maxX)*.5f*base.scale,(minY+maxY)*.5f*base.scale);
        float focusY=base.worldY((minX+maxX)*.5f*base.scale,(minY+maxY)*.5f*base.scale);
        return Layout.focused(layout.width,layout.height,focusX,focusY,zoom,layout.width*.5f,
                layout.height*(practiceView?.59f:.52f));
    }
    private void updateCamera() {
        RiverCamera c=layout.camera;
        if(ropeDragging || towAiming || ropePhase!=0) return;
        Layout desired;
        if(tutorial!=null) desired=tutorialCamera();
        else {
            if(cameraOverviewTicks>0) cameraOverviewTicks--;
            // The long bridge reach is a transit phase even when carrying a load.
            boolean transit=bridgeStage() && tug.y<Feeder.DROP_Y-180 && tug.y>dockY(2)+230;
            boolean overview=cameraOverviewTicks>0;
            desired=workCamera(transit || overview?1:WORK_ZOOM,transit || overview?0:1,false);
        }
        float zoom=c.zoom+(desired.camera.zoom-c.zoom)*.035f;
        // Ease the world focus, not its zoom-dependent camera origin. This avoids
        // a sideways sweep when zoom changes around a far-upstream position.
        float anchorX=layout.width*.5f,anchorY=layout.height*(tutorial!=null?.59f:.52f);
        float x=c.worldX(anchorX,anchorY),y=c.worldY(anchorX,anchorY);
        float tx=desired.camera.worldX(anchorX,anchorY),ty=desired.camera.worldY(anchorX,anchorY);
        float ease=tutorial!=null && tutorial.lesson!=Tutorial.MOVE && tug.x<=80?.12f:.055f;
        Layout next=Layout.focused(layout.width,layout.height,x+(tx-x)*ease,y+(ty-y)*ease,zoom,anchorX,anchorY);
        if(Math.abs(next.camera.worldTop-c.worldTop)<.0001f && Math.abs(next.camera.worldRight-c.worldRight)<.0001f
                && Math.abs(zoom-c.zoom)<.000001f) return;
        changeCamera(next);
    }
    private void changeCamera(Layout nextLayout) {
        RiverCamera c=layout.camera;
        float px=c.screenX(pointerX,pointerY),py=c.screenY(pointerX,pointerY);
        float dx=c.screenX(downX,downY),dy=c.screenY(downX,downY);
        layout=nextLayout;
        if(pointer && mapInRiver) {
            RiverCamera next=layout.camera;
            pointerX=next.worldX(px,py);pointerY=next.worldY(px,py);
            downX=next.worldX(dx,dy);downY=next.worldY(dx,dy);
        }
    }
    public String automationStatus() {
        if(practice!=null) return practice.automationStatus();
        RiverCamera c=layout.camera;
        Body load=null;for(Body b:cargo) if(b!=null && b.active) { load=b;break; }
        Body spare=cargo[2];Towline lead=tow.incoming(load),spareLead=tow.incoming(spare);
        return status()+"; cargo "+(load==null?"none":load.x+","+load.y)+"; tug "+tug.x+","+tug.y+"; gate "+(tutorial==null?0:tutorial.channelGate)
                +(tutorial==null?"":"; drive marker "+tutorial.driveMarker+"; orbit progress "+tutorial.orbitProgress()+"; drive target "+tutorial.driveTargetX()+","+tutorial.driveTargetY())
                +"; spare "+(spare==null?"none":spare.x+","+spare.y)
                +"; cargo id "+(load==null?-1:load.id)+"; cargo leader "+(lead==null?-1:lead.leader.id)
                +"; spare leader "+(spareLead==null?-1:spareLead.leader.id)
                +(tutorial==null?"":"; tow target "+tutorial.towTargetX()+","+tutorial.towTargetY(this))
                +"; cast phase "+ropePhase+"; joystick side "+settings.joystickSide()+"; camera "+c.left+","+c.top+","+c.scale+","+c.worldTop+","+c.worldRight;
    }
    public Game activeGame() { return practice==null?this:practice; }
    public void startTutorial() {
        cancelInputs(); accumulator=0; visualAccumulator=0;
        returnToShift=mode!=TITLE && mode!=FINISH && mode!=CHALLENGES;
        practice=new Game(); practice.reset(); practice.tutorial=new Tutorial();
        practice.restoreProgress(saveProgress());
        practice.settings=settings;
        practice.layout=layout.camera.zoom==1 && layout.camera.worldTop==148?layout:new Layout(layout.width,layout.height);
        practice.hapticsEnabled=hapticsEnabled;
        practice.tutorial.setup(practice);practice.fitTutorialCamera();
        practice.tutorial.resumeShift=returnToShift;
    }
    public void render(Painter painter) { Renderer.draw(this,painter); }
    public String status() {
        if(practice!=null) return practice.status();
        String state=mode==PLAY?"Working":mode==PAUSE?"Paused":mode==HELP?"Help":mode==SETTINGS?"Settings":mode==TITLE?"Ready":mode==CHALLENGES?"Challenges":"Finished";
        String activity=ropePhase==1?"; rope flying":ropePhase==2?"; rope retracting":ropePhase==3?ropeToTug?"; towline caught":"; bollard hit":"";
        if(tow.count()>0) activity="; towing "+tow.loadCount(tug)+" barges";
        for(Body b:cargo) if(b!=null && b.active && b.lineAttached && !b.deliveryRecorded) activity="; securing load";
        for(Body b:cargo) if(b!=null && b.active && Mooring.aligning(b)) activity="; drawing alongside";
        for(Body b:cargo) if(b!=null && b.active && b.unloadingTicks>0 && !b.deliveryRecorded) activity="; unloading";
        for(Body b:cargo) if(canTie(b)) { activity="; ready to tie "+b.berth; break; }
        return state+"; delivered "+delivered+"; score "+score+(tutorial==null?"":"; lesson "+tutorial.lesson)
                +"; drag controls"+activity+"; challenge "+challengeId+"; career stars "+career.totalStars()
                +"; feeder drops "+feederDrops()+"; rank "+career.rankTitle()+"; stripes "+career.stripes()+"; challenge name "+challengeTitle();
    }
    public void reset() {
        practice=null; tutorial=null; tow=new Towline(); towReleases=0; towAimBody=ropeTowTarget=null; ropeToTug=towAiming=false; pushing=false; ropeDragging=false; reversing=false; manualSteering=false; control=IDLE;
        mapPointer=-1; hapticPending=hapticCooldown=contactCooldown=0; contactId=-1;
        ticks=score=delivered=lost=combo=spawnIndex=0; hull=100; nextId=1;
        earnedStars=0; rankPromoted=false;cameraOverviewTicks=0;
        if(layout.camera.zoom!=1 || layout.camera.worldTop!=148 || layout.camera.worldRight!=460) layout=new Layout(layout.width,layout.height);
        bridge=new Bridge();buildingFades=new BuildingFades();
        water=new WaterSurface(); smoke=new VesselSmoke();titleSmoke=new VesselSmoke(); trafficBoat=new Body(-1,-1,110,TRAFFIC_Y,0);
        visualTicks=0; visualAccumulator=0; ropePhase=ropeTicks=0; ropeBody=null;ropeTowTarget=null; ropeTargeted=false;
        celebrationTicks=impactTicks=damageFlashTicks=0;
        damageCooldown=0; accumulator=0; selected=-1;
        tug=new Body(0,-1,330,650,(float)Math.PI);
        for(int i=0;i<cargo.length;i++) cargo[i]=null;
        feeders=challengeId==0?new Feeder[]{new Feeder(260)}:new Feeder[]{new Feeder(260),new Feeder(360)};
        feeders[0].request(2);if(feeders.length>1) feeders[1].request(challengeId==1?2:1);nextFeedTick=feedInterval();
        targetX=tug.x; targetY=tug.y; navigating=false; cancel();
        mode=PLAY; tell(challengeId==0?"ONE FEEDER / DELIVER TO BERTH A":"DELIVERY PUSHERS INBOUND - KEEP BOTH SLOTS CLEAR");
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
        cancel(); mapPointer=-1; hapticPending=0; reversing=false; navigating=false;
    }
    /** Native input is always in landscape canvas coordinates. */
    public void pointerDown(int id,float x,float y) {
        if(practice!=null) { practice.pointerDown(id,x,y); finishPractice(); return; }
        if(mapPointer!=-1 || pointer || x<0 || x>layout.width || y<0 || y>layout.height) return;
        if(Joystick.hit(this,x,y)) {
            beginDown(x,y,false);mapPointer=id;joystick.begin(this,x,y);haptic(HAPTIC_TICK);return;
        }
        boolean river=liveRiverInput() && layout.camera.contains(x,y);
        if(river && liveButton(x,y)) river=false;
        if(river) beginDown(layout.camera.worldX(x,y),layout.camera.worldY(x,y),true);
        else beginDown(x,y,false);
        if(pointer) mapPointer=id;
    }
    public void pointerMove(int id,float x,float y) {
        if(practice!=null) { practice.pointerMove(id,x,y); return; }
        if(id!=mapPointer) return;
        if(joystick.active){pointerX=x;pointerY=y;joystick.move(this,x,y);return;}
        if(mapInRiver) move(layout.camera.worldX(x,y),layout.camera.worldY(x,y)); else move(x,y);
    }
    public void pointerUp(int id,float x,float y) {
        if(practice!=null) { practice.pointerUp(id,x,y); finishPractice(); return; }
        if(id!=mapPointer) return;
        mapPointer=-1;
        if(joystick.active){cancel();return;}
        if(mapInRiver) up(layout.camera.worldX(x,y),layout.camera.worldY(x,y)); else up(x,y);
    }
    private boolean liveButton(float x,float y) {
        return layout.box(Layout.SETTINGS).contains(x,y) || layout.box(Layout.HAPTICS).contains(x,y) || layout.box(Layout.PAUSE).contains(x,y)
                || layout.box(Layout.HELP).contains(x,y)
                || layout.box(Layout.PRACTICE).contains(x,y);
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
        if(pushing || joystick.active) { navigating=false; reversing=false; control=COASTING; }
        joystick.cancel();
        pointer=false; mapPointer=-1; mapInRiver=false; cargoPushGesture=false; pushing=false; ropeDragging=false; towAiming=false; towAimBody=null; navigating=false; held=-1; pressTicks=0; manualSteering=false;
    }
    public boolean holdingTug() { return pointer && held==-2; }
    public boolean canTie(Body b) { return ropePhase==0 && tieEligible(b); }
    boolean inTieArea(Body b) {
        return Math.abs(b.x-DOCK_X[b.berth])<=55 && Math.abs(b.y-dockY(b.berth))<=53;
    }
    private boolean tieEligible(Body b) {
        // The winch aligns the hull after a catch. Requiring the complete hull to
        // fit here rejects vessels touching the bank or arriving at an angle.
        return b!=null && b.active && !b.lineAttached
                && (!b.awaitingPickup || tutorial!=null) // Practice parks the teaching hull in place.
                && inTieArea(b)
                && (tutorial==null || tutorial.lesson==Tutorial.TIE && b==cargo[0]);
    }
    private boolean ropeWithinReach(Body b) {
        return b!=null && b.active && !b.lineAttached
                && Math.abs(b.x-DOCK_X[b.berth])<105 && Math.abs(b.y-dockY(b.berth))<105;
    }
    /** Raised quay fittings share the exact projection used by touch and rope drawing. */
    public float bollardX(int berth) { return TIE_X[berth]+(tutorial!=null?RiverCamera.riseX(GroundArt.LAND_RISE):0); }
    public float bollardY(int berth) { return dockY(berth)+(tutorial!=null?RiverCamera.riseY(GroundArt.LAND_RISE):0); }
    public float ropeOriginX() { return ropeBody.x+ropeLocalX*(float)StrictMath.cos(ropeBody.angle)-ropeLocalY*(float)StrictMath.sin(ropeBody.angle); }
    public float ropeOriginY() { return ropeBody.y+ropeLocalX*(float)StrictMath.sin(ropeBody.angle)+ropeLocalY*(float)StrictMath.cos(ropeBody.angle); }
    public boolean overTiePoint(Body b,float x,float y) {
        return Physics.length(x-bollardX(b.berth),y-bollardY(b.berth))<=TIE_RADIUS;
    }
    public float aimX(Body b,float x,float y) {
        return overTiePoint(b,x,y)?bollardX(b.berth):Physics.clamp(x,TIE_X[0]-TIE_RADIUS,TIE_X[1]+TIE_RADIUS);
    }
    public float aimY(Body b,float x,float y) {
        return overTiePoint(b,x,y)?bollardY(b.berth):Physics.clamp(y,channelTop()-47,816);
    }
    public Body heldCargo() { return held>=0 && cargo[held]!=null && cargo[held].active?cargo[held]:null; }
    public Body workingCargo() { return heldCargo(); }
    public boolean canTow(Body b) {
        if(pointer && cargoPushGesture && heldCargo()==b || ropePhase!=0 || tutorial!=null && tutorial.lesson<Tutorial.CONNECT)return false;
        if(canTowAtRelease(b,tug))return true;
        for(Body parent:cargo)if(canTowAtRelease(b,parent))return true;
        return false;
    }
    public boolean canTowTo(Body b,Body parent){return canTow(b) && canTowAtRelease(b,parent);}
    public Body towTargetBody(Body b,float x,float y) {
        if(!canTow(b) || Physics.length(x-downX,y-downY)<=26)return null;
        Body best=null;float distance=Float.MAX_VALUE;
        for(int i=-1;i<cargo.length;i++) {
            Body parent=i<0?tug:cargo[i];
            if(canTowAtRelease(b,parent) && parent.hit(x,y,22)) {
                float d=Physics.length(x-parent.x,y-parent.y);if(d<distance){best=parent;distance=d;}
            }
        }
        return best;
    }
    public boolean towTarget(Body b,float x,float y){return towTargetBody(b,x,y)!=null;}
    private boolean canTowAtRelease(Body b,Body parent) {
        return tow.canAttach(this,b,parent) && Physics.length(parent.x-b.x,parent.y-b.y)<=(parent==tug?170:220)
                && Towline.clear(this,b.x,b.y,parent.x,parent.y);
    }
    public void releaseTow(){releaseTow(tow.primary());}
    public void releaseTow(Body b) {
        if(tow.incoming(b)!=null) {
            towReleases++;
            if(ropeToTug && ropePhase==3 && ropeBody==b){ropePhase=ropeTicks=0;ropeBody=null;ropeTowTarget=null;}
            tow.detach(this,b);tell("LINK RELEASED / DOWNSTREAM BARGES STAY LINKED");
        }
    }

    public float holdProgress() { return pointer && held!=-1?Math.min(1,pressTicks/12f):0; }
    public void background() { if(practice!=null) { practice.background(); return; } cancelInputs(); accumulator=0; visualAccumulator=0; if(mode==PLAY) mode=PAUSE; }
    public void pause() { if(practice!=null) { practice.pause(); return; } cancelInputs(); accumulator=0; visualAccumulator=0; if(mode==PLAY) mode=PAUSE; else if(mode==PAUSE) mode=PLAY; }
    /** Direct/replay input uses world coordinates on the board, canvas coordinates in menus. */
    public void down(float x,float y) {
        if(practice!=null) { practice.down(x,y); return; }
        boolean river=liveRiverInput() && worldTouch(x,y);
        beginDown(x,y,river);
    }
    private boolean worldTouch(float x,float y) {
        return x>=(tutorial!=null?TrainingChannel.BASIN_LEFT-40:RiverPainter.WORLD_LEFT)
                && x<=RiverPainter.WORLD_RIGHT
                && y>=channelTop()-67 && y<=RiverPainter.WORLD_BOTTOM;
    }
    private void beginDown(float x,float y,boolean river) {
        if(pointer) return;
        if(river) {
            if(!worldTouch(x,y)) return;
        } else if(x<0 || x>layout.width || y<0 || y>layout.height) return;
        pointer=true; tapCandidate=true; cargoPushGesture=false; mapInRiver=river; pointerX=downX=x; pointerY=downY=y; pressTicks=0; held=-1;
        if(!river) return;
        // Pick the nearest eligible hull before classifying the gesture. A
        // connected neighbor's padded hit area must not steal the spare's center
        // while the player is reconfiguring a close pair of barges.
        int cargoHit=-1;float closest=Float.MAX_VALUE;
        for(int i=0;i<cargo.length;i++) {
            Body b=cargo[i];if(b==null || !b.active || !b.hit(x,y,tow.incoming(b)!=null?12:22))continue;
            float distance=Physics.length(x-b.x,y-b.y);
            if(distance<closest){closest=distance;cargoHit=i;}
        }
        if(tug.hit(x,y,8) && Physics.length(x-tug.x,y-tug.y)<=closest) {
            held=-2;reversing=false;control=HOLDING;haptic(HAPTIC_TICK);return;
        }
        if(cargoHit>=0) {
            Body b=cargo[cargoHit];
            if(b.lineAttached || ((ropePhase==1 || ropePhase==2) && b==ropeBody)){selected=cargoHit;return;}
            held=selected=cargoHit;navigating=false;reversing=false;control=HOLDING;
            ropeDragging=canTie(b);haptic(HAPTIC_TICK);
        }
    }

    public void move(float x,float y) {
        if(practice!=null) { practice.move(x,y); return; }
        if(pointer) {
            pointerX=x; pointerY=y;
            if(!mapInRiver) return;
            float distance=Physics.length(x-downX,y-downY);
            if(distance>22) tapCandidate=false;
            Body b=heldCargo();
            if(canTow(b) && distance>26) {
                Body target=towTargetBody(b,x,y);float best=.88f;
                if(target==null)for(int i=-1;i<cargo.length;i++) {
                    Body parent=i<0?tug:cargo[i];if(!canTowAtRelease(b,parent))continue;
                    float tx=parent.x-downX,ty=parent.y-downY;
                    float alignment=((x-downX)*tx+(y-downY)*ty)/(distance*Math.max(1,Physics.length(tx,ty)));
                    if(alignment>best){target=parent;best=alignment;}
                }
                if(target!=null){towAiming=true;towAimBody=target;}
            }
            if(distance<20 || (b!=null && canTie(b) && overTiePoint(b,x,y))) towAiming=false;
            if(towAiming) { pushing=false;navigating=false;reversing=false; }
            else if(b!=null && !ropeDragging && distance>26) cargoPushGesture=true;
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
        if(mapInRiver && tow.incoming(heldCargo())!=null
                && tapCandidate && Physics.length(x-downX,y-downY)<22) {
            if(pressTicks<=18) releaseTow(heldCargo());
            cancel();return;
        }
        if(towAiming || towTarget(heldCargo(),x,y) || ropeDragging) {
            Body b=heldCargo();
            if(b!=null && ropePhase==0) {
                ropeTowTarget=towTargetBody(b,x,y);
                ropeToTug=towAiming || ropeTowTarget!=null;
                ropeBody=b; ropePhase=1; ropeTicks=0;
                ropeTargeted=ropeToTug?ropeTowTarget!=null && canTowAtRelease(b,ropeTowTarget):overTiePoint(b,x,y) && ropeWithinReach(b);
                ropeEndX=ropeToTug?(ropeTargeted?ropeTowTarget.x:x):aimX(b,x,y);
                ropeEndY=ropeToTug?(ropeTargeted?ropeTowTarget.y:y):aimY(b,x,y);
                LassoPath.edge(b,ropeEndX,ropeEndY,route);
                ropeStartX=route[0];ropeStartY=route[1];
                float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
                float dx=ropeStartX-b.x,dy=ropeStartY-b.y;
                ropeLocalX=dx*c+dy*s;ropeLocalY=-dx*s+dy*c;
                ropeStartHeight=LassoPath.DECK_HEIGHT+WaterSurface.lift(this,b);
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
        if(mode==SETTINGS) {
            if(tap) {
                if(tapped(Layout.JOYSTICK_LEFT,x,y))settings.setJoystickSide(GameSettings.LEFT);
                else if(tapped(Layout.JOYSTICK_RIGHT,x,y))settings.setJoystickSide(GameSettings.RIGHT);
                else if(tapped(Layout.JOYSTICK_OFF,x,y))settings.setJoystickSide(GameSettings.OFF);
                else if(tapped(Layout.SETTINGS_BACK,x,y))mode=settingsReturnMode;
            }
            cancel();return;
        }
        if(tap && tapped(Layout.SETTINGS,x,y)){openSettings();return;}
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
            else if(tap && tapped(Layout.CHALLENGES,x,y)) mode=CHALLENGES;
        } else if(mode==CHALLENGES) {
            if(tap && tapped(Layout.BACK,x,y)) mode=TITLE;
            else if(tap && tapped(Layout.CHALLENGE_START,x,y)) reset();
            else if(tap) for(int i=0;i<Challenges.COUNT;i++) {
                if(tapped(Layout.CHALLENGE_CARDS[i],x,y) && selectChallenge(i)) haptic(HAPTIC_TICK);
            }
        } else if(mode==FINISH) {
            if(tap && tapped(Layout.AGAIN,x,y)) reset();
            else if(tap && tapped(Layout.FINISH_CHALLENGES,x,y)) mode=CHALLENGES;
            else if(tap && tapped(Layout.NEXT_CHALLENGE,x,y) && selectChallenge(challengeId+1)) reset();
        } else if(mode==PAUSE) {
            if(tap && tapped(Layout.RESUME,x,y)) pause();
            else if(tap && tapped(Layout.RESTART,x,y)) {
                boolean training=tutorial!=null, resume=training && tutorial.resumeShift;
                reset();
                if(training) { tutorial=new Tutorial(); tutorial.resumeShift=resume; tutorial.setup(this);fitTutorialCamera(); }
            }
        } else if(tap) {
            if(tapped(Layout.PAUSE,x,y)) pause();
            else if(tapped(Layout.HELP,x,y)) {
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
    /** Actual screen target, including title-only right anchoring. */
    public Layout.Box controlBox(Layout.Box box) {return mode==TITLE?layout.titleBox(box):layout.box(box);}
    private boolean tapped(Layout.Box box,float x,float y) {
        box=controlBox(box);
        return box.contains(downX,downY) && box.contains(x,y);
    }
    /** Bounded catch-up protects against a stalled host. Hosts must cancel on background. */
    public void advance(double seconds) {
        if(practice!=null) { practice.advance(seconds); return; }
        if(Double.isNaN(seconds) || seconds<=0) return;
        if(mode!=PLAY) {
            accumulator=0;
            if(mode==TITLE || mode==FINISH || mode==CHALLENGES) {
                visualAccumulator+=Math.min(.25,seconds);
                while(visualAccumulator+1e-10>=1.0/60.0) {
                    visualAccumulator-=1.0/60.0; visualTicks++;
                    if(mode==TITLE)titleSmoke.titleStep(this);else {smoke.drift();buildingFades.step(this);}
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
            smoke.step(this);buildingFades.step(this);
            if(celebrationTicks>0) celebrationTicks--;
            if(impactTicks>0) impactTicks--;
            return;
        }
        ticks++;if(damageFlashTicks>0) damageFlashTicks--; if(celebrationTicks>0) celebrationTicks--; if(impactTicks>0) impactTicks--;
        if(noticeTicks>0) noticeTicks--; if(damageCooldown>0) damageCooldown--;
        if(hapticCooldown>0) hapticCooldown--; if(contactCooldown>0) contactCooldown--;
        if(pointer) {
            pressTicks++;
            if(pressTicks>=12 && held!=-1 && !ropeDragging && !towAiming && tow.incoming(heldCargo())==null) {
                if(!pushing) haptic(HAPTIC_TICK);
                pushing=true;reversing=false;
                if(held>=0) cargoPushGesture=true;
            }
        }
        bridge.step(this);
        for(int i=0;i<MOTION_STEPS;i++) motionStep();
        if(tutorial==null) for(Feeder feeder:feeders) {
            feeder.step(this);
            float load=feeder.phase==Feeder.IDLE || feeder.blocked?0:feeder.phase==Feeder.INBOUND?.9f:.45f;
            feeder.boat.engineLoad+=(load-feeder.boat.engineLoad)*.08f;
        }
        water.step(this);
        smoke.step(this);
        tow.tick(this);
        updateRope();
        for(int i=0;i<cargo.length;i++) {
            Body b=cargo[i]; if(b==null || !b.active) continue;
            if(b.lineSnapTicks>0) b.lineSnapTicks--;
            if(b.tieBurstTicks>0) b.tieBurstTicks--;
            if(tutorial==null && !b.lineAttached) b.age+=Physics.DT;
            if(b.lineAttached) { if(!b.deliveryRecorded) Mooring.step(this,b); }
            else { b.dockTime=0; b.lineAttached=false; }
            if(b.unloadingTicks>=UNLOAD_TICKS && !b.deliveryRecorded) {
                b.deliveryRecorded=true;b.active=tutorial!=null;cameraOverviewTicks=180; delivered++; combo++; int points=300+Math.max(0,180-(int)b.age)*2+Math.min(4,combo)*50;
                celebrationTicks=90; celebrationPoints=points; celebrationX=b.x; celebrationY=b.y;
                score+=points; tell("DELIVERED +"+points+" / "+delivered+" OF "+quota());
                haptic(HAPTIC_SUCCESS);
                if(selected==i) selected=-1;
                if(held==i) { cancel(); navigating=false; }
            } else if(!b.lineAttached && b.age>contractSeconds()) {
                b.active=false; lost++; combo=0; score=Math.max(0,score-100); tell("CONTRACT EXPIRED -100");
                if(held==i) { cancel(); navigating=false; } if(selected==i) selected=-1;
            }
        }
        tow.prune(this);
        if(tutorial!=null) tutorial.update(this);
        else {
            if(ticks>=nextFeedTick && ticks<shiftTicks()-1800 && spawn()) nextFeedTick=ticks+feedInterval();
            if(ticks>=shiftTicks() || hull<=0) {
                mode=FINISH; cancel();
                if(ticks>=shiftTicks() && hull>0) {
                    earnedStars=Challenges.stars(challengeId,delivered,lost,hull);
                    int before=career.rank(); career.record(challengeId,earnedStars,score);
                    rankPromoted=career.rank()>before;
                }
            }
        }
        updateCamera();
        buildingFades.step(this);
    }
    /** Rope flight, catch and recoil are real-time events, independent of 2x boat motion. */
    private void updateRope() {
        if(ropePhase==0 || ropeBody==null) return;
        ropeTicks++;
        // A snapped vessel target keeps its identity while moving during the throw.
        if(ropePhase==1 && ropeToTug && ropeTargeted && ropeTowTarget!=null && ropeTowTarget.active) {
            ropeEndX=ropeTowTarget.x;ropeEndY=ropeTowTarget.y;
        }
        if(ropePhase==1 && ropeTicks>=ROPE_FLIGHT_TICKS) {
            if(ropeTargeted && (ropeToTug?canTowAtRelease(ropeBody,ropeTowTarget) && ropeTowTarget.hit(ropeEndX,ropeEndY,26):ropeWithinReach(ropeBody))) {
                Body b=ropeBody;
                if(ropeToTug) tow.attach(this,b,ropeTowTarget);
                else { tow.detachBody(this,b); Mooring.attach(b); }
                ropePhase=3;ropeTicks=0;haptic(HAPTIC_CONTACT);
            } else { ropePhase=2; ropeTicks=0; }
        } else if((ropePhase==2 && ropeTicks>=ROPE_RETURN_TICKS)
                || (ropePhase==3 && ropeTicks>=ROPE_HIT_TICKS)) {
            ropePhase=ropeTicks=0; ropeBody=null;ropeTowTarget=null;
        }
    }
    private void motionStep() {
        engineDemand=0;reversing=false;
        if(joystick.active) {
            float throttle=joystick.throttle();
            if(throttle>0) {
                float dx=(joystick.x-joystick.y/RiverCamera.DEPTH)/(2*RiverCamera.Q);
                float dy=(joystick.x+joystick.y/RiverCamera.DEPTH)/(2*RiverCamera.Q);
                float len=Math.max(.001f,Physics.length(dx,dy));
                navigating=true;targetX=tug.x+dx/len*200;targetY=tug.y+dy/len*200;
                reversing=dx*(float)StrictMath.cos(tug.angle)+dy*(float)StrictMath.sin(tug.angle)<-.5f*len;
                control=reversing?REVERSING:MOVING;
                navigate(targetX,targetY,(reversing?60:88)*throttle,reversing);
            } else {
                navigating=false;control=tug.speed()>3?COASTING:IDLE;
                tug.vx*=.985f;tug.vy*=.985f;tug.spin*=.985f;
            }
        } else if(pushing && held>=0 && cargo[held]!=null && cargo[held].active) pushCargo(cargo[held]);
        else if(pushing && held==-2) {
            float dx=pointerX-downX,dy=pointerY-downY,len=Physics.length(dx,dy);
            if(manualSteering) {
                navigating=true; reversing=false; control=MOVING;
                targetX=tug.x+dx/len*200; targetY=tug.y+dy/len*200;
                reversing=dx*(float)StrictMath.cos(tug.angle)+dy*(float)StrictMath.sin(tug.angle)<-.5f*len;
                control=reversing?REVERSING:MOVING;
                navigate(targetX,targetY,(reversing?60:88)*Physics.clamp((len-26)/60,0,1),reversing);
            } else {
                navigating=false;control=tug.speed()>3?COASTING:IDLE;
                tug.vx*=.985f;tug.vy*=.985f;tug.spin*=.985f;
            }
        } else {
            float drag=.985f;
            tug.vx*=drag; tug.vy*=drag; tug.spin*=drag;
            control=pointer && held!=-1?HOLDING:tug.speed()>3?COASTING:IDLE;
        }
        tow.step(this);
        engineDemand=Math.max(engineDemand,Math.min(1,tow.pullLoad(tug)/180));
        Physics.integrate(tug);
        if(tutorial!=null) TrainingChannel.collideWater(tug);
        else damage(Physics.banks(tug,channelTop(),Physics.BOTTOM,challengeId==2));
        if(hasBridge()) damage(bridge.collide(tug));
        for(Body b:cargo) if(b!=null && b.active && !b.lineAttached && !b.awaitingPickup) { Physics.integrate(b); if(tutorial!=null) TrainingChannel.collideWater(b);else damage(Physics.banks(b,channelTop(),Physics.BOTTOM,challengeId==2));if(hasBridge()) damage(bridge.collide(b)); }
        for(int iteration=0;iteration<3;iteration++) {
            for(int i=0;i<cargo.length;i++) {
                Body a=cargo[i]; if(a==null || !a.active) continue;
                damage(Physics.collide(tug,a));
                for(int j=i+1;j<cargo.length;j++) if(cargo[j]!=null) damage(Physics.collide(a,cargo[j]));
            }
        }
        if(tutorial==null) for(Feeder f:feeders) if(f.phase!=Feeder.IDLE) {
            feederContact(f.boat);
            if(f.load!=null) feederContact(f.load);
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
        if(contact!=null && pushing && engineDemand>0) engineDemand=Math.max(engineDemand,.88f);
        tug.engineLoad+=(engineDemand-tug.engineLoad)*(engineDemand>tug.engineLoad?.08f:.025f);
        for(Body b:cargo) if(b!=null && b.active && (b.lineAttached || b.awaitingPickup)) {
            b.x=b.tiedX; b.y=b.tiedY; b.angle=b.tiedAngle; b.vx=b.vy=b.spin=0;
        }
        // Fender damping follows motion time; mooring confirmation uses real time.
        for(Body b:cargo) if(b!=null && b.active) {
            if(inDock(b) && b.speed()<65) { b.vx*=.990f; b.vy*=.990f; b.spin*=.985f; }
        }
    }
    private void feederContact(Body obstacle) {
        float x=obstacle.x,y=obstacle.y,vx=obstacle.vx,vy=obstacle.vy,spin=obstacle.spin;
        damage(Physics.collide(tug,obstacle));
        obstacle.x=x; obstacle.y=y; obstacle.vx=vx; obstacle.vy=vy; obstacle.spin=spin;
        for(Body b:cargo) if(b!=null && b.active && !b.lineAttached && !b.awaitingPickup) {
            Physics.collide(b,obstacle);
            obstacle.x=x; obstacle.y=y; obstacle.vx=vx; obstacle.vy=vy; obstacle.spin=spin;
        }
    }
    public boolean inDock(Body b) {
        for(int i=0;i<3;i++) if(Math.abs(b.discX(i)-DOCK_X[b.berth])+b.radius>55
                || Math.abs(b.discY(i)-dockY(b.berth))+b.radius>53) return false;
        return true;
    }
    private boolean spawn() {
        for(int i=0;i<feeders.length;i++) {
            Feeder feeder=feeders[(spawnIndex+i)%feeders.length];
            if(feeder.phase==Feeder.IDLE) {
                feeder.request(challengeId<2?2:spawnIndex%3); spawnIndex++; return true;
            }
        }
        return false;
    }
    private void navigate(float x,float y,float topSpeed) { navigate(x,y,topSpeed,false); }
    private void navigate(float x,float y,float topSpeed,boolean astern) {
        float dx=x-tug.x, dy=y-tug.y, distance=Physics.length(dx,dy);
        if(distance<1) { tug.vx*=.93f; tug.vy*=.93f; return; }
        float desired=(float)StrictMath.atan2(dy,dx)+(astern?(float)Math.PI:0), error=Physics.angle(desired-tug.angle);
        tug.spin+=Physics.clamp(error*6-tug.spin*3,-7,7)*Physics.DT;
        float load=tow.loadMass(tug);
        float speed=Math.min(topSpeed*tow.speedFactor(tug),(float)StrictMath.sqrt(distance*100));
        speed*=Math.max(.1f,1-Math.abs(error)*.45f);
        float dvx=dx/distance*speed-tug.vx, dvy=dy/distance*speed-tug.vy;
        float force=Physics.length(dvx,dvy), maxChange=82*Physics.DT/(1+.05f*load);
        engineDemand=Physics.clamp(.15f+.4f*speed/95+.45f*Math.min(1,force/maxChange),0,1);
        if(force>maxChange) { dvx*=maxChange/force; dvy*=maxChange/force; }
        tug.vx+=dvx; tug.vy+=dvy;
    }
    private void pushCargo(Body b) {
        float gx=DOCK_X[b.berth], gy=dockY(b.berth);
        // Drag from the original contact point to override the assisted berth heading.
        float dragX=pointerX-downX, dragY=pointerY-downY;
        boolean steering=manualSteering;
        if(steering) { gx=b.x+dragX*2; gy=b.y+dragY*2; }
        boolean waitingBridge=!steering && bridgeStage() && !bridge.passable() && bridge.separates(b.y,gy);
        if(waitingBridge) { gx=300;gy=Bridge.Y+(b.y>Bridge.Y?90:-90); }
        if(waitingBridge && Physics.length(gx-b.x,gy-b.y)<22) {
            b.vx*=.85f;b.vy*=.85f;b.spin*=.85f;tug.vx*=.85f;tug.vy*=.85f;tug.spin*=.85f;
            control=WAITING_BRIDGE;pushX=0;pushY=b.y>Bridge.Y?-1:1;return;
        }
        // Holding assist deliberately settles a fitting load; released cargo keeps its own glide.
        if(!steering && inDock(b)) { b.vx*=.93f;b.vy*=.93f;b.spin*=.94f; }
        float finalDistance=Physics.length(gx-b.x,gy-b.y);
        if(!steering && challengeId==2 && tutorial==null) {
            HarborGeometry.waypoint(b.x,b.y,gx,gy,b.half+b.radius+4,channelTop(),Physics.BOTTOM,route); gx=route[0]; gy=route[1];
        }
        float dx=gx-b.x, dy=gy-b.y, distance=Physics.length(dx,dy);
        pushX=dx/Math.max(1,distance); pushY=dy/Math.max(1,distance);
        if(finalDistance<3 && !steering) {
            control=DOCKING;
            // Mooring assist only inside the correct berth; it never transports cargo.
            b.vx*=.97f; b.vy*=.97f; b.spin*=.98f;
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
            guideX=Physics.clamp(sx,198,402); guideY=Physics.clamp(sy,channelTop()+30,787);
            control=steering?STEERING:ALIGNING;
            if(!steering && challengeId==2 && tutorial==null) {
                HarborGeometry.waypoint(tug.x,tug.y,guideX,guideY,tug.half+tug.radius+2,channelTop(),Physics.BOTTOM,route);
                navigate(route[0],route[1],95);
            } else navigateToLoad(guideX,guideY,95);
        } else {
            if(Physics.touching(tug,b,3)) b.awaitingPickup=false;
            guideX=b.x-nx*reach; guideY=b.y-ny*reach;
            control=steering?STEERING:!Physics.touching(tug,b,3)?ALIGNING:inDock(b)?DOCKING:PUSHING;
            navigateToLoad(b.x+nx*26,b.y+ny*26,Math.min(76,18+distance*.85f));
            // A broad fender steadies the contact, while collisions still supply linear momentum.
            float heading=(float)StrictMath.atan2(ny,nx);
            b.spin+=Physics.clamp(Physics.angle(heading-b.angle)*1.25f-b.spin*1.1f,-.75f,.75f)*Physics.DT;
            if(distance<43 && !steering) { b.vx*=.988f; b.vy*=.988f; }
        }
    }
    /** Assisted pickup enters through the same open mouth the player can sail. */
    private void navigateToLoad(float x,float y,float speed) {
        if(tutorial!=null && x>=Physics.LEFT && tug.x<Physics.LEFT+40) {
            if(tug.x<TrainingChannel.X[TrainingChannel.X.length-1]-23) {
                int next=1;
                while(next<TrainingChannel.X.length-1 && tug.x>TrainingChannel.X[next]+23) next++;
                TrainingChannel.guide(tug.x,tug.y,next,route);
                navigate(route[0],route[1],Math.min(speed,45));
            } else navigate(Physics.LEFT+60,TrainingChannel.Y[TrainingChannel.Y.length-1],speed);
        } else navigate(x,y,speed);
    }
    private void damage(float speed) {
        if(tutorial!=null) return;
        if(speed<78 || damageCooldown>0) return;
        hull=Math.max(0,hull-9); combo=0; damageCooldown=75;damageFlashTicks=40; tell("HARD IMPACT - HULL -9");
        impactX=tug.x; impactY=tug.y; impactTicks=45;
        haptic(HAPTIC_IMPACT);
    }
    public int trafficPhase() { return ticks%(28*60); }
    public boolean trafficWarning() { return challengeId==2 && tutorial==null && ticks>=600 && trafficPhase()>=900 && trafficPhase()<1080; }
    public boolean trafficActive() { return challengeId==2 && tutorial==null && ticks>=600 && trafficPhase()>=1080 && trafficPhase()<1080+400/MOTION_STEPS; }
    public float trafficX() { return 110+(trafficPhase()-1080)*.95f*MOTION_STEPS; }
    private void traffic() {
        if(!trafficActive()) return;
        Body ship=trafficBoat;ship.x=trafficX();ship.y=TRAFFIC_Y;ship.angle=0;ship.vx=57;ship.vy=0;
        ship.engineLoad=.55f;
        float beforeX=ship.x, beforeY=ship.y;
        float hit=Physics.collide(ship,tug);
        for(Body b:cargo) if(b!=null && b.active) hit=Math.max(hit,Physics.collide(ship,b));
        boolean collided=hit>25 || Math.abs(ship.x-beforeX)+Math.abs(ship.y-beforeY)>2;
        ship.x=beforeX;ship.y=beforeY;
        if(collided) damage(90);
    }
}
