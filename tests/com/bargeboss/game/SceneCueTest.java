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
            check(painter.tugs>0 && painter.barges>0 && painter.foregroundCaps>0,"solid lower banks occlude vessels on both foreground basin edges");
        }
        for(int family=0;family<4;family++)for(boolean practice:new boolean[]{false,true})for(boolean release:new boolean[]{false,true})for(int heading=0;heading<32;heading++) {
            Game root=new Game();root.reset();if(practice)root.startTutorial();Game scene=root.activeGame();
            for(int i=0;i<scene.cargo.length;i++)scene.cargo[i]=null;
            for(Feeder f:scene.feeders){f.phase=Feeder.IDLE;f.load=null;}
            scene.tug.x=practice?-1550:300;scene.tug.y=600;scene.tug.angle=heading*(float)Math.PI/16;
            scene.tug.floatingInitialized=true;scene.tug.heave=0;
            Body barge=new Body(1,family==3?0:family,scene.tug.x+70,scene.tug.y+20,-scene.tug.angle,family==3);barge.floatingInitialized=true;scene.cargo[0]=barge;
            scene.layout=Layout.focused(960,600,scene.tug.x+35,scene.tug.y,1.5f,480,350);
            scene.tow.attach(scene,barge);
            if(release)scene.tow.release(scene);
            RopeOrder painter=new RopeOrder(scene);Renderer.draw(root,painter);
            check(painter.ropes<=96 && painter.vessels>0,"visible rope pieces remain bounded and merge with vessels: family="+family+" heading="+heading+" release="+release+" practice="+practice+" pieces="+painter.ropes);
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
        final float scale;final RiverCamera camera;final TowView.Segments segments;
        int vessels,ropes;float lastBody=-Float.MAX_VALUE,lastSpan=-Float.MAX_VALUE;
        RopeOrder(Game g){camera=g.layout.camera;scale=camera.scale;segments=new TowView.Segments(g);}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha){
            if(a==SpriteAtlas.TUG || a==SpriteAtlas.BARGE || a==SpriteAtlas.YACHT){check(lastSpan<=y+.01f,"far rope spans precede a hull, near spans follow it");lastBody=y;vessels++;}
        }
        public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float angle,float blend,float alpha){sprite(a,f,x,y,w,h,angle,alpha);}
        public void layer(StaticLayer layer,float x,float y,float scale,float alpha){}
        public void ropeSegment(float x,float y,float xx,float yy,float width,float from,float to,int color) {
            if(Math.abs(width-2.8f*scale)<.001f) {
                float depth=Float.NaN;
                for(int slot=0;slot<segments.count;slot++) {
                    int point=segments.points[segments.order[slot]];
                    if(from>=segments.strand[point]-.001f && to<=segments.strand[point+1]+.001f){depth=segments.depth(camera,slot);break;}
                }
                ropes++;check(Float.isFinite(depth),"visible rope piece belongs to a shared span");
                check(depth+.01f>=lastBody && depth>=lastSpan,"rope sections follow their attachment-dependent ground depth");lastSpan=depth;
            }
        }

    }
    private static final class Order extends Strokes {
        final Game g;float factoryDepth=-Float.MAX_VALUE;int factories,vessels,caps,tugs,barges,foregroundCaps;
        Order(Game g){this.g=g;}
        public void sprite(int a,int f,float x,float y,float w,float h,float angle,float alpha) {
            if(a==SpriteAtlas.FACTORY){factories++;factoryDepth=y+GroundArt.LAND_RISE*g.layout.camera.scale;}
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
            check((color>>>24)==255,"shoreline geometry never fades for a nearby boat");
            if(vessels>0)foregroundCaps++;
            float depth=(xy[1]+xy[3]+xy[5]+xy[7])*.25f+GroundArt.LAND_RISE*g.layout.camera.scale;
            check(depth+3*g.layout.camera.scale>=factoryDepth,"lower shoreline cannot overpaint nearer factory roofs or chimneys");
        }
    }
}
