package com.bargeboss.game;

/** The tag circuit: a loop canal around two islands with a shortcut channel through the middle,
 * two lift bridges on the loop and a lock (two gates) in the shortcut. Every bridge and gate opens over
 * one second when any boat comes near and closes again once the water around it is clear; while
 * closed it blocks the channel, so boats queue.
 *
 * Two games on it, each with a random field of neon NPC boats:
 * - TAG: one boat is IT. IT tags by touching another boat (no tag-backs for two seconds; the new IT
 *   is held for a second). Whoever is IT when the clock runs out loses.
 * - CHASE: the tug is always the chaser and must catch every NPC before the clock runs out.
 * Fixed step and deterministic (seeded per game). Drawing lives in TagArenaView. */
final class TagArena {
    static final float W=2000,H=1300,LANE=180;
    /** Water rectangles [minX,minY,maxX,maxY]: the loop's four legs and the shortcut. */
    static final float[][] WATER={{0,0,W,LANE},{0,H-LANE,W,H},{0,LANE,LANE,H-LANE},{W-LANE,LANE,W,H-LANE},{LANE,560,W-LANE,740}};
    /** Land inside the loop: the north and south islands. */
    static final float[][] ISLANDS={{LANE,LANE,W-LANE,560},{LANE,740,W-LANE,H-LANE}};
    /** Gates: two lift bridges across the loop's top and bottom legs, then the lock's west and east gates. */
    static final int BRIDGE_N=0,BRIDGE_S=1,LOCK_W=2,LOCK_E=3,GATES=4;
    static final float[] GATE_X={1000,1000,900,1100},GATE_Y0={0,H-LANE,560,560},GATE_Y1={LANE,H,740,740};
    static final int OPEN_TICKS=60,HOLD_TICKS=120;
    static final float TRIGGER=170;
    /** Centreline nodes for NPC routing: loop corners, leg midpoints and the shortcut's middle. */
    static final float[] NODE_X={90,1000,1910,1910,1910,1000,90,90,1000},NODE_Y={90,90,90,650,1210,1210,1210,650,650};
    static final int[][] LINKS={{1,7},{0,2},{1,3},{2,4,8},{3,5},{4,6},{5,7},{6,0,8},{7,3}};

    static final int TAG=0,CHASE=1;
    final int mode;final ArenaCourse course;
    final IslandFootbridge[] footbridges;
    final float[] open;final int[] clear,openTicks;
    Body[] npcs=new Body[0];
    int[] goal=new int[0],immune=new int[0],held=new int[0],shoutTicks=new int[0],shoutCooldown=new int[0];
    boolean[] caught=new boolean[0];String[] shout=new String[0];
    /** Fuel for flat-out running (1 = full). Fleeing hard near the threat drains it over FUEL_BURN ticks;
     * an empty tank sputters at SPUTTER speed for SPUTTER_TICKS, opening a catch window. */
    float[] fuel=new float[0];int[] sputter=new int[0];
    /** Rammed boats are stunned (dizzy, drifting) for STUN_TICKS: easy to lasso. */
    int[] stunned=new int[0];static final int STUN_TICKS=100;static final float STUN_SPEED=8;
    /** CHASE: lassoed boats must be towed into the corral; corralled boats park on its slots. */
    boolean[] corralled=new boolean[0];float corralX,corralY;float[] penX=new float[0],penY=new float[0];int[] penCount=new int[0];static final float CORRAL_REACH=85;int combo,comboTicks;
    /** TAG: how long the tug has held a boat on its line (it snaps after HOLD_SNAP); an angry IT runs faster. */
    int holdTicks,angry;static final int HOLD_SNAP=300;
    /** Power-up buoys and the tug's active power-ups (ticks left). */
    static final int NITRO=0,POWER_SPAWN=540;
    float[] powerX=new float[3],powerY=new float[3];int[] powerType=new int[3];boolean[] powerLive=new boolean[3];int powerTimer=240;
    int nitro;
    static final int FUEL_BURN=270,FUEL_REFILL=10*60,SPUTTER_TICKS=150;static final float SPUTTER=.45f;
    /** TAG: index of IT (-1 = the tug, else an NPC). */
    int it;int ticks,limit,playerItTicks,tags;boolean finished,won;
    private int seed;

    TagArena(int mode,int seed){this(mode,seed,new ArenaCourse.Loop());}
    TagArena(int mode,int seed,ArenaCourse course){this.mode=mode;this.course=course;this.seed=seed*1103515245+12345;int n=course.gates();open=new float[n];clear=new int[n];openTicks=new int[n];footbridges=course instanceof ArenaCourse.Islands?new IslandFootbridge[n]:new IslandFootbridge[0];for(int k=0;k<footbridges.length;k++)footbridges[k]=new IslandFootbridge(course,k);if(footbridges.length>0)IslandArt.prepare();}
    int random(int n){seed=seed*1103515245+12345;return Math.floorMod(seed>>>8,n);}

    void setup(Game g){
        int count=3+random(3);
        npcs=new Body[count];goal=new int[count];immune=new int[count];held=new int[count];caught=new boolean[count];
        shout=new String[count];shoutTicks=new int[count];shoutCooldown=new int[count];fuel=new float[count];sputter=new int[count];java.util.Arrays.fill(fuel,1);
        stunned=new int[count];corralled=new boolean[count];
        float[] nx=course.nodeX,ny=course.nodeY;int nodes=nx.length,startNode=random(nodes);
        for(int i=0;i<count;i++){
            int node=(startNode+Math.max(2,nodes/(count+1))*(i+1))%nodes;
            Body b=new Body(g.nextId++,-1,nx[node]+random(40)-20,ny[node]+random(40)-20,random(628)/100f,false,true,i%3==0);
            b.traffic=true;b.hullKind=i%3==0?HarborTraffic.RACER:(i*5+1)%6;b.neon=WakePatrolJob.NEON[i%WakePatrolJob.NEON.length];
            int[] out=course.links[node];npcs[i]=b;goal[i]=out.length==0?node:out[random(out.length)];shoutCooldown[i]=60+random(200);
            // NPC hulls live in the cargo slots so the shared lasso and tow line work on them.
            g.cargo[i]=b;
        }
        g.tug.x=nx[startNode];g.tug.y=ny[startNode];g.tug.angle=0;g.tug.vx=g.tug.vy=g.tug.spin=0;
        limit=(mode==CHASE?180:120)*60;
        it=mode==CHASE?-1:random(4)==0?-1:random(count);
        if(it>=0)held[it]=60;
        for(int k=0;k<open.length;k++){open[k]=0;openTicks[k]=0;}
        // Corral: mid-shortcut on the circuit (central, so tows stay short); the most open water near the middle on the islands.
        // Several pens so the nearest is never far: both sides of the circuit's lock plus two corners;
        // on the islands, the most open water in the west and east halves.
        if(course instanceof ArenaCourse.Loop){penX=new float[]{500,1500,90,1910};penY=new float[]{650,650,90,1210};}
        else{ArenaCourse.Islands is=(ArenaCourse.Islands)course;penX=new float[2];penY=new float[2];
            for(int side=0;side<2;side++){float best=-1e9f;
                for(int k=0;k<nx.length;k++){if((nx[k]<is.w*.5f)!=(side==0))continue;float cx=is.w*(side==0?.3f:.7f),score=is.clearance(nx[k],ny[k])-Physics.length(nx[k]-cx,ny[k]-is.h*.5f)*.15f;if(score>best){best=score;penX[side]=nx[k];penY[side]=ny[k];}}}}
        penCount=new int[penX.length];corralX=penX[0];corralY=penY[0];
    }
    /** Where corralled boat k parks: around the corral centre. */
    /** Nearest pen to a point. */
    int nearestPen(float x,float y){int best=0;float d=Float.MAX_VALUE;for(int k=0;k<penX.length;k++){float e=Physics.length(x-penX[k],y-penY[k]);if(e<d){d=e;best=k;}}return best;}
    float slotX(int pen,int k){return penX[pen]+(float)StrictMath.cos(k*1.047f)*42;}
    float slotY(int pen,int k){return penY[pen]+(float)StrictMath.sin(k*1.047f)*42;}
    boolean towed(Game g,int i){return !corralled[i] && g.tow.connected(npcs[i],g.tug);}
    static boolean contains(float x,float y){for(float[] r:WATER)if(x>=r[0] && x<=r[2] && y>=r[1] && y<=r[3])return true;return false;}
    boolean passable(int gate){return open[gate]>=.8f;}

    /** Bridges and gates: any boat near opens them over OPEN_TICKS; they close after HOLD_TICKS clear. */
    void stepGates(Game g){
        for(int k=0;k<open.length;k++){
            float cx=course.gateX(k),cy=course.gateY(k);
            boolean near=Physics.length(g.tug.x-cx,g.tug.y-cy)<TRIGGER;
            for(Body b:npcs)if(b.active && Physics.length(b.x-cx,b.y-cy)<TRIGGER)near=true;
            if(near){clear[k]=0;openTicks[k]=Math.min(OPEN_TICKS,openTicks[k]+1);}
            else if(++clear[k]>HOLD_TICKS)openTicks[k]=Math.max(0,openTicks[k]-1);
            open[k]=openTicks[k]/(float)OPEN_TICKS;
            if(footbridges.length>0)footbridges[k].update(open[k]);
        }
    }
    /** Walls, islands and closed gates for one hull; returns the hardest inward impact speed. */
    float collide(Body b,CollisionSplashes splashes){
        float impact=0;
        for(int disc=0;disc<3;disc++){
            float x=b.discX(disc),y=b.discY(disc),r=b.radius;
            impact=Math.max(impact,course.walls(b,splashes,x,y,r));
            for(int k=0;k<open.length;k++)if(!passable(k))impact=Math.max(impact,course.gateWall(b,splashes,k,b.discX(disc),b.discY(disc),r));
        }
        return impact;
    }
    /** Nearest node to a point. */
    int node(float x,float y){float[] nx=course.nodeX,ny=course.nodeY;int best=0;float d=Float.MAX_VALUE;for(int i=0;i<nx.length;i++){float e=Physics.length(x-nx[i],y-ny[i]);if(e<d){d=e;best=i;}}return best;}
    /** First hop from node a toward node b (breadth-first over the node graph). */
    int nextHop(int a,int b){
        if(a==b)return b;
        int n0=course.nodeX.length;int[] from=new int[n0];java.util.Arrays.fill(from,-1);from[a]=a;int[] queue=new int[n0];int head=0,tail=0;queue[tail++]=a;
        while(head<tail){int n=queue[head++];if(n==b)break;for(int m:course.links[n])if(from[m]<0){from[m]=n;queue[tail++]=m;}}
        if(from[b]<0)return a;
        int step=b;while(from[step]!=a && from[step]>=0)step=from[step];
        return step;
    }

    /** One fixed step of the game: gates, NPC pilots, tags or catches, and the clock. */
    void step(Game g,CollisionSplashes splashes){
        if(finished)return;
        ticks++;
        // The swell pushes the NPC boats just as it pushes the tug.
        for(Body b:npcs)if(b.active)g.water.affect(g,b);
        if(nitro>0)nitro--;if(angry>0)angry--;if(comboTicks>0 && --comboTicks==0)combo=0;
        powerUps(g);
        for(int i=0;i<npcs.length;i++){if(stunned[i]>0)stunned[i]--;if(sputter[i]>0)sputter[i]--;if(immune[i]>0)immune[i]--;if(held[i]>0)held[i]--;if(shoutTicks[i]>0)shoutTicks[i]--;if(shoutCooldown[i]>0)shoutCooldown[i]--;}
        if(mode==TAG && it<0)playerItTicks++;
        // Ramming a boat stuns it (both modes): dizzy, drifting, easy to rope.
        for(int i=0;i<npcs.length;i++){Body b=npcs[i];
            if(corralled[i] || stunned[i]>0 || !Physics.touching(g.tug,b,4))continue;
            float closing=Physics.length(g.tug.vx-b.vx,g.tug.vy-b.vy);
            if(closing>=STUN_SPEED){stunned[i]=STUN_TICKS;b.vx+=g.tug.vx*.5f;b.vy+=g.tug.vy*.5f;b.spin+=3;yell(i,DIZZY,true);g.haptic(Game.HAPTIC_IMPACT);}}
        if(mode==TAG)tagRules(g);else chaseRules(g);
        if(ticks>=limit && !finished){
            finished=true;
            if(mode==TAG){won=it>=0;g.tell(won?"TIME! YOU'RE NOT IT. YOU WIN!":"TIME! YOU'RE STILL IT.");}
            else {won=false;g.tell("TIME! SOME GOT AWAY.");}
        }
    }
    private void tagRules(Game g){
        Body itBody=it<0?g.tug:npcs[it];
        // The tug's lasso: when the tug is IT, a rope on any boat tags it (with a yank); when it is not,
        // roping the IT boat holds it off until the line snaps.
        for(int i=0;i<npcs.length;i++){
            if(!towed(g,i))continue;
            if(it<0){if(immune[i]<=0){g.tow.detachBody(g,npcs[i]);yank(g,npcs[i]);pass(g,i);g.tell("LASSO TAG!");return;}}
            else if(++holdTicks>=HOLD_SNAP){g.tow.detachBody(g,npcs[i]);holdTicks=0;
                if(i==it){angry=240;yell(i,ANGRY,true);g.tell("SNAP! IT'S FREE AND FURIOUS");}else g.tell("SNAP! THE LINE PARTED");}
            break;
        }
        if(g.tow.count()==0)holdTicks=0;
        if(it>=0 && held[it]>0)return;
        // IT tags whoever it touches (not the boat that just tagged it).
        if(it>=0 && immuneTug<=0 && !towed(g,it) && Physics.touching(itBody,g.tug,3)){pass(g,-1);return;}
        for(int i=0;i<npcs.length;i++){
            if(i==it || immune[i]>0 || !npcs[i].active)continue;
            if(Physics.touching(itBody,npcs[i],3)){pass(g,i);return;}
        }
    }
    /** A lasso tag snaps the roped boat toward the tug. */
    private void yank(Game g,Body b){float dx=g.tug.x-b.x,dy=g.tug.y-b.y,d=Math.max(1,Physics.length(dx,dy));b.vx+=dx/d*70;b.vy+=dy/d*70;b.spin+=2;g.sounds.lassoHit();}
    int immuneTug;
    private void pass(Game g,int next){
        int previous=it;it=next;tags++;
        if(previous>=0)immune[previous]=120;else immuneTug=120;
        if(next>=0){held[next]=60;yell(next,next>=0 && previous<0?TAGGED_BY_YOU:TAGGED,true);}
        if(previous>=0 && next<0)yell(previous,GOT_YOU,true);
        g.haptic(Game.HAPTIC_SUCCESS);
        g.tell(next<0?"YOU'RE IT! TAG SOMEONE!":previous<0?"TAGGED! YOU'RE NOT IT":"TAG! A NEW BOAT IS IT");
    }
    private void chaseRules(Game g){
        int left=0;
        for(int i=0;i<npcs.length;i++){
            if(corralled[i])continue;
            Body b=npcs[i];
            // Towed into the corral: the gate crew takes it.
            int pen=nearestPen(b.x,b.y);
            if(towed(g,i) && Physics.length(b.x-penX[pen],b.y-penY[pen])<CORRAL_REACH){
                g.tow.detachBody(g,b);corralled[i]=true;caught[i]=true;int slot=penCount[pen]++;g.delivered++;
                b.x=b.tiedX=slotX(pen,slot);b.y=b.tiedY=slotY(pen,slot);b.angle=b.tiedAngle=slot*1.047f+1.57f;b.vx=b.vy=b.spin=0;b.awaitingPickup=true;b.deliveryRecorded=true;
                // Parked in the pen: out of the collision world (drawn from npcs), so the corral never walls off the channel.
                for(int k=0;k<g.cargo.length;k++)if(g.cargo[k]==b)g.cargo[k]=null;
                combo=comboTicks>0?combo+1:1;comboTicks=20*60;int points=400*combo+Math.max(0,(limit-ticks)/60)*5;g.score+=points;g.haptic(Game.HAPTIC_SUCCESS);
                yell(i,CAUGHT,true);g.tell(combo>1?"CORRALLED! COMBO x"+combo+" +"+points:"CORRALLED! +"+points+" / "+g.delivered+" OF "+npcs.length);continue;
            }
            left++;
        }
        if(left==0 && !finished){finished=true;won=true;g.tell("EVERY BOAT CORRALLED!");}
    }
    /** Power-up buoys: one spawns every POWER_SPAWN ticks at a random route node (three at most). */
    private void powerUps(Game g){
        if(--powerTimer<=0){powerTimer=POWER_SPAWN;
            for(int k=0;k<powerLive.length;k++)if(!powerLive[k]){int n=random(course.nodeX.length);powerX[k]=course.nodeX[n];powerY[k]=course.nodeY[n];
                powerType[k]=NITRO;powerLive[k]=true;break;}}
        for(int k=0;k<powerLive.length;k++){
            if(!powerLive[k] || Physics.length(g.tug.x-powerX[k],g.tug.y-powerY[k])>34)continue;
            powerLive[k]=false;g.haptic(Game.HAPTIC_SUCCESS);g.sounds.complete();
            nitro=240;g.tell("NITRO!");
        }
    }
    /** Lasso reach multiplier: arena throws carry 30% farther. */
    float reach(){return 1.3f;}
    /** NPC pilots, motion, walls, gates and hull contacts; called once per physics sub-step. */
    /** NPC steering (Game integrates and collides the hulls as cargo); called once per physics sub-step. */
    void motion(Game g,CollisionSplashes splashes){
        if(immuneTug>0)immuneTug--;
        stepGates(g);
        for(int i=0;i<npcs.length;i++){
            Body b=npcs[i];if(!b.active || corralled[i])continue;
            if(caught[i] || finished || held[i]>0 || stunned[i]>0){b.vx*=.97f;b.vy*=.97f;if(stunned[i]>0)b.spin=b.spin*.98f+.05f;else b.spin*=.9f;}
            else if(towed(g,i))struggle(g,i);
            else pilot(g,i);
        }
    }
    /** A roped boat fights the line: thrashes side to side and pulls away from the tug. */
    private void struggle(Game g,int i){
        Body b=npcs[i];float dx=b.x-g.tug.x,dy=b.y-g.tug.y,d=Math.max(1,Physics.length(dx,dy));
        float wiggle=(float)StrictMath.sin(ticks*.15f+i)*1.1f,ax=dx/d,ay=dy/d,cx=ax-ay*wiggle,cy=ay+ax*wiggle,l=Math.max(.001f,Physics.length(cx,cy));
        float pull=sputter[i]>0?12:24;b.vx+=cx/l*pull*Physics.DT;b.vy+=cy/l*pull*Physics.DT;b.spin+=wiggle*.4f*Physics.DT;
        yell(i,TOWED,false);
    }
    private void pilot(Game g,int i){
        Body b=npcs[i];boolean chasing=mode==TAG && it==i;
        Body threat=mode==CHASE?g.tug:it<0?g.tug:npcs[it];
        // Narrow canals leave nowhere to dodge, so circuit runners are a touch slower than island ones.
        float tx,ty,speed=mode==CHASE?(course instanceof ArenaCourse.Loop?70:76):chasing?(angry>0?96:84):80;
        if(chasing){
            // Hunt the nearest boat that may be tagged.
            Body prey=null;float best=Float.MAX_VALUE;
            // Everybody wants to tag the tug: it counts as 60% of its real distance away.
            if(immuneTug<=0){prey=g.tug;best=Physics.length(g.tug.x-b.x,g.tug.y-b.y)*.6f;}
            for(int k=0;k<npcs.length;k++)if(k!=i && immune[k]<=0 && npcs[k].active){float d=Physics.length(npcs[k].x-b.x,npcs[k].y-b.y);if(d<best){best=d;prey=npcs[k];}}
            if(prey==null){tx=course.nodeX[goal[i]];ty=course.nodeY[goal[i]];}
            else if(lineClear(b.x,b.y,prey.x,prey.y)){tx=prey.x+prey.vx*.4f;ty=prey.y+prey.vy*.4f;}
            else{int hop=nextHop(node(b.x,b.y),node(prey.x,prey.y));goal[i]=hop;tx=course.nodeX[hop];ty=course.nodeY[hop];}
        }else{
            // Roam the loop; when the threat is close, run for the neighbouring node farthest from it.
            if(Physics.length(course.nodeX[goal[i]]-b.x,course.nodeY[goal[i]]-b.y)<70){
                int at=goal[i],pick=course.links[at][random(course.links[at].length)];
                float danger=Physics.length(threat.x-b.x,threat.y-b.y);
                if(danger<500){float far=-1;for(int m:course.links[at]){float d=Physics.length(course.nodeX[m]-threat.x,course.nodeY[m]-threat.y);if(d>far){far=d;pick=m;}}}
                goal[i]=pick;
            }
            float danger=Physics.length(threat.x-b.x,threat.y-b.y);
            // Running flat out with the threat close burns fuel; an empty tank sputters.
            if(danger<300){fuel[i]-=1f/FUEL_BURN;if(fuel[i]<=0 && sputter[i]==0){fuel[i]=.35f;sputter[i]=SPUTTER_TICKS;yell(i,GAS,true);}}
            else fuel[i]=Math.min(1,fuel[i]+1f/FUEL_REFILL);
            if(danger<260){
                // Too close: turn tail if the current goal leads toward the threat.
                float gx=course.nodeX[goal[i]]-b.x,gy=course.nodeY[goal[i]]-b.y,ax=threat.x-b.x,ay=threat.y-b.y;
                if(gx*ax+gy*ay>0){int at=node(b.x,b.y);float far=-1;for(int m:course.links[at]){float d=Physics.length(course.nodeX[m]-threat.x,course.nodeY[m]-threat.y);if(d>far){far=d;goal[i]=m;}}}
                yell(i,mode==CHASE?RUN:CLOSE,false);
            }else yell(i,mode==CHASE?TAUNT:IDLE,false);
            tx=course.nodeX[goal[i]];ty=course.nodeY[goal[i]];
        }
        if(sputter[i]>0)speed*=SPUTTER;
        // Wait for a closed bridge or gate ahead to finish opening.
        for(int k=0;k<open.length;k++){
            if(passable(k))continue;
            float d=Physics.length(course.gateX(k)-b.x,course.gateY(k)-b.y);
            if(d<110 && course.crosses(k,b.x,b.y,b.x+(tx-b.x)*2,b.y+(ty-b.y)*2))speed=Math.min(speed,Math.max(0,(d-40)*.6f));
        }
        float desired=(float)StrictMath.atan2(ty-b.y,tx-b.x),error=Physics.angle(desired-b.angle);
        b.spin+=Physics.clamp(error*5-b.spin*2.4f,-8,8)*Physics.DT;
        float c=(float)StrictMath.cos(b.angle),s=(float)StrictMath.sin(b.angle),go=speed*Math.max(.3f,1-Math.abs(error)*.45f);
        float dvx=c*go-b.vx,dvy=s*go-b.vy,f=Physics.length(dvx,dvy),max=120*Physics.DT;
        if(f>max){dvx*=max/f;dvy*=max/f;}
        b.vx+=dvx;b.vy+=dvy;
    }
    /** Straight water between two points with no closed gate across it. */
    /** A tow rope may sweep over islands and banks; only a shut bridge or gate across it parts it. */
    boolean ropeClear(float ax,float ay,float bx,float by){
        for(int k=0;k<open.length;k++)if(!passable(k) && course.crosses(k,ax,ay,bx,by))return false;
        return true;
    }
    boolean lineClear(float ax,float ay,float bx,float by){
        if(!course.water(ax,ay,bx,by,20))return false;
        for(int k=0;k<open.length;k++)if(!passable(k) && course.crosses(k,ax,ay,bx,by))return false;
        return true;
    }
    static final String[] IDLE={"CAN'T TAG ME!","NA NA NA!","OVER HERE!","TOO SLOW!"};
    static final String[] CLOSE={"NOPE NOPE NOPE!","GO AWAY!","EEEK!","NOT IT!"};
    static final String[] TAGGED={"AW, MAN!","I'M IT?!","YOU'RE GOING DOWN!"};
    static final String[] TAGGED_BY_YOU={"NO FAIR!","HEY! WATCH THE PAINT!","I'LL GET YOU BACK!"};
    static final String[] GOT_YOU={"TAG, TUGGY!","YOU'RE IT! HA!","GOTCHA!"};
    static final String[] TAUNT={"CATCH ME IF YOU CAN!","BEEP BEEP!","LATER, TUGBOAT!","WOOO!"};
    static final String[] RUN={"FLOOR IT!","HE'S ON ME!","SCATTER!","NOT TODAY!"};
    static final String[] DIZZY={"WHOA, WHOA!","SEEING STARS...","MY HEAD!","OOF!"};
    static final String[] TOWED={"LET ME GO!","I KNOW MY RIGHTS!","PUT ME DOWN!","NOT THE CORRAL!"};
    static final String[] ANGRY={"NOW I'M MAD!","YOU'RE GOING DOWN!","RAAAH!"};
    static final String[] GAS={"OUT OF GAS!","SPUTTER... SPUTTER...","NOT NOW, ENGINE!"};
    static final String[] CAUGHT={"BUSTED!","OK, OK, YOU GOT ME","NO FAIR!"};
    void yell(int i,String[] lines,boolean force){
        if(!force && (shoutTicks[i]>0 || shoutCooldown[i]>0))return;
        shout[i]=lines[random(lines.length)];shoutTicks[i]=110;shoutCooldown[i]=200+random(220);
    }
    int secondsLeft(){return Math.max(0,(limit-ticks+59)/60);}
    int stars(){
        if(!won)return 0;
        if(mode==CHASE){int s=ticks/60;boolean open=course instanceof ArenaCourse.Islands;return s<(open?65:100)?3:s<(open?100:140)?2:1;}
        int it=playerItTicks/60;return it<10?3:it<35?2:1;
    }
    String status(Game g){
        if(finished)return won?(mode==CHASE?"EVERY BOAT CAUGHT!":"YOU WIN! NOT IT AT THE HORN"):(mode==CHASE?"TIME UP / "+g.delivered+" CAUGHT":"YOU WERE IT AT THE HORN");
        if(mode==CHASE){for(int i=0;i<npcs.length;i++)if(towed(g,i))return "TOW IT TO THE NEAREST CORRAL / IT WILL FIGHT THE LINE";return "RAM TO STUN / LASSO / TOW TO THE CORRAL / "+g.delivered+" OF "+npcs.length;}
        return it<0?"YOU'RE IT! TOUCH OR LASSO A BOAT TO TAG IT":"DON'T GET TAGGED / LASSO IT TO HOLD IT OFF";
    }
}
