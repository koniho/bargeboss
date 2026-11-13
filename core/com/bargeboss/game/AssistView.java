package com.bargeboss.game;

/** Compact landscape controls leave the whole river visible. */
public final class AssistView {
    private static final int INK=VisualStyle.OUTLINE,PAPER=VisualStyle.CREAM,MUTED=VisualStyle.MUTED;
    private AssistView() {}
    public static void draw(Game g,Painter p) {
        Layout layout=g.layout;
        if(!layout.rails) {
            p.rect(0,layout.height-80,layout.width,80,VisualStyle.NAVY);
            p.rect(16,layout.height-80,layout.width-32,3,VisualStyle.GOLD);
        }
        ControlView.status(g,p);
        boolean driving=g.holdingTug(),pushing=g.heldCargo()!=null && !g.ropeDragging,mooring=g.ropeDragging || g.ropePhase!=0;
        for(Body b:g.cargo) if(b!=null && b.active && b.lineAttached) mooring=true;

        String moor=g.ropeDragging?"AIM":g.ropePhase==1?"THROW":g.ropePhase==2?"RETRACT":g.ropePhase==3?"CAUGHT":mooring?"UNLOAD":"MOOR";
        if(layout.rails) {
            chip(g,p,layout.width-150,256,138,"DRIVE",driving,0);
            chip(g,p,layout.width-150,291,138,"PUSH",pushing,1);
            chip(g,p,layout.width-150,326,138,moor,mooring,2);
        } else {
            Painter ui=ScreenView.offset(p,layout.offsetX,layout.height-Layout.HEIGHT);
            chip(g,ui,24,573,174,"DRIVE",driving,0);
            chip(g,ui,210,573,174,"PUSH",pushing,1);
            chip(g,ui,396,573,174,moor,mooring,2);
        }
        button(p,layout.box(Layout.BRAKE),"BRAKE",g.braking?VisualStyle.RED:0xff2b4156);
        button(p,layout.box(Layout.HELP),g.tutorial==null?"HELP":"RESET",0xff2b4156);
        button(p,layout.box(Layout.PRACTICE),g.tutorial==null?"PRACTICE":"EXIT",0xff2b4156);
    }
    private static void chip(Game g,Painter p,float x,float y,float w,String label,boolean active,int kind) {
        VisualStyle.panel(p,x,y,w,22,active?0xff304b59:0xff192b3f);
        int color=active?VisualStyle.GOLD:MUTED;
        p.rect(x+7,y+7,4,4,active?VisualStyle.GOLD:0xff43566a);
        float cx=x+42,cy=y+11,phase=(g.visualTicks%36)/36f;
        if(kind==0) {
            p.poly(new float[]{cx-12,cy-4,cx+8,cy-4,cx+13,cy,cx+8,cy+4,cx-12,cy+4},INK);
            p.rect(cx-9,cy-3,15,6,VisualStyle.RED); p.rect(cx-5,cy-6,8,7,PAPER);
            for(int i=0;i<2;i++) p.rect(cx-16-i*5-(active?phase*4:0),cy-2+i*4,3,1,active?0xff9be1d8:0xff3c586a);
        } else if(kind==1) {
            p.rect(cx-12,cy-5,27,10,INK);
            for(int i=0;i<3;i++) p.rect(cx-11+i*9,cy-4,7,7,Game.COLORS[i]);
            if(active) p.poly(new float[]{cx-20+phase*3,cy-3,cx-16+phase*3,cy,cx-20+phase*3,cy+3},color);
        } else {
            p.rect(cx+13,cy-5,5,10,INK); p.rect(cx+14,cy-6,3,4,VisualStyle.GOLD);
            float travel=g.ropePhase==1?(float)g.ropeTicks/Game.ROPE_FLIGHT_TICKS:g.ropePhase==2?1-(float)g.ropeTicks/Game.ROPE_RETURN_TICKS:1;
            float end=g.ropeDragging || !active?cx-10:cx-10+25*Physics.clamp(travel,0,1);
            ControlView.ring(p,end,cy,4,1,g.ropePhase==2?VisualStyle.RED:color,1.3f);
            p.line(cx-13,cy+3,end,cy+2,1,color);
        }
        Lettering.draw(p,label,x+64,y+6,10,color);
    }
    private static void button(Painter p,Layout.Box b,String label,int fill) {
        p.rect(b.x+2,b.y+4,b.w,b.h,INK); VisualStyle.panel(p,b.x,b.y,b.w,b.h,fill);
        p.rect(b.x+7,b.y+5,b.w-14,2,0x667b9fb1);
        Lettering.center(p,label,b.centerX(),b.y+18,label.length()>7?11:14,PAPER);
    }
}
