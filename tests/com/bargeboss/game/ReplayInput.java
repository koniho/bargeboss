package com.bargeboss.game;

/** Deterministic user gestures shared by tests and real-renderer captures. */
public final class ReplayInput {
    private ReplayInput() {}
    /** Stable local cargo fixture for isolated control/art tests, separate from feeder replays. */
    public static void controlFixture(Game g) {
        g.resetPracticeYard(); g.tug=new Body(0,-1,376,390,(float)Math.PI); for(Feeder f:g.feeders) { f.phase=Feeder.IDLE; f.load=null; }
        g.addCargo(2,292,290); g.addCargo(1,320,480);
    }
    /** Isolated later Montrose job; production entry always starts with the yacht. */
    static Game montroseJob(int id){
        Game g=new Game();g.challengeId=Challenges.MONTROSE;g.reset();
        g.challengeId=id;for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        g.rescue.setup(g);return g;
    }
    public static void tap(Game g,float x,float y) { g.pointerDown(0,x,y); g.pointerUp(0,x,y); }
    public static void tap(Game g,Layout.Box box) { box=g.activeGame().controlBox(box); tap(g,box.centerX(),box.centerY()); }
    public static void down(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerDown(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void move(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerMove(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void up(Game g,float x,float y) { RiverCamera camera=g.activeGame().layout.camera; g.pointerUp(0,camera.screenX(x,y),camera.screenY(x,y)); }
    public static void releaseTow(Game root) {
        Body b=root.activeGame().tow.primary();
        if(b!=null) { down(root,b.x,b.y);up(root,b.x,b.y); }
    }
    public static void lasso(Game root,Body b) {
        Game g=root.activeGame();
        down(root,b.x,b.y); move(root,g.bollardX(b),g.bollardY(b));
        up(root,g.bollardX(b),g.bollardY(b));
    }
    public static void work(Game g) {
        if(g.mode!=Game.PLAY || g.ropePhase!=0) return;
        if(MontroseRescue.swimmers(g)){swimmerRescue(g);return;}
        if(MontroseRescue.cruisers(g))return; // Dedicated phase fixtures cover raft release and both narrow slips.
        if(g.indianaStage()){indianaTow(g);return;}
        if(g.montroseStage()){marinaTow(g);return;}
        if(g.lockStage()){lockPassage(g);return;}
        if(g.sharedBerthStage() && clearSharedSlip(g))return;
        // A covered or drifting hull can make a press miss; lift before retrying.
        if(g.pointer && g.heldCargo()==null && !g.holdingTug()) up(g,g.pointerX,g.pointerY);
        Body target=null;
        for(Body b:g.cargo) if(b!=null && b.active && !b.deliveryRecorded && !b.lineAttached && (!g.depthStage() || b.berth==g.nextRequiredBerth())) { target=b; break; }
        if(g.pointer && g.heldCargo()!=null && g.heldCargo()!=target)up(g,g.pointerX,g.pointerY);
        if(target!=null && (!g.pointer || g.holdingTug()) && !g.layout.camera.contains(g.layout.camera.screenX(target.x,target.y),g.layout.camera.screenY(target.x,target.y))) {
            drive(g,300,target.y);return;
        }
        if(target!=null && g.holdingTug() && Math.abs(target.y-g.tug.y)>220) { drive(g,300,target.y);return; }
        // Return down the outer basin beside the parked empty hull.
        if(target!=null && g.sharedBerthStage() && target.awaitingPickup && g.heldCargo()==null && g.tug.x>300 && g.tug.y<625 && g.delivered>0){
            if(g.tug.x>340)drive(g,315,g.tug.y);else drive(g,325,665);
            return;
        }
        // Lift each feeder load north into the turning basin before crossing toward the eastern slip.
        if(target!=null && g.orderStage() && target.y>645){
            if(g.holdingTug())up(g,g.pointerX,g.pointerY);
            if(!g.pointer)down(g,target.x,target.y);
            if(g.heldCargo()==target && g.pushing)move(g,g.downX,g.downY-90);
            return;
        }
        if(target!=null && g.orderStage() && g.heldCargo()==target && g.manualSteering)move(g,g.downX,g.downY);
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
    /** Swap an unloaded hull out of the single slip using an actual tow throw and drag. */
    private static boolean clearSharedSlip(Game g){
        Body empty=null;
        for(Body b:g.cargo)if(b!=null && b.active && b.deliveryRecorded && (b.lineAttached || g.tow.incoming(b)!=null)){empty=b;break;}
        if(empty==null)return false;
        if(g.pointer && !g.holdingTug())up(g,g.pointerX,g.pointerY);
        if(g.tow.incoming(empty)==null){
            if(Physics.length(g.tug.x-empty.x,g.tug.y-empty.y)>140 || g.tug.y<empty.y+65){drive(g,PhysicalBerths.CENTER,empty.y+100);return true;}
            if(g.pointer)up(g,g.pointerX,g.pointerY);
            down(g,empty.x,empty.y);move(g,g.tug.x,g.tug.y);up(g,g.tug.x,g.tug.y);return true;
        }
        if(empty.x>500 && empty.y<635){drive(g,PhysicalBerths.CENTER,780);return true;}
        // Cross above the two waiting feeder loads at y=730, not through them.
        if(empty.x>390){drive(g,300,620);return true;}
        float parkY=g.delivered==1?240:430;
        if(empty.y>parkY+155){drive(g,350,parkY);return true;}
        if(g.pointer)up(g,g.pointerX,g.pointerY);releaseTow(g);return true;
    }
    /** Actual feeder handoff and two lasso gestures, followed by a fully physical loaded passage. */
    private static void lockPassage(Game g){
        if(g.pointer && !g.holdingTug())up(g,g.pointerX,g.pointerY);
        int loads=0;Body next=null;
        for(Body b:g.cargo)if(b!=null && b.active && b.id>0){loads++;if(g.tow.incoming(b)==null && next==null)next=b;}
        if(loads<Lock.REQUIRED){drive(g,300,600);return;}
        if(next!=null){
            if(Physics.length(g.tug.x-next.x,g.tug.y-next.y)>135 || g.tug.y>next.y-65){drive(g,300,next.y-95);return;}
            if(g.pointer)up(g,g.pointerX,g.pointerY);
            down(g,next.x,next.y);move(g,g.tug.x,g.tug.y);up(g,g.tug.x,g.tug.y);return;
        }
        drive(g,300,g.lock.phase>=Lock.EXIT?-280:90);
    }
    /** Public gestures through the inlet, clear turning fairway and physical yacht slip. */
    private static void indianaTow(Game g){
        Body b=g.cargo[0];if(b==null || b.lineAttached || b.deliveryRecorded)return;
        if(g.canTie(b)){if(g.pointer)up(g,g.pointerX,g.pointerY);if(g.tow.incoming(b)!=null)releaseTow(g);lasso(g,b);return;}
        if(!g.tow.connected(b,g.tug)){if(g.pointer)up(g,g.pointerX,g.pointerY);down(g,b.x,b.y);move(g,g.tug.x,g.tug.y);up(g,g.tug.x,g.tug.y);return;}
        float x=g.tug.y<1200?375:325,y=g.tug.y< -250?-180:1640;
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
        if(!g.pointer)down(g,g.tug.x,g.tug.y);
        move(g,g.downX+dx/len*52,g.downY+dy/len*52);
    }
    private static void swimmerRescue(Game g){
        if(g.rescue.flying>=0 || g.rescue.cooldown>0)return;
        int target=-1;for(int col=0;col<10 && target<0;col++)for(int row=0;row<2;row++){int i=col+row*10;if(g.rescue.state[i]==0){target=i;break;}}
        if(target<0)return;
        float x=g.rescue.x[target],y=650+(target%10)*3;
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Physics.length(dx,dy);
        if(len>18){if(!g.pointer)down(g,g.tug.x,g.tug.y);move(g,g.downX+dx/len*55,g.downY+dy/len*55);return;}
        if(g.pointer)up(g,g.pointerX,g.pointerY);
        if(g.tug.speed()>3)return;
        down(g,g.rescue.x[target],g.rescue.y[target]);up(g,g.rescue.x[target],g.rescue.y[target]);
    }
    private static void marinaTow(Game g) {
        Body b=g.cargo[0];
        if(!g.tow.connected(b,g.tug)){
            if(g.pointer)up(g,g.pointerX,g.pointerY);
            down(g,b.x,b.y);move(g,g.tug.x,g.tug.y);up(g,g.tug.x,g.tug.y);return;
        }
        float mx=MontroseHarbor.planX(g.tug.x),my=MontroseHarbor.planY(g.tug.y),x=1003,y=345;
        if(my>1190){x=440;y=1160;}
        else if(mx<500 && my>980){x=330;y=950;}
        else if(mx<440 && my>700){x=330;y=650;}
        else if(mx<820){x=860;y=570;}
        else if(mx<1000 && my>520){x=1015;y=550;}
        float dx=MontroseHarbor.mapX(x)-g.tug.x,dy=MontroseHarbor.mapY(y)-g.tug.y,len=Physics.length(dx,dy);
        if(!g.pointer)down(g,g.tug.x,g.tug.y);
        // Keel-aligned steering turns a real tow instead of translating sideways.
        // Moderate thrust gives the yacht time to follow without stretching its line.
        float drag=y==345 && len<90?42:52;
        if(len<8)move(g,g.downX,g.downY);
        else move(g,g.downX+dx/len*drag,g.downY+dy/len*drag);
    }
    private static void drive(Game g,float x,float y) {
        if(g.bridgeStage() && !g.bridge.passable() && g.bridge.separates(g.tug.y,y)) y=Bridge.Y+(g.tug.y>Bridge.Y?90:-90);
        if(g.tutorial==null && PhysicalBerths.active(g.challengeId)){float[] route=new float[2];PhysicalBerths.waypoint(g.challengeId,g.tug.x,g.tug.y,x,y,18,route);x=route[0];y=route[1];}
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Physics.length(dx,dy);
        if(!g.pointer) down(g,g.tug.x,g.tug.y);
        if(len<25) move(g,g.downX,g.downY);
        else move(g,g.downX+dx/len*90,g.downY+dy/len*90);
    }
    public static void helmDrive(Game g,float x,float y,boolean reverse){
        if(!g.pointer){
            down(g,g.tug.x,g.tug.y);
            float sign=reverse?-1:1;
            move(g,g.downX+sign*65*(float)StrictMath.cos(g.tug.angle),g.downY+sign*65*(float)StrictMath.sin(g.tug.angle));
        }
        float dx=x-g.tug.x,dy=y-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
        float distance=g.tutorial!=null && g.tutorial.lesson==Tutorial.MOVE && g.tutorial.helm.phase==HelmPractice.FORWARD?(g.tutorial.helm.pace==0?48:80):60;
        move(g,g.downX+dx/len*distance,g.downY+dy/len*distance);
    }
    public static void channel(Game root) {
        Game g=root.activeGame();
        TutorialCanalBridge bridge=g.tutorial.canal;
        if(g.dialogue.active){
            if(g.dialogue.ready()){Layout.Box b=g.dialogue.choiceBox(g,0);tap(root,b.centerX(),b.centerY());}
            return;
        }
        if(!bridge.authorized && bridge.contactable(g)){
            if(g.pointer)up(root,g.pointerX,g.pointerY);
            tap(root,bridge.houseX(g),bridge.houseY(g));return;
        }
        if(bridge.phase!=TutorialCanalBridge.IDLE && !g.passages.passable(0)){
            if(g.pointer)up(root,g.pointerX,g.pointerY);return;
        }
        Body teacher=g.teacherBoat();
        if(bridge.authorized && teacher!=null && (!bridge.escort.leading || teacher.x<TrainingChannel.MIN+60
                || Physics.length(teacher.x-g.tug.x,teacher.y-g.tug.y)<60)){
            if(g.pointer)up(root,g.pointerX,g.pointerY);return;
        }
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
    /** Focused exercise fixture; entrance motion is covered by TutorialEntranceTest. */
    public static void startPractice(Game root){root.startTutorial();Game g=root.activeGame();g.tutorial.setup(g);}
    public static void tutorial(Game root) {
        Game g=root.activeGame();
        if(g.tutorial.entering){
            if(g.dialogue.ready()){Layout.Box b=g.dialogue.choiceBox(g,0);root.pointerDown(991,b.centerX(),b.centerY());root.pointerUp(991,b.centerX(),b.centerY());}
            return;
        }
        if(g.tutorial.instructorRadio.teach){InstructorRadio radio=g.tutorial.instructorRadio;tap(root,radio.x(g),radio.y(g));return;}
        if(g.tutorial.instructorRadio.phase!=0){if(g.dialogue.ready()){Layout.Box b=g.dialogue.choiceBox(g,0);tap(root,b.centerX(),b.centerY());}return;}
        switch(g.tutorial.lesson) {
            case Tutorial.MOVE:
                helmDrive(g,g.tutorial.driveTargetX(),g.tutorial.driveTargetY(),g.tutorial.helm.phase==HelmPractice.REVERSE);break;
            case Tutorial.FIGURE_EIGHT:
            case Tutorial.REVERSE:
                helmDrive(g,g.tutorial.driveTargetX(),g.tutorial.driveTargetY(),g.tutorial.lesson==Tutorial.REVERSE);break;
            case Tutorial.SLIP:
                if(g.tutorial.navigation.readyToRelease(g)){if(g.pointer)up(g,g.pointerX,g.pointerY);break;}
                helmDrive(g,g.tutorial.navigation.targetX(g),g.tutorial.navigation.targetY(g),!g.tutorial.navigation.retry && g.tutorial.navigation.slipPhase==2);break;
            case Tutorial.MAP:
                if(g.tutorial.navigation.mapPhase==2){if(g.pointer)up(root,g.pointerX,g.pointerY);Layout.Box slider=OverviewMap.sliderBounds(g);tap(root,slider.x+24,slider.centerY());}
                else helmDrive(g,g.tutorial.navigation.targetX(g),g.tutorial.navigation.targetY(g),false);break;
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
            case Tutorial.BRIDGE:
            case Tutorial.TRAIN:
                drive(g,g.tutorial.towTargetX(),g.tutorial.towTargetY(g));break;
            case Tutorial.DOUBLE:
                if(g.ropePhase==0) {
                    Body b=!Tutorial.direct(g,g.cargo[0])?g.cargo[0]:g.cargo[2];
                    if(Tutorial.direct(g,b))break;
                    if(g.tow.incoming(b)!=null){if(g.pointer)up(root,g.pointerX,g.pointerY);down(root,b.x,b.y);up(root,b.x,b.y);break;}
                    if(Physics.length(b.x-g.tug.x,b.y-g.tug.y)>145){drive(g,b.x-65,b.y-85);break;}
                    if(g.pointer)up(root,g.pointerX,g.pointerY);
                    down(root,b.x,b.y);move(root,g.tug.x,g.tug.y);up(root,g.tug.x,g.tug.y);
                }break;
            case Tutorial.LINK:
                if(g.ropePhase==0) {
                    if(g.pointer)up(root,g.pointerX,g.pointerY);
                    Body spare=g.cargo[2],parent=g.cargo[0];
                    if(g.tow.incoming(spare)!=null){down(root,spare.x,spare.y);up(root,spare.x,spare.y);break;}
                    if(!g.tow.connected(parent,g.tug)){down(root,parent.x,parent.y);move(root,g.tug.x,g.tug.y);up(root,g.tug.x,g.tug.y);break;}
                    down(root,spare.x,spare.y);move(root,parent.x,parent.y);up(root,parent.x,parent.y);
                }break;
            case Tutorial.RELEASE:
                if(g.pointer) up(root,g.pointerX,g.pointerY);
                releaseTow(root);break;
            default: break;
        }
    }
}
