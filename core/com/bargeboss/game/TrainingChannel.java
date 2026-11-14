package com.bargeboss.game;

/** A long industrial canal with exact, shared right-angle banks. */
public final class TrainingChannel {
    public static final float MIN=120,MAX=1540,HALF_WIDTH=58;
    public static final float[] X={200,380,560,740,920,1100,1280,1460};
    public static final float[] Y={500,390,620,390,620,390,600,500};
    /** Each water rectangle is [minX,minY,maxX,maxY]; their union is navigable. */
    public static final float[][] RECTANGLES=rectangles();
    private static final float[] LOW=makeBank(-1),HIGH=makeBank(1),BOUNDARY=boundary();
    private TrainingChannel() {}
    /** Banks run from MIN to MAX; repeated X coordinates are vertical quay edges. */
    public static float[] bank(int side) {return side<0?LOW:HIGH;}
    private static float[] makeBank(int side) {
        float[] result=new float[X.length*4];int n=0;
        result[n++]=MIN;result[n++]=Y[0]+side*HALF_WIDTH;
        for(int i=1;i<X.length;i++) {
            float turn=(X[i-1]+X[i])*.5f;
            float edge=turn-side*(Y[i]>Y[i-1]?1:-1)*HALF_WIDTH;
            result[n++]=edge;result[n++]=Y[i-1]+side*HALF_WIDTH;
            result[n++]=edge;result[n++]=Y[i]+side*HALF_WIDTH;
        }
        result[n++]=MAX;result[n]=Y[Y.length-1]+side*HALF_WIDTH;
        return result;
    }
    private static float[] boundary() {
        float[] result=new float[LOW.length+HIGH.length];
        for(int i=0;i<LOW.length;i++) result[i]=LOW[i];
        for(int i=0;i<HIGH.length;i+=2) {
            result[LOW.length+i]=HIGH[HIGH.length-2-i];
            result[LOW.length+i+1]=HIGH[HIGH.length-1-i];
        }
        return result;
    }
    private static float[][] rectangles() {
        float[][] result=new float[(X.length-1)*2+1][4];float from=MIN;int n=0;
        for(int i=1;i<X.length;i++) {
            float turn=(X[i-1]+X[i])*.5f;
            result[n++]=new float[]{Math.max(MIN,from-HALF_WIDTH),Y[i-1]-HALF_WIDTH,turn+HALF_WIDTH,Y[i-1]+HALF_WIDTH};
            result[n++]=new float[]{turn-HALF_WIDTH,Math.min(Y[i-1],Y[i])-HALF_WIDTH,turn+HALF_WIDTH,Math.max(Y[i-1],Y[i])+HALF_WIDTH};
            from=turn;
        }
        result[n]=new float[]{from-HALF_WIDTH,Y[Y.length-1]-HALF_WIDTH,MAX,Y[Y.length-1]+HALF_WIDTH};
        return result;
    }
    /** Horizontal reach center; vertical jogs are represented by RECTANGLES and bank(). */
    public static float center(float x) {
        for(int i=1;i<X.length;i++) if(x<(X[i-1]+X[i])*.5f) return Y[i-1];
        return Y[Y.length-1];
    }
    public static boolean contains(float x,float y) {
        for(float[] r:RECTANGLES) if(x>=r[0] && x<=r[2] && y>=r[1] && y<=r[3]) return true;
        return false;
    }
    /** A gate spans the whole waterway, not a small circle around its label. */
    public static boolean reachedGate(int gate,float previousX,float previousY,float x,float y) {
        float gx=X[gate],gy=Y[gate];
        if(Math.abs(x-gx)<=23 && Math.abs(y-gy)<=HALF_WIDTH) return true;
        // Catch a fast forward crossing even when neither sampled position is
        // in the gate band. Only the next ordered gate can receive credit.
        if(previousX<gx && x>=gx) {
            float t=(gx-previousX)/(x-previousX);
            return Math.abs(previousY+(y-previousY)*t-gy)<=HALF_WIDTH;
        }
        return false;
    }
    /** Follow the three orthogonal legs leading from the previous gate to the next. */
    public static void guide(float x,float y,int gate,float[] out) {
        gate=Math.max(1,Math.min(X.length-1,gate));float turn=(X[gate-1]+X[gate])*.5f;
        float[] px={X[gate-1],turn,turn,X[gate]},py={Y[gate-1],Y[gate-1],Y[gate],Y[gate]};
        float best=Float.MAX_VALUE,along=0,total=0;
        for(int i=0;i<3;i++) {
            float dx=px[i+1]-px[i],dy=py[i+1]-py[i],length=Physics.length(dx,dy);
            float t=Physics.clamp(((x-px[i])*dx+(y-py[i])*dy)/(length*length),0,1);
            float ex=x-px[i]-t*dx,ey=y-py[i]-t*dy,distance=ex*ex+ey*ey;
            if(distance<=best) {best=distance;along=total+t*length;}
            total+=length;
        }
        float target=Math.min(total,along+28);
        for(int i=0;i<3;i++) {
            float dx=px[i+1]-px[i],dy=py[i+1]-py[i],length=Physics.length(dx,dy);
            if(target<=length || i==2) {
                float t=Math.min(1,target/length);out[0]=px[i]+t*dx;out[1]=py[i]+t*dy;return;
            }
            target-=length;
        }
    }
    /** Resolve every hull disc against the exterior boundary of the rectangle union. */
    public static void collide(Body b) {
        for(int iteration=0;iteration<4;iteration++) for(int disc=0;disc<3;disc++) {
            float x=b.discX(disc),y=b.discY(disc),best=Float.MAX_VALUE,qx=0,qy=0,nx=0,ny=0;
            for(int i=0;i<BOUNDARY.length;i+=2) {
                int next=(i+2)%BOUNDARY.length;
                float ax=BOUNDARY[i],ay=BOUNDARY[i+1],dx=BOUNDARY[next]-ax,dy=BOUNDARY[next+1]-ay;
                float length2=dx*dx+dy*dy;if(length2==0) continue;
                float t=Physics.clamp(((x-ax)*dx+(y-ay)*dy)/length2,0,1);
                float px=ax+t*dx,py=ay+t*dy,ex=x-px,ey=y-py,distance=ex*ex+ey*ey;
                if(distance<best) {
                    best=distance;qx=px;qy=py;float length=(float)StrictMath.sqrt(length2);nx=-dy/length;ny=dx/length;
                }
            }
            boolean inside=contains(x,y);float distance=(float)StrictMath.sqrt(best);
            float penetration=inside?b.radius-distance:b.radius+distance;
            if(penetration<=0) continue;
            if(distance>.0001f) {float sign=inside?1:-1;nx=sign*(x-qx)/distance;ny=sign*(y-qy)/distance;}
            b.x+=nx*penetration;b.y+=ny*penetration;
            float inward=b.vx*nx+b.vy*ny;
            if(inward<0) {b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f;}
            b.spin*=.94f;
        }
    }
}
