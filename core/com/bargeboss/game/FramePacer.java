package com.bargeboss.game;

/** Host-supplied monotonic time limits rendering only; simulation keeps its fixed steps. */
public final class FramePacer {
    private static final long PERIOD=16666667L, TOLERANCE=500000L;
    private long next;
    public void reset(){next=0;}
    public boolean allow(long now){
        if(next!=0 && now+TOLERANCE<next)return false;
        if(next==0 || now-next>=PERIOD)next=now+PERIOD;
        else next+=PERIOD;
        return true;
    }
}
