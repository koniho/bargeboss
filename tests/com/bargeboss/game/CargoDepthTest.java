package com.bargeboss.game;

/** Cargo overlap follows the view while unloading preserves each deck-slot identity. */
public final class CargoDepthTest {
    private static int checks;
    private static void check(boolean ok,String message) {checks++;if(!ok) throw new AssertionError(message);}
    private static boolean near(float a,float b) {return Math.abs(a-b)<.002f;}
    public static void run() {
        Game g=new Game();g.visualTicks=43;
        int[] ticks={0,10,70,130},counts={3,2,1,0};
        for(boolean river:new boolean[]{false,true}) for(int berth=0;berth<2;berth++)
            for(int phase=0;phase<ticks.length;phase++) for(int heading=0;heading<32;heading++) {
                Body b=new Body(1,berth,300,550,heading*(float)Math.PI/16);b.unloadingTicks=ticks[phase];
                Capture capture=new Capture(berth==0?SpriteAtlas.STEEL:SpriteAtlas.CONTAINER);
                Painter painter=river?new RiverPainter(capture):capture;
                VesselView.draw(g,painter,b,false,false);
                check(capture.count==counts[phase],"each lift removes exactly its scheduled cargo layer");
                for(int i=1;i<capture.count;i++) check(capture.y[i]>=capture.y[i-1]-.002f,
                        "cargo layers draw from farther to nearer at every heading and unload phase");
                float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle);
                for(int slot=0;slot<counts[phase];slot++) {
                    float localX=berth==0?0:-17+slot*17,localY=berth==0?-8+slot*8:0;
                    float dx=c*localX-s*localY,dy=s*localX+c*localY;
                    float px=river?RiverCamera.Q*(dx+dy)*RiverCamera.DEFAULT.scale:dx;
                    float py=river?RiverCamera.Q*RiverCamera.DEPTH*(dy-dx)*RiverCamera.DEFAULT.scale:dy;
                    boolean found=false;
                    for(int i=0;i<capture.count;i++) if(near(capture.x[i]-capture.hullX,px)
                            && near(capture.y[i]-capture.hullY,py)) found=true;
                    check(found,"depth sorting keeps surviving cargo in its original deck slot");
                }
            }
        System.out.println(checks+" cargo depth order and unloading identity assertions passed");
    }
    private static final class Capture implements Painter {
        final int cargoAtlas;final float[] x=new float[3],y=new float[3];int count;float hullX,hullY;
        Capture(int atlas) {cargoAtlas=atlas;}
        private void record(int atlas,float cx,float cy) {
            if(atlas==SpriteAtlas.BARGE) {hullX=cx;hullY=cy;}
            if(atlas==cargoAtlas) {check(count<3,"no unexpected cargo layers");x[count]=cx;y[count]=cy;count++;}
        }
        public void sprite(int a,int f,float cx,float cy,float w,float h,float r,float alpha) {record(a,cx,cy);}
        public void spriteBlend(int a,int f,int next,float cx,float cy,float w,float h,float r,float blend,float alpha) {record(a,cx,cy);}
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
    }
}
