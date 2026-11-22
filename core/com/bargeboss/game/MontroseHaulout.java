package com.bargeboss.game;

/** Tow, release and dock-crew handoff into the north-shore haulout well. */
public final class MontroseHaulout {
    public static final float X=MontroseHarbor.mapX(1060),Y=MontroseHarbor.mapY(240),
            APPROACH_Y=MontroseHarbor.mapY(310),ANGLE=-(float)Math.PI/2;
    public boolean towed,warping;
    public int settled;
    public static boolean active(Game g){return g.tutorial==null && g.challengeId==Challenges.HAULOUT;}
    public void setup(Game g){
        g.tug.x=MontroseHarbor.mapX(1090);g.tug.y=MontroseHarbor.mapY(350);g.tug.angle=ANGLE;
        Body b=new Body(g.nextId++,2,MontroseHarbor.mapX(1090),MontroseHarbor.mapY(410),ANGLE,true,true);
        b.voyageJob=Challenges.HAULOUT;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;g.cargo[0]=b;
        g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y+35,Game.TRANSIT_ZOOM*MontroseHarbor.ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    public String status(){return !towed?"DRAG DISABLED BOAT TO TUG / RELEASE TO LINK":warping?"DOCK CREW HAS THE BOAT / KEEP THE WELL CLEAR":"TOW TO N-M HAULOUT / SLOW AND RELEASE OUTSIDE";}
    public static boolean fits(Body b){
        for(int i=0;i<3;i++)if(Math.abs(b.discX(i)-X)+b.radius>27 || Math.abs(b.discY(i)-Y)+b.radius>45)return false;
        return Math.abs(StrictMath.cos(b.angle))<.22f;
    }
    public void step(Game g){
        Body b=g.cargo[0];if(b==null || !b.active || b.deliveryRecorded)return;
        if(g.tow.connected(b,g.tug))towed=true;
        float distance=Physics.length(b.x-X,b.y-Y);
        if(towed && !warping && g.tow.incoming(b)==null && b.speed()<8 && distance<155 && Math.abs(b.x-X)<24
                && MontroseHarbor.clear(b.x,b.y,X,Y)){
            warping=true;g.tell("DOCKMASTER: CREW HAS YOUR LINE. KEEP CLEAR.");g.radio.receiveDockmaster();
        }
        if(warping){
            if(g.tow.incoming(b)!=null)warping=false;
            else {
                float speed=Math.min(12,distance*.8f),denom=Math.max(1,distance);
                b.vx=(X-b.x)/denom*speed;b.vy=(Y-b.y)/denom*speed;
                float heading=ANGLE;if(Math.abs(Physics.angle(heading-b.angle))>Math.PI/2)heading+=(float)Math.PI;
                b.spin=Physics.clamp(Physics.angle(heading-b.angle)*.8f,-.3f,.3f);
            }
        }
        if(warping && fits(b) && distance<4 && b.speed()<3 && Math.abs(b.spin)<.1f && g.tow.incoming(b)==null)settled++;else settled=0;
        if(settled==60){
            b.deliveryRecorded=true;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;b.vx=b.vy=b.spin=0;
            g.delivered=1;g.score+=1200;g.celebrationTicks=90;g.celebrationPoints=1200;g.celebrationX=b.x;g.celebrationY=b.y;
            g.haptic(Game.HAPTIC_SUCCESS);g.tell("DISABLED BOAT SAFE IN THE HAULOUT WELL");
        }
    }
    public static void cue(Game g,Painter p){
        int color=SoftCue.animated(g,g.haulout.warping?0xbb75cfb2:0xbbffe69c);
        for(int side=-1;side<=1;side+=2){float x=X+side*23;
            SoftCue.line(p,x,APPROACH_Y-24,x,APPROACH_Y+24,1.2f,color);
            SoftCue.line(p,x,APPROACH_Y-24,x-side*10,APPROACH_Y-24,1.2f,color);
        }
    }
}
