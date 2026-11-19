package com.bargeboss.game;

/** Exercise the public touch boundary across windows without changing the simulation geometry. */
public final class ResponsiveTest {
    private static int assertions;
    private static final Layout.Box[] BUTTONS={Layout.SETTINGS,Layout.HAPTICS,Layout.HELP,
            Layout.PRACTICE,Layout.LEARN,Layout.START,Layout.RESUME,Layout.RESTART,Layout.AGAIN};
    private static final Layout.Box[] LIVE_BUTTONS={Layout.HELP,Layout.PRACTICE};
    private static void check(boolean condition,String message) { assertions++; if(!condition) throw new AssertionError(message); }
    private static boolean near(float a,float b) { return Math.abs(a-b)<.002f; }
    private static void tap(Game game,Layout.Box box) { ReplayInput.tap(game,box); }
    public static void run() {
        float[][] windows={{960,600},{1920,1080},{2400,1080},{2732,2048},{3840,1080},{900,1440},{600,600},{720,360},{1259,600},{1261,600}};
        for(float[] window:windows) {
            Viewport viewport=new Viewport(window[0],window[1]);
            check(viewport.x==0 && viewport.y==0,"responsive viewport has no letterbox offsets");
            check(near(viewport.logicalWidth()*viewport.scale,window[0])
                    && near(viewport.logicalHeight()*viewport.scale,window[1]),"logical canvas covers every host pixel");
            check(near(viewport.worldX(window[0]),viewport.logicalWidth())
                    && near(viewport.worldY(window[1]),viewport.logicalHeight()),"host edge input maps to canvas edges");
            Game game=new Game(); game.layout=new Layout(960,600); game.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight());
            checkLayout(game.layout); menuAndGestureRouting(game); practiceAndResize(game);
        }
        independentGames(); cameraMath();
        System.out.println(assertions+" responsive canvas, camera and pointer-routing assertions passed");
    }
    private static void checkLayout(Layout layout) {
        RiverCamera camera=layout.camera;
        check(camera.left>=0 && camera.top>=0 && camera.left+camera.width()<=layout.width+.002f
                && camera.top+camera.height()<=layout.height+.002f,"entire river rectangle fits the canvas");
        float[] xs={RiverPainter.WORLD_LEFT,Physics.LEFT,Game.DOCK_X[0],Physics.RIGHT,RiverPainter.WORLD_RIGHT};
        float[] ys={RiverPainter.WORLD_TOP,Physics.TOP,Game.DOCK_Y[1],Physics.BOTTOM,RiverPainter.WORLD_BOTTOM};
        for(float x:xs) for(float y:ys) {
            float sx=camera.screenX(x,y),sy=camera.screenY(x,y);
            check(sx>=-.002f && sx<=layout.width+.002f && sy>=-.002f && sy<=layout.height+.002f,"world, banks and tie points remain visible");
            check(near(camera.worldX(sx,sy),x) && near(camera.worldY(sx,sy),y),"camera pointer inverse preserves world coordinates");
        }
        float xDistance=Physics.length(camera.screenX(330,500)-camera.screenX(300,500),camera.screenY(330,500)-camera.screenY(300,500));
        float yDistance=Physics.length(camera.screenX(300,530)-camera.screenX(300,500),camera.screenY(300,530)-camera.screenY(300,500));
        float edge=30*camera.scale*(float)StrictMath.sqrt(2.0/3);
        check(near(xDistance,edge) && near(yDistance,edge),"isometric ground axes have equal foreshortening on every host");
        check(camera.screenX(330,500)>camera.screenX(300,500) && camera.screenY(330,500)<camera.screenY(300,500)
                && camera.screenX(300,530)>camera.screenX(300,500) && camera.screenY(300,530)>camera.screenY(300,500),
                "positive world axes project right-up and right-down at the fixed isometric angle");
        for(int i=0;i<Game.TIE_X.length;i++) {
            float sx=camera.screenX(Game.TIE_X[i],Game.DOCK_Y[i]),sy=camera.screenY(Game.TIE_X[i],Game.DOCK_Y[i]);
            check(sx>=0 && sx<=layout.width && sy>=0 && sy<=layout.height,"actual inland tie targets remain on screen");
        }
        for(Layout.Box original:BUTTONS) {
            Layout.Box box=layout.box(original);
            check(box.x>=0 && box.y>=0 && box.x+box.w<=layout.width && box.y+box.h<=layout.height,"all menu and control targets fit the canvas");
        }
        for(Layout.Box original:LIVE_BUTTONS) {
            Layout.Box box=layout.box(original);
            // Controls overlay only the shore and outer apron, never navigable water.
            check(!overlapsWater(camera,box),"live controls leave the projected navigable channel clear");
        }
    }
    private static void menuAndGestureRouting(Game game) {
        Layout layout=game.layout;
        tap(game,Layout.SETTINGS);
        boolean haptics=game.hapticsEnabled; tap(game,Layout.HAPTICS);
        check(game.hapticsEnabled!=haptics,"responsive settings haptic target routes");game.back();
        check(game.mode==Game.TITLE,"Back returns settings to title");
        tap(game,Layout.START);
        check(game.mode==Game.PLAY && game.layout.width==layout.width && game.layout.height==layout.height,"start preserves responsive canvas");
        ReplayInput.down(game,game.tug.x,game.tug.y);
        check(game.holdingTug(),"screen-space tug gesture selects the same physical hull");
        ReplayInput.move(game,game.downX,game.downY-90);
        for(int i=0;i<12;i++) game.step();
        check(game.manualSteering && game.navigating,"responsive drag drives shared navigation");
        ReplayInput.up(game,game.pointerX,game.pointerY);
        check(!game.navigating && !game.pointer,"responsive release cuts throttle");
        game.back(); check(game.mode==Game.PAUSE,"Back pauses gameplay");
        tap(game,Layout.RESUME); check(game.mode==Game.PLAY,"responsive resume target routes");
        tap(game,Layout.HELP); check(game.mode==Game.HELP,"responsive help target routes");
        tap(game,Layout.START); check(game.mode==Game.PLAY,"responsive help return routes");
        game.back(); tap(game,Layout.RESTART);
        check(game.mode==Game.PLAY && game.ticks==0 && game.layout.width==layout.width && game.layout.height==layout.height,"restart preserves canvas and resets shift");
        game.mode=Game.FINISH; tap(game,Layout.AGAIN);
        check(game.mode==Game.PLAY && game.layout.width==layout.width && game.layout.height==layout.height,"results replay uses responsive target");
    }
    private static void practiceAndResize(Game game) {
        float originalX=game.tug.x,originalY=game.tug.y;
        tap(game,Layout.PRACTICE); Game practice=game.activeGame();
        for(int i=0;i<480 && practice.tutorial.entering;i++)game.step();
        int practiceTicks=practice.ticks;
        check(practice!=game && practice.layout.width==game.layout.width && practice.layout.height==game.layout.height,"practice inherits the exact current layout");
        ReplayInput.down(game,practice.tug.x,practice.tug.y);
        check(practice.holdingTug() && !game.pointer,"root pointer routes through the practice camera");
        Layout unchanged=game.layout;
        game.configureCanvas(unchanged.width,unchanged.height);
        check(game.layout==unchanged && practice.holdingTug(),"identical host bounds preserve an active gesture");
        game.configureCanvas(unchanged.width+96,unchanged.height+40);
        check(!practice.pointer && !practice.navigating,"resizing cancels gestures before changing projection");
        check(practice.layout.width==game.layout.width && practice.layout.height==game.layout.height,"active practice receives resized bounds");
        check(game.tug.x==originalX && game.tug.y==originalY && game.ticks==0 && practice.ticks==practiceTicks,"resizing cannot advance either simulation");
        checkPracticeLayout(practice);
        ReplayInput.down(game,practice.tug.x,practice.tug.y);
        check(practice.holdingTug(),"new gesture works after camera resize");
        ReplayInput.up(game,practice.tug.x,practice.tug.y); tap(game,Layout.PRACTICE);
        check(game.activeGame()==game && game.mode==Game.PLAY,"practice exit returns to resized live shift");
        game.mode=Game.TITLE; tap(game,Layout.LEARN);
        check(game.activeGame()!=game && game.activeGame().layout.width==game.layout.width && game.activeGame().layout.height==game.layout.height,"title learn target inherits responsive layout");
        tap(game,Layout.PRACTICE); check(game.activeGame()==game && game.mode==Game.TITLE,"leaving title practice returns to title");
    }
    private static void checkPracticeLayout(Game g) {
        Layout layout=g.layout;RiverCamera camera=layout.camera;
        float tx=camera.screenX(g.tug.x,g.tug.y),ty=camera.screenY(g.tug.x,g.tug.y);
        check(tx>80 && tx<layout.width-80 && ty>100 && ty<layout.height-80,
                "resized practice camera keeps the current tug visible in the long connected scene");
        for(int gate=0;gate<TrainingChannel.X.length;gate++) {
            float x=TrainingChannel.X[gate],y=TrainingChannel.Y[gate];
            float sx=camera.screenX(x,y),sy=camera.screenY(x,y);
            check(near(camera.worldX(sx,sy),x) && near(camera.worldY(sx,sy),y),
                    "tutorial camera inverse preserves both on-screen and downstream world positions");
        }
        for(Layout.Box original:BUTTONS) {
            Layout.Box box=layout.box(original);
            check(box.x>=0 && box.y>=0 && box.x+box.w<=layout.width && box.y+box.h<=layout.height,
                    "all tutorial control targets fit the resized canvas");
        }
        for(Layout.Box original:LIVE_BUTTONS) {
            Layout.Box box=layout.box(original);
            check(tx+40<box.x || tx-40>box.x+box.w || ty+40<box.y || ty-40>box.y+box.h,
                    "tutorial follow camera leaves the tug clear of live controls");
        }
    }
    private static void independentGames() {
        Game first=new Game(),second=new Game(); first.configureCanvas(1400,600);
        Layout layout=first.layout; float sx=layout.camera.screenX(376,700),sy=layout.camera.screenY(376,700);
        second.configureCanvas(960,1100);
        check(first.layout==layout && first.layout.width==1400 && first.layout.height==600,"second game cannot overwrite first layout");
        check(first.layout.camera.screenX(376,700)==sx && first.layout.camera.screenY(376,700)==sy,"camera transforms remain instance-local");
        tap(first,Layout.START); tap(second,Layout.START);
        ReplayInput.down(first,first.tug.x,first.tug.y); ReplayInput.down(second,second.tug.x,second.tug.y);
        check(first.holdingTug() && second.holdingTug(),"different canvases accept the same world gesture independently");
        ReplayInput.move(first,376,610); ReplayInput.move(second,376,610);
        for(int i=0;i<60;i++) { first.step(); second.step(); }
        check(first.ticks==second.ticks && Math.abs(first.tug.x-second.tug.x)<.01f
                && Math.abs(first.tug.y-second.tug.y)<.01f,"canvas aspect does not change movement speed or simulation geometry");
        layout=first.layout; // Follow/zoom may have advanced during the sixty ticks.
        second.configureCanvas(1800,600);
        check(first.layout==layout && first.pointer,"resizing another game cannot cancel the first gesture");
    }
    private static void cameraMath() {
        RiverCamera camera=new RiverCamera(-71,203,2.4f);
        float x=302,y=519,sx=camera.screenX(x,y),sy=camera.screenY(x,y);
        check(near(camera.worldX(sx,sy),x) && near(camera.worldY(sx,sy),y),"translated and zoomed camera preserves exact input inverse");
        check(camera.contains(sx,sy) && !camera.contains(camera.left-1,camera.top),"camera containment follows its translated projection");
        check(near(camera.width(),1008*RiverCamera.Q*2.4f) && near(camera.height(),1008*RiverCamera.Q*RiverCamera.DEPTH*2.4f),"camera bounds include all four projected world corners");
        for(float zoom:new float[]{1,1.17f,1.4f}) {
            camera=new RiverCamera(37,81,1.3f*zoom,-470,1020,zoom);
            for(float wx:new float[]{-200,120,480,1460}) for(float wy:new float[]{-520,290,817}) {
                sx=camera.screenX(wx,wy);sy=camera.screenY(wx,wy);
                check(near(camera.worldX(sx,sy),wx) && near(camera.worldY(sx,sy),wy),"scrolled and zoomed isometric inverse preserves both axes");
            }
            for(float rise:new float[]{-3,0,4}) {
                float baseX=camera.screenX(310,540),baseY=camera.screenY(310,540);
                float liftedX=camera.screenX(310+RiverCamera.riseX(rise),540+RiverCamera.riseY(rise));
                float liftedY=camera.screenY(310+RiverCamera.riseX(rise),540+RiverCamera.riseY(rise));
                check(near(baseX,liftedX) && near(liftedY,baseY-rise*camera.scale),
                        "wave heave lifts upright sprites vertically without screen-sideways drift");
            }
            GroundCapture capture=new GroundCapture();new RiverPainter(capture,camera).rect(170,215,260,602,0xff112233);
            check(capture.points!=null && capture.points.length==8 && capture.rects==0,"ground rectangles draw as four-corner projected polygons");
            float[] world={170,215,430,215,430,817,170,817};
            for(int i=0;i<8;i+=2) {
                boolean found=false;
                for(int j=0;j<8;j+=2) if(near(capture.points[j],camera.screenX(world[i],world[i+1]))
                        && near(capture.points[j+1],camera.screenY(world[i],world[i+1]))) found=true;
                check(found,"projected ground footprint retains every collision corner");
            }
        }
    }
    /** Separating axes of the screen rectangle and the projected water parallelogram. */
    private static boolean overlapsWater(RiverCamera camera,Layout.Box box) {
        float[] world={Physics.LEFT,Physics.TOP,Physics.RIGHT,Physics.TOP,Physics.RIGHT,Physics.BOTTOM,Physics.LEFT,Physics.BOTTOM};
        float[] water=new float[8],control={box.x,box.y,box.x+box.w,box.y,box.x+box.w,box.y+box.h,box.x,box.y+box.h};
        for(int i=0;i<8;i+=2) {water[i]=camera.screenX(world[i],world[i+1]);water[i+1]=camera.screenY(world[i],world[i+1]);}
        float[][] axes={{1,0},{0,1},{RiverCamera.DEPTH,1},{RiverCamera.DEPTH,-1}};
        for(float[] axis:axes) {
            float wmin=Float.POSITIVE_INFINITY,wmax=Float.NEGATIVE_INFINITY,cmin=wmin,cmax=wmax;
            for(int i=0;i<8;i+=2) {
                float wp=water[i]*axis[0]+water[i+1]*axis[1],cp=control[i]*axis[0]+control[i+1]*axis[1];
                wmin=Math.min(wmin,wp);wmax=Math.max(wmax,wp);cmin=Math.min(cmin,cp);cmax=Math.max(cmax,cp);
            }
            if(wmax<=cmin || cmax<=wmin) return false;
        }
        return true;
    }
    private static final class GroundCapture implements Painter {
        float[] points;int rects;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {}
        public void spriteBlend(int a,int f,int next,float x,float y,float w,float h,float r,float blend,float alpha) {}
        public void rect(float x,float y,float w,float h,int c) {rects++;}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {points=xy;}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
    }

}
