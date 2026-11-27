package com.bargeboss.game;
/** Tag circuit: geometry, one-second bridges and gates, walls, NPCs on the water, tag passing, chase catches and endings. */
public final class TagArenaTest {
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static Game game(int id){Game g=new Game();ReplayInput.veteran(g);g.challengeId=id;g.reset();g.mode=Game.PLAY;g.dialogue.close(g);if(id!=g.challengeId)g.startArenaRound(id);check(g.arenaStage() && g.arena!=null,"arena "+id+" sets up");Parity.frame(g);return g;}
    public static void run(){
        for(int i=0;i<TagArena.NODE_X.length;i++)check(TagArena.contains(TagArena.NODE_X[i],TagArena.NODE_Y[i]),"route node "+i+" is on the water");
        for(int i=0;i<TagArena.NODE_X.length;i++)for(int j:TagArena.LINKS[i]){
            float ax=TagArena.NODE_X[i],ay=TagArena.NODE_Y[i],bx=TagArena.NODE_X[j],by=TagArena.NODE_Y[j],len=Physics.length(bx-ax,by-ay);
            for(float t=0;t<=len;t+=10)check(TagArena.contains(ax+(bx-ax)*t/len,ay+(by-ay)*t/len),"route "+i+"-"+j+" stays on the water");}
        // Bridges and gates: shut until someone comes near, open within one second, and they block while shut.
        Game g=game(Challenges.BOAT_TAG);TagArena a=g.arena;
        for(Body b:a.npcs){b.x=90;b.y=1210;b.active=false;}
        g.tug.x=TagArena.GATE_X[TagArena.BRIDGE_N]-400;g.tug.y=90;
        for(int t=0;t<5;t++)a.stepGates(g);
        check(a.open[TagArena.BRIDGE_N]==0 && !a.passable(TagArena.BRIDGE_N),"a bridge stays down with nobody near");
        Body probe=new Body(-1,-1,TagArena.GATE_X[TagArena.BRIDGE_N]-30,90,0);probe.vx=60;a.collide(probe,null);
        check(probe.x<TagArena.GATE_X[TagArena.BRIDGE_N]-30 && probe.vx<0,"a closed bridge blocks the channel");
        g.tug.x=TagArena.GATE_X[TagArena.BRIDGE_N]-120;
        for(int t=0;t<TagArena.OPEN_TICKS;t++)a.stepGates(g);
        check(a.open[TagArena.BRIDGE_N]==1 && a.passable(TagArena.BRIDGE_N),"a bridge is fully open one second after a boat approaches");
        for(int t=0;t<TagArena.OPEN_TICKS;t++){a.npcs[0].active=true;a.npcs[0].x=TagArena.GATE_X[TagArena.LOCK_W]+60;a.npcs[0].y=650;a.stepGates(g);}
        check(a.passable(TagArena.LOCK_W),"an NPC approaching opens the lock gate in a second too");
        g.tug.x=-3000;a.npcs[0].x=90;a.npcs[0].y=1210;for(int t=0;t<TagArena.HOLD_TICKS+TagArena.OPEN_TICKS+2;t++)a.stepGates(g);
        check(a.open[TagArena.BRIDGE_N]==0,"bridges close again once the water is clear");
        // Long run: every NPC stays on the water and keeps moving.
        g=game(Challenges.BOAT_TAG);a=g.arena;
        for(int t=0;t<60*40;t++){g.step();if(a.finished)break;}
        for(Body b:a.npcs)check(TagArena.contains(b.x,b.y),"NPC stays on the water at "+(int)b.x+","+(int)b.y);
        // Tagging passes IT, with no tag-backs.
        g=game(Challenges.BOAT_TAG);a=g.arena;a.it=0;a.held[0]=0;Body it=a.npcs[0];it.angle=0;
        g.tug.x=it.x+it.half+g.tug.half-2;g.tug.y=it.y;g.tug.angle=0;g.arena.step(g,g.splashes);
        check(a.it==-1 && a.tags==1,"IT touching the tug makes the tug IT");
        g.arena.step(g,g.splashes);check(a.it==-1,"no tag-backs: the boat that tagged you is immune");
        Body victim=a.npcs[1];victim.x=g.tug.x+g.tug.half+victim.half-2;victim.y=g.tug.y;victim.angle=0;g.arena.step(g,g.splashes);
        check(a.it==1,"the tug passes IT by touching another boat");
        a.ticks=a.limit-1;g.step();check(g.challengeId==Challenges.BOAT_CHASE && g.arena.mode==TagArena.CHASE && !g.goalFailed() && g.career.bestStars(Challenges.BOAT_TAG)>0,"not IT at the horn wins tag and rolls into the chase round");
        g=game(Challenges.BOAT_TAG);g.arena.it=-1;g.arena.ticks=g.arena.limit-1;g.step();check(g.goalFailed(),"IT at the horn loses");
        // Chase: ram to stun, lasso, tow into the corral; every boat corralled wins.
        g=game(Challenges.BOAT_CHASE);a=g.arena;check(a.mode==TagArena.CHASE && g.quota()==a.npcs.length,"chase asks for every boat");
        for(int i=0;i<a.npcs.length;i++)check(g.cargo[i]==a.npcs[i],"NPC "+i+" sits in a cargo slot so the lasso works on it");
        for(int i=0;i<a.npcs.length;i++){Body b=a.npcs[i];b.angle=0;b.vx=b.vy=0;g.tug.x=b.x-b.half-g.tug.half+2;g.tug.y=b.y;g.tug.angle=0;g.tug.vx=60;g.tug.vy=0;a.step(g,g.splashes);
            check(a.stunned[i]>0 && !a.corralled[i],"ramming boat "+i+" stuns it but does not catch it");
            g.tug.vx=0;check(g.tow.attach(g,b,g.tug),"a stunned boat takes the lasso");
            int pen=i%a.penX.length;b.x=a.penX[pen]+5;b.y=a.penY[pen];b.vx=b.vy=0;g.tug.x=a.penX[pen]+(pen==3?-90:90);g.tug.y=a.penY[pen]+(pen==3?-40:40);a.step(g,g.splashes);
            check(a.corralled[i] && g.delivered==i+1 && !g.tow.connected(b,g.tug) && b.awaitingPickup && g.cargo[i]==null,"towing boat "+i+" into the corral banks it and parks it out of the way");}
        check(a.finished && a.won && g.delivered==a.npcs.length && a.combo>1,"corralling every boat wins the chase with a combo");g.step();check(g.goalComplete() && g.mode==Game.FINISH,"winning the chase finishes the circuit");
        // Tag with the lasso: when the tug is IT, roping a boat tags it; when not, roping IT holds it until the line snaps.
        g=game(Challenges.BOAT_TAG);a=g.arena;a.it=-1;Body roped=a.npcs[1];g.tug.x=roped.x-80;g.tug.y=roped.y;a.immune[1]=0;
        check(g.tow.attach(g,roped,g.tug),"the tug ropes a boat");a.step(g,g.splashes);
        check(a.it==1 && !g.tow.connected(roped,g.tug),"a lasso tag passes IT and lets the line go");
        g=game(Challenges.BOAT_TAG);a=g.arena;a.it=0;a.held[0]=0;Body itBoat=a.npcs[0];g.tug.x=itBoat.x-90;g.tug.y=itBoat.y;
        check(g.tow.attach(g,itBoat,g.tug),"the tug ropes the IT boat");
        for(int t=0;t<TagArena.HOLD_SNAP-1;t++){a.step(g,g.splashes);if(!g.tow.connected(itBoat,g.tug))break;}
        check(g.tow.connected(itBoat,g.tug) && a.it==0,"the roped IT boat is held off");a.step(g,g.splashes);
        check(!g.tow.connected(itBoat,g.tug) && a.angry>0,"the line snaps and IT comes back angry");
        // Power-ups: touching a buoy applies it.
        g=game(Challenges.BOAT_TAG);a=g.arena;a.powerLive[0]=true;a.powerType[0]=TagArena.NITRO;a.powerX[0]=g.tug.x;a.powerY[0]=g.tug.y;
        a.step(g,g.splashes);check(a.nitro>0 && !a.powerLive[0],"nitro applies on pickup");
        g=game(Challenges.BOAT_CHASE);g.arena.ticks=g.arena.limit-1;g.step();check(g.goalFailed(),"time running out fails the chase");
        // Thousand Islands: routes stay on water, footbridges open in a second, a heavy random swell, NPCs stay afloat.
        g=game(Challenges.ISLAND_TAG);a=g.arena;ArenaCourse.Islands isles=(ArenaCourse.Islands)a.course;
        check(isles.nodeX.length>40,"the islands get a dense route graph ("+isles.nodeX.length+")");
        for(int i=0;i<isles.nodeX.length;i++){check(isles.contains(isles.nodeX[i],isles.nodeY[i]),"island route node "+i+" is on the water");
            for(int j:isles.links[i])check(isles.water(isles.nodeX[i],isles.nodeY[i],isles.nodeX[j],isles.nodeY[j],10),"island route "+i+"-"+j+" stays on the water");}
        check(g.weather.swell>1 && g.weather.waves(g)>2 && SeaWaves.openSea(g),"island rounds roll a heavy swell on the open-sea wave model");
        for(Body b:a.npcs)b.active=false;g.tug.x=-3000;
        for(int t=0;t<5;t++)a.stepGates(g);check(!a.passable(0),"footbridges stay down with nobody near");
        g.tug.x=isles.gateX(0)+60;g.tug.y=isles.gateY(0)+60;for(int t=0;t<TagArena.OPEN_TICKS;t++)a.stepGates(g);
        check(a.passable(0),"a footbridge lifts within a second of a boat approaching");
        java.util.HashSet<Long> bearings=new java.util.HashSet<Long>();
        for(int r=0;r<6;r++){g.startArenaRound(Challenges.ISLAND_TAG);bearings.add(Math.round(g.weather.swellCos*100)*1000+Math.round(g.weather.swellSin*100));}
        check(bearings.size()>1,"each round picks its own swell direction");
        g=game(Challenges.ISLAND_CHASE);a=g.arena;
        for(int t=0;t<60*40;t++){g.step();if(a.finished)break;}
        for(Body b:a.npcs)check(a.course.contains(b.x,b.y),"island NPC stays on the water through the swell at "+(int)b.x+","+(int)b.y);
        g=game(Challenges.ISLAND_TAG);a=g.arena;a.ticks=a.limit-1;a.it=0;g.step();check(g.challengeId==Challenges.ISLAND_CHASE && g.arena.course instanceof ArenaCourse.Islands,"winning island tag rolls into island chase");
        System.out.println("Tag circuit geometry, one-second bridges and gates, NPCs, tag passing and chase passed");
    }
    public static void main(String[] a){run();}
}
