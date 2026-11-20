package com.bargeboss.game;

/** Observer tug: holds outside exercises and relocates only at long range. */
final class TeacherFollow {
    static final int CAPACITY=1024;
    static final float GAP=300;
    private final float[] x=new float[CAPACITY],y=new float[CAPACITY];
    private int head,count,frames;
    private boolean relocating;
    private float observerX=-1770,observerY=510;
    void step(Game g,Body boat){
        int last=(head+count-1+CAPACITY)%CAPACITY;
        if(count==0 || Physics.length(g.tug.x-x[last],g.tug.y-y[last])>=8){
            if(count==CAPACITY){head=(head+1)%CAPACITY;count--;}
            int at=(head+count)%CAPACITY;x[at]=g.tug.x;y[at]=g.tug.y;count++;
        }
        last=(head+count-1)%CAPACITY;
        if(g.tutorial.lesson<=Tutorial.REVERSE){
            if(Physics.length(boat.x-g.tug.x,boat.y-g.tug.y)>520){
                float x=boat.x+(g.tug.x-boat.x)*.45f,y=boat.y+(g.tug.y-boat.y)*.45f;
                if(CalumetMap.clear(boat.x,boat.y,x,y,boat.half+boat.radius+2)){observerX=x;observerY=y;}
            }
            move(g,boat,observerX,observerY,120);facePlayer(g,boat);return;
        }
        float separation=Physics.length(boat.x-g.tug.x,boat.y-g.tug.y);
        if(separation>520)relocating=true;
        if(separation<320)relocating=false;
        if(!relocating){
            boat.vx*=.7f;boat.vy*=.7f;boat.engineLoad*=.92f;
            Physics.integrate(boat);facePlayer(g,boat);return;
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
        Physics.integrate(boat);CalumetMap.collide(boat,g.splashes);g.bridge.collide(boat,g.splashes);facePlayer(g,boat);
    }
    private static void facePlayer(Game g,Body boat){
        float wanted=(float)StrictMath.atan2(g.tug.y-boat.y,g.tug.x-boat.x);
        boat.angle=Physics.angle(boat.angle+Physics.clamp(Physics.angle(wanted-boat.angle),-.025f,.025f));
    }
    private static void move(Game g,Body boat,float x,float y,float limit){
        float dx=x-boat.x,dy=y-boat.y,d=Physics.length(dx,dy),speed=Math.min(limit,d*1.5f);
        if(Physics.length(boat.x-g.tug.x,boat.y-g.tug.y)<85)speed=0;
        boat.vx+=(dx/Math.max(1,d)*speed-boat.vx)*.08f;
        boat.vy+=(dy/Math.max(1,d)*speed-boat.vy)*.08f;
        boat.engineLoad+=(speed/180-boat.engineLoad)*.08f;
        Physics.integrate(boat);CalumetMap.collide(boat,g.splashes);
    }

}
