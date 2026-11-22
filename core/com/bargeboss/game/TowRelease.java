package com.bargeboss.game;

/** Tap the rope itself to cast off; equipment buttons never change tow topology. */
final class TowRelease {
    private int owner=-1,startTick;
    private Body cargo;
    private float downX,downY;
    private boolean armed;
    void cancel(){owner=-1;cargo=null;armed=false;}
    boolean owns(int id){return owner==id;}
    static float point(Game g,Towline l,float t,boolean horizontal){
        float x=RopeAttachment.x(l.cargo,l.cargoEnd),y=RopeAttachment.y(l.cargo,l.cargoEnd);
        float xx=RopeAttachment.x(l.leader,l.leaderEnd),yy=RopeAttachment.y(l.leader,l.leaderEnd);
        float slack=Physics.clamp((l.length-Physics.length(xx-x,yy-y))*.25f,0,12);
        float h=RopeAttachment.height(l.cargo)+WaterSurface.lift(g,l.cargo);
        float hh=RopeAttachment.height(l.leader)+WaterSurface.lift(g,l.leader);
        RiverCamera c=g.layout.camera;
        return horizontal?c.screenX(x+(xx-x)*t,y+(yy-y)*t):c.screenY(x+(xx-x)*t,y+(yy-y)*t)-Math.max(.5f,h+(hh-h)*t-4*t*(1-t)*slack)*c.scale;
    }
    private static Body hit(Game g,float x,float y){
        Body best=null;float distance=16;
        for(int i=0;i<Towline.CAPACITY;i++){
            Towline l=g.tow.link(i);if(l==null || l.cargo==null)continue;
            for(int n=4;n<=20;n++){
                float d=Physics.length(point(g,l,n/24f,true)-x,point(g,l,n/24f,false)-y);
                if(d<distance){distance=d;best=l.cargo;}
            }
        }
        return best;
    }
    boolean down(Game g,int id,float x,float y){
        if(owner!=-1)return true;
        if(g.mode!=Game.PLAY || g.conversation() || g.pointer || g.pinchZoom.active() || g.ropePhase==1 || g.ropePhase==2
                || y<200 && x>g.layout.width-562 || Joystick.hit(g,x,y) || OverviewMap.bounds(g).contains(x,y))return false;
        Body b=hit(g,x,y);if(b==null)return false;
        owner=id;startTick=g.visualTicks;cargo=b;downX=x;downY=y;armed=true;return true;
    }
    boolean move(int id,float x,float y){if(owner!=id)return false;if(Physics.length(x-downX,y-downY)>14)armed=false;return true;}
    boolean up(Game g,int id,float x,float y){
        if(owner!=id)return false;
        Body b=cargo;boolean release=armed && g.visualTicks-startTick<=18 && Physics.length(x-downX,y-downY)<=14 && hit(g,x,y)==b;
        cancel();if(release)g.releaseTow(b);return true;
    }
    static void draw(Game g,Painter p){
        if(g.mode!=Game.PLAY || g.conversation())return;
        for(int i=0;i<Towline.CAPACITY;i++){
            Towline l=g.tow.link(i);if(l==null || l.cargo==null)continue;
            boolean show=g.abilities.selected==l.cargo && g.abilities.visibleTicks>0 || g.tutorial!=null && (g.tutorial.lesson==Tutorial.CASTOFF || g.tutorial.lesson==Tutorial.LINK || g.tutorial.lesson==Tutorial.RELEASE);
            if(!show)continue;
            float x=point(g,l,.5f,true),y=point(g,l,.5f,false);
            SoftCue.ring(p,x,y,13,1,SoftCue.animated(g,0xccffd329),1);
            ArcadeIcons.draw(p,ArcadeIcons.UNLINK,x,y,.5f,0xffffedbc);
        }
    }
}
