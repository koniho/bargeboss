package com.bargeboss.game;

public final class ChannelTest {
    private static int checks;
    private static void check(boolean ok,String message) { checks++;if(!ok) throw new AssertionError(message); }
    private static Game course(float w,float h) {
        Game r=new Game();r.configureCanvas(w,h);r.startTutorial();Game g=r.activeGame();
        g.tutorial.lesson=Tutorial.CHANNEL;g.tutorial.setup(g);return r;
    }
    public static void run() {
        for(float[] size:new float[][]{{960,600},{1333,600},{960,720}}) {
            Game root=course(size[0],size[1]),g=root.activeGame();
            float original=g.layout.camera.worldRight;
            check(g.layout.camera.screenY(TrainingChannel.X[7],TrainingChannel.Y[7])<0
                    && g.layout.camera.screenX(TrainingChannel.X[7],TrainingChannel.Y[7])>g.layout.width,
                    "long winding course continues upward beyond the initial isometric view");
            for(int i=0;i<120;i++)root.step();
            check(g.tutorial.channelGate==1 && g.tutorial.lesson==Tutorial.CHANNEL,"waiting never passes a gate");
            int seen=0;boolean scrolled=false;
            for(int i=0;i<6500 && g.trainingChannel();i++) {
                if(i%20==0) ReplayInput.channel(root);
                RiverCamera c=g.layout.camera;
                float px=c.screenX(g.pointerX,g.pointerY),py=c.screenY(g.pointerX,g.pointerY);
                float ax=c.screenX(g.downX,g.downY),ay=c.screenY(g.downX,g.downY);
                seen|=1<<g.tutorial.channelGate;root.step();
                if(g.trainingChannel()) {
                    check(g.inWater(g.tug.x,g.tug.y),"tug remains within the winding waterway");
                    check(Math.abs(g.layout.camera.screenX(g.downX,g.downY)-ax)<.002f
                            && Math.abs(g.layout.camera.screenY(g.downX,g.downY)-ay)<.002f
                            && Math.abs(g.layout.camera.screenX(g.pointerX,g.pointerY)-px)<.002f
                            && Math.abs(g.layout.camera.screenY(g.pointerX,g.pointerY)-py)<.002f,"scroll preserves physical touch anchors");
                    scrolled|=g.layout.camera.worldRight>original+700;
                    float x=g.layout.camera.screenX(g.tug.x,g.tug.y),y=g.layout.camera.screenY(g.tug.x,g.tug.y);
                    check(x>100 && x<g.layout.width-100 && y>100 && y<g.layout.height-100,
                            "camera keeps tug clear of every screen edge throughout course");
                }
            }
            check(scrolled && seen==254 && g.tutorial.lesson==Tutorial.HOLD,"seven ordered gates lead into cargo practice");
            check(g.cargo[0]!=null && g.cargo[0].awaitingPickup && g.layout.camera.worldRight<=460,"harbor fixtures and camera reset after course");
        }
        Game root=course(960,600),g=root.activeGame();
        g.tug.x=TrainingChannel.X[7];g.tug.y=TrainingChannel.Y[7];g.tutorial.update(g);
        check(g.tutorial.lesson==Tutorial.CHANNEL && g.tutorial.channelGate==1,"finish cannot bypass earlier gates");
        root=course(960,600);g=root.activeGame();ReplayInput.down(root,g.tug.x,g.tug.y);ReplayInput.move(root,g.downX,g.downY+100);
        for(int i=0;i<300;i++)root.step();
        check(g.tutorial.channelGate==1 && g.inWater(g.tug.x,g.tug.y),"driving into a solid bank cannot shortcut the course");
        root.background();float x=g.tug.x;int ticks=g.ticks;root.advance(1);
        check(g.tug.x==x && g.ticks==ticks,"course pauses on background");
        root.configureCanvas(1333,600);g.pause();ReplayInput.tap(root,Layout.HELP);
        check(g.trainingChannel() && g.tutorial.channelGate==1 && g.tug.x==200,"reset restarts channel and responsive camera");
        check(Parity.frame(root)==Parity.frame(root),"channel rendering has no side effects");
        Game low=course(960,600),high=course(960,600);
        for(int second=0;second<12;second++) {
            ReplayInput.channel(low);ReplayInput.channel(high);
            for(int i=0;i<30;i++) low.advance(1.0/30);
            for(int i=0;i<144;i++) high.advance(1.0/144);
        }
        Game a=low.activeGame(),b=high.activeGame();
        check(a.tug.x==b.tug.x && a.tug.y==b.tug.y && a.tug.angle==b.tug.angle
                && a.layout.camera.worldRight==b.layout.camera.worldRight && a.layout.camera.worldTop==b.layout.camera.worldTop && a.tutorial.channelGate==b.tutorial.channelGate,
                "course physics and camera match at 30 and 144 Hz");
        System.out.println(checks+" winding course, solid banks, camera, gates and lifecycle assertions passed");
    }
}
