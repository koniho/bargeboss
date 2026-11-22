package com.bargeboss.game;

/** Hold training must recognize a physical push, including a finger that moves. */
public final class TutorialHoldTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static Game lesson(float width,float height) {
        Game root=new Game();root.configureCanvas(width,height);root.startTutorial();
        Game g=root.activeGame();g.tutorial.lesson=Tutorial.HOLD;g.tutorial.setup(g);return root;
    }
    public static void run() {
        for(float[] size:new float[][]{{960,600},{1333,600},{960,720}}) for(boolean drag:new boolean[]{true,false}) {
            Game root=lesson(size[0],size[1]),g=root.activeGame();Body b=g.cargo[0];
            RiverCamera camera=g.layout.camera;
            for(float dx:new float[]{-37,0,37}) for(float dy:new float[]{-8,8}) {
                float x=camera.screenX(b.x,b.y)+dx,y=camera.screenY(b.x,b.y)+dy;
                root.pointerDown(0,x,y);
                check(g.heldCargo()==b && !g.holdingTug(),"push label selects cargo across its visible face");
                root.pointerUp(0,x,y);
            }
            ReplayInput.down(root,b.x,b.y);
            if(!drag) {
                steps(root,60);
                check(g.tutorial.lesson==Tutorial.HOLD,"push lesson waits for the demonstrated drag");
            }
            ReplayInput.move(root,g.downX-65,g.downY-15);
            int frames=0;
            while(g.tutorial.lesson==Tutorial.HOLD && frames++<1200) { ReplayInput.pushPractice(root);root.step(); }
            check(g.tutorial.lesson==Tutorial.STEER,"hold must finish after real "+(drag?"steered":"assisted")+" push on "+size[0]+"x"+size[1]);
            check(Physics.length(b.x-Tutorial.PUSH_X,b.y-Tutorial.PUSH_Y)<=Tutorial.PUSH_RADIUS,"lesson requires pushing cargo into the destination zone");
            ReplayInput.up(root,g.pointerX,g.pointerY);
            for(int i=0;i<7000 && g.tutorial.lesson!=Tutorial.DONE;i++) { ReplayInput.tutorial(root);root.step(); }
            check(g.tutorial.lesson==Tutorial.DONE,"remaining lessons remain completable after holding: "+size[0]+"x"+size[1]+" drag="+drag+" lesson="+g.tutorial.lesson+" tug="+g.tug.x+","+g.tug.y+" radio="+g.tutorial.towBridges.current(g)+"/"+g.tutorial.towBridges.phase+" contact="+g.tutorial.towBridges.contactable(g)+" tow="+g.tow.count()+" house="+g.tutorial.towBridges.houseX(g,1)+","+g.tutorial.towBridges.houseY(g,1)+" zoom="+g.layout.camera.zoom);
        }
        Game root=lesson(960,600),g=root.activeGame();Body b=g.cargo[0];
        steps(root,7200);
        check(b.x==292 && b.y==700 && g.tutorial.lesson==Tutorial.HOLD,"reading the card cannot drift the teaching cargo away");
        ReplayInput.down(root,b.x,b.y);steps(root,5);ReplayInput.up(root,b.x,b.y);steps(root,120);
        check(g.tutorial.lesson==Tutorial.HOLD,"brief tap cannot complete hold lesson");
        ReplayInput.down(root,g.tug.x,g.tug.y);steps(root,90);
        check(!g.holdingTug() && g.tutorial.lesson==Tutorial.HOLD,"holding tug cannot complete cargo lesson");
        g.pointerCancel(0);ReplayInput.down(root,b.x,b.y);steps(root,5);g.pointerCancel(0);steps(root,60);
        check(g.tutorial.lesson==Tutorial.HOLD,"cancelled hold cannot complete lesson");
        ReplayInput.down(root,b.x,b.y);ReplayInput.move(root,g.downX-65,g.downY-65);
        for(int i=0;i<1200 && g.tutorial.lesson==Tutorial.HOLD;i++) { ReplayInput.pushPractice(root);root.step(); }
        check(g.tutorial.lesson==Tutorial.STEER,"retry after waiting or wrong target still works");
        root=lesson(960,600);g=root.activeGame();b=g.cargo[0];
        ReplayInput.down(root,b.x,b.y);ReplayInput.move(root,g.downX-65,g.downY-15);steps(root,75);
        check(Physics.length(b.x-292,b.y-700)>3 && g.tutorial.lesson==Tutorial.HOLD,"a small push away from the target cannot complete delivery exercise");
        System.out.println(checks+" hold tutorial, moving finger, delayed input and continuation assertions passed");
    }
    public static void main(String[] args) { run(); }
}
