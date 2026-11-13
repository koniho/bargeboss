package com.bargeboss.game;

public final class HelmView {
    private static final int INK=0xff101e25,PAPER=0xffeee7d4,MUTED=0xff8eaaa9;
    private HelmView() {}
    public static void draw(Game g,Painter p) {
        p.rect(0,714,600,246,INK); p.rect(24,714,552,1,0xff476064);
        ControlView.status(g,p);
        int active=g.helm.enabled?PAPER:0xff52656a;
        Lettering.center(p,"THROTTLE",110,765,13,active);
        p.rect(96,793,28,141,0xff060f15);
        for(int i=-2;i<=2;i++) {
            float y=864-i*33;
            p.line(77,y,143,y,2,i==0?PAPER:0xff567077);
        }
        Lettering.draw(p,"FWD",151,794,10,ControlView.GREEN);
        Lettering.draw(p,"N",151,859,12,PAPER);
        Lettering.draw(p,"REV",151,925,10,Game.COLORS[2]);
        float lever=864-g.helm.throttle*66;
        int color=g.helm.enabled?(g.helm.throttle<0?Game.COLORS[2]:g.helm.throttle>0?Game.COLORS[0]:PAPER):0xff52656a;
        p.rect(70,lever-10,80,20,color); p.rect(80,lever-2,60,4,INK);
        Lettering.center(p,"WHEEL",455,765,13,active);
        ControlView.ring(p,455,865,68,1,active,7); ControlView.ring(p,455,865,59,1,0xff30464b,2);
        float rotation=g.helm.rudder*1.05f;
        for(int i=0;i<6;i++) {
            double a=rotation+i*Math.PI/3-Math.PI/2;
            float x=455+66*(float)StrictMath.cos(a),y=865+66*(float)StrictMath.sin(a);
            p.line(455,865,x,y,4,active);
            if(i==0) p.circle(x,y,8,Game.COLORS[0]);
        }
        p.circle(455,865,19,INK); ControlView.ring(p,455,865,19,1,active,3);
        p.line(402,949,508,949,3,0xff52656a); p.circle(455+g.helm.rudder*50,949,5,ControlView.BLUE);
        button(p,768,g.tutorial!=null?"RESET LESSON":g.helm.enabled?"TAP ASSIST":"USE WHEEL",0xff30464b,PAPER);
        button(p,824,g.tutorial!=null?"EXIT PRACTICE":"HELP",0xff30464b,PAPER);
        button(p,880,"BRAKE",g.braking?Game.COLORS[2]:0xff30464b,g.braking?INK:PAPER);
    }
    private static void button(Painter p,float y,String label,int bg,int fg) {
        p.rect(220,y,140,y==880?56:48,bg);
        Lettering.center(p,label,290,y+18,label.length()>10?10:13,fg);
    }
}
