package com.bargeboss.game;
/** Running out of hull plays the death sequence (tug sinks, GAME OVER) before the results screen. */
public final class TugDeathTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(){
        Game g=new Game();ReplayInput.veteran(g);g.challengeId=Challenges.BOAT_TAG;g.reset();g.configureCanvas(1333,750);g.mode=Game.PLAY;g.dialogue.close(g);
        g.step();g.hull=0;g.step();
        check(g.dying() && g.mode==Game.PLAY,"an empty hull starts the death sequence, not the results screen");
        for(int t=0;t<TugDeath.SUNK-60;t++)g.step();
        check(g.tug.sinkTicks>0 && g.mode==Game.PLAY,"the tug sinks while the explosion plays");
        g.render(new Painter(){public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}public void poly(float[] q,int c){}
            public void line(float x,float y,float xx,float yy,float w,int c){}public void sprite(int a,int f,float x,float y,float w,float h,float r,float al){}
            public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float m,float al){}});
        g.pointerDown(0,600,400);g.pointerUp(0,600,400);check(g.dying() && g.mode==Game.PLAY,"taps during the explosion are ignored");
        while(g.deathTicks<TugDeath.SKIP)g.step();
        g.pointerDown(0,600,400);g.pointerUp(0,600,400);g.step();
        check(g.mode==Game.FINISH && g.goalFailed(),"a tap on GAME OVER continues to the results screen");
        g.reset();check(!g.dying() && g.tug.sinkTicks==0 && g.hull==100,"a retry brings back a fresh tug");
        System.out.println("tug death: explosion, sink, GAME OVER, then results");
    }
    public static void main(String[] a){run();}
}
