package com.bargeboss.game;

/** An endless harbor voyage using the same scenery, hulls, light and water as play. */
public final class TitleView {
    private TitleView() {}
    private static float sine(double value) { return (float)StrictMath.sin(value); }
    private static float scroll(float value,float period) { return (value%period+period)%period; }
    public static float horizon(Game g) {return 260+g.layout.offsetY;}
    public static void background(Game g,Painter p) {
        int tick=g.visualTicks;float horizon=horizon(g);
        // Side-elevation steelworks and polluted dusk, painted as one distant plane.
        // Register the factory bases behind the closer bridge and rockwall planes.
        float skyHeight=horizon/.89f,skyWidth=skyHeight*3;
        float skyPeriod=skyWidth*.88f;
        float skyDrift=scroll(skyWidth*.73f-(550+g.layout.offsetX)+tick*.035f,skyPeriod);
        // The painted cloud/smog plane veils the independent, much slower lunar plane.
        p.rect(0,0,g.layout.width,skyHeight,0xff101b24);
        moon(g,p);
        sky(p,skyWidth,skyHeight,g.layout.width,skyDrift);
        p.rect(0,0,g.layout.width,horizon,0x18091920);
        Painter water=TitleWaterView.perspective(p,horizon,g.layout.height);
        WaveView.draw(water,tick,0,horizon,g.layout.width,g.layout.height-horizon,1.35f,-tick*.25f);
    }
    private static void moon(Game g,Painter p) {
        float x=g.layout.width*.55f+55*sine(g.visualTicks*.00012),y=65+g.layout.offsetY*.2f;
        p.circle(x,y,44,0x0ce1c99c);
        p.circle(x,y,35,0x15e1c99c);
        p.circle(x,y,27,0xffd9c9a5);
        p.circle(x-8,y+4,7,0x28797971);
        p.circle(x+9,y-7,5,0x20797971);
        p.circle(x+7,y+12,3,0x18797971);
    }
    private static void shore(Game g,Painter p) {TitleShoreView.draw(g,p);}
    private static void sky(Painter p,float width,float height,float extent,float drift) {
        float overlap=width*.12f,period=width-overlap,slice=overlap/SpriteAtlas.TITLE_SKY_FADE_SLICES;
        // Overlap painted edges with registered alpha slices. This keeps
        // clouds continuous without a hard repeat seam on wide displays.
        for(float x=-drift-period;x<extent;x+=period) {
            for(int i=0;i<SpriteAtlas.TITLE_SKY_FADE_SLICES;i++) {
                float alpha=(i+.5f)/SpriteAtlas.TITLE_SKY_FADE_SLICES;alpha=alpha*alpha*(3-2*alpha);
                p.sprite(SpriteAtlas.TITLE_BACKDROP,i+1,x+(i+.5f)*slice,height/2,slice,height,0,alpha*.82f);
            }
            p.sprite(SpriteAtlas.TITLE_BACKDROP,0,x+overlap+period/2,height/2,period,height,0,.82f);
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
            VesselView.draw(g,p,barges[i],false,false);TitleWaterView.foreground(water,barges[i],tick);RopeAttachment.fitting(g,p,barges[i],1);
            if(i<BARGES-1)RopeAttachment.fitting(g,p,barges[i],-1);
        }
        VesselView.draw(g,p,tug,true,false);TitleWaterView.foreground(water,tug,tick);RopeAttachment.fitting(g,p,tug,-1);
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
