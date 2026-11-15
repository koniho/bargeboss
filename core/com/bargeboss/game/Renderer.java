package com.bargeboss.game;

/** Shared composition only: concept art, animations and layout live in pure Java views. */
public final class Renderer {
    private Renderer() {}
    public static void draw(Game root,Painter p) {
        p.beginFrame();
        Game g=root.activeGame();
        p=new SceneryOcclusion(new ViewportPainter(p,g.layout.width,g.layout.height));
        if(g.mode==Game.SETTINGS){ScreenView.settings(g,p);return;}
        if(g.mode==Game.TITLE) { ScreenView.title(g,p); return; }
        if(g.mode==Game.CHALLENGES) { ScreenView.challenges(g,p); return; }
        if(g.tutorial!=null) TrainingChannelView.draw(g,p);
        else {
            SceneryView.ground(g,p);
            SceneryView.piers(g,p);
            SceneryView.props(g,p,false);
            river(g,new RiverPainter(p,g.layout.camera));
            SceneryView.props(g,p,true);
            SceneryView.bridge(g,p);
        }
        g.smoke.draw(p,g.layout.camera,g.layout.width,g.layout.height);
        ControlView.draw(g,new RiverPainter(p,g.layout.camera));
        ArcadeView.effects(g,new RiverPainter(p,g.layout.camera));
        HarborView.objective(g,p);
        FeederView.callouts(g,p);
        ScreenView.hud(g,p);
        AssistView.draw(g,p);
        if(g.mode==Game.HELP) ScreenView.help(g,p);
        else if(g.mode==Game.PAUSE) ScreenView.pause(g,p);
        else if(g.mode==Game.FINISH) ScreenView.finish(g,p);
        else if(g.tutorial!=null) TutorialView.draw(g,p);
        Joystick.draw(g,p);
    }
    private static void river(Game g,Painter p) {
        HarborView.draw(g,p);
        NavigationLights.reflections(g,((RiverPainter)p).canvas());
        if(g.lockStage())LockView.water(g,g.lock,((RiverPainter)p).canvas());
        if(g.trafficWarning() || g.trafficActive()) {
            for(int x=173;x<426;x+=18) p.line(x,Game.TRAFFIC_Y,x+8,Game.TRAFFIC_Y,2,0x88ffc52f);
            if(g.trafficWarning()) SoftCue.caption(p,"CROSS TRAFFIC INBOUND",300,372,10,VisualStyle.GOLD);
        }
        FeederView.draw(g,p);
        Body[] ordered=new Body[g.cargo.length+6]; int count=0;
        for(Body b:g.cargo) if(b!=null && b.active) ordered[count++]=b;
        ordered[count++]=g.tug;
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            ordered[count++]=f.boat;if(f.load!=null) ordered[count++]=f.load;
        }
        if(g.trafficActive()) ordered[count++]=g.trafficBoat;
        for(int i=1;i<count;i++) { Body b=ordered[i]; int j=i-1;
            while(j>=0 && g.layout.camera.screenY(ordered[j].x,ordered[j].y)>g.layout.camera.screenY(b.x,b.y)) { ordered[j+1]=ordered[j];j--; } ordered[j+1]=b;
        }
        TowView.Segments rope=new TowView.Segments(g);int span=0,mark=0,gate=0,gateCount=g.lockStage()?2:0;int[] lights=NavigationLights.ordered(g);
        for(int n=0;n<count;n++) {
            float depth=g.layout.camera.screenY(ordered[n].x,ordered[n].y);
            while(span<rope.count || mark<lights.length || gate<gateCount) {
                float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
                float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
                float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
                if(Math.min(gd,Math.min(rd,ld))>depth)break;
                if(gd<=rd && gd<=ld && gate<gateCount){LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
                if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
            }
            body(g,p,ordered[n]);
        }
        while(span<rope.count || mark<lights.length || gate<gateCount) {
            float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
            float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
            float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
            if(gd<=rd && gd<=ld && gate<gateCount){LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
            if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
        }
        if(g.ticks<360) tag(p,"YOU",g.tug.x,g.tug.y+35,9,VisualStyle.CREAM);
        DockView.draw(g,((RiverPainter)p).canvas());

    }
    /** One vessel and its play cues, shared by the continuous yard depth sorter. */
    static void body(Game g,Painter p,Body b) {
        if(b.berth<0) {
            VesselView.draw(g,p,b,true);
            if(b!=g.tug && b.id==0) HarborView.badge(p,"DELIVERY",b.x,b.y+15,6,VisualStyle.CREAM);
            return;
        }
        if(b.id<0) { VesselView.draw(g,p,b,false);return; }
        int i=-1; for(int slot=0;slot<g.cargo.length;slot++) if(g.cargo[slot]==b) { i=slot;break; }
        int color=Game.COLORS[b.berth];
        if(g.selected==i && !b.lineAttached && g.berthEnabled(b.berth)) {
            SoftCue.dot(p,b.x,b.y,30,SoftCue.animated(g,0x66fff0ce));
            for(int j=1;j<10;j++) {
                float t=j/10f;
                SoftCue.dot(p,b.x+(g.dockX(b.berth)-b.x)*t,b.y+(g.dockY(b.berth)-b.y)*t,1.5f,SoftCue.animated(g,color));
            }
        }
        VesselView.draw(g,p,b,false);
        tag(p,b.deliveryRecorded?"EMPTY":""+g.berthLetter(b.berth)+"-"+(b.id<10?"0":"")+b.id,b.x,b.y-44,10,color);

    }
    private static void tag(Painter p,String text,float x,float y,float size,int color) {
        // Anchor labels beside the hull while keeping their glyphs horizontal.
        SoftCue.caption(p,text,x,y,size,color);
    }
}
