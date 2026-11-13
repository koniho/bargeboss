package com.bargeboss.game;

/** Translated by J2ObjC too: CI compares this state/drawing trace with the JVM output. */
public final class Parity implements Painter {
    private int hash=1;
    private void add(int value) { hash=31*hash+value; }
    private void value(float f) { add(Math.round(f*100)); }
    public void rect(float x,float y,float w,float h,int c) { add(1); value(x); value(y); value(w); value(h); add(c); }
    public void circle(float x,float y,float r,int c) { add(2); value(x); value(y); value(r); add(c); }
    public void poly(float[] xy,int c) { add(3); for(float f:xy) value(f); add(c); }
    public void line(float x,float y,float xx,float yy,float w,int c) { add(4); value(x); value(y); value(xx); value(yy); value(w); add(c); }
    public static int frame(Game g) { Parity p=new Parity(); Renderer.draw(g,p); return p.hash; }
    public static String signature() {
        Parity p=new Parity(); Game g=new Game(); Renderer.draw(g,p);
        for(int i=0;i<90;i++) { g.advance(1.0/60); if(i%30==0) Renderer.draw(g,p); }
        g.reset();
        for(int i=0;i<Game.SHIFT_TICKS;i++) {
            ReplayInput.work(g);
            if(i==300) { g.background(); Renderer.draw(g,p); g.pause(); }
            if(i==1200) { g.cancel(); g.down(g.tug.x,g.tug.y); g.move(g.downX,g.downY-90); }
            if(i==1260) g.cancel();
            g.step();
            if(i%60==0) {
                p.add(g.ticks); p.add(g.score); p.add(g.delivered); p.add(g.hull); p.add(g.ropePhase); p.add(g.ropeTicks);
                p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle);
                for(Body b:g.cargo) if(b!=null) { p.add(b.id); p.add(b.active?1:0); p.value(b.x); p.value(b.y); p.value(b.angle); p.add(b.lineAttached?1:0); p.add(b.unloadingTicks); p.add(b.lineSnapTicks); }
            }
            if(i%600==0) Renderer.draw(g,p);
        }
        Renderer.draw(g,p);
        for(int i=0;i<60;i++) { g.advance(1.0/60); if(i%20==0) Renderer.draw(g,p); }
        tutorial(p);
        return Integer.toHexString(p.hash)+":"+g.delivered+":"+g.score;
    }
    private static void tap(Game root,float x,float y) { root.pointerDown(0,x,y); root.pointerUp(0,x,y); }
    private static void tutorial(Parity p) {
        Game root=new Game(); tap(root,300,608);
        for(int i=0;i<2400;i++) {
            Game g=root.activeGame();
            if(g.tutorial.lesson==Tutorial.DONE) { Renderer.draw(root,p); return; }
            ReplayInput.tutorial(root);
            root.step(); p.add(g.tutorial.lesson); p.add(g.control); p.add(root.pollHaptic());
            p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle); p.add(g.ropeDragging?1:0); p.add(g.ropePhase); p.add(g.ropeTicks);
            if(i%30==0) Renderer.draw(root,p);
        }
        throw new IllegalStateException("Tutorial replay did not finish");
    }
    public static void main(String[] args) { System.out.println(signature()); }
}
