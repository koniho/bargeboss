package com.bargeboss.game;

public final class Physics {
    private Physics() {}
    public static final float DT = 1f / 60f;
    public static float clamp(float v, float low, float high) { return Math.max(low, Math.min(high, v)); }
    public static float length(float x, float y) { return (float) StrictMath.sqrt(x*x + y*y); }
    public static float angle(float a) {
        while (a > Math.PI) a -= (float) (2 * Math.PI);
        while (a < -Math.PI) a += (float) (2 * Math.PI);
        return a;
    }
    public static void integrate(Body b) {
        float c = (float) StrictMath.cos(b.angle), s = (float) StrictMath.sin(b.angle);
        float forward = b.vx * c + b.vy * s;
        float side = -b.vx * s + b.vy * c;
        // Low forward drag, stronger lateral drag: heavy glide without uncontrollable skating.
        forward *= 1f - .22f * DT;
        side *= 1f - 1.5f * DT;
        b.vx = forward * c - side * s;
        b.vy = forward * s + side * c + .9f * DT;
        b.spin *= 1f - 2.6f * DT;
        b.x += b.vx * DT; b.y += b.vy * DT;
        b.angle = angle(b.angle + b.spin * DT);
    }
    public static float collide(Body a, Body b) {
        if (!a.active || !b.active || Math.abs(a.x-b.x)>100 || Math.abs(a.y-b.y)>100) return 0;
        float impact = 0;
        for (int i=0; i<3; i++) for (int j=0; j<3; j++) {
            float ax=a.discX(i), ay=a.discY(i), bx=b.discX(j), by=b.discY(j);
            float dx=bx-ax, dy=by-ay, distance=length(dx,dy), reach=a.radius+b.radius;
            if (distance >= reach) continue;
            float nx=distance>.001f?dx/distance:1, ny=distance>.001f?dy/distance:0;
            float invA=1/a.mass, invB=1/b.mass;
            float correction=Math.max(0,reach-distance-.15f)*.45f/(invA+invB);
            a.x-=nx*correction*invA; a.y-=ny*correction*invA;
            b.x+=nx*correction*invB; b.y+=ny*correction*invB;
            float arx=ax-a.x, ary=ay-a.y, brx=bx-b.x, bry=by-b.y;
            float relative=(b.vx-b.spin*bry-a.vx+a.spin*ary)*nx
                    +(b.vy+b.spin*brx-a.vy-a.spin*arx)*ny;
            if (relative>=0) continue;
            impact=Math.max(impact,-relative);
            float ca=arx*ny-ary*nx, cb=brx*ny-bry*nx;
            float impulse=-(1.08f)*relative/(invA+invB+ca*ca/a.inertia+cb*cb/b.inertia);
            a.vx-=impulse*nx*invA; a.vy-=impulse*ny*invA; a.spin-=ca*impulse/a.inertia;
            b.vx+=impulse*nx*invB; b.vy+=impulse*ny*invB; b.spin+=cb*impulse/b.inertia;
        }
        return impact;
    }
    public static float banks(Body b) {
        float impact=0;
        for(int i=0;i<3;i++) {
            float x=b.discX(i), y=b.discY(i), r=b.radius;
            if(x-r<110) { impact=Math.max(impact,-b.vx); b.x+=110-x+r; b.vx=Math.abs(b.vx)*.2f; b.spin*=.8f; }
            if(x+r>490) { impact=Math.max(impact,b.vx); b.x-=x+r-490; b.vx=-Math.abs(b.vx)*.2f; b.spin*=.8f; }
            if(y-r<215) { impact=Math.max(impact,-b.vy); b.y+=215-y+r; b.vy=Math.abs(b.vy)*.2f; }
            if(y+r>817) { impact=Math.max(impact,b.vy); b.y-=y+r-817; b.vy=-Math.abs(b.vy)*.2f; }
        }
        return impact;
    }
}
