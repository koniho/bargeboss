package com.bargeboss.game;

/** Stateless workshop cycles, attached only to reviewed factory light/exhaust points. */
public final class FactoryActivity {
    private FactoryActivity() {}
    // Bit positions match the named anchors in factory-v1/factory-anchors.json.
    // Rolling-mill anchor1 is the reviewed loading opening, not a new lamp.
    private static final int[] HOT={30,0,7,1,2,0,0,0,0,0,0,0,0,0,0,0};
    private static final int[] WINDOWS={0,15,0,62,60,14,6,2,0,0,0,7,3,12,2,3};
    private static final StaticLayer STEAM=new StaticLayer() {
        public int key(){return 0x42000010;}
        public float width(){return 28;}public float height(){return 28;}
        public void paint(Painter p) {
            p.circle(14,14,11,0x1ed8e5df);p.circle(11,14,7,0x3de7eee5);
            p.circle(17,11,6,0x35e7eee5);p.circle(14,14,5,0x38f5f2dc);
        }
    };
    private static final StaticLayer HEAT=new StaticLayer() {
        public int key(){return 0x42000011;}
        public float width(){return 28;}public float height(){return 28;}
        public void paint(Painter p) {
            p.circle(14,14,12,0x18ff7b26);p.circle(14,14,8,0x3fff9d38);
            p.circle(14,14,4,0x99ffd17b);p.circle(14,14,1.7f,0xd9fff0c3);
        }
    };
    public static boolean hotOpening(int asset,int light){return (HOT[asset]&(1<<light))!=0;}
    public static boolean window(int asset,int light){return (WINDOWS[asset]&(1<<light))!=0;}
    public static boolean steamOutlet(int asset,int outlet) {
        return outlet==0 && (asset==1 || asset==6 || asset==7 || asset==9 || asset==12 || asset==14);
    }
    public static int period(int asset){return 420+(asset*37)%241;}
    private static int clock(int ticks,float phase){return ticks+(int)StrictMath.floor(phase*67);}
    public static int age(int asset,int ticks,float phase){return Math.floorMod(clock(ticks,phase),period(asset));}
    private static float smooth(float value){float t=Physics.clamp(value,0,1);return t*t*(3-2*t);}
    private static float envelope(int age,int duration){return smooth(age/18f)*smooth((duration-age)/42f);}
    /** A different reviewed portal takes each successive furnace charge. */
    public static int activeOpening(int asset,int ticks,float phase) {
        int count=Integer.bitCount(HOT[asset]);if(count==0)return -1;
        int choice=Math.floorMod(Math.floorDiv(clock(ticks,phase),period(asset)),count);
        for(int light=0;light<FactoryArt.lightCount(asset);light++)if(hotOpening(asset,light) && choice--==0)return light;
        return -1;
    }
    public static float activity(int asset,int light,int ticks,float phase) {
        if(hotOpening(asset,light))return light==activeOpening(asset,ticks,phase)?envelope(age(asset,ticks,phase),116):0;
        if(window(asset,light))return envelope(age(asset,ticks,phase+light*.91f),144);
        return 0;
    }
    public static float radiusGain(int asset,int light,int ticks,float phase) {
        return 1+activity(asset,light,ticks,phase)*(hotOpening(asset,light)?.5f:.28f);
    }
    /** One cached flare and three tiny arcs; no sparks are attached to ordinary windows. */
    public static void light(Painter p,int asset,int light,float x,float y,float radius,float opacity,int ticks,float phase) {
        if(opacity<=0 || !hotOpening(asset,light))return;
        float heat=activity(asset,light,ticks,phase);if(heat<=.02f)return;
        float scale=radius*(.8f+.35f*heat)/7;
        p.layer(HEAT,x-14*scale,y-14*scale,scale,opacity*heat*.8f);
        int age=age(asset,ticks,phase);
        for(int spark=0;spark<3;spark++) {
            float t=Math.floorMod(age+spark*11,34)/34f;
            float dx=(spark-1)*radius*.85f*t,dy=radius*(-2.4f*t+3.4f*t*t);
            int alpha=(int)(195*opacity*heat*(1-t));
            if(alpha<2)continue;
            p.line(x+dx,y+dy,x+dx+(spark-1)*radius*.10f,y+dy+radius*.25f,.65f,alpha<<24|0xffd88d);
        }
    }
    /** Three cached pale puffs periodically leave the existing primary exhaust. */
    public static void steam(Painter p,int asset,int outlet,float x,float y,float radius,float scale,float opacity,int ticks,float phase) {
        if(opacity<=0 || !steamOutlet(asset,outlet))return;
        int age=age(asset,ticks,phase+2.4f);
        for(int puff=0;puff<3;puff++) {
            int lived=age-puff*14;if(lived<0 || lived>88)continue;
            float t=lived/88f,fade=smooth(lived/10f)*(1-t)*(1-t);
            if(fade<=.005f)continue;
            float r=(radius*.65f+5*t)*scale;
            float px=x+(-22*t+2*t*(float)StrictMath.sin(t*7+phase))*scale;
            float py=y-58*t*scale;
            float size=r/8;
            p.layer(STEAM,px-14*size,py-14*size,size,opacity*fade*.9f);
        }
    }
}
