package com.bargeboss.game;
/** Montrose traffic: clear lanes, live movement, player-only damage, empty slips. */
public final class HarborTrafficTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    /** Smallest clearance (world units) from a lane to shore, docks, berthed hulls and mooring balls. */
    static float laneClearance(float[] route,boolean loop){
        float worst=Float.MAX_VALUE;int n=route.length/2;
        for(int i=0;i<(loop?n:n-1);i++){
            int j=(i+1)%n;float ax=route[i*2],ay=route[i*2+1],bx=route[j*2],by=route[j*2+1],len=Physics.length(bx-ax,by-ay);
            for(float t=0;t<=len;t+=6){float x=ax+(bx-ax)*t/len,y=ay+(by-ay)*t/len;worst=Math.min(worst,clearance(x,y));}
        }
        return worst;
    }
    private static float clearance(float x,float y){
        for(float r=0;r<=60;r+=4){
            for(int k=0;k<16;k++){double a=k*Math.PI/8;if(!MontroseHarbor.contains(x+r*(float)Math.cos(a),y+r*(float)Math.sin(a)))return r;}
            for(MooredBoats.Berth b:MooredBoats.BERTHS)if(Physics.length(x-b.x,y-b.y)<b.length*.5f+r)return r;
            for(float[] m:MontroseHarbor.MOORINGS)if(Physics.length(x-m[0],y-m[1])<12+r)return r;
        }
        return 60;
    }
    public static void run(){
        float[][] lanes={HarborTraffic.INLET_RUN,HarborTraffic.NORTH_RUN,HarborTraffic.MOUTH,
            HarborTraffic.RACE_LOOP,HarborTraffic.SOUTH_LANE,HarborTraffic.EAST_LANE,HarborTraffic.NORTH_LANE,HarborTraffic.SHORE_LANE};
        for(int i=0;i<lanes.length;i++){float c=laneClearance(lanes[i],lanes[i]==HarborTraffic.RACE_LOOP);System.out.println("lane "+i+" clearance "+c);check(c>=20,"traffic lane "+i+" keeps clear of shore, docks and hulls ("+c+")");}
        Game g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.mode=Game.PLAY;
        check(g.harborTraffic.craft.length==12,"Montrose launches its harbor and lake traffic");
        int empty=0;for(MooredBoats.Berth b:MooredBoats.BERTHS)if(MooredFleet.away(b))empty++;
        check(empty>MooredBoats.BERTHS.length/5 && empty<MooredBoats.BERTHS.length/2,"about a third of the slips stand empty: "+empty+" of "+MooredBoats.BERTHS.length);
        float[] x0=new float[12],y0=new float[12];for(int i=0;i<12;i++){x0[i]=g.harborTraffic.craft[i].body.x;y0[i]=g.harborTraffic.craft[i].body.y;}
        g.tug.x=-2000;g.tug.y=-4000;
        for(int t=0;t<60*40;t++){g.harborTraffic.step(g,g.splashes);g.harborTraffic.collide(g,g.splashes);g.splashes.step(g);HullDamage.step(g);g.ticks++;}
        for(int i=0;i<12;i++){Body b=g.harborTraffic.craft[i].body;
            check(Physics.length(b.x-x0[i],b.y-y0[i])>60 || g.harborTraffic.craft[i].idle>0,"craft "+i+" makes way");
            check(b.health==100 && b.active,"traffic never damages itself without the player: craft "+i+" health "+b.health);
            check(MontroseHarbor.contains(b.x,b.y),"craft "+i+" stays on the water");}
        check(g.othersDamage==0,"traffic bumping traffic is not the player's fault");
        // The tug ramming a cruiser costs it health and gets the skipper shouting.
        HarborTraffic.Craft c=g.harborTraffic.craft[2];Body boat=c.body;
        g.splashes.contact(g.tug,boat,boat.x,boat.y,120,0,0,0,120);g.splashes.step(g);
        check(boat.health==100-HullDamage.SMASH && g.othersDamage==HullDamage.SMASH && c.shoutTicks>0,"ramming traffic smashes it and the skipper yells");
        // Smash through: a full-speed ram keeps the tug moving, throws the boat aside and costs the tug nothing.
        g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.mode=Game.PLAY;
        Body victim=g.harborTraffic.craft[0].body;victim.vx=victim.vy=0;victim.spin=0;
        g.tug.angle=0;g.tug.x=victim.x-victim.half-g.tug.half-8;g.tug.y=victim.y+4;g.tug.vx=95;g.tug.vy=0;
        int hull=g.hull;float vx=victim.vx;
        float smashed=0;for(int t=0;t<4;t++){Physics.integrate(g.tug);smashed=Math.max(smashed,g.harborTraffic.collide(g,g.splashes));}
        check(g.tug.vx>60,"the tug keeps its way through the boat ("+g.tug.vx+")");
        check(victim.vx>vx+30 && victim.spin!=0,"the rammed boat is shoved and spun aside");
        check(smashed>=HullDamage.SMASH_SPEED,"a full-speed smash reports a hard tug impact (Game charges TUG_HIT) ("+smashed+")");
        for(int k=0;k<2;k++){g.ticks+=HullDamage.COOLDOWN;g.splashes.contact(g.tug,victim,victim.x,victim.y,120,0,0,0,120);for(int s=0;s<=CollisionSplashes.COOLDOWN;s++)g.splashes.step(g);}
        check(victim.sinkTicks>0 && victim.health==0,"two hard smashes sink a pleasure boat");
        // No-wake marina: law-abiding traffic idles inside the harbor; the hooligan and lake boats do not.
        g=new Game();g.selectChallenge(Challenges.MONTROSE);g.reset();g.mode=Game.PLAY;g.tug.x=-2000;g.tug.y=-4000;
        float fastest=0,hooligan=0,lake=0;
        for(int t=0;t<60*30;t++){g.harborTraffic.step(g,g.splashes);
            for(HarborTraffic.Craft each:g.harborTraffic.craft){Body b=each.body;if(t<300)continue;
                if(HarborTraffic.noWake(b.x,b.y)){if(each.hooligan)hooligan=Math.max(hooligan,b.speed());else fastest=Math.max(fastest,b.speed());}
                else lake=Math.max(lake,b.speed());}}
        check(fastest<=HarborTraffic.NO_WAKE+3,"marina traffic keeps to no-wake speed ("+fastest+")");
        check(hooligan>40 && lake>60,"the hooligan and the lake boats still run fast ("+hooligan+", "+lake+")");
        System.out.println("Harbor traffic lanes, motion and damage passed");
    }
    public static void main(String[] a){run();}
}
