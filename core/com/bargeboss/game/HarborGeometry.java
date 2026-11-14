package com.bargeboss.game;

/** Solid quay projections in world space; the camera never changes these footprints. */
public final class HarborGeometry {
    // left, top, right, bottom. North finger pier and south finger pier.
    public static final float[][] PIERS={{381,248,430,322},{170,345,212,390}};
    private HarborGeometry() {}
    public static float collide(Body b) {
        float impact=0;
        for(float[] r:PIERS) for(int i=0;i<3;i++) {
            float x=b.discX(i),y=b.discY(i),cx=Physics.clamp(x,r[0],r[2]),cy=Physics.clamp(y,r[1],r[3]);
            float dx=x-cx,dy=y-cy,d=Physics.length(dx,dy);
            if(d>=b.radius) continue;
            if(d<.001f) {
                float left=x-r[0],right=r[2]-x,top=y-r[1],bottom=r[3]-y;
                float nearest=Math.min(Math.min(left,right),Math.min(top,bottom));
                if(nearest==left) { dx=-1;dy=0;d=-left; }
                else if(nearest==right) { dx=1;dy=0;d=-right; }
                else if(nearest==top) { dx=0;dy=-1;d=-top; }
                else { dx=0;dy=1;d=-bottom; }
            } else { dx/=d;dy/=d; }
            b.x+=dx*(b.radius-d);b.y+=dy*(b.radius-d);
            float inward=b.vx*dx+b.vy*dy;
            if(inward<0) { impact=Math.max(impact,-inward); b.vx-=1.15f*inward*dx;b.vy-=1.15f*inward*dy; }
            b.spin*=.8f;
        }
        return impact;
    }
    static boolean clear(float x,float y,float xx,float yy,float pad) {
        for(float[] r:PIERS) {
            float lo=0,hi=1; boolean outside=false;
            for(int axis=0;axis<2;axis++) {
                float min=r[axis]-pad,max=r[axis+2]+pad;
                float start=axis==0?x:y,delta=axis==0?xx-x:yy-y;
                if(Math.abs(delta)<.0001f) { if(start<=min || start>=max) outside=true; }
                else { float a=(min-start)/delta,b=(max-start)/delta;
                    lo=Math.max(lo,Math.min(a,b)); hi=Math.min(hi,Math.max(a,b)); }
            }
            if(!outside && hi>lo+.0001f && hi>0 && lo<1) return false;
        }
        return true;
    }
    /** Tiny visibility graph; returns the first reachable corner on the shortest clear route. */
    public static void waypoint(float x,float y,float tx,float ty,float pad,float[] out) {
        waypoint(x,y,tx,ty,pad,Physics.TOP,Physics.BOTTOM,out);
    }
    public static void waypoint(float x,float y,float tx,float ty,float pad,float top,float bottom,float[] out) {
        out[0]=tx;out[1]=ty;
        // A hull can be physically clear while inside the conservative planning margin.
        // Escape toward the channel before building a route; never deadlock against that margin.
        for(float[] r:PIERS) if(x>r[0]-pad && x<r[2]+pad && y>r[1]-pad && y<r[3]+pad) {
            out[0]=r[0]>300?r[0]-pad-4:r[2]+pad+4;out[1]=y;return;
        }
        if(clear(x,y,tx,ty,pad)) return;
        float[] xs=new float[10],ys=new float[10],cost=new float[10]; int[] previous=new int[10]; boolean[] used=new boolean[10];
        xs[0]=x;ys[0]=y;xs[1]=tx;ys[1]=ty;
        int n=2;
        for(float[] r:PIERS) for(int side=0;side<4;side++) {
            xs[n]=(side%2==0?r[0]-pad-1:r[2]+pad+1);
            ys[n]=(side<2?r[1]-pad-1:r[3]+pad+1);n++;
        }
        for(int i=1;i<n;i++) { cost[i]=Float.MAX_VALUE;previous[i]=-1; }
        for(int step=0;step<n;step++) {
            int a=-1; for(int i=0;i<n;i++) if(!used[i] && (a<0 || cost[i]<cost[a])) a=i;
            if(a<0 || cost[a]==Float.MAX_VALUE) break;
            used[a]=true;if(a==1) break;
            for(int b=1;b<n;b++) if(!used[b] && xs[b]>=Physics.LEFT+pad && xs[b]<=Physics.RIGHT-pad
                    && ys[b]>=top+pad && ys[b]<=bottom-pad && clear(xs[a],ys[a],xs[b],ys[b],pad)) {
                float distance=cost[a]+Physics.length(xs[b]-xs[a],ys[b]-ys[a]);
                if(distance<cost[b]) { cost[b]=distance;previous[b]=a; }
            }
        }
        if(previous[1]<0) return;
        int next=1;while(previous[next]>0) next=previous[next];
        out[0]=xs[next];out[1]=ys[next];
    }
}
