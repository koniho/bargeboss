package com.bargeboss.game;

/** Shared wave field. Hulls emit expanding wake packets at the trough of each moving cycle. */
public final class WaterSurface {
    public static final int CAPACITY=64,LIFE=210;
    public final float[] x=new float[CAPACITY],y=new float[CAPACITY],strength=new float[CAPACITY],heading=new float[CAPACITY];
    public final int[] age=new int[CAPACITY];
    private final Body[] source=new Body[CAPACITY];
    private int cursor;
    public int emitted;
    public WaterSurface() { for(int i=0;i<CAPACITY;i++) age[i]=LIFE; }
    public float energy(float wx,float wy,Body ignore) {
        float total=0;
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE && source[i]!=ignore) {
            float distance=Physics.length(wx-x[i],wy-y[i]),radius=age[i]*.55f;
            float band=Math.max(0,1-Math.abs(distance-radius)/32);
            total+=strength[i]*band*(1-age[i]/(float)LIFE);
        }
        return Math.min(1.8f,total);
    }
    public float height(float wx,float wy,int tick,Body ignore) {
        float local=energy(wx,wy,ignore);
        float wave=(float)StrictMath.sin(tick*.048+wx*.017+wy*.009);
        float cross=(float)StrictMath.sin(tick*.031-wx*.013+wy*.021);
        return (.65f+local*1.2f)*wave+.22f*cross;
    }
    public void step(Game g) {
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) age[i]++;
        floatBoat(g,g.tug);
        for(Body b:g.cargo) if(b!=null && b.active) floatBoat(g,b);
        if(g.tutorial==null) for(Feeder f:g.feeders) if(f.phase!=Feeder.IDLE) {
            floatBoat(g,f.boat);if(f.load!=null) floatBoat(g,f.load);
        }
        if(g.trafficActive()) floatBoat(g,g.trafficBoat);
    }
    private void floatBoat(Game g,Body b) {
        float old=b.heave,velocity=b.heaveVelocity;
        float response=b.berth>=0?.76f:1;
        if(b.lineAttached || b.awaitingPickup) response*=.45f;
        float value=height(b.x,b.y,g.visualTicks,b)*response;
        // Hull response is filtered, so nearby wakes cannot make the sprite snap vertically.
        b.heave+= (value-b.heave)*.18f;
        b.heaveVelocity=(b.heave-old)/Physics.DT;
        if(b.wakeKickTicks>0) b.wakeKickTicks--;
        if(b.wakeCooldown>0) b.wakeCooldown--;
        if(b.floatingInitialized && velocity<0 && b.heaveVelocity>=0 && b.speed()>6 && b.wakeCooldown==0) {
            emit(b);b.wakeKickTicks=18;b.wakeCooldown=35;
        }
        b.floatingInitialized=true;
    }
    private void emit(Body b) {
        int i=cursor;cursor=(cursor+1)%CAPACITY;age[i]=0;source[i]=b;
        float angle=(float)StrictMath.atan2(b.vy,b.vx);
        heading[i]=angle;x[i]=b.x;y[i]=b.y;
        strength[i]=Math.min(1,b.speed()/70)*(b.berth>=0?1.15f:.9f);emitted++;
    }
    public static float lift(Game g,Body b) {
        if(b.floatingInitialized) return b.heave;
        // Title/preview hulls have no simulation clock, but still ride ambient water.
        return g.water.height(b.x,b.y,g.visualTicks,b)*(b.lineAttached?.25f:b.berth>=0?.5f:.75f);
    }
    public void draw(Game g,Painter p) {
        for(int i=0;i<CAPACITY;i++) if(age[i]<LIFE) {
            float t=age[i]/(float)LIFE,r=9+age[i]*.55f;
            int alpha=(int)(150*strength[i]*(1-t)*(1-t));
            float start=heading[i]+(float)Math.PI*.45f,end=heading[i]+(float)Math.PI*1.55f;
            for(int j=0;j<16;j++) {
                float a=start+(end-start)*j/16,b=start+(end-start)*(j+1)/16;
                float xx=x[i]+r*(float)StrictMath.cos(a),yy=y[i]+r*(float)StrictMath.sin(a);
                float bx=x[i]+r*(float)StrictMath.cos(b),by=y[i]+r*(float)StrictMath.sin(b);
                if(xx>=Physics.LEFT && xx<=Physics.RIGHT && yy>=g.channelTop() && yy<=Physics.BOTTOM
                        && bx>=Physics.LEFT && bx<=Physics.RIGHT && by>=g.channelTop() && by<=Physics.BOTTOM)
                    p.line(xx,yy,bx,by,1.5f+strength[i],(alpha<<24)|0xb9f6eb);
            }
        }
    }
}
