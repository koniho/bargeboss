package com.bargeboss.game;

/** Stage-owned deterministic bridge requests. A tow crossing a span holds it open. */
final class BridgePassage {
    static final int OPEN_TICKS=300,WAIT_TICKS=90,HOLD_TICKS=360;
    final LandmarkBridge[] bridges;
    final int[] position,hold,wait;
    BridgePassage(Game g){bridges=LocationBridges.forGame(g);position=new int[bridges.length];hold=new int[bridges.length];wait=new int[bridges.length];}
    float amount(int i){return position[i]/(float)OPEN_TICKS;}
    boolean passable(int i){return !bridges[i].movable() || position[i]>=OPEN_TICKS;}
    boolean blocksLine(float x,float y,float xx,float yy){
        for(int i=0;i<bridges.length;i++)if(!passable(i)){
            LandmarkBridge b=bridges[i];float a=b.across(x,y),c=b.across(xx,yy);
            if(a*c>0 || Math.abs(a-c)<.001f)continue;
            float t=a/(a-c),at=b.along(x+(xx-x)*t,y+(yy-y)*t);
            if(at>=-b.left && at<=b.right)return true;
        }return false;
    }
    private boolean near(LandmarkBridge a,Body b,float distance){return b!=null && b.active && a.along(b.x,b.y)>-a.left-100 && a.along(b.x,b.y)<a.right+100 && Math.abs(a.across(b.x,b.y))<distance+b.half+b.radius;}
    boolean requested(Game g,int i){LandmarkBridge b=bridges[i];
        if(near(b,g.tug,650))return true;
        for(Body cargo:g.cargo)if(cargo!=null && near(b,cargo,Math.max(90,b.width)))return true;
        for(int j=0;j<Towline.CAPACITY;j++){Towline line=g.tow.link(j);if(line!=null && line.cargo!=null){
            float a=b.across(line.leader.x,line.leader.y),c=b.across(line.cargo.x,line.cargo.y);
            if(a*c<=0){float t=a/(a-c==0?1:a-c),along=b.along(line.leader.x+(line.cargo.x-line.leader.x)*t,line.leader.y+(line.cargo.y-line.leader.y)*t);if(along>=-b.left && along<=b.right)return true;}
        }}return false;
    }
    void step(Game g){for(int i=0;i<bridges.length;i++){
        if(!bridges[i].movable())continue;
        boolean request=g.tutorial!=null?g.tutorial.canal.authorized:requested(g,i);
        if(request){hold[i]=HOLD_TICKS;if(position[i]>0 || ++wait[i]>=WAIT_TICKS)position[i]=Math.min(OPEN_TICKS,position[i]+1);}
        else if(hold[i]>0)hold[i]--;
        else if(position[i]>0)position[i]--;
        else wait[i]=0;
    }}
    float collide(Body body,CollisionSplashes splashes){float impact=0;if(body==null || !body.active)return 0;
        for(int n=0;n<bridges.length;n++){LandmarkBridge b=bridges[n];if(passable(n) || !near(b,body,b.width))continue;
            for(int d=0;d<3;d++){float x=body.discX(d),y=body.discY(d),along=b.along(x,y),across=b.across(x,y),reach=body.radius+b.width*.5f;
                if(along<-b.left || along>b.right || Math.abs(across)>=reach)continue;
                float side=b.across(body.x,body.y)>=0?1:-1,nx=-b.dy*side,ny=b.dx*side;
                float dot=body.vx*nx+body.vy*ny;
                Physics.wallSplash(splashes,body,x-nx*body.radius,y-ny*body.radius,nx,ny,x,y);
                body.x+=nx*(reach-side*across);body.y+=ny*(reach-side*across);
                if(dot<0){impact=Math.max(impact,-dot);body.vx-=dot*nx*1.08f;body.vy-=dot*ny*1.08f;}body.spin*=.6f;
            }
        }return impact;
    }
}
