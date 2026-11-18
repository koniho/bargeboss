package com.bargeboss.game;

/** Current forces, slow gate timing, fixed-step pause and visible level feedback. */
public final class LockCurrentTest {
    private static int checks;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    private static Body hull(float angle){return new Body(10,0,300,165,angle);}
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
        // Fixed-step/pause behavior is covered by CoreTest and LockTest. The
        // O'Brien lock is outside the inlet-to-Basin-Five location.
        System.out.println(checks+" slow gate, relative-water current, orientation and lifecycle assertions passed");
    }
    public static void main(String[] args){run();}
}
