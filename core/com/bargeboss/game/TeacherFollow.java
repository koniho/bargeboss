package com.bargeboss.game;

/** Bounded breadcrumb escort: follows the player's actual route, never a shortcut across land. */
final class TeacherFollow {
    static final int CAPACITY=1024;
    static final float GAP=155;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY];
    private int head,count,castOff,frames;
    void step(Game g,Body boat){
        int last=(head+count-1+CAPACITY)%CAPACITY;
        if(count==0 || Physics.length(g.tug.x-x[last],g.tug.y-y[last])>=8){
            if(count<CAPACITY){int at=(head+count)%CAPACITY;x[at]=g.tug.x;y[at]=g.tug.y;count++;}
        }
        last=(head+count-1)%CAPACITY;
        // Clear the student's first departure line before joining the trail.
        if(castOff<120){
            float u=++castOff/120f,previous=boat.x;
            boat.x=TutorialArrival.DOCK_X+85*u*u*(3-2*u);boat.vx=(boat.x-previous)*60;boat.vy=0;boat.engineLoad=.35f;
            return;
        }
        float gap=GAP+g.tow.loadCount(g.tug)*85;
        if(frames++%12==0 && count>2){
            // Collapse redundant buoy loops only when the whole shortcut is clear
            // for the teacher's hull. Canal corners remain in the ordered trail.
            float behind=Physics.length(g.tug.x-x[last],g.tug.y-y[last]);
            for(int i=count-2;i>0;i--){
                int at=(head+i)%CAPACITY,next=(at+1)%CAPACITY;
                behind+=Physics.length(x[next]-x[at],y[next]-y[at]);
                if(behind>=gap){
                    if(CalumetMap.clear(boat.x,boat.y,x[at],y[at],boat.half+boat.radius+2)){head=at;count-=i;}
                    break;
                }
            }
        }
        while(count>1 && Physics.length(boat.x-x[head],boat.y-y[head])<14){head=(head+1)%CAPACITY;count--;}
        float remaining=Physics.length(boat.x-x[head],boat.y-y[head]);
        for(int i=1;i<count;i++){int a=(head+i-1)%CAPACITY,b=(head+i)%CAPACITY;remaining+=Physics.length(x[b]-x[a],y[b]-y[a]);}
        last=(head+count-1)%CAPACITY;remaining+=Physics.length(g.tug.x-x[last],g.tug.y-y[last]);
        float speed=Physics.clamp((remaining-gap)*4,0,220);
        if(Physics.length(boat.x-g.tug.x,boat.y-g.tug.y)<110)speed=0;
        for(Body b:g.cargo)if(b!=null && b.active && Physics.touching(boat,b,38))speed=0;
        float dx=x[head]-boat.x,dy=y[head]-boat.y,d=Math.max(1,Physics.length(dx,dy));
        if(speed>0){
            float wanted=(float)StrictMath.atan2(dy,dx);
            boat.angle=Physics.angle(boat.angle+Physics.clamp(Physics.angle(wanted-boat.angle),-.055f,.055f));
        }
        if(g.bridge.crosses(boat.x,boat.y,x[head],y[head]) && !g.bridge.passable())speed=0;
        // Follow the dense path with bounded acceleration; hard safety gaps cut thrust.
        float ease=speed==0?.4f:.14f;
        boat.vx+=(dx/d*speed-boat.vx)*ease;boat.vy+=(dy/d*speed-boat.vy)*ease;
        boat.engineLoad+=(Math.min(.85f,speed/220)-boat.engineLoad)*.08f;
        Physics.integrate(boat);CalumetMap.collide(boat,g.splashes);g.bridge.collide(boat,g.splashes);
    }
}
