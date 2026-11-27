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
        // Chase: catch every boat.
        g=game(Challenges.BOAT_CHASE);a=g.arena;check(a.mode==TagArena.CHASE && g.quota()==a.npcs.length,"chase asks for every boat");
        for(int i=0;i<a.npcs.length;i++){Body b=a.npcs[i];b.angle=0;g.tug.x=b.x-b.half-g.tug.half+2;g.tug.y=b.y;g.tug.angle=0;a.step(g,g.splashes);check(a.caught[i],"touching boat "+i+" catches it");}
        check(a.finished && a.won && g.delivered==a.npcs.length,"catching every boat wins the chase");g.step();check(g.goalComplete() && g.mode==Game.FINISH,"winning the chase finishes the circuit");
        g=game(Challenges.BOAT_CHASE);g.arena.ticks=g.arena.limit-1;g.step();check(g.goalFailed(),"time running out fails the chase");
        System.out.println("Tag circuit geometry, one-second bridges and gates, NPCs, tag passing and chase passed");
    }
    public static void main(String[] a){run();}
}
