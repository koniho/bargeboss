package com.bargeboss.game;

/** Exercise the public touch boundary across windows without changing the simulation geometry. */
public final class ResponsiveTest {
    private static int assertions;
    private static final Layout.Box[] BUTTONS={Layout.HAPTICS,Layout.PAUSE,Layout.HELP,
            Layout.PRACTICE,Layout.LEARN,Layout.START,Layout.RESUME,Layout.RESTART,Layout.AGAIN};
    private static final Layout.Box[] LIVE_BUTTONS={Layout.HAPTICS,Layout.PAUSE,Layout.HELP,Layout.PRACTICE};
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
            Game game=new Game(); game.configureCanvas(viewport.logicalWidth(),viewport.logicalHeight());
            checkLayout(game.layout); menuAndGestureRouting(game); practiceAndResize(game);
        }
        independentGames(); cameraMath();
        System.out.println(assertions+" responsive canvas, camera and pointer-routing assertions passed");
    }
    private static void checkLayout(Layout layout) {
        RiverCamera camera=layout.camera;
        check(camera.left>=0 && camera.top>=0 && camera.left+camera.width()<=layout.width+.002f
                && camera.top+camera.height()<=layout.height+.002f,"entire river rectangle fits the canvas");
        float[] xs={RiverPainter.WORLD_LEFT,Physics.LEFT,Game.DOCK_X[0],Game.TIE_X[1],Physics.RIGHT,RiverPainter.WORLD_RIGHT};
        float[] ys={RiverPainter.WORLD_TOP,Physics.TOP,Game.DOCK_Y[1],Physics.BOTTOM,RiverPainter.WORLD_BOTTOM};
        for(float x:xs) for(float y:ys) {
            float sx=camera.screenX(x,y),sy=camera.screenY(x,y);
            check(sx>=-.002f && sx<=layout.width+.002f && sy>=-.002f && sy<=layout.height+.002f,"world, banks and tie points remain visible");
            check(near(camera.worldX(sx,sy),x) && near(camera.worldY(sx,sy),y),"camera pointer inverse preserves world coordinates");
        }
        float xDistance=Physics.length(camera.screenX(330,500)-camera.screenX(300,500),camera.screenY(330,500)-camera.screenY(300,500));
        float yDistance=Physics.length(camera.screenX(300,530)-camera.screenX(300,500),camera.screenY(300,530)-camera.screenY(300,500));
        check(near(xDistance,30*camera.scale*RiverCamera.DEPTH) && near(yDistance,30*camera.scale),"ground plane uses the same fixed depth projection on every host");
        for(Layout.Box original:BUTTONS) {
            Layout.Box box=layout.box(original);
            check(box.x>=0 && box.y>=0 && box.x+box.w<=layout.width && box.y+box.h<=layout.height,"all menu and control targets fit the canvas");
        }
        for(Layout.Box original:LIVE_BUTTONS) {
            Layout.Box box=layout.box(original);
            // Controls overlay only the shore and outer apron, never navigable water.
            float waterTop=camera.screenY(Physics.RIGHT,Physics.TOP),waterBottom=camera.screenY(Physics.LEFT,Physics.TOP);
            check(box.y+box.h<=waterTop || box.y>=waterBottom,"live controls leave the navigable channel clear");
        }
    }
    private static void menuAndGestureRouting(Game game) {
        Layout layout=game.layout;
        boolean haptics=game.hapticsEnabled; tap(game,Layout.HAPTICS);
        check(game.hapticsEnabled!=haptics,"responsive haptic target routes");
        tap(game,Layout.START);
        check(game.mode==Game.PLAY && game.layout==layout,"start preserves responsive layout");
        ReplayInput.down(game,game.tug.x,game.tug.y);
        check(game.holdingTug(),"screen-space tug gesture selects the same physical hull");
        ReplayInput.move(game,game.downX,game.downY-90);
        for(int i=0;i<12;i++) game.step();
        check(game.manualSteering && game.navigating,"responsive drag drives shared navigation");
        ReplayInput.up(game,game.pointerX,game.pointerY);
        check(!game.navigating && !game.pointer,"responsive release cuts throttle");
        tap(game,Layout.PAUSE); check(game.mode==Game.PAUSE,"responsive pause target routes");
        tap(game,Layout.RESUME); check(game.mode==Game.PLAY,"responsive resume target routes");
        tap(game,Layout.HELP); check(game.mode==Game.HELP,"responsive help target routes");
        tap(game,Layout.START); check(game.mode==Game.PLAY,"responsive help return routes");
        tap(game,Layout.PAUSE); tap(game,Layout.RESTART);
        check(game.mode==Game.PLAY && game.ticks==0 && game.layout==layout,"restart preserves canvas and resets shift");
        game.mode=Game.FINISH; tap(game,Layout.AGAIN);
        check(game.mode==Game.PLAY && game.layout==layout,"results replay uses responsive target");
    }
    private static void practiceAndResize(Game game) {
        float originalX=game.tug.x,originalY=game.tug.y;
        tap(game,Layout.PRACTICE); Game practice=game.activeGame();
        check(practice!=game && practice.layout.width==game.layout.width && practice.layout.height==game.layout.height,"practice inherits the exact current layout");
        ReplayInput.down(game,practice.tug.x,practice.tug.y);
        check(practice.holdingTug() && !game.pointer,"root pointer routes through the practice camera");
        Layout unchanged=game.layout;
        game.configureCanvas(unchanged.width,unchanged.height);
        check(game.layout==unchanged && practice.holdingTug(),"identical host bounds preserve an active gesture");
        game.configureCanvas(unchanged.width+96,unchanged.height+40);
        check(!practice.pointer && !practice.navigating,"resizing cancels gestures before changing projection");
        check(practice.layout.width==game.layout.width && practice.layout.height==game.layout.height,"active practice receives resized bounds");
        check(game.tug.x==originalX && game.tug.y==originalY && game.ticks==0 && practice.ticks==0,"resizing cannot advance either simulation");
        checkLayout(practice.layout);
        ReplayInput.down(game,practice.tug.x,practice.tug.y);
        check(practice.holdingTug(),"new gesture works after camera resize");
        ReplayInput.up(game,practice.tug.x,practice.tug.y); tap(game,Layout.PRACTICE);
        check(game.activeGame()==game && game.mode==Game.PLAY,"practice exit returns to resized live shift");
        game.mode=Game.TITLE; tap(game,Layout.LEARN);
        check(game.activeGame()!=game && game.activeGame().layout.width==game.layout.width && game.activeGame().layout.height==game.layout.height,"title learn target inherits responsive layout");
        tap(game,Layout.PRACTICE); check(game.activeGame()==game && game.mode==Game.TITLE,"leaving title practice returns to title");
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
        second.configureCanvas(1800,600);
        check(first.layout==layout && first.pointer,"resizing another game cannot cancel the first gesture");
    }
    private static void cameraMath() {
        RiverCamera camera=new RiverCamera(-71,203,2.4f);
        float x=302,y=519,sx=camera.screenX(x,y),sy=camera.screenY(x,y);
        check(near(camera.worldX(sx,sy),x) && near(camera.worldY(sx,sy),y),"translated and zoomed camera preserves exact input inverse");
        check(camera.contains(sx,sy) && !camera.contains(camera.left-1,camera.top),"camera containment follows its translated projection");
        check(near(camera.width(),688*2.4f) && near(camera.height(),320*2.4f*RiverCamera.DEPTH),"camera zoom preserves the fixed ground depth ratio");
    }
}
