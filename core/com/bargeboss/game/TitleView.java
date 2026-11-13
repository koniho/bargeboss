package com.bargeboss.game;

/** An endless harbor voyage using the same scenery, hulls, light and water as play. */
public final class TitleView {
    private TitleView() {}
    private static float sine(double value) { return (float)StrictMath.sin(value); }
    private static float scroll(float value,float period) { return (value%period+period)%period; }
    private static void strip(Painter p,int atlas,float width,float height,float top,float extent,float drift) {
        for(float x=-drift;x<extent;x+=width)
            p.sprite(atlas,0,x+width/2,top+height/2,width,height,0,1);
    }
    public static void background(Game g,Painter p) {
        int tick=g.visualTicks;float horizon=310+g.layout.offsetY;
        // The original panoramic composition, repainted in the gameplay palette.
        // Register the painted waterline and keep the sunset visible beside the logo.
        float skyHeight=horizon/.89f,skyWidth=skyHeight*3;
        float skyDrift=scroll(skyWidth*.73f-(550+g.layout.offsetX)+tick*.035f,skyWidth);
        strip(p,SpriteAtlas.TITLE_BACKDROP,skyWidth,skyHeight,0,g.layout.width,skyDrift);
        WaveView.draw(p,tick,0,horizon,g.layout.width,g.layout.height-horizon,1.35f,-tick*.25f);
        float sun=scroll(skyWidth*.73f-skyDrift,skyWidth);
        for(float x=sun-skyWidth;x<g.layout.width;x+=skyWidth) reflections(p,tick,x,horizon,g.layout.height);
        // A separate transparent bridge/industrial shore passes in front of the
        // distant city. No close quay or bottom bank encloses the foreground water.
        float harborWidth=690,harborHeight=harborWidth/3;
        float harborDrift=scroll(harborWidth*.61f-(420+g.layout.offsetX)+tick*.19f,harborWidth);
        strip(p,SpriteAtlas.TITLE_HARBOR,harborWidth,harborHeight,horizon+40-harborHeight*.92f,
                g.layout.width,harborDrift);
    }
    private static void reflections(Painter p,int tick,float center,float top,float bottom) {
        for(int i=0;i<16;i++) {
            float y=top+9+i*17+sine(tick*.024+i)*3;if(y>bottom) continue;
            float span=13+i*3.4f,x=center+sine(i*2.3)*(15+i*3)+sine(tick*.017+i)*5;
            int alpha=(int)((.55f+.45f*sine(tick*.035+i))*(76-i*2));
            for(int part=1;part<=8;part++) {
                float a=(part-1)/8f,b=part/8f;
                p.line(x+(a-.5f)*span,y-sine(a*Math.PI)*(2+i*.2f),
                        x+(b-.5f)*span,y-sine(b*Math.PI)*(2+i*.2f),1.5f+i*.06f,(alpha<<24)|0xffcc87);
            }
        }
    }
    public static void voyage(Game g,Painter canvas) {
        int tick=g.visualTicks;
        Painter centered=ScreenView.centered(g,canvas);
        RiverCamera c=new RiverCamera(-110,85,3.4f);
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
