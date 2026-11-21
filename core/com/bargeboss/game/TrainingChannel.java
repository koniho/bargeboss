package com.bargeboss.game;

/** A long industrial canal with exact, shared right-angle banks. */
public final class TrainingChannel {
    public static final float MIN=-1260,MAX=Physics.LEFT,HALF_WIDTH=58;
    public static final float TOW_BASIN_TOP=-700, NARROW_LEFT=270,NARROW_RIGHT=330,NARROW_EXIT=-1020,TOW_END=-1500;
    public static final float BASIN_LEFT=-1856,BASIN_TOP=192,BASIN_BOTTOM=832;
    public static final float SLIP_LEFT=-2000,SLIP_TOP=250,SLIP_BOTTOM=350;
    public static final float ENTRY_LEFT=-2240,ENTRY_TOP=570,ENTRY_BOTTOM=730;
    public static final float[] X={-1180,-1000,-820,-640,-460,-280,-100,80};
    public static final float[] Y={500,390,620,390,620,390,600,500};
    /** Each water rectangle is [minX,minY,maxX,maxY]; their union is navigable. */
    public static final float[][] RECTANGLES=rectangles();
    private static final float[] LOW=makeBank(-1),HIGH=makeBank(1),BOUNDARY=boundary();
    /** Canal and practice harbor are permanently connected, regardless of lesson. */
    public static final float[][] WATER_RECTANGLES=waterRectangles();
    public static final float[] WATER_BOUNDARY=waterBoundary();
    private TrainingChannel() {}
    private static float[][] waterRectangles() {
        float[][] water=new float[RECTANGLES.length+8][];
        System.arraycopy(RECTANGLES,0,water,0,RECTANGLES.length);
        water[RECTANGLES.length]=new float[]{Physics.LEFT,TOW_BASIN_TOP,Physics.RIGHT,Physics.BOTTOM};
        water[RECTANGLES.length+1]=new float[]{BASIN_LEFT,BASIN_TOP,MIN,BASIN_BOTTOM};
        water[RECTANGLES.length+2]=new float[]{NARROW_LEFT,NARROW_EXIT,NARROW_RIGHT,TOW_BASIN_TOP};
        water[RECTANGLES.length+3]=new float[]{Physics.LEFT,TOW_END,Physics.RIGHT,NARROW_EXIT};
        water[RECTANGLES.length+4]=new float[]{Physics.RIGHT,570,628,Physics.BOTTOM};
        water[RECTANGLES.length+5]=new float[]{570,148,628,570};
        water[RECTANGLES.length+6]=new float[]{ENTRY_LEFT,ENTRY_TOP,BASIN_LEFT,ENTRY_BOTTOM};
        water[RECTANGLES.length+7]=new float[]{SLIP_LEFT,SLIP_TOP,BASIN_LEFT,SLIP_BOTTOM};
        return water;
    }
    private static float[] waterBoundary() {
        // Open both canal ends into permanent basins; neither mouth has a wall.
        float[] result=new float[LOW.length+HIGH.length+56];int n=0;
        result[n++]=BASIN_LEFT;result[n++]=BASIN_TOP;
        result[n++]=MIN;result[n++]=BASIN_TOP;
        for(float v:LOW) result[n++]=v;
        result[n++]=Physics.LEFT;result[n++]=TOW_BASIN_TOP;
        result[n++]=NARROW_LEFT;result[n++]=TOW_BASIN_TOP;
        result[n++]=NARROW_LEFT;result[n++]=NARROW_EXIT;
        result[n++]=Physics.LEFT;result[n++]=NARROW_EXIT;
        result[n++]=Physics.LEFT;result[n++]=TOW_END;
        result[n++]=Physics.RIGHT;result[n++]=TOW_END;
        result[n++]=Physics.RIGHT;result[n++]=NARROW_EXIT;
        result[n++]=NARROW_RIGHT;result[n++]=NARROW_EXIT;
        result[n++]=NARROW_RIGHT;result[n++]=TOW_BASIN_TOP;
        result[n++]=Physics.RIGHT;result[n++]=TOW_BASIN_TOP;
        result[n++]=Physics.RIGHT;result[n++]=570;
        result[n++]=570;result[n++]=570;
        result[n++]=570;result[n++]=148;
        result[n++]=628;result[n++]=148;
        result[n++]=628;result[n++]=Physics.BOTTOM;
        result[n++]=Physics.LEFT;result[n++]=Physics.BOTTOM;
        for(int i=HIGH.length-2;i>=0;i-=2) { result[n++]=HIGH[i];result[n++]=HIGH[i+1]; }
        result[n++]=MIN;result[n++]=BASIN_BOTTOM;
        result[n++]=BASIN_LEFT;result[n++]=BASIN_BOTTOM;
        result[n++]=BASIN_LEFT;result[n++]=ENTRY_BOTTOM;
        result[n++]=ENTRY_LEFT;result[n++]=ENTRY_BOTTOM;
        result[n++]=ENTRY_LEFT;result[n++]=ENTRY_TOP;
        result[n++]=BASIN_LEFT;result[n++]=ENTRY_TOP;
        result[n++]=BASIN_LEFT;result[n++]=SLIP_BOTTOM;
        result[n++]=SLIP_LEFT;result[n++]=SLIP_BOTTOM;
        result[n++]=SLIP_LEFT;result[n++]=SLIP_TOP;
        result[n++]=BASIN_LEFT;result[n]=SLIP_TOP;
        return result;
    }
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
        for(float[] r:WATER_RECTANGLES) if(x>=r[0] && x<=r[2] && y>=r[1] && y<=r[3]) return true;
        return false;
    }
    public static boolean containsCanal(float x,float y) {
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
        if(gate==1 && x<X[0]) {
            // Align inside the broad basin before crossing the narrower mouth.
            out[0]=x<MIN-40 && Math.abs(y-Y[0])>24?MIN-100:X[0];out[1]=Y[0];return;
        }
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
    public static void collide(Body b) { collide(b,BOUNDARY,false,null); }
    public static void collideWater(Body b){collideWater(b,null);}
    public static void collideWater(Body b,CollisionSplashes splashes){collide(b,WATER_BOUNDARY,true,splashes);}
    private static void collide(Body b,float[] boundary,boolean harbor,CollisionSplashes splashes) {
        for(int iteration=0;iteration<4;iteration++) for(int disc=0;disc<3;disc++) {
            float x=b.discX(disc),y=b.discY(disc),best=Float.MAX_VALUE,qx=0,qy=0,nx=0,ny=0;
            for(int i=0;i<boundary.length;i+=2) {
                int next=(i+2)%boundary.length;
                float ax=boundary[i],ay=boundary[i+1],dx=boundary[next]-ax,dy=boundary[next+1]-ay;
                float length2=dx*dx+dy*dy;if(length2==0) continue;
                float t=Physics.clamp(((x-ax)*dx+(y-ay)*dy)/length2,0,1);
                float px=ax+t*dx,py=ay+t*dy,ex=x-px,ey=y-py,distance=ex*ex+ey*ey;
                if(distance<best) {
                    best=distance;qx=px;qy=py;float length=(float)StrictMath.sqrt(length2);nx=-dy/length;ny=dx/length;
                }
            }
            boolean inside=harbor?contains(x,y):containsCanal(x,y);float distance=(float)StrictMath.sqrt(best);
            float penetration=inside?b.radius-distance:b.radius+distance;
            if(penetration<=0) continue;
            if(distance>.0001f) {float sign=inside?1:-1;nx=sign*(x-qx)/distance;ny=sign*(y-qy)/distance;}
            Physics.wallSplash(splashes,b,qx+nx*.75f,qy+ny*.75f,nx,ny,x,y);
            b.x+=nx*penetration;b.y+=ny*penetration;
            float inward=b.vx*nx+b.vy*ny;
            if(inward<0) {b.vx-=inward*nx*1.08f;b.vy-=inward*ny*1.08f;}
            b.spin*=.94f;
        }
    }
}
