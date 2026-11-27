package com.bargeboss.game;

/** Shared composition only: concept art, animations and layout live in pure Java views. */
public final class Renderer {
    private Renderer() {}
    public static void draw(Game root,Painter p) {
        Game g=root.activeGame();
        p.lighting(g.lighting());p.beginFrame();
        p=new SceneryOcclusion(new ViewportPainter(p,g.layout.width,g.layout.height));
        if(g.mode==Game.SETTINGS){ScreenView.settings(g,p);return;}
        if(g.mode==Game.TITLE) { ScreenView.title(g,p); return; }
        if(g.mode==Game.CHALLENGES) { ScreenView.challenges(g,p); return; }
        if(g.mode==Game.CAPTAINS) { ScreenView.captains(g,p); return; }
        // Impact shake moves only the world projection; HUD, controls and input keep the live layout.
        Layout live=g.layout;
        try {g.layout=g.splashes.shaken(g);WorldArrows.begin(g);SceneryLighting.begin(g);
        SurveyLevel level=g.level();
        if(level!=null)SurveyLevelView.draw(g,p,level);
        else if(g.arenaStage())TagArenaView.draw(g,p);
        else if(g.montroseStage())MontroseView.draw(g,p);
        else TrainingChannelView.draw(g,p);
        g.smoke.draw(p,g.layout.camera,g.layout.width,g.layout.height);
        WeatherView.draw(g,p);
        ControlView.draw(g,new RiverPainter(p,g.layout.camera));
        ArcadeView.effects(g,new RiverPainter(p,g.layout.camera));
        HullDamage.effects(g,p);TugDeath.effects(g,p);HealthBars.draw(g,p);g.harborTraffic.callouts(g,p);if(g.montroseStage())g.arcade.overlay(g,p);if(g.job!=null && g.montroseStage())g.job.overlay(g,p);
        g.layout=live;
        if(!g.dying())HarborView.objective(g,p);
        if(g.job!=null && g.montroseStage())g.job.hud(g,p);
        if(g.montroseStage()){g.arcade.draw(g,p);g.transit.hud(g,p);}
        if(g.arenaStage())TagArenaView.hud(g,p);
        FeederView.callouts(g,p);
        g.abilities.drawScreenFlash(g,p);
        ScreenView.hud(g,p);
        PlayerPortrait.draw(g,p);
        if(!g.conversation())AssistView.draw(g,p);
        TugDeath.overlay(g,p);
        if(g.mode==Game.HELP) ScreenView.help(g,p);
        else if(g.mode==Game.PAUSE) ScreenView.pause(g,p);
        else if(g.mode==Game.FINISH) ScreenView.finish(g,p);
        else if(g.tutorial!=null && !g.dialogue.active) TutorialView.draw(g,p);
        if(!g.conversation() && !g.dying()){Joystick.draw(g,p);g.abilities.drawMap(g,p);}
        TowRelease.draw(g,p);g.abilities.drawUI(g,p);
        if(g.mode==Game.PLAY){g.rescue.radio.draw(g,p);if(MontroseHaulout.active(g))g.haulout.radio.draw(g,p);g.dialogue.draw(g,p);}
        g.waveControls.draw(g,p);
        } finally {g.layout=live;WorldArrows.end();}
    }
    static void river(Game g,Painter p) {
        HarborView.draw(g,p);
        if(g.job!=null && g.montroseStage())g.job.water(g,((RiverPainter)p).canvas());
        if(g.montroseStage())g.transit.water(g,((RiverPainter)p).canvas());
        DockView.shadows(g,((RiverPainter)p).canvas());
        BridgeLayers bridgeLayers=new BridgeLayers(g,((RiverPainter)p).canvas());
        NavigationLights.reflections(g,((RiverPainter)p).canvas());
        if(g.lockStage())LockView.water(g,g.lock,((RiverPainter)p).canvas());
        if(g.trafficWarning() || g.trafficActive()) {
            for(int x=173;x<426;x+=18) p.line(x,Game.TRAFFIC_Y,x+8,Game.TRAFFIC_Y,2,0x88ffc52f);
            if(g.trafficWarning()) SoftCue.caption(p,"CROSS TRAFFIC INBOUND",300,372,10,VisualStyle.GOLD);
        }
        FeederView.draw(g,p);
        Body[] ordered=new Body[g.cargo.length+6+g.harborTraffic.craft.length+(g.job==null?0:g.job.bodies().length)]; int count=0;
        for(Body b:g.cargo) if(b!=null && b.active) ordered[count++]=b;
        ordered[count++]=g.tug;
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            ordered[count++]=f.boat;if(f.load!=null) ordered[count++]=f.load;
        }
        if(g.trafficActive()) ordered[count++]=g.trafficBoat;
        for(HarborTraffic.Craft c:g.harborTraffic.craft)if(c.body.active && HarborTrafficView.visible(g,c.body))ordered[count++]=c.body;
        if(g.job!=null)for(Body b:g.job.bodies())if(b!=null && b.active && HarborTrafficView.visible(g,b))ordered[count++]=b;
        for(int i=1;i<count;i++) { Body b=ordered[i]; int j=i-1;
            while(j>=0 && g.layout.camera.screenY(ordered[j].x,ordered[j].y)>g.layout.camera.screenY(b.x,b.y)) { ordered[j+1]=ordered[j];j--; } ordered[j+1]=b;
        }
        int crane=0;int[] cranes=DockView.ordered(g);TowView.Segments rope=new TowView.Segments(g);int spray=0;int[] splashes=SprayView.ordered(g);int span=0,mark=0,gate=0,gateCount=g.lockStage()?2:0,pedestal=0,pedestalCount=g.montroseStage()?MarinaPedestals.count(g):0;int[] lights=NavigationLights.ordered(g);boolean hoist=g.montroseStage();int landmark=0;int[] landmarks=LocationLandmarks.ordered(g);int moored=0,mooredCount=MooredBoats.count(g);int swimmer=0;int[] swimmers=MontroseRescueView.ordered(g);
        for(int n=0;n<count;n++) {
            float depth=g.layout.camera.screenY(ordered[n].x,ordered[n].y);
            while(crane<cranes.length || span<rope.count || mark<lights.length || gate<gateCount || spray<splashes.length || pedestal<pedestalCount || hoist || landmark<landmarks.length || moored<mooredCount || swimmer<swimmers.length || WorldArrows.pending()) {
                float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
                float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
                float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
                float ed=spray<splashes.length?SprayView.depth(g,splashes[spray]):Float.MAX_VALUE;
                float pd=pedestal<pedestalCount?MarinaPedestals.depth(g,pedestal):Float.MAX_VALUE;
                float hd=Math.min(hoist?MontroseBoatHoist.depth(g):Float.MAX_VALUE,landmark<landmarks.length?LocationLandmarks.depth(g,landmarks[landmark]):Float.MAX_VALUE);
                float md=moored<mooredCount?MooredBoats.depth(g,moored):Float.MAX_VALUE;
                float sd=swimmer<swimmers.length?MontroseRescueView.depth(g,swimmers[swimmer]):Float.MAX_VALUE;
                float cd=crane<cranes.length?DockView.depth(g,cranes[crane]):Float.MAX_VALUE;
                if(crane<cranes.length && cd<=Math.min(depth,Math.min(sd,Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld)))))))))){bridgeLayers.to(cd);DockView.draw(g,((RiverPainter)p).canvas(),cranes[crane++]);continue;}
                if(sd<=Math.min(depth,Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))))) {bridgeLayers.to(sd);MontroseRescueView.draw(g,((RiverPainter)p).canvas(),swimmers[swimmer++]);continue;}
                float ad=WorldArrows.depth();
                if(WorldArrows.pending()&&ad<=Math.min(depth,Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld)))))))){bridgeLayers.to(ad);WorldArrows.draw(p);continue;}
                if(Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))>depth)break;
                if(moored<mooredCount && md<=hd && md<=pd && md<=ed && md<=gd && md<=rd && md<=ld){bridgeLayers.to(md);MooredBoats.draw(g,((RiverPainter)p).canvas(),moored++);continue;}
                if((hoist || landmark<landmarks.length) && hd<=pd && hd<=ed && hd<=gd && hd<=rd && hd<=ld){if(hoist && MontroseBoatHoist.depth(g)<=hd){bridgeLayers.to(hd);MontroseBoatHoist.draw(g,((RiverPainter)p).canvas());hoist=false;}else{bridgeLayers.landmark(hd,landmarks[landmark]);LocationLandmarks.draw(g,((RiverPainter)p).canvas(),landmarks[landmark++]);}continue;}
                if(pd<=ed && pd<=gd && pd<=rd && pd<=ld && pedestal<pedestalCount){bridgeLayers.to(pd);MarinaPedestals.draw(g,((RiverPainter)p).canvas(),pedestal++);continue;}
                if(ed<=gd && ed<=rd && ed<=ld && spray<splashes.length){bridgeLayers.to(ed);SprayView.draw(g,((RiverPainter)p).canvas(),splashes[spray++]);continue;}
                if(gd<=rd && gd<=ld && gate<gateCount){bridgeLayers.to(gd);LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
                bridgeLayers.to(Math.min(rd,ld));if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
            }
            bridgeLayers.to(depth);body(g,p,ordered[n]);
        }
        while(crane<cranes.length || span<rope.count || mark<lights.length || gate<gateCount || spray<splashes.length || pedestal<pedestalCount || hoist || landmark<landmarks.length || moored<mooredCount || swimmer<swimmers.length || WorldArrows.pending()) {
            float rd=span<rope.count?rope.depth(g.layout.camera,span):Float.MAX_VALUE;
            float ld=mark<lights.length?NavigationLights.depth(g,lights[mark]):Float.MAX_VALUE;
            float gd=gate<gateCount?LockView.gateDepth(g,gate):Float.MAX_VALUE;
            float ed=spray<splashes.length?SprayView.depth(g,splashes[spray]):Float.MAX_VALUE;
            float pd=pedestal<pedestalCount?MarinaPedestals.depth(g,pedestal):Float.MAX_VALUE;
            float hd=Math.min(hoist?MontroseBoatHoist.depth(g):Float.MAX_VALUE,landmark<landmarks.length?LocationLandmarks.depth(g,landmarks[landmark]):Float.MAX_VALUE);
            float md=moored<mooredCount?MooredBoats.depth(g,moored):Float.MAX_VALUE;
            float sd=swimmer<swimmers.length?MontroseRescueView.depth(g,swimmers[swimmer]):Float.MAX_VALUE;
            float cd=crane<cranes.length?DockView.depth(g,cranes[crane]):Float.MAX_VALUE;
            if(crane<cranes.length && cd<=Math.min(sd,Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))))){bridgeLayers.to(cd);DockView.draw(g,((RiverPainter)p).canvas(),cranes[crane++]);continue;}
            if(swimmer<swimmers.length && sd<=Math.min(WorldArrows.depth(),Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld)))))))){bridgeLayers.to(sd);MontroseRescueView.draw(g,((RiverPainter)p).canvas(),swimmers[swimmer++]);continue;}
            float ad=WorldArrows.depth();
            if(WorldArrows.pending()&&ad<=Math.min(md,Math.min(hd,Math.min(pd,Math.min(ed,Math.min(gd,Math.min(rd,ld))))))){bridgeLayers.to(ad);WorldArrows.draw(p);continue;}
            if(moored<mooredCount && md<=hd && md<=pd && md<=ed && md<=gd && md<=rd && md<=ld){bridgeLayers.to(md);MooredBoats.draw(g,((RiverPainter)p).canvas(),moored++);continue;}
            if((hoist || landmark<landmarks.length) && hd<=pd && hd<=ed && hd<=gd && hd<=rd && hd<=ld){if(hoist && MontroseBoatHoist.depth(g)<=hd){bridgeLayers.to(hd);MontroseBoatHoist.draw(g,((RiverPainter)p).canvas());hoist=false;}else{bridgeLayers.landmark(hd,landmarks[landmark]);LocationLandmarks.draw(g,((RiverPainter)p).canvas(),landmarks[landmark++]);}continue;}
            if(pd<=ed && pd<=gd && pd<=rd && pd<=ld && pedestal<pedestalCount){bridgeLayers.to(pd);MarinaPedestals.draw(g,((RiverPainter)p).canvas(),pedestal++);continue;}
            if(ed<=gd && ed<=rd && ed<=ld && spray<splashes.length){bridgeLayers.to(ed);SprayView.draw(g,((RiverPainter)p).canvas(),splashes[spray++]);continue;}
            if(gd<=rd && gd<=ld && gate<gateCount){bridgeLayers.to(gd);LockView.drawGate(g,g.lock,((RiverPainter)p).canvas(),gate++);continue;}
            bridgeLayers.to(Math.min(rd,ld));if(rd<=ld)rope.draw(p,span++);else NavigationLights.draw(g,((RiverPainter)p).canvas(),lights[mark++]);
        }
        bridgeLayers.to(Float.POSITIVE_INFINITY);
    }
    /** One vessel and its play cues, shared by the continuous yard depth sorter. */
    static void body(Game g,Painter p,Body b) {
        if(b.traffic){VesselView.draw(g,p,b,false);return;}
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
                SoftCue.dot(p,b.x+(g.dockX(b)-b.x)*t,b.y+(g.dockY(b)-b.y)*t,1.5f,SoftCue.animated(g,color));
            }
        }
        VesselView.draw(g,p,b,false);

    }
}
