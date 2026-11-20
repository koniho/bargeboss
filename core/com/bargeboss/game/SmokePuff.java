package com.bargeboss.game;

/** Dark exhaust material with two bounded cached fallbacks; transport stays with emitters. */
public final class SmokePuff {
    public static final int SOOT=0x1c201b,INDUSTRIAL=0x363326;
    private static final StaticLayer SOOT_KERNEL=kernel(SOOT,0x43000000),DIRTY_KERNEL=kernel(INDUSTRIAL,0x43000001);
    private static final StaticLayer[] PROFILE_KERNELS={kernel(LightingProfile.DAWN.smoke,0x43000002),kernel(LightingProfile.DAYLIGHT.smoke,0x43000003),SOOT_KERNEL,kernel(LightingProfile.NIGHT.smoke,0x43000004)};
    private SmokePuff() {}
    private static StaticLayer kernel(final int rgb,final int id) {
        return new StaticLayer() {
            public int key(){return id;}
            public float width(){return 40;}public float height(){return 40;}
            public boolean backgroundSafe(){return true;}
            public void paint(Painter p) {
                for(int i=0;i<8;i++) {
                    float r=17-i*1.35f;
                    p.circle(20,21,r,0x20000000|rgb);
                    p.circle(15,20,r*.68f,0x22000000|rgb);
                    p.circle(24,16,r*.65f,0x22000000|rgb);
                }
            }
        };
    }
    public static void fallback(Painter p,float x,float y,float radius,int argb) {
        if(radius<=0 || (argb>>>24)==0)return;
        float scale=radius/20;
        p.layer(p.lighting()!=LightingProfile.DUSK?PROFILE_KERNELS[p.lighting().id]:(argb&0xffffff)==SOOT?SOOT_KERNEL:DIRTY_KERNEL,x-radius,y-radius,scale,(argb>>>24)/255f);
    }
}
