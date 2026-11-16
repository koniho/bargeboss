package com.bargeboss.game;

/** Per-game damped, finite berth tethers. Shared fixed steps own all movement. */
public final class MooredFleet {
    static final float REACH=2.5f,YAW=.065f;
    static final class FloatBoat {
        final MooredBoats.Berth berth;final float[] hull=new float[8];final boolean roomy;
        float x,y,vx,vy,yaw,spin,heave;
        FloatBoat(MooredBoats.Berth b){berth=b;x=b.x;y=b.y;updateHull();boolean room=true;for(int corner=0;corner<8;corner+=2)for(int n=0;n<16;n++){double a=n*StrictMath.PI/8;if(!MontroseHarbor.contains(hull[corner]+5*(float)StrictMath.cos(a),hull[corner+1]+5*(float)StrictMath.sin(a)))room=false;}roomy=room;}
        void updateHull(){float a=berth.angle+yaw,c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a);
            for(int i=0;i<4;i++){float u=(i<2?-1:1)*berth.length*.48f,v=(i==0||i==3?-1:1)*berth.width*.48f;hull[i*2]=x+c*u-s*v;hull[i*2+1]=y+s*u+c*v;}}
        boolean clear(){for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(hull[i],hull[i+1]))return false;return true;}
        void constrain(){float dx=x-berth.x,dy=y-berth.y,d=Physics.length(dx,dy);
            if(d>REACH){float nx=dx/d,ny=dy/d;x=berth.x+nx*REACH;y=berth.y+ny*REACH;float outward=vx*nx+vy*ny;if(outward>0){vx-=outward*nx;vy-=outward*ny;}}
            yaw=Physics.clamp(yaw,-YAW,YAW);if(Math.abs(yaw)>=YAW)spin=0;
            updateHull();if(roomy)return;for(int tries=0;!clear() && tries<8;tries++){x=(x+berth.x)*.5f;y=(y+berth.y)*.5f;yaw*=.5f;vx*=.5f;vy*=.5f;spin*=.5f;updateHull();}
            if(!clear()){x=berth.x;y=berth.y;yaw=vx=vy=spin=0;updateHull();}
        }
    }
    FloatBoat[] boats=new FloatBoat[0];int[] order=new int[0];private final float[] force=new float[2];
    public void reset(Game g){int count=g.montroseStage()?MooredBoats.BERTHS.length:0;boats=new FloatBoat[count];order=new int[count];for(int i=0;i<count;i++){boats[i]=new FloatBoat(MooredBoats.BERTHS[i]);order[i]=i;}}
    FloatBoat sorted(int i){return i<order.length?boats[order[i]]:null;}
    public void step(Game g){if(!g.montroseStage())return;
        for(int i=0;i<boats.length;i++){FloatBoat b=boats[i];float dx=b.x-b.berth.x,dy=b.y-b.berth.y;
            // Ambient orbital forcing applies everywhere; full wake sampling is local to moving craft.
            boolean near=Physics.length(b.x-g.tug.x,b.y-g.tug.y)<240;
            for(Body cargo:g.cargo)if(cargo!=null && cargo.active && Math.abs(cargo.x-b.x)<240 && Math.abs(cargo.y-b.y)<240)near=true;
            if(near)g.water.buoyForce(g,b.x,b.y,force);
            else {force[0]=.65f*(float)StrictMath.sin(g.visualTicks*.048+b.x*.017+b.y*.009);force[1]=.5f*(float)StrictMath.sin(g.visualTicks*.031-b.x*.013+b.y*.021);}
            float spring=.45f+.9f*Math.max(0,Physics.length(dx,dy)-REACH*.55f);
            b.vx+=(force[0]*1.6f-dx*spring-b.vx*.95f)*Physics.DT;b.vy+=(force[1]*1.6f-dy*spring-b.vy*.95f)*Physics.DT;
            b.x+=b.vx*Physics.DT;b.y+=b.vy*Physics.DT;
            b.spin+=((force[0]-force[1])*.035f-b.yaw*.8f-b.spin*1.2f)*Physics.DT;b.yaw+=b.spin*Physics.DT;
            float height=near?g.water.height(b.x,b.y,g.visualTicks,null):force[0]+.44f*force[1];
            b.heave+=(height*.55f-b.heave)*.12f;
            b.constrain();
        }
        for(int i=1;i<order.length;i++){int value=order[i],j=i-1;FloatBoat b=boats[value];while(j>=0 && boats[order[j]].y-boats[order[j]].x>b.y-b.x){order[j+1]=order[j];j--;}order[j+1]=value;}
    }
    public float collide(Body body,CollisionSplashes splashes){float hit=0;
        for(FloatBoat b:boats)if(Math.abs(body.x-b.x)<body.half+body.radius+b.berth.length && Math.abs(body.y-b.y)<body.half+body.radius+b.berth.length){
            float vx=body.vx,vy=body.vy,impact=PolygonWater.collide(body,b.hull,false,splashes);hit=Math.max(hit,impact);
            if(impact>0){b.vx=Physics.clamp(b.vx+vx*.08f,-4,4);b.vy=Physics.clamp(b.vy+vy*.08f,-4,4);b.spin=Physics.clamp(b.spin+(vx-vy)*.0005f,-.08f,.08f);}
        }return hit;
    }
}
