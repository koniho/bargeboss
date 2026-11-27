package com.bargeboss.game;

/** WATER TAXI: the beach shuttle. Beachgoers wave from the water's edge along Montrose Beach; ease up
 * alongside to let them hop aboard (three at a time), then race them out to the party raft offshore.
 * The clock is short, full loads pay a bonus, and the guests cheer louder the faster you go. */
final class WaterTaxiJob extends MontroseJob {
    static final int GUESTS=10,SEATS=3,BOARD_TICKS=22;
    static final float REACH=62,SLOW=26;
    /** Pickups at the water's edge along the beach crescent, and the party raft anchored offshore. */
    static final float[] PICKUPS={-180,-3610,380,-3090,900,-2770,1420,-2480,1950,-2300,120,-3380};
    static final float PIER_X=880,PIER_Y=-3330;
    final int[] waiting=new int[PICKUPS.length/2];
    int aboard,boardTimer,spawned,spawnTimer;
    private int seed=29;

    void setup(Game g){
        limit=180*60;
        // Two crowds are already waiting; more guests show up as the party fills.
        waiting[0]=2;waiting[2]=2;spawned=4;
        start(g,PIER_X,PIER_Y-40,-(float)Math.PI/2);
    }
    void objective(Game g,Painter canvas){
        int near=-1;float best=Float.MAX_VALUE;
        for(int s=0;s<waiting.length;s++)if(waiting[s]>0){float d=Physics.length(PICKUPS[s*2]-g.tug.x,PICKUPS[s*2+1]-g.tug.y);if(d<best){best=d;near=s;}}
        if(aboard>=SEATS || aboard>0 && near<0)arrow(g,canvas,PIER_X,PIER_Y,"PARTY RAFT");
        else if(near>=0)arrow(g,canvas,PICKUPS[near*2],PICKUPS[near*2+1],"GUESTS");
    }
    String status(Game g){
        if(aboard>=SEATS)return "SHUTTLE FULL / RUSH THEM TO THE PARTY RAFT";
        if(aboard>0)return aboard+" ABOARD / PICK UP MORE OR DROP AT THE RAFT";
        return "PICK UP BEACHGOERS / SLOW DOWN AT THE WATER'S EDGE";
    }
    private int random(int n){seed=seed*1103515245+12345;return Math.floorMod(seed>>>8,n);}
    void step(Game g){
        super.step(g);
        // New guests turn up at a random dock until everyone needed has arrived.
        if(spawned<GUESTS+2 && ++spawnTimer>=300){spawnTimer=0;int s=random(waiting.length);if(waiting[s]<4){waiting[s]++;spawned++;}}
        boolean slow=g.tug.speed()<SLOW;
        if(boardTimer>0)boardTimer--;
        for(int s=0;s<waiting.length;s++){
            if(waiting[s]==0 || aboard>=SEATS || !slow || boardTimer>0)continue;
            float x=PICKUPS[s*2],y=PICKUPS[s*2+1];
            if(Physics.length(g.tug.x-x,g.tug.y-y)>REACH)continue;
            waiting[s]--;aboard++;boardTimer=BOARD_TICKS;g.haptic(Game.HAPTIC_TICK);pop(x,y,"HOP ON!");
            g.tell(aboard==SEATS?"SHUTTLE FULL! TO THE PARTY RAFT":"GUEST ABOARD / "+aboard+" OF "+SEATS+" SEATS");
        }
        if(aboard>0 && slow && boardTimer==0 && Physics.length(g.tug.x-PIER_X,g.tug.y-PIER_Y)<REACH+10){
            boolean full=aboard==SEATS;int need=g.quota()-g.delivered;int drop=Math.min(aboard,need);
            for(int k=0;k<drop;k++){
                boolean last=k==drop-1;
                score(g,PIER_X+k*14,PIER_Y,150+(full && last?250:0),full && last?"FULL LOAD BONUS! PARTY ON":"GUEST DROPPED OFF");
            }
            aboard=0;boardTimer=BOARD_TICKS;
        }
    }
    void water(Game g,Painter p){
        for(int s=0;s<waiting.length;s++)if(waiting[s]>0)ring(g,p,PICKUPS[s*2],PICKUPS[s*2+1],REACH*.6f,aboard>=SEATS?0xff8899aa:0xffffd166,aboard>=SEATS?.35f:.9f);
        ring(g,p,PIER_X,PIER_Y,REACH*.75f,0xffff6fd8,aboard>0?1:.4f);
    }
    void overlay(Game g,Painter p){
        RiverCamera c=g.layout.camera;
        // Waving guests: a bobbing cluster of bright heads with a count.
        for(int s=0;s<waiting.length;s++){
            if(waiting[s]==0)continue;
            float x=c.screenX(PICKUPS[s*2],PICKUPS[s*2+1]),y=c.screenY(PICKUPS[s*2],PICKUPS[s*2+1]);
            if(x<-60 || x>g.layout.width+60 || y<-60 || y>g.layout.height+60)continue;
            for(int k=0;k<waiting[s];k++){
                float hop=Math.abs((float)StrictMath.sin(g.visualTicks*.18f+k*1.3f+s))*5*c.scale,hx=x+(k-(waiting[s]-1)*.5f)*7*c.scale;
                int color=new int[]{0xffff6fd8,0xff6dffb0,0xffffd166,0xff8fd0ff}[(s+k)%4];
                p.material(hx,y-12*c.scale-hop,5*c.scale,0,0,8*c.scale,EffectMaterial.GLOW,0,1,color);
                p.line(hx,y-14*c.scale-hop,hx+(k%2==0?-4:4)*c.scale,y-22*c.scale-hop-(float)StrictMath.sin(g.visualTicks*.4f+k)*2*c.scale,1.4f*c.scale,color);
            }
            Lettering.center(p,"x"+waiting[s],x,y-34*c.scale,Physics.clamp(10*c.scale,11,15),0xffffffff);
        }
        float px=c.screenX(PIER_X,PIER_Y),py=c.screenY(PIER_X,PIER_Y);
        raft(g,p,c,px,py);Lettering.center(p,"PARTY RAFT",px,py-44*c.scale,Physics.clamp(10*c.scale,11,15),0xffff6fd8);
        if(aboard>0){float tx=c.screenX(g.tug.x,g.tug.y),ty=c.screenY(g.tug.x,g.tug.y)-44*c.scale;Lettering.center(p,"TAXI "+aboard+"/"+SEATS,tx,ty,Physics.clamp(10*c.scale,11,15),0xffffd166);}
        super.overlay(g,p);
    }
    /** The party raft: a swim platform with a canopy, string lights and dancers. */
    private static void raft(Game g,Painter p,RiverCamera c,float px,float py){
        float s=c.scale,bob=(float)StrictMath.sin(g.visualTicks*.07f)*1.5f*s,w=46*s,h=24*s,y=py+bob;
        p.convexQuad(px-w*.5f,y-h*.5f+3*s,px+w*.5f,y-h*.5f+3*s,px+w*.5f+3*s,y+h*.5f+3*s,px-w*.5f+3*s,y+h*.5f+3*s,0x66000000);
        p.rect(px-w*.5f,y-h*.5f,w,h,0xff8a6a4a);p.rect(px-w*.5f,y-h*.5f,w,3*s,0xffb08a62);
        for(int k=0;k<6;k++){float lx=px-w*.5f+(k+.5f)*w/6,ly=y-h*.5f-10*s+(float)StrictMath.abs(k-2.5f)*1.6f*s;
            int col=new int[]{0xffff6fd8,0xffffd166,0xff6dffb0,0xff8fd0ff}[(k+g.visualTicks/15)%4];
            p.material(lx,ly,7*s,0,0,7*s,EffectMaterial.GLOW,0,1,col);}
        for(int k=0;k<4;k++){float dx=px+(k-1.5f)*9*s,hop=(float)StrictMath.abs(StrictMath.sin(g.visualTicks*.2f+k*1.7f))*4*s;
            p.circle(dx,y-4*s-hop,2.6f*s,new int[]{0xffffe0c0,0xffd9a37a,0xffa8705a,0xfff0c8a0}[k]);
            p.line(dx,y-2*s-hop,dx,y+4*s-hop,2*s,new int[]{0xffff6fd8,0xff6dffb0,0xffffd166,0xff8fd0ff}[k]);}
    }
}
