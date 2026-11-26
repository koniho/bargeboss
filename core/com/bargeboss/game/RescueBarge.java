package com.bargeboss.game;

/** Flingable rings and swimmer return/boarding, independent of renderer/platform. */
final class RescueBarge {
    static final int RINGS=32,FLIGHT=48,CLIMB=75,HURT=90,VANISH=30;
    static final float RANGE=240,CATCH_RADIUS=38;
    final float[] x=new float[RINGS],y=new float[RINGS],z=new float[RINGS],sx=new float[RINGS],sy=new float[RINGS],tx=new float[RINGS],ty=new float[RINGS];
    final int[] age=new int[RINGS],phase=new int[RINGS],person=new int[RINGS];
    final int[] ring=new int[MontroseRescue.PEOPLE],timer=new int[MontroseRescue.PEOPLE];
    final float[] heading=new float[MontroseRescue.PEOPLE];
    Body boat;boolean towed;int cooldown,throwsMade;
    RescueBarge(){java.util.Arrays.fill(ring,-1);java.util.Arrays.fill(person,-1);}
    void setup(Game g){RescueDeck.prepare();boat=new Body(g.nextId++,2,MontroseHarbor.mapX(1050),MontroseHarbor.mapY(570),-(float)Math.PI/2);boat.ability=BargeAbilities.RESCUE;boat.awaitingPickup=true;boat.tiedX=boat.x;boat.tiedY=boat.y;boat.tiedAngle=boat.angle;g.cargo[0]=boat;}
    static float localX(Body b,float x,float y){return b.x+x*(float)StrictMath.cos(b.angle)-y*(float)StrictMath.sin(b.angle);}
    static float localY(Body b,float x,float y){return b.y+x*(float)StrictMath.sin(b.angle)+y*(float)StrictMath.cos(b.angle);}
    float boardX(){return localX(boat,-12,boat.radius+5);}
    float boardY(){return localY(boat,-12,boat.radius+5);}
    boolean launch(Game g,float targetX,float targetY){
        if(boat==null || !boat.active || !towed || cooldown>0 || boat.speed()>24)return false;
        float dx=targetX-boat.x,dy=targetY-boat.y,d=Physics.length(dx,dy),scale=Math.min(1,RANGE/Math.max(1,d));
        targetX=boat.x+dx*scale;targetY=boat.y+dy*scale;
        if(d<35 || boat.hit(targetX,targetY,5) || !MontroseHarbor.contains(targetX,targetY) || !Towline.clear(g,boat.x,boat.y,targetX,targetY))return false;
        int at=-1;for(int i=0;i<RINGS;i++)if(phase[i]==0){at=i;break;}if(at<0)return false;
        sx[at]=x[at]=boat.x;sy[at]=y[at]=boat.y;tx[at]=targetX;ty[at]=targetY;z[at]=10;age[at]=0;phase[at]=1;person[at]=-1;cooldown=24;throwsMade++;g.haptic(Game.HAPTIC_TICK);return true;
    }
    void step(Game g){
        if(boat==null)return;MontroseRescue r=g.rescue;if(g.tow.connected(boat,g.tug))towed=true;if(cooldown>0)cooldown--;
        for(int i=0;i<RINGS;i++)if(phase[i]>0){age[i]++;
            if(phase[i]==1){float t=Math.min(1,age[i]/(float)FLIGHT);x[i]=sx[i]+(tx[i]-sx[i])*t;y[i]=sy[i]+(ty[i]-sy[i])*t;z[i]=10*(1-t)+4*42*t*(1-t);
                if(age[i]>=FLIGHT){phase[i]=2;age[i]=0;z[i]=.6f;}}
            else if(person[i]<0){
                if(age[i]>900){phase[i]=0;continue;}
                int best=-1;float distance=CATCH_RADIUS;
                for(int j=0;j<MontroseRescue.PEOPLE;j++)if(r.state[j]==0){float d=Physics.length(x[i]-r.x[j],y[i]-r.y[j]);if(d<distance){best=j;distance=d;}}
                if(best>=0){person[i]=best;ring[best]=i;r.state[best]=1;r.found[best]=true;r.flash[best]=45;}
            }
        }
        for(int i=0;i<MontroseRescue.PEOPLE;i++){
            if(r.state[i]==4){seat(r,i);continue;}
            // A struck swimmer thrashes, then blinks out; the rescue carries on without them.
            if(r.state[i]==5){if(++timer[i]>=HURT){r.state[i]=6;g.tell("SWIMMER LOST / "+(g.quota()-g.delivered)+" STILL IN THE WATER");}continue;}
            if(r.state[i]==6){if(timer[i]<HURT+VANISH)timer[i]++;continue;}
            // Slow, deliberate boarding is exempt; treading/swimming casualties are not.
            if(g.tug.hit(r.x[i],r.y[i],3) || boat.hit(r.x[i],r.y[i],1) && (r.state[i]<2 || boat.speed()>8)){
                r.state[i]=5;r.found[i]=true;timer[i]=0;r.flash[i]=HURT;if(ring[i]>=0)phase[ring[i]]=0;g.lost++;g.haptic(Game.HAPTIC_IMPACT);g.sounds.yell();
                RiverCamera cam=g.layout.camera;g.splashes.shake.hit(.6f,cam.screenX(g.tug.vx,g.tug.vy)-cam.screenX(0,0),cam.screenY(g.tug.vx,g.tug.vy)-cam.screenY(0,0),true);
                PlayerPortrait.wince(g);g.tell("SWIMMER HIT! SLOW DOWN NEAR PEOPLE");continue;
            }
            if(r.state[i]==1){int k=ring[i];if(move(r,i,x[k],y[k],12)<2){r.state[i]=2;r.flash[i]=60;}}
            if(r.state[i]==2){int k=ring[i];float d=move(r,i,boardX(),boardY(),10);x[k]=r.x[i];y[k]=r.y[i];
                if(d<6 && boat.speed()<8){r.state[i]=3;timer[i]=0;}}
            if(r.state[i]==3){float t=Math.min(1,++timer[i]/(float)CLIMB);r.x[i]=localX(boat,-12,boat.radius+5-13*t);r.y[i]=localY(boat,-12,boat.radius+5-13*t);
                if(ring[i]>=0){x[ring[i]]=r.x[i];y[ring[i]]=r.y[i];}
                if(timer[i]>=CLIMB){r.state[i]=4;if(ring[i]>=0)phase[ring[i]]=0;g.delivered++;g.score+=100;g.haptic(Game.HAPTIC_SUCCESS);g.tell("SURVIVOR ABOARD / "+g.delivered+" OF "+g.quota());seat(r,i);}
            }
        }
    }
    private float move(MontroseRescue r,int i,float targetX,float targetY,float speed){float dx=targetX-r.x[i],dy=targetY-r.y[i],d=Physics.length(dx,dy);heading[i]=(float)StrictMath.atan2(dy,dx);float step=Math.min(d,speed/60);
        if(d>0){r.x[i]+=dx/d*step;r.y[i]+=dy/d*step;}return d;}
    private void seat(MontroseRescue r,int i){r.x[i]=localX(boat,-14+i%5*7,-8+i/5*5);r.y[i]=localY(boat,-14+i%5*7,-8+i/5*5);}
    float rise(MontroseRescue r,int i){return r.state[i]==4?8:r.state[i]==3?8*Math.min(1,timer[i]/(float)CLIMB):0;}
    void drawRing(Game g,Painter p,int i){float phaseTime=age[i]*.22f;float pitch=phase[i]==1?.65f*(float)StrictMath.sin(phaseTime):.12f*(float)StrictMath.sin(g.visualTicks*.07+i),roll=phase[i]==1?.5f*(float)StrictMath.cos(phaseTime*.8):.08f*(float)StrictMath.cos(g.visualTicks*.08+i);
        RescueRingModel.draw(g,p,x[i],y[i],z[i],phaseTime,pitch,roll);}
}
