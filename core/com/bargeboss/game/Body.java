package com.bargeboss.game;

/** Three overlapping collision discs approximate an oriented hull and transfer torque. */
public final class Body implements Watercraft {
    public final int id, berth;
    /** Stable stage berth and job assignment; cargo family stays in berth. */
    public int destination=-1,voyageJob=-1;
    public final boolean yacht,cruiser,racing;
    public final float mass, half, radius, inertia;
    public float x, y, vx, vy, angle, spin, age, dockTime;
    public int ability,abilityTicks,abilityCooldown;
    public float abilityAim;
    public int unloadingTicks,lineSnapTicks,tieBurstTicks;
    public int mooringTicks;
    public float mooringX,mooringY,mooringAngle;
    public float engineLoad;
    float smokeCredit;
    public float heave,heaveVelocity;
    float wakeHeave;
    public int wakeKickTicks,wakeCooldown,wakeTrailTicks;
    public boolean floatingInitialized;
    public float tiedX,tiedY,tiedAngle;
    public boolean active = true, lineAttached, awaitingPickup, deliveryRecorded;
    /** Non-player hull health (0..100) and sinking state; see HullDamage. The tug uses Game.hull. */
    public int health=100,healthBefore=100,damagedTick=-1000,sinkTicks;
    public boolean sunk;
    /** Live harbor traffic (not mission cargo) and the moored-boat hull kind it is drawn as (-1: by role). */
    public boolean traffic;public int hullKind=-1;
    /** Height a travel-lift job is hoisting this hull to (0: afloat); see MontroseHaulout.vesselRise. */
    public float liftRise;
    public Body(int id, int berth, float x, float y, float angle) {
        this(id,berth,x,y,angle,false);
    }
    public Body(int id,int berth,float x,float y,float angle,boolean yacht) {
        this(id,berth,x,y,angle,yacht,false);
    }
    public Body(int id,int berth,float x,float y,float angle,boolean yacht,boolean cruiser) {
        this(id,berth,x,y,angle,yacht,cruiser,false);
    }
    public Body(int id,int berth,float x,float y,float angle,boolean yacht,boolean cruiser,boolean racing) {
        this.yacht=yacht;this.cruiser=cruiser;this.racing=racing;
        this.id = id; this.berth = berth; this.x = x; this.y = y; this.angle = angle;
        mass = racing?3.8f:cruiser?2.5f:yacht?6.5f:berth>=0?5f:id==0?2f:4f;
        half = racing?18:cruiser?11:yacht?40:id == 0 ? 12.5f : 21.25f;
        radius = racing?7:cruiser?8:yacht?15:id == 0 ? 15f : 17.5f;
        inertia = mass * (half * half + radius * radius) * .8f;
    }
    public float speed() { return (float) StrictMath.sqrt(vx * vx + vy * vy); }
    public float waterX(){return x;}
    public float waterY(){return y;}
    public float discX(int i) { return x + (i - 1) * half * (float) StrictMath.cos(angle); }
    public float discY(int i) { return y + (i - 1) * half * (float) StrictMath.sin(angle); }
    public boolean hit(float px, float py, float padding) {
        for (int i = 0; i < 3; i++) {
            float dx = px - discX(i), dy = py - discY(i);
            if (dx * dx + dy * dy < (radius + padding) * (radius + padding)) return true;
        }
        return false;
    }
}
