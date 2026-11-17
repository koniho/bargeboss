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
            for(int tick=0;tick<12000 && g.tutorial.lesson!=Tutorial.DONE;tick++) {
                int lesson=g.tutorial.lesson;seen|=1<<lesson;
                int marker=g.tutorial.driveMarker,gate=g.tutorial.channelGate;root.pollHaptic();
                float x=tug.x,y=tug.y,lx=load.x,ly=load.y,bx=towLoad.x,by=towLoad.y;
                RiverCamera camera=g.layout.camera;
                if(lesson==Tutorial.CONNECT && waitAtTow++<180) {
                    // Leave the unloaded barge alongside while reading the next
                    // card; it must not disappear or keep awarding deliveries.
                    if(g.pointer) ReplayInput.up(root,g.pointerX,g.pointerY);
                } else ReplayInput.tutorial(root);
                root.step();
                int feedback=root.pollHaptic();
                if(g.tutorial.lesson>lesson || g.tutorial.driveMarker>marker || g.tutorial.channelGate>gate)
                    check(feedback==Game.HAPTIC_SUCCESS,"each completed tutorial goal emits success feedback");
                check(g.tug==tug && present(g,load) && present(g,towLoad),"lesson progression preserves all vessel identities");
                check(Physics.length(tug.x-x,tug.y-y)<8,"tug moves physically instead of teleporting between lessons");
                check(Physics.length(load.x-lx,load.y-ly)<8 && Physics.length(towLoad.x-bx,towLoad.y-by)<8,
                        "cargo moves physically instead of being replaced at a lesson boundary");
                check(Math.abs(camera.screenX(tug.x,tug.y)-g.layout.camera.screenX(tug.x,tug.y))<20
                        && Math.abs(camera.screenY(tug.x,tug.y)-g.layout.camera.screenY(tug.x,tug.y))<20,
                        "camera eases across lesson changes without a scene reset: tick="+tick+" lesson="+lesson+"->"+g.tutorial.lesson+" zoom="+camera.zoom+"->"+g.layout.camera.zoom+" delta="+(camera.screenX(tug.x,tug.y)-g.layout.camera.screenX(tug.x,tug.y))+","+(camera.screenY(tug.x,tug.y)-g.layout.camera.screenY(tug.x,tug.y)));
                check(g.inWater(tug.x,tug.y),"continuous tutorial keeps the tug in connected water");
                if(lesson<=Tutorial.CHANNEL && tug.x<=80) check(g.layout.camera.zoom>=Game.PRACTICE_CANAL_ZOOM-.01f
                        && g.layout.camera.zoom<=Game.PRACTICE_DRIVE_ZOOM+.01f,"driving stays close, easing wider only through the canal");
                if(lesson>=Tutorial.HOLD && g.layout.camera.zoom>1.35f) closeWorkSeen=true;
                if(g.tutorial.lesson!=lesson) {
                    transitions++;
                    if(lesson==Tutorial.CHANNEL) check(Math.abs(tug.x-TrainingChannel.X[7])<30,
                            "last gate advances the lesson while the tug remains at the canal mouth");
                    if(lesson==Tutorial.STEER) check(g.cargo[0]==load && Physics.length(load.x-Tutorial.PUSH_X,load.y-Tutorial.PUSH_Y)<55,
                            "assisted docking continues from the practiced push destination");
                }
                if(g.delivered>0) check(g.delivered==1 && load.active && load.lineAttached && load.unloadingTicks>=Game.UNLOAD_TICKS,
                        "the completed empty barge remains alongside and scores exactly once");
            }
            check(g.tutorial.lesson==Tutorial.DONE && seen==(1<<Tutorial.DONE)-1 && transitions==Tutorial.DONE,
                    "real gestures finish every lesson in one scene at "+size[0]+"x"+size[1]+"; stopped at "+g.tutorial.lesson);
            check(closeWorkSeen,"cargo lessons use the closer working camera on each layout");
            check(g.cargo[0]==towLoad && load.active && g.delivered==1,"the second original barge becomes the tow exercise");
        }
        retryTow();
        lateCoast();
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
            int seen=0,turns=0;float previousAngle=tug.angle;
            for(int tick=0;tick<1800 && g.tutorial.lesson==Tutorial.MOVE;tick++) {
                seen|=1<<g.tutorial.driveMarker;ReplayInput.tutorial(root);
                RiverCamera c=g.layout.camera;float px=c.screenX(g.pointerX,g.pointerY),py=c.screenY(g.pointerX,g.pointerY);
                float ax=c.screenX(g.downX,g.downY),ay=c.screenY(g.downX,g.downY);root.step();
                check(Math.abs(g.layout.camera.screenX(g.pointerX,g.pointerY)-px)<.003f && Math.abs(g.layout.camera.screenY(g.pointerX,g.pointerY)-py)<.003f
                        && Math.abs(g.layout.camera.screenX(g.downX,g.downY)-ax)<.003f && Math.abs(g.layout.camera.screenY(g.downX,g.downY)-ay)<.003f,
                        "close open-water camera preserves screen-space steering anchors");
                check(g.inWater(tug.x,tug.y) && tug.x<TrainingChannel.MIN,"driving goals are reached entirely inside the open basin");
                float screenX=g.layout.camera.screenX(tug.x,tug.y),screenY=g.layout.camera.screenY(tug.x,tug.y);
                check(screenX>30 && screenX<g.layout.width-30 && screenY>80 && screenY<g.layout.height-50,
                        "orbit camera keeps the tug touchable while circling and approaching the next buoy");
                if(Math.abs(Physics.angle(tug.angle-previousAngle))>.4f)turns++;
            }
            check(seen==3 && turns>0 && g.tutorial.driveMarker==2 && g.tutorial.lesson==Tutorial.COAST,
                    "two full buoy orbits teach continuous steering before coast");
            check(Physics.length(tug.x-Tutorial.START_X,tug.y-Tutorial.START_Y)>250,"opening requires a meaningful drive, not a thirty-unit nudge");
            check(g.tutorial.channelGate==1,"open-water targets cannot skip canal gates");
            ReplayInput.up(root,g.pointerX,g.pointerY);float cx=tug.x,cy=tug.y;
            for(int tick=0;tick<60;tick++)root.step();
            check(g.tutorial.lesson==Tutorial.REVERSE && g.tug==tug && Physics.length(tug.x-cx,tug.y-cy)>5,
                    "release teaches actual coasting and preserves momentum into the next lesson");
            for(int tick=0;tick<600 && g.tutorial.lesson==Tutorial.REVERSE;tick++){ReplayInput.tutorial(root);root.step();}
            check(g.tutorial.lesson==Tutorial.CHANNEL,"Backing into the target unlocks canal navigation");
            for(int tick=0;tick<1200 && tug.x<TrainingChannel.X[0];tick++) {ReplayInput.channel(root);root.step();}
            check(tug.x>=TrainingChannel.X[0] && g.tutorial.channelGate==1 && g.tutorial.lesson==Tutorial.CHANNEL,
                    "basin navigation reaches the first canal reach through its open mouth without a teleport or gate shortcut");
        }
    }
    private static void nativeOpeningCadence() {
        for(int width:new int[]{960,1333}) for(int idle:new int[]{0,30,60,180,300}) {
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();int attempts=0;
            while(g.tutorial.lesson==Tutorial.MOVE && attempts++<100) {
                float dx=g.tutorial.driveTargetX()-g.tug.x,dy=g.tutorial.driveTargetY()-g.tug.y,len=Math.max(1,Physics.length(dx,dy));
                // Match the native test's fresh snapshot, gesture duration and
                // adaptive thrust; each fresh drag must engage manual steering.
                float thrust=Math.max(55,Math.min(85,len*.4f));RiverCamera c=g.layout.camera;
                float sx=c.screenX(g.tug.x,g.tug.y),sy=c.screenY(g.tug.x,g.tug.y);
                float ex=c.screenX(g.tug.x+thrust*dx/len,g.tug.y+thrust*dy/len),ey=c.screenY(g.tug.x+thrust*dx/len,g.tug.y+thrust*dy/len);
                root.pointerDown(0,sx,sy);for(int tick=0;tick<3;tick++)root.step();
                int drag=Math.max(1,(int)StrictMath.ceil(Physics.length(ex-sx,ey-sy)/500*60));
                for(int tick=1;tick<=drag;tick++) {root.pointerMove(0,sx+(ex-sx)*tick/drag,sy+(ey-sy)*tick/drag);root.step();}
                for(int tick=0;tick<21;tick++)root.step();root.pointerUp(0,ex,ey);
                for(int tick=0;tick<idle;tick++)root.step();
            }
            for(int tick=0;tick<60;tick++)root.step();
            check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.driveMarker==2 && g.tutorial.channelGate==1,
                    "native opening gestures circle both buoys then coast without skipping gates, with "+idle+" idle ticks at width "+width+"; lesson="+g.tutorial.lesson+", buoy="+g.tutorial.driveMarker+", lap="+g.tutorial.orbitProgress()+", attempts="+attempts);
            check(attempts<100,"feedback-guided native opening converges within its gesture budget");
        }
    }
    private static void nativePushCadence() {
        for(float width:new float[]{960,1333}) for(int coast:new int[]{180,300,480}) {
            Game root=new Game();root.configureCanvas(width,600);ReplayInput.startPractice(root);Game g=root.activeGame();
            g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);Body load=g.cargo[0];
            int attempts=0;
            while(g.tutorial.lesson==Tutorial.HOLD && attempts++<40) {
                RiverCamera camera=g.layout.camera;
                float dx=Tutorial.PUSH_X-load.x,dy=Tutorial.PUSH_Y-load.y,len=Math.max(1,Physics.length(dx,dy));
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
            check(g.cargo[0]==load && Physics.length(load.x-Tutorial.PUSH_X,load.y-Tutorial.PUSH_Y)<=Tutorial.PUSH_RADIUS,
                    "native cadence earns the actual push-target gate rather than bypassing it");
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
            check(p.first!=null && Math.abs(p.first[0]-(sx-12*camera.scale*.8f))<.002f
                    && Math.abs(p.first[1]-(sy+4*camera.scale*.8f))<.002f,
                    "rendered bollard foundation uses the same elevated anchor as the gesture target");
            ReplayInput.down(root,load.x,load.y);ReplayInput.move(root,wx,wy);
            check(g.overTiePoint(load,wx,wy) && Math.abs(g.aimX(load,wx,wy)-wx)<.002f
                    && Math.abs(g.aimY(load,wx,wy)-wy)<.002f,"aiming at the exact raised visual anchor has no hidden snap offset");
            ReplayInput.up(root,wx,wy);
            check(Math.abs(g.ropeEndX-wx)<.002f && Math.abs(g.ropeEndY-wy)<.002f,
                    "released lasso targets the raised bollard rather than the old water-plane point");
            for(int tick=0;tick<Game.ROPE_FLIGHT_TICKS;tick++) root.step();
            check(load.lineAttached,"throwing at the elevated visual anchor catches the bollard");
        }
    }
    private static final class BollardCapture implements Painter {
        float[] first;
        public void poly(float[] xy,int color) {if(first==null) first=xy.clone();}
        public void rect(float x,float y,float w,float h,int color) {}
        public void circle(float x,float y,float r,int color) {}
        public void line(float x,float y,float xx,float yy,float w,int color) {}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {}
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
    private static void lateCoast() {
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();Body tug=g.tug,load=g.cargo[0];
        for(int tick=0;tick<1800 && g.tutorial.lesson==Tutorial.MOVE;tick++) {ReplayInput.tutorial(root);root.step();}
        for(int tick=0;tick<6500 && g.tutorial.channelGate<TrainingChannel.X.length;tick++) {
            // Deliberately ignore the lift instruction and keep steering. These
            // are genuine route crossings, not fixture position assignments.
            ReplayInput.channel(root);root.step();
        }
        check(g.tutorial.lesson==Tutorial.COAST && g.tutorial.channelGate==TrainingChannel.X.length,
                "gates crossed while the coast instruction is visible still receive credit");
        ReplayInput.up(root,g.pointerX,g.pointerY);
        for(int tick=0;tick<61;tick++) root.step();
        check(g.tutorial.lesson==Tutorial.REVERSE && g.tutorial.channelGate==TrainingChannel.X.length,"Late coast still teaches reverse while preserving all cleared gates");
        for(int tick=0;tick<600 && g.tutorial.lesson==Tutorial.REVERSE;tick++){ReplayInput.tutorial(root);root.step();}
        root.step();
        check(g.tutorial.lesson==Tutorial.HOLD && g.tug==tug && g.cargo[0]==load,
                "lifting late advances into cargo practice without losing the course or resetting the scene");
        check(Math.abs(tug.x-TrainingChannel.X[7])<100,"late coast keeps the tug at the actual canal exit");
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
        check(g.tutorial.lesson==Tutorial.PULL && g.tow.cargo==load,"tow retry fixture connects using the public gesture");
        ReplayInput.releaseTow(root);
        float x=tug.x,y=tug.y,bx=load.x,by=load.y;root.step();
        check(g.tutorial.lesson==Tutorial.CONNECT && g.tow.cargo==null,"an early release returns to connect instruction");
        check(g.tug==tug && g.cargo[0]==load && Physics.length(tug.x-x,tug.y-y)<8 && Physics.length(load.x-bx,load.y-by)<8,
                "retrying a tow preserves the existing boats and positions");
    }
    public static void main(String[] args) { run(); }
}
