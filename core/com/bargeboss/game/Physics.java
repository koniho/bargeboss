package com.bargeboss.game;

public final class Physics {
    private Physics() {}
    public static final float DT = 1f / 60f;
    public static final float LEFT=170, RIGHT=430, TOP=215, BOTTOM=817;
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
        // Displacement barges glide along their keel but present a broad face to lateral flow.
        boolean barge=b.berth>=0;
        float forwardDrag=barge?.065f+.0012f*Math.abs(forward):.22f;
        float sideDrag=barge?.8f+.022f*Math.abs(side):1.5f;
        forward/=1+forwardDrag*DT;
        side/=1+sideDrag*DT;
        b.vx = forward * c - side * s;
        b.vy = forward * s + side * c + .9f * DT;
        if(barge) {
            float flow=(float)StrictMath.atan2(side,forward);
            if(flow>Math.PI/2) flow-=(float)Math.PI;
            if(flow<-Math.PI/2) flow+=(float)Math.PI;
            b.spin+=flow*Math.min(1,b.speed()/35)*.28f*DT;
        }
        b.spin/=1+(barge?.42f+.012f*Math.abs(forward):2.6f)*DT;
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
    public static boolean touching(Body a,Body b,float padding) {
        float reach=a.radius+b.radius+padding;
        for(int i=0;i<3;i++) for(int j=0;j<3;j++)
            if(length(a.discX(i)-b.discX(j),a.discY(i)-b.discY(j))<=reach) return true;
        return false;
    }
    public static float banks(Body b) {
        float impact=0;
        for(int i=0;i<3;i++) {
            float x=b.discX(i), y=b.discY(i), r=b.radius;
            if(x-r<LEFT) { impact=Math.max(impact,-b.vx); b.x+=LEFT-x+r; b.vx=Math.abs(b.vx)*.2f; b.spin*=.8f; }
            if(x+r>RIGHT) { impact=Math.max(impact,b.vx); b.x-=x+r-RIGHT; b.vx=-Math.abs(b.vx)*.2f; b.spin*=.8f; }
            if(y-r<TOP) { impact=Math.max(impact,-b.vy); b.y+=TOP-y+r; b.vy=Math.abs(b.vy)*.2f; }
            if(y+r>BOTTOM) { impact=Math.max(impact,b.vy); b.y-=y+r-BOTTOM; b.vy=-Math.abs(b.vy)*.2f; }
        }
        return Math.max(impact,HarborGeometry.collide(b));
    }
}
