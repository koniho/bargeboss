package com.bargeboss.game;

/** A registered cargo layer stays inside the same hull coordinate system while unloading. */
public final class BargeAtlasTest {
    private static int checks;
    private static void check(boolean ok,String why) {checks++;if(!ok) throw new AssertionError(why);}
    private static boolean near(float a,float b) {return Math.abs(a-b)<.0001f;}
    public static void run() {
        for(int atlas:new int[]{SpriteAtlas.BARGE,SpriteAtlas.BULK}) {
            boolean[] seen=new boolean[32];
            for(int frame=0;frame<32;frame++) {
                float l=SpriteAtlas.sourceLeft(atlas,frame),r=SpriteAtlas.sourceRight(atlas,frame);
                float t=SpriteAtlas.sourceTop(atlas,frame),b=SpriteAtlas.sourceBottom(atlas,frame);
                check(l>=0 && t>=0 && r<=1 && b<=1 && near(r-l,.125f) && near(b-t,.25f),"barge/coal crops address a single square cell");
                int index=Math.round(t*4)*8+Math.round(l*8);
                check(!seen[index],"every source direction is distinct");seen[index]=true;
                check(near(SpriteAtlas.widthScale(atlas,frame),SpriteAtlas.heightScale(atlas,frame))
                    && near(SpriteAtlas.widthScale(atlas,frame),SpriteAtlas.widthScale(atlas,0)),"registration never stretches individual headings");
            }
        }
        Game g=new Game();g.reset();g.visualTicks=39;
        Body b=new Body(1,2,300,600,0);b.lineAttached=true;
        for(int heading=0;heading<32;heading++) for(int tick:new int[]{0,1,60,120,179,180}) {
            b.angle=heading*(float)Math.PI/16;b.unloadingTicks=tick;
            Capture out=new Capture();VesselView.draw(g,new RiverPainter(out),b,false,false);
            check(out.hull==1 && out.coal==(tick<CargoTransfer.PICK?1:0),"coal transfers off hull at pickup while hull remains");
            if(out.coal>0) {
                check(near(out.hx,out.cx) && near(out.hy,out.cy) && near(out.hw,out.cw) && near(out.hh,out.ch),"coaming mask retains the hull origin and square extent through every scoop and heading");
                check(out.alpha>0 && out.alpha<=1,"remaining coal quantity has valid opacity");
            }
        }
        System.out.println(checks+" barge/coal registration and unloading alignment assertions passed");
    }
    private static final class Capture implements Painter {
        int hull,coal;float hx,hy,hw,hh,cx,cy,cw,ch,alpha;
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float a) {
            if(atlas==SpriteAtlas.BARGE) {hull++;hx=x;hy=y;hw=w;hh=h;}
            if(atlas==SpriteAtlas.BULK) {coal++;cx=x;cy=y;cw=w;ch=h;alpha=a;}
        }
        public void spriteBlend(int atlas,int first,int second,float x,float y,float w,float h,float rotation,float blend,float a) {sprite(atlas,first,x,y,w,h,rotation,a);}
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
        public void poly(float[] xy,int c) {}
    }
}
