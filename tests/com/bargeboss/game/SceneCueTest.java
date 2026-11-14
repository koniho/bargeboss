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
                Order painter=new Order(g);TrainingChannelView.draw(g,painter);
                check(painter.factories>0 && painter.caps>0 && painter.vessels>0,"regression scene includes shoreline caps and factory sprites");
            }
        }
        for(int bank=0;bank<2;bank++) {
            Game root=new Game();root.startTutorial();Game scene=root.activeGame();
            scene.tug.x=bank==0?-1610:TrainingChannel.BASIN_LEFT+16;
            scene.tug.y=bank==0?TrainingChannel.BASIN_BOTTOM-16:600;
            scene.tug.angle=bank==0?0:(float)Math.PI/2;
            Body barge=new Body(1,0,scene.tug.x-(bank==0?85:-3),scene.tug.y-(bank==0?3:85),scene.tug.angle);
            scene.cargo[0]=barge;
            scene.layout=Layout.focused(1333,600,scene.tug.x,scene.tug.y,1.7f,666,360);
            Order painter=new Order(scene);TrainingChannelView.draw(scene,painter);
            check(painter.tugs>0 && painter.barges>0 && painter.caps>0,"both vessel types remain above shoreline on each lower bank");
        }
        for(boolean practice:new boolean[]{false,true})for(boolean release:new boolean[]{false,true}) {
            Game root=new Game();root.reset();if(practice)root.startTutorial();Game scene=root.activeGame();
            Body barge=new Body(1,0,scene.tug.x+70,scene.tug.y,0);scene.cargo[0]=barge;
            scene.tow.attach(scene,barge);scene.tug.angle=(float)Math.PI; // Rope crosses the hull in projection.
            if(release)scene.tow.release(scene);
            RopeOrder painter=new RopeOrder(scene.layout.camera.scale);Renderer.draw(root,painter);
            check(painter.ropes==24 && painter.vessels>0,"connected and recoiling tow rope draw once beneath hulls in practice and shifts");
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
    private static final class RopeOrder extends Strokes {
        final float scale;int vessels,ropes;
        RopeOrder(float scale){this.scale=scale;}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha){if(a==SpriteAtlas.TUG || a==SpriteAtlas.BARGE)vessels++;}
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float angle,float blend,float alpha){sprite(a,f,x,y,w,h,angle,alpha);}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){}
        public void line(float x,float y,float xx,float yy,float width,int color) {
            if((color&0xffffff)==0xffe3a1 && Math.abs(width-3*scale)<.001f) {
                ropes++;check(vessels==0,"physical towline must not be painted over any hull");
            }
        }
    }
    private static final class Order extends Strokes {
        int factories,vessels,caps,tugs,barges;
        Order(Game g){}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha) {
            if(a==SpriteAtlas.FACTORY)factories++;
            if(a==SpriteAtlas.TUG){vessels++;tugs++;}
            if(a==SpriteAtlas.BARGE){vessels++;barges++;}
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float angle,float blend,float alpha) {
            sprite(a,f,x,y,w,h,angle,alpha);
        }
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha) {
            if((layer.key()&0xe0000000)==0x20000000) {
                check(vessels==0 && factories==0,"cached shorelines must precede all boats and factories");
                layer.paint(this);
            }
        }
        public void poly(float[] xy,int color) {
            if(color!=0xff43525a || xy.length!=8)return;
            caps++;
            check(vessels==0 && factories==0,"no shoreline cap may overpaint hulls, roofs or chimneys");
        }
    }
}
