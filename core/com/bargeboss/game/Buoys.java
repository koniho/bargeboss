package com.bargeboss.game;

/** Small displacement floats on damped seabed moorings. Simulation only; rendering is read-only. */
public final class Buoys {
    public static final class Buoy implements Watercraft {
        public final float anchorX,anchorY,radius,mass,reach;
        public float x,y,vx,vy,roll,rollVelocity;
        int contactTicks;
        public float waterX(){return x;}
        public float waterY(){return y;}
        Buoy(float x,float y,float size){
            anchorX=this.x=x;anchorY=this.y=y;radius=size*.27f;mass=size*.014f;reach=size*.55f;
        }
    }
    private Buoy[] floats=new Buoy[0];
    private final float[] force=new float[2];
    public void reset(Game g){
        floats=new Buoy[NavigationLights.count(g)];
        for(int i=0;i<floats.length;i++)if(!NavigationLights.land(g,i))
            floats[i]=new Buoy(NavigationLights.anchorX(g,i),NavigationLights.anchorY(g,i),NavigationLights.size(g,i));
    }
    /** Uninitialized preview/stage fixtures fall back to anchors without mutating during drawing. */
    public Buoy get(Game g,int i){
        if(i<0 || i>=floats.length || NavigationLights.land(g,i))return null;
        Buoy b=floats[i];
        return b!=null && b.anchorX==NavigationLights.anchorX(g,i) && b.anchorY==NavigationLights.anchorY(g,i)?b:null;
    }
    public void step(Game g){
        if(floats.length!=NavigationLights.count(g))reset(g);
        for(int i=0;i<floats.length;i++){
            if(NavigationLights.land(g,i))continue;
            if(get(g,i)==null){reset(g);break;}
        }
        for(Buoy b:floats)if(b!=null){
            if(b.contactTicks>0)b.contactTicks--;
            g.water.buoyForce(g,b.x,b.y,force);
            float dx=b.x-b.anchorX,dy=b.y-b.anchorY,d=Physics.length(dx,dy);
            // Slack chain gives way easily, then loads progressively before its finite limit.
            float spring=.8f+.18f*Math.max(0,d-b.reach*.45f);
            float cx=g.lockStage()?g.lock.currentX(b.x,b.y):0;
            float cy=g.lockStage()?g.lock.currentY(b.x,b.y):0;
            b.vx+=(force[0]-dx*spring+(cx-b.vx)*1.4f)*Physics.DT;
            b.vy+=(force[1]-dy*spring+(cy-b.vy)*1.4f)*Physics.DT;
            b.x+=b.vx*Physics.DT;b.y+=b.vy*Physics.DT;
            b.rollVelocity+=(-24*b.roll-3.4f*b.rollVelocity+(force[0]+force[1])*.025f)*Physics.DT;
            b.roll=Physics.clamp(b.roll+b.rollVelocity*Physics.DT,-.19f,.19f);
            constrain(g,b);
            if(g.lockStage())g.lock.collide(b,g.splashes);
            for(int pass=0;pass<4;pass++){
                contact(g,b,g.tug,false);
                for(Body body:g.cargo)if(body!=null && body.active)
                    contact(g,b,body,body.lineAttached || body.awaitingPickup);
                if(g.tutorial==null)for(Feeder f:g.feeders)if(f.phase!=Feeder.IDLE){
                    contact(g,b,f.boat,true);if(f.load!=null)contact(g,b,f.load,true);
                }
                if(g.trafficActive())contact(g,b,g.trafficBoat,true);
                if(g.lockStage()){constrain(g,b);g.lock.collide(b,g.splashes);}
            }
        }
    }
    /** Pickup assistance leaves room for newly solid entrance marks. Manual driving stays direct. */
    public void pickupWaypoint(Game g,float x,float y,float[] out){
        out[0]=x;out[1]=y;
        float dx=x-g.tug.x,dy=y-g.tug.y,length2=dx*dx+dy*dy;
        if(length2<1)return;
        for(Buoy b:floats)if(b!=null){
            float t=Physics.clamp(((b.x-g.tug.x)*dx+(b.y-g.tug.y)*dy)/length2,0,1);
            float clearance=g.tug.half+g.tug.radius+b.radius+4;
            if(Physics.length(g.tug.x+dx*t-b.x,g.tug.y+dy*t-b.y)>=clearance)continue;
            // Entrance floats lie beside a bank: approach on their open-water side.
            // Training floats are far from cargo pickup routes and need no route override.
            if(g.tutorial!=null)continue;
            float px=b.x+(b.anchorX<(Physics.LEFT+Physics.RIGHT)*.5f?clearance:-clearance);
            float py=Physics.clamp(y,b.y-clearance,b.y+clearance);
            if(WaterEffectsPainter.contains(g,px,py)){out[0]=px;out[1]=py;return;}
        }
    }
    /** Physical footprint, rather than the tall sprite bounds, must remain on water. */
    static boolean onWater(Game g,Buoy b,float x,float y){
        if(!WaterEffectsPainter.contains(g,x,y))return false;
        for(int i=0;i<8;i++){
            double a=i*StrictMath.PI/4;
            if(!WaterEffectsPainter.contains(g,x+b.radius*(float)StrictMath.cos(a),y+b.radius*(float)StrictMath.sin(a)))return false;
        }
        return true;
    }
    private static void constrain(Game g,Buoy b){
        float dx=b.x-b.anchorX,dy=b.y-b.anchorY,d=Physics.length(dx,dy);
        if(d>b.reach){
            float nx=dx/d,ny=dy/d;b.x=b.anchorX+nx*b.reach;b.y=b.anchorY+ny*b.reach;
            float outward=b.vx*nx+b.vy*ny;
            if(outward>0){b.vx-=outward*nx;b.vy-=outward*ny;}
        }
        if(!onWater(g,b,b.x,b.y)){
            float lo=0,hi=1;dx=b.x-b.anchorX;dy=b.y-b.anchorY;
            for(int j=0;j<12;j++){
                float t=(lo+hi)*.5f;
                if(onWater(g,b,b.anchorX+dx*t,b.anchorY+dy*t))lo=t;else hi=t;
            }
            b.x=b.anchorX+dx*lo;b.y=b.anchorY+dy*lo;
            float len=Physics.length(dx,dy),outward=len>.001f?(b.vx*dx+b.vy*dy)/len:0;
            if(outward>0){
                g.splashes.contact(b,null,b.x,b.y,b.vx,b.vy,0,0,outward);
                b.vx-=outward*dx/len;b.vy-=outward*dy/len;
            }
        }
    }
    /** Disc contact transfers linear impulse and torque; a taut tether transfers resistance to the hull. */
    static void contact(Game g,Buoy b,Body body,boolean fixed){
        if(body==null || !body.active || Math.abs(body.x-b.x)>body.half+body.radius+b.radius
                || Math.abs(body.y-b.y)>body.half+body.radius+b.radius)return;
        boolean touched=false;
        for(int disc=0;disc<3;disc++){
            float hx=body.discX(disc),hy=body.discY(disc),dx=b.x-hx,dy=b.y-hy;
            float d=Physics.length(dx,dy),r=body.radius+b.radius;if(d>=r)continue;
            touched=true;
            float nx=d>.001f?dx/d:-(float)StrictMath.cos(body.angle);
            float ny=d>.001f?dy/d:-(float)StrictMath.sin(body.angle);
            float cvx=body.vx-body.spin*(hy-body.y),cvy=body.vy+body.spin*(hx-body.x);
            g.splashes.contact(body,b,hx+nx*body.radius,hy+ny*body.radius,
                    cvx,cvy,b.vx,b.vy,(cvx-b.vx)*nx+(cvy-b.vy)*ny);
            float invHull=fixed?0:1/body.mass,invBuoy=1/b.mass;
            float correction=Math.max(0,r-d-.025f)*.85f,ox=b.x,oy=b.y;
            b.x+=nx*correction*invBuoy/(invHull+invBuoy);b.y+=ny*correction*invBuoy/(invHull+invBuoy);
            constrain(g,b);
            float moved=(b.x-ox)*nx+(b.y-oy)*ny;
            if(!fixed){body.x-=nx*(correction-moved);body.y-=ny*(correction-moved);}
            // If land/chain prevented separation, the buoy cannot yield further along this normal.
            float desired=correction*invBuoy/(invHull+invBuoy);
            if(desired>.001f)invBuoy*=Physics.clamp(moved/desired,0,1);
            float rx=hx-body.x,ry=hy-body.y,arm=rx*ny-ry*nx;
            float relative=(b.vx-body.vx+body.spin*ry)*nx+(b.vy-body.vy-body.spin*rx)*ny;
            if(relative>=0)continue;
            float inverse=invHull+invBuoy+(fixed?0:arm*arm/body.inertia);
            if(inverse<.001f)continue;
            float impulse=-1.05f*relative/inverse;
            if(!fixed){body.vx-=impulse*nx*invHull;body.vy-=impulse*ny*invHull;body.spin-=arm*impulse/body.inertia;}
            b.vx+=impulse*nx*invBuoy;b.vy+=impulse*ny*invBuoy;
            b.rollVelocity=Physics.clamp(b.rollVelocity-impulse*(nx+ny)*.045f,-1.8f,1.8f);
            if(body==g.tug && relative<-4 && b.contactTicks==0){g.haptic(Game.HAPTIC_CONTACT);b.contactTicks=45;}
        }
        // Resolve against the active world's shoreline. Marina mooring balls live far
        // outside the old river corridor; Physics.banks would teleport their hulls.
        if(touched && !fixed){
            if(g.montroseStage())MontroseHarbor.collide(body,g.splashes);
            else CalumetMap.collide(body,g.splashes);
        }
    }
}
