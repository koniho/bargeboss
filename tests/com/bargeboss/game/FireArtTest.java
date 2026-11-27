package com.bargeboss.game;
/** One fire look everywhere: the Navy Pier pavilions and the tug's death fires draw the tutorial
 * woodpile's flames (FireArt / FlameArt sprites, core glow, embers), and the pier gets the same
 * red wash and smoke column. */
public final class FireArtTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    /** Counts the calls that make up a fire. */
    private static final class Capture implements Painter {
        int flames,glows,lines,puffs;
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float m,float al){if(a==SpriteAtlas.FLAME)flames++;}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){if(kind==EffectMaterial.GLOW)glows++;}
        public void smokePuff(float x,float y,float radius,float phase,int color){puffs++;}
        public void line(float x,float y,float xx,float yy,float w,int c){lines++;}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float al){}
        public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void poly(float[] q,int c){}
    }
    /** A 1:1 camera with world (x,y) at the middle of a 1333x750 screen. */
    private static void centre(Game g,float x,float y){g.layout=Layout.renderTile(1333,750,new RiverCamera(666-RiverCamera.Q*320,375,1,y,x,1));}
    public static void run(){
        // Tutorial woodpile: the reference look.
        Game t=new Game();t.resetPracticeYard();t.configureCanvas(1333,750);t.tutorial=new Tutorial();t.tutorial.fireworks.ignite();
        t.tutorial.fireworks.launched=true;centre(t,FireworksLesson.x(0),FireworksLesson.y(0));
        Capture wood=new Capture();t.tutorial.fireworks.draw(t,wood,0);
        check(wood.flames==FlameArt.INSTANCES && wood.glows==1 && wood.lines==20,"woodpile: five blended flames, one core glow, twenty embers ("+wood.flames+","+wood.glows+","+wood.lines+")");

        // Navy Pier: the same fire on every pavilion.
        Game g=new Game();g.challengeId=Challenges.CHICAGO;g.reset();g.configureCanvas(1333,750);
        check(g.chicago.fire.ignited,"the pier is burning on arrival");
        for(int pile=0;pile<ChicagoFire.PILES;pile++){
            centre(g,ChicagoFire.x(pile),ChicagoFire.y(pile));
            Capture pier=new Capture();g.chicago.fire.draw(g,pier,pile);
            check(pier.flames==wood.flames && pier.glows==wood.glows && pier.lines==wood.lines,"pavilion "+pile+" burns exactly like the tutorial woodpile ("+pier.flames+","+pier.glows+","+pier.lines+")");
        }
        for(int i=0;i<FireArt.SMOKE_LIFE;i++)g.chicago.fire.step(g);
        Capture ground=new Capture();g.chicago.fire.ground(g,ground);
        check(ground.glows==1 && ground.puffs>0,"the pier gets the tutorial's red wash and soot columns");
        int emitted=g.smoke.emitted;for(int i=0;i<FlameArt.EMIT_PERIOD;i++){g.visualTicks++;g.chicago.fire.step(g);}
        check(g.smoke.emitted>emitted,"pavilion flames feed the shared smoke pool like the woodpiles");

        // Tug death, on any stage: the deck burns with the same flame sprites.
        Game d=new Game();ReplayInput.veteran(d);d.challengeId=Challenges.BOAT_TAG;d.reset();d.configureCanvas(1333,750);d.mode=Game.PLAY;d.dialogue.close(d);
        d.step();d.hull=0;d.step();while(d.deathTicks<TugDeath.BLAST+60)d.step();
        Capture death=new Capture();TugDeath.effects(d,death);
        check(death.flames>=3*FlameArt.INSTANCES,"the burning tug uses the shared flame sprites ("+death.flames+")");
        System.out.println("Shared fire: woodpile, Navy Pier and tug death draw one flame look");
    }
    public static void main(String[] args){run();}
}
