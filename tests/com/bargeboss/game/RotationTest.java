package com.bargeboss.game;

/** Rotation contract: fixed footprint registration, wrap-safe blends, and pure drawing. */
public final class RotationTest {
    private static int assertions;
    private static void check(boolean ok,String message) { assertions++; if(!ok) throw new AssertionError(message); }
    private static boolean near(float a,float b) { return Math.abs(a-b)<.0002f; }
    public static void run() {
        final float fullTurn=(float)(2*Math.PI),step=fullTurn/32,epsilon=.000005f;
        int[] assets={SpriteAtlas.TUG,SpriteAtlas.BARGE,SpriteAtlas.STEEL,SpriteAtlas.CONTAINER,SpriteAtlas.BULK};
        for(int atlas:assets) {
            long seen=0;
            check(SpriteAtlas.count(atlas)==32,"each turning asset exposes 32 logical headings");
            for(int i=0;i<=720;i++) {
                float angle=i*fullTurn/720-fullTurn;
                Capture p=sample(atlas,angle,.37f);
                check(p.count==1,"one native blend operation per asset draw");
                Draw d=p.draws[0];
                check(d.frameA>=0 && d.frameA<32 && d.frameB>=0 && d.frameB<32,"rotation always addresses a valid 32-frame cell");
                check(d.cx==250.25f && d.cy==400.75f,"every heading preserves the requested footprint center");
                check(d.width==128 && d.height==144 && d.rotation==0,"turning swaps directional art without rotating or resizing the sprite rectangle");
                check(d.blend>=0 && d.blend<=1 && near(d.alpha,.37f),"blend keeps its supplied opacity, including semitransparent rendering");
                check((d.frameB-d.frameA+32)%32<=1,"blend only combines neighboring headings including the 31-to-0 wrap");
                seen|=1L<<d.frameA; seen|=1L<<d.frameB;
            }
            check(seen==0xffffffffL,"full turn uses all 32 headings for atlas "+atlas);
            for(int boundary=0;boundary<=32;boundary++) {
                Capture before=sample(atlas,boundary*step-epsilon,1),after=sample(atlas,boundary*step+epsilon,1);
                check(weightDifference(before,after)<.001f,"no frame contribution jump at sector boundary "+boundary+" atlas "+atlas);
            }
            // Short crossfades hold crisp views outside the center of a sector.
            // Check the hold-to-blend edges as well as the steepest midpoint.
            for(int sector=0;sector<32;sector++) for(float fraction:new float[]{.4f,.5f,.6f}) {
                float angle=(sector+fraction)*step;
                check(weightDifference(sample(atlas,angle-epsilon,1),sample(atlas,angle+epsilon,1))<.001f,
                        "short blend stays continuous at "+fraction+" of sector "+sector+" atlas "+atlas);
            }
            check(weightDifference(sample(atlas,0,1),sample(atlas,fullTurn,1))<.0001f,"full revolution returns to exactly the same appearance contract");
            check(weightDifference(sample(atlas,-epsilon,1),sample(atlas,fullTurn-epsilon,1))<.0001f,"negative headings wrap consistently");
            Capture nativeCalls=new Capture();
            SpriteAtlas.draw(new RiverPainter(nativeCalls),atlas,.27f,300,550,128,144,1);
            Capture direct=new Capture(); SpriteAtlas.draw(direct,atlas,.27f,300,550,128,144,1);
            Draw river=nativeCalls.draws[0],world=direct.draws[0];
            check(near(river.cx,RiverPainter.screenX(300,550)) && near(river.cy,RiverPainter.screenY(300,550)),"camera rotation preserves one mapped footprint center");
            check(river.frameA==(world.frameA+28)%32 && river.frameB==(world.frameB+28)%32,"isometric camera shifts both blend frames by minus 45 degrees");
            check(near(river.width,128*RiverPainter.SCALE) && near(river.height,144*RiverPainter.SCALE)
                    && near(river.blend,world.blend),"camera scales dimensions without changing blend progress");
        }
        // Full vessel composition: cargo rotates around the hull instead of drifting
        // when an image frame changes. The game itself never advances during this check.
        Game g=new Game(); g.reset(); g.visualTicks=43;
        for(int kind=-1;kind<3;kind++) {
            Body b=new Body(kind<0?0:1,kind,300,550,0); b.lineAttached=true;
            Capture first=new Capture(); VesselView.draw(g,first,b,kind<0);
            float[] radius=new float[first.count];
            for(int k=0;k<first.count;k++) radius[k]=Physics.length(first.draws[k].cx-first.draws[0].cx,first.draws[k].cy-first.draws[0].cy);
            for(int i=0;i<=360;i++) {
                b.angle=i*fullTurn/360;
                int ticks=g.ticks,visual=g.visualTicks; float x=b.x,y=b.y,angle=b.angle;
                Capture p=new Capture(); VesselView.draw(g,p,b,kind<0);
                check(p.count==first.count,"turning cannot add or remove cargo layers");
                check(p.draws[0].cx==first.draws[0].cx && p.draws[0].cy==first.draws[0].cy,"stationary hull stays on crosshair through the entire turn");
                for(int k=0;k<p.count;k++) check(near(Physics.length(p.draws[k].cx-p.draws[0].cx,p.draws[k].cy-p.draws[0].cy),radius[k]),"cargo keeps its fixed distance from the hull center at every heading");
                check(g.ticks==ticks && g.visualTicks==visual && b.x==x && b.y==y && b.angle==angle,"rotation drawing never mutates clocks or pose");
            }
        }
        depthOrder();
        System.out.println(assertions+" sprite rotation, fixed-anchor and blend continuity assertions passed");
    }
    private static void depthOrder() {
        Game g=new Game();g.reset();
        for(Feeder feeder:g.feeders) {feeder.phase=Feeder.IDLE;feeder.load=null;}
        for(int i=0;i<g.cargo.length;i++) g.cargo[i]=null;
        g.cargo[0]=new Body(1,2,250,400,0);g.tug.x=350;g.tug.y=650;
        HullOrder capture=new HullOrder();g.render(capture);
        check(capture.count==2 && capture.first==SpriteAtlas.BARGE && capture.last==SpriteAtlas.TUG,
                "smaller projected ground depth renders behind even when world X alone suggests the opposite");
        g.cargo[0].y=650;g.tug.y=350;capture=new HullOrder();g.render(capture);
        check(capture.count==2 && capture.first==SpriteAtlas.TUG && capture.last==SpriteAtlas.BARGE,
                "changing world Y reverses overlap order across the isometric plane");
    }
    private static final class HullOrder implements Painter {
        int first=-1,last=-1,count;
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha) {record(a);}
        public void spriteBlend(int a,int f,int b,float x,float y,float w,float h,float r,float blend,float alpha) {record(a);}
        private void record(int atlas) {if(atlas==SpriteAtlas.TUG || atlas==SpriteAtlas.BARGE) {if(count++==0) first=atlas;last=atlas;}}
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
    }
    private static Capture sample(int atlas,float angle,float alpha) {
        Capture p=new Capture(); SpriteAtlas.draw(p,atlas,angle,250.25f,400.75f,128,144,alpha); return p;
    }
    private static float weightDifference(Capture a,Capture b) {
        float[] left=weights(a),right=weights(b); float difference=0;
        for(int i=0;i<32;i++) difference+=Math.abs(left[i]-right[i]); return difference;
    }
    private static float[] weights(Capture p) {
        float[] result=new float[32];
        for(int i=0;i<p.count;i++) {
            Draw d=p.draws[i]; result[d.frameA]+=d.alpha*(1-d.blend); result[d.frameB]+=d.alpha*d.blend;
        }
        return result;
    }
    private static final class Draw {
        int atlas,frameA,frameB; float cx,cy,width,height,rotation,blend,alpha;
    }
    private static final class Capture implements Painter {
        final Draw[] draws=new Draw[16]; int count;
        public void sprite(int atlas,int frame,float cx,float cy,float width,float height,float rotation,float alpha) {
            spriteBlend(atlas,frame,frame,cx,cy,width,height,rotation,0,alpha);
        }
        public void spriteBlend(int atlas,int frameA,int frameB,float cx,float cy,float width,float height,float rotation,float blend,float alpha) {
            if(SurfaceLightArt.atlas(atlas))return; // Response registration is checked by SurfaceLightingTest.
            if(count>=draws.length) throw new AssertionError("Unexpected sprite layer count");
            Draw d=new Draw(); d.atlas=atlas;d.frameA=frameA;d.frameB=frameB;d.cx=cx;d.cy=cy;
            d.width=width;d.height=height;d.rotation=rotation;d.blend=blend;d.alpha=alpha;draws[count++]=d;
        }
        public void rect(float x,float y,float w,float h,int c) {}
        public void circle(float x,float y,float r,int c) {}
        public void poly(float[] xy,int c) {}
        public void line(float x,float y,float xx,float yy,float w,int c) {}
    }
}
