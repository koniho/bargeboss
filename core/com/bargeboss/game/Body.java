package com.bargeboss.game;

/** Three overlapping collision discs approximate an oriented hull and transfer torque. */
public final class Body {
    public final int id, berth;
    public final float mass, half, radius, inertia;
    public float x, y, vx, vy, angle, spin, age, dockTime;
    public int unloadingTicks;
    public float tiedX,tiedY,tiedAngle;
    public boolean active = true, lineAttached;
    public Body(int id, int berth, float x, float y, float angle) {
        this.id = id; this.berth = berth; this.x = x; this.y = y; this.angle = angle;
        mass = id == 0 ? 2f : 4f;
        half = id == 0 ? 10f : 17f;
        radius = id == 0 ? 12f : 14f;
        inertia = mass * (half * half + radius * radius) * .8f;
    }
    public float speed() { return (float) StrictMath.sqrt(vx * vx + vy * vy); }
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
