package com.bargeboss.game;

/** Shared two-finger camera input and fixed-step return to automatic framing. */
final class PinchZoom {
    static final int HOLD_TICKS=360,RETURN_TICKS=360;
    static final float MIN=.65f,MAX=4.5f;
    private int first=-1,second=-1,remaining;
    private float ax,ay,bx,by,startDistance,startZoom,worldX,worldY,manualZoom;
    private boolean captured;
    boolean active(){return captured;}
    void reset(){cancelTouches();remaining=0;}
    void cancelTouches(){first=second=-1;captured=false;}
    boolean down(Game g,int id,float x,float y){
        if(captured)return true;
        if(first==-1){first=id;ax=x;ay=y;return false;}
        if(first==id)return false;
        int old=first;float oldX=ax,oldY=ay;
        g.cancel(); // A pinch must never release a world tap or throw a rope.
        first=old;ax=oldX;ay=oldY;second=id;bx=x;by=y;captured=true;
        startDistance=Math.max(16,Physics.length(bx-ax,by-ay));startZoom=g.layout.camera.zoom;
        worldX=g.layout.camera.worldX((ax+bx)*.5f,(ay+by)*.5f);
        worldY=g.layout.camera.worldY((ax+bx)*.5f,(ay+by)*.5f);
        manualZoom=startZoom;remaining=HOLD_TICKS+RETURN_TICKS;
        return true;
    }
    boolean move(Game g,int id,float x,float y){
        if(id==first){ax=x;ay=y;}else if(id==second){bx=x;by=y;}else return captured;
        if(!captured)return false;
        if(first!=-1 && second!=-1){
            manualZoom=Physics.clamp(startZoom*Physics.length(bx-ax,by-ay)/startDistance,MIN,MAX);
            remaining=HOLD_TICKS+RETURN_TICKS;
            g.changeCamera(Layout.focused(g.layout.width,g.layout.height,worldX,worldY,manualZoom,(ax+bx)*.5f,(ay+by)*.5f));
            manualZoom=g.layout.camera.zoom;
        }
        return true;
    }
    boolean up(int id){
        boolean consume=captured;
        if(id==first)first=-1;if(id==second)second=-1;
        // Consume the surviving finger until both lift: no accidental throttle/tap.
        if(first==-1 && second==-1)captured=false;
        return consume;
    }
    float target(float automatic,boolean busy){
        if(remaining==0)return automatic;
        float weight=Physics.clamp(remaining/(float)RETURN_TICKS,0,1);
        weight=weight*weight*(3-2*weight);
        if(!busy)remaining--;
        return automatic+(manualZoom-automatic)*weight;
    }
}
