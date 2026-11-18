package com.bargeboss.game;
/** Continuity, attachment transport, bounded spans and native-wrapper forwarding. */
public final class RopeMaterialTest {
    private static int checks;
    private static void check(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
    private static final class Trace implements Painter {
        int ropes,clips,color;float width;
        public void ropeSegment(float x,float y,float xx,float yy,float w,float from,float to,int c){check(Float.isFinite(x+y+xx+yy+w+from+to),"finite native rope");ropes++;width=w;color=c;}
        public void pushClipOutside(float[] p){clips++;}public void popClip(){clips--;check(clips>=0,"balanced rope clips");}
        public void line(float x,float y,float xx,float yy,float w,int c){}
        public void circle(float x,float y,float r,int c){}public void rect(float x,float y,float w,float h,int c){}public void poly(float[] p,int c){}
        public void sprite(int a,int f,float x,float y,float w,float h,float r,float alpha){}public void spriteBlend(int a,int f,int ff,float x,float y,float w,float h,float r,float mix,float alpha){}
    }
    public static void run(){
        for(int kind=0;kind<4;kind++)for(int heading=0;heading<32;heading++)for(int end:new int[]{-1,1}) {
            Game g=new Game();g.resetPracticeYard();for(int i=0;i<g.cargo.length;i++)g.cargo[i]=null;
            Body b=new Body(1,kind==3?0:kind,300,500,heading*(float)Math.PI/16,kind==3);g.cargo[0]=b;
            g.tug.x=b.x+end*120*(float)StrictMath.cos(b.angle);g.tug.y=b.y+end*120*(float)StrictMath.sin(b.angle);
            g.tow.attach(g,b);TowView.Segments spans=new TowView.Segments(g);
            check(spans.count==24,"one bounded curve per connection for all hull families");
            check(Math.abs(spans.x[0]-RopeAttachment.x(b,end))<.001f && Math.abs(spans.y[0]-RopeAttachment.y(b,end))<.001f,"rope meets selected body-local fitting");
            check(Math.abs(spans.height[0]-RopeAttachment.height(b)-WaterSurface.lift(g,b))<.001f,"fitting follows deck heave");
            for(int i=1;i<25;i++)check(spans.strand[i]>=spans.strand[i-1],"braid does not reset at span joins");
            Trace t=new Trace();Painter p=new RiverPainter(new SceneryOcclusion(new ViewportPainter(t,10000,10000)),g.layout.camera);
            for(int i=0;i<spans.count;i++)spans.draw(p,i);check(t.clips==0,"all hull families restore shore clips");
            check(t.ropes>0,"separated vessels retain visible rope outside their solids");
            float x=spans.x[24],y=spans.y[24],h=spans.height[24];g.tow.release(g);TowView.Segments released=new TowView.Segments(g);
            check(released.x[0]==x && released.y[0]==y && released.height[0]==h,"release begins on the same leader fitting without a height jump");
        }
        Game crossed=new Game();crossed.resetPracticeYard();crossed.tug.x=300;crossed.tug.y=500;crossed.tug.angle=0;crossed.tug.floatingInitialized=true;crossed.tug.heave=0;
        check(RopeOcclusion.blocks(crossed,crossed.tug,300,500,10),"cabin hides a crossing below its roof");
        check(!RopeOcclusion.blocks(crossed,crossed.tug,300,500,40),"high airborne rope clears the roof");
        check(!RopeOcclusion.blocks(crossed,crossed.tug,250,550,3),"near-side water rope remains visible");
        Trace trace=new Trace();Painter p=new WaterBatchPainter(new SceneryOcclusion(new OpacityPainter(trace,.5f)),1000,1000);
        ScreenView.scaled(p,20,30,2).ropeSegment(0,0,10,0,3,0,2,0xc0aa9069);
        check(trace.ropes==1 && trace.color==0x60aa9069 && trace.width==6,"braid and opacity reach native through screen/world wrappers");
        System.out.println(checks+" rope attachment, material continuity and clip assertions passed");
    }
}
