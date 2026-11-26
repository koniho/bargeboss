package com.bargeboss.game;

/** WAKE PATROL: six speedboats are tearing up the no-wake marina. Run each one down and bump it to hand
 * out a ticket: its engine is cut and it sits dead in the water. Lasso it and tow it to the police
 * boat by the mooring cans; drop it in the impound ring and the police crew ties it into the holding
 * row, where it stays. Speeders roam the fairways, break away when the tug closes in, and juke at the
 * last moment. Each impounded speeder counts. */
final class WakePatrolJob extends MontroseJob {
    static final int SPEEDERS=6;
    static final float CRUISE=78,FLEE=92,ALERT=170;
    /** Open fairway water the speeders hop between (all on verified traffic lanes). */
    static final float[] SPOTS={-82,-30,746,-474,1442,-666,2,-102,300,-300,1100,-570,520,-420};
    /** Spot pairs a speeder can run between in a straight line without crossing docks or shore. */
    static final boolean[][] LINKS=links();
    private static boolean[][] links(){
        int n=SPOTS.length/2;boolean[][] out=new boolean[n][n];
        for(int a=0;a<n;a++)for(int b=0;b<n;b++)if(a!=b)out[a][b]=clear(SPOTS[a*2],SPOTS[a*2+1],SPOTS[b*2],SPOTS[b*2+1]) && clearOfDocks(SPOTS[a*2],SPOTS[a*2+1],SPOTS[b*2],SPOTS[b*2+1]);
        return out;
    }
    static boolean clear(float ax,float ay,float bx,float by){return clearBut(ax,ay,bx,by,null);}
    /** No dock walkway or finger along the line: a hull-sized probe is never pushed by the harbor walls. */
    static boolean clearOfDocks(float ax,float ay,float bx,float by){
        float len=Physics.length(bx-ax,by-ay),angle=(float)StrictMath.atan2(by-ay,bx-ax);
        for(float t=0;t<=len;t+=10){Body probe=new Body(-1,-1,ax+(bx-ax)*t/len,ay+(by-ay)*t/len,angle);float px=probe.x,py=probe.y;
            MontroseHarbor.collide(probe,null);if(Physics.length(probe.x-px,probe.y-py)>.01f)return false;}
        return true;
    }
    /** As clear(), ignoring one berth (an empty slip being the destination). */
    static boolean clearBut(float ax,float ay,float bx,float by,MooredBoats.Berth skip){
        float len=Physics.length(bx-ax,by-ay);
        for(float t=0;t<=len;t+=8){float x=ax+(bx-ax)*t/len,y=ay+(by-ay)*t/len;
            if(!MontroseHarbor.contains(x,y))return false;
            for(MooredBoats.Berth b:MooredBoats.BERTHS)if(b!=skip && !MooredFleet.away(b) && Math.abs(x-b.x)<70 && Math.abs(y-b.y)<70 && Physics.length(x-b.x,y-b.y)<b.length*.5f+20)return false;}
        return true;
    }
    /** Police boat and the impound: a holding row of side-by-side slots south of the mooring cans, and
     * the ring a towed speeder is dropped in. All verified clear by MontroseJobsTest. */
    static final float POLICE_X=-130,POLICE_Y=-95,RING_X=-240,RING_Y=-100,RING_REACH=95,SLOT_Y=-85,SLOT_ANGLE=-(float)Math.PI/2;
    static final float[] SLOT_X={-300,-276,-252,-228,-204,-180};
    static final int WARP=70;
    Body police;final boolean[] impounded=new boolean[SPEEDERS];final int[] warp=new int[SPEEDERS];
    final float[] fromX=new float[SPEEDERS],fromY=new float[SPEEDERS],fromAngle=new float[SPEEDERS];int slotsUsed;final int[] slot=new int[SPEEDERS];
    /** Abrasive custom paint jobs, one per speeder: hot pink, lime, blaze orange, cyan, acid yellow, violet. */
    static final int[] NEON={0xffff2bd6,0xff9dff00,0xffff6a00,0xff00f0ff,0xfff2ff00,0xffa040ff};
    /** Hooligan chatter: idle taunts, being chased, ticketed, towed and impounded. */
    static final String[] TAUNTS={"WOOOO!","CAN'T CATCH ME!","NO WAKE? NO WAY!","FULL SEND!","YEEHAW!","TOO SLOW, GRANDPA!","NICE TUGBOAT, LOL","CATCH ME IF YOU CAN!","SPEED LIMIT? NEVER HEARD OF IT","WATCH THIS!"};
    static final String[] CHASED={"UH OH!","FLOOR IT!","SCATTER!","NOT TODAY, TUGGY!","LOSE HIM!"};
    static final String[] BUSTED={"AW, COME ON!","THIS IS A WARNING, RIGHT?","I WAS GOING 5 KNOTS!","MY DAD OWNS THIS MARINA!"};
    static final String[] TOWED={"I KNOW MY RIGHTS!","THIS IS SO EMBARRASSING","CAN WE TALK ABOUT THIS?"};
    static final String[] JAILED={"I WANT MY LAWYER!","THIS ISN'T OVER!","TOTALLY WORTH IT"};
    final String[] shout=new String[SPEEDERS];final int[] shoutTicks=new int[SPEEDERS],shoutCooldown=new int[SPEEDERS];
    final int[] at=new int[SPEEDERS];
    final Body[] speeders=new Body[SPEEDERS];
    final int[] spot=new int[SPEEDERS],juke=new int[SPEEDERS];
    final boolean[] ticketed=new boolean[SPEEDERS];
    private int seed=17;

    void setup(Game g){
        for(int i=0;i<SPEEDERS;i++){
            int s=(i*3+1)%(SPOTS.length/2);at[i]=s;spot[i]=next(s,-1);
            Body b=new Body(g.nextId++,-1,SPOTS[s*2],SPOTS[s*2+1],i*1.1f,false,true,i%3==0);
            b.traffic=true;b.hullKind=i%3==0?HarborTraffic.RACER:i%6;b.neon=NEON[i];speeders[i]=b;shoutCooldown[i]=40+i*70;
        }
        police=new Body(g.nextId++,-1,POLICE_X,POLICE_Y,(float)Math.PI,false,true);police.traffic=true;police.hullKind=1;
        police.awaitingPickup=true;police.tiedX=police.x;police.tiedY=police.y;police.tiedAngle=police.angle;
        start(g,-60,320,-(float)Math.PI/2);
    }
    private final Body[] live=new Body[SPEEDERS+1];
    /** Free-running hulls: speeders still loose, plus the police boat. Ticketed ones move to the cargo slots. */
    Body[] bodies(){int n=0;for(int i=0;i<SPEEDERS;i++)live[n++]=ticketed[i]?null:speeders[i];live[n]=police;return live;}
    boolean towable(Body b){for(int i=0;i<SPEEDERS;i++)if(speeders[i]==b)return ticketed[i] && !impounded[i] && warp[i]==0;return b!=police && !b.deliveryRecorded;}
    void objective(Game g,Painter canvas){
        for(int i=0;i<SPEEDERS;i++)if(ticketed[i] && !impounded[i] && warp[i]==0 && g.tow.connected(speeders[i],g.tug)){arrow(g,canvas,RING_X,RING_Y,"POLICE IMPOUND");return;}
        Body waiting=null;float wait=Float.MAX_VALUE;
        for(int i=0;i<SPEEDERS;i++)if(ticketed[i] && !impounded[i] && warp[i]==0){float d=Physics.length(speeders[i].x-g.tug.x,speeders[i].y-g.tug.y);if(d<wait){wait=d;waiting=speeders[i];}}
        Body near=null;float best=Float.MAX_VALUE;
        for(int i=0;i<SPEEDERS;i++){Body b=speeders[i];if(!b.active || ticketed[i])continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;near=b;}}
        if(waiting!=null && (near==null || wait<best))arrow(g,canvas,waiting.x,waiting.y,"TOW TO POLICE");
        else if(near!=null)arrow(g,canvas,near.x,near.y,"SPEEDER");
    }
    String status(Game g){
        for(int i=0;i<SPEEDERS;i++)if(ticketed[i] && !impounded[i] && g.tow.connected(speeders[i],g.tug))return "TOW IT TO THE POLICE BOAT BY THE MOORING CANS / DROP IN THE RING";
        for(int i=0;i<SPEEDERS;i++)if(ticketed[i] && !impounded[i] && warp[i]==0)return "TICKETED SPEEDER DEAD IN THE WATER / LASSO IT AND TOW TO POLICE";
        return "BUMP THE SPEEDERS TO TICKET THEM / "+g.delivered+" OF "+SPEEDERS+" IMPOUNDED";
    }
    private int random(int n){seed=seed*1103515245+12345;return Math.floorMod(seed>>>8,n);}
    /** A random linked spot from s (avoiding `not` when possible). */
    private int next(int s,int not){int n=SPOTS.length/2,count=0;for(int k=0;k<n;k++)if(LINKS[s][k] && k!=not)count++;if(count==0)return s==0?1:0;int pick=random(count);for(int k=0;k<n;k++)if(LINKS[s][k] && k!=not && pick--==0)return k;return s;}

    private void yell(int i,String[] lines,boolean force){
        if(!force && (shoutTicks[i]>0 || shoutCooldown[i]>0))return;
        shout[i]=lines[random(lines.length)];shoutTicks[i]=120;shoutCooldown[i]=180+random(200);
    }
    void step(Game g){
        super.step(g);
        for(int i=0;i<SPEEDERS;i++){
            if(shoutTicks[i]>0)shoutTicks[i]--;if(shoutCooldown[i]>0)shoutCooldown[i]--;
            Body b=speeders[i];if(!b.active)continue;
            // Hooligan chatter: taunts while loose, panic when the tug closes in.
            if(!ticketed[i]){float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);yell(i,d<ALERT?CHASED:TAUNTS,false);}
            else if(!impounded[i] && warp[i]==0 && g.tow.connected(b,g.tug))yell(i,TOWED,false);
            if(!ticketed[i]){
                if(!Physics.touching(g.tug,b,4))continue;
                // Ticket: engine cut, the speeder sits dead in the water (a cargo slot, so it can be lassoed).
                ticketed[i]=true;b.vx*=.3f;b.vy*=.3f;b.spin*=.3f;
                b.awaitingPickup=true;b.tiedX=b.x;b.tiedY=b.y;b.tiedAngle=b.angle;g.cargo[i]=b;
                RiverCamera c=g.layout.camera;g.splashes.shake.hit(.45f,c.screenX(b.x-g.tug.x,b.y-g.tug.y)-c.screenX(0,0),c.screenY(b.x-g.tug.x,b.y-g.tug.y)-c.screenY(0,0),true);
                int bonus=Math.round(g.tug.speed()*2);g.score+=150+bonus;g.haptic(Game.HAPTIC_SUCCESS);
                pop(b.x,b.y,"TICKETED! +"+(150+bonus));yell(i,BUSTED,true);g.tell("TICKETED! NOW TOW IT TO THE POLICE BOAT");
                continue;
            }
            if(impounded[i])continue;
            if(warp[i]>0){
                // Police crew walks it into its holding slot.
                float t=Math.min(1,++warp[i]/(float)WARP),e=t*t*(3-2*t),sx=SLOT_X[slot[i]];
                b.x=b.tiedX=fromX[i]+(sx-fromX[i])*e;b.y=b.tiedY=fromY[i]+(SLOT_Y-fromY[i])*e;b.angle=b.tiedAngle=fromAngle[i]+Physics.angle(SLOT_ANGLE-fromAngle[i])*e;
                b.vx=b.vy=b.spin=0;b.awaitingPickup=true;
                if(t>=1){impounded[i]=true;secure(b,sx,SLOT_Y,SLOT_ANGLE);yell(i,JAILED,true);
                    score(g,sx,SLOT_Y,400,g.delivered+1==SPEEDERS?"LAST ONE IMPOUNDED! WAKE ZONE CLEAR":"IMPOUNDED!");}
                continue;
            }
            if(!b.awaitingPickup && Physics.length(b.x-RING_X,b.y-RING_Y)<RING_REACH && b.speed()<35){
                warp[i]=1;slot[i]=slotsUsed++;fromX[i]=b.x;fromY[i]=b.y;fromAngle[i]=b.angle;g.tow.detachBody(g,b);
                g.tell("POLICE: WE'LL TAKE IT FROM HERE");g.radio.receiveDockmaster();
            }
        }
    }
    void motion(Game g){
        for(int i=0;i<SPEEDERS;i++){
            Body b=speeders[i];if(!b.active || ticketed[i])continue;
            float tx,ty,speed;
            {
                float dx=b.x-g.tug.x,dy=b.y-g.tug.y,d=Math.max(1,Physics.length(dx,dy));
                tx=SPOTS[spot[i]*2];ty=SPOTS[spot[i]*2+1];speed=CRUISE;
                if(Physics.length(tx-b.x,ty-b.y)<60){int from=at[i];at[i]=spot[i];spot[i]=next(spot[i],from);tx=SPOTS[spot[i]*2];ty=SPOTS[spot[i]*2+1];}
                if(d<ALERT){
                    // Break away: pick the roaming spot that leads furthest from the tug, juking when it is close.
                    float best=-1;int pick=spot[i];
                    for(int s=0;s<SPOTS.length/2;s++){if(s!=at[i] && !LINKS[at[i]][s])continue;float sx=SPOTS[s*2],sy=SPOTS[s*2+1],away=Physics.length(sx-g.tug.x,sy-g.tug.y)-Physics.length(sx-b.x,sy-b.y)*.6f;if(away>best){best=away;pick=s;}}
                    if(pick!=spot[i]){spot[i]=pick;}tx=SPOTS[pick*2];ty=SPOTS[pick*2+1];speed=FLEE;
                    if(d<70 && juke[i]==0)juke[i]=24;
                }
            }
            if(juke[i]>0)juke[i]--;
            float desired=(float)StrictMath.atan2(ty-b.y,tx-b.x)+(juke[i]>12?.9f:juke[i]>0?-.9f:0),error=Physics.angle(desired-b.angle);
            b.spin+=Physics.clamp(error*5-b.spin*2.4f,-8,8)*Physics.DT;
            float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle),go=speed*Math.max(.3f,1-Math.abs(error)*.4f);
            float dvx=c*go-b.vx,dvy=s*go-b.vy,f=Physics.length(dvx,dvy),max=130*Physics.DT;
            if(f>max){dvx*=max/f;dvy*=max/f;}
            b.vx+=dvx;b.vy+=dvy;
            Physics.integrate(b);MontroseHarbor.collide(g,b,g.splashes);
        }
        if(police!=null){police.x=police.tiedX;police.y=police.tiedY;police.angle=police.tiedAngle;police.vx=police.vy=police.spin=0;}
        collide(g,bodies(),g.splashes);
    }
    void water(Game g,Painter p){
        boolean towing=false;
        for(int i=0;i<SPEEDERS;i++){
            Body b=speeders[i];if(!b.active)continue;
            if(!ticketed[i])continue; // Loose speeders glow from their own ripple layer (CruiserView.underglow).
            else if(!impounded[i] && warp[i]==0){if(g.tow.connected(b,g.tug))towing=true;else ring(g,p,b.x,b.y,30,0xffffd166,.7f);}
        }
        ring(g,p,RING_X,RING_Y,RING_REACH*.7f,0xff5aa8ff,towing?1:.45f);
    }
    /** Comic speech bubble over a speeder: black-outlined white box, tail to the boat, pops in and fades. */
    private static void bubble(Game g,Painter p,Body b,String text,int age,int left){
        RiverCamera c=g.layout.camera;float bx=c.screenX(b.x,b.y),by=c.screenY(b.x,b.y)-(26*ArticulatedModel.Z_SCALE+WaterSurface.lift(g,b))*c.scale-22;
        if(bx<-150 || bx>g.layout.width+150 || by<-40 || by>g.layout.height+40)return;
        float pop=age<5?.6f+age*.08f:age<9?1.15f-(age-5)*.0375f:1,size=12*pop,w=Lettering.width(text,size)+16,h=size+12,x=bx-w*.5f,y=by-h;
        float a=Physics.clamp(left/12f,0,1);int ink=SoftCue.alpha(0xff101014,a),paper=SoftCue.alpha(0xfffffbf0,a);
        p.poly(new float[]{bx-7,y+h-1,bx+7,y+h-1,bx+2,y+h+10},ink);
        p.rect(x-2,y-2,w+4,h+4,ink);p.rect(x,y,w,h,paper);
        p.poly(new float[]{bx-5,y+h-1,bx+5,y+h-1,bx+2,y+h+7},paper);
        Lettering.center(p,text,bx,y+5,size,SoftCue.alpha(b.neon!=0?darken(b.neon):0xff101014,a));
    }
    private static int darken(int c){int r=((c>>16)&255)*55/100,gg=((c>>8)&255)*55/100,bb=(c&255)*55/100;return 0xff000000|(r<<16)|(gg<<8)|bb;}
    void overlay(Game g,Painter p){
        // Police light bar: alternating red and blue strobes over the police boat.
        RiverCamera c=g.layout.camera;float sx=c.screenX(POLICE_X,POLICE_Y),sy=c.screenY(POLICE_X,POLICE_Y)-30*ArticulatedModel.Z_SCALE*c.scale;
        boolean red=(g.visualTicks/8)%2==0;float s=c.scale;
        p.material(sx-5*s,sy,22*s,0,0,16*s,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xffff3040,red?.95f:.25f));
        p.material(sx+5*s,sy,22*s,0,0,16*s,EffectMaterial.GLOW,0,1,SoftCue.alpha(0xff3a7bff,red?.25f:.95f));
        Lettering.center(p,"POLICE",sx,sy-14*s,Physics.clamp(9*s,10,14),0xffcfe3ff);
        for(int i=0;i<SPEEDERS;i++)if(shoutTicks[i]>0 && speeders[i].active)bubble(g,p,speeders[i],shout[i],120-shoutTicks[i],shoutTicks[i]);
        super.overlay(g,p);
    }
}
