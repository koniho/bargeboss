package com.bargeboss.game;

/** A vertical, several-screen practice course in the same rotated river coordinates. */
public final class TrainingChannel {
    public static final float MIN=120,MAX=1540,HALF_WIDTH=58;
    public static final float[] X={200,380,560,740,920,1100,1280,1460};
    public static final float[] Y={500,390,620,390,620,390,600,500};
    private TrainingChannel() {}
    private static int segment(float x) {
        for(int i=0;i<X.length-1;i++) if(x<X[i+1]) return i;
        return X.length-2;
    }
    public static float center(float x) {
        int i=segment(x);float t=Physics.clamp((x-X[i])/(X[i+1]-X[i]),0,1);
        return Y[i]+(Y[i+1]-Y[i])*t*t*(3-2*t);
    }
    public static float slope(float x) {
        int i=segment(x);float t=Physics.clamp((x-X[i])/(X[i+1]-X[i]),0,1);
        return (Y[i+1]-Y[i])*6*t*(1-t)/(X[i+1]-X[i]);
    }
    public static boolean contains(float x,float y) {
        return x>=MIN && x<=MAX && Math.abs(y-center(x))<=HALF_WIDTH;
    }
    /** Resolve every hull disc against the actual curved banks, including their slope. */
    public static void collide(Body b) {
        for(int iteration=0;iteration<3;iteration++) for(int i=0;i<3;i++) {
            float x=b.discX(i),y=b.discY(i);
            if(x-b.radius<MIN) { b.x+=MIN-x+b.radius;if(b.vx<0) b.vx=-b.vx*.08f; }
            if(x+b.radius>MAX) { b.x-=x+b.radius-MAX;if(b.vx>0) b.vx=-b.vx*.08f; }
            float slope=slope(x),normal=(float)StrictMath.sqrt(1+slope*slope),offset=y-center(x);
            float penetration=(Math.abs(offset)-HALF_WIDTH)/normal+b.radius;
            if(penetration<=0) continue;
            float side=offset>=0?1:-1,nx=side*slope/normal,ny=-side/normal;
            b.x+=nx*penetration;b.y+=ny*penetration;
            float inward=b.vx*nx+b.vy*ny;
            if(inward<0) { b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f; }
            b.spin*=.94f;
        }
    }
}
