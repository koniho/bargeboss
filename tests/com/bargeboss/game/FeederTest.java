package com.bargeboss.game;

public final class FeederTest {
    private static int assertions;
    private static void check(boolean ok,String message) { assertions++; if(!ok) throw new AssertionError(message); }
    private static void steps(Game g,int ticks) { for(int i=0;i<ticks;i++) g.step(); }
    private static int cargo(Game g) { int total=0; for(Body b:g.cargo) if(b!=null && b.active) total++; return total; }
    public static void run() {
        for(int id=0;id<Challenges.COUNT;id++) {
            Game g=new Game(); g.restoreProgress("BB1|0|3,1|3,1|3,1"); g.selectChallenge(id); g.reset();
            check(g.feeders.length==(id==0?1:2) && cargo(g)==0,"challenge begins with its configured edge feeders and no teleported cargo");
            float previous=g.feeders[0].load.y;
            steps(g,60);
            check(g.feeders[0].load.y<previous && g.feeders[0].load.y>Feeder.DROP_Y,"cargo visibly moves in from the boundary");
            check(cargo(g)==0,"NPC cargo cannot score or age before handoff");
            steps(g,105);
            check(cargo(g)==g.feeders.length && g.feederDrops()==g.feeders.length,"both slots hand off exactly one barge");
            check(g.feeders[0].shout.equals(id==2?"ZONE C!":"ZONE A!") && (id==0 || g.feeders[1].shout.equals(id==2?"ZONE B!":"ZONE A!")),"each captain calls the actual destination");
            check(g.cargo[0].age<.1f && (id==0 || g.cargo[1].age<.1f),"contract age starts at handoff");
            float y=g.feeders[0].boat.y; steps(g,30);
            check(g.feeders[0].boat.y>y && g.feeders[0].drops==1,"pusher backs out without dropping duplicates");
            steps(g,180); check(g.feeders[0].phase==Feeder.IDLE,"delivery pusher leaves the play area");
        }
        Game g=new Game();g.challengeId=2; g.reset(); g.tug.x=260; g.tug.y=760;
        steps(g,165); Feeder f=g.feeders[0];
        check(f.blocked && f.drops==0 && f.shout.equals("HURRY UP!"),"blocked slot waits and captain shouts hurry up");
        check(g.feeders[1].drops==1,"other feeder works while one lane is blocked");
        float y=f.load.y; int calls=f.shoutTicks; steps(g,20);
        check(f.load.y==y && f.shoutTicks<calls,"blocked arrival neither overlaps the tug nor restarts shouting every frame");
        check(!Physics.touching(f.load,g.tug,0),"waiting arrival stops before contact");
        int frame=Parity.frame(g),shout=f.shoutTicks;
        check(frame==Parity.frame(g) && shout==f.shoutTicks && y==f.load.y,"drawing feeder calls never advances delivery state");
        int clock=g.ticks,blocked=f.blockedTicks; g.background(); g.advance(.25);
        check(g.ticks==clock && f.blockedTicks==blocked,"pause freezes feeder and callout clocks");
        g.pause(); g.tug.x=400; g.tug.y=600; steps(g,180);
        check(f.drops==1 && !f.blocked && f.shout.equals("ZONE C!"),"cleared lane resumes delivery and announces the correct zone");
        g.reset(); g.tug.x=260; g.tug.y=760; steps(g,400);
        check(g.feeders[0].shoutTicks>0 && g.feeders[0].shout.equals("HURRY UP!"),"blocked captain repeats a throttled reminder");
        g.reset(); steps(g,165); Body waiting=g.cargo[0]; float px=waiting.x,py=waiting.y;
        steps(g,100); check(waiting.x==px && waiting.y==py && waiting.awaitingPickup,"slot fenders hold waiting cargo until pickup");
        ReplayInput.down(g,waiting.x,waiting.y);
        check(waiting.awaitingPickup && g.heldCargo()==waiting,"selection keeps the barge steady while the tug approaches");
        for(int i=0;i<1200 && waiting.awaitingPickup;i++) g.step();
        check(!waiting.awaitingPickup,"physical push contact releases feeder fenders");
        g.startTutorial(); Game practice=g.activeGame(); steps(g,240);
        check(practice.feederDrops()==0,"practice does not feed extra cargo into lessons");
        g.reset(); check(g.feeders[0].drops==0 && !g.feeders[0].blocked && cargo(g)==0,"restart clears queues, warnings and old loads");
        g=new Game();g.challengeId=1;g.reset();steps(g,2100);
        check(g.feeders[0].blocked && g.feeders[0].load!=null && g.feeders[0].load.y>Physics.BOTTOM+45,
                "queued feeder waits outside the tug's stern approach to an uncollected load");
        System.out.println(assertions+" feeder entry, handoff, blockage, callout and lifecycle assertions passed");
    }
}
