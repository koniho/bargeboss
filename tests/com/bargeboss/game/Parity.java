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
        add(filenameHash(SpriteAtlas.file(atlas,frame))); add(SpriteAtlas.sourceFrame(atlas,frame)); value(SpriteAtlas.sourceLeft(atlas,frame)); value(SpriteAtlas.sourceTop(atlas,frame)); value(SpriteAtlas.sourceRight(atlas,frame)); value(SpriteAtlas.sourceBottom(atlas,frame)); value(SpriteAtlas.pivotX(atlas,frame)); value(SpriteAtlas.pivotY(atlas,frame));
        value(SpriteAtlas.widthScale(atlas,frame)); value(SpriteAtlas.heightScale(atlas,frame));
    }
    public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,
                            float rotation,float blend,float alpha) {
        mark("spriteBlend"); add(6); add(atlas); add(frameA); add(frameB);
        value(cx); value(cy); value(width); value(height); value(rotation); value(blend); value(alpha);
        add(filenameHash(SpriteAtlas.file(atlas,frameA))); add(SpriteAtlas.sourceFrame(atlas,frameA));
        add(filenameHash(SpriteAtlas.file(atlas,frameB))); add(SpriteAtlas.sourceFrame(atlas,frameB));
        add(SpriteAtlas.columns(atlas)); add(SpriteAtlas.rows(atlas)); add(SpriteAtlas.count(atlas));
        value(SpriteAtlas.pivotX(atlas,frameA)); value(SpriteAtlas.pivotY(atlas,frameA));
        value(SpriteAtlas.widthScale(atlas,frameA)); value(SpriteAtlas.heightScale(atlas,frameA));
        value(SpriteAtlas.pivotX(atlas,frameB)); value(SpriteAtlas.pivotY(atlas,frameB));
        value(SpriteAtlas.widthScale(atlas,frameB)); value(SpriteAtlas.heightScale(atlas,frameB));
    }
    public static int frame(Game g) { Parity p=new Parity(); Renderer.draw(g,p); return p.hash; }
    private void spriteGrades() {
        mark("spriteGrade");
        int[] samples={0xff808080,0xffffc02a,0xffaa6844,0xff23466a,0x804080c0,0x10e4b230,0x00112233};
        for(int atlas=0;atlas<=SpriteAtlas.GROUND;atlas++) {
            add(atlas);add(SpriteGrade.applies(atlas)?1:0);
            for(int pixel:samples) {
                add(SpriteGrade.argb(atlas,pixel));int a=pixel>>>24;
                byte[] rgba={(byte)SpriteGrade.premultiplied((pixel>>>16)&255,a),
                        (byte)SpriteGrade.premultiplied((pixel>>>8)&255,a),(byte)SpriteGrade.premultiplied(pixel&255,a),(byte)a};
                SpriteGrade.applyPremultipliedRgba(atlas,rgba);
                for(byte channel:rgba)add(channel&255);
            }
        }
    }
    public static String signature() {
        Parity p=new Parity();p.spriteGrades(); Game g=new Game(); Renderer.draw(g,p);
        for(int i=0;i<90;i++) { g.advance(1.0/60); if(i%30==0) Renderer.draw(g,p); }
        g.reset();
        for(int i=0;i<Game.SHIFT_TICKS;i++) {
            ReplayInput.work(g);
            if(i==300) { g.background(); Renderer.draw(g,p); g.pause(); }
            if(i==1200) { g.cancel(); g.down(g.tug.x,g.tug.y); g.move(g.downX,g.downY-90); }
            if(i==1260) g.cancel();
            g.step();
            if(i%60==0) {
                p.add(g.ticks); p.add(g.score); p.add(g.delivered); p.add(g.hull); p.add(g.ropePhase); p.add(g.ropeTicks); p.add(g.cameraOverviewTicks);
                p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle); p.value(g.tug.heave); p.value(g.tug.engineLoad); p.add(g.water.emitted);
                for(Body b:g.cargo) if(b!=null) { p.add(b.id); p.add(b.active?1:0); p.value(b.x); p.value(b.y); p.value(b.angle); p.add(b.lineAttached?1:0); p.add(b.mooringTicks); p.add(b.unloadingTicks); p.add(b.lineSnapTicks); p.add(b.tieBurstTicks); p.value(b.heave); p.add(b.wakeKickTicks); }
            }
            if(i%600==0) Renderer.draw(g,p);
        }
        Renderer.draw(g,p);
        for(int i=0;i<60;i++) { g.advance(1.0/60); if(i%20==0) Renderer.draw(g,p); }
        tutorial(p);
        responsive(p);
        progression(p);
        feeders(p);
        bridge(p);
        return Integer.toHexString(p.hash)+":"+g.delivered+":"+g.score;
    }
    private static void bridge(Parity p) {
        Game g=new Game();g.challengeId=1;g.reset();
        for(int i=0;i<9000;i++) {
            ReplayInput.work(g);g.step();
            if(i%30==0) {
                p.value(g.tug.x);p.value(g.tug.y);p.add(g.bridge.phase);p.add(g.bridge.ticks);
                p.value(g.layout.camera.zoom);p.value(g.layout.camera.worldTop);p.value(g.layout.camera.worldRight);
            }
            if(i%120==0) Renderer.draw(g,p);
        }
    }
    private static void feeders(Parity p) {
        Game g=new Game(); g.reset();
        for(int i=0;i<350;i++) {
            g.step();
            if(i%30==0) { Renderer.draw(g,p); for(Feeder f:g.feeders) {
                p.add(f.phase); p.add(f.drops); p.add(f.shoutTicks); p.add(f.blocked?1:0); p.value(f.boat.y);
            } }
        }
        g.reset(); g.tug.x=260; g.tug.y=760;
        for(int i=0;i<410;i++) g.step();
        Renderer.draw(g,p); p.add(g.feeders[0].blockedTicks);
    }
    private static void tap(Game root,float x,float y) { root.pointerDown(0,x,y); root.pointerUp(0,x,y); }
    private static void tutorial(Parity p) {
        Game root=new Game(); ReplayInput.tap(root,Layout.LEARN);
        for(int i=0;i<9000;i++) {
            Game g=root.activeGame();
            if(g.tutorial.lesson==Tutorial.DONE) { Renderer.draw(root,p); return; }
            ReplayInput.tutorial(root);
            root.step();p.add(g.tow.cargo==null?0:g.tow.cargo.id);p.value(g.tow.length);p.value(g.tow.tension);
            if(g.cargo[0]!=null) { p.value(g.cargo[0].x);p.value(g.cargo[0].y);p.value(g.cargo[0].angle); }
            p.add(g.tutorial.lesson);p.add(g.tutorial.channelGate);p.value(g.layout.camera.worldRight); p.add(g.control); p.add(root.pollHaptic());
            p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle); p.value(g.tug.heave); p.add(g.water.emitted); p.add(g.ropeDragging?1:0); p.add(g.ropePhase); p.add(g.ropeTicks);
            if(i%30==0) Renderer.draw(root,p);
        }
        throw new IllegalStateException("Tutorial replay did not finish");
    }
    private static void responsive(Parity p) {
        Game g=new Game(); g.configureCanvas(1333.3334f,600); Renderer.draw(g,p);
        ReplayInput.tap(g,Layout.START); Renderer.draw(g,p);
        ReplayInput.down(g,g.tug.x,g.tug.y); ReplayInput.move(g,g.downX,g.downY-100);
        for(int i=0;i<45;i++) g.step();
        p.value(g.tug.x); p.value(g.tug.y); Renderer.draw(g,p);
        g.background(); Renderer.draw(g,p);
        g.configureCanvas(960,720); Renderer.draw(g,p);
        g.pause(); g.startTutorial(); Renderer.draw(g,p);
    }
    private static void progression(Parity p) {
        Game g=new Game(); g.restoreProgress("BB1|0|3,7000|0,0|0,0");
        g.mode=Game.CHALLENGES; Renderer.draw(g,p);
        g.selectChallenge(1); g.reset();
        g.ticks=g.shiftTicks()-1; g.delivered=g.quota(); g.hull=95; g.lost=0; g.score=7500;
        g.step(); p.add(g.earnedStars); p.add(g.career.rank()); p.add(g.progressVersion());
        p.add(filenameHash(g.saveProgress())); Renderer.draw(g,p);
        Game restored=new Game(); restored.restoreProgress(g.saveProgress());
        restored.configureCanvas(1333.3334f,600); restored.mode=Game.CHALLENGES;
        p.add(filenameHash(restored.saveProgress())); Renderer.draw(restored,p);
    }
    public static void diagnostic() {
        tracing=true;
        try { System.out.println("SIGNATURE "+signature()); } finally { tracing=false; }
    }
    public static void main(String[] args) {
        if(args.length>0 && args[0].equals("--trace")) diagnostic(); else System.out.println(signature());
    }
}
