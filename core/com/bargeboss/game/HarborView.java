package com.bargeboss.game;

/** The concept harbor, built from shaded pixel-sized surfaces in the shared river coordinates. */
public final class HarborView {
    private static final int DARK=VisualStyle.OUTLINE, GOLD=VisualStyle.GOLD;
    private HarborView() {}
    public static void draw(Game g,Painter p) {
        ArcadeView.water(g,p);
        berths(g,p);
    }
    static void berths(Game g,Painter p) {
        for(int i=0;i<3;i++) if(g.berthEnabled(i)) berth(g,p,i);
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
        float x=Game.DOCK_X[index]-55,y=g.dockY(index)-53;
        boolean ready=false,occupied=false,working=false,selected=false;
        for(Body b:g.cargo) if(b!=null && b.active && b.berth==index) {
            ready|=g.canTie(b); occupied|=b.lineAttached && !b.deliveryRecorded; working|=b.unloadingTicks>0 && !b.deliveryRecorded;
            selected|=g.workingCargo()==b;
        }
        int color=occupied?0xff8ce7c2:ready || focused(g)==index?GOLD:(Game.COLORS[index]&0xffffff)|0x88000000;
        p.rect(x,y,110,106,(selected?0x27000000:0x14000000)|(color&0xffffff));
        int run=(g.visualTicks/6)%16;
        for(int k=0;k<110;k+=16) {
            float xx=x+(k+run)%110;
            p.rect(xx,y,Math.min(8,x+110-xx),2,color); p.rect(xx,y+104,Math.min(8,x+110-xx),2,color);
        }
        for(int k=0;k<106;k+=16) {
            float yy=y+(k+run)%106;
            p.rect(x,yy,2,Math.min(8,y+106-yy),color); p.rect(x+108,yy,2,Math.min(8,y+106-yy),color);
        }
        for(int side=0;side<2;side++) for(int end=0;end<2;end++) {
            float xx=x+side*110, yy=y+end*106, sx=side==0?1:-1,sy=end==0?1:-1;
            p.line(xx,yy,xx+sx*15,yy,4,color); p.line(xx,yy,xx,yy+sy*15,4,color);
        }
        RiverPainter.center(p,""+g.berthLetter(index),x+75,y+53,22,0x6656bac1);
        RiverPainter.center(p,Game.CARGO_NAMES[index],x+34,y+53,8,0x99b8d9cf);
        String badge=(working?"UNLOAD ":occupied?"TYING ":"BERTH ")+g.berthLetter(index);
        badge(p,badge,x+95,y+53,8,color);
        float bank=index==1?433:143;
        Painter shore=g.tutorial!=null && p instanceof RiverPainter?
                new RiverPainter(GroundArt.raisedCanvas(g,((RiverPainter)p).canvas()),g.layout.camera):p;
        shore.rect(bank,g.dockY(index)-52,24,3,DARK);
        for(int i=0;i<3;i++) shore.rect(bank+i*8,g.dockY(index)-52,4,3,GOLD);
        if(working) lamp(shore,index==1?429:171,g.dockY(index)+40,g.visualTicks*3);
    }
    private static int focused(Game g) {
        Body held=g.heldCargo(); if(held!=null && !held.lineAttached) return held.berth;
        if(g.selected>=0) { Body b=g.cargo[g.selected]; if(b!=null && b.active && !b.lineAttached && !b.deliveryRecorded) return b.berth; }
        for(Body b:g.cargo) if(b!=null && b.active && !b.lineAttached && !b.deliveryRecorded) return b.berth;
        return -1;
    }
    public static void objective(Game g,Painter canvas) {
        // The early lessons teach a touch target, before the berth destination.
        if(g.tutorial!=null && (g.tutorial.lesson<Tutorial.DOCK || g.tutorial.lesson>=Tutorial.CONNECT)) return;
        int berth=focused(g); if(berth<0 || g.ropeDragging || g.ropePhase!=0) return;
        boolean ready=false; for(Body b:g.cargo) if(b!=null && b.active && b.berth==berth) ready|=g.canTie(b);
        if(ready) return;
        RiverCamera c=g.layout.camera;
        float x=c.screenX(Game.DOCK_X[berth],g.dockY(berth)),y=c.screenY(Game.DOCK_X[berth],g.dockY(berth));
        if(x<32 || x>g.layout.width-32 || y<80 || y>g.layout.height-70) {
            float cx=g.layout.width/2,cy=g.layout.height/2,dx=x-cx,dy=y-cy;
            float fraction=Math.min((g.layout.width/2-32)/Math.max(1,Math.abs(dx)),
                    (g.layout.height/2-80)/Math.max(1,Math.abs(dy)));
            float ex=cx+dx*fraction,ey=cy+dy*fraction,distance=Physics.length(dx,dy);
            float ux=dx/distance,uy=dy/distance;
            ControlView.arrow(canvas,ex-ux*32,ey-uy*32,ex,ey,GOLD);
            Lettering.center(canvas,"BERTH "+g.berthLetter(berth),Physics.clamp(ex,85,g.layout.width-85),
                    Physics.clamp(ey+22,85,g.layout.height-62),12,GOLD);return;
        }
        Painter p=ScreenView.scaled(canvas,x,y,c.scale);
        float bob=3*(float)StrictMath.sin(g.visualTicks*.10);
        p.poly(new float[]{-10,-57+bob,10,-57+bob,10,-38+bob,19,-38+bob,0,-16+bob,-19,-38+bob,-10,-38+bob},DARK);
        p.poly(new float[]{-7,-54+bob,7,-54+bob,7,-35+bob,13,-35+bob,0,-20+bob,-13,-35+bob,-7,-35+bob},GOLD);
        p.line(-5,-52+bob,5,-52+bob,2,VisualStyle.CREAM);
    }
    static void badge(Painter p,String text,float x,float y,float size,int color) {
        float width=Lettering.width(text,size)+10;
        if(p instanceof RiverPainter) {
            RiverPainter river=(RiverPainter)p;RiverCamera c=river.camera;
            Painter canvas=river.canvas();float sx=c.screenX(x,y),sy=c.screenY(x,y),scale=c.scale;
            VisualStyle.panel(canvas,sx-width*scale/2,sy-3*scale,width*scale,(size+6)*scale,VisualStyle.NAVY);
            Lettering.center(canvas,text,sx,sy,size*scale,color);
        } else {
            VisualStyle.panel(p,x-width/2,y-3,width,size+6,VisualStyle.NAVY);
            Lettering.center(p,text,x,y,size,color);
        }
    }

}
