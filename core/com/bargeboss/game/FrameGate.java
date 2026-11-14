package com.bargeboss.game;

/** Shared redraw policy only. Hosts still advance simulation and poll haptics every callback. */
public final class FrameGate {
    private Game root,active;
    private Layout rootLayout,activeLayout;
    private int rootMode,activeMode,ticks,visualTicks,progressVersion;
    private boolean haptics,dirty=true;

    /** Input and native exposure/lifecycle changes may change pixels before the next fixed step. */
    public void markDirty() {dirty=true;}

    /** Repeated requests are harmless; only an actual completed draw consumes a dirty state. */
    public boolean needsFrame(Game game) {
        Game current=game.activeGame();
        return dirty || root!=game || active!=current || rootLayout!=game.layout || activeLayout!=current.layout
                || rootMode!=game.mode || activeMode!=current.mode || ticks!=current.ticks
                || visualTicks!=current.visualTicks || haptics!=current.hapticsEnabled
                || progressVersion!=game.progressVersion();
    }

    public void rendered(Game game) {
        root=game;active=game.activeGame();rootLayout=game.layout;activeLayout=active.layout;
        rootMode=game.mode;activeMode=active.mode;ticks=active.ticks;visualTicks=active.visualTicks;
        haptics=active.hapticsEnabled;progressVersion=game.progressVersion();dirty=false;
    }
}
