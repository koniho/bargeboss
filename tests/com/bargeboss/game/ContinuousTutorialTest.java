package com.bargeboss.game;

/** Exercise one physical scene through navigation, cargo work and the return tow. */
public final class ContinuousTutorialTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++;if(!ok) throw new AssertionError(message);
    }
    private static boolean present(Game g,Body expected) {
        for(Body b:g.cargo) if(b==expected) return true;
        return false;
    }
    public static void run() {
        canalMouth();
        openPractice();
        nativeOpeningCadence();
        for(float[] size:new float[][]{{960,600},{1333,600},{960,720}}) {
            Game root=new Game();root.configureCanvas(size[0],size[1]);root.startTutorial();
            Game g=root.activeGame();Body tug=g.tug,load=g.cargo[0],towLoad=g.cargo[1];
            check(load!=null && towLoad!=null && load!=towLoad,"both teaching barges exist when practice begins");
            int seen=0,transitions=0,waitAtTow=0;boolean closeWorkSeen=false;
            for(int tick=0;tick<16000 && g.tutorial.lesson!=Tutorial.DONE;tick++) {
                int lesson=g.tutorial.lesson;seen|=1<<lesson;
                int marker=g.tutorial.driveMarker,gate=g.tutorial.channelGate;root.pollHaptic();
                float x=tug.x,y=tug.y,lx=load.x,ly=load.y,bx=towLoad.x,by=towLoad.y;
                RiverCamera camera=g.layout.camera;
                if(lesson==Tutorial.CONNECT && waitAtTow++<180) {
                    // Leave the far-side barge waiting while reading the connection
                    // card; it must remain in the same continuous scene.
                    if(g.pointer) ReplayInput.up(root,g.pointerX,g.pointerY);
                } else ReplayInput.tutorial(root);
                root.step();
                int feedback=root.pollHaptic();
                if(g.tutorial.lesson>lesson || g.tutorial.driveMarker>marker || g.tutorial.channelGate>gate)
                    check(feedback==(lesson==Tutorial.SHOW?Game.HAPTIC_CONTACT:Game.HAPTIC_SUCCESS),"each completed tutorial goal emits success feedback: tick="+tick+" lesson="+lesson+" next="+g.tutorial.lesson+" feedback="+feedback);
                check(g.tug==tug && present(g,load) && present(g,towLoad),"lesson progression preserves all vessel identities");
                check(Physics.length(tug.x-x,tug.y-y)<8,"tug moves physically instead of teleporting between lessons");
                check(Physics.length(load.x-lx,load.y-ly)<8 && Physics.length(towLoad.x-bx,towLoad.y-by)<8,
                        "cargo moves physically instead of being replaced at a lesson boundary");
                check(Math.abs(camera.screenX(tug.x,tug.y)-g.layout.camera.screenX(tug.x,tug.y))<20
                        && Math.abs(camera.screenY(tug.x,tug.y)-g.layout.camera.screenY(tug.x,tug.y))<20,
                        "camera eases across lesson changes without a scene reset: tick="+tick+" lesson="+lesson+"->"+g.tutorial.lesson+" zoom="+camera.zoom+"->"+g.layout.camera.zoom+" delta="+(camera.screenX(tug.x,tug.y)-g.layout.camera.screenX(tug.x,tug.y))+","+(camera.screenY(tug.x,tug.y)-g.layout.camera.screenY(tug.x,tug.y)));
                check(g.inWater(tug.x,tug.y),"continuous tutorial keeps the tug in connected water");
                if(lesson<=Tutorial.CHANNEL && tug.x<=80) check(g.layout.camera.zoom>=1.39f
                        && g.layout.camera.zoom<=Game.PRACTICE_DRIVE_ZOOM+.01f,"driving stays close, easing wider only through the canal");
                if(lesson>=Tutorial.HOLD && g.layout.camera.zoom>1.35f) closeWorkSeen=true;
                if(g.tutorial.lesson!=lesson) {
                    transitions++;
                    if(lesson==Tutorial.CHANNEL) check(Math.abs(tug.x-TrainingChannel.X[7])<30,
                            "last gate advances the lesson while the tug remains at the canal mouth");
                    if(lesson==Tutorial.STEER) check(g.cargo[0]==load,
                            "assisted docking continues with the same manually pushed load");
                }
                if(g.delivered>0) check(g.delivered==1 && load.active && load.deliveryRecorded && load.unloadingTicks>=Game.UNLOAD_TICKS,
                        "the completed empty barge remains alongside and scores exactly once");
            }
            check(g.tutorial.lesson==Tutorial.DONE && seen==(((1<<Tutorial.DONE)-1)&~(1<<Tutorial.PULL)) && transitions==Tutorial.DONE-1,
                    "real gestures finish every lesson in one scene at "+size[0]+"x"+size[1]+"; stopped at "+g.tutorial.lesson);
            check(closeWorkSeen,"cargo lessons use the closer working camera on each layout");
            check(g.cargo[0]==towLoad && load.active && g.delivered==1,"the second original barge becomes the tow exercise");
        }
        retryTow();

        holdFromMouth();
        raisedBollard();
        nativePushCadence();
        System.out.println(checks+" continuous tutorial, vessel continuity, camera and tow retry assertions passed");
    }
    private static void openPractice() {
        check(TrainingChannel.MIN-TrainingChannel.BASIN_LEFT>500 && TrainingChannel.BASIN_BOTTOM-TrainingChannel.BASIN_TOP>500,
                "initial driving area is genuinely broad open water, not another narrow reach");
        for(int marker=0;marker<Tutorial.DRIVE_X.length;marker++)
            for(int x=-100;x<=100;x+=20) for(int y=-100;y<=100;y+=20)
                check(TrainingChannel.contains(Tutorial.DRIVE_X[marker]+x,Tutorial.DRIVE_Y[marker]+y),"practice markers have open maneuvering water on every side");
        for(float width:new float[]{960,1333}) {
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();Body tug=g.tug;
            check(tug.x==Tutorial.START_X && tug.y==Tutorial.START_Y && g.layout.camera.zoom==Game.PRACTICE_DRIVE_ZOOM,
                    "practice starts inside the new basin with a close driving camera");
            for(int tick=0;tick<120;tick++)root.step();
            check(g.tutorial.lesson==Tutorial.MOVE && g.tutorial.driveMarker==0,"waiting cannot earn a driving marker");
            int phases=0,lessons=0;
            for(int tick=0;tick<6500 && g.tutorial.lesson<Tutorial.CHANNEL;tick++){
                phases|=1<<g.tutorial.helm.phase;lessons|=1<<g.tutorial.lesson;
                ReplayInput.tutorial(root);root.step();
                check(g.inWater(tug.x,tug.y),"opening remains in maneuvering water");
            }
            check(phases==31 && lessons==31 && g.tutorial.lesson==Tutorial.CHANNEL,"helm, rendezvous and both powered figure eights precede canal");
            check(g.tutorial.channelGate==1,"opening cannot skip gates");
            for(int tick=0;tick<1200 && tug.x<TrainingChannel.X[0];tick++) {ReplayInput.channel(root);root.step();}
            check(tug.x>=TrainingChannel.X[0] && g.tutorial.channelGate==1 && g.tutorial.lesson==Tutorial.CHANNEL,
                    "basin navigation reaches the first canal reach through its open mouth without a teleport or gate shortcut");
        }
    }
    private static void nativeOpeningCadence() {
        for(int width:new int[]{960,1333})for(int idle:new int[]{0,30,60}){
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();
            for(int tick=0;tick<14000 && g.tutorial.lesson<Tutorial.CHANNEL;tick++){
                if(tick%180==0 && g.pointer){ReplayInput.up(root,g.pointerX,g.pointerY);for(int pause=0;pause<idle;pause++)root.step();}
                ReplayInput.tutorial(root);root.step();
            }
            check(g.tutorial.lesson==Tutorial.CHANNEL,"repeated public presses complete both figure eights with idle="+idle+" width="+width+" lesson="+g.tutorial.lesson);
        }
    }
    private static void nativePushCadence() {
        for(float width:new float[]{960,1333}) for(int coast:new int[]{180,300,480}) {
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();
            g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);Body load=g.cargo[0];
            int attempts=0;
            while(g.tutorial.lesson==Tutorial.HOLD && attempts++<40) {
                RiverCamera camera=g.layout.camera;
                float dx=g.dockX(load)-load.x,dy=g.dockY(load)-load.y,len=Math.max(1,Physics.length(dx,dy));
                float sx=camera.screenX(load.x,load.y),sy=camera.screenY(load.x,load.y);
                float ex=camera.screenX(load.x+90*dx/len,load.y+90*dy/len);
                float ey=camera.screenY(load.x+90*dx/len,load.y+90*dy/len);
                root.pointerDown(0,sx,sy);
                for(int tick=0;tick<3;tick++) root.step();
                for(int tick=1;tick<=12 && g.tutorial.lesson==Tutorial.HOLD;tick++) {
                    root.pointerMove(0,sx+(ex-sx)*tick/12,sy+(ey-sy)*tick/12);root.step();
                }
                for(int tick=0;tick<180 && g.tutorial.lesson==Tutorial.HOLD;tick++) root.step();
                root.pointerUp(0,ex,ey);
                // XCTest can spend several seconds reading accessibility state
                // and resolving the next two coordinates after a release.
                for(int tick=0;tick<coast && g.tutorial.lesson==Tutorial.HOLD;tick++) root.step();
            }
            check(g.tutorial.lesson==Tutorial.STEER,
                    "native three-second manual push bursts finish despite "+coast+" idle ticks between gestures at width "+width);
            check(g.cargo[0]==load && Physics.length(load.x-292,load.y-700)>6,
                    "native cadence earns real manual movement without a placement checkpoint");
        }
    }
    private static void raisedBollard() {
        for(float width:new float[]{960,1333}) {
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();
            g.tutorial.lesson=Tutorial.TIE;g.tutorial.setup(g);Body load=g.cargo[0];
            RiverCamera camera=g.layout.camera;
            float sx=camera.screenX(Game.TIE_X[load.berth],g.dockY(load.berth));
            float sy=camera.screenY(Game.TIE_X[load.berth],g.dockY(load.berth))-18*camera.scale;
            float wx=camera.worldX(sx,sy),wy=camera.worldY(sx,sy);
            check(Math.abs(wx-g.bollardX(load.berth))<.002f && Math.abs(wy-g.bollardY(load.berth))<.002f,
                    "raised bollard hit point is the exact inverse of its elevated visual anchor");
            BollardCapture p=new BollardCapture();MooringView.draw(g,new RiverPainter(p,camera));
            check(p.first!=null && Math.abs(p.first[0]-sx)<.002f
                    && Math.abs(p.first[1]-sy)<.002f,
                    "rendered bollard foundation uses the same elevated anchor as the gesture target");
            check(!MooringView.reachableBollard(g,load.berth),"bollard stays quiet before lasso hold");
            ReplayInput.down(root,load.x,load.y);
            check(MooringView.reachableBollard(g,load.berth),"reachable bollard glows before aiming at it");
            check(!MooringView.reachableBollard(g,(load.berth+1)%3),"unusable berth does not glow");
            float savedX=load.x;load.x+=500;
            check(!MooringView.reachableBollard(g,load.berth),"out-of-reach bollard does not glow");load.x=savedX;
            ReplayInput.move(root,wx,wy);
            check(g.overTiePoint(load,wx,wy) && Math.abs(g.aimX(load,wx,wy)-wx)<.002f
                    && Math.abs(g.aimY(load,wx,wy)-wy)<.002f,"aiming at the exact raised visual anchor has no hidden snap offset");
            ReplayInput.up(root,wx,wy);
            check(!MooringView.reachableBollard(g,load.berth),"bollard glow ends on release");
            check(Math.abs(g.ropeEndX-wx)<.002f && Math.abs(g.ropeEndY-wy)<.002f,
                    "released lasso targets the raised bollard rather than the old water-plane point");
            for(int tick=0;tick<Game.ROPE_FLIGHT_TICKS;tick++) root.step();
            check(load.lineAttached,"throwing at the elevated visual anchor catches the bollard");
        }
    }
    private static final class BollardCapture implements Painter {
        float[] first;
        public void poly(float[] xy,int color) {}
        public void rect(float x,float y,float w,float h,int color) {}
        public void circle(float x,float y,float r,int color) {}
        public void line(float x,float y,float xx,float yy,float w,int color) {}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {if(a==SpriteAtlas.SHORE_BOLLARDS && first==null)first=new float[]{x,y};}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha) {}
    }
    private static void holdFromMouth() {
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);
        g.tug.x=TrainingChannel.X[7];g.tug.y=TrainingChannel.Y[7];g.tug.angle=0;
        g.fitTutorialCamera();Body tug=g.tug,load=g.cargo[0];
        // Isolate the assist route with the public world-coordinate replay API.
        // At the closer camera this distant load is intentionally offscreen;
        // the full gesture replay above tests approaching it before selection.
        g.down(load.x,load.y);
        int ticks=0;
        while(!Physics.touching(tug,load,4) && ticks++<1200) {
            root.step();
            check(g.inWater(tug.x,tug.y),"held-barge assist stays inside connected water from the canal mouth");
        }
        check(Physics.touching(tug,load,4) && g.heldCargo()==load,
                "holding the waiting barge from the last gate routes the tug through the mouth without a bank trap");
        check(g.tutorial.lesson==Tutorial.HOLD,"assisted approach alone cannot pass the drag-to-push lesson");
        g.up(g.pointerX,g.pointerY);
        check(!g.pointer && g.tug==tug && g.cargo[0]==load,"releasing the assisted approach preserves the same scene");
    }
    private static void canalMouth() {
        for(float mouth:new float[]{170,TrainingChannel.MIN}) for(int heading=0;heading<16;heading++) for(int direction:new int[]{-1,1}) {
            Body tug=new Body(0,-1,mouth+(direction>0?-40:50),500,heading*(float)Math.PI/8);
            for(int tick=0;tick<90;tick++) {
                float x=tug.x+direction;tug.x=x;
                TrainingChannel.collideWater(tug);
                check(Math.abs(tug.x-x)<.001f && Math.abs(tug.y-500)<.001f,
                        "all tug headings pass through the open canal mouth in both directions");
                for(int disc=0;disc<3;disc++) for(int point=0;point<8;point++) {
                    double angle=point*Math.PI/4;
                    check(TrainingChannel.contains(tug.discX(disc)+tug.radius*(float)StrictMath.cos(angle),
                            tug.discY(disc)+tug.radius*(float)StrictMath.sin(angle)),
                            "complete hull remains in connected water while crossing the mouth");
                }
            }
        }
    }
    private static void retryTow() {
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.CONNECT;g.tutorial.setup(g);
        Body tug=g.tug,load=g.cargo[0];
        for(int i=0;i<300 && g.tutorial.lesson==Tutorial.CONNECT;i++) { ReplayInput.tutorial(root);root.step(); }
        check(g.tutorial.lesson==Tutorial.CASTOFF && g.tow.cargo==load,"tow retry fixture connects using the public gesture");
        ReplayInput.releaseTow(root);
        float x=tug.x,y=tug.y,bx=load.x,by=load.y;root.step();
        check(g.tutorial.lesson==Tutorial.HOLD && g.tow.cargo==null,"explicit release starts push instruction without any placement checkpoint");
        check(g.tug==tug && g.cargo[0]==load && Physics.length(tug.x-x,tug.y-y)<8 && Physics.length(load.x-bx,load.y-by)<8,
                "retrying a tow preserves the existing boats and positions");
    }
    public static void main(String[] args) { run(); }
}
