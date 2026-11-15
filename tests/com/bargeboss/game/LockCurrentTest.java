package com.bargeboss.game;

/** Current forces, slow gate timing, fixed-step pause and visible level feedback. */
public final class LockCurrentTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Body hull(float angle){return new Body(10,0,300,165,angle);}
    private static Game filling(){
        Game g=new Game();g.challengeId=2;g.reset();
        for(int i=0;i<240;i++)g.step();
        g.tug.x=300;g.tug.y=450;g.lock.step(g); // Acquire the real feeder loads.
        g.tug.y=100;
        int slot=0;for(Body b:g.cargo)if(b!=null){b.x=245+slot++*110;b.y=210;b.awaitingPickup=false;}
        g.lock.phase=Lock.FILLING;g.lock.ticks=70;return g;
    }
    public static void run(){
        Lock lock=new Lock();lock.phase=Lock.ENTRY;
        check(Lock.GATE_TICKS==300,"gate movement takes five real seconds");
        lock.ticks=75;check(lock.entryOpening()<.25f && !lock.entryPassable(),"heavy gate accelerates slowly and stays blocked");
        lock.ticks=150;check(lock.entryOpening()==.5f,"gate reaches midpoint after 2.5 seconds");
        lock.ticks=299;check(!lock.entryPassable(),"gate stays closed to navigation until its full travel completes");
        lock.ticks=300;check(lock.entryPassable() && lock.entryOpening()==1,"fully open gate permits passage");
        check(lock.currentY(300,330)>20 && lock.currentY(300,700)==0,"opposing entry current is local to the gate reach");
        lock.phase=Lock.CLOSING;check(lock.currentY(300,330)==0,"sealing chamber stops the through current");
        lock.phase=Lock.FILLING;lock.ticks=Lock.CYCLE_TICKS/2;
        check(lock.currentX(300,165)!=0,"filling has a cross current to steer against");
        for(int phase:new int[]{Lock.READY,Lock.CLOSING,Lock.FILLING,Lock.FAILED,Lock.COMPLETE}){
            lock.phase=phase;
            check(lock.currentX(300,-100)==0 && lock.currentY(300,430)==0,"sealed/inactive phases do not leak current outside chamber");
        }
        lock.phase=Lock.FILLING;
        check(lock.currentX(160,165)==0 && lock.currentY(440,165)==0,"flow never acts on dry bank positions");
        lock.ticks=0;check(lock.currentX(300,165)==0,"filling current ramps from zero");
        lock.ticks=Lock.CYCLE_TICKS;check(Math.abs(lock.currentX(300,165))<.0001f,"filling current settles at matched water level");
        lock.phase=Lock.EXIT;lock.ticks=300;
        Body drift=new Body(10,0,300,-20,-(float)Math.PI/2),still=new Body(11,0,300,-20,-(float)Math.PI/2);
        for(int i=0;i<180;i++){lock.integrate(drift);Physics.integrate(still);}
        check(drift.y>still.y+12,"open-lock current visibly carries a coasting barge downstream");
        Body along=hull(0),broad=hull(-(float)Math.PI/2);
        for(int i=0;i<60;i++){Physics.integrate(along,25,0);Physics.integrate(broad,25,0);}
        check(broad.vx>along.vx+5,"broadside hull catches more current than an aligned keel");
        Game a=filling(),b=filling();
        for(int i=0;i<60;i++)a.advance(1.0/30);
        for(int i=0;i<288;i++)b.advance(1.0/144);
        check(a.tug.x==b.tug.x && a.tug.y==b.tug.y && a.cargo[0].x==b.cargo[0].x && a.lock.ticks==b.lock.ticks,"current and loaded hull motion agree at 30 and 144 Hz");
        float x=a.tug.x,y=a.tug.y,level=a.lock.waterLevel();int ticks=a.lock.ticks;
        a.background();for(int i=0;i<120;i++)a.advance(1.0/60);
        check(a.tug.x==x && a.tug.y==y && a.lock.ticks==ticks && a.lock.waterLevel()==level,"pause freezes current-driven motion and water level together");
        a.pause();int before=Parity.frame(a);a.lock.ticks+=90;int after=Parity.frame(a);
        check(before!=after,"water gauge and current field visibly change with fill progress");
        System.out.println(checks+" slow gate, relative-water current, orientation and lifecycle assertions passed");
    }
    public static void main(String[] args){run();}
}
