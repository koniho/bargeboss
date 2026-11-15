package com.bargeboss.game;

/** Cached UI lettering remains present, while world-space labels submit no text. */
public final class OverlayTextTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final class Count implements Painter {
        int text;StaticLayer last;float alpha;
        public void layer(StaticLayer l,float x,float y,float s,float a){if((l.key()&0xf0000000)==0x60000000){text++;last=l;alpha=a;}}
        public void poly(float[] q,int c){}public void rect(float x,float y,float w,float h,int c){}public void circle(float x,float y,float r,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run(){
        Count p=new Count();Lettering.draw(p,"READY",10,20,14,0xffabcdef);StaticLayer first=p.last;
        Lettering.draw(p,"READY",40,70,14,0x44abcdef);
        check(p.last==first && Math.abs(p.alpha-68/255f)<.00001f,"pulse and location reuse immutable text raster");
        Lettering.draw(p,"READY",40,70,14,0xff112233);check(p.last!=first,"color change gets distinct glyph raster");
        Lettering.draw(p,"WAIT",40,70,14,0xffabcdef);check(p.last!=first,"content change gets distinct glyph raster");
        for(int i=0;i<160;i++)Lettering.draw(p,"VALUE "+i,0,0,14,0xffabcdef);
        Lettering.draw(p,"READY",0,0,14,0xffabcdef);check(p.last.key()!=first.key(),"evicted text never reuses an older native cache key");
        Game g=new Game();ReplayInput.controlFixture(g);p=new Count();Painter world=new RiverPainter(p,g.layout.camera);
        SoftCue.caption(world,"NO WORLD LABEL",300,400,12,0xffffffff);RiverPainter.center(world,"NO LABEL",300,400,12,0xffffffff);
        HarborView.draw(g,world);Renderer.body(g,world,g.tug);for(Body b:g.cargo)if(b!=null)Renderer.body(g,world,b);
        DockView.draw(g,p);MooringView.draw(g,world);check(p.text==0,"vessels, berths, dock machinery and mooring cues contain no world text");
        ScreenView.hud(g,p);check(p.text>0,"HUD still has readable cached text");
        g.startTutorial();g=g.activeGame();p=new Count();TutorialView.draw(g,p);check(p.text>0,"tutorial instruction overlay remains readable");
        System.out.println(checks+" cached UI and world-text removal assertions passed");
    }
    public static void main(String[] args){run();}
}
