package com.bargeboss.game;

/** Edge markings, delivery wakes and comic captain calls share one renderer on all hosts. */
public final class FeederView {
    private FeederView() {}
    public static void draw(Game g,Painter p) {
        if(g.tutorial!=null) return;
        for(int i=0;i<g.feeders.length;i++) {
            Feeder f=g.feeders[i];
            int color=f.blocked?VisualStyle.RED:VisualStyle.GOLD;
            p.rect(f.x-42,790,84,46,(f.blocked?0x44000000:0x22000000)|(color&0xffffff));
            for(int side=-1;side<=1;side+=2) {
                p.line(f.x+side*42,789,f.x+side*42,836,3,color);
                float bob=g.water.height(f.x+side*42,805,g.visualTicks,null)*.8f;
                p.circle(f.x+side*42+RiverCamera.riseX(bob),805+RiverCamera.riseY(bob),5,VisualStyle.OUTLINE);
                p.circle(f.x+side*42+RiverCamera.riseX(bob),805+RiverCamera.riseY(bob),3,(g.visualTicks/18)%2==0?color:VisualStyle.CREAM);
            }
            for(int j=0;j<3;j++) {
                float y=798+j*15-(g.visualTicks%30)/2f;
                p.line(f.x-8,y+5,f.x,y,2,color); p.line(f.x,y,f.x+8,y+5,2,color);
            }
            HarborView.badge(p,"FEED "+(i+1),f.x,820,7,color);

        }
    }
    public static void callouts(Game g,Painter p) {
        if(g.tutorial!=null) return;
        RiverCamera c=g.layout.camera;
        for(Feeder f:g.feeders) if(f.shoutTicks>0) {
            boolean hurry=f.shout.equals("HURRY UP!");
            int color=hurry?VisualStyle.RED:Game.COLORS[f.destination];
            float anchorX=c.screenX(f.boat.x,f.boat.y),anchorY=c.screenY(f.boat.x,f.boat.y);
            if(anchorX>g.layout.width+100 || anchorX<-100) {
                Lettering.center(p,"FEED "+(f==g.feeders[0]?1:2)+" / "+f.shout,g.layout.width-150,58+(f==g.feeders[0]?0:20),10,color);continue;
            }
            float x=Physics.clamp(anchorX-220,8,g.layout.width-238),y=Physics.clamp(anchorY-78,60,g.layout.height-112);
            float pulse=f.shoutTicks>132?(150-f.shoutTicks)%6:0;
            p.poly(new float[]{x+174,y+48,x+216,y+48,Math.min(g.layout.width-8,anchorX),anchorY},VisualStyle.OUTLINE);
            VisualStyle.panel(p,x,y-pulse,226,54,VisualStyle.OUTLINE);
            p.rect(x+5,y+5-pulse,216,44,VisualStyle.CREAM);
            Lettering.center(p,f.shout,x+113,y+13-pulse,20,hurry?0xffaa302f:0xff152c42);
            p.line(x+12,y+44-pulse,x+214,y+44-pulse,3,color);
            for(int j=0;j<3;j++) p.line(x-8-j*3,y+12+j*12,x-16-j*4,y+7+j*16,2,color);
            if(!hurry) {
                int zone=f.destination;
                float tx=c.screenX(Game.DOCK_X[zone],g.dockY(zone)),ty=c.screenY(Game.DOCK_X[zone],g.dockY(zone));
                ControlView.ring(p,tx,ty,38+(150-f.shoutTicks)%30,1,color,3);
            }
        }
    }
}
