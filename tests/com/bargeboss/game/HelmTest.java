package com.bargeboss.game;

public final class HelmTest {
    private static int count;
    private static void check(boolean condition,String message) { count++; if(!condition) throw new AssertionError(message); }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static void tap(Game g,float x,float y) { g.pointerDown(0,x,y); g.pointerUp(0,x,y); }
    private static Game game() { Game g=new Game(); g.reset(); return g; }
    public static void run() {
        Game g=game();
        g.pointerDown(10,110,825); check(g.helm.throttle>.5f,"lever engages forward");
        g.pointerDown(20,510,865); check(g.helm.rudder>.5f,"second finger can steer while holding throttle");
        g.pointerMove(10,110,910); check(g.helm.throttle<-.5f && g.helm.rudder>.5f,"independent finger ownership");
        g.pointerUp(20,510,865); check(g.helm.rudder==0 && g.helm.throttle<-.5f,"wheel springs home without changing throttle");
        g.pointerUp(10,110,910); check(g.helm.throttle<-.5f,"throttle stays set on normal release");
        g.background(); check(g.helm.throttle==0 && g.helm.rudder==0 && g.mode==Game.PAUSE,"background kills latched thrust");
        g.pause(); g.pointerDown(30,110,810); g.pointerCancel(30); check(g.helm.throttle==0,"cancelled throttle goes neutral");
        g=game(); tap(g,110,825); float start=g.tug.x; steps(g,120); check(g.tug.x<start-20,"forward thrust moves along bow");
        tap(g,290,908); steps(g,40); check(g.tug.speed()<5 && g.helm.throttle==0,"brake stops helm thrust");
        for(Body b:g.cargo) if(b!=null) b.active=false;
        start=g.tug.x; tap(g,110,910); steps(g,90); check(g.tug.x>start+10,"reverse backs away");
        g=game(); tap(g,110,825); g.pointerDown(1,510,865); float angle=g.tug.angle;
        steps(g,90); check(Math.abs(Physics.angle(g.tug.angle-angle))>.3,"wheel changes heading through physics");
        g.pointerCancel(1); check(g.helm.rudder==0,"cancelled wheel centers");
        g=game(); tap(g,290,790); check(!g.helm.enabled,"tap assist remains available");
        tap(g,RiverPainter.screenX(360),RiverPainter.screenY(580));
        check(g.navigating && Math.abs(g.targetX-360)<.01 && Math.abs(g.targetY-580)<.01,"river draw/input inverse match");
        g.down(g.cargo[0].x,g.cargo[0].y); steps(g,20); g.up(g.downX,g.downY);
        check(!g.navigating && !g.pushing,"release never resumes stale navigation");
        // Haptic patterns are semantic, edge-triggered, rate-limited, and optional.
        g=game(); tap(g,110,825); check(g.pollHaptic()==Game.HAPTIC_TICK,"throttle detent tick");
        check(g.pollHaptic()==0,"event drains once");
        g.pointerDown(1,510,865); check(g.pollHaptic()==0,"rapid detents rate limited");
        g.haptic(Game.HAPTIC_IMPACT); g.haptic(Game.HAPTIC_SUCCESS);
        check(g.pollHaptic()==Game.HAPTIC_SUCCESS,"delivery wins simultaneous feedback");
        tap(g,438,55); check(!g.hapticsEnabled,"haptics switch turns off");
        g.haptic(Game.HAPTIC_SUCCESS); check(g.pollHaptic()==0,"disabled means no event");
        g=game(); g.tug.x=340; g.tug.y=700; steps(g,1);
        check(g.pollHaptic()==Game.HAPTIC_CONTACT,"real contact generates feedback");
        steps(g,1); check(g.pollHaptic()==0,"sustained contact does not buzz each frame");
        g=game(); g.tug.x=479; g.tug.vx=100; steps(g,1);
        check(g.pollHaptic()==Game.HAPTIC_IMPACT,"hard collision feedback");
        g=game(); tap(g,110,831); int guard=1200;
        while(g.delivered==0 && guard-->0) g.step();
        check(g.delivered==1,"a novice can deliver first cargo with gentle forward throttle");
        check(g.pollHaptic()==Game.HAPTIC_SUCCESS,"delivery feedback");
        Game root=new Game(); tap(root,300,608); g=root.activeGame();
        check(g!=root && g.tutorial!=null,"learn button starts separate practice");
        steps(root,2000); check(g.tutorial.lesson==Tutorial.THROTTLE && g.delivered==0 && g.hull==100,"waiting does not advance lessons or apply pressure");
        tap(root,110,831); untilLesson(root,Tutorial.WHEEL,300);
        root.pointerDown(1,510,865); untilLesson(root,Tutorial.STOP,300); root.pointerUp(1,510,865);
        tap(root,290,908); untilLesson(root,Tutorial.REVERSE,200);
        tap(root,110,900); steps(root,100); check(g.tutorial.reversed,"reverse lesson requires actual backward motion");
        tap(root,290,908); untilLesson(root,Tutorial.DOCK,200);
        tap(root,110,831); untilLesson(root,Tutorial.DONE,1200);
        check(g.delivered==1 && g.mode==Game.PLAY && g.helm.throttle==0,"practice finishes safely after a real delivery");
        tap(root,300,680); check(root.activeGame()==root && root.mode==Game.PLAY && root.ticks==0,"complete practice begins a fresh shift");
        root=game(); root.score=321; steps(root,12); int ticks=root.ticks; float x=root.tug.x,y=root.tug.y;
        tap(root,290,848); tap(root,300,608); steps(root,300); tap(root,290,848);
        check(root.activeGame()==root && root.mode==Game.PLAY && root.score==321 && root.ticks==ticks && root.tug.x==x && root.tug.y==y,"practice preserves suspended shift");
        root=new Game(); tap(root,300,608); root.background();
        check(root.activeGame().mode==Game.PAUSE,"practice pauses on background");
        tap(root,300,570); tap(root,290,848); check(root.activeGame()==root && root.mode==Game.TITLE,"early practice exit returns to title");
        System.out.println(count+" helm, haptic and tutorial assertions passed");
    }
    private static void untilLesson(Game root,int lesson,int limit) {
        while(root.activeGame().tutorial.lesson!=lesson && limit-->0) root.step();
        Game g=root.activeGame();
        check(g.tutorial.lesson==lesson,"reach lesson "+lesson+", was "+g.tutorial.lesson+", tug "+g.tug.x+","+g.tug.y);
    }
}
