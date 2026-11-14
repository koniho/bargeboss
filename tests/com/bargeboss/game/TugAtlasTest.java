package com.bargeboss.game;

/** Production sheet addressing and smoke-to-bitmap registration across headings. */
public final class TugAtlasTest {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    private static boolean near(float a,float b) { return Math.abs(a-b)<.00002f; }
    public static void main(String[] args) { run(); }
    public static void run() {
        boolean[] occupied=new boolean[32];
        for(int frame=0;frame<32;frame++) {
            check(SpriteAtlas.file(SpriteAtlas.TUG,frame).equals("tug-isometric-v5-32.png"),"all headings use approved production sheet");
            float l=SpriteAtlas.sourceLeft(0,frame),t=SpriteAtlas.sourceTop(0,frame),r=SpriteAtlas.sourceRight(0,frame),b=SpriteAtlas.sourceBottom(0,frame);
            check(l>=0 && t>=0 && r<=1 && b<=1 && near(r-l,.125f) && near(b-t,.25f),"source crop selects one 8 by 4 cell");
            int cell=Math.round(t*4)*8+Math.round(l*8);
            check(!occupied[cell],"each direction has a distinct source cell");occupied[cell]=true;
            check(near(SpriteAtlas.widthScale(0,frame),SpriteAtlas.heightScale(0,frame))
                    && near(SpriteAtlas.widthScale(0,frame),SpriteAtlas.widthScale(0,0)),"one uniform scale for all directions");
        }
        // Compare the smoke transform to the frame weights actually sent to a host.
        for(boolean river:new boolean[]{false,true}) for(int i=-64;i<=128;i++) {
            float angle=i*(float)Math.PI/64;
            Capture capture=new Capture();Painter painter=river?new RiverPainter(capture):capture;
            SpriteAtlas.draw(painter,0,angle,300,500,90,90,1);
            float x=offset(capture.a,false)*(1-capture.blend)+offset(capture.b,false)*capture.blend;
            float y=offset(capture.a,true)*(1-capture.blend)+offset(capture.b,true)*capture.blend;
            check(near(x,SpriteAtlas.stackOffset(angle,river,false)) && near(y,SpriteAtlas.stackOffset(angle,river,true)),
                    "smoke follows the rendered cell and registration through camera yaw and wrap");
        }
        System.out.println(checks+" production tug atlas and smoke attachment assertions passed");
    }
    private static float offset(int frame,boolean vertical) {
        return vertical?(TugArt.stackY(frame)-SpriteAtlas.pivotY(0,frame))*SpriteAtlas.heightScale(0,frame)
                :(TugArt.stackX(frame)-SpriteAtlas.pivotX(0,frame))*SpriteAtlas.widthScale(0,frame);
    }
    private static final class Capture implements Painter {
        int a,b;float blend;
        public void sprite(int atlas,int frame,float x,float y,float w,float h,float rotation,float alpha) { a=b=frame;blend=0; }
        public void spriteBlend(int atlas,int first,int second,float x,float y,float w,float h,float rotation,float mix,float alpha) { a=first;b=second;blend=mix; }
        public void rect(float x,float y,float w,float h,int color) {}
        public void circle(float x,float y,float radius,int color) {}
        public void line(float x,float y,float xx,float yy,float w,int color) {}
        public void poly(float[] xy,int color) {}
    }
}
