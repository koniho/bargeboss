package com.bargeboss.game;

/** WAKE PATROL: six speedboats are tearing up the no-wake marina. Run each one down and bump it to hand
 * out a ticket. Speeders roam the fairways, break away when the tug closes in, and juke at the last
 * moment; a ticketed skipper idles out through the inlet in shame. */
final class WakePatrolJob extends MontroseJob {
    static final int SPEEDERS=6;
    static final float CRUISE=78,FLEE=92,ALERT=170;
    /** Open fairway water the speeders hop between (all on verified traffic lanes). */
    static final float[] SPOTS={-82,-30,746,-474,1442,-666,1736,-942,2,-102,300,-300,1100,-570,520,-420};
    /** Spot pairs a speeder can run between in a straight line without crossing docks or shore. */
    static final boolean[][] LINKS=links();
    private static boolean[][] links(){
        int n=SPOTS.length/2;boolean[][] out=new boolean[n][n];
        for(int a=0;a<n;a++)for(int b=0;b<n;b++)if(a!=b)out[a][b]=clear(SPOTS[a*2],SPOTS[a*2+1],SPOTS[b*2],SPOTS[b*2+1]);
        return out;
    }
    static boolean clear(float ax,float ay,float bx,float by){return clearBut(ax,ay,bx,by,null);}
    /** As clear(), ignoring one berth (an empty slip being the destination). */
    static boolean clearBut(float ax,float ay,float bx,float by,MooredBoats.Berth skip){
        float len=Physics.length(bx-ax,by-ay);
        for(float t=0;t<=len;t+=8){float x=ax+(bx-ax)*t/len,y=ay+(by-ay)*t/len;
            if(!MontroseHarbor.contains(x,y))return false;
            for(MooredBoats.Berth b:MooredBoats.BERTHS)if(b!=skip && !MooredFleet.away(b) && Math.abs(x-b.x)<70 && Math.abs(y-b.y)<70 && Physics.length(x-b.x,y-b.y)<b.length*.5f+20)return false;}
        return true;
    }
    final int[] at=new int[SPEEDERS];
    final Body[] speeders=new Body[SPEEDERS];
    final int[] spot=new int[SPEEDERS],juke=new int[SPEEDERS];
    final boolean[] ticketed=new boolean[SPEEDERS];
    private int seed=17;

    void setup(Game g){
        for(int i=0;i<SPEEDERS;i++){
            int s=(i*3+1)%(SPOTS.length/2);at[i]=s;spot[i]=next(s,-1);
            Body b=new Body(g.nextId++,-1,SPOTS[s*2],SPOTS[s*2+1],i*1.1f,false,true,i%3==0);
            b.traffic=true;b.hullKind=i%3==0?HarborTraffic.RACER:i%6;speeders[i]=b;
        }
        start(g,-60,320,-(float)Math.PI/2);
    }
    Body[] bodies(){return speeders;}
    void objective(Game g,Painter canvas){
        Body near=null;float best=Float.MAX_VALUE;
        for(int i=0;i<SPEEDERS;i++){Body b=speeders[i];if(!b.active || ticketed[i])continue;float d=Physics.length(b.x-g.tug.x,b.y-g.tug.y);if(d<best){best=d;near=b;}}
        if(near!=null)arrow(g,canvas,near.x,near.y,"SPEEDER");
    }
    String status(Game g){return "BUMP THE SPEEDERS TO TICKET THEM / "+g.delivered+" OF "+SPEEDERS;}
    private int random(int n){seed=seed*1103515245+12345;return Math.floorMod(seed>>>8,n);}
    /** A random linked spot from s (avoiding `not` when possible). */
    private int next(int s,int not){int n=SPOTS.length/2,count=0;for(int k=0;k<n;k++)if(LINKS[s][k] && k!=not)count++;if(count==0)return s==0?1:0;int pick=random(count);for(int k=0;k<n;k++)if(LINKS[s][k] && k!=not && pick--==0)return k;return s;}

    void step(Game g){
        super.step(g);
        for(int i=0;i<SPEEDERS;i++){
            Body b=speeders[i];if(!b.active || ticketed[i])continue;
            if(Physics.touching(g.tug,b,4)){
                ticketed[i]=true;b.vx*=.3f;b.vy*=.3f;
                RiverCamera c=g.layout.camera;g.splashes.shake.hit(.45f,c.screenX(b.x-g.tug.x,b.y-g.tug.y)-c.screenX(0,0),c.screenY(b.x-g.tug.x,b.y-g.tug.y)-c.screenY(0,0),true);
                int bonus=Math.round(g.tug.speed()*3);
                score(g,b.x,b.y,300+bonus,g.delivered+1==SPEEDERS?"LAST ONE! WAKE ZONE CLEAR":"TICKETED!");
                pop(b.x,b.y+30,"TICKETED!");
            }
        }
    }
    void motion(Game g){
        for(int i=0;i<SPEEDERS;i++){
            Body b=speeders[i];if(!b.active)continue;
            float tx,ty,speed;
            if(ticketed[i]){
                // Shamed: idle out of the harbor and off the board.
                tx=146;ty=900;speed=30;
                if(b.y>760){b.active=false;continue;}
            }else{
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
        collide(g,speeders,g.splashes);
    }
    void water(Game g,Painter p){
        for(int i=0;i<SPEEDERS;i++)if(speeders[i].active && !ticketed[i])ring(g,p,speeders[i].x,speeders[i].y,30,0xffff5a4a,.8f);
    }
}
