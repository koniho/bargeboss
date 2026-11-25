package com.bargeboss.game;

/** Draw-only screen shake from watercraft impacts. Gameplay, input mapping and the HUD never see it.
 *
 * Two parts, both aged on the 60 Hz visual clock:
 * - a directional punch: a damped spring kicked along the projected impact direction, so a hit
 *   reads as a shove from the side it came from;
 * - trauma: a decaying 0..1 level whose square scales a smooth two-frequency wobble, so light
 *   bumps barely move the view and heavy rams rattle it briefly.
 * Each part is bounded, so stacked hits cannot throw the view far. */
final class ImpactShake {
    /** Screen units of wobble at full trauma. */
    static final float WOBBLE=9;
    /** Largest punch displacement in screen units. */
    static final float PUNCH=11;
    static final float DECAY=1/30f;
    private float trauma,x,y,vx,vy;
    private int tick;

    /** power is CollisionSplashes' 0.12..1 impact power; the player's own hits shake hardest. */
    void hit(float power,float screenDx,float screenDy,boolean player){
        float weight=player?1:.35f,p=power*power;
        trauma=Math.min(1,trauma+weight*(.08f+.72f*p));
        float len=Physics.length(screenDx,screenDy);
        if(len<.001f || !Float.isFinite(len))return;
        float kick=weight*(1.2f+4.8f*p);
        vx+=screenDx/len*kick;vy+=screenDy/len*kick;
    }
    void step(){
        tick++;trauma=Math.max(0,trauma-DECAY);
        // Critically under-damped spring: one shove, one small rebound, settled in about 0.4 s.
        vx=(vx-.32f*x)*.74f;vy=(vy-.32f*y)*.74f;x+=vx;y+=vy;
        float len=Physics.length(x,y);if(len>PUNCH){x*=PUNCH/len;y*=PUNCH/len;}
        if(trauma==0 && Math.abs(x)+Math.abs(y)+Math.abs(vx)+Math.abs(vy)<.01f)x=y=vx=vy=0;
    }
    boolean active(){return trauma>0 || x!=0 || y!=0;}
    float offsetX(){
        float a=trauma*trauma*WOBBLE;
        return x+a*(float)(.62*StrictMath.sin(tick*1.73)+.38*StrictMath.sin(tick*3.11+1.3));
    }
    float offsetY(){
        float a=trauma*trauma*WOBBLE*.75f;
        return y+a*(float)(.58*StrictMath.sin(tick*2.07+.7)+.42*StrictMath.sin(tick*3.67+2.1));
    }
}
