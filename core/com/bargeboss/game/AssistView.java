package com.bargeboss.game;

/** Cabinet-style controls with live driving, pushing and mooring phase indicators. */
public final class AssistView {
    private static final int INK=VisualStyle.OUTLINE,PAPER=VisualStyle.CREAM,MUTED=VisualStyle.MUTED;
    private AssistView() {}
    public static void draw(Game g,Painter p) {
        p.rect(0,714,600,246,VisualStyle.NAVY); p.rect(16,714,568,3,VisualStyle.GOLD);
        ControlView.status(g,p);
        boolean driving=g.holdingTug(),pushing=g.heldCargo()!=null && !g.ropeDragging,mooring=g.ropeDragging || g.ropePhase!=0;
        for(Body b:g.cargo) if(b!=null && b.active && b.lineAttached) mooring=true;
        tile(g,p,24,"DRIVE","DRAG THE TUG",driving,0);
        tile(g,p,210,"PUSH","HOLD THE CARGO",pushing,1);
        String moorHint=g.ropeDragging?"RELEASE TO THROW":g.ropePhase==1?"LINE IN FLIGHT":g.ropePhase==2?"MISS / RETRACT":g.ropePhase==3?"LINE CAUGHT":mooring?"CREW AT WORK":"DRAG TO AIM";
        tile(g,p,396,"MOOR",moorHint,mooring,2);
        button(p,30,"BRAKE",g.braking?VisualStyle.RED:0xff2b4156);
        button(p,216,g.tutorial==null?"HELP":"RESET LESSON",0xff2b4156);
        button(p,402,g.tutorial==null?"PRACTICE":"EXIT PRACTICE",0xff2b4156);
        Lettering.center(p,"2X PACE / RELEASE TO COAST / BRAKE TO STOP",300,948,8,MUTED);
    }
    private static void tile(Game g,Painter p,float x,String label,String hint,boolean active,int kind) {
        VisualStyle.panel(p,x,770,180,94,active?0xff304b59:0xff192b3f);
        int color=active?VisualStyle.GOLD:MUTED;
        p.rect(x+8,778,4,4,active?VisualStyle.GOLD:0xff43566a);
        Lettering.center(p,label,x+90,778,12,color);
        float cx=x+90,cy=817,phase=(g.visualTicks%36)/36f;
        if(kind==0) {
            p.poly(new float[]{cx-24,cy-7,cx+16,cy-7,cx+26,cy,cx+16,cy+7,cx-24,cy+7},INK);
            p.rect(cx-18,cy-6,30,12,VisualStyle.RED); p.rect(cx-10,cy-10,17,13,PAPER);
            p.rect(cx+2,cy-8,5,7,0xff44829a);
            for(int i=0;i<3;i++) p.rect(cx-30-i*8-(active?phase*8:0),cy-7+i%2*11,5,2,active?0xff9be1d8:0xff3c586a);
            if(active) arrow(p,cx+36,cy,8,color);
        } else if(kind==1) {
            p.rect(cx-20,cy-10,47,20,INK); p.rect(cx-18,cy-9,43,16,VisualStyle.RUST);
            for(int i=0;i<3;i++) { p.rect(cx-16+i*14,cy-8,12,13,Game.COLORS[i]); p.rect(cx-15+i*14,cy-8,10,2,PAPER); }
            arrow(p,cx-37+(active?phase*6:0),cy,8,color);
        } else {
            p.rect(cx+19,cy-8,9,16,INK); p.rect(cx+21,cy-10,5,7,VisualStyle.GOLD);
            float loop=active?1+(float)StrictMath.sin(g.visualTicks*.12):0;
            if(g.ropeDragging || !active) {
                // Aim preview keeps a coiled line aboard; the marker is the intended target.
                ControlView.ring(p,cx-28,cy+3,7,1,color,2);
                for(int i=0;i<4;i++) p.rect(cx-13+i*8,cy,3,1,color);
                p.line(cx+17,cy,cx+29,cy,1,color); p.line(cx+23,cy-6,cx+23,cy+6,1,color);
            } else {
                float travel=g.ropePhase==1?(float)g.ropeTicks/Game.ROPE_FLIGHT_TICKS:
                        g.ropePhase==2?1-(float)g.ropeTicks/Game.ROPE_RETURN_TICKS:1;
                travel=Physics.clamp(travel,0,1);
                float end=cx-28+51*travel;
                ControlView.ring(p,end,cy,8+loop,1,g.ropePhase==2?VisualStyle.RED:color,2);
                p.line(cx-28,cy+6,end,cy+4,2,color);
            }
            p.rect(cx-31,cy,9,12,VisualStyle.RUST);
        }
        Lettering.center(p,hint,cx,845,9,active?PAPER:MUTED);
    }
    private static void arrow(Painter p,float x,float y,float size,int color) {
        p.poly(new float[]{x-size,y-3,x+1,y-3,x+1,y-size,x+size,y,x+1,y+size,x+1,y+3,x-size,y+3},color);
    }
    private static void button(Painter p,float x,String label,int fill) {
        p.rect(x+2,885,168,59,INK); VisualStyle.panel(p,x,880,168,60,fill);
        p.rect(x+7,885,154,2,0x667b9fb1);
        Lettering.center(p,label,x+84,902,label.length()>10?11:15,PAPER);
    }
}
