package com.bargeboss.game;

/** Soft lamp volume and broad reflected pool, rasterized once by each native host. */
public final class DiffuseLight {
    private DiffuseLight() {}
    private static void ellipse(Painter p,float x,float y,float rx,float ry,int alpha) {
        float[] xy=new float[48];
        for(int i=0;i<24;i++){double a=i*StrictMath.PI/12;xy[i*2]=x+rx*(float)StrictMath.cos(a);xy[i*2+1]=y+ry*(float)StrictMath.sin(a);}
        p.poly(xy,(alpha<<24)|0xffc17b);
    }
    static final StaticLayer BEAM=new StaticLayer() {
        public int key(){return 0x42000001;}public float width(){return 80;}public float height(){return 64;}
        public void paint(Painter p) {
            // Overlapping soft lobes widen through haze, then dissolve beyond the foot.
            // No triangle or shared horizontal end edge is present at any intensity.
            for(int step=11;step>=0;step--) {
                float t=step/11f,y=9+step*2.6f;
                int alpha=1+Math.round(2.5f*(1-t));
                for(int ring=6;ring>=1;ring--) {
                    float k=ring/6f;
                    ellipse(p,40+3*t,y,(2+14*t)*k,(5+6*t)*k,alpha);
                }
            }
        }
    };
    static final StaticLayer POOL=new StaticLayer() {
        public int key(){return 0x42000002;}public float width(){return 112;}public float height(){return 64;}
        public void paint(Painter p) {
            for(int ring=18;ring>=1;ring--) {
                float t=ring/18f;
                ellipse(p,56,32,52*t,27*t,1+Math.round(3*(1-t)));
            }
        }
    };
    public static void lamp(Painter p,float x,float foot,float scale,float opacity) {
        p.layer(POOL,x-56*scale,foot-30*scale,scale,opacity);
        p.layer(BEAM,x-40*scale,foot-26*scale,scale,opacity);
    }
}
