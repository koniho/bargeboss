package com.bargeboss.game;

/** An endless harbor voyage using the same scenery, hulls, light and water as play. */
public final class TitleView {
    private TitleView() {}
    private static float sine(double value) { return (float)StrictMath.sin(value); }
    public static void background(Game g,Painter p) {
        int tick=g.visualTicks;float shore=330+g.layout.offsetY,bottom=g.layout.height-18;
        WaveView.draw(p,tick,0,shore,g.layout.width,bottom-shore,1.65f,-tick*.25f);
        p.rect(0,0,g.layout.width,shore,0xff7c8a99);
        // Smaller distant buildings and larger quay props pan at different rates,
        // but retain the gameplay atlas's fixed projection and warm key light.
        props(g,p,shore-74,98,.085f,true);
        props(g,p,shore-10,148,.30f,false);
        p.rect(0,shore-13,g.layout.width,13,0xff25394a);
        p.rect(0,shore-15,g.layout.width,4,0xffc3b89b);
        p.rect(0,bottom,g.layout.width,18,0xff7c8a99);
        p.rect(0,bottom,g.layout.width,6,0xffc3b89b);
        for(float x=-(tick*.3f)%96;x<g.layout.width;x+=96) {
            p.line(x,shore-15,x-18,shore-45,1.2f,0x5546576b);
            for(int i=0;i<3;i++) p.poly(new float[]{x+i*8,shore-15,x+i*8+4,shore-15,x+i*8+8,shore-11,x+i*8+4,shore-11},VisualStyle.GOLD);
        }
    }
    private static void props(Game g,Painter p,float foot,float size,float speed,boolean distant) {
        float period=220,drift=(g.visualTicks*speed)%period;
        int[] cells=distant?new int[]{1,4,0,2}:new int[]{0,4,2,1};
        for(int i=-1;i<g.layout.width/period+2;i++) {
            float x=i*period-drift+80;
            LightingView.shadow(p,x,foot,size*.83f,size*.14f,1);
            p.sprite(SpriteAtlas.SCENERY,cells[(i+8)%4],x,foot-size*.47f,size,size,0,distant?.72f:1);
        }
    }
    public static void voyage(Game g,Painter canvas) {
        int tick=g.visualTicks;
        Painter centered=ScreenView.centered(g,canvas);
        RiverCamera c=new RiverCamera(0,154,2.3f);
        Painter p=new RiverPainter(centered,c);
        Body tug=new Body(0,-1,300,230,(float)Math.PI/2);
        Body barge=new Body(1,1,300,300,(float)Math.PI/2);
        tug.angle+=sine(tick*.018)*.025f;barge.angle+=sine(tick*.018-.4)*.018f;
        tug.heave=sine(tick*.048)*1.6f+sine(tick*.031)*.4f;
        barge.heave=sine(tick*.048-.7)*1.2f+sine(tick*.031-.5)*.3f;
        tug.floatingInitialized=barge.floatingInitialized=true;
        tug.vy=barge.vy=48;tug.engineLoad=.84f;
        tug.wakeKickTicks=(int)(18*Math.max(0,sine(tick*.048+Math.PI/2)));
        barge.wakeKickTicks=(int)(16*Math.max(0,sine(tick*.048-.7+Math.PI/2)));
        VesselView.draw(g,p,barge,false);
        VesselView.draw(g,p,tug,true);
    }
}
