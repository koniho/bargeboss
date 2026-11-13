package com.bargeboss.game;

/** A slack rope and lasso follow the pointer, then tighten around a bank bollard. */
public final class MooringView {
    private static final int ROPE=0xffe5cc91;
    private MooringView() {}
    public static void draw(Game g,Painter p) {
        for(int berth=0;berth<3;berth++) {
            float x=Game.TIE_X[berth],y=Game.DOCK_Y[berth];
            boolean ready=false;
            for(Body b:g.cargo) if(b!=null && b.berth==berth && (g.canTie(b) || (b.active && b.lineAttached))) ready=true;
            p.circle(x,y,10,0xff13272c); p.circle(x,y,6,0xffa4a68c);
            p.line(x-8,y,x+8,y,4,0xffeee7d4);
            if(ready) {
                float pulse=2*(float)StrictMath.sin(g.ticks*.1);
                ControlView.ring(p,x,y,Game.TIE_RADIUS+pulse,1,ControlView.GREEN,2);
                Lettering.center(p,"TIE",x+(berth==1?-25:25),y-43,10,ControlView.GREEN);
            }
        }
        for(Body b:g.cargo) if(b!=null && b.active) {
            boolean throwing=g.ropeDragging && g.heldCargo()==b;
            if(!throwing && !b.lineAttached) {
                if(g.canTie(b)) ControlView.ring(p,b.x,b.y,38,1,ControlView.GREEN,2);
                continue;
            }
            float x=throwing?g.pointerX:Game.TIE_X[b.berth],y=throwing?g.pointerY:Game.DOCK_Y[b.berth];
            boolean onPoint=throwing && g.overTiePoint(b,x,y) && g.canTie(b);
            int color=onPoint?ControlView.GREEN:ROPE;
            float progress=Math.min(1,b.dockTime/1.2f),slack=throwing?24:24*(1-progress);
            float lastX=b.x,lastY=b.y;
            for(int i=1;i<=18;i++) {
                float t=i/18f,xx=b.x+(x-b.x)*t,yy=b.y+(y-b.y)*t+4*t*(1-t)*slack;
                p.line(lastX,lastY,xx,yy,3,color); lastX=xx; lastY=yy;
            }
            ControlView.ring(p,x,y,throwing?(onPoint?18:22):18-8*progress,1,color,3);
            if(b.lineAttached) {
                ControlView.ring(p,b.x,b.y,43,progress,ControlView.GREEN,4);
                Lettering.center(p,b.unloadingTicks>0?"UNLOADING":"TYING UP",b.x,b.y-64,12,ControlView.GREEN);
            }
        }
    }
}
