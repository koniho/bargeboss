package com.bargeboss.game;

/** Regression for shoreline overpainting roofs and opaque, zoom-thick guidance. */
public final class SceneCueTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void run() {
        for(int width:new int[]{960,1333,1920}) {
            Game root=new Game();root.configureCanvas(width,600);root.startTutorial();Game g=root.activeGame();
            for(int lesson:new int[]{Tutorial.MOVE,Tutorial.CHANNEL,Tutorial.HOLD}) {
                g.tutorial.lesson=lesson;g.tutorial.setup(g);
                float last=-Float.MAX_VALUE;
                for(int i=0;i<TrainingGroundView.frontCount();i++) {
                    float depth=TrainingGroundView.frontDepth(g,i);check(depth>=last,"shore sections are in stable camera depth order");last=depth;
                }
                Order painter=new Order(g);TrainingChannelView.draw(g,painter);
                check(painter.factories>0 && painter.caps>0,"regression scene includes shoreline caps and factory sprites");
            }
        }
        for(float zoom:new float[]{.7f,1,2,4}) {
            Strokes strokes=new Strokes();Painter p=new RiverPainter(strokes,new RiverCamera(0,0,zoom));
            ControlView.ring(p,300,500,40,1,VisualStyle.GOLD,5);
            ControlView.arrow(p,300,500,360,560,VisualStyle.GOLD);
            check(strokes.maxAlpha<=135,"even bright cues remain translucent");
            check(strokes.maxWidth<=6.5f,"zoom cannot inflate guidance into thick opaque strokes");
            check(strokes.polys==0 && strokes.circles==0,"arrows are open and rings leave their interiors clear");
        }
        Game g=new Game();g.visualTicks=0;int a=SoftCue.animated(g,VisualStyle.GOLD);g.visualTicks=35;
        check(a!=SoftCue.animated(g,VisualStyle.GOLD),"guidance gently animates with the shared visual clock");
        System.out.println(checks+" shoreline depth and soft guidance assertions passed");
    }
    private static class Strokes implements Painter {
        int maxAlpha,polys,circles;float maxWidth;
        public void line(float x,float y,float xx,float yy,float width,int color){maxAlpha=Math.max(maxAlpha,color>>>24);maxWidth=Math.max(maxWidth,width);}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha){}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float angle,float blend,float alpha){}
        public void rect(float x,float y,float w,float h,int color){}
        public void poly(float[] xy,int color){polys++;}
        public void circle(float x,float y,float r,int color){circles++;}
    }
    private static final class Order extends Strokes {
        final Game g;float factoryDepth=-Float.MAX_VALUE;int factories,caps;
        Order(Game g){this.g=g;}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha) {
            if(a==SpriteAtlas.FACTORY){factories++;factoryDepth=y+GroundArt.LAND_RISE*g.layout.camera.scale;}
        }
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){}
        public void poly(float[] xy,int color) {
            if(color!=0xff43525a || xy.length!=8)return;
            caps++;
            float depth=(xy[1]+xy[3]+xy[5]+xy[7])*.25f+GroundArt.LAND_RISE*g.layout.camera.scale;
            check(depth+3*g.layout.camera.scale>=factoryDepth,"a shoreline cap behind a factory must be painted before that factory");
        }
    }
}
