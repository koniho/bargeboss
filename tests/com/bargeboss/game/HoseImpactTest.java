package com.bargeboss.game;
public final class HoseImpactTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Game fixture(){
        Game root=new Game();root.configureCanvas(1243,600);ReplayInput.startPractice(root);
        Game g=root.activeGame();g.tutorial.lesson=Tutorial.EXTINGUISH;g.tutorial.setup(g);
        g.abilities.reveal(g,g.cargo[3]);return g;
    }
    static int spray(Game g,float x,float y){
        g.pointerDown(88,g.abilities.buttonX(g,0),g.abilities.buttonY(g));ReplayInput.hoseAim(g,88,x,y);
        int most=0;for(int i=0;i<110;i++){g.abilities.step(g);most=Math.max(most,g.abilities.impactCount());}
        g.pointerCancel(88);return most;
    }
    public static void run(){
        Game g=fixture();Body b=g.cargo[3];
        check(spray(g,b.x-100,b.y)==0,"open water produces no dry-surface impact");
        g=fixture();b=g.cargo[3];g.pointerDown(88,g.abilities.buttonX(g,0),g.abilities.buttonY(g));ReplayInput.hoseAim(g,88,b.x-100,b.y);
        int splashes=0;for(int i=0;i<110;i++){g.abilities.step(g);splashes=Math.max(splashes,g.abilities.waterSplashCount());}
        g.pointerCancel(88);check(splashes>0,"open water landings ripple");
        g=fixture();check(spray(g,FireworksLesson.X+15,FireworksLesson.Y)>0,"wood hits splash");
        g=fixture();check(spray(g,465,-1230)>0,"bare quay hits splash");
        for(int i=0;i<100;i++)g.abilities.step(g);check(g.abilities.impactCount()==0,"impact stamps expire");
        g=fixture();b=g.cargo[3];for(int i=0;i<10;i++)g.abilities.step(g);
        float alpha=g.abilities.opacity();ReplayInput.tap(g,600,285);
        check(g.abilities.selected==null && g.abilities.opacity()==alpha,"empty play-area tap deselects without opacity jump");
        g.abilities.reveal(g,b);ReplayInput.throttleDown(g);g.pointerUp(0,Joystick.centerX(g),Joystick.centerY(g));
        check(g.abilities.selected==b,"active throttle tap preserves selection");
        RiverCamera c=g.layout.camera;
        b.x=c.worldX(-10,300);b.y=c.worldY(-10,300);
        check(g.abilities.inView(g,b),"partially visible hull remains in view");
        b.x=c.worldX(-700,300);b.y=c.worldY(-700,300);
        g.abilities.step(g);check(g.abilities.selected==null && !g.abilities.spraying,"offscreen hull deselects and stops nozzle");
        System.out.println("Hose impact stamps, empty-tap exclusion, partial visibility and offscreen deselection passed");
    }
    public static void main(String[]args){run();}
}
