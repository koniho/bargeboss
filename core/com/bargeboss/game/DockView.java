package com.bargeboss.game;

/** Shaded dock machinery and people react to approach, ropes, work, payoff and idle. */
public final class DockView {
    private static final int GOLD=VisualStyle.GOLD,DARK=VisualStyle.OUTLINE,LIGHT=VisualStyle.CREAM;
    private DockView() {}
    public static void draw(Game g,Painter p) {
        for(int berth=0;berth<3;berth++) {
            Body load=null,mooring=null;
            boolean ready=false;
            for(Body b:g.cargo) if(b!=null && b.active && b.berth==berth) {
                if(b.unloadingTicks>0) load=b;
                else if(b.lineAttached) mooring=b;
                ready|=g.canTie(b);
            }
            boolean working=load!=null,tying=mooring!=null;
            boolean throwing=(g.ropePhase==1 || g.ropePhase==3) && g.ropeBody!=null && g.ropeBody.berth==berth;
            boolean celebrating=g.celebrationTicks>0 && Math.abs(g.celebrationY-Game.DOCK_Y[berth])<54;
            int tick=g.visualTicks;
            float sign=berth==1?-1:1,base=berth==1?445:155,y=Game.DOCK_Y[berth];
            // The warehouse apron is inlaid into the shore, clear of the playable water.
            float yard=berth==1?433:143;
            p.rect(yard,y-41,24,106,0xff686c7c); p.rect(yard+2,y-39,20,102,0xff9b9299);
            for(int i=0;i<3;i++) p.line(yard+4+i*7,y-34,yard+4+i*7,y+56,1,0xff817d88);
            p.rect(yard,y+62,24,6,DARK);
            for(int i=0;i<3;i++) p.poly(new float[]{yard+i*8,y+62,yard+i*8+4,y+62,yard+i*8+8,y+68,yard+i*8+4,y+68},GOLD);
            float phase=working?(load.unloadingTicks%60)/60f:0;
            float lift=working?Physics.clamp((phase-.16f)/.62f,0,1):1;
            float hookX=working?load.x+(base-load.x)*lift:base;
            float hookY=working?load.y-14-24*(float)StrictMath.sin(lift*Math.PI):y-29;
            if(!working) hookX+=2*(float)StrictMath.sin(tick*.025+berth);
            crane(p,base,y,hookX,hookY,working||tying||throwing,tick);
            if(working && phase>.15f && phase<.91f) payload(p,hookX,hookY,berth);
            else { p.line(hookX,hookY-3,hookX,hookY+3,2,LIGHT); p.line(hookX,hookY+3,hookX+4,hookY+3,2,LIGHT); }
            float truckY=y+22+(working?10*(float)StrictMath.sin(phase*Math.PI*2):0);
            forklift(p,base,truckY,sign,working,phase,tick);
            for(int i=0;i<2;i++) {
                float wx=base+sign*(i==0?4:-4),wy=y-10+i*66;
                float walk=0,arms=0;
                if(working) {
                    walk=(float)StrictMath.sin(phase*Math.PI*2+i);
                    wx+=sign*1.5f*walk; wy+=3*(float)StrictMath.cos(phase*Math.PI*2+i);
                    arms=i==0?(float)StrictMath.sin(phase*Math.PI*2):.3f;
                } else if(tying || throwing) {
                    if(i==0) wx=base+sign*4;
                    walk=(float)StrictMath.sin(tick*.22); arms=.6f+.3f*walk;
                } else if(celebrating) {
                    arms=1; wy-=2*(float)StrictMath.abs(StrictMath.sin(tick*.2+i));
                } else if(ready) arms=i==0?.65f+.3f*(float)StrictMath.sin(tick*.15):.2f;
                else arms=tick%240>210 && i==1?.45f:0;
                worker(p,wx,wy,sign,walk,arms,i==0?0xffe66b32:0xffd2ab55);
            }
            int stateColor=working||celebrating?0xffa1f0cb:ready||throwing||tying?GOLD:0xffb7bcc2;
            String state=working?"WORK":tying?"TIE":throwing?"CATCH":ready?"READY":celebrating?"DONE":"IDLE";
            HarborView.badge(p,state,base+4,y+86,7.5f,stateColor);
            if(working) {
                p.rect(load.x-30,load.y+55,60,8,DARK);
                p.rect(load.x-28,load.y+57,56*load.unloadingTicks/Game.UNLOAD_TICKS,4,GOLD);
                for(int i=0;i<3;i++) {
                    float t=((tick+i*11)%32)/32f;
                    float xx=base-4+i*3,yy=y-11-t*19;
                    p.rect(xx,yy,3+3*t,3+3*t,((int)(90*(1-t))<<24)|0xd6c2a2);
                }
            }
        }
    }
    private static void crane(Painter p,float x,float y,float hookX,float hookY,boolean active,int tick) {
        // Four-footed lattice mast, dark side face and golden sunlit cross braces.
        p.rect(x-12,y-26,26,24,0x55343b51);
        p.rect(x-12,y-30,24,13,DARK); p.rect(x-10,y-31,20,10,0xffa96322);
        p.line(x-7,y-26,x-5,y-70,5,DARK); p.line(x+8,y-26,x+6,y-70,5,DARK);
        p.line(x-7,y-27,x-5,y-69,3,GOLD); p.line(x+7,y-27,x+5,y-69,3,0xffc58925);
        for(int i=0;i<4;i++) {
            float yy=y-30-i*10;
            p.line(x-6,yy,x+6,yy-8,2,GOLD); p.line(x+6,yy,x-6,yy-8,2,0xffc68c2e);
        }
        p.rect(x-10,y-75,20,10,DARK); p.rect(x-8,y-76,16,8,GOLD);
        p.rect(x-7,y-76,5,4,LIGHT); p.rect(x+2,y-75,5,5,0xff4c8198);
        float boomY=hookY-24;
        p.line(x,y-70,hookX,boomY,7,DARK); p.line(x,y-72,hookX,boomY-2,4,GOLD);
        p.line(x,y-85,hookX,boomY-3,1,LIGHT); p.line(x,y-85,x,y-73,2,DARK);
        p.rect(x-3,y-87,6,4,active&&(tick/6)%2==0?0xffffed94:0xffa85c2d);
        float dx=hookX-x,dy=boomY-(y-70);
        int sections=Math.max(1,(int)(Physics.length(dx,dy)/16));
        for(int i=1;i<sections;i++) {
            float t=i/(float)sections;
            p.line(x+dx*t,y-70+dy*t-3,x+dx*t+4,y-70+dy*t+2,1,0xff8b551d);
        }
        p.circle(hookX,boomY,4,DARK); p.circle(hookX,boomY-1,2,GOLD);
        p.line(hookX,boomY+2,hookX,hookY,1.6f,DARK); p.line(hookX+1,boomY+2,hookX+1,hookY,1,LIGHT);
    }
    private static void forklift(Painter p,float x,float y,float direction,boolean active,float phase,int tick) {
        p.rect(x-14,y-2,31,19,0x663b4250);
        p.rect(x-13,y-4,25,16,DARK); p.rect(x-11,y-5,21,15,GOLD);
        p.rect(x-9,y+2,17,7,0xffbf7929); p.rect(x-8,y-8,12,10,DARK);
        p.rect(x-6,y-8,8,8,0xff72b7c1); p.rect(x-5,y-7,3,5,0xffc6e6db);
        p.line(x-9,y-9,x+5,y-9,3,GOLD);
        for(int side=-1;side<=1;side+=2) {
            p.rect(x+side*9-3,y+8,6,8,DARK); p.rect(x+side*9-1,y+10,2,4,0xff687383);
            if(active) p.rect(x+side*9-2,y+10+(tick/3)%3,4,1,0xffa5a7a3);
        }
        float fx=x+direction*12;
        p.line(fx,y-3,fx,y+10,3,DARK); p.line(fx,y-3,fx,y+10,1,0xffd7d8c6);
        p.line(fx,y+3,fx+direction*8,y+3,2,LIGHT); p.line(fx,y+9,fx+direction*8,y+9,2,LIGHT);
        p.rect(x-1,y-12,3,3,active&&(tick/5)%2==0?0xfffff0a1:0xffbb7134);
        if(active && phase>.7f) { p.rect(x-7,y-4,12,8,0xff9f5d36); p.rect(x-6,y-4,10,2,0xffe1b16b); }
    }
    private static void worker(Painter p,float x,float y,float direction,float stride,float arms,int vest) {
        p.rect(x-6,y+8,13,4,0x66414a58);
        p.line(x-3,y+5,x-3+stride*2,y+11,3,DARK); p.line(x+3,y+5,x+3-stride*2,y+11,3,DARK);
        p.rect(x-5,y-1,10,9,DARK); p.rect(x-4,y-1,8,7,vest); p.rect(x-3,y+2,6,2,LIGHT);
        float handY=y+5-arms*11;
        p.line(x-5,y+1,x-8,y+4-arms*8,2,vest); p.line(x+5,y+1,x+8,handY,2,vest);
        p.rect(x+6,handY-1,3,3,0xffe2b28c); p.rect(x-9,y+3-arms*8,3,3,0xffe2b28c);
        p.rect(x-3,y-7,7,7,DARK); p.rect(x-2,y-6,5,5,0xffdcad87);
        p.rect(x-5,y-10,10,5,GOLD); p.rect(x-6,y-6,12,2,0xffffe089); p.rect(x-3,y-10,2,3,LIGHT);
        p.rect(x+direction*2,y-4,1,2,DARK);
    }
    private static void payload(Painter p,float x,float y,int type) {
        p.line(x-8,y+1,x,y-6,1,LIGHT); p.line(x+8,y+1,x,y-6,1,LIGHT);
        if(type==0) {
            p.rect(x-12,y+1,25,9,DARK);
            for(int i=0;i<3;i++) {
                p.rect(x-10,y+i*3,20,3,0xff8197a4); p.rect(x-10,y+i*3,19,1,0xffe7e5d5);
                p.circle(x+10,y+1+i*3,2,DARK); p.circle(x+10,y+1+i*3,1,0xff97aab2);
            }
            p.rect(x-5,y,2,9,0xffb98039); p.rect(x+4,y,2,9,0xffb98039);
        } else if(type==1) {
            p.rect(x-13,y,26,14,DARK); p.rect(x-12,y-2,24,12,0xffb84732);
            p.poly(new float[]{x-12,y-2,x+12,y-2,x+15,y-6,x-9,y-6},0xffdc7b4c);
            for(int i=-8;i<11;i+=5) p.rect(x+i,y,2,9,0xff7b3f3a);
        } else {
            p.poly(new float[]{x-12,y-3,x+12,y-3,x+8,y+10,x-8,y+10},DARK);
            p.poly(new float[]{x-10,y-3,x+10,y-3,x+6,y+8,x-6,y+8},0xffc88a3e);
            p.poly(new float[]{x-9,y-3,x-4,y-8,x+3,y-6,x+9,y-3},0xfff0c27b);
            p.rect(x-2,y-6,3,2,0xffffdfa2);
        }
    }
}
