package com.bargeboss.game;

/** A requested opening, safe waiting period, lift, passage window and guarded closing. */
public final class Bridge {
    public static final float Y=200;
    public static final int CLOSED=0,WAITING=1,OPENING=2,OPEN=3,CLOSING=4;
    public int phase,ticks,openings;
    public float lift() { return phase==OPEN?1:phase==OPENING?ticks/120f:phase==CLOSING?1-ticks/120f:0; }
    public boolean passable() { return lift()>=.95f; }
    public boolean separates(float y,float target) { return (y-Y)*(target-Y)<0; }
    private boolean near(Game g,float distance) {
        if(g.tow.cargo!=null && separates(g.tug.y,g.tow.cargo.y)) return true;
        if(Math.abs(g.tug.y-Y)<distance) return true;
        for(Body b:g.cargo) if(b!=null && b.active && Math.abs(b.y-Y)<distance) return true;
        return false;
    }
    public void step(Game g) {
        if(!g.bridgeStage()) return;
        ticks++;
        switch(phase) {
            case CLOSED: if(near(g,190)) { phase=WAITING;ticks=0;g.tell("BRIDGE REQUESTED / WAIT FOR GREEN"); } break;
            case WAITING: if(ticks>=360) { phase=OPENING;ticks=0; } break;
            case OPENING: if(ticks>=120) { phase=OPEN;ticks=0;openings++;g.tell("BRIDGE OPEN / CLEAR TO PASS");g.haptic(Game.HAPTIC_TICK); } break;
            case OPEN: if(ticks>=720 && !near(g,80)) { phase=CLOSING;ticks=0; } break;
            case CLOSING:
                if(near(g,80)) { phase=OPENING;ticks=120-ticks; }
                else if(ticks>=120) { phase=CLOSED;ticks=0; }
                break;
            default: break;
        }
    }
    public float collide(Body b) {
        if(passable() || !b.active) return 0;
        float impact=0;
        for(int i=0;i<3;i++) {
            float delta=b.discY(i)-Y,reach=b.radius+12;
            if(Math.abs(delta)>=reach) continue;
            float side=b.y>=Y?1:-1;
            b.y+=side*reach-delta;
            if(b.vy*side<0) { impact=Math.max(impact,Math.abs(b.vy));b.vy=-b.vy*.08f; }
            b.spin*=.6f;
        }
        return impact;
    }
    public String signal() {
        if(phase==WAITING) return "WAIT "+((360-ticks+59)/60)+"S";
        return phase==OPEN?"OPEN":phase==OPENING?"OPENING":phase==CLOSING?"CLOSING":"REQUEST ON APPROACH";
    }
}
