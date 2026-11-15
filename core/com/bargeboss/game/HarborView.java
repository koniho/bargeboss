package com.bargeboss.game;

/** The concept harbor, built from shaded pixel-sized surfaces in the shared river coordinates. */
public final class HarborView {
    private static final int DARK=VisualStyle.OUTLINE, GOLD=VisualStyle.GOLD;
    private HarborView() {}
    public static void draw(Game g,Painter p) {
        berths(g,p);
    }
    static void berths(Game g,Painter p) {
        if(g.sharedBerthStage())emptyHolding(g,p);
        for(int i=0;i<3;i++) if(g.berthDisplayEnabled(i)) berth(g,p,i);
    }
    /** A suggested parking area only: no snapping, mooring or collision changes. */
    private static void emptyHolding(Game g,Painter p) {
        float x=305,y=405,w=80,h=150;
        int color=SoftCue.animated(g,0x6687bbaf);
        for(int side=0;side<2;side++)for(int end=0;end<2;end++){
            float xx=x+side*w,yy=y+end*h,sx=side==0?1:-1,sy=end==0?1:-1;
            SoftCue.line(p,xx,yy,xx+sx*14,yy,1,color);
            SoftCue.line(p,xx,yy,xx,yy+sy*18,1,color);
        }
        badge(p,"EMPTY HOLDING",345,396,7,color);
    }
    static void lamp(Painter p,float x,float y,int tick) {
        int glow=120+(int)(30*StrictMath.sin(tick*.06));
        p.rect(x-4,y-3,8,8,DARK); p.rect(x-2,y-3,4,5,GOLD); p.rect(x-1,y-4,2,2,VisualStyle.CREAM);
        p.rect(x-7,y-6,14,12,(glow/4<<24)|0xffc840);
        int side=x<300?1:-1;
        for(int i=0;i<4;i++) {
            float xx=x+side*(10+i*4),yy=y+5+i*5+(tick/8+i)%3;
            p.rect(xx-2,yy,4+(i%2)*3,2,((80-i*14)<<24)|0xffc840);
        }
    }
    private static void berth(Game g,Painter p,int index) {
        float width=g.dockHalfWidth()*2,x=g.dockX(index)-width*.5f,y=g.dockY(index)-53;
        Body occupant=g.berthBlocker(index,null);
        boolean ready=false,occupied=occupant!=null,empty=occupied && occupant.deliveryRecorded,working=false,selected=false;
        for(Body b:g.cargo) if(b!=null && b.active && (g.sharedBerthStage() || b.berth==index)) {
            ready|=g.canTie(b); working|=b.lineAttached && b.unloadingTicks>0 && !b.deliveryRecorded;
            selected|=g.workingCargo()==b;
        }
        int color=empty?GOLD:occupied?0xff8ce7c2:ready || focused(g)==index?GOLD:(Game.COLORS[index]&0xffffff)|0x88000000;
        p.rect(x,y,width,106,(selected?0x27000000:0x14000000)|(color&0xffffff));
        color=SoftCue.animated(g,color);
        int run=(g.visualTicks/6)%16;
        for(int k=0;k<width;k+=16) {
            float xx=x+(k+run)%width;
            SoftCue.line(p,xx,y,Math.min(xx+8,x+width),y,1,color); SoftCue.line(p,xx,y+104,Math.min(xx+8,x+width),y+104,1,color);
        }
        for(int k=0;k<106;k+=16) {
            float yy=y+(k+run)%106;
            SoftCue.line(p,x,yy,x,Math.min(yy+8,y+106),1,color); SoftCue.line(p,x+width,yy,x+width,Math.min(yy+8,y+106),1,color);
        }
        for(int side=0;side<2;side++) for(int end=0;end<2;end++) {
            float xx=x+side*width, yy=y+end*106, sx=side==0?1:-1,sy=end==0?1:-1;
            SoftCue.line(p,xx,yy,xx+sx*15,yy,1,color); SoftCue.line(p,xx,yy,xx,yy+sy*15,1,color);
        }
        SoftCue.caption(p,g.sharedBerthStage()?"SLIP":""+g.berthLetter(index),x+width*.68f,y+53,g.sharedBerthStage()?12:22,0x6656bac1);
        if(!g.orderStage())SoftCue.caption(p,Game.CARGO_NAMES[index],x+34,y+53,8,0x99b8d9cf);
        String role=g.depthStage() && g.nextRequiredBerth()==index?"DEEP FIRST ":"BERTH ";
        String name=g.sharedBerthStage()?"SLIP":""+g.berthLetter(index);
        String badge=empty?"EMPTY":(working?"UNLOAD ":occupied?"OCCUPIED ":role)+name;
        // Keep narrow-slip labels beyond the hull footprint instead of beneath the empty vessel.
        badge(p,badge,g.orderStage()?g.dockX(index):x+width*.86f,g.orderStage()?y-14:y+53,8,color);
        boolean right=g.berthRight(index);float bank=right?433:143;
        Painter shore=g.tutorial!=null && p instanceof RiverPainter?
                new RiverPainter(GroundArt.raisedCanvas(g,((RiverPainter)p).canvas()),g.layout.camera):p;
        shore.rect(bank,g.dockY(index)-52,24,3,DARK);
        for(int i=0;i<3;i++) shore.rect(bank+i*8,g.dockY(index)-52,4,3,GOLD);
        if(working) lamp(shore,right?429:171,g.dockY(index)+40,g.visualTicks*3);
    }
    private static int focused(Game g) {
        if(g.sharedBerthStage())return 2;
        if(g.depthStage())return g.nextRequiredBerth();
        Body held=g.heldCargo(); if(held!=null && !held.lineAttached && !held.deliveryRecorded) return held.berth;
        if(g.selected>=0) { Body b=g.cargo[g.selected]; if(b!=null && b.active && !b.lineAttached && !b.deliveryRecorded) return b.berth; }
        for(Body b:g.cargo) if(b!=null && b.active && !b.lineAttached && !b.deliveryRecorded) return b.berth;
        return -1;
    }
    public static void objective(Game g,Painter canvas) {
        if(g.lockStage()) {
            if(g.ropeDragging || g.ropePhase!=0)return;
            if(g.lock.phase==Lock.READY)destination(g,canvas,300,Lock.ENTRY_Y+85,"LOCK ENTRY");
            else if(g.lock.phase==Lock.ENTRY)destination(g,canvas,300,(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,"INTO LOCK");
            else if(g.lock.phase==Lock.EXIT)destination(g,canvas,300,Lock.EXIT_Y-120,"LOCK EXIT");
            return;
        }
        // The early lessons teach a touch target, before the berth destination.
        if(g.tutorial!=null && (g.tutorial.lesson<Tutorial.DOCK || g.tutorial.lesson>=Tutorial.CONNECT)) return;
        int berth=focused(g); if(berth<0 || g.ropeDragging || g.ropePhase!=0) return;
        Body blocker=g.berthBlocker(berth,null);
        if(blocker!=null && blocker.deliveryRecorded){destination(g,canvas,blocker.x,blocker.y,"TOW EMPTY CLEAR");return;}
        boolean ready=false; for(Body b:g.cargo) if(b!=null && b.active && (g.sharedBerthStage() || b.berth==berth)) ready|=g.canTie(b);
        if(ready) return;
        destination(g,canvas,g.dockX(berth),g.dockY(berth),g.sharedBerthStage()?"UNLOADING SLIP":"BERTH "+g.berthLetter(berth));
    }
    private static void destination(Game g,Painter canvas,float wx,float wy,String label) {
        RiverCamera c=g.layout.camera;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy);
        if(x<32 || x>g.layout.width-32 || y<80 || y>g.layout.height-70) {
            float cx=g.layout.width/2,cy=g.layout.height/2,dx=x-cx,dy=y-cy;
            float fraction=Math.min((g.layout.width/2-32)/Math.max(1,Math.abs(dx)),
                    (g.layout.height/2-80)/Math.max(1,Math.abs(dy)));
            float ex=cx+dx*fraction,ey=cy+dy*fraction,distance=Physics.length(dx,dy);
            float ux=dx/distance,uy=dy/distance;
            ControlView.arrow(g,canvas,ex-ux*32,ey-uy*32,ex,ey,GOLD);
            Lettering.center(canvas,label,Physics.clamp(ex,85,g.layout.width-85),
                    Physics.clamp(ey+22,85,g.layout.height-62),12,GOLD);return;
        }
        Painter p=ScreenView.scaled(canvas,x,y,c.scale);
        float bob=3*(float)StrictMath.sin(g.visualTicks*.10);
        ControlView.arrow(g,p,0,-54+bob,0,-22+bob,GOLD);
    }
    static void badge(Painter p,String text,float x,float y,float size,int color) {
        float width=Lettering.width(text,size)+10;
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;
            Painter canvas=river.canvas();float sx=c.screenX(x,y),sy=c.screenY(x,y),scale=c.scale;
            canvas.rect(sx-width*scale/2,sy-3*scale,width*scale,(size+6)*scale,0x40101c30);
            Lettering.center(canvas,text,sx,sy,size*scale,color);
        } else {
            p.rect(x-width/2,y-3,width,size+6,0x40101c30);
            Lettering.center(p,text,x,y,size,color);
        }
    }

}
