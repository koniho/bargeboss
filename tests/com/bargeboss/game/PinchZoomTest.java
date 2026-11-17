package com.bargeboss.game;

public final class PinchZoomTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game scene(float width){
        Game g=new Game();g.mode=Game.PLAY;g.configureCanvas(width,600);
        g.tug.x=-5000;g.tug.y=-5000;
        for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
        return g;
    }
    public static void run(){
        for(float width:new float[]{960,1333}){
            Game g=scene(width);RiverCamera c=g.layout.camera;
            float wx=c.worldX(600,300),wy=c.worldY(600,300),z=c.zoom;
            g.pointerDown(11,500,300);g.pointerDown(22,700,300);
            check(g.pinchZoom.active() && !g.pointer,"pinch takes empty-scene gesture without thrust");
            g.pointerMove(11,400,300);g.pointerMove(22,800,300);
            check(Math.abs(g.layout.camera.zoom-z*2)<.001f,"spread doubles zoom");
            check(Math.abs(g.layout.camera.screenX(wx,wy)-600)<.01f && Math.abs(g.layout.camera.screenY(wx,wy)-300)<.01f,"pinch midpoint retains its world anchor");
            g.pointerUp(11,400,300);float zoom=g.layout.camera.zoom;
            g.pointerMove(22,820,300);check(g.layout.camera.zoom==zoom && !g.pointer,"remaining finger cannot steer or change zoom");
            g.pointerUp(22,820,300);check(!g.pinchZoom.active() && !g.pointer,"both releases end pinch cleanly");
            for(int i=0;i<PinchZoom.HOLD_TICKS;i++)check(Math.abs(g.pinchZoom.target(1,false)-zoom)<.001f,"six second manual hold");
            float previous=zoom;
            for(int i=0;i<PinchZoom.RETURN_TICKS;i++){
                float target=g.pinchZoom.target(1,false);
                check(target<=previous+.00001f && previous-target<.02f,"smooth monotonic auto return");previous=target;
            }
            check(g.pinchZoom.target(1,false)==1,"auto zoom regains control");
            g.pointerDown(31,500,300);g.pointerDown(32,700,300);g.pointerMove(31,-10000,300);
            check(g.layout.camera.zoom==PinchZoom.MAX,"zoom in bounded");
            g.pointerMove(31,699,300);check(g.layout.camera.zoom==PinchZoom.MIN,"zoom out bounded");
            g.cancelInputs();check(!g.pinchZoom.active(),"lifecycle cancellation clears pinch");
            float held=g.pinchZoom.target(2,true);
            for(int i=0;i<1000;i++)check(g.pinchZoom.target(2,true)==held,"active steering pauses return timer");
            g.reset();check(g.pinchZoom.target(2,false)==2,"new level clears manual override");
        }
        Game root=new Game();root.startTutorial();Game g=root.activeGame();g.tutorial.entering=false;
        g.tutorial.lesson=Tutorial.TIE;g.tutorial.setup(g);Body b=g.cargo[0];
        ReplayInput.down(root,b.x,b.y);RiverCamera c=g.layout.camera;
        root.pointerDown(99,600,300);root.pointerMove(99,750,300);
        check(g.ropeDragging && g.layout.camera==c && !g.pinchZoom.active(),"second finger cannot disturb lasso projection");
        root.cancelInputs();
        System.out.println("Pinch zoom anchors, bounds, release, auto-return and lasso protection passed");
    }
}
