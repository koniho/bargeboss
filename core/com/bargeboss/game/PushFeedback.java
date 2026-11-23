package com.bargeboss.game;

/** Short, read-only hull clearance probes; never confuse normal tug alignment with obstruction. */
final class PushFeedback {
    static boolean blocked(Game g,Body b){
        if(g.control==Game.WAITING_BRIDGE)return true;
        if(g.control==Game.DOCKING || g.pushX*g.pushX+g.pushY*g.pushY<.5f)return false;
        float nx=g.pushX,ny=g.pushY;
        for(int i=0;i<3;i++)for(int side=-1;side<=1;side++){
            float lateral=side*b.radius*.65f,forward=b.radius*(side==0?1:.76f);
            float x=b.discX(i)+nx*forward-ny*lateral,y=b.discY(i)+ny*forward+nx*lateral;
            float xx=x+nx*10,yy=y+ny*10;
            if(!Towline.clear(g,x,y,xx,yy))return true;
            for(Body other:g.cargo)if(other!=null && other!=b && other.active && other.hit(xx,yy,0))return true;
        }
        return false;
    }
    private PushFeedback(){}
}
