package com.bargeboss.game;

/** LOOSE BOATS: a squall snapped four boats off their lines and the wind is pushing them across the
 * marina. Lasso each drifter, tow it to its glowing empty slip and let go; the dock crew warps it in.
 * Drifters keep moving while you work, so the order you catch them in matters. */
final class LooseBoatsJob extends MontroseJob {
    static final int BOATS=4;
    /** Gusting wind (world units/s^2) toward the north-east shore. */
    static final float WIND_X=5.5f,WIND_Y=-4f,CAPTURE=70;
    static final int WARP=70;
    final MooredBoats.Berth[] slips=new MooredBoats.Berth[BOATS];
    final int[] warp=new int[BOATS];final Body[] boats=new Body[BOATS];
    final float[] fromX=new float[BOATS],fromY=new float[BOATS],fromAngle=new float[BOATS];
    private static final float[] START={-60,-90,420,-380,820,-500,1150,-600};

    void setup(Game g){
        int found=0;
        // Empty fairway slips (their boats are the drifters), nearest the main fairway first.
        for(MooredBoats.Berth b:MooredBoats.BERTHS){
            if(found==BOATS)break;
            if(!MooredFleet.away(b) || b.mooring>=0 || MontroseRescue.reserved(g,b) || MontroseHaulout.reserved(b))continue;
            if(b.y<-1000 || b.y>-150 || b.x<0 || b.x>1700)continue;
            slips[found++]=b;
        }
        for(int i=0;i<BOATS;i++){
            MooredBoats.Berth slip=slips[i]==null?MooredBoats.BERTHS[i]:slips[i];slips[i]=slip;
            Body b=new Body(g.nextId++,2,START[i*2],START[i*2+1],i*1.7f,false,true);
            b.hullKind=slip.kind;b.vx=WIND_X*3;b.vy=WIND_Y*3;g.cargo[i]=boats[i]=b;
        }
        start(g,-80,180,-(float)Math.PI/2);
    }
    void objective(Game g,Painter canvas){
        for(int i=0;i<BOATS;i++)if(boats[i]!=null && !boats[i].deliveryRecorded && g.tow.connected(boats[i],g.tug)){arrow(g,canvas,slips[i].x,slips[i].y,"EMPTY SLIP");return;}
        Body near=null;float best=Float.MAX_VALUE;
        for(int i=0;i<BOATS;i++){Body b=boats[i];if(b==null || b.deliveryRecorded || warp[i]>0)continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;near=b;}}
        if(near!=null)arrow(g,canvas,near.x,near.y,"DRIFTER");
    }
    String status(Game g){
        for(int i=0;i<BOATS;i++)if(g.cargo[i]!=null && g.tow.connected(g.cargo[i],g.tug))return "TOW IT TO THE GLOWING SLIP / RELEASE TO DOCK";
        return "SQUALL! LASSO THE DRIFTING BOATS / "+g.delivered+" OF "+BOATS+" HOME";
    }
    void step(Game g){
        super.step(g);
        for(int i=0;i<BOATS;i++){
            Body b=g.cargo[i];if(b==null || !b.active || b.deliveryRecorded)continue;
            MooredBoats.Berth slip=slips[i];
            if(warp[i]>0){
                // Dock crew hauls it in: ease into the slip pose.
                float t=Math.min(1,++warp[i]/(float)WARP),e=t*t*(3-2*t);
                b.x=fromX[i]+(slip.x-fromX[i])*e;b.y=fromY[i]+(slip.y-fromY[i])*e;b.angle=fromAngle[i]+Physics.angle(slip.angle-fromAngle[i])*e;b.vx=b.vy=b.spin=0;
                b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
                if(t>=1){secure(b,slip.x,slip.y,slip.angle);score(g,slip.x,slip.y,600+Math.max(0,b.health-50)*8,"BACK IN ITS SLIP");}
                continue;
            }
            boolean towed=g.tow.connected(b,g.tug);
            if(!towed && Physics.length(b.x-slip.x,b.y-slip.y)<CAPTURE && b.speed()<30){
                warp[i]=1;fromX[i]=b.x;fromY[i]=b.y;fromAngle[i]=b.angle;g.tow.detachBody(g,b);g.tell("DOCK CREW HAS IT / NICE TOW");
            }
        }
    }
    void motion(Game g){
        // The squall gusts: every untowed drifter is shoved downwind.
        float gust=1+.6f*(float)StrictMath.sin(ticks*.02f);
        for(int i=0;i<BOATS;i++){
            Body b=g.cargo[i];if(b==null || !b.active || b.deliveryRecorded || b.awaitingPickup || g.tow.connected(b,g.tug))continue;
            b.vx+=WIND_X*gust*Physics.DT;b.vy+=WIND_Y*gust*Physics.DT;b.spin+=.04f*gust*Physics.DT;
        }
    }
    boolean towable(Body b){for(int i=0;i<BOATS;i++)if(warp[i]>0 && boats[i]==b)return false;return !b.deliveryRecorded;}
    void water(Game g,Painter p){
        for(int i=0;i<BOATS;i++){
            Body b=g.cargo[i];if(b==null || b.deliveryRecorded)continue;
            boolean towed=g.tow.connected(b,g.tug);
            ring(g,p,slips[i].x,slips[i].y,34,towed?0xff6dffb0:0xff8fd0ff,towed?1:.55f);
            if(!towed)ring(g,p,b.x,b.y,30,0xffffb347,.7f);
        }
    }
}
