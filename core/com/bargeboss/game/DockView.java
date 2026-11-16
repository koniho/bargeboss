package com.bargeboss.game;

/** Shaded dock machinery and people react to approach, ropes, work, payoff and idle. */
public final class DockView {
    private static final int GOLD=SceneLighting.sceneryColor(VisualStyle.GOLD),DARK=SceneLighting.sceneryColor(VisualStyle.OUTLINE),LIGHT=SceneLighting.sceneryColor(VisualStyle.CREAM);
    private DockView() {}
    public static void draw(Game g,Painter canvas) {
        if(g.montroseStage())return;
        for(int berth=0;berth<3;berth++) {
            if(!g.berthDisplayEnabled(berth)) continue;
            Body load=null,mooring=null;
            for(Body b:g.cargo) if(b!=null && b.active && !b.deliveryRecorded && (g.sharedBerthStage() || b.berth==berth)) {
                if(b.unloadingTicks>0) load=b;
                else if(b.lineAttached) mooring=b;
            }
            boolean working=load!=null,tying=mooring!=null;
            boolean throwing=(g.ropePhase==1 || g.ropePhase==3) && g.ropeBody!=null && (g.sharedBerthStage() || g.ropeBody.berth==berth);
            int tick=g.visualTicks;
            RiverCamera camera=g.layout.camera;
            float[] shore=new float[2];DockWorkers.shoreAnchor(g,berth,0,16,shore);
            float anchorX=camera.screenX(shore[0],shore[1]);
            float anchorY=camera.screenY(shore[0],shore[1])-GroundArt.LAND_RISE*camera.scale;
            float scale=camera.scale*.55f;
            Painter p=ScreenView.scaled(canvas,anchorX,anchorY,scale);
            float base=0,y=0;
            float phase=working?(load.unloadingTicks%60)/60f:0;
            float lift=working?Physics.clamp((phase-.16f)/.62f,0,1):1;
            float loadX=working?(camera.screenX(load.x,load.y)-anchorX)/scale:0;
            float loadY=working?(camera.screenY(load.x,load.y)-anchorY)/scale:0;
            float hookX=working?loadX*(1-lift):base+28;
            float hookY=working?loadY*(1-lift)-14-24*(float)StrictMath.sin(lift*Math.PI):y-29;
            if(!working) hookX+=2*(float)StrictMath.sin(tick*.025+berth);
            crane(p,base,y+17,hookX,hookY,working||tying||throwing,tick);
            if(working && phase>.15f && phase<.91f) payload(p,hookX,hookY,berth);
            else { p.line(hookX,hookY-3,hookX,hookY+3,2,LIGHT); p.line(hookX,hookY+3,hookX+4,hookY+3,2,LIGHT); }
            if(working) {
                p.rect(loadX-30,loadY+35,60,8,DARK);
                p.rect(loadX-28,loadY+37,56*load.unloadingTicks/Game.UNLOAD_TICKS,4,VisualStyle.GOLD);
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
        p.poly(new float[]{x-16,y-17,x,y-21,x+16,y-17,x,y-13},SceneLighting.sceneryColor(0x55343b51));
        p.rect(x-12,y-30,24,13,DARK); p.rect(x-10,y-31,20,10,SceneLighting.sceneryColor(0xffa96322));
        p.line(x-7,y-26,x-5,y-70,5,DARK); p.line(x+8,y-26,x+6,y-70,5,DARK);
        p.line(x-7,y-27,x-5,y-69,3,GOLD); p.line(x+7,y-27,x+5,y-69,3,SceneLighting.sceneryColor(0xffc58925));
        for(int i=0;i<4;i++) {
            float yy=y-30-i*10;
            p.line(x-6,yy,x+6,yy-8,2,GOLD); p.line(x+6,yy,x-6,yy-8,2,SceneLighting.sceneryColor(0xffc68c2e));
        }
        p.rect(x-10,y-75,20,10,DARK); p.rect(x-8,y-76,16,8,GOLD);
        p.rect(x-7,y-76,5,4,LIGHT); p.rect(x+2,y-75,5,5,SceneLighting.sceneryColor(0xff4c8198));
        float boomY=hookY-24;
        p.line(x,y-70,hookX,boomY,7,DARK); p.line(x,y-72,hookX,boomY-2,4,GOLD);
        p.line(x,y-85,hookX,boomY-3,1,LIGHT); p.line(x,y-85,x,y-73,2,DARK);
        p.rect(x-3,y-87,6,4,active&&(tick/6)%2==0?0xffffed94:0xffa85c2d);
        float dx=hookX-x,dy=boomY-(y-70);
        int sections=Math.max(1,(int)(Physics.length(dx,dy)/16));
        for(int i=1;i<sections;i++) {
            float t=i/(float)sections;
            p.line(x+dx*t,y-70+dy*t-3,x+dx*t+4,y-70+dy*t+2,1,SceneLighting.sceneryColor(0xff8b551d));
        }
        p.circle(hookX,boomY,4,DARK); p.circle(hookX,boomY-1,2,GOLD);
        p.line(hookX,boomY+2,hookX,hookY,1.6f,DARK); p.line(hookX+1,boomY+2,hookX+1,hookY,1,LIGHT);
    }
    private static void payload(Painter p,float x,float y,int type) {
        p.line(x-8,y+1,x,y-6,1,LIGHT); p.line(x+8,y+1,x,y-6,1,LIGHT);
        if(type==0) {
            p.rect(x-12,y+1,25,9,DARK);
            for(int i=0;i<3;i++) {
                p.rect(x-10,y+i*3,20,3,SceneLighting.sceneryColor(0xff8197a4)); p.rect(x-10,y+i*3,19,1,SceneLighting.sceneryColor(0xffe7e5d5));
                p.circle(x+10,y+1+i*3,2,DARK); p.circle(x+10,y+1+i*3,1,SceneLighting.sceneryColor(0xff97aab2));
            }
            p.rect(x-5,y,2,9,SceneLighting.sceneryColor(0xffb98039)); p.rect(x+4,y,2,9,SceneLighting.sceneryColor(0xffb98039));
        } else if(type==1) {
            p.rect(x-13,y,26,14,DARK); p.rect(x-12,y-2,24,12,SceneLighting.sceneryColor(0xffb84732));
            p.poly(new float[]{x-12,y-2,x+12,y-2,x+15,y-6,x-9,y-6},SceneLighting.sceneryColor(0xffdc7b4c));
            for(int i=-8;i<11;i+=5) p.rect(x+i,y,2,9,SceneLighting.sceneryColor(0xff7b3f3a));
        } else {
            p.poly(new float[]{x-12,y-3,x+12,y-3,x+8,y+10,x-8,y+10},DARK);
            p.poly(new float[]{x-10,y-3,x+10,y-3,x+6,y+8,x-6,y+8},SceneLighting.sceneryColor(0xffc88a3e));
            p.poly(new float[]{x-9,y-3,x-4,y-8,x+3,y-6,x+9,y-3},SceneLighting.sceneryColor(0xff37434a));
            p.rect(x-2,y-6,3,2,SceneLighting.sceneryColor(0xff84969a));
        }
    }
}
