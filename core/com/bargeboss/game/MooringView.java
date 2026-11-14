package com.bargeboss.game;

/** Aim with a coiled rope; release launches a loop that catches or reels back to the barge. */
public final class MooringView {
    private static final int ROPE=0xffd3a65f, HIGHLIGHT=0xffffe3a1,DARK=VisualStyle.OUTLINE,GREEN=0xff92ecc2;
    private MooringView() {}
    public static void draw(Game g,Painter p) {
        int tick=g.visualTicks;
        for(int berth=0;berth<3;berth++) {
            if(!g.berthEnabled(berth)) continue;
            float x=Game.TIE_X[berth],y=g.dockY(berth);
            boolean ready=false,tied=false;
            for(Body b:g.cargo) if(b!=null && b.active && b.berth==berth) {
                ready|=g.canTie(b); tied|=b.lineAttached;
            }
            bollard(g,p,x,y);
            if(ready) {
                float pulse=2*(float)StrictMath.sin(tick*.11);
                ControlView.ring(p,x,y,Game.TIE_RADIUS+pulse,1,0x665feac1,4);
                ControlView.ring(p,x,y,Game.TIE_RADIUS,1,GREEN,1.5f);
                float top=y-48+3*(float)StrictMath.sin(tick*.11);
                p.poly(new float[]{x-4,top,x+4,top,x+4,top+9,x+9,top+9,x,top+18,x-9,top+9,x-4,top+9},DARK);
                p.poly(new float[]{x-2,top+2,x+2,top+2,x+2,top+11,x+5,top+11,x,top+15,x-5,top+11,x-2,top+11},VisualStyle.GOLD);
                RiverPainter.center(p,"TIE",x+(berth==1?-12:12),y-38,8,GREEN);
            } else if(tied) {
                p.rect(x-3,y-11,6,2,GREEN);
                ControlView.ring(p,x,y,17,1,0x6688e2bb,1);
            }
        }
        for(Body b:g.cargo) if(b!=null && b.active) {
            if((g.ropeDragging || g.towAiming) && g.heldCargo()==b) {
                aim(g,p,b); continue;
            }
            if(!b.lineAttached) {
                if(g.canTie(b) && g.ropeBody!=b) {
                    float pulse=1+(float)StrictMath.sin(tick*.09);
                    ControlView.ring(p,b.x,b.y,40+pulse,1,GREEN,3);
                    readiness(g,p,b);
                }
                continue;
            }
            float x=Game.TIE_X[b.berth],y=g.dockY(b.berth);
            float progress=Math.min(1,b.dockTime/1.2f);
            // Successful throws are taut immediately; only the impact vibration remains.
            float slack=23*(1-progress*progress*progress);
            if(b.tieBurstTicks>0) catchBurst(p,b);
            if(b.lineSnapTicks>0) {
                float snap=Physics.clamp(1-b.lineSnapTicks/18f,0,1);
                float shake=4.5f*(1-snap)*(1-snap);
                rope(p,b.x,b.y,x,y,0,ROPE,shake,snap);
                snapRings(p,b.x,b.y,x,y,snap);
            } else rope(p,b.x,b.y,x,y,slack,ROPE);
            float rx=17-8*progress,ry=14-7*progress;
            lasso(p,x,y,rx,ry,0,ROPE);
            if(progress>.75f) {
                p.line(x-6,y+3,x+6,y-3,3,ROPE); p.line(x-5,y-3,x+6,y+3,2,HIGHLIGHT);
                p.line(x+5,y+2,x+10,y+10,2,ROPE);
            }
            if(progress<1) ControlView.ring(p,b.x,b.y,42,progress,GREEN,3);
            if(Mooring.aligning(b)) {
                float t=(b.mooringTicks-Mooring.TIGHTEN_TICKS)/(float)Mooring.ALIGN_TICKS;
                ControlView.ring(p,b.x,b.y,43,t,GREEN,2);
            }
        }
        if(g.ropeBody!=null && g.ropeBody.active) {
            if(g.ropePhase==1 || g.ropePhase==2) projectile(g,p);
            else if(g.ropePhase==3) hit(g,p);
        }
    }
    public static void glow(Game g,Painter p,Body b) {
        if(!g.canTie(b) || g.ropeDragging) return;
        float pulse=.5f+.5f*(float)StrictMath.sin(g.visualTicks*.10);
        float dx=(float)StrictMath.cos(b.angle)*b.half,dy=(float)StrictMath.sin(b.angle)*b.half;
        for(int layer=5;layer>=1;layer--) p.line(b.x-dx,b.y-dy,b.x+dx,b.y+dy,
                2*b.radius+layer*(3+pulse),((int)(12+8*pulse)<<24)|0x65ffb6);
        ControlView.ring(p,b.x,b.y,b.half+b.radius+5+pulse*3,1,0xbb92ffd0,2.5f);
    }
    private static void catchBurst(Painter p,Body b) {
        float t=1-b.tieBurstTicks/30f;int alpha=(int)(235*(1-t)*(1-t));
        float radius=b.half+b.radius+7+t*65;
        ControlView.ring(p,b.x,b.y,radius,1,(alpha<<24)|0x9bffe0,4*(1-t)+1);
        ControlView.ring(p,b.x,b.y,radius-9,1,((alpha/2)<<24)|0xffe5a8,2);
        if(t<.25f) p.circle(b.x,b.y,b.half+b.radius,((int)(65*(1-t*4))<<24)|0xb8ffe2);
    }
    private static void readiness(Game g,Painter p,Body b) {
        if(!(p instanceof RiverPainter)) return;
        RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;Painter canvas=river.canvas();
        float bx=c.screenX(b.x,b.y),by=c.screenY(b.x,b.y);
        if(!c.contains(bx,by)) return;
        float tx=c.screenX(Game.TIE_X[b.berth],g.dockY(b.berth)),ty=c.screenY(Game.TIE_X[b.berth],g.dockY(b.berth));
        float dx=tx-bx,dy=ty-by,distance=Math.max(1,Physics.length(dx,dy));dx/=distance;dy/=distance;
        for(int i=0;i<4;i++) {
            float t=((g.visualTicks%60)/60f+i*.25f)%1;
            float along=38*c.scale+t*Math.max(0,distance-55*c.scale);
            float x=bx+dx*along,y=by+dy*along;
            canvas.line(x-dx*5-dy*4,y-dy*5+dx*4,x,y,2,GREEN);
            canvas.line(x-dx*5+dy*4,y-dy*5-dx*4,x,y,2,GREEN);
        }
        float x=Physics.clamp(bx,110,g.layout.width-110),y=Math.max(82,by-62*c.scale);
        VisualStyle.panel(canvas,x-101,y-5,202,45,0xf0102b35);
        canvas.line(x-92,y+9,x-87,y+15,3,GREEN);canvas.line(x-87,y+15,x-77,y+2,3,GREEN);
        Lettering.center(canvas,"READY TO TIE",x+8,y+2,13,GREEN);
        Lettering.center(canvas,g.pushing?"LIFT, THEN DRAG TO BOLLARD":"DRAG TO BOLLARD / RELEASE",x,y+24,8,VisualStyle.CREAM);
    }
    private static void bollard(Game g,Painter p,float x,float y) {
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p; RiverCamera c=river.camera;
            p=ScreenView.scaled(river.canvas(),c.screenX(x,y),c.screenY(x,y),c.scale*.8f); x=0;y=0;
        }
        p.poly(new float[]{x-12,y+4,x-7,y-2,x+9,y-2,x+14,y+6,x+8,y+12,x-8,y+12},DARK);
        p.poly(new float[]{x-9,y+3,x-5,y-1,x+7,y-1,x+10,y+5,x+6,y+9,x-6,y+9},0xff6a7381);
        p.rect(x-5,y-9,11,14,DARK); p.rect(x-4,y-9,8,12,0xffbc7a27);
        p.rect(x-4,y-9,3,11,VisualStyle.GOLD); p.rect(x-7,y-12,14,6,DARK);
        p.rect(x-6,y-12,12,4,VisualStyle.GOLD); p.rect(x-4,y-12,8,1,HIGHLIGHT);
        p.rect(x-9,y-7,18,3,DARK); p.rect(x-8,y-7,16,1,HIGHLIGHT);
    }
    private static void aim(Game g,Painter p,Body b) {
        boolean tow=g.towAiming;
        boolean onPoint=tow?g.towTarget(b,g.pointerX,g.pointerY):g.canTie(b) && g.overTiePoint(b,g.pointerX,g.pointerY);
        float x=tow?(onPoint?g.tug.x:g.pointerX):g.aimX(b,g.pointerX,g.pointerY);
        float y=tow?(onPoint?g.tug.y:g.pointerY):g.aimY(b,g.pointerX,g.pointerY);
        int color=onPoint?GREEN:VisualStyle.GOLD;
        // This thin continuous trajectory is a guide; the rope remains coiled on deck.
        float lastX=b.x,lastY=b.y;
        for(int i=1;i<=32;i++) {
            float t=i/32f,xx=b.x+(x-b.x)*t,yy=b.y+(y-b.y)*t-30*(float)StrictMath.sin(t*Math.PI);
            p.line(lastX,lastY,xx,yy,3,0x990b1424);
            p.line(lastX,lastY,xx,yy,1.25f,(color&0xffffff)|0xcc000000);
            lastX=xx; lastY=yy;
        }
        for(int i=0;i<4;i++) {
            float t=((g.visualTicks%48)/48f+i*.25f)%1;
            float xx=b.x+(x-b.x)*t,yy=b.y+(y-b.y)*t-30*(float)StrictMath.sin(t*Math.PI);
            p.rect(xx-2,yy-2,4,4,DARK); p.rect(xx-1,yy-1,3,3,onPoint?HIGHLIGHT:color);
        }
        float radius=onPoint?14:11;
        for(int sx=-1;sx<=1;sx+=2) for(int sy=-1;sy<=1;sy+=2) {
            p.line(x+sx*radius,y+sy*radius,x+sx*(radius-5),y+sy*radius,2,color);
            p.line(x+sx*radius,y+sy*radius,x+sx*radius,y+sy*(radius-5),2,color);
        }
        p.rect(x-1,y-1,3,3,color);
        // The rope stays visibly coiled on deck until the finger lifts.
        for(int i=0;i<3;i++) {
            loop(p,b.x+1,b.y+5-i*2,10-i,6-i*.5f,0,DARK,4);
            loop(p,b.x,b.y+3-i*2,10-i,6-i*.5f,0,i==1?HIGHLIGHT:ROPE,2);
        }
    }
    private static void projectile(Game g,Painter p) {
        boolean returning=g.ropePhase==2;
        float t=Physics.clamp(g.ropeTicks/(float)(returning?Game.ROPE_RETURN_TICKS:Game.ROPE_FLIGHT_TICKS),0,1);
        float eased=1-(1-t)*(1-t),distance=returning?1-eased:eased;
        float startX=returning?g.ropeBody.x:g.ropeStartX,startY=returning?g.ropeBody.y:g.ropeStartY;
        float x=startX+(g.ropeEndX-startX)*distance;
        float y=startY+(g.ropeEndY-startY)*distance-30*(float)StrictMath.sin(distance*Math.PI);
        float slack=(returning?10:18)*(float)StrictMath.sin(distance*Math.PI);
        rope(p,g.ropeBody.x,g.ropeBody.y,x,y,slack,ROPE);
        float radius=returning?8+14*distance:8+14*eased;
        lasso(p,x,y,radius,radius*.72f,.3f*(float)StrictMath.sin(g.ropeTicks*.34),ROPE);
        // Short fading twine streaks emphasize launch and the recoil of a miss.
        if(t>.08f && t<.85f) {
            float dx=g.ropeEndX-startX,dy=g.ropeEndY-startY,len=Math.max(1,Physics.length(dx,dy));
            float sign=returning?-1:1;
            p.line(x-sign*dx/len*(radius+4),y-sign*dy/len*(radius+4),
                    x-sign*dx/len*(radius+12),y-sign*dy/len*(radius+12),1.5f,0xaae8c581);
        }
    }
    private static void hit(Game g,Painter p) {
        float t=Physics.clamp(g.ropeTicks/(float)Game.ROPE_HIT_TICKS,0,1);
        float x=g.ropeEndX,y=g.ropeEndY,radius=10+32*t;
        int alpha=(int)(220*(1-t));
        ControlView.ring(p,x,y,radius,1,(alpha<<24)|0x9bffe0,3*(1-t)+1);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            float xx=x+(float)StrictMath.cos(a)*radius,yy=y+(float)StrictMath.sin(a)*radius;
            float arm=4*(1-t)+1;
            int color=(alpha<<24)|(i%2==0?0xffe38d:0xc5fff0);
            p.line(xx-arm,yy,xx+arm,yy,2,color); p.line(xx,yy-arm,xx,yy+arm,2,color);
        }
        if(t<.5f) {
            float size=14*(1-t*2);
            p.poly(new float[]{x,y-size,x+3,y-3,x+size,y,x+3,y+3,x,y+size,x-3,y+3,x-size,y,x-3,y-3},HIGHLIGHT);
        }
    }
    private static void snapRings(Painter p,float startX,float startY,float endX,float endY,float t) {
        int alpha=(int)(200*(1-t)*(1-t));
        float radius=5+17*t;
        ControlView.ring(p,startX,startY,radius,1,(alpha<<24)|0xffe5a8,2*(1-t)+1);
        ControlView.ring(p,endX,endY,radius+3,1,(alpha<<24)|0xa1ffe3,2*(1-t)+1);
        if(t<.35f) {
            float size=4*(1-t/.35f);
            p.line(endX-size,endY-size,endX+size,endY+size,2,HIGHLIGHT);
            p.line(endX-size,endY+size,endX+size,endY-size,2,HIGHLIGHT);
        }
    }
    static void rope(Painter p,float startX,float startY,float x,float y,float slack,int color) {
        rope(p,startX,startY,x,y,slack,color,0,-1);
    }
    private static void rope(Painter p,float startX,float startY,float x,float y,float slack,int color,float shake,float snapTime) {
        float dx=x-startX,dy=y-startY,len=Math.max(1,Physics.length(dx,dy));
        float normalX=-dy/len,normalY=dx/len,lastX=startX,lastY=startY;
        for(int i=1;i<=24;i++) {
            float t=i/24f;
            float quiver=shake*(float)StrictMath.sin(t*Math.PI)*(float)StrictMath.sin(t*Math.PI*4-snapTime*20);
            float xx=startX+dx*t+normalX*quiver,yy=startY+dy*t+4*t*(1-t)*slack+normalY*quiver;
            p.line(lastX+1,lastY+2,xx+1,yy+2,4.5f,DARK);
            p.line(lastX,lastY,xx,yy,3,color);
            if(i%2==0) p.line(lastX,lastY-1,xx,yy-1,1,HIGHLIGHT);
            if(snapTime>=0 && Math.abs(t-(1-snapTime*1.4f))<.14f) {
                int alpha=(int)(240*(1-snapTime));
                p.line(lastX,lastY,xx,yy,7,((alpha/3)<<24)|0xffe8aa);
                p.line(lastX,lastY,xx,yy,3,(alpha<<24)|0xfff7df);
            }
            lastX=xx; lastY=yy;
        }
    }
    private static void lasso(Painter p,float x,float y,float rx,float ry,float rotation,int color) {
        loop(p,x+1,y+2,rx,ry,rotation,DARK,5);
        loop(p,x,y,rx,ry,rotation,color,3);
        loop(p,x,y-1,rx-1,ry-1,rotation,HIGHLIGHT,1);
    }
    private static void loop(Painter p,float x,float y,float rx,float ry,float rotation,int color,float width) {
        float c=(float)StrictMath.cos(rotation),s=(float)StrictMath.sin(rotation);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI*2/24,b=(i+1)*Math.PI*2/24;
            float ax=rx*(float)StrictMath.cos(a),ay=ry*(float)StrictMath.sin(a);
            float bx=rx*(float)StrictMath.cos(b),by=ry*(float)StrictMath.sin(b);
            p.line(x+ax*c-ay*s,y+ax*s+ay*c,x+bx*c-by*s,y+bx*s+by*c,width,color);
        }
    }
}
