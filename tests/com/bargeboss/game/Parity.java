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
    public void pushClipOutside(float[] triangles){mark("clipOutside");add(8);add(triangles.length);for(float v:triangles)value(v);}
    public void popClip(){mark("clipRestore");add(9);}
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
    public void spriteAffine(int atlas,int frame,float x,float y,float ax,float ay,float bx,float by,float alpha) {
        mark("spriteAffine");add(7);add(atlas);add(frame);value(x);value(y);value(ax);value(ay);value(bx);value(by);value(alpha);
        add(filenameHash(SpriteAtlas.file(atlas,frame)));value(SpriteAtlas.pivotX(atlas,frame));value(SpriteAtlas.pivotY(atlas,frame));
        value(SpriteAtlas.sourceLeft(atlas,frame));value(SpriteAtlas.sourceTop(atlas,frame));value(SpriteAtlas.sourceRight(atlas,frame));value(SpriteAtlas.sourceBottom(atlas,frame));
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
        g.resetPracticeYard();
        for(int i=0;i<Game.SHIFT_TICKS;i++) {
            ReplayInput.work(g);
            if(i==300) { g.background(); Renderer.draw(g,p); g.pause(); }
            if(i==1200) { g.cancel(); ReplayInput.throttle(g,0,-90); }
            if(i==1260) g.cancel();
            g.step();
            if(i%60==0) {
                p.add(g.ticks); p.add(g.score); p.add(g.delivered); p.add(g.hull); p.add(g.ropePhase); p.add(g.ropeTicks); p.add(g.cameraZoomHoldTicks);
                p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle); p.value(g.tug.heave); p.value(g.tug.engineLoad); p.add(g.water.emitted);
                for(Body b:g.cargo) if(b!=null) { p.add(b.id); p.add(b.ability);p.add(b.abilityTicks);p.add(b.abilityCooldown);p.value(b.abilityAim); p.add(b.active?1:0); p.value(b.x); p.value(b.y); p.value(b.angle); p.add(b.lineAttached?1:0); p.add(b.mooringTicks); p.add(b.unloadingTicks); p.add(b.lineSnapTicks); p.add(b.tieBurstTicks); p.value(b.heave); p.add(b.wakeKickTicks); }
            }
            if(i%600==0) Renderer.draw(g,p);
        }
        Renderer.draw(g,p);
        for(int i=0;i<60;i++) { g.advance(1.0/60); if(i%20==0) Renderer.draw(g,p); }
        radio(p);
        tutorial(p);
        responsive(p);
        progression(p);
        feeders(p);
        goalLevels(p);
        rescue(p);
        joystick(p);
        slipTether(p);
        return Integer.toHexString(p.hash)+":"+g.delivered+":"+g.score;
    }
    private static void slipTether(Parity p){
        Game root=new Game();ReplayInput.startPractice(root);Game g=root.activeGame();
        g.tutorial.lesson=Tutorial.MAP;g.tutorial.setup(g);g.tug.x=-1910;g.tug.y=300;g.tug.angle=0;g.fitTutorialCamera();
        ReplayInput.lasso(g,g.tug);
        for(int i=0;i<240;i++){
            if(i==45)ReplayInput.throttle(g,90,0);
            if(i==180){g.cancelInputs();ReplayInput.down(g,g.bollardX(g.tug),g.bollardY(g.tug));ReplayInput.up(g,g.bollardX(g.tug),g.bollardY(g.tug));}
            g.step();p.value(g.tug.x);p.value(g.tug.y);p.value(g.tug.angle);p.value(g.tug.vx);p.value(g.tug.vy);p.value(g.tug.spin);
            p.add(g.tug.lineAttached?1:0);p.value(g.slipMooring.length);p.add(g.ropePhase);
            if(i%30==0)Renderer.draw(g,p);
        }
    }
    /** Native parity includes arrival motion, livery, modal reply and audio event ordering. */
    private static void radio(Parity p){
        Game root=new Game();root.configureCanvas(1333,600);root.startTutorial();Game g=root.activeGame();
        for(int tick=0;tick<460;tick++){
            if(tick==280){Layout.Box box=g.dialogue.choiceBox(g,1);root.pointerDown(77,box.centerX(),box.centerY());root.pointerUp(77,box.centerX(),box.centerY());}
            root.step();p.value(g.tug.x);p.value(g.teacherBoat().x);p.value(g.teacherBoat().vx);
            p.add(RadioAudio.sequence(root));p.add(RadioAudio.cue(root));p.add(g.dialogue.result());
            if(tick==100 || tick==270 || tick==281 || tick==400 || tick==459)Renderer.draw(root,p);
        }
        for(int color:new int[]{0xffa95d32,0xffb9b5ad,0x608e4228,0x00112233})p.add(SpriteGrade.argb(SpriteAtlas.TEACHER_TUG,color));
    }
    private static void joystick(Parity p) {
        Game g=new Game();g.configureCanvas(1333.3334f,600);ReplayInput.startPractice(g);
        for(int side:new int[]{GameSettings.LEFT,GameSettings.RIGHT}) {
            g.activeGame().openSettings();
            ReplayInput.tap(g,side==GameSettings.LEFT?Layout.JOYSTICK_LEFT:Layout.JOYSTICK_RIGHT);
            Renderer.draw(g,p);p.add(filenameHash(g.saveProgress()));ReplayInput.tap(g,Layout.SETTINGS_BACK);
            Game active=g.activeGame();float x=Joystick.centerX(active),y=Joystick.centerY(active);
            g.pointerDown(4,x,y);g.pointerMove(4,x+28,y-28);
            for(int tick=0;tick<30;tick++){g.step();p.value(active.tug.x);p.value(active.tug.y);p.value(active.tug.angle);}
            Renderer.draw(g,p);g.pointerUp(4,x+28,y-28);g.step();Renderer.draw(g,p);
        }
    }
    /** Geographic render/state samples; long public voyages have separate tests. */
    private static void goalLevels(Parity p) {
        for(int id:new int[]{0,Challenges.MONTROSE,Challenges.INDIANA}){
            Game g=new Game();g.challengeId=id;g.reset();
            for(int i=0;i<120;i++){g.step();p.value(g.tug.x);p.value(g.tug.y);p.add(g.hull);}
            Renderer.draw(g,p);p.add(g.goalProgress());
            if(id==Challenges.MONTROSE){
                Body disabled=g.cargo[0];for(int tick=0;tick<2400 && !disabled.deliveryRecorded;tick++){ReplayInput.work(g);g.step();if(tick%90==0){p.value(disabled.x);p.value(disabled.y);p.add(g.haulout.warping?1:0);Renderer.draw(g,p);}}
                p.add(g.challengeId);p.add(g.career.bestStars(Challenges.HAULOUT));Renderer.draw(g,p);
            }
            if(id==0){
                for(int site=0;site<CalumetRiver.JOB_SITES.length;site++){
                    float[] at=CalumetRiver.JOB_SITES[site];g.layout=Layout.focused(960,600,at[0],at[1],1.5f,480,300);Renderer.draw(g,p);
                }
                for(Body body:g.cargo)if(body!=null)body.deliveryRecorded=true;
                g.voyage.step(g);p.add(g.voyage.job);p.add(g.career.totalStars());Renderer.draw(g,p);
            }
        }
    }
    private static void rescue(Parity p){
        Game g=ReplayInput.montroseJob(Challenges.RAFT_RESCUE);Renderer.draw(g,p);
        float x=(g.cargo[0].x+g.cargo[1].x)*.5f,y=g.cargo[0].y;
        ReplayInput.down(g,x,y);ReplayInput.up(g,x,y);for(int i=0;i<30;i++)g.step();p.add(g.rescue.rafted?1:0);Renderer.draw(g,p);
        g=ReplayInput.montroseJob(Challenges.SWIMMER_RESCUE);
        for(int i=0;i<4000 && g.mode==Game.PLAY;i++){ReplayInput.work(g);g.step();if(i%60==0){p.add(g.delivered);p.add(g.rescue.flying);p.value(g.tug.x);p.value(g.tug.y);p.value(g.layout.camera.zoom);Renderer.draw(g,p);}}
        if(!g.goalComplete())throw new IllegalStateException("Swimmer replay did not finish");
        p.add(g.score);p.add(g.earnedStars);Renderer.draw(g,p);
    }
    private static void feeders(Parity p) {
        Game g=new Game(); g.resetPracticeYard();
        for(int i=0;i<350;i++) {
            g.step();
            if(i%30==0) { Renderer.draw(g,p); for(Feeder f:g.feeders) {
                p.add(f.phase); p.add(f.drops); p.add(f.shoutTicks); p.add(f.blocked?1:0); p.value(f.boat.y);
            } }
        }
        g.resetPracticeYard(); g.tug.x=260; g.tug.y=760;
        for(int i=0;i<410;i++) g.step();
        Renderer.draw(g,p); p.add(g.feeders[0].blockedTicks);
    }
    private static void tap(Game root,float x,float y) { root.pointerDown(0,x,y); root.pointerUp(0,x,y); }
    private static void tutorial(Parity p) {
        Game root=new Game(); ReplayInput.tap(root,Layout.LEARN);
        for(int i=0;i<16000;i++) {
            Game g=root.activeGame();
            if(g.tutorial.lesson==Tutorial.DONE) { Renderer.draw(root,p); return; }
            ReplayInput.tutorial(root);
            root.step();
            for(int slot=0;slot<Towline.CAPACITY;slot++) {
                Towline link=g.tow.link(slot);p.add(link==null || link.cargo==null?0:link.cargo.id);
                if(link!=null && link.cargo!=null){p.add(link.leader.id);p.value(link.length);p.value(link.tension);}
            }
            if(g.cargo[0]!=null) { p.value(g.cargo[0].x);p.value(g.cargo[0].y);p.value(g.cargo[0].angle); }
            p.add(g.tutorial.lesson);p.add(g.tutorial.helm.phase);p.add(g.tutorial.helm.pace);p.add(g.tutorial.navigation.slipPhase);p.add(g.tutorial.navigation.retry?1:0);p.add(g.tutorial.navigation.mapPhase);p.add(SoundEffects.sequence(g));p.add(SoundEffects.cue(g));p.add(g.tutorial.channelGate);p.value(g.layout.camera.worldRight); p.add(g.control); p.add(root.pollHaptic());
            p.add(g.tutorial.firstTowParked?1:0);p.add(g.tutorial.towBridges.phase);p.add(g.tutorial.towBridges.authorized[1]?1:0);p.add(g.tutorial.towBridges.authorized[2]?1:0);p.add(g.passages.position[1]);p.add(g.passages.position[2]);
            p.add(g.tutorial.canal.phase);p.add(g.tutorial.canal.authorized?1:0);p.add(g.passages.position[0]);
            p.add(g.tutorial.canal.escort.gate);p.add(g.tutorial.canal.escort.waiting?1:0);p.add(g.radio.transmitter());
            p.value(g.tug.x); p.value(g.tug.y); p.value(g.tug.angle); p.value(g.tug.heave); p.add(g.water.emitted); p.add(g.ropeDragging?1:0); p.add(g.ropePhase); p.add(g.ropeTicks);
            if(i%30==0) Renderer.draw(root,p);
        }
        throw new IllegalStateException("Tutorial replay did not finish");
    }
    private static void responsive(Parity p) {
        Game g=new Game(); g.configureCanvas(1333.3334f,600); Renderer.draw(g,p);
        ReplayInput.tap(g,Layout.START); Renderer.draw(g,p);
        ReplayInput.throttle(g,0,-100);
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
        g.ticks=120; g.delivered=g.quota(); g.hull=95; g.lost=0; g.score=7500;
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
