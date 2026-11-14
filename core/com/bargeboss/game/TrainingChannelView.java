package com.bargeboss.game;

/** Existing harbor palette and sprites, arranged along solid winding practice banks. */
public final class TrainingChannelView {
    private TrainingChannelView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        WaveView.draw(canvas,g.visualTicks,0,0,g.layout.width,g.layout.height,c.scale,0);
        g.water.draw(g,p);
        float low=Float.MAX_VALUE,high=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
        for(int corner=0;corner<4;corner++) {
            float sx=(corner%2)*g.layout.width,sy=(corner/2)*g.layout.height;
            low=Math.min(low,c.worldX(sx,sy)-35);high=Math.max(high,c.worldX(sx,sy)+35);
            minY=Math.min(minY,c.worldY(sx,sy)-35);maxY=Math.max(maxY,c.worldY(sx,sy)+35);
        }
        int samples=(int)((high-low)/8)+1;
        for(int side=-1;side<=1;side+=2) {
            float outer=side<0?minY:maxY;
            float[] land=new float[(samples+3)*2];land[0]=low;land[1]=outer;
            for(int i=0;i<=samples;i++) {
                float x=low+(high-low)*i/samples,y=TrainingChannel.center(x)+side*TrainingChannel.HALF_WIDTH;
                land[(i+1)*2]=x;land[(i+1)*2+1]=y;
            }
            land[(samples+2)*2]=high;land[(samples+2)*2+1]=outer;
            p.poly(land,side<0?0xff7c8a99:0xff67788c);
            for(int i=1;i<=samples;i++) {
                float x=land[i*2],y=land[i*2+1],xx=land[(i+1)*2],yy=land[(i+1)*2+1];
                p.line(x,y,xx,yy,7,VisualStyle.OUTLINE);
                p.line(x,y+side*3,xx,yy+side*3,2,VisualStyle.CREAM);
            }
            for(int i=(int)(low/38);i*38<high;i++) {
                float x=i*38,y=TrainingChannel.center(x)+side*(TrainingChannel.HALF_WIDTH+4);
                p.circle(x,y,5,VisualStyle.OUTLINE);p.circle(x+1,y,2,0xff4b5a65);
            }
            for(int i=(int)(low/170);i*170<high;i++) {
                float x=i*170+50,y=TrainingChannel.center(x)+side*(TrainingChannel.HALF_WIDTH+85);
                float sx=c.screenX(x,y),sy=c.screenY(x,y);
                float alpha=SceneryView.opacity(g,sx-45*c.scale,sy-80*c.scale,90*c.scale,100*c.scale);
                canvas.sprite(SpriteAtlas.SCENERY,Math.abs(i*3+(side>0?2:0))%8,sx,sy,90*c.scale,90*c.scale,0,alpha);
            }
        }
        if(low<TrainingChannel.MIN) p.rect(low,minY,TrainingChannel.MIN-low,maxY-minY,0xff67788c);
        if(high>TrainingChannel.MAX) p.rect(TrainingChannel.MAX,minY,high-TrainingChannel.MAX,maxY-minY,0xff67788c);
        int gate=g.tutorial.channelGate;
        for(int i=1;i<TrainingChannel.X.length;i++) {
            float x=TrainingChannel.X[i],y=TrainingChannel.Y[i];
            if(x<low || x>high) continue;
            int color=i<gate?ControlView.GREEN:i==gate?VisualStyle.GOLD:VisualStyle.MUTED;
            for(int side=-1;side<=1;side+=2) {
                float yy=y+side*43;
                p.circle(x,yy,6,VisualStyle.OUTLINE);p.circle(x,yy,4,color);
                p.line(x,yy,x+13,yy,2,VisualStyle.CREAM);
                p.poly(new float[]{x+13,yy,x+7,yy,x+10,yy+side*10},color);
            }
            for(int j=-3;j<=3;j++) p.line(x,y+j*10-3,x,y+j*10+3,2,color);
            RiverPainter.center(p,i==TrainingChannel.X.length-1?"HARBOR":"GATE "+i,x+20,y,9,color);
        }
        int next=Math.min(gate,TrainingChannel.X.length-1);
        float gx=TrainingChannel.X[next],gy=TrainingChannel.Y[next];
        float sy=c.screenY(gx,gy);
        if(sy<82) {
            float sx=Physics.clamp(c.screenX(gx,gy),80,g.layout.width-80);
            ControlView.arrow(canvas,sx,122,sx,88,VisualStyle.GOLD);
            Lettering.center(canvas,"NEXT GATE",sx,131,10,VisualStyle.CREAM);
        }
        VesselView.draw(g,p,g.tug,true);
    }
}
