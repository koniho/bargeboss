package com.bargeboss.game;

/** Deterministic submission budgets guard dense-view performance without flaky wall-clock tests. */
public final class RenderBudgetTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static Game fixture(int width,int scene) {
        Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();
        if(scene==0) {
            g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);
            g.layout=Layout.focused(width,600,-640,390,1,width*.5f,372);
            return g;
        }
        int channelTicks=0;
        for(int tick=0;tick<16000;tick++) {
            if(g.tutorial.lesson==Tutorial.CHANNEL)channelTicks++;
            if(scene==1 && channelTicks==700)return g;
            if(scene==2 && g.tutorial.lesson==Tutorial.STEER)return g;
            ReplayInput.tutorial(root);root.step();
        }
        throw new AssertionError("Render budget scene was not reached");
    }
    public static void run() {
        int[] widths={960,1333,1920},callBudget={11800,13900,16200},circleBudget={1700,2500,3700};
        int[] minimumEntryFactories={40,55,85},minimumCloseFactories={10,15,30};
        int maximumCalls=0;
        for(int viewport=0;viewport<widths.length;viewport++)for(int scene=0;scene<3;scene++) {
            Game g=fixture(widths[viewport],scene);Counts p=new Counts(g.layout.width,g.layout.height);
            int ticks=g.ticks,visualTicks=g.visualTicks;float x=g.tug.x,y=g.tug.y;
            Renderer.draw(g,p);maximumCalls=Math.max(maximumCalls,p.calls);
            check(p.calls<=callBudget[viewport],"dense scene native draw-call budget: width="+widths[viewport]+" scene="+scene+" calls="+p.calls);
            check(p.circles<=circleBudget[viewport],"wide scenery plume/glow submission budget");
            check(p.offscreenCircles<=32,"only conservative antialias-edge circles may remain outside the viewport");
            check(p.terrainLayers>0 && p.terrainLayers<=48,"static terrain submits a bounded set of visible chunks");
            if(scene==0)check(p.factories>=minimumEntryFactories[viewport],"performance gain retains the dense factory district");
            if(scene==2)check(p.factories>=minimumCloseFactories[viewport],"close view retains surrounding industrial structures");
            check(p.terrainLayers>0 && p.circles+p.smokePuffs>100,"terrain and organic effects remain rendered: circles="+p.circles+" smoke="+p.smokePuffs);
            check(g.ticks==ticks && g.visualTicks==visualTicks && g.tug.x==x && g.tug.y==y,"render optimization never advances or changes simulation");
        }
        System.out.println(checks+" dense-scene rendering budget assertions passed; maximum submissions "+maximumCalls);
    }
    public static void main(String[] args){run();}
    private static final class Counts implements Painter {
        final float width,height;int calls,circles,smokePuffs,offscreenCircles,factories,groundSprites,terrainLayers;
        Counts(float w,float h){width=w;height=h;}
        public void material(float x,float y,float ax,float ay,float bx,float by,int kind,float detail,float amount,int color){calls++;}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){calls++;if((layer.key()&0xe0000000)==0x20000000)terrainLayers++;}
        // Native smoke is now one cached/shader patch, not several submitted circles.
        public void smokePuff(float x,float y,float radius,float phase,int color){calls++;smokePuffs++;}
        public void rect(float x,float y,float w,float h,int color){calls++;}
        public void line(float x,float y,float xx,float yy,float w,int color){calls++;}
        public void poly(float[] xy,int color){calls++;}
        public void circle(float x,float y,float r,int color){calls++;circles++;if(x+r<=0 || y+r<=0 || x-r>=width || y-r>=height)offscreenCircles++;}
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float r,float alpha){calls++;if(atlas==SpriteAtlas.FACTORY)factories++;if(atlas==SpriteAtlas.GROUND)groundSprites++;}
        public void spriteBlend(int atlas,int a,int b,float x,float y,float w,float h,float r,float blend,float alpha){calls++;}
    }
}
