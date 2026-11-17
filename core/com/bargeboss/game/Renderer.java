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
        try {WorldArrows.begin(g);
        if(g.indianaStage())IndianaView.draw(g,p);
        else if(g.montroseStage())MontroseView.draw(g,p);
        else TrainingChannelView.draw(g,p);
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
        } finally {WorldArrows.end();}
    }
    static void river(Game g,Painter p) {
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
        TowView.Segments rope=new TowView.Segments(g);int spray=0;int[] splashes=SprayView.ordered(g);int span=0,mark=0,gate=0,gateCount=g.lockStage()?2:0,pedestal=0,pedestalCount=g.montroseStage()?MarinaPedestals.count(g):0;int[] lights=NavigationLights.ordered(g);boolean hoist=g.montroseStage();int landmark=0;int moored=0,mooredCount=MooredBoats.count(g);int swimmer=0;int[] swimmers=MontroseRescueView.ordered(g);
        for(int n=0;n<count;n++) {
            float depth=g.layout.camera.screenY(ordered[n].x,ordered[n].y);
            while(span<rope.count || mark<lights.length || gate<gateCount || spray<splashes.length || pedestal<pedestalCount || hoist || landmark<IndianaView.count(g) || moored<mooredCount || swimmer<swimmers.length || WorldArrows.pending()) {
                float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
                float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
                float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
                float ed=spray<splashes.length?SprayView.depth(g,splashes[spray]):Float.MAX_VALUE;
                float pd=pedestal<pedestalCount?MarinaPedestals.depth(g,pedestal):Float.MAX_VALUE;
                float hd=hoist?MontroseBoatHoist.depth(g):landmark<IndianaView.count(g)?IndianaView.depth(g,landmark):Float.MAX_VALUE;
                float md=moored<mooredCount?MooredBoats.depth(g,moored):Float.MAX_VALUE;
                float sd=swimmer<swimmers.length?MontroseRescueView.depth(g,swimmers[swimmer]):Float.MAX_VALUE;
                if(sd<=Math.min(depth,Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))))) {MontroseRescueView.draw(g,((RiverPainter)p).canvas(),swimmers[swimmer++]);continue;}
                float ad=WorldArrows.depth();
                if(WorldArrows.pending()&&ad<=Math.min(depth,Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld)))))))){WorldArrows.draw(p);continue;}
                if(Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))>depth)break;
                if(moored<mooredCount && md<=hd && md<=pd && md<=ed && md<=gd && md<=rd && md<=ld){MooredBoats.draw(g,((RiverPainter)p).canvas(),moored++);continue;}
                if((hoist || landmark<IndianaView.count(g)) && hd<=pd && hd<=ed && hd<=gd && hd<=rd && hd<=ld){if(hoist){MontroseBoatHoist.draw(g,((RiverPainter)p).canvas());hoist=false;}else IndianaView.landmark(g,((RiverPainter)p).canvas(),landmark++);continue;}
                if(pd<=ed && pd<=gd && pd<=rd && pd<=ld && pedestal<pedestalCount){MarinaPedestals.draw(g,((RiverPainter)p).canvas(),pedestal++);continue;}
                if(ed<=gd && ed<=rd && ed<=ld && spray<splashes.length){SprayView.draw(g,((RiverPainter)p).canvas(),splashes[spray++]);continue;}
                if(gd<=rd && gd<=ld && gate<gateCount){LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
                if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
            }
            body(g,p,ordered[n]);
        }
        while(span<rope.count || mark<lights.length || gate<gateCount || spray<splashes.length || pedestal<pedestalCount || hoist || landmark<IndianaView.count(g) || moored<mooredCount || swimmer<swimmers.length || WorldArrows.pending()) {
            float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
            float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
            float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
            float ed=spray<splashes.length?SprayView.depth(g,splashes[spray]):Float.MAX_VALUE;
            float pd=pedestal<pedestalCount?MarinaPedestals.depth(g,pedestal):Float.MAX_VALUE;
            float hd=hoist?MontroseBoatHoist.depth(g):landmark<IndianaView.count(g)?IndianaView.depth(g,landmark):Float.MAX_VALUE;
            float md=moored<mooredCount?MooredBoats.depth(g,moored):Float.MAX_VALUE;
            float sd=swimmer<swimmers.length?MontroseRescueView.depth(g,swimmers[swimmer]):Float.MAX_VALUE;
            if(swimmer<swimmers.length && sd<=Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld)))))))){MontroseRescueView.draw(g,((RiverPainter)p).canvas(),swimmers[swimmer++]);continue;}
            float ad=WorldArrows.depth();
            if(WorldArrows.pending()&&ad<=Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))){WorldArrows.draw(p);continue;}
            if(moored<mooredCount && md<=hd && md<=pd && md<=ed && md<=gd && md<=rd && md<=ld){MooredBoats.draw(g,((RiverPainter)p).canvas(),moored++);continue;}
            if((hoist || landmark<IndianaView.count(g)) && hd<=pd && hd<=ed && hd<=gd && hd<=rd && hd<=ld){if(hoist){MontroseBoatHoist.draw(g,((RiverPainter)p).canvas());hoist=false;}else IndianaView.landmark(g,((RiverPainter)p).canvas(),landmark++);continue;}
            if(pd<=ed && pd<=gd && pd<=rd && pd<=ld && pedestal<pedestalCount){MarinaPedestals.draw(g,((RiverPainter)p).canvas(),pedestal++);continue;}
            if(ed<=gd && ed<=rd && ed<=ld && spray<splashes.length){SprayView.draw(g,((RiverPainter)p).canvas(),splashes[spray++]);continue;}
            if(gd<=rd && gd<=ld && gate<gateCount){LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
            if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
        }
        DockView.draw(g,((RiverPainter)p).canvas());

    }
    /** One vessel and its play cues, shared by the continuous yard depth sorter. */
    static void body(Game g,Painter p,Body b) {
        if(b.berth<0) {
            VesselView.draw(g,p,b,true);
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

    }
}
