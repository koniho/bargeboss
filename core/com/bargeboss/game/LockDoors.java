package com.bargeboss.game;

/** Shared rigid leaf geometry for artwork, watercraft contacts and towline obstruction. */
public final class LockDoors {
    public static final float LENGTH=(Physics.RIGHT-Physics.LEFT)*.5f;
    private LockDoors(){}
    public static float hingeX(int side){return side==0?Physics.LEFT:Physics.RIGHT;}
    public static float hingeY(int gate){return gate==0?Lock.EXIT_Y:Lock.ENTRY_Y;}
    public static float angle(int gate,int side,float opening){
        float a=Physics.clamp(opening,0,1)*(float)Math.PI*.5f*(gate==0?-1:1);
        return side==0?a:(float)Math.PI-a;
    }
    public static float tipX(int gate,int side,float opening){return hingeX(side)+LENGTH*(float)StrictMath.cos(angle(gate,side,opening));}
    public static float tipY(int gate,int side,float opening){return hingeY(gate)+LENGTH*(float)StrictMath.sin(angle(gate,side,opening));}
    /** Six-unit padded leaf segments follow both doors, even when folded against their banks. */
    static float collide(Watercraft craft,int gate,float opening,CollisionSplashes splashes){
        Body body=craft instanceof Body?(Body)craft:null;
        Buoys.Buoy buoy=craft instanceof Buoys.Buoy?(Buoys.Buoy)craft:null;
        if(body==null && buoy==null || body!=null && !body.active)return 0;
        float radius=body!=null?body.radius:buoy.radius,impact=0;
        if(Math.abs(craft.waterY()-hingeY(gate))>LENGTH+Lock.GATE_HALF+radius+(body!=null?body.half:0))return 0;
        for(int pass=0;pass<8;pass++){
            boolean touched=false;
            for(int side=0;side<2;side++){
                float ax=hingeX(side),ay=hingeY(gate),a=angle(gate,side,opening);
                float ux=(float)StrictMath.cos(a),uy=(float)StrictMath.sin(a);
                for(int disc=0;disc<(body!=null?3:1);disc++){
                    float x=body!=null?body.discX(disc):buoy.x,y=body!=null?body.discY(disc):buoy.y;
                    float along=Physics.clamp((x-ax)*ux+(y-ay)*uy,0,LENGTH);
                    float qx=ax+ux*along,qy=ay+uy*along,dx=x-qx,dy=y-qy,d=Physics.length(dx,dy),reach=radius+Lock.GATE_HALF;
                    if(d>=reach)continue;
                    touched=true;
                    float nx,ny;
                    if(d>.0001f){nx=dx/d;ny=dy/d;}
                    else {float sign=(craft.waterX()-ax)*-uy+(craft.waterY()-ay)*ux>=0?1:-1;nx=-uy*sign;ny=ux*sign;}
                    float vx=body!=null?body.vx:buoy.vx,vy=body!=null?body.vy:buoy.vy;
                    float cvx=body!=null?vx-body.spin*(y-body.y):vx,cvy=body!=null?vy+body.spin*(x-body.x):vy;
                    float inward=vx*nx+vy*ny;impact=Math.max(impact,-cvx*nx-cvy*ny);
                    if(splashes!=null)splashes.contact(craft,null,qx+nx*(Lock.GATE_HALF+.75f),qy+ny*(Lock.GATE_HALF+.75f),cvx,cvy,0,0,-cvx*nx-cvy*ny);
                    float push=reach-d;
                    if(body!=null){
                        body.x+=nx*push;body.y+=ny*push;
                        if(inward<0){body.vx-=1.08f*inward*nx;body.vy-=1.08f*inward*ny;}
                        body.spin*=.6f;
                    }else{
                        buoy.x+=nx*push;buoy.y+=ny*push;
                        if(inward<0){
                            buoy.vx-=1.08f*inward*nx;buoy.vy-=1.08f*inward*ny;
                            buoy.rollVelocity=Physics.clamp(buoy.rollVelocity+inward*(nx+ny)*.025f,-1.8f,1.8f);
                        }
                    }
                }
            }
            if(!touched)break;
            if(touched){
                if(body!=null)Physics.banks(body,-100000,100000,false,splashes);
                else {
                    buoy.x=Physics.clamp(buoy.x,Physics.LEFT+radius,Physics.RIGHT-radius);
                }
            }
        }
        return impact;
    }
    static float pointDistance(float x,float y,float ax,float ay,float bx,float by){
        float dx=bx-ax,dy=by-ay,len=dx*dx+dy*dy;
        float t=len>.0001f?Physics.clamp(((x-ax)*dx+(y-ay)*dy)/len,0,1):0;
        return Physics.length(x-ax-t*dx,y-ay-t*dy);
    }
    public static boolean blocksLine(int gate,float opening,float x,float y,float xx,float yy){
        for(int side=0;side<2;side++){
            float ax=hingeX(side),ay=hingeY(gate),bx=tipX(gate,side,opening),by=tipY(gate,side,opening);
            float dx=xx-x,dy=yy-y,ex=bx-ax,ey=by-ay,den=dx*ey-dy*ex;
            if(Math.abs(den)>.0001f){
                float t=((ax-x)*ey-(ay-y)*ex)/den,u=((ax-x)*dy-(ay-y)*dx)/den;
                if(t>=0 && t<=1 && u>=0 && u<=1)return true;
            }
            if(Math.min(Math.min(pointDistance(x,y,ax,ay,bx,by),pointDistance(xx,yy,ax,ay,bx,by)),
                    Math.min(pointDistance(ax,ay,x,y,xx,yy),pointDistance(bx,by,x,y,xx,yy)))<=Lock.GATE_HALF)return true;
        }
        return false;
    }
}
