package com.bargeboss.game;

/** Aim with a coiled rope; release launches a loop that catches or reels back to the barge. */
public final class MooringView {
    private static final int ROPE=RopeMaterial.COLOR, HIGHLIGHT=0xffffe3a1,DARK=VisualStyle.OUTLINE,GREEN=0xff92ecc2;
    private MooringView() {}
    public static void draw(Game g,Painter p) {
        int tick=g.visualTicks;
        for(int berth=0;berth<3;berth++) {
            if(g.montroseStage() || !g.berthEnabled(berth)) continue;
            float x=g.bollardX(berth),y=g.bollardY(berth);
            boolean ready=false,tied=false;
            for(Body b:g.cargo) if(b!=null && b.active && b.berth==berth) {
                ready|=g.canTie(b); tied|=b.lineAttached;
            }
            if(reachableBollard(g,berth)) bollardGlow(g,p,x,y);
            bollard(g,p,x,y);
            if(ready) {
                float pulse=2*(float)StrictMath.sin(tick*.11);
                ControlView.ring(g,p,x,y,Game.TIE_RADIUS,1,GREEN,1.5f);
                float top=y-48+3*(float)StrictMath.sin(tick*.11);
                ControlView.arrow(g,p,x,top,x,top+18,VisualStyle.GOLD);
                SoftCue.caption(p,"TIE",x+(berth==1?-12:12),y-38,8,GREEN);
            } else if(tied) {
                p.rect(x-3,y-11,6,2,GREEN);
                ControlView.ring(g,p,x,y,17,1,0x6688e2bb,1);
            }
        }
        for(Body b:g.cargo) if(b!=null && b.active) {
            if((g.ropeDragging || g.towAiming) && g.heldCargo()==b) {
                aim(g,p,b); continue;
            }
            if(!b.lineAttached) {
                if(g.canTie(b) && g.ropeBody!=b) {
                    if(b==readyFocus(g))readiness(g,p,b);
                }
                continue;
            }
            float x=g.bollardX(b.berth),y=g.bollardY(b.berth);
            float[] edge=new float[2];RopeAttachment.mooring(b,x,y,edge);
            float deck=RopeAttachment.height(b)+WaterSurface.lift(g,b);
            float originX=edge[0]+liftX(p,deck),originY=edge[1]+liftY(p,deck);
            float progress=Math.min(1,b.dockTime/1.2f);
            // Successful throws are taut immediately; only the impact vibration remains.
            float slack=23*(1-progress*progress*progress);
            if(b.tieBurstTicks>0) catchBurst(p,b);
            if(b.lineSnapTicks>0) {
                float snap=Physics.clamp(1-b.lineSnapTicks/18f,0,1);
                float shake=4.5f*(1-snap)*(1-snap);
                if(!(p instanceof RiverPainter))rope(p,originX,originY,x,y,0,ROPE,shake,snap);
                snapRings(p,originX,originY,x,y,snap);
            } else if(!(p instanceof RiverPainter))rope(p,originX,originY,x,y,slack,ROPE);
            float rx=17-8*progress,ry=14-7*progress;
            lasso(p,x,y,rx,ry,0,ROPE);
            if(progress>.75f) {
                p.line(x-6,y+3,x+6,y-3,3,ROPE); p.line(x-5,y-3,x+6,y+3,2,HIGHLIGHT);
                p.line(x+5,y+2,x+10,y+10,2,ROPE);
            }
            if(progress<1) ControlView.ring(g,p,b.x,b.y,42,progress,GREEN,3);
            if(Mooring.aligning(b)) {
                float t=(b.mooringTicks-Mooring.TIGHTEN_TICKS)/(float)Mooring.ALIGN_TICKS;
                ControlView.ring(g,p,b.x,b.y,43,t,GREEN,2);
            }
        }
        if(g.ropeBody!=null && g.ropeBody.active) {
            if(g.ropePhase==1 || g.ropePhase==2) {if(!(p instanceof RiverPainter))projectile(g,p);}
            else if(g.ropePhase==3) hit(g,p);
        }
    }
    private static void catchBurst(Painter p,Body b) {
        float t=1-b.tieBurstTicks/30f;int alpha=(int)(235*(1-t)*(1-t));
        float radius=b.half+b.radius+7+t*65;
        ControlView.ring(p,b.x,b.y,radius,1,(alpha<<24)|0x9bffe0,4*(1-t)+1);
        ControlView.ring(p,b.x,b.y,radius-9,1,((alpha/2)<<24)|0xffe5a8,2);
        if(t<.25f) p.circle(b.x,b.y,b.half+b.radius,((int)(65*(1-t*4))<<24)|0xb8ffe2);
    }
    private static Body readyFocus(Game g) {
        Body held=g.heldCargo();if(g.canTie(held))return held;
        Body nearest=null;float distance=Float.MAX_VALUE;
        for(Body b:g.cargo)if(g.canTie(b)) {float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<distance){nearest=b;distance=d;}}
        return nearest;
    }
    private static void readiness(Game g,Painter p,Body b) {
        if(!(p instanceof RiverPainter)) return;
        RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;Painter canvas=river.canvas();
        float bx=c.screenX(b.x,b.y),by=c.screenY(b.x,b.y);
        if(!c.contains(bx,by)) return;
        float tx=c.screenX(g.bollardX(b.berth),g.bollardY(b.berth)),ty=c.screenY(g.bollardX(b.berth),g.bollardY(b.berth));
        float dx=tx-bx,dy=ty-by,distance=Math.max(1,Physics.length(dx,dy));dx/=distance;dy/=distance;
        for(int i=0;i<4;i++) {
            float t=((g.visualTicks%60)/60f+i*.25f)%1;
            float along=38*c.scale+t*Math.max(0,distance-55*c.scale);
            float x=bx+dx*along,y=by+dy*along;
            SoftCue.line(canvas,x-dx*5-dy*4,y-dy*5+dx*4,x,y,1,SoftCue.animated(g,GREEN));
            SoftCue.line(canvas,x-dx*5+dy*4,y-dy*5-dx*4,x,y,1,SoftCue.animated(g,GREEN));
        }

    }
    /** Show only a target the held hull can actually catch, independent of aim direction. */
    static boolean reachableBollard(Game g,int berth) {
        Body held=g.heldCargo();
        return g.pointer && (g.ropeDragging || g.towAiming) && g.ropePhase==0
                && held!=null && held.berth==berth && g.ropeWithinReach(held);
    }
    private static void bollardGlow(Game g,Painter p,float x,float y) {
        float scale=1;
        if(p instanceof RiverPainter){RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;
            float sx=c.screenX(x,y);y=c.screenY(x,y);x=sx;
            scale=c.scale;p=river.canvas();}
        SoftCue.dot(p,x,y-5*scale,10*scale,SoftCue.animated(g,0x996be3bf));
    }
    private static void bollard(Game g,Painter p,float x,float y) {
        // Use the same weathered iron artwork as the shore scenery. Coordinates
        // already include quay rise: preserve the gameplay/lasso attachment point.
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p; RiverCamera c=river.camera;
            river.canvas().sprite(SpriteAtlas.SHORE_BOLLARDS,1,c.screenX(x,y),c.screenY(x,y),
                    28*c.scale,28*c.scale,0,1);
        } else p.sprite(SpriteAtlas.SHORE_BOLLARDS,1,x,y,28,28,0,1);
    }

    private static void aim(Game g,Painter p,Body b) {
        boolean tow=g.towAiming;
        Body target=tow?g.towTargetBody(b,g.pointerX,g.pointerY):null;
        boolean onPoint=tow?target!=null:g.canTie(b) && g.overTiePoint(b,g.pointerX,g.pointerY);
        float x=tow?(onPoint?target.x:g.pointerX):g.aimX(b,g.pointerX,g.pointerY);
        float y=tow?(onPoint?target.y:g.pointerY):g.aimY(b,g.pointerX,g.pointerY);
        int color=onPoint?GREEN:VisualStyle.GOLD;
        // This thin continuous trajectory is a guide; the rope remains coiled on deck.
        float[] edge=new float[2],point=new float[2];RopeAttachment.cast(b,x,y,tow,edge);
        float deck=RopeAttachment.height(b)+WaterSurface.lift(g,b);boolean river=p instanceof RiverPainter;
        LassoPath.sample(edge[0],edge[1],x,y,deck,0,river,point);
        float originX=point[0],originY=point[1],lastX=originX,lastY=originY;
        for(int i=1;i<=32;i++) {
            float t=i/32f;
            LassoPath.sample(edge[0],edge[1],x,y,deck,t,river,point);
            float xx=point[0],yy=point[1];
            SoftCue.line(p,lastX,lastY,xx,yy,1.25f,SoftCue.animated(g,color));
            lastX=xx; lastY=yy;
        }
        for(int i=0;i<4;i++) {
            float t=((g.visualTicks%48)/48f+i*.25f)%1;
            LassoPath.sample(edge[0],edge[1],x,y,deck,t,river,point);
            float xx=point[0],yy=point[1];
            SoftCue.dot(p,xx,yy,2,SoftCue.animated(g,onPoint?HIGHLIGHT:color));
        }
        float radius=onPoint?14:11;
        for(int sx=-1;sx<=1;sx+=2) for(int sy=-1;sy<=1;sy+=2) {
            SoftCue.line(p,x+sx*radius,y+sy*radius,x+sx*(radius-5),y+sy*radius,1,SoftCue.animated(g,color));
            SoftCue.line(p,x+sx*radius,y+sy*radius,x+sx*radius,y+sy*(radius-5),1,SoftCue.animated(g,color));
        }
        SoftCue.dot(p,x,y,2,SoftCue.animated(g,color));
        // The rope stays visibly coiled on deck until the finger lifts.
        for(int i=0;i<3;i++) {
            lasso(p,originX,originY-i*2,8-i,5-i*.5f,0,ROPE);
        }
    }
    private static float liftX(Painter p,float height) { return p instanceof RiverPainter?RiverCamera.riseX(height):0; }
    private static float liftY(Painter p,float height) { return p instanceof RiverPainter?RiverCamera.riseY(height):-height; }
    private static void projectile(Game g,Painter p) {
        boolean returning=g.ropePhase==2,river=p instanceof RiverPainter;
        float t=Physics.clamp(g.ropeTicks/(float)(returning?Game.ROPE_RETURN_TICKS:Game.ROPE_FLIGHT_TICKS),0,1);
        float eased=1-(1-t)*(1-t),distance=returning?1-eased:eased;
        float currentX=g.ropeOriginX(),currentY=g.ropeOriginY();
        float currentHeight=RopeAttachment.height(g.ropeBody)+WaterSurface.lift(g,g.ropeBody);
        float startX=returning?currentX:g.ropeStartX,startY=returning?currentY:g.ropeStartY;
        float height=returning?currentHeight:g.ropeStartHeight;
        float[] point=new float[2];
        LassoPath.sample(startX,startY,g.ropeEndX,g.ropeEndY,height,distance,river,point);
        float x=point[0],y=point[1];
        // Pay out the very same arc as the guide, not a sagging straight chord.
        // The tether root follows its local deck fitting; the flying tip keeps
        // the released trajectory even if the barge translates or turns.
        float offsetX=currentX-startX+liftX(p,currentHeight-height);
        float offsetY=currentY-startY+liftY(p,currentHeight-height);
        float lastX=currentX+liftX(p,currentHeight),lastY=currentY+liftY(p,currentHeight),strand=0;
        for(int i=1;i<=32;i++) {
            float fraction=i/32f;
            LassoPath.sample(startX,startY,g.ropeEndX,g.ropeEndY,height,distance*fraction,river,point);
            float xx=point[0]+offsetX*(1-fraction),yy=point[1]+offsetY*(1-fraction);
            float next=strand+Physics.length(xx-lastX,yy-lastY)/5f;
            p.ropeSegment(lastX,lastY,xx,yy,2.8f,strand,next,ROPE);strand=next;
            lastX=xx;lastY=yy;
        }
        float radius=returning?8+14*distance:8+14*eased;
        lasso(p,x,y,radius,radius*.72f,.3f*(float)StrictMath.sin(g.ropeTicks*.34),ROPE);
        if(t>.08f && t<.85f) {
            float behind=Physics.clamp(distance+(returning?.07f:-.07f),0,1);
            LassoPath.sample(startX,startY,g.ropeEndX,g.ropeEndY,height,behind,river,point);
            float dx=x-point[0],dy=y-point[1],length=Math.max(1,Physics.length(dx,dy));
            p.line(x-dx/length*(radius+4),y-dy/length*(radius+4),
                    x-dx/length*(radius+12),y-dy/length*(radius+12),1.5f,0xaae8c581);
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
        float normalX=-dy/len,normalY=dx/len,lastX=startX,lastY=startY,strand=0;
        for(int i=1;i<=24;i++) {
            float t=i/24f;
            float quiver=shake*(float)StrictMath.sin(t*Math.PI)*(float)StrictMath.sin(t*Math.PI*4-snapTime*20);
            float xx=startX+dx*t+normalX*quiver,yy=startY+dy*t+4*t*(1-t)*slack+normalY*quiver;
            float next=strand+Physics.length(xx-lastX,yy-lastY)/5f;
            p.ropeSegment(lastX,lastY,xx,yy,2.8f,strand,next,color);strand=next;
            if(snapTime>=0 && Math.abs(t-(1-snapTime*1.4f))<.14f) {
                int alpha=(int)(240*(1-snapTime));
                p.line(lastX,lastY,xx,yy,7,((alpha/3)<<24)|0xffe8aa);
                p.line(lastX,lastY,xx,yy,3,(alpha<<24)|0xfff7df);
            }
            lastX=xx; lastY=yy;
        }
    }
    static void lasso(Painter p,float x,float y,float rx,float ry,float rotation,int color) {
        float c=(float)StrictMath.cos(rotation),s=(float)StrictMath.sin(rotation),strand=0;
        for(int i=0;i<24;i++) {
            float a=(float)(i*Math.PI*2/24),b=(float)((i+1)*Math.PI*2/24);
            float ax=rx*(float)StrictMath.cos(a),ay=ry*(float)StrictMath.sin(a),bx=rx*(float)StrictMath.cos(b),by=ry*(float)StrictMath.sin(b);
            float next=strand+Physics.length(bx-ax,by-ay)/5f;
            p.ropeSegment(x+ax*c-ay*s,y+ax*s+ay*c,x+bx*c-by*s,y+bx*s+by*c,2.8f,strand,next,color);strand=next;
        }
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
