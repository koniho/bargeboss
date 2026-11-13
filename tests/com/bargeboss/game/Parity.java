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
    public static String signature() {
        Parity p=new Parity(); Game g=new Game(); Renderer.draw(g,p); g.reset();
        for(int i=0;i<Game.SHIFT_TICKS;i++) {
            if(!g.pointer) for(Body b:g.cargo) if(b!=null && b.active) { g.down(b.x,b.y); break; }
            if(i==300) { g.background(); Renderer.draw(g,p); g.pause(); }
            if(i==1200) { g.cancel(); g.down(385,600); g.up(385,600); }
            if(i==1260) g.cancel();
            g.step();
            if(i%60==0) {
                p.add(g.ticks); p.add(g.score); p.add(g.delivered); p.add(g.hull);
                p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle);
                for(Body b:g.cargo) if(b!=null) { p.add(b.id); p.add(b.active?1:0); p.value(b.x); p.value(b.y); p.value(b.angle); }
            }
            if(i%600==0) Renderer.draw(g,p);
        }
        Renderer.draw(g,p); return Integer.toHexString(p.hash)+":"+g.delivered+":"+g.score;
    }
    public static void main(String[] args) { System.out.println(signature()); }
}
