package com.bargeboss.game;

/** Bounded fixed-step Montrose rescue rules; shared by every native host. */
public final class MontroseRescue {
    final SwimmerModel swimmerModel=new SwimmerModel();
    public static final int PEOPLE=20,THROW_TICKS=36;
    public static final float THROW_RANGE=135;
    public boolean rafted,closeCamera;
    boolean touring;private int tourLeg;private int radioPhase,replyTicks,replyChoice;
    static final float RAFT_X=MontroseHarbor.mapX(2350),RAFT_Y=MontroseHarbor.mapY(-1530);
    static String name(int slot){return slot==0?"RUMRUNNER 2":"NEVER WETTER";}
    void distress(Game g){g.montroseWeather.evening=true;g.montroseWeather.storm=true;touring=false;radioPhase=1;replyTicks=0;
        g.dialogue.showFrom(g,RadioAudio.CREW,"CHAS / RUMRUNNER 2","HELP! THREE KIDS ABOARD!",
            "RUMRUNNER 2 AND NEVER WETTER. WEATHER'S TURNING!",
            "WHY ARE YOU OUT THERE IN THIS WEATHER?","STAY CALM, CHAS. WHAT HAPPENED?");
    }
    void radioStep(Game g){
        if(!g.dialogue.crewConversation() || g.dialogue.result()<0)return;
        if(replyTicks++==0)replyChoice=g.dialogue.result();
        if(replyTicks<36)return;replyTicks=0;
        if(radioPhase==1){radioPhase=2;
            g.dialogue.showFrom(g,RadioAudio.CREW,"CHAS / RUMRUNNER 2","BOTH BATTERIES ARE DEAD!",
                "THE MUSIC WAS TOO LOUD! THE KIDS ARE SCARED!",
                "GREAT SOUND SYSTEM. TERRIBLE LIFE PLAN.","LIFE JACKETS ON. I'M COMING TO GET YOU.");
        }else if(radioPhase==2){radioPhase=3;
            g.dialogue.showFrom(g,RadioAudio.CREW,"CHAS / RUMRUNNER 2",replyChoice==0?"YES! BAD PLAYLIST! PLEASE HURRY!":"JACKETS ON! PLEASE HURRY!",
                "JUST PAST THE HORN, BY THE LIGHTHOUSE. BOTH BOATS!",
                "STAY ABOARD. I'LL SPLIT THE RAFT AND TOW YOU HOME.");
        }else {g.dialogue.close(g);radioPhase=0;g.tell("RESCUE RUMRUNNER 2 AND NEVER WETTER / RETURN TO DOCK C");}
    }
    private static final float[][] TOUR={{1060,550},{870,570},{330,650},{330,950},{440,1160},{453,1370},{1800,1360},{2370,350}};
    float[] tourTarget(Game g){float[] p=TOUR[tourLeg];return new float[]{MontroseHarbor.mapX(p[0]),MontroseHarbor.mapY(p[1])};}

    public final boolean[] towed=new boolean[2],warping=new boolean[2];
    public final int[] settled=new int[2];
    public final float[] x=new float[PEOPLE],y=new float[PEOPLE];
    public final int[] state=new int[PEOPLE],flash=new int[PEOPLE]; // 0 waiting, 1 buoy secured
    public int aiming=-1,flying=-1,flightTicks,cooldown;
    public float fromX,fromY,toX,toY;
    public boolean failed;
    public static boolean cruisers(Game g){return g.tutorial==null && g.challengeId==Challenges.RAFT_RESCUE;}
    public static boolean swimmers(Game g){return g.tutorial==null && g.challengeId==Challenges.SWIMMER_RESCUE;}
    public static boolean active(Game g){return cruisers(g)||swimmers(g);}
    public static boolean reserved(Game g,MooredBoats.Berth b){return g.montroseStage()&&isTarget(b);}
    private static boolean isTarget(MooredBoats.Berth b){return b.row==8 && (b.station==345 && b.side==1 || b.station==405 && b.side==-1);}
    public static MooredBoats.Berth berth(int slot){for(MooredBoats.Berth b:MooredBoats.BERTHS)if(b.row==8 && b.station==(slot==0?345:405) && b.side==(slot==0?1:-1))return b;throw new IllegalStateException("Missing Dock C rescue berth");}
    public static String slip(int slot){return slot==0?"C-38":"C-22";}
    public void setup(Game g){setup(g,true);}
    public void setupContinuation(Game g){setup(g,false);}
    private void setup(Game g,boolean reposition){
        if(cruisers(g)){
            rafted=true;
            for(int i=0;i<2;i++){Body b=new Body(g.nextId++,i,RAFT_X+i*22,RAFT_Y,-(float)Math.PI/2,true,true);
                b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;g.cargo[i]=b;}
            if(reposition){g.tug.x=RAFT_X;g.tug.y=RAFT_Y-90;g.tug.angle=-(float)Math.PI/2;}
        }else{
            // Outside the southern seawall, clear of the entrance and traffic lane.
            for(int i=0;i<PEOPLE;i++){int col=i%10,row=i/10;
                x[i]=MontroseHarbor.mapX(800+col*22);y[i]=MontroseHarbor.mapY(1220+col*4+row*20);}
            if(reposition){g.tug.x=MontroseHarbor.mapX(700);g.tug.y=MontroseHarbor.mapY(1320);g.tug.angle=-.2f;}
        }
        if(reposition)g.layout=Layout.focused(g.layout.width,g.layout.height,g.tug.x,g.tug.y,Game.TRANSIT_ZOOM*MontroseHarbor.ZOOM_MULTIPLIER,g.layout.width*.5f,g.layout.height*.52f);
    }
    public String status(Game g){
        if(touring)return "GET TO KNOW THE AREA / HEAD OUT TOWARD THE BEACH";
        if(cruisers(g))return rafted?"APPROACH / TAP THE RAFT LINE TO SEPARATE":"TOW EACH CRUISER TO DOCK C / RELEASE IN ITS SLIP";
        if(failed)return "SWIMMER STRUCK / RESTART THE RESCUE";
        if(flying>=0)return "BUOY AWAY / KEEP CLEAR OF SWIMMERS";
        return "TAP A SWIMMER TO THROW A BUOY / KEEP THE HULL CLEAR";
    }
    public boolean tapRaft(Game g,float px,float py){
        if(!cruisers(g)||!rafted)return false;
        Body a=g.cargo[0],b=g.cargo[1];float mx=(a.x+b.x)*.5f,my=(a.y+b.y)*.5f;
        if(Physics.length(px-mx,py-my)>35)return false;
        if(Physics.length(g.tug.x-mx,g.tug.y-my)>125){g.tell("GET CLOSER TO RELEASE THE RAFT LINE");return true;}
        rafted=false;g.haptic(Game.HAPTIC_SUCCESS);g.tell("RAFT LINE RELEASED / TOW EACH CRUISER HOME");return true;
    }
    public int swimmerAt(Game g,float px,float py){if(!swimmers(g)||failed||flying>=0)return -1;
        int best=-1;float distance=18;
        for(int i=0;i<PEOPLE;i++)if(state[i]==0){float d=Physics.length(px-x[i],py-y[i]);if(d<distance){distance=d;best=i;}}
        return best;
    }
    public void release(Game g,float px,float py){int target=aiming;aiming=-1;if(target<0)return;
        if(Physics.length(px-x[target],py-y[target])>24){g.tell("AIM AT A SWIMMER / RELEASE TO THROW");return;}
        float distance=Physics.length(g.tug.x-x[target],g.tug.y-y[target]);
        if(distance>THROW_RANGE || !MontroseHarbor.clear(g.tug.x,g.tug.y,x[target],y[target])){g.tell("GET WITHIN BUOY RANGE / KEEP WATER CLEAR");return;}
        if(g.tug.speed()>24){g.tell("SLOW DOWN BEFORE THROWING A BUOY");return;}
        if(cooldown>0)return;
        fromX=g.tug.x;fromY=g.tug.y;toX=x[target];toY=y[target];flightTicks=0;flying=target;g.haptic(Game.HAPTIC_TICK);
    }
    public boolean fits(Body b){MooredBoats.Berth target=berth(b.berth);float dx=b.x-target.x,dy=b.y-target.y,c=(float)StrictMath.cos(target.angle),s=(float)StrictMath.sin(target.angle);
        return Math.abs(dx*c+dy*s)<8 && Math.abs(-dx*s+dy*c)<9 && Math.abs(StrictMath.sin(b.angle-target.angle))<.18f;
    }
    public void step(Game g){
        if(touring){
            float[] target=tourTarget(g);if(tourLeg<TOUR.length-1 && Physics.length(g.tug.x-target[0],g.tug.y-target[1])<100)tourLeg++;
            if(MontroseHarbor.planX(g.tug.x)>2250 && MontroseHarbor.planY(g.tug.y)<550)distress(g);
            return;
        }
        if(cruisers(g)){
            for(int i=0;i<2;i++){Body b=g.cargo[i];if(b==null||b.deliveryRecorded)continue;
                if(g.tow.connected(b,g.tug))towed[i]=true;
                MooredBoats.Berth target=berth(i);
                float distance=Physics.length(b.x-target.x,b.y-target.y);
                if(!rafted && towed[i] && !warping[i] && g.tow.incoming(b)==null && distance<115 && b.speed()<8
                        && MontroseHarbor.clear(b.x,b.y,target.x,target.y)){
                    warping[i]=true;g.tell("DOCK CREW WARPING CRUISER INTO "+slip(i));
                }
                if(warping[i]){
                    if(g.tow.incoming(b)!=null)warping[i]=false;
                    else {float speed=Math.min(10,distance*.7f),denom=Math.max(1,distance);
                        b.vx=(target.x-b.x)/denom*speed;b.vy=(target.y-b.y)/denom*speed;
                        float angle=target.angle;if(Math.abs(Physics.angle(angle-b.angle))>Math.PI/2)angle+=(float)Math.PI;
                        b.spin=Physics.clamp(Physics.angle(angle-b.angle)*.8f,-.3f,.3f);
                    }
                }
                if(!rafted && towed[i] && fits(b) && b.speed()<5 && Math.abs(b.spin)<.12f && g.tow.incoming(b)==null)settled[i]++;else settled[i]=0;
                if(settled[i]==60){b.deliveryRecorded=true;b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;
                    g.delivered++;g.score+=1200;g.haptic(Game.HAPTIC_SUCCESS);g.tell("CRUISER SAFE / DOCK "+slip(i));}
            }return;
        }
        float nearest=Float.MAX_VALUE;
        for(int i=0;i<PEOPLE;i++){if(flash[i]>0)flash[i]--;if(state[i]==0)nearest=Math.min(nearest,Physics.length(g.tug.x-x[i],g.tug.y-y[i]));}
        if(nearest<240)closeCamera=true;else if(nearest>330)closeCamera=false;
        if(cooldown>0)cooldown--;
        if(failed)return;
        // Fixed-step hull contact has a small safety margin and a non-graphic failure.
        for(int i=0;i<PEOPLE;i++)if(g.tug.hit(x[i],y[i],4)){failed=true;g.lost++;g.haptic(Game.HAPTIC_IMPACT);g.tell("SWIMMER STRUCK / RESCUE FAILED");return;}
        if(flying>=0 && ++flightTicks>=THROW_TICKS){int i=flying;flying=-1;state[i]=1;flash[i]=60;g.delivered++;g.score+=100;cooldown=12;g.haptic(Game.HAPTIC_SUCCESS);g.tell("BUOY SECURED / "+g.delivered+" OF 20 SAFE");}
    }
}
