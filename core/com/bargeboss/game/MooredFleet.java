package com.bargeboss.game;

/** Per-game damped, finite berth tethers. Shared fixed steps own all movement. */
public final class MooredFleet {
    static final float REACH=4f,YAW=.10f;
    static final class FloatBoat {
        final MooredBoats.Berth berth;final float[] hull=new float[8];final boolean roomy;
        float x,y,vx,vy,yaw,spin,heave,wakeHeave,anchorX,anchorY;
        FloatBoat(MooredBoats.Berth b){berth=b;x=b.x;y=b.y;anchorX=b.ax;anchorY=b.ay;updateHull();boolean room=true;for(int corner=0;corner<8;corner+=2)for(int n=0;n<16;n++){double a=n*StrictMath.PI/8;if(!MontroseHarbor.contains(hull[corner]+9*(float)StrictMath.cos(a),hull[corner+1]+9*(float)StrictMath.sin(a)))room=false;}roomy=room;}
        void updateHull(){float a=berth.angle+yaw,c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a);
            for(int i=0;i<4;i++){float u=(i<2?-1:1)*berth.length*.48f,v=(i==0||i==3?-1:1)*berth.width*.48f;hull[i*2]=x+c*u-s*v;hull[i*2+1]=y+s*u+c*v;}}
        boolean clear(){for(int i=0;i<8;i+=2)if(!MontroseHarbor.contains(hull[i],hull[i+1]))return false;return true;}
        boolean swinging(){return berth.mooring>=0;}
        float bowDistance(){float a=berth.angle+yaw;return Physics.length(x+(float)StrictMath.cos(a)*berth.length*.45f-anchorX,y+(float)StrictMath.sin(a)*berth.length*.45f-anchorY);}
        void constrain(){
            if(swinging()){
                yaw=Physics.angle(yaw);float a=berth.angle+yaw,c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),arm=berth.length*.45f;
                float dx=x+c*arm-anchorX,dy=y+s*arm-anchorY,d=Physics.length(dx,dy);
                if(d>20){float nx=dx/d,ny=dy/d;x-=(d-20)*nx;y-=(d-20)*ny;
                    float outward=vx*nx+vy*ny;if(outward>0){vx-=outward*nx;vy-=outward*ny;}}
                updateHull();if(clear())return;
                x=berth.x;y=berth.y;yaw=vx=vy=spin=0;updateHull();return;
            }
            float dx=x-berth.x,dy=y-berth.y,d=Physics.length(dx,dy);
            if(d>REACH){float nx=dx/d,ny=dy/d;x=berth.x+nx*REACH;y=berth.y+ny*REACH;float outward=vx*nx+vy*ny;if(outward>0){vx-=outward*nx;vy-=outward*ny;}}
            yaw=Physics.clamp(yaw,-YAW,YAW);if(Math.abs(yaw)>=YAW)spin=0;
            updateHull();if(roomy)return;for(int tries=0;!clear() && tries<8;tries++){x=(x+berth.x)*.5f;y=(y+berth.y)*.5f;yaw*=.5f;vx*=.5f;vy*=.5f;spin*=.5f;updateHull();}
            if(!clear()){x=berth.x;y=berth.y;yaw=vx=vy=spin=0;updateHull();}
        }
    }
    FloatBoat[] boats=new FloatBoat[0];int[] order=new int[0];private final float[] force=new float[2];
    public void reset(Game g){java.util.ArrayList<FloatBoat> list=new java.util.ArrayList<FloatBoat>();
        if(g.montroseStage())for(MooredBoats.Berth b:MooredBoats.BERTHS)if(!MontroseRescue.reserved(g,b) && !MontroseHaulout.reserved(b))list.add(new FloatBoat(b));
        boats=list.toArray(new FloatBoat[list.size()]);order=new int[boats.length];for(int i=0;i<order.length;i++)order[i]=i;
    }
    FloatBoat sorted(int i){return i<order.length?boats[order[i]]:null;}
    public void step(Game g){if(!g.montroseStage())return;
        for(int i=0;i<boats.length;i++){FloatBoat b=boats[i];float dx=b.x-b.berth.x,dy=b.y-b.berth.y;
            g.water.buoyForce(g,b.x,b.y,force);
            // Amplify only propagated wake motion, leaving sheltered idle water calm.
            float seaX=SeaWaves.value(g,b.x,b.y,g.visualTicks,1)*1.4f,seaY=SeaWaves.value(g,b.x,b.y,g.visualTicks,2)*1.4f;
            force[0]=seaX+(force[0]-seaX)*3;force[1]=seaY+(force[1]-seaY)*3;
            if(b.swinging()){
                Buoys.Buoy ball=g.buoys.get(g,b.berth.mooring+2);b.anchorX=ball==null?b.berth.ax:ball.x;b.anchorY=ball==null?b.berth.ay:ball.y;
                float a=b.berth.angle+b.yaw,c=(float)StrictMath.cos(a),s=(float)StrictMath.sin(a),arm=b.berth.length*.45f;
                float rx=b.x+c*arm-b.anchorX,ry=b.y+s*arm-b.anchorY,d=Physics.length(rx,ry),pull=Math.max(0,d-14)*1.8f/Math.max(1,d);
                float fx=-rx*pull,fy=-ry*pull;
                b.vx+=(force[0]*1.6f+fx-b.vx*.65f)*Physics.DT;b.vy+=(force[1]*1.6f+fy-b.vy*.65f)*Physics.DT;
                b.spin+=((c*fy-s*fx)/Math.max(8,arm)+(force[0]-force[1])*.035f-b.spin*.8f)*Physics.DT;
                b.spin=Physics.clamp(b.spin,-.8f,.8f);b.x+=b.vx*Physics.DT;b.y+=b.vy*Physics.DT;b.yaw+=b.spin*Physics.DT;
            }else{
            float spring=.45f+.9f*Math.max(0,Physics.length(dx,dy)-REACH*.55f);
            b.vx+=(force[0]*1.6f-dx*spring-b.vx*.95f)*Physics.DT;b.vy+=(force[1]*1.6f-dy*spring-b.vy*.95f)*Physics.DT;
            b.x+=b.vx*Physics.DT;b.y+=b.vy*Physics.DT;
            b.spin+=((force[0]-force[1])*.035f-b.yaw*.8f-b.spin*1.2f)*Physics.DT;b.yaw+=b.spin*Physics.DT;
            }
            float height=g.water.height(g,b.x,b.y,g.visualTicks,null);
            float sea=SeaWaves.value(g,b.x,b.y,g.visualTicks,0);
            b.wakeHeave+=((height-sea)*1.35f-b.wakeHeave)*.12f;b.heave=sea+b.wakeHeave;
            b.constrain();
        }
        for(int i=1;i<order.length;i++){int value=order[i],j=i-1;FloatBoat b=boats[value];while(j>=0 && boats[order[j]].y-boats[order[j]].x>b.y-b.x){order[j+1]=order[j];j--;}order[j+1]=value;}
    }
    public float collide(Body body,CollisionSplashes splashes){float hit=0;
        for(FloatBoat b:boats)if(Math.abs(body.x-b.x)<body.half+body.radius+b.berth.length && Math.abs(body.y-b.y)<body.half+body.radius+b.berth.length){
            float vx=body.vx,vy=body.vy,impact=PolygonWater.collide(body,b.hull,false,splashes);hit=Math.max(hit,impact);
            if(impact>0){b.vx=Physics.clamp(b.vx+vx*.08f,-4,4);b.vy=Physics.clamp(b.vy+vy*.08f,-4,4);if(b.swinging()){
                    float torque=((body.x-b.x)*vy-(body.y-b.y)*vx)/Math.max(1,b.berth.length*b.berth.length);
                    b.spin=Physics.clamp(b.spin+torque*.6f,-.8f,.8f);
                }else b.spin=Physics.clamp(b.spin+(vx-vy)*.0005f,-.08f,.08f);}
        }return hit;
    }
}
