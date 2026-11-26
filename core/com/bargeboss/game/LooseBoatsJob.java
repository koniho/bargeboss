package com.bargeboss.game;

/** LOOSE BOATS: a squall snapped four boats off their lines and the wind is pushing them across the
 * marina. Lasso each drifter, tow it to its glowing empty slip and let go; the dock crew warps it in.
 * Drifters keep moving while you work, so the order you catch them in matters. */
final class LooseBoatsJob extends MontroseJob {
    static final int BOATS=4;
    /** Gusting wind (world units/s^2) down the main fairway toward the inlet. */
    static final float WIND_X=-3.2f,WIND_Y=2.2f,CAPTURE=140;
    static final int WARP=70;
    final MooredBoats.Berth[] slips=new MooredBoats.Berth[BOATS];
    final int[] warp=new int[BOATS];final Body[] boats=new Body[BOATS];
    final float[] fromX=new float[BOATS],fromY=new float[BOATS],fromAngle=new float[BOATS];
    private static final float[] START={1300,-660,1000,-560,700,-480,400,-400};

    void setup(Game g){
        // Empty slips (their boats are the drifters) that open straight onto the fairway, best first.
        float[] best=new float[BOATS];java.util.Arrays.fill(best,Float.MAX_VALUE);
        for(MooredBoats.Berth b:MooredBoats.BERTHS){
            if(!MooredFleet.away(b) || b.mooring>=0 || MontroseRescue.reserved(g,b) || MontroseHaulout.reserved(b))continue;
            float reach=fairway(b);
            for(int k=0;k<BOATS;k++)if(reach<best[k]){
                for(int m=BOATS-1;m>k;m--){best[m]=best[m-1];slips[m]=slips[m-1];}
                best[k]=reach;slips[k]=b;break;
            }
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
    /** Distance from a slip to the nearest open fairway point it can be reached from in a straight line. */
    static float fairway(MooredBoats.Berth b){
        float best=Float.MAX_VALUE;
        for(float[] lane:new float[][]{HarborTraffic.INLET_RUN,HarborTraffic.NORTH_RUN,HarborTraffic.MOUTH})
            for(int i=0;i+3<lane.length;i+=2){
                float ax=lane[i],ay=lane[i+1],bx=lane[i+2],by=lane[i+3],len=Physics.length(bx-ax,by-ay);
                for(float t=0;t<=len;t+=40){
                    float x=ax+(bx-ax)*t/len,y=ay+(by-ay)*t/len,d=Physics.length(b.x-x,b.y-y);
                    if(d<best && d<260 && WakePatrolJob.clearBut(x,y,b.x,b.y,b))best=d;
                }
            }
        return best;
    }
    String status(Game g){
        for(int i=0;i<BOATS;i++)if(g.cargo[i]!=null && g.tow.connected(g.cargo[i],g.tug))return "TOW IT INTO THE GLOWING SLIP RING / THE CREW TAKES IT";
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
            // Tow it into the ring: the dock crew grabs it there, line on or off.
            if(Physics.length(b.x-slip.x,b.y-slip.y)<CAPTURE && b.speed()<35){
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
