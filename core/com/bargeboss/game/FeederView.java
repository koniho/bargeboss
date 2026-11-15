package com.bargeboss.game;

/** Edge markings, delivery wakes and comic captain calls share one renderer on all hosts. */
public final class FeederView {
    private FeederView() {}
    public static void draw(Game g,Painter p) {
        if(g.tutorial!=null) return;
        for(int i=0;i<g.feeders.length;i++) {
            Feeder f=g.feeders[i];
            int color=SoftCue.animated(g,f.blocked?VisualStyle.RED:VisualStyle.GOLD);
            p.rect(f.x-42,790,84,46,(f.blocked?0x44000000:0x22000000)|(color&0xffffff));
            for(int side=-1;side<=1;side+=2) {
                SoftCue.line(p,f.x+side*42,789,f.x+side*42,836,1,color);
                float bob=g.water.height(f.x+side*42,805,g.visualTicks,null)*.8f;
                p.circle(f.x+side*42+RiverCamera.riseX(bob),805+RiverCamera.riseY(bob),5,VisualStyle.OUTLINE);
                p.circle(f.x+side*42+RiverCamera.riseX(bob),805+RiverCamera.riseY(bob),3,(g.visualTicks/18)%2==0?color:VisualStyle.CREAM);
            }
            for(int j=0;j<3;j++) {
                float y=798+j*15-(g.visualTicks%30)/2f;
                SoftCue.line(p,f.x-8,y+5,f.x,y,1,color); SoftCue.line(p,f.x,y,f.x+8,y+5,1,color);
            }

        }
    }
    public static void callouts(Game g,Painter p) {
        if(g.tutorial!=null) return;
        RiverCamera c=g.layout.camera;
        for(Feeder f:g.feeders) if(f.shoutTicks>0) {
            boolean hurry=f.shout.equals("HURRY UP!");
            int color=hurry?VisualStyle.RED:Game.COLORS[f.destination];
            // Captain announcements live in a fixed HUD lane, never over the vessels.
            Lettering.center(p,"FEED "+(f==g.feeders[0]?1:2)+" / "+f.shout,g.layout.width-150,58+(f==g.feeders[0]?0:20),10,color);
            if(!hurry) {
                int zone=f.destination;
                float tx=c.screenX(g.dockX(zone),g.dockY(zone)),ty=c.screenY(g.dockX(zone),g.dockY(zone));
                ControlView.ring(g,p,tx,ty,38+(150-f.shoutTicks)%30,1,color,3);
            }
        }
    }
}
