package com.bargeboss.game;

/** All gameplay, timers, gestures and lifecycle behavior live here, never in native hosts. */
public final class Game {
    /** Developer render exclusions; zero in ordinary play, never read by simulation. */
    public int renderExclusions;
    BridgePassage passages;
    public static final int TITLE=0, PLAY=1, PAUSE=2, FINISH=3, HELP=4, CHALLENGES=5, SETTINGS=6;
    public static final int SHIFT_TICKS=180*60, QUOTA=2;
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
    public Buoys buoys=new Buoys();
    public MooredFleet mooredFleet=new MooredFleet();
    public CollisionSplashes splashes=new CollisionSplashes();
    public VesselSmoke smoke=new VesselSmoke(),titleSmoke=new VesselSmoke();
    public BuildingFades buildingFades=new BuildingFades();
    DockView.Crane[] dockCranes;
    public Bridge bridge=new Bridge();
    public Lock lock=new Lock();
    public MontroseHarbor mission=new MontroseHarbor();
    public MontroseRescue rescue=new MontroseRescue();
    public CalumetVoyage voyage=new CalumetVoyage();
    public boolean montroseComplete;
    private int montroseJobScore;
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
    // First deliberate thrust input owns forward/reverse until this pointer ends.
    private int driveDirection;
    public int driveDirection(){return driveDirection;}
    void selectDriveDirection(float dx,float dy){
        if(driveDirection==0)driveDirection=Joystick.reverseWorld(tug.angle,dx,dy)?-1:1;
    }

    // A committed cargo push owns this touch until lift/cancel. Moving vessels
    // must not reclassify the same steering finger as a towline throw.
    private boolean cargoPushGesture;
    public float guideX, guideY, pushX=-1, pushY;
    public Tutorial tutorial;
    public final Dialogue dialogue=new Dialogue();
    public final SoundEffects sounds=new SoundEffects();
    public final RadioAudio radio=new RadioAudio(sounds);
    public Body teacherBoat(){return tutorial==null || tutorial.arrival==null?null:tutorial.arrival.teacher;}
    public boolean conversation(){return dialogue.active || tutorial!=null && tutorial.entering;}
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
    public int cameraZoomHoldTicks;
    final PinchZoom pinchZoom=new PinchZoom();
    private boolean cameraTransit;
    public static final float TRAFFIC_Y=420;
    private double accumulator, visualAccumulator;

    public Layout layout=Layout.DEFAULT;
    public final Career career=new Career();
    public GameSettings settings=new GameSettings();
    public final Joystick joystick=new Joystick();
    public final OverviewMap overview=new OverviewMap();
    private int settingsReturnMode=TITLE;
    public int challengeId,earnedStars,challengePage;
    public boolean rankPromoted;
    public Game() { reset(); mode=TITLE; }
    public int quota() { return calumetStage()?voyage.quota():Challenges.quota(challengeId); }
    public int shiftTicks() { return calumetStage()?6000*60:Challenges.shiftTicks(challengeId); }
    public String challengeTitle() { return calumetStage()?"CALUMET RIVER / "+voyage.title():Challenges.title(challengeId); }
    public boolean calumetStage() {return tutorial==null && Challenges.calumet(challengeId);}
    public boolean hasBridge() { return tutorial!=null || challengeId<0; }
    /** The practice bridge is separate from surveyed location bridge instances. */
    public boolean bridgeStage() { return false; }
    public boolean indianaStage() {return tutorial==null && Challenges.kind(challengeId)==Challenges.INDIANA;}
    public boolean montroseStage() {return tutorial==null && Challenges.kind(challengeId)==Challenges.MONTROSE;}
    public boolean lockStage() { return false; }
    public boolean sharedBerthStage() { return calumetStage() && voyage.job==1; }
    public boolean depthStage() { return calumetStage() && voyage.job==3; }
    public boolean orderStage() { return sharedBerthStage() || depthStage(); }
    public boolean berthDisplayEnabled(int berth) { return berthEnabled(berth) && (!sharedBerthStage() || berth==2); }
    public float dockX(int berth) { return calumetStage()?voyage.dockX(voyage.destination(berth)):MontroseRescue.cruisers(this)?MontroseRescue.berth(berth).x:indianaStage()?IndianaHarbor.DOCK_X:montroseStage()?MontroseHarbor.SLIP_X:orderStage()?PhysicalBerths.dockX(challengeId,berth):DOCK_X[berth]; }
    public float dockHalfWidth() { return calumetStage()?CalumetVoyage.HALF_WIDTH:orderStage()?PhysicalBerths.HALF_WIDTH:55; }
    public boolean berthRight(int berth) { return calumetStage()?voyage.normalX(voyage.destination(berth))>0:!orderStage() && berth==1; }
    public Body berthBlocker(int berth,Body incoming) {
        if(calumetStage())return voyage.blocker(this,incoming==null?voyage.destination(berth):incoming.destination,incoming);
        float x=dockX(berth),y=dockY(berth),half=dockHalfWidth();
        for(Body other:cargo)if(other!=null && other!=incoming && other.active)for(int i=0;i<3;i++) {
            float dx=other.discX(i)-Physics.clamp(other.discX(i),x-half,x+half);
            float dy=other.discY(i)-Physics.clamp(other.discY(i),y-53,y+53);
            if(dx*dx+dy*dy<other.radius*other.radius)return other;
        }
        return null;
    }
    public boolean berthEnabled(int berth) { return calumetStage()?voyage.berthEnabled(berth):MontroseRescue.cruisers(this)?berth>=0 && berth<2:indianaStage()?berth==0:montroseStage()?berth==2:tutorial!=null?berth==2:!lockStage() && (orderStage() || berth==2 || berth==1); }
    public char berthLetter(int berth) { return tutorial!=null?'A':orderStage()?(berth==2?'A':berth==0?'B':'C'):berth==1?'B':'A'; }
    public float dockY(int berth) { return calumetStage()?voyage.dockY(voyage.destination(berth)):MontroseRescue.cruisers(this)?MontroseRescue.berth(berth).y:indianaStage()?IndianaHarbor.DOCK_Y:montroseStage()?MontroseHarbor.SLIP_Y:orderStage()?PhysicalBerths.dockY(challengeId,berth):Challenges.kind(challengeId)==Challenges.TWO_BANKS && tutorial==null && berth==2?290:DOCK_Y[berth]; }
    public float channelTop() { return calumetStage()?CalumetRiver.MIN_Y:indianaStage()?IndianaHarbor.MIN_Y:montroseStage()?-1600:TrainingChannel.TOW_END; }
    /** Kept as a score-age scale for legacy previews; cargo never expires. */
    public int contractSeconds() { return 68; }
    public int feedInterval() { return 14*60; }
    public int nextRequiredBerth() {
        if(!orderStage())return -1;
        for(int berth:new int[]{2,0,1}) {
            boolean done=false;for(Body b:cargo)if(b!=null && b.berth==berth && b.deliveryRecorded && (!calumetStage() || b.voyageJob==voyage.job))done=true;
            if(!done)return berth;
        }
        return -1;
    }
    public boolean correctOrder(Body b) { return true; }
    public int goalProgress() { return calumetStage()?voyage.progress(this):lockStage()?lock.clearedCount(this):delivered; }
    public boolean goalComplete() { return tutorial==null && (calumetStage()?voyage.complete:montroseStage()?montroseComplete:lockStage()?lock.complete():delivered>=quota()); }
    public boolean goalFailed() { return hull<=0 || MontroseRescue.swimmers(this) && rescue.failed || lockStage() && lock.failed(); }
    public String goalStatus() {
        if(calumetStage())return voyage.status();
        if(indianaStage())return "TOW STEEL PAST HICK TOWER / SOUTH FORK MILL";
        if(montroseStage())return mission.status(this);
        if(lockStage())return lock.signal();
        if(sharedBerthStage())return "ONE SLIP / PULL EMPTIES CLEAR";
        if(depthStage())return "NARROW CHANNEL / DEEPEST FIRST";
        return "DELIVER BOTH / OPPOSITE BANKS";
    }
    public int feederDrops() { int count=0;for(Feeder f:feeders) count+=f.drops;return count; }
    public void restoreProgress(String value) {
        if(value==null || value.length()>64+Challenges.COUNT*24)return;
        String progress=value;int side=GameSettings.LEFT;
        if(value.startsWith("BBG1\n")) {
            String[] parts=value.split("\n",-1);
            if(parts.length!=3 || parts[2].length()!=1 || parts[2].charAt(0)<'0' || parts[2].charAt(0)>'2')return;
            progress=parts[1];side=parts[2].charAt(0)-'0';
        }
        if(career.restore(progress)){challengeId=Challenges.stageEntry(career.selectedChallenge());challengePage=Challenges.pageFor(challengeId);settings.restore(side);}
    }
    public String saveProgress() { return "BBG1\n"+career.save()+"\n"+settings.joystickSide(); }
    public int progressVersion() { return career.version()+settings.version(); }
    public void openSettings(){if(mode==SETTINGS)return;settingsReturnMode=mode;cancelInputs();accumulator=visualAccumulator=0;mode=SETTINGS;}
    public boolean selectChallenge(int id) {
        id=Challenges.stageEntry(id);
        if(!career.select(id)) return false;
        challengeId=id;challengePage=Challenges.pageFor(id); return true;
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
        return calumetStage()?CalumetRiver.contains(x,y):indianaStage()?IndianaHarbor.contains(x,y):montroseStage()?MontroseHarbor.contains(x,y):TrainingChannel.contains(x,y);
    }
    public static final float WORK_ZOOM=2.15f,DOCK_ZOOM=2.5f,TRANSIT_ZOOM=1.2f;
    public static final float PRACTICE_DRIVE_ZOOM=2.4f,PRACTICE_CANAL_ZOOM=1.65f;
    /** Travel gets context; close work gets a larger hull without changing the physics. */
    private Layout tutorialCamera() {
        if(tutorial.entering)return Layout.focused(layout.width,layout.height,TutorialArrival.DOCK_X,780,2.4f,layout.width*.36f,layout.height*.57f);
        if(tutorial.lesson==Tutorial.MOVE)return Layout.focused(layout.width,layout.height,tug.x,tug.y,
                PRACTICE_DRIVE_ZOOM,layout.width*.52f,layout.height*.55f);
        if(tutorial.lesson==Tutorial.FIGURE_EIGHT || tutorial.lesson==Tutorial.REVERSE)return Layout.focused(layout.width,layout.height,
                HelmPractice.MEET_X,HelmPractice.MEET_Y,1.4f,layout.width*.60f,layout.height*.52f);
        if(tutorial.lesson==Tutorial.CHANNEL && (!tutorial.canal.authorized || !passages.passable(0)
                || !tutorial.canal.escort.leading || tug.x<TrainingChannel.MIN+20)
                && Physics.length(tug.x-TutorialCanalBridge.X,tug.y-TutorialCanalBridge.Y)<400)
            return Layout.focused(layout.width,layout.height,(tug.x+TutorialCanalBridge.X)*.5f,
                    (tug.y+TutorialCanalBridge.Y)*.5f,1.4f,layout.width*.45f,layout.height*.65f);
        if(tutorial.lesson==Tutorial.CHANNEL && tutorial.canal.escort.leading && teacherBoat()!=null)
            return Layout.focused(layout.width,layout.height,tug.x*.75f+teacherBoat().x*.25f,
                    tug.y*.75f+teacherBoat().y*.25f,PRACTICE_CANAL_ZOOM,layout.width*.5f,layout.height*.65f);
        if(tutorial.lesson==Tutorial.BRIDGE || tutorial.lesson==Tutorial.TRAIN)return workCamera(1.5f,0,true);
        float entrance=Physics.clamp((tug.x-(TrainingChannel.MIN-160))/160,0,1);
        entrance=entrance*entrance*(3-2*entrance);
        float travelZoom=PRACTICE_DRIVE_ZOOM+(PRACTICE_CANAL_ZOOM-PRACTICE_DRIVE_ZOOM)*entrance;
        float blend=Physics.clamp((tug.x-80)/160,0,1);
        blend=blend*blend*(3-2*blend);
        if(blend==0) return Layout.focused(layout.width,layout.height,tug.x,tug.y,travelZoom,layout.width*.5f,layout.height*.62f);
        return workCamera(travelZoom+(WORK_ZOOM-travelZoom)*blend,blend,true);
    }
    void fitTutorialCamera() { layout=TrainingScenery.constrain(tutorialCamera(),tug.x,tug.y); }
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
        boolean docking=load!=null && berthEnabled(load.berth) && !load.awaitingPickup && tow.incoming(load)==null
                && Physics.length(load.x-dockX(load),load.y-dockY(load))<190;
        if(docking && workBlend>0) {
            float distance=Physics.length(load.x-dockX(load),load.y-dockY(load));
            zoom+=(DOCK_ZOOM-WORK_ZOOM)*workBlend*Physics.clamp((190-distance)/130,0,1);
        }
        // Marina work targets twice the former scale; retain hull fitting below.
        if(montroseStage() && zoom>1)zoom*=MontroseHarbor.ZOOM_MULTIPLIER;
        // Fit the working hulls and, when relevant, their actual elevated tie point.
        // Far-away held loads do not drag the camera away from the player.
        if(load!=null && Physics.length(tug.x-load.x,tug.y-load.y)>230 && !(tutorial!=null && (tutorial.lesson==Tutorial.LINK || tutorial.lesson==Tutorial.DOUBLE))) {load=null;docking=false;}
        RiverCamera base=RiverCamera.DEFAULT;
        float minX=Float.MAX_VALUE,minY=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        int count=load==null?1:docking?3:2;
        for(int i=0;i<count+cargo.length;i++) {
            Body train=i>=count?cargo[i-count]:null;
            if(i>=count && (train==null || train==load || !tow.connected(train,tug)))continue;
            float wx=i>=count?train.x:i==0?tug.x:i==1?load.x:bollardX(load);
            float wy=i>=count?train.y:i==0?tug.y:i==1?load.y: bollardY(load);
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
        if(pinchZoom.active())return;
        if(ropeDragging || towAiming || ropePhase!=0 || MontroseRescue.swimmers(this) && (rescue.aiming>=0 || rescue.flying>=0)){cameraZoomHoldTicks=90;return;}
        boolean steering=joystick.active || pointer && (holdingTug() || manualSteering);
        if(steering)cameraZoomHoldTicks=90;else if(cameraZoomHoldTicks>0)cameraZoomHoldTicks--;
        Layout desired;
        if(tutorial!=null) desired=tutorialCamera();
        else if(MontroseRescue.swimmers(this))desired=Layout.focused(layout.width,layout.height,tug.x,tug.y,rescue.closeCamera?3.6f:2.4f,layout.width*.5f,layout.height*.52f);
        else {
            // Distinct enter/leave thresholds avoid zoom pumping near a transit boundary.
            if(calumetStage() || indianaStage() || montroseStage()){
                Body load=cameraLoad();int berth=load==null?0:load.berth;
                float distance=Physics.length(tug.x-(load==null?dockX(berth):dockX(load)),tug.y-(load==null?dockY(berth):dockY(load)));
                if(distance>340)cameraTransit=true;else if(distance<260)cameraTransit=false;
            }else if(lockStage()){
                if(tug.y<Feeder.DROP_Y-220)cameraTransit=true;
                else if(tug.y>Feeder.DROP_Y-140)cameraTransit=false;
            }else cameraTransit=false;
            desired=workCamera(cameraTransit?TRANSIT_ZOOM:WORK_ZOOM,cameraTransit?0:1,false);
        }
        float targetZoom=desired.camera.zoom;
        // Do not pull the view away while the player steers or immediately after a cast/release.
        // Keep panning and allow closer approach views, including swimmer rescue proximity.
        // Early tutorial framing must still reveal the next lesson's cargo and tie target.
        boolean guidedReveal=tutorial!=null && tutorial.lesson<Tutorial.PULL;
        if(!guidedReveal && cameraZoomHoldTicks>0 && targetZoom<c.zoom)targetZoom=c.zoom;
        targetZoom=pinchZoom.target(targetZoom,steering);
        float zoom=c.zoom+(targetZoom-c.zoom)*.035f;
        // Ease the world focus, not its zoom-dependent camera origin. This avoids
        // a sideways sweep when zoom changes around a far-upstream position.
        float anchorX=layout.width*.5f,anchorY=layout.height*(tutorial!=null?.59f:.52f);
        float x=c.worldX(anchorX,anchorY),y=c.worldY(anchorX,anchorY);
        float tx=desired.camera.worldX(anchorX,anchorY),ty=desired.camera.worldY(anchorX,anchorY);
        float ease=tutorial!=null && tutorial.lesson<=Tutorial.CHANNEL?.045f:.055f;
        Layout next=Layout.focused(layout.width,layout.height,x+(tx-x)*ease,y+(ty-y)*ease,zoom,anchorX,anchorY);
        if(Math.abs(next.camera.worldTop-c.worldTop)<.0001f && Math.abs(next.camera.worldRight-c.worldRight)<.0001f
                && Math.abs(zoom-c.zoom)<.000001f) return;
        changeCamera(next);
    }
    void changeCamera(Layout nextLayout) {
        RiverCamera c=layout.camera;
        float px=c.screenX(pointerX,pointerY),py=c.screenY(pointerX,pointerY);
        float dx=c.screenX(downX,downY),dy=c.screenY(downX,downY);
        layout=tutorial!=null?TrainingScenery.constrain(nextLayout,tug.x,tug.y):nextLayout;
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
        Body teacher=teacherBoat();
        Body spare=cargo[2];Towline lead=tow.incoming(load),spareLead=tow.incoming(spare);
        return status()+"; dialogue "+(dialogue.active?dialogue.count():0)+"; arrival "+(tutorial!=null && tutorial.entering?1:0)+"; cargo "+(load==null?"none":load.x+","+load.y)+"; tug "+tug.x+","+tug.y+"; gate "+(tutorial==null?0:tutorial.channelGate)
                +(tutorial==null?"":"; slip phase "+tutorial.navigation.slipPhase+"; slip retry "+(tutorial.navigation.retry?1:0)+"; map phase "+tutorial.navigation.mapPhase+"; navigation target "+tutorial.navigation.targetX(this)+","+tutorial.navigation.targetY(this)+"; helm phase "+tutorial.helm.phase+"; heading "+tug.angle+"; drive marker "+tutorial.driveMarker+"; orbit progress "+tutorial.orbitProgress()+"; drive target "+tutorial.driveTargetX()+","+tutorial.driveTargetY())
                +(tutorial==null?"":"; reverse target "+tutorial.reverseTargetX+","+tutorial.reverseTargetY+"; reverse progress "+tutorial.reverseProgress()+"; reverse armed "+(tutorial.reverseArmed?1:0))
                +(tutorial==null?"":"; canal contact "+(tutorial.canal.contactable(this)?1:0)+"; canal ready "+(tutorial.canal.authorized && passages.passable(0)?1:0)+"; bridge house "+tutorial.canal.houseX(this)+","+tutorial.canal.houseY(this)+"; teacher "+(teacher==null?"0,0":teacher.x+","+teacher.y))
                +"; bollard "+(load==null?"none":bollardX(load)+","+bollardY(load))
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
        practice=new Game(); practice.reset(); practice.tutorial=new Tutorial();practice.bridge=new Bridge();practice.lock.passive=true;
        practice.restoreProgress(saveProgress());
        practice.settings=settings;
        practice.layout=layout.camera.zoom==1 && layout.camera.worldTop==148?layout:new Layout(layout.width,layout.height);
        practice.hapticsEnabled=hapticsEnabled;
        practice.tutorial.setup(practice);practice.tutorial.enter(practice);practice.fitTutorialCamera();
        practice.tutorial.resumeShift=returnToShift;
    }
    public void render(Painter painter) { Renderer.draw(this,painter); }
    public String status() {
        if(practice!=null) return practice.status();
        String state=mode==PLAY?"Working":mode==PAUSE?"Paused":mode==HELP?"Help":mode==SETTINGS?"Settings":mode==TITLE?"Ready":mode==CHALLENGES?"Challenges":"Finished";
        String activity=ropePhase==1?"; rope flying":ropePhase==2?"; rope retracting":ropePhase==3?ropeToTug?"; towline caught":"; bollard hit":"";
        if(tow.count()>0) activity=montroseStage()?"; towing yacht":"; towing "+tow.loadCount(tug)+" barges";
        for(Body b:cargo) if(b!=null && b.active && b.lineAttached && !b.deliveryRecorded) activity="; securing load";
        for(Body b:cargo) if(b!=null && b.active && Mooring.aligning(b)) activity="; drawing alongside";
        for(Body b:cargo) if(b!=null && b.active && b.unloadingTicks>0 && !b.deliveryRecorded) activity="; unloading";
        for(Body b:cargo) if(canTie(b)) { activity="; ready to tie "+b.berth; break; }
        return state+"; delivered "+delivered+"; score "+score+(tutorial==null?"":"; lesson "+tutorial.lesson)
                +"; drag controls"+activity+"; challenge "+challengeId+"; career stars "+career.totalStars()
                +"; feeder drops "+feederDrops()+"; rank "+career.rankTitle()+"; stripes "+career.stripes()+"; challenge name "+challengeTitle();
    }
    public void reset() { reset(false); }
    /** Isolated existing practice workyard for deterministic control/render tooling.
     * Never a location-menu entry or persisted career ID. Geographic tests use reset(). */
    void resetPracticeYard(){reset(true);}
    private void reset(boolean practiceYard) {
        challengeId=practiceYard?-1:Challenges.stageEntry(challengeId);montroseComplete=false;montroseJobScore=0;
        dialogue.close(this);practice=null; tutorial=null; tow=new Towline(); towReleases=0; towAimBody=ropeTowTarget=null; ropeToTug=towAiming=false; pushing=false; ropeDragging=false; reversing=false; manualSteering=false; control=IDLE;
        mapPointer=-1; hapticPending=hapticCooldown=contactCooldown=0; contactId=-1;
        joystick.resetGuide();
        overview.reset();
        ticks=score=delivered=lost=combo=spawnIndex=0; hull=100; nextId=1;
        earnedStars=0; rankPromoted=false;pinchZoom.reset();cameraZoomHoldTicks=0;cameraTransit=false;
        if(layout.camera.zoom!=1 || layout.camera.worldTop!=148 || layout.camera.worldRight!=460) layout=new Layout(layout.width,layout.height);
        voyage=new CalumetVoyage();passages=new BridgePassage(this);
        bridge=new Bridge();if(tutorial==null)bridge.phase=Bridge.OPEN;lock=new Lock();lock.passive=!lockStage();mission=new MontroseHarbor();rescue=new MontroseRescue();buildingFades=new BuildingFades();
        water=new WaterSurface(); smoke=new VesselSmoke();titleSmoke=new VesselSmoke(); trafficBoat=new Body(-1,-1,110,TRAFFIC_Y,0);
        visualTicks=0; visualAccumulator=0; ropePhase=ropeTicks=0; ropeBody=null;ropeTowTarget=null; ropeTargeted=false;
        celebrationTicks=impactTicks=damageFlashTicks=0;
        damageCooldown=0; accumulator=0; selected=-1;
        tug=new Body(0,-1,330,650,(float)Math.PI);
        for(int i=0;i<cargo.length;i++) cargo[i]=null;
        feeders=new Feeder[]{new Feeder(260),new Feeder(360)};
        if(calumetStage()){CalumetRiverView.prepare();feeders=new Feeder[0];voyage.setup(this);spawnIndex=quota();}
        else if(indianaStage()){feeders=new Feeder[0];IndianaHarbor.setup(this);spawnIndex=quota();}
        else if(montroseStage()){feeders=new Feeder[0];mission.setup(this);spawnIndex=quota();}
        else {feeders[0].request(feedBerth(0));feeders[1].request(feedBerth(1));spawnIndex=2;}
        nextFeedTick=feedInterval();
        targetX=tug.x; targetY=tug.y; navigating=false; cancel();
        mode=PLAY; tell(goalStatus());
        buoys=new Buoys();buoys.reset(this);mooredFleet=new MooredFleet();mooredFleet.reset(this);splashes=new CollisionSplashes();
        ShorelineView.prepare(this);
        DockView.prepare(this);
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
        rescue.aiming=-1;
        if(practice!=null) { practice.cancelInputs(); return; }
        overview.cancelTouch();
        if(tutorial!=null)tutorial.canal.cancelTouch();
        dialogue.cancel();cancel(); mapPointer=-1; hapticPending=0; reversing=false; navigating=false;
    }
    /** Native input is always in landscape canvas coordinates. */
    public void pointerDown(int id,float x,float y) {
        if(practice!=null) { practice.pointerDown(id,x,y); finishPractice(); return; }
        if(mode==PLAY && conversation()){dialogue.down(this,id,x,y);return;}
        if(OverviewMap.sliderHit(this,x,y)) {
            // A second finger may zoom while the existing drive touch remains held.
            // Slider fingers never enter the world/pinch pointer router.
            if(!overview.active && id!=mapPointer && !pinchZoom.active())overview.begin(this,id,x);
            return;
        }
        if(tutorial!=null && tutorial.canal.down(this,id,x,y))return;
        if(pinchZoom.active()){pinchZoom.down(this,id,x,y);return;}
        if(liveRiverInput() && !(tutorial!=null && tutorial.entering) && ropePhase==0
                && !joystick.active && !ropeDragging && !towAiming && !holdingTug() && heldCargo()==null
                && rescue.aiming<0 && x>=0 && x<=layout.width && y>=64 && y<layout.height-64
                && !liveButton(x,y) && !Joystick.hit(this,x,y)
                && pinchZoom.down(this,id,x,y))return;
        if(mapPointer!=-1 || pointer || x<0 || x>layout.width || y<0 || y>layout.height) return;
        if(!(tutorial!=null && tutorial.entering) && Joystick.hit(this,x,y)) {
            beginDown(x,y,false);mapPointer=id;joystick.begin(this,x,y);haptic(HAPTIC_TICK);return;
        }
        boolean river=liveRiverInput() && (calumetStage() || indianaStage() || montroseStage() || layout.camera.contains(x,y));
        if(river && liveButton(x,y)) river=false;
        if(river) beginDown(layout.camera.worldX(x,y),layout.camera.worldY(x,y),true);
        else beginDown(x,y,false);
        if(pointer) mapPointer=id;
    }
    public void pointerMove(int id,float x,float y) {
        if(practice!=null) { practice.pointerMove(id,x,y); return; }
        if(mode==PLAY && conversation()){dialogue.move(this,id,x,y);return;}
        if(tutorial!=null && tutorial.canal.move(this,id,x,y))return;
        if(overview.owns(id)){overview.setFromX(this,x);return;}
        if(pinchZoom.move(this,id,x,y))return;
        if(id!=mapPointer) return;
        if(joystick.active){pointerX=x;pointerY=y;joystick.move(this,x,y);return;}
        if(mapInRiver) move(layout.camera.worldX(x,y),layout.camera.worldY(x,y)); else move(x,y);
    }
    public void pointerUp(int id,float x,float y) {
        if(practice!=null) { practice.pointerUp(id,x,y); finishPractice(); return; }
        if(mode==PLAY && conversation()){dialogue.up(this,id,x,y);return;}
        if(tutorial!=null && tutorial.canal.up(this,id,x,y))return;
        if(overview.owns(id)){overview.setFromX(this,x);overview.cancelTouch();return;}
        if(pinchZoom.up(id))return;
        if(id!=mapPointer) return;
        mapPointer=-1;
        if(joystick.active){cancel();return;}
        if(mapInRiver) up(layout.camera.worldX(x,y),layout.camera.worldY(x,y)); else up(x,y);
    }
    private boolean liveButton(float x,float y) {
        return layout.box(Layout.HELP).contains(x,y)
                || layout.box(Layout.PRACTICE).contains(x,y);
    }
    private boolean liveRiverInput() { return mode==PLAY && (tutorial==null || tutorial.lesson!=Tutorial.DONE); }
    public void pointerCancel(int id) {
        if(practice!=null) { practice.pointerCancel(id); return; }
        dialogue.cancel(id);
        if(tutorial!=null)tutorial.canal.cancel(id);
        if(overview.owns(id)){overview.cancelTouch();return;}
        if(pinchZoom.up(id))return;
        if(id==mapPointer) { cancel(); mapPointer=-1; }
    }
    private void finishPractice() {
        if(practice==null) return;
        // Explicit title navigation also discards a suspended voyage, instead of
        // following the tutorial's usual return-to-shift/completion behavior.
        if(practice.mode==TITLE) {
            hapticsEnabled=practice.hapticsEnabled; practice=null;
            returnToTitle(); return;
        }
        if(!practice.tutorial.exitRequested) return;
        boolean finished=practice.tutorial.lesson==Tutorial.DONE;
        hapticsEnabled=practice.hapticsEnabled; practice=null; accumulator=0;
        if(returnToShift) mode=PLAY; else if(finished) reset();
    }
    private void returnToTitle() {
        cancelInputs(); accumulator=visualAccumulator=0;
        returnToShift=false; mode=TITLE;
    }
    public void cancel() {
        if(practice!=null) { practice.cancel(); return; }
        if(pushing || joystick.active) { navigating=false; reversing=false; control=COASTING; }
        pinchZoom.cancelTouches();joystick.cancel();driveDirection=0;rescue.aiming=-1;
        pointer=false; mapPointer=-1; mapInRiver=false; cargoPushGesture=false; pushing=false; ropeDragging=false; towAiming=false; towAimBody=null; navigating=false; held=-1; pressTicks=0; manualSteering=false;
    }
    public boolean holdingTug() { return pointer && held==-2; }
    public boolean canTie(Body b) { return ropePhase==0 && tieEligible(b); }
    boolean inTieArea(Body b) {
        if(calumetStage())return voyage.inTieArea(b);
        return orderStage()?PhysicalBerths.fits(challengeId,b):Math.abs(b.x-dockX(b))<=55 && Math.abs(b.y-dockY(b))<=53;
    }
    private boolean tieEligible(Body b) {
        // Wide berths permit angled catches and winch alignment. Narrow physical
        // slips require the complete hull inside so the winch cannot cross a quay.
        return b!=null && !b.yacht && b.active && !b.deliveryRecorded && !b.lineAttached && berthEnabled(b.berth) && (!calumetStage() || b.voyageJob==voyage.job) && berthBlocker(b.berth,b)==null
                && (!b.awaitingPickup || tutorial!=null) // Practice parks the teaching hull in place.
                && inTieArea(b)
                && (tutorial==null || tutorial.lesson==Tutorial.TIE && b==cargo[0]);
    }
    boolean ropeWithinReach(Body b) {
        return b!=null && !b.yacht && b.active && !b.deliveryRecorded && !b.lineAttached && berthEnabled(b.berth) && (!calumetStage() || b.voyageJob==voyage.job) && berthBlocker(b.berth,b)==null
                && Math.abs(b.x-dockX(b))<105 && Math.abs(b.y-dockY(b))<105;
    }
    /** Raised quay fittings share the exact projection used by touch and rope drawing. */
    public float bollardX(int berth) { return calumetStage()?voyage.bollardX(voyage.destination(berth)):(indianaStage()?IndianaHarbor.DOCK_X+IndianaHarbor.DOCK_NX*64:orderStage()?PhysicalBerths.LEFT-25:berthRight(berth)?480:120)+(!montroseStage()?RiverCamera.riseX(GroundArt.LAND_RISE):0); }
    public float bollardY(int berth) { return calumetStage()?voyage.bollardY(voyage.destination(berth)):(indianaStage()?IndianaHarbor.DOCK_Y+IndianaHarbor.DOCK_NY*64:dockY(berth))+(!montroseStage()?RiverCamera.riseY(GroundArt.LAND_RISE):0); }
    public float dockX(Body b){if(b.cruiser)return MontroseRescue.berth(b.berth).x;if(b.yacht)return MontroseHarbor.SLIP_X;return calumetStage() && b.destination>=0?voyage.dockX(b.destination):dockX(b.berth);}
    public float dockY(Body b){if(b.cruiser)return MontroseRescue.berth(b.berth).y;if(b.yacht)return MontroseHarbor.SLIP_Y;return calumetStage() && b.destination>=0?voyage.dockY(b.destination):dockY(b.berth);}
    public float bollardX(Body b){return calumetStage() && b.destination>=0?voyage.bollardX(b.destination):bollardX(b.berth);}
    public float bollardY(Body b){return calumetStage() && b.destination>=0?voyage.bollardY(b.destination):bollardY(b.berth);}
    public float ropeOriginX() { return ropeBody.x+ropeLocalX*(float)StrictMath.cos(ropeBody.angle)-ropeLocalY*(float)StrictMath.sin(ropeBody.angle); }
    public float ropeOriginY() { return ropeBody.y+ropeLocalX*(float)StrictMath.sin(ropeBody.angle)+ropeLocalY*(float)StrictMath.cos(ropeBody.angle); }
    public boolean overTiePoint(Body b,float x,float y) {
        return Physics.length(x-bollardX(b),y-bollardY(b))<=TIE_RADIUS;
    }
    public float aimX(Body b,float x,float y) {
        return overTiePoint(b,x,y)?bollardX(b):indianaStage()?Physics.clamp(x,IndianaHarbor.MIN_X,IndianaHarbor.MAX_X):calumetStage()?Physics.clamp(x,CalumetRiver.MIN_X,CalumetRiver.MAX_X):Physics.clamp(x,TIE_X[0]-TIE_RADIUS,TIE_X[1]+TIE_RADIUS);
    }
    public float aimY(Body b,float x,float y) {
        return overTiePoint(b,x,y)?bollardY(b):indianaStage()?Physics.clamp(y,IndianaHarbor.MIN_Y,IndianaHarbor.MAX_Y):calumetStage()?Physics.clamp(y,CalumetRiver.MIN_Y,CalumetRiver.MAX_Y):Physics.clamp(y,channelTop()-47,816);
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
    /** Host Back/Escape actions share navigation and input cancellation. */
    public void back() {
        if(practice!=null){practice.back();return;}
        if(mode==PLAY || mode==PAUSE){pause();return;}
        cancelInputs();accumulator=visualAccumulator=0;
        if(mode==SETTINGS)mode=settingsReturnMode;
        else if(mode==HELP)mode=PLAY;
        else if(mode==CHALLENGES || mode==FINISH)mode=TITLE;
    }
    public void pause() { if(practice!=null) { practice.pause(); return; } cancelInputs(); accumulator=0; visualAccumulator=0; if(mode==PLAY) mode=PAUSE; else if(mode==PAUSE) mode=PLAY; }
    /** Direct/replay input uses world coordinates on the board, canvas coordinates in menus. */
    public void down(float x,float y) {
        if(practice!=null) { practice.down(x,y); return; }
        boolean river=liveRiverInput() && worldTouch(x,y);
        beginDown(x,y,river);
    }
    private boolean worldTouch(float x,float y) {
        if(calumetStage())return x>=CalumetRiver.MIN_X && x<=CalumetRiver.MAX_X && y>=CalumetRiver.MIN_Y && y<=CalumetRiver.MAX_Y;
        if(indianaStage())return x>=IndianaHarbor.MIN_X && x<=IndianaHarbor.MAX_X && y>=IndianaHarbor.MIN_Y && y<=IndianaHarbor.MAX_Y;
        if(montroseStage())return x>=-900 && x<=2700 && y>=-1600 && y<=1100;
        return x>=(TrainingChannel.ENTRY_LEFT-40)
                && x<=CalumetMap.EAST+40
                && y>=channelTop()-67 && y<=RiverPainter.WORLD_BOTTOM;
    }
    private void beginDown(float x,float y,boolean river) {
        if(pointer) return;
        if(tutorial!=null && tutorial.entering && river)return;
        if(river) {
            if(!worldTouch(x,y)) return;
        } else if(x<0 || x>layout.width || y<0 || y>layout.height) return;
        pointer=true; driveDirection=0; tapCandidate=true; cargoPushGesture=false; mapInRiver=river; pointerX=downX=x; pointerY=downY=y; pressTicks=0; held=-1;
        if(!river) return;
        if(MontroseRescue.swimmers(this)){int swimmer=rescue.swimmerAt(this,x,y);if(swimmer>=0){rescue.aiming=swimmer;return;}}
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
            if(!b.deliveryRecorded && berthBlocker(b.berth,b)!=null && inTieArea(b))tell("BERTH OCCUPIED / PULL THE EMPTY CLEAR");
            if(b.lineAttached && !b.deliveryRecorded || ((ropePhase==1 || ropePhase==2) && b==ropeBody)){selected=cargoHit;return;}
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
            if(held==-2 && manualSteering)selectDriveDirection(x-downX,y-downY);
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
        if(mapInRiver && MontroseRescue.swimmers(this) && rescue.aiming>=0){rescue.release(this,x,y);cancel();return;}
        if(mapInRiver && tapCandidate && MontroseRescue.cruisers(this) && rescue.tapRaft(this,x,y)){cancel();return;}
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
                RopeAttachment.cast(b,ropeEndX,ropeEndY,ropeToTug,route);
                ropeStartX=route[0];ropeStartY=route[1];
                float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
                float dx=ropeStartX-b.x,dy=ropeStartY-b.y;
                ropeLocalX=dx*c+dy*s;ropeLocalY=-dx*s+dy*c;
                ropeStartHeight=RopeAttachment.height(b)+WaterSurface.lift(this,b);
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
                else if(tapped(Layout.HAPTICS,x,y)) {
                    hapticsEnabled=!hapticsEnabled;hapticPending=0;
                    if(hapticsEnabled)haptic(HAPTIC_TICK);
                }
                else if(tapped(Layout.SETTINGS_BACK,x,y))mode=settingsReturnMode;
            }
            cancel();return;
        }
        if(mode==TITLE && tap && tapped(Layout.SETTINGS,x,y)){openSettings();return;}
        if(tutorial!=null && mode==PLAY && tutorial.lesson==Tutorial.DONE) {
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
            else if(tap && tapped(Layout.CHALLENGE_PREV_PAGE,x,y)) challengePage=Math.max(0,challengePage-1);
            else if(tap && tapped(Layout.CHALLENGE_NEXT_PAGE,x,y)) challengePage=Math.min(Challenges.pageCount()-1,challengePage+1);
            else if(tap && tapped(Layout.CHALLENGE_START,x,y) && Challenges.pageFor(challengeId)==challengePage) reset();
            else if(tap) for(int i=0;i<Layout.CHALLENGE_CARDS.length;i++) {
                int id=Challenges.menuId(challengePage*Challenges.PAGE_SIZE+i);
                if(Challenges.valid(id) && tapped(Layout.CHALLENGE_CARDS[i],x,y) && selectChallenge(id)) haptic(HAPTIC_TICK);
            }
        } else if(mode==FINISH) {
            if(tap && tapped(Layout.AGAIN,x,y)) reset();
            else if(tap && tapped(Layout.FINISH_CHALLENGES,x,y)) mode=CHALLENGES;
            else if(tap && tapped(Layout.NEXT_CHALLENGE,x,y) && selectChallenge(Challenges.nextStage(challengeId))) reset();
        } else if(mode==PAUSE) {
            if(tap && tapped(Layout.RESUME,x,y)) pause();
            else if(tap && tapped(Layout.TITLE_SCREEN,x,y)) returnToTitle();
            else if(tap && tapped(Layout.RESTART,x,y)) {
                boolean training=tutorial!=null, resume=training && tutorial.resumeShift;
                reset();
                if(training) { tutorial=new Tutorial(); tutorial.resumeShift=resume; tutorial.setup(this);tutorial.enter(this);fitTutorialCamera(); }
            }
        } else if(tap) {
            if(tapped(Layout.HELP,x,y)) {
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
                    if(mode==TITLE)titleSmoke.titleStep(this);else {smoke.drift();splashes.step(this);buildingFades.step(this);}
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
        sounds.step();radio.step();dialogue.step();visualTicks++;joystick.step(this);splashes.step(this);
        if(tutorial!=null)tutorial.instructionPanel.step(TutorialView.instructionContent(this));
        if(tutorial!=null && tutorial.entering){
            tutorial.update(this);buoys.step(this);water.step(this);smoke.step(this);buildingFades.step(this);updateCamera();return;
        }
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
            if(pressTicks>=12 && held!=-1 && (heldCargo()==null || !heldCargo().lineAttached) && !ropeDragging && !towAiming && tow.incoming(heldCargo())==null) {
                if(!pushing) haptic(HAPTIC_TICK);
                pushing=true;reversing=false;
                if(held>=0) cargoPushGesture=true;
            }
        }
        bridge.step(this);passages.step(this);
        if(lockStage())lock.step(this);
        for(int i=0;i<MOTION_STEPS;i++) motionStep();
        if(tutorial==null) for(Feeder feeder:feeders) {
            feeder.step(this);
            if(lockStage() && feeder.phase!=Feeder.IDLE){
                lock.collide(feeder.boat,splashes);if(feeder.load!=null)lock.collide(feeder.load,splashes);
            }
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
            if(b.lineAttached) { if(!b.deliveryRecorded && correctOrder(b)) Mooring.step(this,b); }
            else { b.dockTime=0; b.lineAttached=false; }
            if(b.unloadingTicks>=UNLOAD_TICKS && !b.deliveryRecorded && berthEnabled(b.berth) && berthBlocker(b.berth,b)==null) {
                b.deliveryRecorded=true;b.active=true; delivered++; combo++; int points=600+Math.min(4,combo)*50;
                celebrationTicks=90; celebrationPoints=points; celebrationX=b.x; celebrationY=b.y;
                score+=points; tell("DELIVERED +"+points+" / "+delivered+" OF "+quota());
                haptic(HAPTIC_SUCCESS);
                if(selected==i) selected=-1;
                if(held==i) { cancel(); navigating=false; }
            }
        }
        tow.prune(this);
        if(montroseStage())mission.step(this);
        if(tutorial!=null) tutorial.update(this);
        else {
            if(calumetStage())voyage.step(this);
            else if(montroseStage())advanceMontroseJob();
            if(ticks>=nextFeedTick && spawn()) nextFeedTick=ticks+feedInterval();
            if(goalComplete() || goalFailed()) {
                mode=FINISH; overview.cancelTouch(); cancel();
                if(goalComplete() && !goalFailed()) {
                    if(lockStage()) {score+=1200;delivered=quota();haptic(HAPTIC_SUCCESS);}
                    earnedStars=Challenges.stars(challengeId,goalProgress(),lost,hull);
                    int before=career.rank(); if(!calumetStage() && !montroseStage())career.record(challengeId,earnedStars,score);
                    rankPromoted|=career.rank()>before;
                }
            }
        }
        updateCamera();
        buildingFades.step(this);
    }
    private void advanceMontroseJob(){
        if(montroseComplete || delivered<quota() || goalFailed())return;
        int before=career.rank();career.record(challengeId,Challenges.stars(challengeId,delivered,lost,hull),score-montroseJobScore);rankPromoted|=career.rank()>before;
        if(challengeId==Challenges.SWIMMER_RESCUE){montroseComplete=true;return;}
        if(challengeId==Challenges.MONTROSE){
            // Preserve the actual docked yacht; rescue code owns only its two new actor slots.
            Body yacht=cargo[0];cargo[0]=null;int slot=cargo.length-1;while(slot>1 && cargo[slot]!=null)slot--;
            if(slot<=1)throw new IllegalStateException("No retained yacht slot");cargo[slot]=yacht;challengeId=Challenges.RAFT_RESCUE;
        }else challengeId=Challenges.SWIMMER_RESCUE;
        delivered=0;montroseJobScore=score;cancelInputs();rescue=new MontroseRescue();rescue.setupContinuation(this);tell(goalStatus());
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
                selectDriveDirection(dx,dy);reversing=driveDirection<0;
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
                selectDriveDirection(dx,dy);reversing=driveDirection<0;
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
        if(lockStage())lock.integrate(tug);else Physics.integrate(tug);
        if(calumetStage())damage(CalumetRiver.collide(tug,splashes));else if(indianaStage())damage(IndianaHarbor.collide(tug,splashes));else if(!montroseStage()){float hit=CalumetMap.collide(tug,splashes);if(tutorial==null)damage(hit);}else damage(MontroseHarbor.collide(this,tug,splashes));
        damage(passages.collide(tug,splashes));
        if(hasBridge()) damage(bridge.collide(tug,splashes));
        if(lockStage()) damage(lock.collide(tug,splashes));
        for(Body b:cargo) if(b!=null && b.active && !b.lineAttached && !b.awaitingPickup) { if(lockStage())lock.integrate(b);else Physics.integrate(b); if(calumetStage())damage(CalumetRiver.collide(b,splashes));else if(indianaStage())damage(IndianaHarbor.collide(b,splashes));else if(!montroseStage()){float hit=CalumetMap.collide(b,splashes);if(tutorial==null)damage(hit);}else damage(MontroseHarbor.collide(this,b,splashes));damage(passages.collide(b,splashes));if(hasBridge()) damage(bridge.collide(b,splashes));if(lockStage()) damage(lock.collide(b,splashes)); }
        for(int iteration=0;iteration<3;iteration++) {
            for(int i=0;i<cargo.length;i++) {
                Body a=cargo[i]; if(a==null || !a.active) continue;
                damage(Physics.collide(tug,a,splashes));
                for(int j=i+1;j<cargo.length;j++) if(cargo[j]!=null) damage(Physics.collide(a,cargo[j],splashes));
            }
        }
        if(teacherBoat()!=null){
            Body teacher=teacherBoat();float dx=teacher.x-tug.x,dy=teacher.y-tug.y,d=Math.max(1,Physics.length(dx,dy));
            if((tug.vx*dx+tug.vy*dy)/d>5 && Physics.touching(tug,teacher,0))tutorial.arrival.warn(this);
            feederContact(teacher);
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
        buoys.step(this);mooredFleet.step(this);
        if(lockStage()){
            // Cargo/float contact correction happens after integration: it cannot push hulls through doors.
            damage(lock.collide(tug,splashes));
            for(Body b:cargo)if(b!=null && b.active && !b.lineAttached && !b.awaitingPickup)damage(lock.collide(b,splashes));
            if(trafficActive())lock.collide(trafficBoat,splashes);
        }
        damage(passages.collide(tug,splashes));for(Body b:cargo)if(b!=null && b.active)damage(passages.collide(b,splashes));
        if(calumetStage()){damage(CalumetRiver.collide(tug,splashes));for(Body b:cargo)if(b!=null && b.active)damage(CalumetRiver.collide(b,splashes));}
        if(indianaStage()){damage(IndianaHarbor.collide(tug,splashes));for(Body b:cargo)if(b!=null && b.active)damage(IndianaHarbor.collide(b,splashes));}
        if(montroseStage()){
            damage(MontroseHarbor.collide(this,tug,splashes));
            for(Body b:cargo)if(b!=null && b.active)damage(MontroseHarbor.collide(this,b,splashes));
        }
        // Fender damping follows motion time; mooring confirmation uses real time.
        for(Body b:cargo) if(b!=null && b.active) {
            if(inDock(b) && b.speed()<65) { b.vx*=.990f; b.vy*=.990f; b.spin*=.985f; }
        }
    }
    private void feederContact(Body obstacle) {
        float x=obstacle.x,y=obstacle.y,vx=obstacle.vx,vy=obstacle.vy,spin=obstacle.spin;
        damage(Physics.collide(tug,obstacle,splashes));
        obstacle.x=x; obstacle.y=y; obstacle.vx=vx; obstacle.vy=vy; obstacle.spin=spin;
        for(Body b:cargo) if(b!=null && b.active && !b.lineAttached && !b.awaitingPickup) {
            Physics.collide(b,obstacle,splashes);
            obstacle.x=x; obstacle.y=y; obstacle.vx=vx; obstacle.vy=vy; obstacle.spin=spin;
        }
    }
    public boolean inDock(Body b) {
        if(calumetStage())return voyage.fits(b);
        if(indianaStage())return Mooring.orientedFits(b,IndianaHarbor.DOCK_X,IndianaHarbor.DOCK_Y,IndianaHarbor.DOCK_NX,IndianaHarbor.DOCK_NY,55,57);
        if(b.cruiser)return MontroseRescue.cruisers(this) && rescue.fits(b);
        if(b.yacht)return montroseStage() && MontroseHarbor.fits(b);
        if(!berthEnabled(b.berth))return false;
        for(int i=0;i<3;i++) if(Math.abs(b.discX(i)-dockX(b))+b.radius>dockHalfWidth()
                || Math.abs(b.discY(i)-dockY(b))+b.radius>53) return false;
        return true;
    }
    private int feedBerth(int index) { return Challenges.feedBerth(challengeId,index); }
    private boolean spawn() {
        if(calumetStage() || spawnIndex>=quota())return false;
        for(Feeder feeder:feeders) if(feeder.phase==Feeder.IDLE) {
            boolean occupied=false;
            for(Body b:cargo)if(b!=null && b.active && Math.abs(b.x-feeder.x)<55 && b.y>Feeder.DROP_Y-80)occupied=true;
            if(occupied)continue;
            feeder.request(feedBerth(spawnIndex++));return true;
        }
        return false;
    }
    private void navigate(float x,float y,float topSpeed) { navigate(x,y,topSpeed,false,false); }
    private void navigate(float x,float y,float topSpeed,boolean astern) { navigate(x,y,topSpeed,astern,true); }
    private void navigate(float x,float y,float topSpeed,boolean astern,boolean hullThrust) {
        float dx=x-tug.x, dy=y-tug.y, distance=Physics.length(dx,dy);
        if(distance<1) { tug.vx*=.93f; tug.vy*=.93f; return; }
        float desired=(float)StrictMath.atan2(dy,dx)+(astern?(float)Math.PI:0), error=Physics.angle(desired-tug.angle);
        tug.spin+=Physics.clamp(error*6-tug.spin*3,-7,7)*Physics.DT;
        float load=tow.loadMass(tug);
        float speed=Math.min(topSpeed*tow.speedFactor(tug),(float)StrictMath.sqrt(distance*100));
        speed*=Math.max(.1f,1-Math.abs(error)*.45f);
        // Manual gestures steer toward the pointer but drive along the actual keel.
        // Otherwise dragging behind a turning tug could translate astern despite
        // a latched FORWARD label. Pickup assistance keeps its existing target vector.
        float sign=astern?-1:1;
        float thrustX=hullThrust?sign*(float)StrictMath.cos(tug.angle):dx/distance;
        float thrustY=hullThrust?sign*(float)StrictMath.sin(tug.angle):dy/distance;
        float dvx=thrustX*speed-tug.vx, dvy=thrustY*speed-tug.vy;
        float force=Physics.length(dvx,dvy), maxChange=82*Physics.DT/(1+.05f*load);
        engineDemand=Physics.clamp(.15f+.4f*speed/95+.45f*Math.min(1,force/maxChange),0,1);
        if(force>maxChange) { dvx*=maxChange/force; dvy*=maxChange/force; }
        tug.vx+=dvx; tug.vy+=dvy;
    }
    private void pushCargo(Body b) {
        float gx=lockStage()?300:dockX(b), gy=lockStage()?(lock.phase<Lock.EXIT?90:-190):dockY(b);
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
        if(!steering && orderStage() && !calumetStage()) {
            PhysicalBerths.waypoint(challengeId,b.x,b.y,gx,gy,b.half+b.radius+4,route); gx=route[0]; gy=route[1];
        }
        float dx=gx-b.x, dy=gy-b.y, distance=Physics.length(dx,dy);
        pushX=dx/Math.max(1,distance); pushY=dy/Math.max(1,distance);
        if(finalDistance<3 && !steering) {
            control=DOCKING;
            // Mooring assist only inside the correct berth; it never transports cargo.
            b.vx*=.97f; b.vy*=.97f;
            if(orderStage() && !calumetStage()) {
                float error=Physics.angle((float)Math.PI/2-b.angle);
                if(error>Math.PI/2)error-=(float)Math.PI;
                if(error<-Math.PI/2)error+=(float)Math.PI;
                b.spin+=Physics.clamp(error*1.25f-b.spin*1.1f,-.75f,.75f)*Physics.DT;
            } else b.spin*=.98f;
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
            guideX=(calumetStage() || indianaStage() || montroseStage())?sx:Physics.clamp(sx,198,orderStage()?PhysicalBerths.RIGHT-20:402); guideY=(calumetStage() || indianaStage() || montroseStage())?sy:Physics.clamp(sy,channelTop()+30,787);
            control=steering?STEERING:ALIGNING;
            if(!steering && orderStage() && !calumetStage()) {
                PhysicalBerths.waypoint(challengeId,tug.x,tug.y,guideX,guideY,tug.half+tug.radius+2,route);
                navigateToLoad(route[0],route[1],95);
            } else navigateToLoad(guideX,guideY,95);
        } else {
            if(Physics.touching(tug,b,3) && !(b.cruiser && rescue.rafted)) b.awaitingPickup=false;
            guideX=b.x-nx*reach; guideY=b.y-ny*reach;
            control=steering?STEERING:!Physics.touching(tug,b,3)?ALIGNING:inDock(b)?DOCKING:PUSHING;
            navigateToLoad(b.x+nx*26,b.y+ny*26,Math.min(76,18+distance*.85f));
            // A broad fender steadies the contact, while collisions still supply linear momentum.
            float heading=(float)StrictMath.atan2(ny,nx);
            // Both barge ends can lead. A contact deflection must not trigger a half-turn in a slip.
            float keelError=Physics.angle(heading-b.angle);
            if(keelError>Math.PI/2)keelError-=(float)Math.PI;
            if(keelError<-Math.PI/2)keelError+=(float)Math.PI;
            b.spin+=Physics.clamp(keelError*1.25f-b.spin*1.1f,-.75f,.75f)*Physics.DT;
            if(distance<43 && !steering) { b.vx*=.988f; b.vy*=.988f; }
        }
    }
    /** Assisted pickup enters through the same open mouth the player can sail. */
    private void navigateToLoad(float x,float y,float speed) {
        if(control==ALIGNING && !manualSteering && !calumetStage() && !montroseStage() && !indianaStage()){buoys.pickupWaypoint(this,x,y,route);x=route[0];y=route[1];}
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
    public boolean trafficWarning() { return false; }
    public boolean trafficActive() { return false; }
    public float trafficX() { return 110+(trafficPhase()-1080)*.95f*MOTION_STEPS; }
    private void traffic() {
        if(!trafficActive()) return;
        Body ship=trafficBoat;ship.x=trafficX();ship.y=TRAFFIC_Y;ship.angle=0;ship.vx=57;ship.vy=0;
        ship.engineLoad=.55f;
        float beforeX=ship.x, beforeY=ship.y;
        float hit=Physics.collide(ship,tug,splashes);
        for(Body b:cargo) if(b!=null && b.active) hit=Math.max(hit,Physics.collide(ship,b,splashes));
        boolean collided=hit>25 || Math.abs(ship.x-beforeX)+Math.abs(ship.y-beforeY)>2;
        ship.x=beforeX;ship.y=beforeY;
        if(collided) damage(90);
    }
}
