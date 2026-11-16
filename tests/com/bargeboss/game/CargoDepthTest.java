package com.bargeboss.game;

/** Whole-load registration survives heading, projection, heave and the shared unload cycle. */
public final class CargoDepthTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static boolean near(float a,float b){return Math.abs(a-b)<.002f;}
    public static void run(){
        coamingClip();
        Game g=new Game();g.visualTicks=43;
        check(Game.UNLOAD_TICKS==180,"all cargo uses the existing three-second unload duration");
        for(boolean river:new boolean[]{false,true})for(int berth=0;berth<3;berth++)for(int heading=0;heading<64;heading++){
            Body b=new Body(1,berth,300,550,heading*(float)Math.PI/32);
            b.floatingInitialized=true;b.heave=1.5f;b.lineAttached=true;
            float previous=2;
            for(int tick:new int[]{0,10,70,90,130,179,180,181}){
                b.unloadingTicks=tick;
                Capture capture=new Capture(berth==0?SpriteAtlas.STEEL:berth==1?SpriteAtlas.CONTAINER:SpriteAtlas.BULK);
                Painter painter=river?new RiverPainter(capture):capture;
                VesselView.draw(g,painter,b,false,false);
                check(capture.hull==1,"unloading keeps one hull");
                check(capture.count==(tick<Game.UNLOAD_TICKS?1:0),"exactly one whole-load layer until unloading completes");
                if(capture.count>0){
                    check(near(capture.x,capture.hullX) && near(capture.y,capture.hullY),"cargo and hull retain one fixed waterline anchor including heave");
                    check(near(capture.w,capture.hullW) && near(capture.h,capture.hullH),"quantity changes never shrink or stretch cargo placement");
                    check(capture.first==capture.hullFirst && capture.next==capture.hullNext && near(capture.blend,capture.hullBlend),"cargo uses the same source-heading pair and blend as its hull");
                    check(capture.alpha>0 && capture.alpha<=1 && capture.alpha<previous,"load fades monotonically through unloading");
                    check(tick!=0 || near(capture.alpha,1),"full load starts opaque");
                    check(tick!=90 || near(capture.alpha,.5f),"halfway unloading retains half quantity");
                    check(capture.cargoSequence>capture.hullSequence,"registered coaming-cut cargo follows the base hull");
                    previous=capture.alpha;
                }
            }
        }
        System.out.println(checks+" whole-load cargo, fixed-anchor, heading and unload-quantity assertions passed");
    }
    private static void coamingClip(){
        Game g=new Game();g.reset();RiverCamera camera=new RiverCamera(100,120,2);
        for(int berth=0;berth<3;berth++)for(int heading=0;heading<32;heading++){
            Body b=new Body(1,berth,300,550,heading*(float)Math.PI/16);
            int atlas=berth==0?SpriteAtlas.STEEL:berth==1?SpriteAtlas.CONTAINER:SpriteAtlas.BULK;
            float[] previous=null;
            for(int bob=0;bob<2;bob++){
                ClipCapture out=new ClipCapture(atlas);
                CargoView.draw(g,new RiverPainter(out,camera),b,b.x+RiverCamera.riseX(bob*3),b.y+RiverCamera.riseY(bob*3),100);
                check(out.pushes==1 && out.pops==1 && out.depth==0,"coaming clip scope balances at every heading");
                check(out.colors==1 && out.shades>0,"both color and surface shade are inside the same coaming cutoff");
                check(out.clip.length==24,"both near rim edges supply two exclusion triangles each");
                for(int i=0;i<out.clip.length;i++){
                    check(Float.isFinite(out.clip[i]),"coaming clip coordinates finite");
                    if(previous!=null)check(near(out.clip[i],previous[i]-(i%2==0?0:3*camera.scale)),"coaming cutoff heaves with its registered load");
                }
                previous=out.clip;
            }
            b.unloadingTicks=180;ClipCapture empty=new ClipCapture(atlas);
            CargoView.draw(g,new RiverPainter(empty,camera),b,b.x,b.y,100);
            check(empty.pushes==0 && empty.colors==0 && empty.shades==0,"empty load does not open a clip or shade invisible cargo");
        }
        Body b=new Body(1,0,300,550,0);ClipCapture failure=new ClipCapture(SpriteAtlas.STEEL);failure.fail=true;
        try{CargoView.draw(g,new RiverPainter(failure,camera),b,b.x,b.y,100);throw new AssertionError("expected painter failure");}catch(PaintFailure expected){}
        check(failure.depth==0 && failure.pushes==1 && failure.pops==1,"coaming clip restores on painter exception");
    }
    private static final class PaintFailure extends RuntimeException{}
    private static final class ClipCapture extends Capture{
        int depth,pushes,pops,colors,shades;float[] clip;boolean fail;
        ClipCapture(int atlas){super(atlas);}
        public void pushClipOutside(float[] q){check(depth==0,"isolated cargo opens one shared coaming clip");depth++;pushes++;clip=q.clone();}
        public void popClip(){check(depth==1,"clip pop matches prior push");depth--;pops++;}
        public void spriteBlend(int a,int f,int n,float cx,float cy,float width,float height,float r,float mix,float opacity){
            if(a==cargoAtlas || a==SurfaceLightArt.forSource(cargoAtlas)){
                check(depth==1,"cargo source and shade never bypass coaming cutoff");
                if(a==cargoAtlas){colors++;if(fail)throw new PaintFailure();}else shades++;
            }
            super.spriteBlend(a,f,n,cx,cy,width,height,r,mix,opacity);
        }
    }
    private static class Capture implements Painter{
        final int cargoAtlas;int count,hull,sequence,hullSequence,cargoSequence,first,next,hullFirst,hullNext;
        float x,y,w,h,alpha,blend,hullX,hullY,hullW,hullH,hullBlend;
        Capture(int atlas){cargoAtlas=atlas;}
        public void sprite(int a,int f,float cx,float cy,float width,float height,float r,float opacity){spriteBlend(a,f,f,cx,cy,width,height,r,0,opacity);}
        public void spriteBlend(int a,int f,int n,float cx,float cy,float width,float height,float r,float mix,float opacity){
            sequence++;
            if(a==SpriteAtlas.BARGE){hull++;hullX=cx;hullY=cy;hullW=width;hullH=height;hullFirst=f;hullNext=n;hullBlend=mix;hullSequence=sequence;}
            if(a==cargoAtlas){count++;x=cx;y=cy;w=width;h=height;first=f;next=n;blend=mix;alpha=opacity;cargoSequence=sequence;}
        }
        public void rect(float x,float y,float w,float h,int c){}
        public void circle(float x,float y,float r,int c){}
        public void poly(float[] xy,int c){}
        public void line(float x,float y,float xx,float yy,float w,int c){}
    }
    public static void main(String[]args){run();}
}
