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
    public static float horizon(Game g) {return 260+g.layout.offsetY;}
    public static void background(Game g,Painter p) {
        int tick=g.visualTicks;float horizon=horizon(g);
        // The original panoramic composition, repainted in the gameplay palette.
        // Register the painted waterline and keep the sunset visible beside the logo.
        float skyHeight=horizon/.89f,skyWidth=skyHeight*3;
        float skyPeriod=skyWidth*.88f;
        float skyDrift=scroll(skyWidth*.73f-(550+g.layout.offsetX)+tick*.035f,skyPeriod);
        sky(p,skyWidth,skyHeight,g.layout.width,skyDrift);
        Painter water=TitleWaterView.perspective(p,horizon,g.layout.height);
        WaveView.draw(water,tick,0,horizon,g.layout.width,g.layout.height-horizon,1.35f,-tick*.25f);
        float sun=scroll(skyWidth*.73f-skyDrift,skyPeriod);
        for(float x=sun-skyPeriod;x<g.layout.width;x+=skyPeriod) reflections(water,tick,x,horizon,g.layout.height);
    }
    private static void shore(Game g,Painter p) {
        int tick=g.visualTicks;float horizon=horizon(g);
        // A separate transparent bridge/industrial shore passes in front of the
        // distant city. No close quay or bottom bank encloses the foreground water.
        float harborWidth=690,harborHeight=harborWidth/3;
        float harborDrift=scroll(harborWidth*.61f-(420+g.layout.offsetX)+tick*.19f,harborWidth);
        coast(p,harborWidth,horizon+40,g.layout.width,harborDrift);
        strip(p,SpriteAtlas.TITLE_HARBOR,harborWidth,harborHeight,horizon+40-harborHeight*.92f,
                g.layout.width,harborDrift);
    }
    private static void coast(Painter p,float period,float shore,float extent,float drift) {
        for(float x=-drift;x<extent;x+=period) {
            p.sprite(SpriteAtlas.TITLE_LAND,0,x+period*.13f,shore-6,235,235*4/3f,0,1);
            p.sprite(SpriteAtlas.TITLE_LAND,1,x+period*.36f,shore-2,190,190*4/3f,0,1);
            p.sprite(SpriteAtlas.TITLE_LAND,2,x+period*.86f,shore-5,225,225*4/3f,0,1);
            p.sprite(SpriteAtlas.TITLE_LAND,3,x+period*.02f,shore+43,72,96,0,1);
        }
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
    static final float HEADING=(float)StrictMath.PI/4;
    static final int BARGES=3;
    static final float GAP=18,SPACING=77.5f+GAP;
    static void bargePose(Body barge,int index,int tick){
        float behind=27.5f+38.75f+GAP+index*SPACING;
        barge.x=300-RiverCamera.Q*behind;barge.y=300-RiverCamera.Q*behind;
        barge.angle=HEADING;
        barge.heave=sine(tick*.048-index*.55f)*1.1f;
        barge.floatingInitialized=true;barge.vx=barge.vy=48*RiverCamera.Q;
    }
    static RiverCamera camera(Game g){
        float menuLeft=g.layout.width-352,available=menuLeft-64;
        // Three loaded barges and the leading tug occupy one horizontal open-water lane.
        float scale=Math.min(2.35f,available/360);
        float tugX=menuLeft-32-34*scale,waterY=g.layout.height-148;
        RiverCamera origin=new RiverCamera(0,0,scale);
        return new RiverCamera(tugX-origin.screenX(300,300),waterY-origin.screenY(300,300),scale);
    }
    public static void voyage(Game g,Painter canvas) {
        int tick=g.visualTicks;RiverCamera c=camera(g);Painter p=new RiverPainter(canvas,c);
        Body tug=new Body(0,-1,300,300,HEADING);tugPose(tug,tick);
        Body[] barges=new Body[BARGES];
        for(int i=0;i<BARGES;i++){barges[i]=new Body(i+1,i,0,0,HEADING);bargePose(barges[i],i,tick);}
        float waterY=c.screenY(300,300);
        Painter water=new RiverPainter(TitleWaterView.perspective(canvas,horizon(g),waterY),c);
        TitleWaterView.water(water,tug,tick,true);
        for(Body barge:barges)TitleWaterView.water(water,barge,tick,false);
        shore(g,canvas);
        // Tow lines meet the stern of each leader and bow of its follower, rather
        // than leaving the old push knees and pressure arrows between the boats.
        Body leader=tug;
        for(Body barge:barges){
            float ax=RopeAttachment.x(leader,-1),ay=RopeAttachment.y(leader,-1);
            float bx=RopeAttachment.x(barge,1),by=RopeAttachment.y(barge,1);
            float leaderRise=RopeAttachment.height(leader)+leader.heave,bargeRise=RopeAttachment.height(barge)+barge.heave;
            ax+=RiverCamera.riseX(leaderRise);ay+=RiverCamera.riseY(leaderRise);
            bx+=RiverCamera.riseX(bargeRise);by+=RiverCamera.riseY(bargeRise);
            p.ropeSegment(ax,ay,bx,by,1.35f,0,Physics.length(bx-ax,by-ay),RopeMaterial.COLOR);
            leader=barge;
        }
        for(int i=BARGES-1;i>=0;i--){
            VesselView.draw(g,p,barges[i],false,false);RopeAttachment.fitting(g,p,barges[i],1);
            if(i<BARGES-1)RopeAttachment.fitting(g,p,barges[i],-1);
        }
        VesselView.draw(g,p,tug,true,false);RopeAttachment.fitting(g,p,tug,-1);
        g.titleSmoke.draw(canvas,c,g.layout.width,g.layout.height);
    }
    /** Shared pose for the title renderer and fixed-step exhaust emitter. */
    static void tugPose(Body tug,int tick) {
        float pressure=.5f+.5f*sine(tick*.065);
        tug.x=300;tug.y=300;tug.angle=HEADING;
        tug.heave=sine(tick*.048)*1.6f+sine(tick*.031)*.4f;
        tug.floatingInitialized=true;tug.vx=tug.vy=48*RiverCamera.Q;tug.engineLoad=.75f+pressure*.2f;
        tug.wakeKickTicks=(int)(18*Math.max(0,sine(tick*.048+Math.PI/2)));
    }
}
