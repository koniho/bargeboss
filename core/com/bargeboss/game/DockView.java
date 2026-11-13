package com.bargeboss.game;

/** Each dock has a crane, forklift and two workers: three one-second lifts, then idle. */
public final class DockView {
    private static final int YELLOW=0xffffcc54, DARK=0xff102b40, LIGHT=0xfff3f8df;
    private DockView() {}
    public static void draw(Game g,Painter p) {
        for(int berth=0;berth<3;berth++) {
            Body load=null;
            for(Body b:g.cargo) if(b!=null && b.active && b.berth==berth && b.unloadingTicks>0) { load=b; break; }
            boolean working=load!=null;
            float sign=berth==1?-1:1,base=berth==1?518:82,y=Game.DOCK_Y[berth];
            p.rect(berth==1?502:10,y-45,88,104,0xff163d50);
            p.rect(berth==1?502:10,y+56,88,3,Game.COLORS[berth]);
            for(int i=0;i<4;i++) p.line(base-sign*48+i*sign*12,y+37,base-sign*40+i*sign*12,y+45,3,YELLOW);
            float phase=working?(load.unloadingTicks%60)/60f:0;
            float lift=working?Math.min(1,Math.max(0,(phase-.15f)/.65f)):1;
            float hookX=working?load.x+(base-sign*25-load.x)*lift:base-sign*20;
            float hookY=working?load.y-12-24*(float)StrictMath.sin(lift*Math.PI):y-22;
            // Raised boom and cable visibly carry a container from deck to shore.
            p.circle(base,y-25,11,DARK); p.circle(base,y-27,8,YELLOW);
            p.line(base,y-27,hookX,hookY-18,7,DARK);
            p.line(base,y-29,hookX,hookY-20,4,YELLOW);
            p.line(base-sign*10,y-4,base,y-30,4,YELLOW);
            p.line(hookX,hookY-20,hookX,hookY,2,LIGHT);
            if(working && phase>.15f && phase<.9f) {
                p.rect(hookX-12,hookY,24,14,DARK); p.rect(hookX-11,hookY-2,22,12,Game.COLORS[berth]);
                for(int i=-7;i<11;i+=5) p.line(hookX+i,hookY,hookX+i,hookY+8,1,LIGHT);
            } else p.line(hookX-4,hookY,hookX+4,hookY,2,LIGHT);
            float truckX=base-sign*27+(working?sign*12*(float)StrictMath.sin(phase*Math.PI*2):0);
            p.rect(truckX-12,y+10,24,14,DARK); p.rect(truckX-10,y+8,20,14,YELLOW);
            p.rect(truckX-6,y+10,8,9,0xff48dcea);
            p.circle(truckX-7,y+23,3,DARK); p.circle(truckX+7,y+23,3,DARK);
            p.line(truckX+sign*10,y+12,truckX+sign*17,y+12,2,LIGHT);
            p.line(truckX+sign*10,y+19,truckX+sign*17,y+19,2,LIGHT);
            for(int i=0;i<2;i++) {
                float wx=base-sign*(i==0?8:49),wy=y+5+i*26;
                if(working) { wx+=sign*5*(float)StrictMath.sin(phase*Math.PI*2+i); wy+=3*(float)StrictMath.cos(phase*Math.PI*2+i); }
                p.circle(wx+2,wy+3,6,0x66091524);
                p.line(wx-3,wy+5,wx-3,wy+9,2,DARK); p.line(wx+3,wy+5,wx+3,wy+9,2,DARK);
                p.circle(wx,wy+3,5,0xffff8753); p.circle(wx,wy-2,4,LIGHT); p.rect(wx-5,wy-5,10,3,YELLOW);
            }
            Lettering.center(p,working?"WORKING":"IDLE",berth==1?547:53,y+65,8,working?ControlView.GREEN:0xff8eaaa9);
            if(working) {
                p.rect(load.x-30,load.y+55,60,7,DARK);
                p.rect(load.x-29,load.y+56,58*load.unloadingTicks/Game.UNLOAD_TICKS,5,ControlView.GREEN);
                for(int i=0;i<3;i++) {
                    float t=((g.ticks+i*13)%30)/30f;
                    p.circle(base-sign*25+sign*i*5,y-7-t*18,2+3*t,0x668de8d4);
                }
            }
        }
    }
}
