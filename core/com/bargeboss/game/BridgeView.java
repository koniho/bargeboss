package com.bargeboss.game;

/** Moving deck leaves and shore signals remain registered to the bridge's physical gate. */
public final class BridgeView {
    private BridgeView() {}
    private static int tint(int color,float opacity) { return ((int)((color>>>24)*opacity)<<24)|(color&0xffffff); }
    private static float[] point(RiverCamera c,float x,float y,float height) {
        return new float[]{c.screenX(x,y),c.screenY(x,y)-height*c.scale};
    }
    private static void beam(Painter p,float[] a,float[] b,float width,int color) {
        p.line(a[0],a[1],b[0],b[1],width,color);
    }
    private static void leaf(Game g,Painter canvas,int bank,float lift) {
        RiverCamera c=g.layout.camera;float s=c.scale;
        float root=bank==0?Physics.LEFT:Physics.RIGHT,side=bank==0?1:-1;
        float angle=lift*(float)(Math.PI/2);
        float end=root+side*130*(float)StrictMath.cos(angle),rise=130*(float)StrictMath.sin(angle);
        float[] a=point(c,root,Bridge.Y-18,0),b=point(c,end,Bridge.Y-18,rise);
        float[] d=point(c,root,Bridge.Y+18,0),e=point(c,end,Bridge.Y+18,rise);
        float minX=Math.min(Math.min(a[0],b[0]),Math.min(d[0],e[0]));
        float maxX=Math.max(Math.max(a[0],b[0]),Math.max(d[0],e[0]));
        float minY=Math.min(Math.min(a[1],b[1]),Math.min(d[1],e[1]))-18*s;
        float maxY=Math.max(Math.max(a[1],b[1]),Math.max(d[1],e[1]));
        float alpha=SceneryView.opacity(g,minX,minY,maxX-minX,maxY-minY+5*s);
        canvas.poly(new float[]{a[0],a[1],b[0],b[1],e[0],e[1],d[0],d[1]},tint(0xff95583f,alpha));
        canvas.poly(new float[]{d[0],d[1],e[0],e[1],e[0],e[1]+5*s,d[0],d[1]+5*s},tint(0xff3e3940,alpha));
        // Both raised rails use the same projected hinges and leaf tip, so the
        // machinery retains its perspective throughout opening and closing.
        for(int rail=0;rail<2;rail++) {
            float y=Bridge.Y+(rail==0?-18:18);
            float[] start=point(c,root,y,18),tip=point(c,end,y,rise+18);
            beam(canvas,start,tip,3*s,tint(0xffefb567,alpha));
            for(int rib=0;rib<6;rib++) {
                float t=rib/6f,next=(rib+1)/6f;
                float xx=root+(end-root)*t,xxx=root+(end-root)*next;
                float[] low=point(c,xx,y,rise*t),high=point(c,xx,y,rise*t+18);
                float[] nextHigh=point(c,xxx,y,rise*next+18);
                beam(canvas,low,high,2*s,tint(0xffd99b60,alpha));
                beam(canvas,low,nextHigh,2*s,tint(0xffd99b60,alpha));
            }
        }
    }
    public static void draw(Game g,Painter canvas) {
        RiverCamera c=g.layout.camera;Painter p=new RiverPainter(canvas,c);
        float lift=g.bridge.lift();
        int signal=g.bridge.passable()?0xff9fe4a6:lift>0?VisualStyle.GOLD:VisualStyle.RED;
        leaf(g,canvas,1,lift);leaf(g,canvas,0,lift);
        for(int bank=0;bank<2;bank++) {
            float wx=bank==0?Physics.LEFT:Physics.RIGHT;
            float direction=bank==0?-1:1;
            p.rect(wx+Math.min(0,direction*12),Bridge.Y-24,12,48,VisualStyle.OUTLINE);
            p.line(wx+direction*6,Bridge.Y-22,wx+direction*6,Bridge.Y+22,5,0xff9c8571);
            float sx=c.screenX(wx+direction*6,Bridge.Y+27),sy=c.screenY(wx+direction*6,Bridge.Y+27);
            canvas.line(sx,sy,sx,sy-15*c.scale,3*c.scale,VisualStyle.OUTLINE);
            canvas.circle(sx,sy-15*c.scale,6*c.scale,VisualStyle.OUTLINE);
            canvas.circle(sx,sy-15*c.scale,3.5f*c.scale,signal);
        }
        RiverPainter.center(p,g.bridge.signal(),447,Bridge.Y,10,signal);
        if(!g.bridge.passable()) for(int side=-1;side<=1;side+=2) {
            float y=Bridge.Y+side*90;
            for(int x=250;x<350;x+=14) p.line(x,y,x+7,y,2,0x99ffce65);
            RiverPainter.center(p,"WAIT HERE",300,y+side*15,8,VisualStyle.GOLD);
        }
    }
}
