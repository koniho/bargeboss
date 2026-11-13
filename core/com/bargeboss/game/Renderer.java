package com.bargeboss.game;

/** Shared composition only: concept art, animations and layout live in pure Java views. */
public final class Renderer {
    private Renderer() {}
    public static void draw(Game root,Painter p) {
        Game g=root.activeGame();
        if(g.mode==Game.TITLE) { ScreenView.title(g,p); return; }
        if(g.mode==Game.CHALLENGES) { ScreenView.challenges(g,p); return; }
        p.rect(0,0,g.layout.width,g.layout.height,0xff737582);
        river(g,new RiverPainter(p,g.layout.camera));
        FeederView.callouts(g,p);
        ScreenView.hud(g,p);
        AssistView.draw(g,p);
        if(g.mode==Game.HELP) ScreenView.help(g,p);
        else if(g.mode==Game.PAUSE) ScreenView.pause(g,p);
        else if(g.mode==Game.FINISH) ScreenView.finish(g,p);
        else if(g.tutorial!=null) TutorialView.draw(g,p);
    }
    private static void river(Game g,Painter p) {
        HarborView.draw(g,p);
        if(g.trafficWarning() || g.trafficActive()) {
            for(int x=173;x<426;x+=18) p.line(x,395,x+8,395,2,0x88ffc52f);
            if(g.trafficWarning()) RiverPainter.center(p,"CROSS TRAFFIC INBOUND",300,372,10,VisualStyle.GOLD);
        }
        if(g.trafficActive()) {
            Body ship=new Body(-1,-1,g.trafficX(),395,0); ship.vx=57;
            VesselView.draw(g,p,ship,true);
        }
        FeederView.draw(g,p);
        // Painted hulls share the enlarged physics footprint.
        for(int i=0;i<g.cargo.length;i++) {
            Body b=g.cargo[i]; if(b==null || !b.active) continue;
            int color=Game.COLORS[b.berth];
            if(g.selected==i && !b.lineAttached) {
                p.circle(b.x,b.y,40,0x18fff0ce);
                for(int j=1;j<10;j++) {
                    float t=j/10f;
                    p.circle(b.x+(Game.DOCK_X[b.berth]-b.x)*t,b.y+(Game.DOCK_Y[b.berth]-b.y)*t,1.5f,(color&0xffffff)|0x77000000);
                }
            }
            VesselView.draw(g,p,b,false);
            tag(p,""+(char)('A'+b.berth)+"-"+(b.id<10?"0":"")+b.id,b.x,b.y-44,10,color);
            if(!b.lineAttached) {
                p.rect(b.x-23,b.y+31,46,4,VisualStyle.OUTLINE);
                p.rect(b.x-22,b.y+32,44*Math.max(0,1-b.age/68),2,b.age>48?VisualStyle.RED:color);
            }
        }
        VesselView.draw(g,p,g.tug,true);
        if(g.ticks<360) tag(p,"YOU",g.tug.x,g.tug.y+35,9,VisualStyle.CREAM);
        DockView.draw(g,p);
        ControlView.draw(g,p);
        ArcadeView.effects(g,p);
    }
    private static void tag(Painter p,String text,float x,float y,float size,int color) {
        // Anchor labels beside the hull while keeping their glyphs horizontal.
        RiverPainter.center(p,text,x,y,size,color);
    }
}
