package com.bargeboss.game;

/** Translated by J2ObjC too: CI compares this state/drawing trace with the JVM output. */
public final class Parity implements Painter {
    private static boolean tracing;
    private int hash=1,ordinal;
    private void mark(String label) { if(tracing) System.out.println("COMMAND "+label); }
    public static int filenameHash(String value) {
        // J2ObjC maps String.hashCode() to NSString.hash, whose algorithm is native.
        // Explicit UTF-16 hashing preserves Java's trace value on every platform.
        int result=0;
        for(int i=0;i<value.length();i++) result=31*result+value.charAt(i);
        return result;
    }
    private void add(int value) { hash=31*hash+value; if(tracing) System.out.println((ordinal++)+" "+value+" "+hash); }
    private void value(float f) { add(Math.round(f*100)); }
    public void rect(float x,float y,float w,float h,int c) { mark("rect"); add(1); value(x); value(y); value(w); value(h); add(c); }
    public void circle(float x,float y,float r,int c) { mark("circle"); add(2); value(x); value(y); value(r); add(c); }
    public void poly(float[] xy,int c) { mark("poly"); add(3); for(float f:xy) value(f); add(c); }
    public void line(float x,float y,float xx,float yy,float w,int c) { mark("line"); add(4); value(x); value(y); value(xx); value(yy); value(w); add(c); }
    public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
        mark("sprite"); add(5); add(atlas); add(frame); value(cx); value(cy); value(width); value(height); value(rotation); value(alpha);
        // Registration and file choice are part of the shared renderer contract too.
        add(filenameHash(SpriteAtlas.file(atlas))); value(SpriteAtlas.pivotX(atlas,frame)); value(SpriteAtlas.pivotY(atlas,frame));
    }
    public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) {
        mark("spriteBlend"); add(6); add(atlas); add(frameA); add(frameB);
        value(cx); value(cy); value(width); value(height); value(rotation); value(blend); value(alpha);
        add(filenameHash(SpriteAtlas.file(atlas))); add(SpriteAtlas.columns(atlas)); add(SpriteAtlas.rows(atlas)); add(SpriteAtlas.count(atlas));
        value(SpriteAtlas.pivotX(atlas,frameA)); value(SpriteAtlas.pivotY(atlas,frameA));
        value(SpriteAtlas.pivotX(atlas,frameB)); value(SpriteAtlas.pivotY(atlas,frameB));
    }
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
        Game root=new Game(); ReplayInput.tap(root,Layout.LEARN);
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
    public static void diagnostic() {
        tracing=true;
        try { System.out.println("SIGNATURE "+signature()); } finally { tracing=false; }
    }
    public static void main(String[] args) {
        if(args.length>0 && args[0].equals("--trace")) diagnostic(); else System.out.println(signature());
    }
}
