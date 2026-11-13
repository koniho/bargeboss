package com.bargeboss.game;

/** Moving deck leaves and shore signals remain registered to the bridge's physical gate. */
public final class BridgeView {
    private BridgeView() {}
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        float lift=g.bridge.lift(),length=130*(1-lift);
        int signal=g.bridge.passable()?0xff9fe4a6:lift>0?VisualStyle.GOLD:VisualStyle.RED;
        for(int bank=0;bank<2;bank++) {
            float x=bank==0?Physics.LEFT:Physics.RIGHT-length;
            if(length>0) {
                p.rect(x,Bridge.Y-18,length,36,VisualStyle.OUTLINE);
                p.rect(x+2,Bridge.Y-15,Math.max(0,length-4),30,0xffa65b42);
                p.rect(x,Bridge.Y+14,length,4,0xff603d39);
                p.line(x,Bridge.Y-16,x+length,Bridge.Y-16,3,0xffefb567);
                for(int rib=0;rib<length;rib+=22) {
                    float end=Math.min(length,rib+22);
                    p.line(x+rib,Bridge.Y-13,x+end,Bridge.Y+12,2,0xffe2a367);
                    p.line(x+rib,Bridge.Y+12,x+end,Bridge.Y-13,2,0xffe2a367);
                }
            }
            float wx=bank==0?Physics.LEFT:Physics.RIGHT;
            float sx=c.screenX(wx,Bridge.Y),sy=c.screenY(wx,Bridge.Y);
            float size=92*c.scale;
            canvas.sprite(SpriteAtlas.BRIDGE,bank==0?1:0,sx,sy+(bank==0?24:-24)*c.scale,size,size,0,lift);
            p.circle(wx+(bank==0?7:-7),Bridge.Y+27,7,VisualStyle.OUTLINE);
            p.circle(wx+(bank==0?7:-7),Bridge.Y+27,4,signal);
        }
        RiverPainter.center(p,g.bridge.signal(),447,Bridge.Y,10,signal);
        if(!g.bridge.passable()) for(int side=-1;side<=1;side+=2) {
            float y=Bridge.Y+side*90;
            for(int x=250;x<350;x+=14) p.line(x,y,x+7,y,2,0x99ffce65);
            RiverPainter.center(p,"WAIT HERE",300,y+side*15,8,VisualStyle.GOLD);
        }
    }
}
