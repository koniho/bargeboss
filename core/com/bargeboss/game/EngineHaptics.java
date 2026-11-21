package com.bargeboss.game;

/** Bounded diesel pulses. Shared timing/load selection; native hosts supply tactile output. */
public final class EngineHaptics {
    private int ticks,pending,quiet;
    void reset(){ticks=pending=quiet=0;}
    void interrupt(){pending=0;quiet=24;ticks=0;}
    void step(Game g){
        pending=0; // Never queue a backlog across slow frames.
        if(g.mode!=Game.PLAY || !g.hapticsEnabled || g.conversation() || g.tug==null){ticks=0;return;}
        if(quiet>0){quiet--;return;}
        float load=Physics.clamp(g.tug.engineLoad,0,1);
        int interval=Math.round(48-30*load); // Quiet idle at 1.25 Hz; loaded engine at 3.33 Hz.
        if(++ticks>=interval){ticks=0;pending=load>=.55f?2:1;}
    }
    public int poll(Game g){
        int result=pending;pending=0;
        return g.mode==Game.PLAY && g.hapticsEnabled && !g.conversation()?result:0;
    }
}
