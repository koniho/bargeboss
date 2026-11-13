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
        float skyPeriod=skyWidth*.88f;
        float skyDrift=scroll(skyWidth*.73f-(550+g.layout.offsetX)+tick*.035f,skyPeriod);
        sky(p,skyWidth,skyHeight,g.layout.width,skyDrift);
        WaveView.draw(p,tick,0,horizon,g.layout.width,g.layout.height-horizon,1.35f,-tick*.25f);
        float sun=scroll(skyWidth*.73f-skyDrift,skyPeriod);
        for(float x=sun-skyPeriod;x<g.layout.width;x+=skyPeriod) reflections(p,tick,x,horizon,g.layout.height);
        // A separate transparent bridge/industrial shore passes in front of the
        // distant city. No close quay or bottom bank encloses the foreground water.
        float harborWidth=690,harborHeight=harborWidth/3;
        float harborDrift=scroll(harborWidth*.61f-(420+g.layout.offsetX)+tick*.19f,harborWidth);
        strip(p,SpriteAtlas.TITLE_HARBOR,harborWidth,harborHeight,horizon+40-harborHeight*.92f,
                g.layout.width,harborDrift);
    }
    private static void sky(Painter p,float width,float height,float extent,float drift) {
        float overlap=width*.12f,period=width-overlap,slice=overlap/SpriteAtlas.TITLE_SKY_FADE_SLICES;
        // Overlap painted edges with registered alpha slices. This keeps
        // clouds continuous without a hard repeat seam on wide displays.
        for(float x=-drift-period;x<extent;x+=period) {
            for(int i=0;i<SpriteAtlas.TITLE_SKY_FADE_SLICES;i++) {
                float alpha=(i+.5f)/SpriteAtlas.TITLE_SKY_FADE_SLICES;alpha=alpha*alpha*(3-2*alpha);
                p.sprite(SpriteAtlas.TITLE_BACKDROP,i+1,x+(i+.5f)*slice,height/2,slice,height,0,alpha);
            }
            p.sprite(SpriteAtlas.TITLE_BACKDROP,0,x+overlap+period/2,height/2,period,height,0,1);
        }
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
