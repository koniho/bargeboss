package com.bargeboss.game;

public final class CoreTest {
    private static int assertions;
    private static void check(boolean condition,String message) {
        assertions++; if(!condition) throw new AssertionError(message);
    }
    private static Game playing() { Game g=new Game(); ReplayInput.controlFixture(g); return g; }
    private static void steps(Game g,int n) { for(int i=0;i<n;i++) g.step(); }
    private static void tap(Game g,Layout.Box box) { ReplayInput.tap(g,box); }
    public static void main(String[] args) {
        Game g=new Game(); tap(g,Layout.START); check(g.mode==Game.PLAY,"start button");

        g.down(g.tug.x,g.tug.y); g.move(g.downX,g.downY-100); steps(g,60);
        check(g.navigating && g.tug.y<650,"drag drives tug in finger direction");
        g.background(); int ticks=g.ticks; g.advance(1); check(g.ticks==ticks,"background freezes shift");
        check(!g.pointer && !g.pushing,"background cancels gesture"); tap(g,Layout.RESUME); check(g.mode==Game.PLAY,"resume button");
        ticks=g.ticks; tap(g,Layout.HELP); check(g.mode==Game.HELP,"help opens");
        g.advance(.1); check(g.ticks==ticks,"help freezes shift"); tap(g,Layout.START);
        check(g.mode==Game.PLAY && g.ticks==ticks,"help resumes existing shift without reset");
        g=playing(); g.down(g.cargo[0].x,g.cargo[0].y); steps(g,11); check(!g.pushing,"hold threshold avoids accidental push");
        steps(g,1); check(g.pushing,"hold engages"); g.cancel(); check(!g.pushing && !g.pointer,"cancel releases push");
        g.down(g.cargo[0].x,g.cargo[0].y); g.down(100,100);
        check(g.downX==g.cargo[0].x,"second pointer cannot replace owned gesture"); g.cancel();
        Game coast=playing(), drag=playing();
        coast.down(coast.cargo[0].x,coast.cargo[0].y); drag.down(drag.cargo[0].x,drag.cargo[0].y);
        drag.move(drag.cargo[0].x,drag.cargo[0].y-120); steps(coast,150); steps(drag,150);
        check(Physics.length(coast.tug.x-drag.tug.x,coast.tug.y-drag.tug.y)>15,"drag overrides assisted direction");
        g=playing(); g.tug.x=300;g.tug.y=560;g.tug.angle=0;g.tug.vx=60;
        ReplayInput.down(g,g.tug.x,g.tug.y);ReplayInput.move(g,g.downX-90,g.downY);steps(g,25);
        check(g.reversing && g.tug.vx<20,"astern thrust slows forward motion");
        g=playing(); for(int i=0;i<1200;i++) { ReplayInput.work(g); g.step(); }
        check(g.delivered>=1,"assisted first delivery within 20 seconds: tug="+g.tug.x+","+g.tug.y+" cargo="+g.cargo[0].x+","+g.cargo[0].y);
        check(g.score>0,"delivery scores"); int score=g.score; steps(g,10); check(g.score==score,"delivery scores only once");
        g=playing(); Body b=g.cargo[0]; b.x=Game.DOCK_X[0]; b.y=Game.DOCK_Y[0];
        check(!g.inDock(b),"wrong berth does not accept cargo"); b.x=Game.DOCK_X[2]; b.y=Game.DOCK_Y[2];
        check(g.inDock(b),"whole hull fits correct berth"); b.x+=40; check(!g.inDock(b),"center alone insufficient for delivery");
        g=playing(); b=g.cargo[0]; b.x=Game.DOCK_X[2]; b.y=Game.DOCK_Y[2]; b.vx=60; steps(g,1);
        check(b.dockTime==0,"must slow down to moor");
        Body a=new Body(0,-1,300,500,0), c=new Body(1,0,342,500,0); a.vx=70;
        float momentum=a.vx*a.mass+c.vx*c.mass; Physics.collide(a,c);
        check(c.vx>0,"physical collision transfers motion");
        check(Math.abs(a.vx*a.mass+c.vx*c.mass-momentum)<.001,"collision conserves linear momentum");
        g=playing(); steps(g,Game.SHIFT_TICKS); check(g.mode==Game.FINISH,"shift ends");
        check(g.lost>0,"unhandled cargo expires"); g.reset(); check(g.lost==0 && g.ticks==0 && g.hull==100,"restart clears state");
        g.down(300,700); g.move(360,700); g.cancel(); check(!g.pointer,"cancelled drag cannot stick");
        Viewport v=new Viewport(1200,1000); check(Math.abs(v.worldX(v.x+300*v.scale)-300)<.001,"letterboxed coordinate mapping");
        g=playing(); g.down(-1,500); check(!g.pointer,"letterbox tap ignored");
        g=playing(); g.tug.x=415; g.tug.vx=100; steps(g,1); check(g.hull==91,"hard bank collision damages hull once");
        steps(g,1); check(g.hull==91,"impact cooldown avoids multiple charges");
        g=playing(); g.ticks=1200; g.tug.x=g.trafficX()+31; g.tug.y=Game.TRAFFIC_Y; steps(g,1);
        check(g.hull<100,"cross traffic is a physical hazard");
        Game low=playing(), high=playing(); low.down(low.tug.x,low.tug.y); low.move(low.downX,low.downY-100);
        high.down(high.tug.x,high.tug.y); high.move(high.downX,high.downY-100);
        for(int i=0;i<300;i++) low.advance(1.0/30); for(int i=0;i<1440;i++) high.advance(1.0/144);
        check(low.ticks==high.ticks && low.tug.x==high.tug.x && low.tug.y==high.tug.y && low.tug.heave==high.tug.heave && low.tug.engineLoad==high.tug.engineLoad && low.water.emitted==high.water.emitted && low.layout.camera.zoom==high.layout.camera.zoom && low.layout.camera.worldTop==high.layout.camera.worldTop && low.layout.camera.worldRight==high.layout.camera.worldRight,"30 and 144 Hz identical physics, exhaust load and water state");
        g=playing(); g.advance(Double.NaN); check(g.ticks==0,"invalid frame delta ignored");
        g.advance(10); check(g.ticks==15,"long stall has bounded catch-up");
        // A bounded player exercises arrivals and the introductory berth using the public touch contract.
        g=new Game(); g.reset();
        for(int i=0;i<Game.SHIFT_TICKS && g.mode==Game.PLAY;i++) {
            ReplayInput.work(g);
            g.step();
            check(Float.isFinite(g.tug.x) && Float.isFinite(g.tug.y),"finite tug state");
            check(g.tug.x>=Physics.LEFT && g.tug.x<=Physics.RIGHT && g.tug.y>=Physics.TOP && g.tug.y<=Physics.BOTTOM,"tug remains navigable");
        }
        check(g.delivered>=Game.QUOTA,"bounded drag/rope player can meet quota: "+g.delivered+" delivered, "+g.lost+" expired, hull "+g.hull+", ticks "+g.ticks);
        float tx=g.tug.x, ty=g.tug.y; ticks=g.ticks; score=g.score;
        Renderer.draw(g,new Parity()); Renderer.draw(g,new Parity());
        check(g.tug.x==tx && g.tug.y==ty && g.ticks==ticks && g.score==score,"rendering cannot advance or mutate game");
        MooringAccessTest.run();
        ControlsTest.run();
        TutorialHoldTest.run();
        ChannelTest.run();
        ContinuousTutorialTest.run();
        LassoFeedbackTest.run();
        BridgeModelTest.run();
        TowTest.run();
        CargoGestureTest.run();
        ThrottleTest.run();
        MooringEngineTest.run();
        VesselSmokeTest.run();
        SceneCueTest.run();
        ShoreOcclusionTest.run();
        VesselWaterlineTest.run();
        BuildingFadeAnimationTest.run();
        AnimationTest.run();
        TitleLayoutTest.run();
        FrameGateTest.run();
        SpriteGradeTest.run();
        VesselLightingTest.run();
        VesselHighlightTest.run();
        RotationTest.run();
        TugAtlasTest.run();
        BargeAtlasTest.run();
        CargoDepthTest.run();
        ResponsiveTest.run();
        CareerTest.run();
        FeederTest.run();
        SceneryTest.run();
        SceneryOcclusionTest.run();
        BuildingOcclusionTest.run();
        FactoryPlacementTest.run();
        RenderBudgetTest.run();
        TrainingGroundTest.run();
        StaticTerrainTest.run();
        WaterPhysicsTest.run();
        WaterEffectsTest.run();
        SceneryEffectsTest.run();
        FactoryActivityTest.run();
        ShadowActivityTest.run();
        StageCameraTest.run();
        check(Parity.signature().equals("77b68781:11:8972"),"canonical replay changed; review parity before updating baseline: "+Parity.signature());
        System.out.println(assertions+" assertions passed; bounded player delivered "+g.delivered+", score "+g.score+", hull "+g.hull);
        System.out.println("Replay signature: "+Parity.signature());
    }
}
