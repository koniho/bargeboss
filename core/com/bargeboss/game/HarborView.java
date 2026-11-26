package com.bargeboss.game;

/** The concept harbor, built from shaded pixel-sized surfaces in the shared river coordinates. */
public final class HarborView {
    private static final int DARK=VisualStyle.OUTLINE, GOLD=VisualStyle.GOLD;
    private HarborView() {}
    public static void draw(Game g,Painter p) {
        if(g.calumetStage()){for(int i=0;i<3;i++)if(g.berthDisplayEnabled(i))CalumetRiverView.berth(g,p,i);return;}
        if(g.indianaStage()){IndianaView.berth(g,p);return;}
        if(g.chicagoStage())return;
        if(g.montroseStage()){MontroseView.slip(g,p);return;}
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
        boolean right=g.berthRight(index);float bank=g.orderStage()?PhysicalBerths.LEFT-27:right?433:143;
        Painter shore=!g.montroseStage() && p instanceof RiverPainter?
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
        if(g.calumetStage()){
            if(g.voyage.job==2){destination(g,canvas,g.voyage.gateX(),g.voyage.gateY(),"TOW TWO EMPTIES / 106TH STREET");return;}
            Body target=null;for(Body b:g.cargo)if(b!=null && b.active && !b.deliveryRecorded && b.voyageJob==g.voyage.job){target=b;break;}
            if(target==null || g.ropeDragging || g.ropePhase!=0)return;
            Body blocker=g.voyage.blocker(g,target.destination,target);
            if(blocker!=null && blocker.deliveryRecorded){destination(g,canvas,blocker.x,blocker.y,"TOW EMPTY CLEAR");return;}
            boolean working=g.tow.connected(target,g.tug) || g.workingCargo()==target;
            destination(g,canvas,working?g.dockX(target):target.x,working?g.dockY(target):target.y,working?"DELIVERY BERTH":"COLLECT BARGE");return;
        }
        if(MontroseRescue.swimmers(g)){
            if(!g.rescue.barge.towed){Body rescue=g.rescue.barge.boat;destination(g,canvas,rescue.x,rescue.y,"TOW RESCUE BARGE");return;}
            int nearest=-1;float distance=Float.MAX_VALUE;for(int i=0;i<20;i++)if(g.rescue.state[i]<4 && g.rescue.found[i]){float d=Physics.length(g.tug.x-g.rescue.x[i],g.tug.y-g.rescue.y[i]);if(d<distance){nearest=i;distance=d;}}
            if(nearest>=0)destination(g,canvas,g.rescue.x[nearest],g.rescue.y[nearest],"FLING RINGS FROM RESCUE BARGE");else destination(g,canvas,MontroseHarbor.mapX(1350),MontroseHarbor.mapY(-1530),"SEARCH OFFSHORE / USE SPOTLIGHT");return;
        }
        if(g.montroseStage() && g.rescue.touring){float[] target=g.rescue.tourTarget(g);destination(g,canvas,target[0],target[1],"EXPLORE / TOWARD THE BEACH");return;}
        if(MontroseRescue.cruisers(g)){
            Body target=null;for(Body b:g.cargo)if(b!=null && !b.deliveryRecorded){target=b;break;}
            if(target!=null){boolean tow=g.rescue.towed[target.berth];destination(g,canvas,tow?g.dockX(target.berth):target.x,tow?g.dockY(target.berth):target.y,g.rescue.rafted?"RELEASE RAFT LINE":tow?"DOCK "+MontroseRescue.slip(target.berth):"ATTACH TOWLINE");}return;
        }
        if(g.montroseArrival.active())return;
        if(MontroseHaulout.active(g)){Body boat=g.cargo[0];boolean towed=g.haulout.towed;boolean lift=!g.haulout.lowered();destination(g,canvas,lift||towed?MontroseHaulout.X:boat.x,lift?MontroseHaulout.Y:towed?MontroseHaulout.APPROACH_Y:boat.y,!g.haulout.lowered()?"RADIO TRAVELIFT":towed?"HAULOUT / PUSH BOAT IN":"AIN'T BROKE / STAR DOCK S");return;}
        if(g.montroseStage()){Body yacht=g.cargo[0];boolean attached=g.mission.wasTowed;destination(g,canvas,attached?MontroseHarbor.SLIP_X:yacht.x,attached?MontroseHarbor.SLIP_Y:yacht.y,attached?"YACHT SLIP":"ATTACH TOWLINE");return;}
        if(g.chicagoStage()){
            if(g.ropeDragging || g.ropePhase!=0 || g.chicago.complete())return;
            destination(g,canvas,g.chicago.targetX(g),g.chicago.targetY(g),g.chicago.label(g));return;
        }
        if(g.lockStage()) {
            if(g.ropeDragging || g.ropePhase!=0)return;
            if(g.lock.phase==Lock.READY)destination(g,canvas,300,Lock.ENTRY_Y+85,"LOCK ENTRY");
            else if(g.lock.phase==Lock.ENTRY)destination(g,canvas,300,(Lock.ENTRY_Y+Lock.EXIT_Y)*.5f,"INTO LOCK");
            else if(g.lock.phase==Lock.EXIT)destination(g,canvas,300,Lock.EXIT_Y-120,"LOCK EXIT");
            return;
        }
        // The early lessons teach a touch target, before the berth destination.
        if(g.tutorial!=null && (g.tutorial.lesson<Tutorial.DOCK || g.tutorial.lesson>=Tutorial.DOUBLE)) return;
        int berth=focused(g); if(berth<0 || g.ropeDragging || g.ropePhase!=0) return;
        Body blocker=g.berthBlocker(berth,null);
        if(blocker!=null && blocker.deliveryRecorded){destination(g,canvas,blocker.x,blocker.y,"TOW EMPTY CLEAR");return;}
        boolean ready=false; for(Body b:g.cargo) if(b!=null && b.active && (g.sharedBerthStage() || b.berth==berth)) ready|=g.canTie(b);
        if(ready) return;
        destination(g,canvas,g.dockX(berth),g.dockY(berth),g.sharedBerthStage()?"UNLOADING SLIP":"BERTH "+g.berthLetter(berth));
    }
    /** Marker height that keeps an edge marker (arrow plus its label below) off the HUD controls. */
    static float clearOfControls(Game g,float x,float y){
        float jx=Joystick.centerX(g),jy=Joystick.centerY(g),r=Joystick.RADIUS+14;
        if(Joystick.visible(g) && x>jx-r-60 && x<jx+r+60 && y>jy-r-30)y=Math.min(y,jy-r-30);
        Layout.Box map=OverviewMap.bounds(g);
        if(x>map.x-60 && x<map.x+map.w+60 && y>map.y-30)y=Math.min(y,map.y-30);
        return Math.min(y,g.layout.height-92);
    }
    private static void destination(Game g,Painter canvas,float wx,float wy,String label) {
        RiverCamera c=g.layout.camera;
        float x=c.screenX(wx,wy),y=c.screenY(wx,wy);
        if(x<32 || x>g.layout.width-32 || y<80 || y>g.layout.height-70) {
            float cx=g.layout.width/2,cy=g.layout.height/2,dx=x-cx,dy=y-cy;
            float fraction=Math.min((g.layout.width/2-32)/Math.max(1,Math.abs(dx)),
                    (g.layout.height/2-80)/Math.max(1,Math.abs(dy)));
            float ex=cx+dx*fraction,ey=cy+dy*fraction,distance=Physics.length(dx,dy);
            // Every stage keeps edge markers clear of the joystick, chart (with its zoom slider) and button row.
            ey=clearOfControls(g,ex,ey);
            // The captain portrait owns the top-left corner; slide edge cues below it.
            if(ex<PlayerPortrait.RIGHT+40 && ey<PlayerPortrait.BOTTOM+24)ey=PlayerPortrait.BOTTOM+24;
            float ux=dx/distance,uy=dy/distance;
            ControlView.arrow(g,new RiverPainter(canvas,c),c.worldX(ex-ux*40,ey-uy*40),c.worldY(ex-ux*40,ey-uy*40),c.worldX(ex,ey),c.worldY(ex,ey),GOLD);
            Lettering.center(canvas,label,Physics.clamp(ex,85,g.layout.width-85),
                    Physics.clamp(ey+22,85,g.layout.height-70),12,GOLD);return;
        }
        // Place the entire arrow, including its head, in the water plane.
        float dx=wx-g.tug.x,dy=wy-g.tug.y,length=Physics.length(dx,dy);
        if(length<1){dx=0;dy=-1;length=1;}dx/=length;dy/=length;
        float pulse=3*(float)StrictMath.sin(g.visualTicks*.055);
        ControlView.arrow(g,new RiverPainter(canvas,c),wx-dx*(68+pulse),wy-dy*(68+pulse),wx-dx*(24+pulse),wy-dy*(24+pulse),GOLD);
    }
}
